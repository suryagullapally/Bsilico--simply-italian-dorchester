package com.basilico.backend.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.basilico.backend.admin.entity.AdminRole;
import com.basilico.backend.admin.entity.AdminUser;
import com.basilico.backend.admin.repository.AdminUserRepository;
import com.basilico.backend.admin.service.AdminEmail;
import com.basilico.backend.menu.entity.MenuItem;
import com.basilico.backend.menu.repository.MenuItemRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminSecurityApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private MenuItemRepository menuItemRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

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

		MvcResult loginResult = mockMvc.perform(post("/api/admin/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "OWNER@BASILICO.TEST",
								  "password": "correct-password"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("owner@basilico.test"))
				.andExpect(jsonPath("$.displayName").value("Basilico Owner"))
				.andExpect(jsonPath("$.role").value("OWNER"))
				.andReturn();

		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		mockMvc.perform(get("/api/admin/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("owner@basilico.test"));
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
	void logoutInvalidatesSession() throws Exception {
		createAdmin("logout@basilico.test", "correct-password", true);

		MvcResult loginResult = mockMvc.perform(post("/api/admin/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "logout@basilico.test",
								  "password": "correct-password"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();

		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		mockMvc.perform(post("/api/admin/auth/logout")
						.session(session)
						.with(csrf()))
				.andExpect(status().isNoContent())
				.andExpect(cookie().maxAge("JSESSIONID", 0));

		mockMvc.perform(get("/api/admin/auth/me").session(session))
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
