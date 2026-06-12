package com.keystone.policy.source;

import com.keystone.policy.domain.exception.PolicySyncException;
import com.keystone.policy.domain.model.Policy;
import com.keystone.policy.domain.model.PolicyScope;
import com.keystone.policy.domain.model.PolicySeverity;
import com.keystone.policy.infrastructure.source.GitPolicySource;
import com.keystone.policy.validator.PolicyValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Fetches policy YAML files from a local Git repository clone.
 *
 * <p>Policies are stored as {@code .policy} YAML files in a designated directory
 * within the Git repository (e.g. {@code .keystone/policies/}).
 *
 * <p>Uses JGit for Git operations. The repository is cloned/mirrored to a local
 * cache directory on first access and updated on subsequent syncs.
 */
@Component
public class GitPolicySourceImpl implements GitPolicySource {

    private static final Logger log = LoggerFactory.getLogger(GitPolicySourceImpl.class);

    private final String sourceId;
    private final String repoUrl;
    private final String branch;
    private final String policyDirectory;
    private final String localClonePath;
    private final PolicyValidator policyValidator;

    public GitPolicySourceImpl(
            @Value("${policy.git.source-id:default}") String sourceId,
            @Value("${policy.git.repository:}") String repoUrl,
            @Value("${policy.git.branch:main}") String branch,
            @Value("${policy.git.policy-path:.keystone/policies}") String policyDirectory,
            @Value("${policy.git.local-path:${java.io.tmpdir}/keystone-policies}") String localClonePath,
            PolicyValidator policyValidator) {
        this.sourceId = sourceId;
        this.repoUrl = repoUrl;
        this.branch = branch;
        this.policyDirectory = policyDirectory;
        this.localClonePath = localClonePath;
        this.policyValidator = policyValidator;
    }

    @Override
    public String getSourceId() {
        return sourceId;
    }

    @Override
    public String getSourceType() {
        return "git";
    }

    @Override
    public String getRepositoryUrl() {
        return repoUrl;
    }

    @Override
    public String getBranch() {
        return branch;
    }

    @Override
    public String getPolicyDirectory() {
        return policyDirectory;
    }

    @Override
    public PolicySeverity getDefaultSeverity() {
        return PolicySeverity.MAJOR;
    }

    @Override
    public List<Policy> fetchPolicies() throws PolicySyncException {
        return fetchPoliciesFromRef(branch);
    }

    @Override
    public List<Policy> fetchPoliciesFromRef(String ref) throws PolicySyncException {
        if (repoUrl == null || repoUrl.isBlank()) {
            log.warn("No Git repository configured for policy source '{}'", sourceId);
            return List.of();
        }

        Path cloneDir = Path.of(localClonePath, sourceId);
        try {
            ensureLocalClone(cloneDir, ref);
            return loadPolicyFiles(cloneDir);
        } catch (Exception e) {
            throw new PolicySyncException(sourceId, "Failed to fetch policies from " + repoUrl, e);
        }
    }

    @Override
    public String getCurrentVersion() throws PolicySyncException {
        Path cloneDir = Path.of(localClonePath, sourceId);
        try {
            return getHeadCommitSha(cloneDir);
        } catch (Exception e) {
            throw new PolicySyncException(sourceId, "Failed to get current version", e);
        }
    }

    @Override
    public boolean hasChanged(String lastKnownVersion) throws PolicySyncException {
        try {
            String currentVersion = getCurrentVersion();
            return !currentVersion.equals(lastKnownVersion);
        } catch (Exception e) {
            throw new PolicySyncException(sourceId, "Failed to check for changes", e);
        }
    }

    @Override
    public String getHeadCommitSha() throws PolicySyncException {
        return getCurrentVersion();
    }

