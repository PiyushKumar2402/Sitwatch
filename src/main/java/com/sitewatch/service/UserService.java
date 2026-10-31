package com.sitewatch.service;

import com.sitewatch.dto.RegisterForm;
import com.sitewatch.model.AppUser;
import com.sitewatch.repo.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public AppUser register(RegisterForm form) {
        String username = form.getUsername().trim();
        if (users.existsByUsername(username)) throw new IllegalArgumentException("That username is already taken.");
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setEmail(form.getEmail() == null || form.getEmail().isBlank() ? null : form.getEmail().trim());
        u.setPasswordHash(encoder.encode(form.getPassword()));
        return users.save(u);
    }

    public AppUser require(String username) {
        return users.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser u = users.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));
        return new User(u.getUsername(), u.getPasswordHash(), List.of());
    }
}
