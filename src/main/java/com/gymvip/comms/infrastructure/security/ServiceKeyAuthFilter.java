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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro de seguridad para autenticar comunicaciones internas entre servicios.
 * Exige que el header X-Service-Key coincida con el token compartido COMMS_SERVICE_TOKEN.
 * Rutas públicas excluidas: GET/HEAD /, /api/v1/health, /health, /actuator/** y /api/v1/webhooks/**.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ServiceKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ServiceKeyAuthFilter.class);
    private static final long UNAUTH_LOG_THROTTLE_MS = 60_000L;

    private final ConcurrentHashMap<String, Long> lastWarnByIpAndPath = new ConcurrentHashMap<>();
    private final String expectedServiceToken;
    private final String commsMode;

    public ServiceKeyAuthFilter(
            @Value("${comms.service-token:gymvip-internal-token-2026}") String expectedServiceToken,
            @Value("${comms.mode:simulation}") String commsMode
    ) {
        this.expectedServiceToken = expectedServiceToken;
        this.commsMode = commsMode;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        if ("/".equals(path) && ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method))) {
            return true;
        }

        return path.equals("/health") || path.startsWith("/health/") ||
               path.startsWith("/api/v1/health") ||
               path.equals("/actuator/health") || path.startsWith("/actuator/health/") ||
               path.equals("/actuator/info") || path.startsWith("/actuator/info/") ||
               path.startsWith("/api/v1/webhooks");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if ("production".equalsIgnoreCase(commsMode) &&
                (expectedServiceToken == null || expectedServiceToken.isBlank() || "gymvip-internal-token-2026".equals(expectedServiceToken))) {
            log.error("[COMMS SECURITY CRITICAL] Intento de operación en producción con COMMS_SERVICE_TOKEN por defecto o ausente.");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                    {"status":500,"error":"Insecure Configuration","message":"Configuración insegura en producción: configure un COMMS_SERVICE_TOKEN externo."}
                    """.trim());
            return;
        }

        String key = request.getHeader("X-Service-Key");

        if (key != null && key.equals(expectedServiceToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        String throttleKey = request.getRemoteAddr() + "|" + request.getRequestURI();
        Long previous = lastWarnByIpAndPath.putIfAbsent(throttleKey, now);
        if (previous == null || (now - previous) >= UNAUTH_LOG_THROTTLE_MS) {
            lastWarnByIpAndPath.put(throttleKey, now);
            if (lastWarnByIpAndPath.size() > 512) {
                lastWarnByIpAndPath.clear();
            }
            log.warn("[COMMS SECURITY] Intento de acceso no autorizado a {} desde IP {} (header X-Service-Key ausente o no coincide)",
                    request.getRequestURI(), request.getRemoteAddr());
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
                {"status":401,"error":"Unauthorized","message":"Acceso interno no autorizado: header X-Service-Key inv\u00E1lido o ausente."}
                """.trim());
    }
}
