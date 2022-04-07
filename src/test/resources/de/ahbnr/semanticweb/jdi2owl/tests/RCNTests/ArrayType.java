package RCNTests;

class MyClass {
    static class StaticMemberClass {}
}

public class ArrayType {
    public static void main(String[] args) {
        // force loading of the types
        var x = new MyClass.StaticMemberClass[] { new MyClass.StaticMemberClass() };
    }
}