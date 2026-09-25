package com.vn.otech.security.config;

import com.vn.otech.security.web.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
	@Value("${app.cors.allowed-origins}")
	private String allowedOrigins;

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(java.util.Arrays.stream(allowedOrigins.split(","))
				.map(String::trim).filter(origin -> !origin.isBlank()).toList());
		configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type"));
		configuration.setExposedHeaders(java.util.List.of("Authorization"));
		configuration.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.cors(cors -> { })
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.formLogin(formLogin -> formLogin.disable())
			.httpBasic(httpBasic -> httpBasic.disable())
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/", "/error", "/api/auth/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/posts/**", "/api/products/**", "/api/categories/**", "/api/pages/**").permitAll()
				.requestMatchers("/api/admin/**").hasRole("ADMIN")
				.requestMatchers("/api/manager/**").hasAnyRole("MANAGER", "ADMIN")
				.requestMatchers("/api/user/**").hasAnyRole("USER", "MANAGER", "ADMIN")
				.anyRequest().authenticated())
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}