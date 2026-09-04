package io.github.thetaoofcoding.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// F**K checked Exception
public class CheckedExceptionFucker {
    public static void filesDeleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static long filesSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
