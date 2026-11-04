package pro.teaz.account.security;

import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.server.ResponseStatusException;

/**
 * Production security: a valid Keycloak access token (Bearer JWT) issued by the
 * realm configured in application-prod.properties, for everything except
 * reading profile pictures. Those are public: an img tag can't send a token,
 * and public wikis show them to guests.
 */
@Configuration
@Profile("prod")
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.GET, "/api/avatars/*").permitAll()
				.anyRequest().authenticated())
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.csrf(csrf -> csrf.disable());
		return http.build();
	}

	@Bean
	CurrentUser currentUser() {
		return () -> {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			try {
				return UUID.fromString(auth.getName());
			} catch (IllegalArgumentException | NullPointerException e) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This token has no user id.");
			}
		};
	}
}
