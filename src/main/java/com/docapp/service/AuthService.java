package com.docapp.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.docapp.dto.AuthDtos.AuthResponse;
import com.docapp.dto.AuthDtos.LoginRequest;
import com.docapp.dto.AuthDtos.RegisterRequest;
import com.docapp.dto.AuthDtos.UserInfo;
import com.docapp.exception.ApiException;
import com.docapp.model.Role;
import com.docapp.model.User;
import com.docapp.repository.UserRepository;
import com.docapp.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "This email is already registered");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(Role.PATIENT);
        userRepository.save(user);

        return new AuthResponse(jwtService.generateToken(user), toInfo(user));
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return new AuthResponse(jwtService.generateToken(user), toInfo(user));
    }

    public static UserInfo toInfo(User user) {
        return new UserInfo(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name());
    }
}
