package com.gymvip.comms.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de seguridad para autenticar comunicaciones internas entre servicios.
 * Exige que el header X-Service-Key coincida con el token compartido COMMS_SERVICE_TOKEN.
 * Rutas públicas excluidas: /api/v1/health, /actuator/** y /api/v1/webhooks/**.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ServiceKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ServiceKeyAuthFilter.class);

    private final String expectedServiceToken;

    public ServiceKeyAuthFilter(
            @Value("${comms.service-token:gymvip-internal-token-2026}") String expectedServiceToken
    ) {
        this.expectedServiceToken = expectedServiceToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        return path.startsWith("/api/v1/health") ||
               path.startsWith("/actuator") ||
               path.startsWith("/api/v1/webhooks");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String key = request.getHeader("X-Service-Key");

        if (key != null && key.equals(expectedServiceToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("[COMMS SECURITY] Intento de acceso no autorizado a {} desde IP {} (header X-Service-Key ausente o no coincide)",
                request.getRequestURI(), request.getRemoteAddr());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
                {"status":401,"error":"Unauthorized","message":"Acceso interno no autorizado: header X-Service-Key inv\u00E1lido o ausente."}
                """.trim());
    }
}
