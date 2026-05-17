package com.buffett.backend.global.config; // 패키지 경로는 프로젝트 구조에 맞게 수정하세요.

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 0. 브라우저(프론트)에서 다른 origin으로 호출할 수 있도록 CORS 활성화
                .cors(Customizer.withDefaults())

                // 1. 개발 초반 AI 서버의 POST 요청을 허용하기 위해 CSRF 보호를 임시로 비활성화합니다.
                .csrf(csrf -> csrf.disable())

                // 2. HTTP Basic 및 Form 로그인을 꺼두어 브라우저 팝업이나 로그인 창이 뜨지 않게 합니다.
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())

                // 3. URL별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // /api/predictions로 시작하는 모든 요청(GET, POST 등)은 로그인 없이 허용
                        .requestMatchers("/api/predictions/**").permitAll()
                        // 나머지 모든 요청은 일단 인증(로그인)이 필요하도록 잠금
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    // 프론트 개발 서버에서 호출 가능하도록 CORS 정책 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(
                "http://localhost:5173", // Vite 기본
                "http://localhost:3000", // Next.js / CRA 기본
                "http://127.0.0.1:5500",
                "http://localhost:8080"
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
