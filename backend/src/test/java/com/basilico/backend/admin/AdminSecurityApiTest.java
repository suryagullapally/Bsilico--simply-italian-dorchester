package com.basilico.backend.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import java.time.DayOfWeek;
import java.time.LocalDate;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.basilico.backend.admin.entity.AdminRole;
import com.basilico.backend.admin.entity.AdminUser;
import com.basilico.backend.admin.repository.AdminUserRepository;
import com.basilico.backend.admin.service.AdminEmail;
import com.basilico.backend.menu.entity.MenuItem;
import com.basilico.backend.menu.repository.MenuItemRepository;

@SpringBootTest(properties = {
		"spring.session.timeout=365d",
		"server.servlet.session.timeout=365d",
		"basilico.admin.session.cookie.max-age=365d",
		"basilico.admin.session.cookie.secure=true",
		"basilico.admin.session.cookie.same-site=Lax"
})
@AutoConfigureMockMvc
@Transactional
class AdminSecurityApiTest {

	private static final int SESSION_COOKIE_MAX_AGE_SECONDS = 365 * 24 * 60 * 60;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private MenuItemRepository menuItemRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	void unauthenticatedAdminOrdersAreRejected() throws Exception {
		mockMvc.perform(get("/api/admin/orders"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	void unauthenticatedAdminMenuItemsAreRejected() throws Exception {
		mockMvc.perform(get("/api/admin/menu/items"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	void unauthenticatedAdminMutationWithCsrfIsRejected() throws Exception {
		MenuItem item = menuItemRepository.findBySlug("pizza-margherita").orElseThrow();

		mockMvc.perform(patch("/api/admin/menu/items/{id}/availability", item.getId())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"available\": false}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void csrfEndpointReturnsSpringToken() throws Exception {
		mockMvc.perform(get("/api/admin/auth/csrf"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isString())
				.andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"));
	}

	@Test
	void validLoginPersistsSecurityContextForMeEndpoint() throws Exception {
		createAdmin("owner@basilico.test", "correct-password", true);

		Cookie sessionCookie = loginAndReturnSessionCookie("OWNER@BASILICO.TEST", "correct-password");

		mockMvc.perform(get("/api/admin/auth/me").cookie(sessionCookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("owner@basilico.test"));
	}

	@Test
	void validLoginPersistsSpringSessionInJdbc() throws Exception {
		createAdmin("jdbc-session@basilico.test", "correct-password", true);

		Cookie sessionCookie = loginAndReturnSessionCookie("jdbc-session@basilico.test", "correct-password");

		Integer sessionCount = jdbcTemplate.queryForObject(
				"select count(*) from SPRING_SESSION where SESSION_ID = ?",
				Integer.class,
				sessionCookie.getValue()
		);
		Integer attributeCount = jdbcTemplate.queryForObject(
				"""
						select count(*)
						from SPRING_SESSION_ATTRIBUTES a
						join SPRING_SESSION s on s.PRIMARY_ID = a.SESSION_PRIMARY_ID
						where s.SESSION_ID = ?
						""",
				Integer.class,
				sessionCookie.getValue()
		);

		assertEquals("login should create one JDBC-backed Spring Session", 1, sessionCount);
		assertTrue("login should persist session attributes", attributeCount != null && attributeCount > 0);
	}

	@Test
	void loginCookieIsLongLivedHttpOnlySecureAndSameSiteLax() throws Exception {
		createAdmin("cookie@basilico.test", "correct-password", true);

		mockMvc.perform(post("/api/admin/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "cookie@basilico.test",
								  "password": "correct-password"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(cookie().exists("SESSION"))
				.andExpect(cookie().httpOnly("SESSION", true))
				.andExpect(cookie().secure("SESSION", true))
				.andExpect(cookie().path("SESSION", "/"))
				.andExpect(cookie().maxAge("SESSION", SESSION_COOKIE_MAX_AGE_SECONDS))
				.andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")));
	}

	@Test
	void wrongPasswordReturnsGenericUnauthorizedMessage() throws Exception {
		createAdmin("owner-wrong-password@basilico.test", "correct-password", true);

		mockMvc.perform(post("/api/admin/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "owner-wrong-password@basilico.test",
								  "password": "wrong-password"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid email or password."));
	}

	@Test
	void unknownEmailReturnsSameGenericUnauthorizedMessage() throws Exception {
		mockMvc.perform(post("/api/admin/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "missing@basilico.test",
								  "password": "wrong-password"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid email or password."));
	}

	@Test
	void inactiveAdminCannotLogin() throws Exception {
		createAdmin("inactive@basilico.test", "correct-password", false);

		mockMvc.perform(post("/api/admin/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "inactive@basilico.test",
								  "password": "correct-password"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid email or password."));
	}

	@Test
	void authenticatedAdminCanReadOperationalApis() throws Exception {
		mockMvc.perform(get("/api/admin/orders").with(adminUser()))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/admin/bookings").with(adminUser()))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/admin/menu/items").with(adminUser()))
				.andExpect(status().isOk());
	}

	@Test
	void authenticatedMutationWithoutCsrfIsForbidden() throws Exception {
		MenuItem item = menuItemRepository.findBySlug("pizza-margherita").orElseThrow();

		mockMvc.perform(patch("/api/admin/menu/items/{id}/availability", item.getId())
						.with(adminUser())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"available\": false}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("FORBIDDEN"));
	}

	@Test
	void authenticatedMutationWithCsrfSucceeds() throws Exception {
		MenuItem item = menuItemRepository.findBySlug("pizza-margherita").orElseThrow();

		mockMvc.perform(patch("/api/admin/menu/items/{id}/availability", item.getId())
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"available\": false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.available").value(false));
	}

	@Test
	void persistedSessionCanPerformCsrfProtectedMutation() throws Exception {
		createAdmin("csrf-session@basilico.test", "correct-password", true);
		MenuItem item = menuItemRepository.findBySlug("pizza-margherita").orElseThrow();
		Cookie sessionCookie = loginAndReturnSessionCookie("csrf-session@basilico.test", "correct-password");

		mockMvc.perform(patch("/api/admin/menu/items/{id}/availability", item.getId())
						.cookie(sessionCookie)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"available\": false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.available").value(false));
	}

	@Test
	void logoutInvalidatesSession() throws Exception {
		createAdmin("logout@basilico.test", "correct-password", true);

		Cookie sessionCookie = loginAndReturnSessionCookie("logout@basilico.test", "correct-password");

		mockMvc.perform(post("/api/admin/auth/logout")
						.cookie(sessionCookie)
						.with(csrf()))
				.andExpect(status().isNoContent())
				.andExpect(cookie().maxAge("SESSION", 0));

		assertSessionRowCount(sessionCookie, 0);

		mockMvc.perform(get("/api/admin/auth/me").cookie(sessionCookie))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void expiredJdbcSessionIsRejected() throws Exception {
		createAdmin("expired@basilico.test", "correct-password", true);
		Cookie sessionCookie = loginAndReturnSessionCookie("expired@basilico.test", "correct-password");

		expirePersistedSession(sessionCookie);

		mockMvc.perform(get("/api/admin/auth/me").cookie(sessionCookie))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void publicCustomerRoutesRemainOpenWithoutAuthentication() throws Exception {
		MenuItem margherita = menuItemRepository.findBySlug("pizza-margherita").orElseThrow();

		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/menu"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/menu/items/pizza-margherita"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(collectionOrderPayload(margherita.getId())))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/bookings")
						.contentType(MediaType.APPLICATION_JSON)
						.content(bookingPayload(nextOpenDate())))
				.andExpect(status().isCreated());
	}

	private AdminUser createAdmin(String email, String password, boolean active) {
		AdminUser user = new AdminUser(
				AdminEmail.normalize(email),
				passwordEncoder.encode(password),
				"Basilico Owner",
				AdminRole.OWNER
		);
		user.setActive(active);
		return adminUserRepository.saveAndFlush(user);
	}

	private Cookie loginAndReturnSessionCookie(String email, String password) throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/api/admin/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s"
								}
								""".formatted(email, password)))
				.andExpect(status().isOk())
				.andReturn();

		Cookie sessionCookie = loginResult.getResponse().getCookie("SESSION");
		assertNotNull("login should return the Spring Session cookie", sessionCookie);
		return sessionCookie;
	}

	private void assertSessionRowCount(Cookie sessionCookie, int expectedCount) {
		Integer sessionCount = jdbcTemplate.queryForObject(
				"select count(*) from SPRING_SESSION where SESSION_ID = ?",
				Integer.class,
				sessionCookie.getValue()
		);
		assertEquals("unexpected persisted session row count", expectedCount, sessionCount);
	}

	private void expirePersistedSession(Cookie sessionCookie) {
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		transactionTemplate.executeWithoutResult(status -> jdbcTemplate.update(
				"""
						update SPRING_SESSION
						set LAST_ACCESS_TIME = 0,
						    MAX_INACTIVE_INTERVAL = 1,
						    EXPIRY_TIME = 1
						where SESSION_ID = ?
						""",
				sessionCookie.getValue()
		));
	}

	private org.springframework.test.web.servlet.request.RequestPostProcessor adminUser() {
		return user("owner@basilico.test").roles("OWNER");
	}

	private String collectionOrderPayload(Long menuItemId) {
		return """
				{
				  "fulfilmentType": "COLLECTION",
				  "customer": {
				    "firstName": "John",
				    "lastName": "Smith",
				    "phone": "07123456789",
				    "email": "john@example.com"
				  },
				  "timing": {
				    "type": "ASAP"
				  },
				  "notes": "",
				  "items": [
				    { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 }
				  ]
				}
				""".formatted(menuItemId);
	}

	private String bookingPayload(LocalDate date) {
		return """
				{
				  "date": "%s",
				  "time": "18:30",
				  "partySize": 2,
				  "firstName": "John",
				  "lastName": "Smith",
				  "phone": "07123456789",
				  "email": "john@example.com",
				  "specialRequests": ""
				}
				""".formatted(date);
	}

	private LocalDate nextOpenDate() {
		LocalDate date = LocalDate.now().plusDays(1);
		while (date.getDayOfWeek() == DayOfWeek.TUESDAY) {
			date = date.plusDays(1);
		}
		return date;
	}
}
