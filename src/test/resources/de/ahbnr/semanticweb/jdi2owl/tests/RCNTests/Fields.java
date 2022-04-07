package RCNTests;

public class Fields {
    static class StaticMemberClass {
        static int staticField = 42;
    }

    int instanceField = 1337;

    public static void main(String[] args) {
        // force loading of the types
        new StaticMemberClass();
    }
}