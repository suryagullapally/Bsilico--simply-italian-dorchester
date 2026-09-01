package com.basilico.backend.fulfilment;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.basilico.backend.fulfilment.entity.DeliveryAreaMode;
import com.basilico.backend.fulfilment.entity.DeliveryPostcodeRule;
import com.basilico.backend.fulfilment.entity.DeliveryPricingMode;
import com.basilico.backend.fulfilment.entity.FulfilmentSettings;
import com.basilico.backend.fulfilment.repository.DeliveryPostcodeRuleRepository;
import com.basilico.backend.fulfilment.repository.FulfilmentSettingsRepository;
import com.basilico.backend.fulfilment.service.DeliveryProviderException;
import com.basilico.backend.fulfilment.service.GeoCoordinates;
import com.basilico.backend.fulfilment.service.GeocodedPostcode;
import com.basilico.backend.fulfilment.service.PostcodeGeocoder;
import com.basilico.backend.fulfilment.service.RouteDuration;
import com.basilico.backend.fulfilment.service.RouteDurationProvider;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FulfilmentApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FulfilmentSettingsRepository settingsRepository;

	@Autowired
	private DeliveryPostcodeRuleRepository postcodeRuleRepository;

	@Autowired
	private EntityManager entityManager;

	@MockitoBean
	private PostcodeGeocoder postcodeGeocoder;

	@MockitoBean
	private RouteDurationProvider routeDurationProvider;

	@BeforeEach
	void resetProviders() {
		resetFulfilmentSettings();
		reset(postcodeGeocoder, routeDurationProvider);
		when(routeDurationProvider.isConfigured()).thenReturn(true);
		when(routeDurationProvider.estimateDrivingDuration(any(), any())).thenReturn(Optional.empty());
	}

	@Test
	void publicOptionsExposeSafeDefaultFulfilmentState() throws Exception {
		mockMvc.perform(get("/api/fulfilment/options"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.collectionEnabled").value(true))
				.andExpect(jsonPath("$.deliveryEnabled").value(false))
				.andExpect(jsonPath("$.minimumDeliveryOrderPence").doesNotExist())
				.andExpect(jsonPath("$.deliveryFeePence").doesNotExist())
				.andExpect(jsonPath("$.freeDeliveryThresholdPence").doesNotExist())
				.andExpect(jsonPath("$.deliveryAreaMode").value("RADIUS"))
				.andExpect(jsonPath("$.restaurantPostcode").value("DT1 1TT"))
				.andExpect(jsonPath("$.deliveryRadiusMiles").value(6.00))
				.andExpect(jsonPath("$.preparationTimeMinutes").value(20))
				.andExpect(jsonPath("$.deliveryPricingMode").value("RADIUS_BANDS"))
				.andExpect(jsonPath("$.baseDeliveryRadiusMiles").value(3.00))
				.andExpect(jsonPath("$.baseDeliveryFeePence").value(200))
				.andExpect(jsonPath("$.extraMileFeePence").value(100));
	}

	@Test
	void publicDeliveryCheckNormalizesPostcodeAndReportsDisabledDelivery() throws Exception {
		mockMvc.perform(post("/api/fulfilment/check-delivery")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"postcode\":\" dt1 1tt \"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eligible").value(false))
				.andExpect(jsonPath("$.normalizedPostcode").value("DT1 1TT"))
				.andExpect(jsonPath("$.message").value("Delivery is currently unavailable. Please choose collection."));
	}

	@Test
	void publicDeliveryCheckMatchesActivePostcodeRuleWithCompactInput() throws Exception {
		enableDelivery(250, null, null);
		postcodeRuleRepository.saveAndFlush(new DeliveryPostcodeRule("DT1 1", 1));

		mockMvc.perform(post("/api/fulfilment/check-delivery")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"postcode\":\"DT11TT\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eligible").value(true))
				.andExpect(jsonPath("$.normalizedPostcode").value("DT1 1TT"))
				.andExpect(jsonPath("$.message").value("Delivery available"));
	}

	@Test
	void publicDeliveryCheckIgnoresInactivePostcodeRules() throws Exception {
		enableDelivery(250, null, null);
		DeliveryPostcodeRule rule = postcodeRuleRepository.saveAndFlush(new DeliveryPostcodeRule("DT1", 1));
		rule.setActive(false);
		postcodeRuleRepository.saveAndFlush(rule);

		mockMvc.perform(post("/api/fulfilment/check-delivery")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"postcode\":\"DT1 1TT\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eligible").value(false))
				.andExpect(jsonPath("$.message").value("Delivery is not currently available for this address."));
	}

	@Test
	void adminFulfilmentEndpointsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/admin/fulfilment/settings"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void adminFulfilmentMutationsRequireCsrf() throws Exception {
		mockMvc.perform(put("/api/admin/fulfilment/settings")
						.with(adminUser())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "collectionEnabled": true,
								  "deliveryEnabled": false,
								  "minimumDeliveryOrderPence": null,
								  "deliveryFeePence": null,
								  "freeDeliveryThresholdPence": null,
								  "deliveryAreaMode": "RADIUS",
								  "restaurantPostcode": "DT1 1TT",
								  "restaurantLatitude": 50.71405,
								  "restaurantLongitude": -2.43819,
								  "deliveryRadiusMiles": 6.0,
								  "preparationTimeMinutes": 20,
								  "deliveryPricingMode": "RADIUS_BANDS",
								  "baseDeliveryRadiusMiles": 3.0,
								  "baseDeliveryFeePence": 200,
								  "extraMileFeePence": 100
								}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void authenticatedAdminCanUpdateFulfilmentSettingsWithCsrf() throws Exception {
		mockMvc.perform(put("/api/admin/fulfilment/settings")
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "collectionEnabled": true,
								  "deliveryEnabled": false,
								  "minimumDeliveryOrderPence": 1200,
								  "deliveryFeePence": 250,
								  "freeDeliveryThresholdPence": 3000,
								  "deliveryAreaMode": "RADIUS",
								  "restaurantPostcode": "DT1 1TT",
								  "restaurantLatitude": 50.71405,
								  "restaurantLongitude": -2.43819,
								  "deliveryRadiusMiles": 6.0,
								  "preparationTimeMinutes": 20,
								  "deliveryPricingMode": "RADIUS_BANDS",
								  "baseDeliveryRadiusMiles": 3.0,
								  "baseDeliveryFeePence": 200,
								  "extraMileFeePence": 100
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.collectionEnabled").value(true))
				.andExpect(jsonPath("$.deliveryEnabled").value(false))
				.andExpect(jsonPath("$.minimumDeliveryOrderPence").value(1200))
				.andExpect(jsonPath("$.deliveryFeePence").value(250))
				.andExpect(jsonPath("$.freeDeliveryThresholdPence").value(3000));
	}

	@Test
	void authenticatedAdminCanEnableRadiusDeliveryWithoutPostcodeRulesAndPersistIt() throws Exception {
		postcodeRuleRepository.deleteAll();

		mockMvc.perform(put("/api/admin/fulfilment/settings")
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "collectionEnabled": true,
								  "deliveryEnabled": true,
								  "minimumDeliveryOrderPence": null,
								  "deliveryFeePence": null,
								  "freeDeliveryThresholdPence": null,
								  "deliveryAreaMode": "RADIUS",
								  "restaurantPostcode": "DT1 1TT",
								  "restaurantLatitude": 50.71405,
								  "restaurantLongitude": -2.43819,
								  "deliveryRadiusMiles": 6.0,
								  "preparationTimeMinutes": 20,
								  "deliveryPricingMode": "RADIUS_BANDS",
								  "baseDeliveryRadiusMiles": 3.0,
								  "baseDeliveryFeePence": 200,
								  "extraMileFeePence": 100
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.collectionEnabled").value(true))
				.andExpect(jsonPath("$.deliveryEnabled").value(true))
				.andExpect(jsonPath("$.deliveryAreaMode").value("RADIUS"))
				.andExpect(jsonPath("$.deliveryPricingMode").value("RADIUS_BANDS"));

		entityManager.clear();
		FulfilmentSettings persisted = settingsRepository.findFirstByOrderByIdAsc().orElseThrow();
		assertThat(persisted.isDeliveryEnabled()).isTrue();

		mockMvc.perform(get("/api/admin/fulfilment/settings")
						.with(adminUser()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.deliveryEnabled").value(true));

		mockMvc.perform(get("/api/fulfilment/options"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.deliveryEnabled").value(true));
	}

	@Test
	void authenticatedAdminCanDisableRadiusDeliveryAndPersistIt() throws Exception {
		enableRadiusDelivery();

		mockMvc.perform(put("/api/admin/fulfilment/settings")
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "collectionEnabled": true,
								  "deliveryEnabled": false,
								  "minimumDeliveryOrderPence": null,
								  "deliveryFeePence": null,
								  "freeDeliveryThresholdPence": null,
								  "deliveryAreaMode": "RADIUS",
								  "restaurantPostcode": "DT1 1TT",
								  "restaurantLatitude": 50.71405,
								  "restaurantLongitude": -2.43819,
								  "deliveryRadiusMiles": 6.0,
								  "preparationTimeMinutes": 20,
								  "deliveryPricingMode": "RADIUS_BANDS",
								  "baseDeliveryRadiusMiles": 3.0,
								  "baseDeliveryFeePence": 200,
								  "extraMileFeePence": 100
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.collectionEnabled").value(true))
				.andExpect(jsonPath("$.deliveryEnabled").value(false));

		entityManager.clear();
		FulfilmentSettings persisted = settingsRepository.findFirstByOrderByIdAsc().orElseThrow();
		assertThat(persisted.isDeliveryEnabled()).isFalse();

		mockMvc.perform(get("/api/fulfilment/options"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.deliveryEnabled").value(false));
	}

	@Test
	void adminCannotEnableRadiusDeliveryWithMalformedRadiusConfiguration() throws Exception {
		mockMvc.perform(put("/api/admin/fulfilment/settings")
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "collectionEnabled": true,
								  "deliveryEnabled": true,
								  "minimumDeliveryOrderPence": null,
								  "deliveryFeePence": null,
								  "freeDeliveryThresholdPence": null,
								  "deliveryAreaMode": "RADIUS",
								  "restaurantPostcode": "DT1 1TT",
								  "restaurantLatitude": null,
								  "restaurantLongitude": -2.43819,
								  "deliveryRadiusMiles": 6.0,
								  "preparationTimeMinutes": 20,
								  "deliveryPricingMode": "RADIUS_BANDS",
								  "baseDeliveryRadiusMiles": 3.0,
								  "baseDeliveryFeePence": 200,
								  "extraMileFeePence": 100
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"Restaurant location, delivery radius and preparation time are required for radius delivery."));
	}

	@Test
	void adminCannotEnableDeliveryWithoutActivePostcodeRule() throws Exception {
		mockMvc.perform(put("/api/admin/fulfilment/settings")
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "collectionEnabled": true,
								  "deliveryEnabled": true,
								  "minimumDeliveryOrderPence": null,
								  "deliveryFeePence": 250,
								  "freeDeliveryThresholdPence": null,
								  "deliveryAreaMode": "POSTCODE_RULES",
								  "restaurantPostcode": "DT1 1TT",
								  "restaurantLatitude": 50.71405,
								  "restaurantLongitude": -2.43819,
								  "deliveryRadiusMiles": 6.0,
								  "preparationTimeMinutes": 20,
								  "deliveryPricingMode": "FLAT_FEE",
								  "baseDeliveryRadiusMiles": 3.0,
								  "baseDeliveryFeePence": 200,
								  "extraMileFeePence": 100
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"Delivery cannot be enabled until at least one delivery postcode rule is active."));
	}

	@Test
	void publicDeliveryQuoteReturnsRadiusFeeAndEtaForInsidePostcode() throws Exception {
		enableRadiusDelivery();
		when(postcodeGeocoder.geocode("DT1 1TT"))
				.thenReturn(Optional.of(new GeocodedPostcode("DT1 1TT", pointEast(4.3))));
		when(routeDurationProvider.estimateDrivingDuration(any(), any()))
				.thenReturn(Optional.of(new RouteDuration(11, 5.0)));

		mockMvc.perform(post("/api/fulfilment/delivery-quote")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"postcode\":\"dt11tt\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eligible").value(true))
				.andExpect(jsonPath("$.source").value("POSTCODE"))
				.andExpect(jsonPath("$.normalizedPostcode").value("DT1 1TT"))
				.andExpect(jsonPath("$.deliveryFeePence").value(400))
				.andExpect(jsonPath("$.preparationMinutes").value(20))
				.andExpect(jsonPath("$.travelMinutes").value(11))
				.andExpect(jsonPath("$.estimatedDeliveryMinutes").value(35))
				.andExpect(jsonPath("$.message").value("Delivery available"));
	}

	@Test
	void publicDeliveryQuoteRejectsOutsideRadiusPostcode() throws Exception {
		enableRadiusDelivery();
		when(postcodeGeocoder.geocode("DT9 9ZZ"))
				.thenReturn(Optional.of(new GeocodedPostcode("DT9 9ZZ", pointEast(6.2))));

		mockMvc.perform(post("/api/fulfilment/delivery-quote")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"postcode\":\"DT9 9ZZ\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eligible").value(false))
				.andExpect(jsonPath("$.distanceMiles").exists())
				.andExpect(jsonPath("$.deliveryFeePence").doesNotExist())
				.andExpect(jsonPath("$.estimatedDeliveryMinutes").doesNotExist())
				.andExpect(jsonPath("$.message").value("Sorry, we currently deliver within 6 miles of Basilico."));
	}

	@Test
	void publicDeliveryQuoteHandlesInvalidPostcodeWithoutGuessing() throws Exception {
		enableRadiusDelivery();
		when(postcodeGeocoder.geocode("BAD")).thenReturn(Optional.empty());

		mockMvc.perform(post("/api/fulfilment/delivery-quote")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"postcode\":\"BAD\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eligible").value(false))
				.andExpect(jsonPath("$.message").value(
						"We couldn't verify this postcode. Please check it and try again, or choose collection."));
	}

	@Test
	void publicDeliveryQuoteHandlesPostcodeProviderFailureSafely() throws Exception {
		enableRadiusDelivery();
		when(postcodeGeocoder.geocode("DT1 1TT"))
				.thenThrow(new DeliveryProviderException("postcode service unavailable"));

		mockMvc.perform(post("/api/fulfilment/delivery-quote")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"postcode\":\"DT1 1TT\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eligible").value(false))
				.andExpect(jsonPath("$.message").value(
						"We couldn't verify this postcode. Please check it and try again, or choose collection."));
	}

	@Test
	void publicDeliveryQuoteSupportsGpsInsideRadiusWithoutReturningCoordinates() throws Exception {
		enableRadiusDelivery();
		when(routeDurationProvider.estimateDrivingDuration(any(), any()))
				.thenReturn(Optional.empty());

		mockMvc.perform(post("/api/fulfilment/delivery-quote")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "latitude": 50.71405,
								  "longitude": -2.35000
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eligible").value(true))
				.andExpect(jsonPath("$.source").value("GEOLOCATION"))
				.andExpect(jsonPath("$.normalizedPostcode").doesNotExist())
				.andExpect(jsonPath("$.distanceMiles").exists())
				.andExpect(jsonPath("$.deliveryFeePence").value(300))
				.andExpect(jsonPath("$.travelMinutes").doesNotExist())
				.andExpect(jsonPath("$.estimatedDeliveryMinutes").doesNotExist());
	}

	@Test
	void publicDeliveryQuoteRejectsGpsOutsideRadius() throws Exception {
		enableRadiusDelivery();

		mockMvc.perform(post("/api/fulfilment/delivery-quote")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "latitude": 50.71405,
								  "longitude": -2.30000
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eligible").value(false))
				.andExpect(jsonPath("$.deliveryFeePence").doesNotExist())
				.andExpect(jsonPath("$.message").value("Sorry, we currently deliver within 6 miles of Basilico."));
	}

	@Test
	void outsideCurrentLocationDoesNotBlockInsideDeliveryPostcodeQuote() throws Exception {
		enableRadiusDelivery();
		when(postcodeGeocoder.geocode("DT1 1TT"))
				.thenReturn(Optional.of(new GeocodedPostcode("DT1 1TT", pointEast(2.8))));

		mockMvc.perform(post("/api/fulfilment/delivery-quote")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "latitude": 51.50740,
								  "longitude": -0.12780
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eligible").value(false))
				.andExpect(jsonPath("$.source").value("GEOLOCATION"))
				.andExpect(jsonPath("$.message").value("Sorry, we currently deliver within 6 miles of Basilico."));

		mockMvc.perform(post("/api/fulfilment/delivery-quote")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"postcode\":\"dt11tt\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eligible").value(true))
				.andExpect(jsonPath("$.source").value("POSTCODE"))
				.andExpect(jsonPath("$.normalizedPostcode").value("DT1 1TT"))
				.andExpect(jsonPath("$.deliveryFeePence").value(200))
				.andExpect(jsonPath("$.message").value("Delivery available"));
	}

	@Test
	void publicDeliveryQuoteRejectsMalformedLocationModes() throws Exception {
		mockMvc.perform(post("/api/fulfilment/delivery-quote")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "postcode": "DT1 1TT",
								  "latitude": 50.71405,
								  "longitude": -2.43819
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"));
	}

	@Test
	void authenticatedAdminCanCreateAndDeactivatePostcodeRule() throws Exception {
		mockMvc.perform(post("/api/admin/fulfilment/postcode-rules")
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "postcodePattern": " dt1 1 ",
								  "displayOrder": 2
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.postcodePattern").value("DT1 1"))
				.andExpect(jsonPath("$.active").value(true));

		Long ruleId = postcodeRuleRepository.findByPostcodePattern("DT1 1").orElseThrow().getId();

		mockMvc.perform(patch("/api/admin/fulfilment/postcode-rules/{id}/active", ruleId)
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"active\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));
	}

	@Test
	void adminRejectsMalformedPostcodeRule() throws Exception {
		mockMvc.perform(post("/api/admin/fulfilment/postcode-rules")
						.with(adminUser())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"postcodePattern\":\"!!!\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"));
	}

	private void enableDelivery(int deliveryFeePence, Integer minimumDeliveryOrderPence,
			Integer freeDeliveryThresholdPence) {
		FulfilmentSettings settings = settingsRepository.findFirstByOrderByIdAsc().orElseThrow();
		settings.setDeliveryAreaMode(DeliveryAreaMode.POSTCODE_RULES);
		settings.setDeliveryPricingMode(DeliveryPricingMode.FLAT_FEE);
		settings.setDeliveryEnabled(true);
		settings.setDeliveryFeePence(deliveryFeePence);
		settings.setMinimumDeliveryOrderPence(minimumDeliveryOrderPence);
		settings.setFreeDeliveryThresholdPence(freeDeliveryThresholdPence);
		settingsRepository.saveAndFlush(settings);
	}

	private void resetFulfilmentSettings() {
		postcodeRuleRepository.deleteAll();
		FulfilmentSettings settings = settingsRepository.findFirstByOrderByIdAsc().orElseThrow();
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
		settingsRepository.saveAndFlush(settings);
	}

	private void enableRadiusDelivery() {
		FulfilmentSettings settings = settingsRepository.findFirstByOrderByIdAsc().orElseThrow();
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
		settingsRepository.saveAndFlush(settings);
	}

	private GeoCoordinates pointEast(double miles) {
		GeoCoordinates basilico = new GeoCoordinates(50.71405, -2.43819);
		double longitudeDelta = miles / (69.172 * Math.cos(Math.toRadians(basilico.latitude())));
		return new GeoCoordinates(basilico.latitude(), basilico.longitude() + longitudeDelta);
	}

	private org.springframework.test.web.servlet.request.RequestPostProcessor adminUser() {
		return user("owner@basilico.test").roles("OWNER");
	}
}
