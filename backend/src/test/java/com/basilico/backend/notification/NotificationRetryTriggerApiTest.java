package com.basilico.backend.notification;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.basilico.backend.common.ratelimit.RateLimitFilter;
import com.basilico.backend.config.SecurityConfig;
import com.basilico.backend.notification.controller.InternalNotificationController;
import com.basilico.backend.health.HealthController;
import com.basilico.backend.notification.security.NotificationRetryTriggerFilter;
import com.basilico.backend.notification.service.NotificationDeliveryService;

@WebMvcTest(
		controllers = { InternalNotificationController.class, HealthController.class },
		excludeFilters = @ComponentScan.Filter(
				type = FilterType.ASSIGNABLE_TYPE,
				classes = RateLimitFilter.class
		)
)
@Import({ SecurityConfig.class, NotificationRetryTriggerFilter.class })
@TestPropertySource(properties = "basilico.notifications.email.retry-trigger-token=test-retry-token")
class NotificationRetryTriggerApiTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private NotificationDeliveryService notificationDeliveryService;

	@BeforeEach
	void resetMocks() {
		reset(notificationDeliveryService);
	}

	@Test
	void correctTriggerTokenProcessesBatch() throws Exception {
		when(notificationDeliveryService.processPendingBatch()).thenReturn(2);

		mockMvc.perform(post(NotificationRetryTriggerFilter.TRIGGER_PATH)
						.header(NotificationRetryTriggerFilter.TOKEN_HEADER, "test-retry-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("processed"))
				.andExpect(jsonPath("$.processedCount").value(2));

		verify(notificationDeliveryService).processPendingBatch();
	}

	@Test
	void missingTriggerTokenIsUnauthorizedAndDoesNotProcessBatch() throws Exception {
		mockMvc.perform(post(NotificationRetryTriggerFilter.TRIGGER_PATH))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

		verify(notificationDeliveryService, never()).processPendingBatch();
	}

	@Test
	void wrongTriggerTokenIsUnauthorizedAndDoesNotProcessBatch() throws Exception {
		mockMvc.perform(post(NotificationRetryTriggerFilter.TRIGGER_PATH)
						.header(NotificationRetryTriggerFilter.TOKEN_HEADER, "wrong-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

		verify(notificationDeliveryService, never()).processPendingBatch();
	}

	@Test
	void adminSessionAndCsrfRulesStillApplyOutsideInternalTrigger() throws Exception {
		mockMvc.perform(get("/api/admin/orders"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(patch("/api/admin/menu/items/1/availability")
						.with(user("owner@basilico.test").roles("OWNER"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"available\": false}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void publicHealthRemainsOpen() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk());
	}
}
