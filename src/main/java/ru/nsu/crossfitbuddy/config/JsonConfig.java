package ru.nsu.crossfitbuddy.config;

import com.fasterxml.jackson.core.StreamReadConstraints;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonConfig {
    @Bean
    Jackson2ObjectMapperBuilderCustomizer jsonReadConstraints() {
        return builder -> builder
            .postConfigurer(mapper -> mapper
                .getFactory()
                .setStreamReadConstraints(StreamReadConstraints
                    .builder()
                    .maxNestingDepth(20)
                    .maxStringLength(8192)
                    .maxDocumentLength(65536)
                    .build()));
    }
}
