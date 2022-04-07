package RCNTests;

public class Variables {
    void someMethod() {
        int myVar = 42;
        System.out.println(myVar); // use it, or it might be omitted by compiler
    }

    void sameVarTwice() {
        {
            int myVar = 42;
            System.out.println(myVar); // use it, or it might be omitted by compiler
        }
        {
            int myVar = 42;
            System.out.println(myVar); // use it, or it might be omitted by compiler
        }
    }

    public static void main(String[] args) {
    }
}