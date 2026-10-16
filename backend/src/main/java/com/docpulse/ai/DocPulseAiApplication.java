package com.docpulse.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@SpringBootApplication
public class DocPulseAiApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(DocPulseAiApplication.class, args);
    }

    private static void loadDotEnv() {
        File[] candidates = new File[] { new File(".env"), new File("../.env") };
        for (File file : candidates) {
            if (file.exists() && file.isFile()) {
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
                            if (System.getProperty(key) == null && System.getenv(key) == null) {
                                System.setProperty(key, value);
                            }
                        }
                    }
                } catch (IOException ignored) {
                }
                break;
            }
        }
    }
}