    /**
     * Ensures a local clone of the repository exists and is up-to-date.
     */
    private void ensureLocalClone(Path cloneDir, String ref) throws PolicySyncException {
        try {
            if (!Files.exists(cloneDir)) {
                // Initial clone
                log.info("Cloning policy repository {} to {}", repoUrl, cloneDir);
                executeCommand("git", "clone", "--depth=1", "--branch=" + ref,
                        repoUrl, cloneDir.toString());
            } else {
                // Update existing clone
                log.debug("Updating policy repository at {}", cloneDir);
                executeCommandInDir(cloneDir, "git", "fetch", "origin", ref);
                executeCommandInDir(cloneDir, "git", "checkout", ref);
                executeCommandInDir(cloneDir, "git", "reset", "--hard", "origin/" + ref);
            }
        } catch (Exception e) {
            throw new PolicySyncException(sourceId,
                    "Failed to clone/update repository at " + repoUrl, e);
        }
    }

    /**
     * Loads and parses all .policy files from the policy directory.
     */
    private List<Policy> loadPolicyFiles(Path cloneDir) throws PolicySyncException {
        Path policyPath = cloneDir.resolve(policyDirectory);
        if (!Files.exists(policyPath) || !Files.isDirectory(policyPath)) {
            log.warn("Policy directory does not exist: {}", policyPath);
            return List.of();
        }

        List<Policy> policies = new ArrayList<>();
        List<PolicySyncException> errors = new ArrayList<>();

        try (Stream<Path> files = Files.walk(policyPath)) {
            List<Path> policyFiles = files
                    .filter(Files::isRegularFile)
                    .filter(f -> f.toString().endsWith(".policy"))
                    .sorted()
                    .toList();

            for (Path file : policyFiles) {
                try {
                    String content = Files.readString(file);
                    String relativeName = policyPath.relativize(file).toString();
                    String setName = relativeName.contains("/")
                            ? relativeName.substring(0, relativeName.lastIndexOf('/'))
                            : "default";

                    List<Policy> parsed = parsePolicyFile(file.getFileName().toString(),
                            content, sourceId + "/" + setName);
                    policies.addAll(parsed);
                } catch (Exception e) {
                    log.warn("Failed to parse policy file {}: {}", file, e.getMessage());
                    if (errors.isEmpty()) {
                        errors.add(new PolicySyncException(sourceId,
                                "Failed to parse " + file + ": " + e.getMessage()));
                    }
                }
            }
        } catch (IOException e) {
            throw new PolicySyncException(sourceId,
                    "Failed to walk policy directory: " + policyPath, e);
        }

        if (policies.isEmpty() && !errors.isEmpty()) {
            throw errors.get(0);
        }

        return policies;
    }

    /**
     * Parses a single .policy file into one or more Policy domain objects.
     *
     * <p>Supports both single-policy and multi-policy YAML formats.
     */
    private List<Policy> parsePolicyFile(String fileName, String content,
                                          String sourceId) throws Exception {
        // Simple YAML-like parser for policy files
        // In production, use SnakeYAML or a proper YAML parser
        List<Policy> policies = new ArrayList<>();

        if (content.contains("policies:")) {
            // Multi-policy format
            String[] sections = content.split("\\n  - name:");
            for (int i = 1; i < sections.length; i++) {
                String section = sections[i];
                String name = extractValue(section, "name");
                String description = extractValue(section, "description");
                String severity = extractValue(section, "severity");
                String status = extractValue(section, "status");
                String rule = extractMultilineValue(section, "rule");
                PolicyScope scope = parseScope(section);

                if (name != null && rule != null) {
                    Policy policy = policyValidator.validateAndParse(
                            name, description, severity, status, rule,
                            sourceId, scope);
                    policies.add(policy);
                }
            }
        } else {
            // Single-policy format
            String name = extractValue(content, "name");
            String description = extractValue(content, "description");
            String severity = extractValue(content, "severity");
            String status = extractValue(content, "status");
            String rule = extractMultilineValue(content, "rule");
            PolicyScope scope = parseScope(content);

            if (name != null && rule != null) {
                Policy policy = policyValidator.validateAndParse(
                        name, description, severity, status, rule,
                        sourceId, scope);
                policies.add(policy);
            }
        }

        return policies;
    }

