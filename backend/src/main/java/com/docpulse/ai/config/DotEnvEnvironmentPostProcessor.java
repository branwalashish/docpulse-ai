package com.docpulse.ai.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.Ordered;

public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        File[] candidates = new File[] { 
            new File(".env"), 
            new File("../.env"), 
            new File(System.getProperty("user.dir"), ".env"),
            new File(System.getProperty("user.dir"), "../.env")
        };

        for (File file : candidates) {
            if (file.exists() && file.isFile()) {
                System.out.println("[DotEnv] Found .env file at: " + file.getAbsolutePath());
                Map<String, Object> props = new HashMap<>();
                try {
                    List<String> lines = Files.readAllLines(file.toPath());
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                            continue;
                        }
                        int eqIndex = trimmed.indexOf('=');
                        if (eqIndex > 0) {
                            String key = trimmed.substring(0, eqIndex).trim();
                            String value = trimmed.substring(eqIndex + 1).trim();
                            props.put(key, value);

                            if (System.getProperty(key) == null) {
                                System.setProperty(key, value);
                            }

                            // Normalize uppercase environment keys to Spring property format
                            String dotKey = key.toLowerCase().replace('_', '.');
                            props.put(dotKey, value);

                            // Specifically map datasource keys if provided
                            if ("SPRING_DATASOURCE_URL".equalsIgnoreCase(key)) {
                                props.put("spring.datasource.url", value);
                                System.setProperty("spring.datasource.url", value);
                            } else if ("SPRING_DATASOURCE_USERNAME".equalsIgnoreCase(key) || "POSTGRES_USER".equalsIgnoreCase(key)) {
                                props.put("spring.datasource.username", value);
                                System.setProperty("spring.datasource.username", value);
                            } else if ("SPRING_DATASOURCE_PASSWORD".equalsIgnoreCase(key) || "POSTGRES_PASSWORD".equalsIgnoreCase(key)) {
                                props.put("spring.datasource.password", value);
                                System.setProperty("spring.datasource.password", value);
                            }
                        }
                    }
                    System.out.println("[DotEnv] Loaded " + props.size() + " properties from .env");
                } catch (IOException e) {
                    System.err.println("[DotEnv] Error reading .env: " + e.getMessage());
                }

                if (!props.isEmpty()) {
                    environment.getPropertySources().addFirst(new MapPropertySource("dotenv", props));
                }
                break;
            }
        }
    }
}
