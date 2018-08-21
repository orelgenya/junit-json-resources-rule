package io.github.junit.json;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.util.ReflectionUtils;

import static org.springframework.util.StringUtils.isEmpty;

@Slf4j
public class JsonResourcesRule implements TestRule {

    private Map<String, Object> cache = new ConcurrentHashMap<>();
    private String folder;

    public JsonResourcesRule folder(String folder) {
        this.folder = folder;
        return this;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Object test = getTestInstance(base);

                if (test != null) {

                    ReflectionUtils.doWithFields(
                            description.getTestClass(),
                            field -> initResource(test, field),
                            field -> field.isAnnotationPresent(JsonResource.class));

                }

                base.evaluate();
            }
        };
    }

    private void initResource(Object test, Field resourceField) {
        String resourcePath = getResourcePath(resourceField);

        Object value = readData(resourceField, resourcePath);

        resourceField.setAccessible(true);
        ReflectionUtils.setField(resourceField, test, value);
    }

    private Object readData(Field resourceField, String resourcePath) {
        String genericType = resourceField.getGenericType()
                .toString();

        if (genericType.contains("<")) {
            JavaType javaType = TypeFactory.defaultInstance()
                    .constructFromCanonical(genericType);
            return cache.computeIfAbsent(resourcePath,
                    path -> ResourcesAwareTestUtils.readJson(path, javaType));
        }

        return cache.computeIfAbsent(resourcePath,
                !resourcePath.endsWith(".json") ? ResourcesAwareTestUtils::readAsString :
                        path -> ResourcesAwareTestUtils.readJson(path, resourceField.getType()));
    }

    private Object getTestInstance(Statement base) {
        Field testInstanceField = findTestInstanceField(base.getClass());
        if (testInstanceField == null) {
            Field fieldNextInBase = ReflectionUtils.findField(base.getClass(), "next");
            if (fieldNextInBase != null) {
                Statement nextBase = (Statement) getField(fieldNextInBase, base);
                return getTestInstance(nextBase);
            }
            log.warn("Test instance wasn't found.");
            return null;
        }
        return getField(testInstanceField, base);
    }

    private Object getField(Field field, Object target) {
        field.setAccessible(true);
        return ReflectionUtils.getField(field, target);
    }

    private Field findTestInstanceField(Class clazz) {
        Field testField = ReflectionUtils.findField(clazz, "target");
        if (testField == null) {
            testField = ReflectionUtils.findField(clazz, "testInstance");
        }
        return testField;
    }

    private String getResourcePath(Field resourceField) {
        JsonResource annotation = resourceField.getAnnotation(JsonResource.class);
        String resourceName = !isEmpty(annotation.value()) ?
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
