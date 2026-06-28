package jar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity(debug = true)
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // CSRF 비활성화 (POST 호출을 위해 필수)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login/**").permitAll()    // 로그인 경로 허용
                .requestMatchers("/actuator/**").permitAll() // 모니터링 경로 허용
                .requestMatchers("/error").permitAll()       // 에러 페이지 허용
                .anyRequest().authenticated()                // 나머지는 인증 필요
            )
            // ⭐ 에러 났던 .addFilter(...) 부분과 AuthenticationManager 설정을 과감히 삭제합니다.
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)); // JWT 사용을 위한 설정
            
        return http.build();
    }
    
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
