import com.keystone.policy.dsl.*;
public class TestParser {
    public static void main(String[] args) throws Exception {
        DslParser p = new DslParser();
        // Print to stderr so we can see it
        try {
            DslExpression expr = p.parse("none field in spec.schemas where field.is_deprecated yield violation(\"Deprecated\")");
            System.err.println("PARSED OK: " + expr);
        } catch (Exception e) {
            System.err.println("PARSE ERROR: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
