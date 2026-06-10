package com.alhilal.gateway;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class JwtAuthFilter implements GlobalFilter, Ordered {

    // MUST match JwtUtils.SECRET in customer-service
    private static final String SECRET =
            "al-hilal-bank-super-secret-key-must-be-256-bits-long!!";

    private final SecretKey key = Keys.hmacShaKeyFor(
            SECRET.getBytes(StandardCharsets.UTF_8)
    );

    // Public paths — no JWT required
    private static final String[] PUBLIC_PATHS = {
            "/api/auth/login",
            "/api/auth/register",
            "/actuator/health",
            "/actuator/info"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().toString();

        log.debug("[JWT FILTER] {} {}", method, path);

        // Step 1: skip public paths
        if (isPublicPath(path)) {
            log.debug("[JWT FILTER] Public path — skipping: {}", path);
            return chain.filter(exchange);
        }

        // Step 2: get Authorization header
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[JWT FILTER] BLOCKED — no token for: {}", path);
            return unauthorized(exchange,
                    "Missing token. Add: Authorization: Bearer <token>");
        }

        // Step 3: validate token
        String token = authHeader.substring(7);

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String customerId = claims.getSubject();
            Object roles = claims.get("roles");

            log.info("[JWT FILTER] Valid token — customer: {} path: {}",
                    customerId, path);

            // Step 4: add identity headers for BFF and domain services
            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(r -> r
                            .header("X-Customer-Id", customerId)
                            .header("X-Customer-Roles",
                                    roles != null ? roles.toString() : "")
                            .header("X-Authenticated", "true")
                    )
                    .build();

            // Step 5: forward to BFF
            return chain.filter(modifiedExchange);

        } catch (ExpiredJwtException e) {
            log.warn("[JWT FILTER] BLOCKED — token expired for: {}", path);
            return unauthorized(exchange,
                    "Token expired. Please login again.");

        } catch (JwtException e) {
            log.warn("[JWT FILTER] BLOCKED — invalid token: {}", e.getMessage());
            return unauthorized(exchange, "Invalid token.");
        }
    }

    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) return true;
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange,
                                    String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);

        String body = """
            {
              "error": "Unauthorized",
              "message": "%s",
              "hint": "POST /api/auth/login to get a token"
            }
            """.formatted(message);

        var buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(body.getBytes());

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return 0; // after RateLimitFilter(-1), before routing(10000)
    }
}