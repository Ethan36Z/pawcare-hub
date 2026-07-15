package com.pawcarehub.backend.security;

import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtService jwtService;
    private final AuthService authService;

    public JwtAuthenticationFilter(JwtService jwtService, AuthService authService) {
        this.jwtService = jwtService;
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String bearerToken = extractBearerToken(request);

            if (StringUtils.hasText(bearerToken)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
                String tokenEmail = jwtService.extractEmail(bearerToken);
                User user = authService.getAuthenticatedUserEntity(tokenEmail);

                if (!jwtService.isTokenValid(bearerToken, user)) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
                }

                setAuthentication(user, request);
            }
        } catch (ResponseStatusException exception) {
            SecurityContextHolder.clearContext();
            response.sendError(exception.getStatusCode().value(), exception.getReason());
            return;
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or expired token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(User user, HttpServletRequest request) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user.getEmail(),
            null,
            toAuthorities(user.getRole())
        );
        ((UsernamePasswordAuthenticationToken) authentication)
            .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Collection<? extends GrantedAuthority> toAuthorities(String role) {
        String normalizedRole = role == null ? "user" : role.trim().toUpperCase(Locale.ROOT);
        return List.of(new SimpleGrantedAuthority("ROLE_" + normalizedRole));
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorizationHeader.substring(BEARER_PREFIX.length()).trim();
    }
}
