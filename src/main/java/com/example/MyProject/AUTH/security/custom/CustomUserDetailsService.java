package com.example.MyProject.AUTH.security.custom;

import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.AUTH.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String gmail) throws UsernameNotFoundException {
        Optional<Account> account = accountRepository.findBygmail(gmail);
        if (account.isPresent()) {
            // Không cần kiểm tra password null vì đây là xác thực qua JWT
            // Nếu cần kiểm tra password chỉ dành cho login thường, hãy làm ở nơi khác
            return account.get();
        } else {
            throw new UsernameNotFoundException("Tài khoản không tồn tại");
        }
    }
}