package com.example.MyProject;

import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.AUTH.repository.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class MyProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyProjectApplication.class, args);
        System.out.println("Run Success");
    }

    @Bean
    CommandLineRunner seedDefaultAccounts(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            seedAccountIfAbsent(
                    accountRepository,
                    passwordEncoder,
                    "admin@hkcomputer.vn",
                    "Quản trị viên HK",
                    "Admin@123",
                    "ADMIN"
            );
            seedAccountIfAbsent(
                    accountRepository,
                    passwordEncoder,
                    "khachhang@hkcomputer.vn",
                    "Khách hàng mẫu",
                    "User@123",
                    "USER"
            );
        };
    }

    private void seedAccountIfAbsent(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            String gmail,
            String accountName,
            String rawPassword,
            String role
    ) {
        if (accountRepository.existsBygmail(gmail)) {
            return;
        }
        Account account = new Account();
        account.setGmail(gmail);
        account.setAccountname(accountName);
        account.setPassword(passwordEncoder.encode(rawPassword));
        account.setEnabled(true);
        account.setRole(role);
        accountRepository.save(account);
    }
}
