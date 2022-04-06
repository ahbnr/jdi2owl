package RCNTests;

class MyClass {
    public static MyClass x = new MyClass() { };
}

public class AnonymousType {
    public static void main(String[] args) {
        // force loading of the types
        System.out.println(MyClass.x.toString());
    }
}