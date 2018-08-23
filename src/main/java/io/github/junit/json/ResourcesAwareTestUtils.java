package io.github.junit.json;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import static java.lang.ClassLoader.getSystemResource;
import static java.lang.String.format;

public class ResourcesAwareTestUtils {

    private static final String FAILED_READING_MSG = "Failed reading '%s'!";
    private static ObjectMapper objectMapper = new ObjectMapper();

    private ResourcesAwareTestUtils() {
    }

    public static <T> T readJson(String filepath, Class<T> clazz) {
        try {
            return objectMapper.readValue(getSystemResourceURL(filepath), clazz);
        } catch (IOException e) {
            throw new IllegalStateException(format(FAILED_READING_MSG, filepath), e);
        }
    }

    public static <T> T readJson(String filepath, JavaType type) {
        try {
            return objectMapper.readValue(getSystemResourceURL(filepath), type);
        } catch (IOException e) {
            throw new IllegalStateException(format(FAILED_READING_MSG, filepath), e);
        }
    }

    public static String readAsString(String filepath) {
        try {
            URI uri = getSystemResourceURL(filepath).toURI();
            return new String(Files.readAllBytes(Paths.get(uri)));
        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException(format(FAILED_READING_MSG, filepath), e);
        }
    }

    public static String toJsonString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new IllegalStateException("Failed converting to json string!", e);
        }
    }

    private static URL getSystemResourceURL(String path) {
        URL systemResource = getSystemResource(path);
        if (systemResource == null) {
            throw new IllegalStateException(format("Resource '%s' not found!", path));
        }
        return systemResource;
    }
}