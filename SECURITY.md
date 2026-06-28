# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| latest  | ✅ Yes    |

## Reporting a Vulnerability

We take the security of Keystone seriously. If you believe you have found a security vulnerability, please **do not** open a public issue.

Instead, report it privately by emailing the maintainers or creating a private security advisory on GitHub:

1. Go to https://github.com/arman-jalili/Keystone/security/advisories
2. Click **"New draft security advisory"**
3. Fill in the details — we'll respond within 48 hours

### What to Include

- Description of the vulnerability
- Steps to reproduce (proof of concept)
- Affected versions
- Potential impact
- Suggested fix (if known)

### Response Timeline

- **Acknowledgment:** Within 48 hours
- **Triage:** Within 5 business days
- **Fix:** Dependent on severity (critical: 7 days, high: 14 days, medium: 30 days)
- **Disclosure:** After a fix is released

## Security Practices

- **Authentication:** API keys and tokens should never be committed to the repository
- **Input validation:** All API inputs are validated via Jakarta Validation annotations
- **Webhook security:** GitHub webhooks are verified via HMAC-SHA256 signatures
- **Dependencies:** Regularly updated via Dependabot
- **Secrets:** Use environment variables, never hardcoded values
