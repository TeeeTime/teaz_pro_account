package pro.teaz.account.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class SecurityConfigTests {

	@Autowired
	MockMvc mvc;

	@Test
	void rejectsRequestWithoutToken() throws Exception {
		mvc.perform(get("/")).andExpect(status().isUnauthorized());
	}

	@Test
	void acceptsRequestWithToken() throws Exception {
		// No endpoints exist yet, so an authenticated request gets past security and hits 404.
		mvc.perform(get("/").with(jwt())).andExpect(status().isNotFound());
	}
}
