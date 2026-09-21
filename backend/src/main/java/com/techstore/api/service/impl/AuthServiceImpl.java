package com.techstore.api.service.impl;

import com.techstore.api.dto.auth.AuthResponse;
import com.techstore.api.dto.auth.LoginRequest;
import com.techstore.api.dto.auth.RegisterRequest;
import com.techstore.api.entity.Role;
import com.techstore.api.entity.User;
import com.techstore.api.exception.ConflictException;
import com.techstore.api.repository.UserRepository;
import com.techstore.api.security.JwtService;
import com.techstore.api.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager, UserDetailsService userDetailsService,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("E-mail já cadastrado.");
        }

        User user = new User(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                Role.ROLE_USER);
        user = userRepository.save(user);
        UserDetails details = userDetailsService.loadUserByUsername(user.getEmail());
        return toResponse(user, jwtService.generateToken(details));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));
        UserDetails details = userDetailsService.loadUserByUsername(user.getEmail());
        return toResponse(user, jwtService.generateToken(details));
    }

    private AuthResponse toResponse(User user, String token) {
        return new AuthResponse(token, "Bearer", user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }
}