    private PolicyScope parseScope(String yaml) {
        String pathPatternsStr = extractListValue(yaml, "pathPatterns");
        String operationsStr = extractListValue(yaml, "operations");
        String tagsStr = extractListValue(yaml, "tags");
        String excludePathsStr = extractListValue(yaml, "excludePaths");

        Set<String> pathPatterns = pathPatternsStr != null
                ? Set.of(pathPatternsStr.split("\\s*,\\s*")) : Set.of("/**");
        Set<PolicyScope.HttpOperation> operations = operationsStr != null
                ? Arrays.stream(operationsStr.split("\\s*,\\s*"))
                        .map(String::trim)
                        .map(PolicyScope.HttpOperation::valueOf)
                        .collect(Collectors.toSet())
                : Set.of();
        Set<String> tags = tagsStr != null
                ? Set.of(tagsStr.split("\\s*,\\s*")) : Set.of();
        Set<String> excludePaths = excludePathsStr != null
                ? Set.of(excludePathsStr.split("\\s*,\\s*")) : Set.of();

        return new PolicyScope(pathPatterns, operations, tags, excludePaths);
    }

    private String extractValue(String yaml, String key) {
        Pattern pattern = Pattern.compile(
                "^" + key + ":\\s*\"([^\"]+)\"\\s*$",
                Pattern.MULTILINE);
        var matcher = pattern.matcher(yaml);
        if (matcher.find()) return matcher.group(1);

        // Also try unquoted values
        Pattern unquoted = Pattern.compile(
                "^" + key + ":\\s*([^\\s#]+)",
                Pattern.MULTILINE);
        var m2 = unquoted.matcher(yaml);
        if (m2.find()) return m2.group(1);

        return null;
    }

    private String extractMultilineValue(String yaml, String key) {
        Pattern pattern = Pattern.compile(
                "^" + key + ":\\s*\\|\\s*$\\n((?:^\\s+.*\\n?)*)",
                Pattern.MULTILINE);
        var matcher = pattern.matcher(yaml);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\\n\\s+", "\n").trim();
        }
        return null;
    }

    private String extractListValue(String yaml, String key) {
        Pattern pattern = Pattern.compile(
                "^\\s+" + key + ":\\s*\\n((?:^\\s+-\\s+[^\\n]+\\n?)+)",
                Pattern.MULTILINE);
        var matcher = pattern.matcher(yaml);
        if (matcher.find()) {
            return Arrays.stream(matcher.group(1).split("\\n"))
                    .map(l -> l.replaceAll("^\\s*-\\s*", "").trim())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(","));
        }
        return null;
    }

    /**
     * Executes a shell command and returns the output.
     */
    private String executeCommand(String... command) throws IOException, InterruptedException {
        var processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        var process = processBuilder.start();
        String output;
        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Command failed: " + String.join(" ", command)
                    + "\nExit code: " + exitCode + "\n" + output);
        }
        return output;
    }

    /**
     * Executes a shell command in a specific directory.
     */
    private String executeCommandInDir(Path dir, String... command)
            throws IOException, InterruptedException {
        var processBuilder = new ProcessBuilder(command);
        processBuilder.directory(dir.toFile());
        processBuilder.redirectErrorStream(true);
        var process = processBuilder.start();
        String output;
        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Command failed in " + dir + ": " + String.join(" ", command)
                    + "\nExit code: " + exitCode + "\n" + output);
        }
        return output;
    }

    private String getHeadCommitSha(Path cloneDir) throws IOException, InterruptedException {
        return executeCommandInDir(cloneDir, "git", "rev-parse", "HEAD").trim();
    }
}
