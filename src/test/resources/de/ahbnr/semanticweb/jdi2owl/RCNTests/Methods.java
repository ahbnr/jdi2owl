package RCNTests;

public class Methods {
    static class NotLoaded { }

    static class StaticMemberClass {
        void someMethod() { }

        StaticMemberClass complexMethod(StaticMemberClass x, StaticMemberClass y) { return null; }
    }

    void someMethod() { }
    NotLoaded notLoadedTypesMethod(NotLoaded x) { return null; }

    public static void main(String[] args) {
        // force loading of the types
        new StaticMemberClass();
    }
}