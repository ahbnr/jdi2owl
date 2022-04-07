package RCNTests;

class MyClass {
    void fun() {
        class LocalClass { }

        new LocalClass();
    }
}

public class LocalType {
    public static void main(String[] args) {
        // force loading of the types
        (new MyClass()).fun();
    }
}