package RCNTests;

public class Constructors {
    static class StaticMemberClass {
        StaticMemberClass() { }

        StaticMemberClass(StaticMemberClass x, StaticMemberClass y) { }
    }

    public static void main(String[] args) {
        // force loading of the types
        new StaticMemberClass();
    }
}