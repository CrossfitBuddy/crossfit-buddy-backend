package ru.nsu.crossfitbuddy.auth.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import ru.nsu.crossfitbuddy.core.user.domain.UserAccount;

import java.util.Collection;
import java.util.List;

public final class AccountDetails implements UserDetails {
    private final UserAccount account;

    public AccountDetails(UserAccount account) {
        this.account = account;
    }

    public UserAccount account() {
        return account;
    }

    @Override
    public String getUsername() {
        return account.getEmail();
    }

    @Override
    public String getPassword() {
        return account.getPasswordHash();
    }

    @Override
    public boolean isEnabled() {
        return account.isEnabled() && !account.isDeleted();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }
}
