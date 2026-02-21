package tao.coding.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// F**K checked Exception
public class CheckedExceptionFucker {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static ObjectNode createObjectNode() {
        return OBJECT_MAPPER.createObjectNode();
    }

    public static <T> T readValue(String jsonStr, Class<T> cls) {
        try {
            return OBJECT_MAPPER.readValue(jsonStr, cls);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode readTree(String jsonStr) {
        try {
            return OBJECT_MAPPER.readTree(jsonStr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static <T> T treeToValue(ObjectNode objectNode, Class<T> cls) {
        try {
            return OBJECT_MAPPER.treeToValue(objectNode, cls);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
