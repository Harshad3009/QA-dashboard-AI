package com.harshqa.qadashboardai.security;

import com.harshqa.qadashboardai.entity.ApiKey;
import com.harshqa.qadashboardai.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyAuthenticationFilter(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String headerKey = request.getHeader("X-API-KEY");

        if (headerKey != null && !headerKey.isEmpty()) {
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findBySecretKey(headerKey);

            if (apiKeyOpt.isPresent()) {
                ApiKey apiKey = apiKeyOpt.get();

                // Track usage
                apiKey.setLastUsedAt(java.time.LocalDateTime.now());
                apiKeyRepository.save(apiKey);

                // Create a special "Principal" that contains the Project ID
                // The username is the API Key Name, Role is "SYSTEM"
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        apiKey.getName(),
                        apiKey.getProject().getId(), // Storing Project ID as "Credentials" for easy access
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_SYSTEM"))
                );

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}