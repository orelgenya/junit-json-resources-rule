package samples;

import java.util.List;

import io.github.junit.json.JsonResource;
import io.github.junit.json.JsonResourcesRule;
import lombok.Data;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class SampleTest {

    @Rule
    public JsonResourcesRule jsonRule = new JsonResourcesRule().folder("fixtures");

//    @Rule
//    public MockitoRule rule = MockitoJUnit.rule();

    @JsonResource
    private List<SimpleData> simpleData;

    @JsonResource("simple-data.json")
    private List<SimpleData> simpleDataAgain;

    @JsonResource("data.txt")
    private String data;

    @Test
    public void test() {
        assertThat(simpleData, is(notNullValue()));
        assertThat(simpleDataAgain, is(notNullValue()));
        assertThat(data, is(notNullValue()));
    }

    @Test
    public void test2() {
        assertThat(simpleData, is(notNullValue()));
        assertThat(simpleDataAgain, is(notNullValue()));
        assertThat(data, is(notNullValue()));
    }

    @Data
    public static class SimpleData {
        Long id;
        String name;
    }
}
