package pro.teaz.account.security;

import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Local-dev stand-in for {@link SecurityConfig}: permits everything, so the
 * backend runs without Keycloak. Active whenever the "prod" profile is not.
 */
@Configuration
@Profile("!prod")
public class MockSecurityConfig {

	/** Same id as the frontend's mockAuth.ts. */
	static final UUID MOCK_USER = UUID.fromString("00000000-0000-0000-0000-000000000001");

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
			.csrf(csrf -> csrf.disable());
		return http.build();
	}

	@Bean
	CurrentUser currentUser() {
		return () -> MOCK_USER;
	}
}
