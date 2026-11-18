package com.transporter.assignment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.MediaType;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String INPUT_PATH = "/api/v1/transporters/input";
    private static final String JSON = "application/json";

    private static final List<Sample> INPUT_SAMPLES = List.of(
            new Sample("testCase1", "Test case 1: 5 lanes, 7 transporters quoting every lane", "samples/test-case-1.json"),
            new Sample("testCase2", "Test case 2: 5 lanes, 4 transporters with partial quotes", "samples/test-case-2.json"),
            new Sample("specExample", "API spec example: 10 lanes, each transporter quotes two lanes", "samples/spec-example.json"));

    @Bean
    OpenAPI transporterAssignmentApi() {
        return new OpenAPI().info(new Info()
                .title("Transporter Assignment API")
                .version("v1")
                .description("Submit lanes with transporter quotes, then ask for the cheapest plan that covers every "
                        + "lane using at most N transporters. Test case 1 is preloaded on startup, so the assignment "
                        + "endpoint works straight away."));
    }

    @Bean
    OpenApiCustomizer inputSamples(JsonMapper jsonMapper) {
        return openApi -> {
            MediaType inputBody = openApi.getPaths().get(INPUT_PATH).getPost().getRequestBody().getContent().get(JSON);
            INPUT_SAMPLES.forEach(sample -> inputBody.addExamples(sample.name(),
                    new Example().summary(sample.summary()).value(readJson(jsonMapper, sample.path()))));
        };
    }

    private static Object readJson(JsonMapper jsonMapper, String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return jsonMapper.readValue(in, Object.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record Sample(String name, String summary, String path) {
    }
}
