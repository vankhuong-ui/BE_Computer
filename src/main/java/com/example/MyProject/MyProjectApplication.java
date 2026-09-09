package com.example.MyProject;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class MyProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyProjectApplication.class, args);
        System.out.println("Run Success");
    }
    @Bean
    CommandLineRunner run() {
        return args -> {
            System.out.println("BCRYPT 123456: " + new BCryptPasswordEncoder().encode("123456"));
            PasswordEncoder encoder = new BCryptPasswordEncoder();

            System.out.println(
                    encoder.matches(
                            "123456",
                            "$2a$10$WYyeo5gjVmz8Dlw5VcAFseTPT2LG8SOCI.SWRsyG6dulRMK5.Adn2"
                    )
            );

        };
    }
}
