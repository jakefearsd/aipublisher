package com.jakefear.aipublisher;

import com.jakefear.aipublisher.cli.AiPublisherCommand;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

@SpringBootApplication
public class AiPublisherApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(AiPublisherApplication.class, args)));
    }

    @Bean
    public CommandLineRunner commandLineRunner(AiPublisherCommand command, IFactory factory) {
        return args -> {
            exitCode = new CommandLine(command, factory).execute(args);
        };
    }

    @Bean
    public ExitCodeGenerator exitCodeGenerator() {
        return () -> exitCode;
    }

    private int exitCode;
}
