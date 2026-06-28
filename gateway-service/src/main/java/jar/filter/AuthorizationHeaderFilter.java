package jar.filter;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Jwts;
import reactor.core.publisher.Mono;

@Component
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {

		@Value("${jwt.secret}")
		private String secretKey;

		public AuthorizationHeaderFilter() {
			super(Config.class);
		}
		
		public static class Config{
			
		}
		@Override
	    public GatewayFilter apply(Config config) {
	        return (exchange, chain) -> {
	            ServerHttpRequest request = exchange.getRequest();

	            // 1. 클라이언트가 보낸 요청 헤더에 'Authorization'이 있는지 확인
	            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
	                return onError(exchange, "No authorization header", HttpStatus.UNAUTHORIZED);
	            }

	            // 2. 헤더에서 토큰 추출 ("Bearer " 문자열 제거)
	            String authorizationHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
	            String jwt = authorizationHeader.replace("Bearer ", "");

	            // 3. 토큰이 유효한지(우리가 발급한 게 맞는지) 검증
	            if (!isJwtValid(jwt)) {
	                return onError(exchange, "JWT token is not valid", HttpStatus.UNAUTHORIZED);
	            }

	            // 무사히 통과하면 다음 목적지(마이크로서비스)로 보냅니다.
	            return chain.filter(exchange);
	        };
	    }

	    private boolean isJwtValid(String jwt) {
	        boolean returnValue = true;
	        String subject = null;
	        
	        try {
	            // ⭐ 어제 발급 시 사용했던 바로 그 시크릿 키입니다!

	            // JWT 분해 및 서명 검증 시도
	            subject = Jwts.parserBuilder()
	                    .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
	                    .build()
	                    .parseClaimsJws(jwt)
	                    .getBody()
	                    .getSubject();
	        } catch (Exception ex) {
	            // 서명이 다르거나, 만료되었거나, 위조된 토큰이면 여기서 에러가 터집니다.
	            returnValue = false;
	        }

	        if (subject == null || subject.isEmpty()) {
	            returnValue = false;
	        }

	        return returnValue;
	    }

	    // 에러 발생 시 401 Unauthorized(권한 없음)를 튕겨내는 처리기
	    // 게이트웨이는 Spring WebFlux 기반이므로 Mono<Void> 형태로 반환합니다.
	    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
	        ServerHttpResponse response = exchange.getResponse();
	        response.setStatusCode(httpStatus);
	        
	        // 로그를 찍고 싶다면 여기에 log.error(err); 를 추가할 수 있습니다.
	        return response.setComplete();
	    }
}
