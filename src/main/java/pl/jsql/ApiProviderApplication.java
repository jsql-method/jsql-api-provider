package pl.jsql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApiProviderApplication {

    public static final boolean isLocalVersion = false;

    public static void main(String[] args) {
        SpringApplication.run(ApiProviderApplication.class, args);
    }
}
