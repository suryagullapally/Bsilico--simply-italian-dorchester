package com.basilico.backend.notification;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.basilico.backend.common.ratelimit.RateLimitFilter;
import com.basilico.backend.config.SecurityConfig;
import com.basilico.backend.notification.controller.InternalNotificationController;
import com.basilico.backend.notification.security.NotificationRetryTriggerFilter;
import com.basilico.backend.notification.service.NotificationDeliveryService;

@WebMvcTest(
		controllers = InternalNotificationController.class,
		excludeFilters = @ComponentScan.Filter(
				type = FilterType.ASSIGNABLE_TYPE,
				classes = RateLimitFilter.class
		)
)
@Import({ SecurityConfig.class, NotificationRetryTriggerFilter.class })
class NotificationRetryTriggerMissingConfigApiTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private NotificationDeliveryService notificationDeliveryService;

	@Test
	void missingServerTriggerSecretFailsClosed() throws Exception {
		mockMvc.perform(post(NotificationRetryTriggerFilter.TRIGGER_PATH)
						.header(NotificationRetryTriggerFilter.TOKEN_HEADER, "test-retry-token"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.error").value("SERVICE_UNAVAILABLE"));

		verify(notificationDeliveryService, never()).processPendingBatch();
	}
}
