package RCNTests;

class MyClass {
    interface MemberInterface {}

    class MemberClass implements MemberInterface {}

    static class StaticMemberClass {}
}

public class MemberType {
    public static void main(String[] args) {
        // force loading of the types
        (new MyClass()).new MemberClass();

        new MyClass.StaticMemberClass();
    }
}