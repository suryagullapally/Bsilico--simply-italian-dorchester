package com.basilico.backend.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.basilico.backend.menu.entity.MenuCustomizer;
import com.basilico.backend.menu.entity.MenuItem;
import com.basilico.backend.menu.repository.MenuCustomizerRepository;
import com.basilico.backend.menu.repository.MenuItemRepository;
import com.basilico.backend.fulfilment.entity.DeliveryAreaMode;
import com.basilico.backend.fulfilment.entity.DeliveryPostcodeRule;
import com.basilico.backend.fulfilment.entity.DeliveryPricingMode;
import com.basilico.backend.fulfilment.entity.FulfilmentSettings;
import com.basilico.backend.fulfilment.repository.DeliveryPostcodeRuleRepository;
import com.basilico.backend.fulfilment.repository.FulfilmentSettingsRepository;
import com.basilico.backend.fulfilment.service.GeoCoordinates;
import com.basilico.backend.fulfilment.service.GeocodedPostcode;
import com.basilico.backend.fulfilment.service.PostcodeGeocoder;
import com.basilico.backend.fulfilment.service.RouteDuration;
import com.basilico.backend.fulfilment.service.RouteDurationProvider;

@SpringBootTest(properties = "basilico.payments.manual-payment-status.enabled=true")
@AutoConfigureMockMvc
@Transactional
class OrderApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MenuItemRepository menuItemRepository;

	@Autowired
	private MenuCustomizerRepository customizerRepository;

	@Autowired
	private FulfilmentSettingsRepository fulfilmentSettingsRepository;

	@Autowired
	private DeliveryPostcodeRuleRepository postcodeRuleRepository;

	@MockitoBean
	private PostcodeGeocoder postcodeGeocoder;

	@MockitoBean
	private RouteDurationProvider routeDurationProvider;

	@BeforeEach
	void resetProviders() {
		resetFulfilmentSettings();
		reset(postcodeGeocoder, routeDurationProvider);
		when(routeDurationProvider.estimateDrivingDuration(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
				.thenReturn(Optional.empty());
	}

	@Test
	void createsNormalMenuOrderWithBackendCalculatedSubtotal() throws Exception {
		MenuItem margherita = item("pizza-margherita");
		MenuItem lasagna = item("lasagna");

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(collectionOrderPayload("""
								[
								  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 },
								  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 }
								]
								""".formatted(margherita.getId(), lasagna.getId()))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.orderReference").exists())
				.andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
				.andExpect(jsonPath("$.paymentStatus").value("UNPAID"))
				.andExpect(jsonPath("$.subtotalPence").value(1998))
				.andExpect(jsonPath("$.deliveryFeePence").value(0))
				.andExpect(jsonPath("$.totalPence").value(1998))
				.andExpect(jsonPath("$.items[0].unitPricePence").value(899))
				.andExpect(jsonPath("$.items[1].unitPricePence").value(1099));
	}

	@Test
	void multipliesNormalItemQuantityUsingIntegerPennies() throws Exception {
		MenuItem margherita = item("pizza-margherita");

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(collectionOrderPayload("""
								[
								  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 2 }
								]
								""".formatted(margherita.getId()))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.subtotalPence").value(1798))
				.andExpect(jsonPath("$.totalPence").value(1798))
				.andExpect(jsonPath("$.items[0].lineTotalPence").value(1798));
	}

	@Test
	void createsCustomPizzaFromCurrentCustomizerAndToppingPrices() throws Exception {
		MenuCustomizer customizer = customizer();
		List<Long> toppingIds = customizer.getToppings()
				.stream()
				.limit(2)
				.map(topping -> topping.getId())
				.toList();

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(collectionOrderPayload("""
								[
								  {
								    "type": "CUSTOM_PIZZA",
								    "customizerId": %d,
								    "toppingIds": [%d, %d],
								    "quantity": 1
								  }
								]
								""".formatted(customizer.getId(), toppingIds.get(0), toppingIds.get(1)))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.subtotalPence").value(950))
				.andExpect(jsonPath("$.totalPence").value(950))
				.andExpect(jsonPath("$.items[0].unitPricePence").value(950))
				.andExpect(jsonPath("$.items[0].toppings.length()").value(2))
				.andExpect(jsonPath("$.items[0].toppings[0].pricePence").value(125));
	}

	@Test
	void createsMixedBasketWithBackendCalculatedSubtotal() throws Exception {
		MenuItem margherita = item("pizza-margherita");
		MenuItem lasagna = item("lasagna");
		MenuCustomizer customizer = customizer();
		List<Long> toppingIds = customizer.getToppings()
				.stream()
				.limit(2)
				.map(topping -> topping.getId())
				.toList();

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(collectionOrderPayload("""
								[
								  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 },
								  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 },
								  {
								    "type": "CUSTOM_PIZZA",
								    "customizerId": %d,
								    "toppingIds": [%d, %d],
								    "quantity": 1
								  }
								]
								""".formatted(
								margherita.getId(),
								lasagna.getId(),
								customizer.getId(),
								toppingIds.get(0),
								toppingIds.get(1)
						))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.subtotalPence").value(2948))
				.andExpect(jsonPath("$.totalPence").value(2948));
	}

	@Test
	void rejectsSoldOutMenuItem() throws Exception {
		MenuItem lasagna = item("lasagna");
		lasagna.setAvailable(false);
		menuItemRepository.saveAndFlush(lasagna);

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(collectionOrderPayload("""
								[
								  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 }
								]
								""".formatted(lasagna.getId()))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("CONFLICT"));
	}

	@Test
	void rejectsInactiveMenuItem() throws Exception {
		MenuItem lasagna = item("lasagna");
		lasagna.setActive(false);
		menuItemRepository.saveAndFlush(lasagna);

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(collectionOrderPayload("""
								[
								  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 }
								]
								""".formatted(lasagna.getId()))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("CONFLICT"));
	}

	@Test
	void rejectsDuplicateToppingIds() throws Exception {
		MenuCustomizer customizer = customizer();
		Long toppingId = customizer.getToppings().getFirst().getId();

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(collectionOrderPayload("""
								[
								  {
								    "type": "CUSTOM_PIZZA",
								    "customizerId": %d,
								    "toppingIds": [%d, %d],
								    "quantity": 1
								  }
								]
								""".formatted(customizer.getId(), toppingId, toppingId))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"));
	}

	@Test
	void ignoresFakeFrontendPriceFieldsAndUsesDatabasePrices() throws Exception {
		MenuItem margherita = item("pizza-margherita");

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(collectionOrderPayload("""
								[
								  {
								    "type": "MENU_ITEM",
								    "menuItemId": %d,
								    "quantity": 1,
								    "unitPricePence": 1,
								    "lineTotalPence": 1
								  }
								]
								""".formatted(margherita.getId()))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.subtotalPence").value(899))
				.andExpect(jsonPath("$.totalPence").value(899))
				.andExpect(jsonPath("$.items[0].unitPricePence").value(899));
	}

	@Test
	void rejectsCollectionOrderWhenCollectionIsDisabled() throws Exception {
		FulfilmentSettings settings = fulfilmentSettings();
		settings.setCollectionEnabled(false);
		fulfilmentSettingsRepository.saveAndFlush(settings);
		MenuItem margherita = item("pizza-margherita");

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(collectionOrderPayload("""
								[
								  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 }
								]
								""".formatted(margherita.getId()))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Collection is currently unavailable."));
	}

	@Test
	void rejectsDeliveryOrderWhenDeliveryIsDisabled() throws Exception {
		MenuItem margherita = item("pizza-margherita");

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(deliveryOrderPayload("""
								[
								  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 }
								]
								""".formatted(margherita.getId()), "DT1 1TT")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Delivery is currently unavailable. Please choose collection."));
	}

	@Test
	void createsDeliveryOrderWithMatchingPostcodeAndConfiguredDeliveryFee() throws Exception {
		enableDelivery(250, null, null, "DT1");
		MenuItem margherita = item("pizza-margherita");

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(deliveryOrderPayload("""
								[
								  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 }
								]
								""".formatted(margherita.getId()), "dt1 1tt")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.fulfilmentType").value("DELIVERY"))
				.andExpect(jsonPath("$.subtotalPence").value(899))
				.andExpect(jsonPath("$.deliveryFeePence").value(250))
				.andExpect(jsonPath("$.totalPence").value(1149));
	}

	@Test
	void rejectsDeliveryOrderWhenPostcodeDoesNotMatchRules() throws Exception {
		enableDelivery(250, null, null, "DT1");
		MenuItem margherita = item("pizza-margherita");

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(deliveryOrderPayload("""
								[
								  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 }
								]
								""".formatted(margherita.getId()), "BH1 1AA")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"Sorry, we do not currently deliver to this postcode. Please choose collection or another delivery address."));
	}

	@Test
	void rejectsDeliveryOrderWhenDeliveryHasNoActivePostcodeRules() throws Exception {
		FulfilmentSettings settings = fulfilmentSettings();
		settings.setDeliveryAreaMode(DeliveryAreaMode.POSTCODE_RULES);
		settings.setDeliveryPricingMode(DeliveryPricingMode.FLAT_FEE);
		settings.setDeliveryEnabled(true);
		settings.setDeliveryFeePence(250);
		fulfilmentSettingsRepository.saveAndFlush(settings);
		MenuItem margherita = item("pizza-margherita");

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(deliveryOrderPayload("""
								[
								  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 }
								]
								""".formatted(margherita.getId()), "DT1 1TT")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Delivery is not currently available for this address."));
	}

	@Test
	void rejectsDeliveryOrderBelowConfiguredMinimum() throws Exception {
		enableDelivery(250, 900, null, "DT1");
		MenuItem margherita = item("pizza-margherita");

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(deliveryOrderPayload("""
								[
								  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 }
								]
								""".formatted(margherita.getId()), "DT1 1TT")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Delivery orders need a food subtotal of at least £9.00."));
	}

	@Test
	void acceptsDeliveryOrderWhenMinimumIsExactlyReached() throws Exception {
		enableDelivery(250, 1798, null, "DT1");
		MenuItem margherita = item("pizza-margherita");

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(deliveryOrderPayload("""
								[
								  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 2 }
								]
								""".formatted(margherita.getId()), "DT11TT")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.subtotalPence").value(1798))
				.andExpect(jsonPath("$.deliveryFeePence").value(250))
				.andExpect(jsonPath("$.totalPence").value(2048));
	}

	@Test
	void appliesFreeDeliveryThresholdWhenFoodSubtotalQualifies() throws Exception {
		enableDelivery(250, null, 1798, "DT1");
		MenuItem margherita = item("pizza-margherita");

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(deliveryOrderPayload("""
								[
								  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 2 }
								]
								""".formatted(margherita.getId()), "DT1 1TT")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.subtotalPence").value(1798))
				.andExpect(jsonPath("$.deliveryFeePence").value(0))
				.andExpect(jsonPath("$.totalPence").value(1798));
	}

	@Test
	void ignoresFakeFrontendFeeAndTotalFieldsAndUsesDatabasePricing() throws Exception {
		enableDelivery(250, null, null, "DT1");
		MenuItem margherita = item("pizza-margherita");

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(deliveryOrderPayload("""
								[
								  {
								    "type": "MENU_ITEM",
								    "menuItemId": %d,
								    "quantity": 1,
								    "unitPricePence": 1,
								    "lineTotalPence": 1
								  }
								]
								""".formatted(margherita.getId()), "DT1 1TT").replaceFirst(
										"\"items\"", "\"deliveryFeePence\": 1, \"totalPence\": 1, \"items\"")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.subtotalPence").value(899))
				.andExpect(jsonPath("$.deliveryFeePence").value(250))
				.andExpect(jsonPath("$.totalPence").value(1149));
	}

	@Test
	void createsRadiusDeliveryOrderWithinThreeMilesWithTwoPoundFee() throws Exception {
		assertRadiusDeliveryFee(2.7, 200, 1099);
	}

	@Test
	void createsRadiusDeliveryOrderOverThreeUpToFourMilesWithThreePoundFee() throws Exception {
		assertRadiusDeliveryFee(3.1, 300, 1199);
	}

	@Test
	void createsRadiusDeliveryOrderOverFourUpToFiveMilesWithFourPoundFeeAndEtaSnapshot() throws Exception {
		enableRadiusDelivery();
		mockPostcode("DT4 4AA", 4.4);
		when(routeDurationProvider.estimateDrivingDuration(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
				.thenReturn(Optional.of(new RouteDuration(11, 5.2)));
		MenuItem margherita = item("pizza-margherita");

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(deliveryOrderPayload("""
								[
								  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 }
								]
								""".formatted(margherita.getId()), "DT4 4AA")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.subtotalPence").value(899))
				.andExpect(jsonPath("$.deliveryFeePence").value(400))
				.andExpect(jsonPath("$.totalPence").value(1299))
				.andExpect(jsonPath("$.deliveryDistanceMiles").exists())
				.andExpect(jsonPath("$.deliveryPreparationMinutes").value(20))
				.andExpect(jsonPath("$.deliveryTravelMinutes").value(11))
				.andExpect(jsonPath("$.estimatedDeliveryMinutes").value(35));
	}

	@Test
	void createsRadiusDeliveryOrderOverFiveUpToSixMilesWithFivePoundFee() throws Exception {
		assertRadiusDeliveryFee(5.1, 500, 1399);
	}

	@Test
	void rejectsRadiusDeliveryOrderOutsideSixMiles() throws Exception {
		enableRadiusDelivery();
		mockPostcode("DT6 6AA", 6.01);
		MenuItem margherita = item("pizza-margherita");

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(deliveryOrderPayload("""
								[
								  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 }
								]
								""".formatted(margherita.getId()), "DT6 6AA")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Sorry, we currently deliver within 6 miles of Basilico."));
	}

	@Test
	void radiusDeliveryIgnoresLegacyFreeDeliveryThresholdAndFakeFrontendFee() throws Exception {
		enableRadiusDelivery();
		FulfilmentSettings settings = fulfilmentSettings();
		settings.setFreeDeliveryThresholdPence(1);
		fulfilmentSettingsRepository.saveAndFlush(settings);
		mockPostcode("DT4 4AA", 4.4);
		MenuItem margherita = item("pizza-margherita");

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(deliveryOrderPayload("""
								[
								  {
								    "type": "MENU_ITEM",
								    "menuItemId": %d,
								    "quantity": 1,
								    "unitPricePence": 1,
								    "lineTotalPence": 1
								  }
								]
								""".formatted(margherita.getId()), "DT4 4AA").replaceFirst(
										"\"items\"", "\"deliveryFeePence\": 1, \"totalPence\": 1, \"items\"")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.subtotalPence").value(899))
				.andExpect(jsonPath("$.deliveryFeePence").value(400))
				.andExpect(jsonPath("$.totalPence").value(1299));
	}

	@Test
	void adminCanUpdateOrderStatusAndDevelopmentPaymentStatus() throws Exception {
		MenuItem margherita = item("pizza-margherita");
		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(collectionOrderPayload("""
								[
								  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 }
								]
								""".formatted(margherita.getId()))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

		String orderId = com.jayway.jsonpath.JsonPath.read(
				mockMvc.perform(get("/api/admin/orders")
								.with(adminUser()))
						.andExpect(status().isOk())
						.andReturn()
						.getResponse()
						.getContentAsString(),
				"$.content[0].id"
		).toString();

		mockMvc.perform(patch("/api/admin/orders/{id}/status", orderId)
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "ACCEPTED"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACCEPTED"));

		mockMvc.perform(patch("/api/admin/orders/{id}/payment-status", orderId)
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "paymentStatus": "PAID"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paymentStatus").value("PAID"));
	}

	private String collectionOrderPayload(String itemsJson) {
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
				  "notes": "No onions please",
				  "items": %s
				}
				""".formatted(itemsJson);
	}

	private String deliveryOrderPayload(String itemsJson, String postcode) {
		return """
				{
				  "fulfilmentType": "DELIVERY",
				  "customer": {
				    "firstName": "John",
				    "lastName": "Smith",
				    "phone": "07123456789",
				    "email": "john@example.com"
				  },
				  "deliveryAddress": {
				    "line1": "41 Example Street",
				    "city": "Dorchester",
				    "postcode": "%s"
				  },
				  "timing": {
				    "type": "ASAP"
				  },
				  "notes": "No onions please",
				  "items": %s
				}
				""".formatted(postcode, itemsJson);
	}

	private void enableDelivery(int deliveryFeePence, Integer minimumDeliveryOrderPence,
			Integer freeDeliveryThresholdPence, String postcodePattern) {
		FulfilmentSettings settings = fulfilmentSettings();
		settings.setDeliveryAreaMode(DeliveryAreaMode.POSTCODE_RULES);
		settings.setDeliveryPricingMode(DeliveryPricingMode.FLAT_FEE);
		settings.setDeliveryFeePence(deliveryFeePence);
		settings.setMinimumDeliveryOrderPence(minimumDeliveryOrderPence);
		settings.setFreeDeliveryThresholdPence(freeDeliveryThresholdPence);
		settings.setDeliveryEnabled(true);
		fulfilmentSettingsRepository.saveAndFlush(settings);
		postcodeRuleRepository.saveAndFlush(new DeliveryPostcodeRule(postcodePattern, 1));
	}

	private void resetFulfilmentSettings() {
		postcodeRuleRepository.deleteAll();
		FulfilmentSettings settings = fulfilmentSettings();
		settings.setCollectionEnabled(true);
		settings.setDeliveryEnabled(false);
		settings.setMinimumDeliveryOrderPence(null);
		settings.setDeliveryFeePence(null);
		settings.setFreeDeliveryThresholdPence(null);
		settings.setDeliveryAreaMode(DeliveryAreaMode.RADIUS);
		settings.setRestaurantPostcode("DT1 1TT");
		settings.setRestaurantLatitude(new BigDecimal("50.714050"));
		settings.setRestaurantLongitude(new BigDecimal("-2.438190"));
		settings.setDeliveryRadiusMiles(new BigDecimal("6.00"));
		settings.setPreparationTimeMinutes(20);
		settings.setDeliveryPricingMode(DeliveryPricingMode.RADIUS_BANDS);
		settings.setBaseDeliveryRadiusMiles(new BigDecimal("3.00"));
		settings.setBaseDeliveryFeePence(200);
		settings.setExtraMileFeePence(100);
		fulfilmentSettingsRepository.saveAndFlush(settings);
	}

	private void enableRadiusDelivery() {
		FulfilmentSettings settings = fulfilmentSettings();
		settings.setDeliveryEnabled(true);
		settings.setDeliveryAreaMode(DeliveryAreaMode.RADIUS);
		settings.setRestaurantPostcode("DT1 1TT");
		settings.setRestaurantLatitude(new BigDecimal("50.714050"));
		settings.setRestaurantLongitude(new BigDecimal("-2.438190"));
		settings.setDeliveryRadiusMiles(new BigDecimal("6.00"));
		settings.setPreparationTimeMinutes(20);
		settings.setDeliveryPricingMode(DeliveryPricingMode.RADIUS_BANDS);
		settings.setBaseDeliveryRadiusMiles(new BigDecimal("3.00"));
		settings.setBaseDeliveryFeePence(200);
		settings.setExtraMileFeePence(100);
		settings.setFreeDeliveryThresholdPence(null);
		settings.setMinimumDeliveryOrderPence(null);
		fulfilmentSettingsRepository.saveAndFlush(settings);
	}

	private void assertRadiusDeliveryFee(double distanceMiles, int expectedFeePence, int expectedTotalPence)
			throws Exception {
		enableRadiusDelivery();
		mockPostcode("DT1 1AA", distanceMiles);
		when(routeDurationProvider.estimateDrivingDuration(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
				.thenReturn(Optional.empty());
		MenuItem margherita = item("pizza-margherita");

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(deliveryOrderPayload("""
								[
								  { "type": "MENU_ITEM", "menuItemId": %d, "quantity": 1 }
								]
								""".formatted(margherita.getId()), "DT1 1AA")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.subtotalPence").value(899))
				.andExpect(jsonPath("$.deliveryFeePence").value(expectedFeePence))
				.andExpect(jsonPath("$.totalPence").value(expectedTotalPence));
	}

	private void mockPostcode(String postcode, double distanceMiles) {
		when(postcodeGeocoder.geocode(postcode))
				.thenReturn(Optional.of(new GeocodedPostcode(postcode, pointEast(distanceMiles))));
	}

	private GeoCoordinates pointEast(double miles) {
		GeoCoordinates basilico = new GeoCoordinates(50.71405, -2.43819);
		double longitudeDelta = miles / (69.172 * Math.cos(Math.toRadians(basilico.latitude())));
		return new GeoCoordinates(basilico.latitude(), basilico.longitude() + longitudeDelta);
	}

	private FulfilmentSettings fulfilmentSettings() {
		return fulfilmentSettingsRepository.findFirstByOrderByIdAsc().orElseThrow();
	}

	private MenuItem item(String slug) {
		return menuItemRepository.findBySlug(slug)
				.orElseThrow();
	}

	private MenuCustomizer customizer() {
		return customizerRepository.findBySlugAndActiveTrue("create-your-own")
				.orElseThrow();
	}

	private org.springframework.test.web.servlet.request.RequestPostProcessor adminUser() {
		return user("owner@basilico.test").roles("OWNER");
	}
}
