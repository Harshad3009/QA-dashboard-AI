package com.harshqa.qadashboardai.controller;

import com.harshqa.qadashboardai.entity.Role;
import com.harshqa.qadashboardai.entity.User;
import com.harshqa.qadashboardai.repository.UserRepository;
import com.harshqa.qadashboardai.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        User user = userRepository.findByUsername(username)
                .orElse(null);

        if(user == null) {
            return ResponseEntity.badRequest().body("User not found. Please register first to access the QA Dashboard AI.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.badRequest().body("Wrong password! Try again.");
        }

        String token = jwtUtils.generateToken(user.getUsername(), user.getRole().name());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", user.getUsername(),
                "role", user.getRole().name()
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        if (userRepository.findByUsername(request.get("username")).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        User user = new User(
                request.get("username"),
                passwordEncoder.encode(request.get("password")),
                Role.valueOf(request.getOrDefault("role", "ENGINEER"))
        );

        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }
}