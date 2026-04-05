package com.pawcarehub.backend.security;

import com.pawcarehub.backend.entity.User;
import com.pawcarehub.backend.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

    private static final String USER_EMAIL_HEADER = "X-User-Email";
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
        HttpServletRequest requestToUse = request;

        try {
            String bearerToken = extractBearerToken(request);
            String authenticatedEmail = null;

            if (StringUtils.hasText(bearerToken)) {
                String tokenEmail = jwtService.extractEmail(bearerToken);
                User user = authService.getAuthenticatedUserEntity(tokenEmail);

                if (!jwtService.isTokenValid(bearerToken, user)) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
                }

                setAuthentication(user, request);
                authenticatedEmail = user.getEmail();
            } else if (StringUtils.hasText(request.getHeader(USER_EMAIL_HEADER))
                && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = authService.getAuthenticatedUserEntity(request.getHeader(USER_EMAIL_HEADER));
                setAuthentication(user, request);
                authenticatedEmail = user.getEmail();
            }

            if (StringUtils.hasText(authenticatedEmail)) {
                requestToUse = new HeaderOverrideRequestWrapper(request, USER_EMAIL_HEADER, authenticatedEmail);
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

        filterChain.doFilter(requestToUse, response);
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

    private static final class HeaderOverrideRequestWrapper extends HttpServletRequestWrapper {

        private final String headerName;
        private final String headerValue;

        private HeaderOverrideRequestWrapper(HttpServletRequest request, String headerName, String headerValue) {
            super(request);
            this.headerName = headerName;
            this.headerValue = headerValue;
        }

        @Override
        public String getHeader(String name) {
            if (headerName.equalsIgnoreCase(name)) {
                return headerValue;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (headerName.equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(headerValue));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> headerNames = new LinkedHashSet<>();
            Enumeration<String> existingHeaderNames = super.getHeaderNames();
            while (existingHeaderNames.hasMoreElements()) {
                headerNames.add(existingHeaderNames.nextElement());
            }
            headerNames.add(headerName);
            return Collections.enumeration(new ArrayList<>(headerNames));
        }
    }
}
