package com.transporter.assignment.config;

import com.transporter.assignment.model.TransporterInput;
import com.transporter.assignment.service.TransporterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;

@Component
public class SampleDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataLoader.class);

    private final TransporterService transporterService;
    private final JsonMapper jsonMapper;
    private final Resource sample;

    public SampleDataLoader(TransporterService transporterService, JsonMapper jsonMapper,
                            @Value("classpath:samples/test-case-1.json") Resource sample) {
        this.transporterService = transporterService;
        this.jsonMapper = jsonMapper;
        this.sample = sample;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        try (InputStream in = sample.getInputStream()) {
            transporterService.saveInput(jsonMapper.readValue(in, TransporterInput.class));
        }
        log.info("Preloaded sample input from {}", sample.getFilename());
    }
}
