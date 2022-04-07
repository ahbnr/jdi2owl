package RCNTests;

interface TopLevelInterface { }

class TopLevelClass implements TopLevelInterface { }

public class TopLevelType {
    public static void main(String[] args) {
        // force loading of top level types
        new TopLevelClass();
    }
}