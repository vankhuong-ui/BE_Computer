package com.example.MyProject.AUTH.security.custom;

import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.AUTH.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService
        extends DefaultOAuth2UserService {

    private final AccountRepository accountRepository;

    @Override
    public OAuth2User loadUser(
            OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        OAuth2User oauth2User =
                super.loadUser(userRequest);

        Map<String, Object> attributes =
                oauth2User.getAttributes();

        String googleId =
                (String) attributes.get("sub");

        String email =
                (String) attributes.get("email");

        String name =
                (String) attributes.get("name");

        if (googleId == null || email == null) {
            throw new OAuth2AuthenticationException(
                    "Không lấy được thông tin Google"
            );
        }

        // ==============================
        // TÌM ACCOUNT
        // ==============================

        Optional<Account> accountOpt =
                accountRepository.findByGoogleid(googleId);

        if (accountOpt.isEmpty()) {
            accountOpt =
                    accountRepository.findBygmail(email);
        }

        Account account;

        // ==============================
        // ACCOUNT ĐÃ TỒN TẠI
        // ==============================

        if (accountOpt.isPresent()) {

            account = accountOpt.get();

            // Nếu account trước đây login bằng gmail
            // thì liên kết Google ID vào account
            if (account.getGoogleid() == null
                    || account.getGoogleid().isBlank()) {

                account.setGoogleid(googleId);

                accountRepository.save(account);
            }

        }

        // ==============================
        // ACCOUNT MỚI
        // ==============================

        else {

            account = new Account();

            account.setGmail(email);

            account.setAccountname(
                    name != null
                            ? name
                            : email
            );

            account.setGoogleid(googleId);

            account.setRole("USER");

            account.setEnabled(true);

            // Password để null vì account
            // đăng ký bằng Google
            account.setPassword(null);

            account =
                    accountRepository.save(account);
        }

        // ==============================
        // AUTHORITY
        // ==============================

        String role =
                account.getRole() != null
                        ? account.getRole()
                        : "USER";

        Collection<GrantedAuthority> authorities =
                Arrays.stream(role.split(","))
                        .map(String::trim)
                        .map(r ->
                                r.startsWith("ROLE_")
                                        ? r
                                        : "ROLE_" + r
                        )
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // ==============================
        // RETURN OAUTH USER
        // ==============================

        return new DefaultOAuth2User(
                authorities,
                attributes,
                "sub"
        );
    }
}