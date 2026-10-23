package ru.nsu.crossfitbuddy.core.service;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.nsu.crossfitbuddy.core.entity.AccountDetails;
import ru.nsu.crossfitbuddy.core.repository.UserAccountRepository;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AccountDetailsService implements UserDetailsService {
    private final UserAccountRepository userAccountRepository;

    @Override
    @Transactional(readOnly = true)
    public AccountDetails loadUserByUsername(String email) {
        return userAccountRepository.findByEmail(normalizeEmail(email))
            .map(AccountDetails::new)
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }

    public static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
