package io.github.junit.json;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import static io.github.junit.json.ResourcesAwareTestUtils.readJson;

@Slf4j
public class JsonResourcesRule implements MethodRule {

    private static Map<String, Object> cache = new ConcurrentHashMap<>();
    private String folder;

    public JsonResourcesRule folder(String folder) {
        this.folder = folder;
        return this;
    }

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Class<?> classContext = method.getDeclaringClass();
                while (classContext != Object.class) {
                    process(classContext, target);
                    classContext = classContext.getSuperclass();
                }

                base.evaluate();
            }
        };
    }

    private void process(Class<?> classContext, Object target) {
        Field[] fields = classContext.getDeclaredFields();
        for (Field field : fields) {
            JsonResource[] jsonResources = field.getAnnotationsByType(JsonResource.class);
            if (jsonResources.length == 1) {
                initResource(target, field, jsonResources[0]);
            }
        }
    }

    private void initResource(Object test, Field field, JsonResource annotation) {
        String resourcePath = getResourcePath(annotation, field);
        setFieldValue(test, field, readData(field, resourcePath));
    }

    private void setFieldValue(Object test, Field field, Object value) {
        boolean wasAccessible = field.isAccessible();
        field.setAccessible(true);
        try {
            field.set(test, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Couldn't set resource field value!", e);
        } finally {
            field.setAccessible(wasAccessible);
        }
    }

    private Object readData(Field resourceField, String resourcePath) {
        String genericType = resourceField.getGenericType().toString();

        if (genericType.contains("<")) {
            JavaType javaType = TypeFactory.defaultInstance().constructFromCanonical(genericType);
            return cache.computeIfAbsent(resourcePath, path -> readJson(path, javaType));
        }

        return cache.computeIfAbsent(resourcePath,
                !resourcePath.endsWith(".json") ?
                        ResourcesAwareTestUtils::readAsString :
                        path -> readJson(path, resourceField.getType()));
    }

    private String getResourcePath(JsonResource annotation, Field resourceField) {
        String resourceName = !annotation.value().isEmpty() ?
                annotation.value() : toDashedCase(resourceField.getName());

        return folder == null ? resourceName : folder + "/" + resourceName;
    }

    private String toDashedCase(String camelCase) {
        Matcher matcher = Pattern.compile("(?<=[a-z0-9])[A-Z]").matcher(camelCase);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "-" + matcher.group().toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString() + ".json";
    }
}
