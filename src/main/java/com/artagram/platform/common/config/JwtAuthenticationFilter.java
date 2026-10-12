package com.artagram.platform.common.config;

import com.artagram.platform.auth.JwtService;
import com.artagram.platform.common.tracing.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Optional<JwtService.TokenPrincipal> principal = jwtService.validar(header.substring(7));
            principal.ifPresent(p -> {
                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + p.rolGlobal()));
                var auth = new UsernamePasswordAuthenticationToken(p, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
                MDC.put(TraceContext.USER_ID_MDC_KEY, p.usuarioId().toString());
            });
        }
        filterChain.doFilter(request, response);
    }
}
