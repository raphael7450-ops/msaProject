package jar.filter;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import reactor.core.publisher.Mono;

@Component
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {

    @Value("${jwt.secret}")
    private String secretKey;

    public AuthorizationHeaderFilter() {
        super(Config.class);
    }

    public static class Config {
        private boolean requireAdmin;

        public boolean isRequireAdmin() {
            return requireAdmin;
        }

        public void setRequireAdmin(boolean requireAdmin) {
            this.requireAdmin = requireAdmin;
        }
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authorizationHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            Claims claims = parseClaims(authorizationHeader.substring(7));
            if (claims == null) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            if (config.isRequireAdmin() && !"ADMIN".equals(claims.get("role", String.class))) {
                return onError(exchange, HttpStatus.FORBIDDEN);
            }

            return chain.filter(exchange);
        };
    }

    private Claims parseClaims(String jwt) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody();
            String subject = claims.getSubject();
            return subject == null || subject.isEmpty() ? null : claims;
        } catch (Exception ex) {
            return null;
        }
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }
}
