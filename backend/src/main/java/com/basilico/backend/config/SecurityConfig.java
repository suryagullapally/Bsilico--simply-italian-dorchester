package com.basilico.backend.config;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import tools.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import com.basilico.backend.common.error.ApiErrorResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			ObjectMapper objectMapper,
			SecurityContextRepository securityContextRepository,
			CsrfTokenRepository csrfTokenRepository) throws Exception {
		http.cors(Customizer.withDefaults());
		http.csrf(csrf -> csrf
				.csrfTokenRepository(csrfTokenRepository)
				.ignoringRequestMatchers(
						PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/orders"),
						PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/bookings"),
						PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/fulfilment/check-delivery"),
						PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/fulfilment/delivery-quote"),
						PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/payments/checkout-session"),
						PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/payments/stripe/webhook")
				));
		http.securityContext(securityContext -> securityContext
				.securityContextRepository(securityContextRepository));
		http.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
				.sessionFixation(sessionFixation -> sessionFixation.changeSessionId()));
		http.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/health", "/actuator/health", "/api/menu/**",
						"/api/fulfilment/options").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/orders", "/api/bookings").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/fulfilment/check-delivery").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/fulfilment/delivery-quote").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/payments/checkout-session").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/payments/stripe/webhook").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/payments/checkout-session/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/admin/auth/csrf").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/admin/auth/login").permitAll()
				.requestMatchers("/api/admin/**").hasAnyRole("OWNER", "MANAGER", "STAFF")
				.requestMatchers("/api/**").denyAll()
				.anyRequest().permitAll());
		http.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(jsonAuthenticationEntryPoint(objectMapper))
				.accessDeniedHandler(jsonAccessDeniedHandler(objectMapper)));
		http.formLogin(AbstractHttpConfigurer::disable);
		http.httpBasic(AbstractHttpConfigurer::disable);
		http.logout(logout -> logout
				.logoutUrl("/api/admin/auth/logout")
				.invalidateHttpSession(true)
				.clearAuthentication(true)
				.deleteCookies("JSESSIONID")
				.logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpServletResponse.SC_NO_CONTENT)));

		return http.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	public CsrfTokenRepository csrfTokenRepository() {
		return new HttpSessionCsrfTokenRepository();
	}

	@Bean
	public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
		return new ChangeSessionIdAuthenticationStrategy();
	}

	private AuthenticationEntryPoint jsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
		return (request, response, exception) -> writeSecurityError(
				response,
				objectMapper,
				HttpServletResponse.SC_UNAUTHORIZED,
				"UNAUTHORIZED",
				"Authentication is required."
		);
	}

	private AccessDeniedHandler jsonAccessDeniedHandler(ObjectMapper objectMapper) {
		return (request, response, exception) -> writeSecurityError(
				response,
				objectMapper,
				HttpServletResponse.SC_FORBIDDEN,
				"FORBIDDEN",
				"Access is forbidden."
		);
	}

	private void writeSecurityError(HttpServletResponse response,
			ObjectMapper objectMapper,
			int status,
			String code,
			String message) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), new ApiErrorResponse(status, code, message));
	}
}
