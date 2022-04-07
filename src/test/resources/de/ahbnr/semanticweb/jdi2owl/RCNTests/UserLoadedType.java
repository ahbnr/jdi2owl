package RCNTests;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

class UserClassLoader extends ClassLoader {
    @Override
    public Class findClass(String name) throws ClassNotFoundException {
        try {
            var filePath = Path.of(getClass().getClassLoader().getResource(name.replace('.', File.separatorChar) + ".class").getPath());
            var bytes = Files.readAllBytes(filePath);
            return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(e.getMessage());
        }
    }
}

public class UserLoadedType {
    public void helloWorld() {
        System.out.println("Hello World!");
    }

    public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException {
        var loader = new UserClassLoader();
        var loadedClazz = loader.findClass("RCNTests.UserLoadedType");
        // Force creation of type
        var x = loadedClazz.newInstance();
        loadedClazz.getMethod("helloWorld").invoke(x);
    }
}