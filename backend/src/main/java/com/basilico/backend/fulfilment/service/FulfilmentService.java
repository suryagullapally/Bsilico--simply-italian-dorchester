package com.basilico.backend.fulfilment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.basilico.backend.common.error.BadRequestException;
import com.basilico.backend.common.error.ConflictException;
import com.basilico.backend.common.error.ResourceNotFoundException;
import com.basilico.backend.fulfilment.dto.AdminFulfilmentSettingsResponse;
import com.basilico.backend.fulfilment.dto.CreateDeliveryPostcodeRuleRequest;
import com.basilico.backend.fulfilment.dto.DeliveryEligibilityResponse;
import com.basilico.backend.fulfilment.dto.DeliveryPostcodeRuleResponse;
import com.basilico.backend.fulfilment.dto.DeliveryQuoteRequest;
import com.basilico.backend.fulfilment.dto.DeliveryQuoteResponse;
import com.basilico.backend.fulfilment.dto.FulfilmentOptionsResponse;
import com.basilico.backend.fulfilment.dto.PostcodeRuleActiveUpdateRequest;
import com.basilico.backend.fulfilment.dto.UpdateFulfilmentSettingsRequest;
import com.basilico.backend.fulfilment.entity.DeliveryAreaMode;
import com.basilico.backend.fulfilment.entity.DeliveryPostcodeRule;
import com.basilico.backend.fulfilment.entity.DeliveryPricingMode;
import com.basilico.backend.fulfilment.entity.FulfilmentSettings;
import com.basilico.backend.fulfilment.repository.DeliveryPostcodeRuleRepository;
import com.basilico.backend.fulfilment.repository.FulfilmentSettingsRepository;
import com.basilico.backend.order.dto.DeliveryAddressRequest;
import com.basilico.backend.order.entity.FulfilmentType;

@Service
public class FulfilmentService {

	private static final String DELIVERY_DISABLED_MESSAGE =
			"Delivery is currently unavailable. Please choose collection.";
	private static final String DELIVERY_NOT_AVAILABLE_MESSAGE =
			"Delivery is not currently available for this address.";
	private static final String POSTCODE_UNAVAILABLE_MESSAGE =
			"Sorry, we do not currently deliver to this postcode. Please choose collection or another delivery address.";
	private static final String POSTCODE_VERIFY_MESSAGE =
			"We couldn't verify this postcode. Please check it and try again, or choose collection.";
	private static final String OUTSIDE_RADIUS_MESSAGE =
			"Sorry, we currently deliver within 6 miles of Basilico.";
	private static final String POSTCODE_PATTERN_REGEX = "^[A-Z0-9][A-Z0-9 ]{1,9}$";

	private final FulfilmentSettingsRepository settingsRepository;
	private final DeliveryPostcodeRuleRepository postcodeRuleRepository;
	private final PostcodeGeocoder postcodeGeocoder;
	private final RouteDurationProvider routeDurationProvider;
	private final DeliveryDistanceCalculator distanceCalculator;
	private final DeliveryFeeCalculator feeCalculator;

	public FulfilmentService(FulfilmentSettingsRepository settingsRepository,
			DeliveryPostcodeRuleRepository postcodeRuleRepository,
			PostcodeGeocoder postcodeGeocoder,
			RouteDurationProvider routeDurationProvider,
			DeliveryDistanceCalculator distanceCalculator,
			DeliveryFeeCalculator feeCalculator) {
		this.settingsRepository = settingsRepository;
		this.postcodeRuleRepository = postcodeRuleRepository;
		this.postcodeGeocoder = postcodeGeocoder;
		this.routeDurationProvider = routeDurationProvider;
		this.distanceCalculator = distanceCalculator;
		this.feeCalculator = feeCalculator;
	}

	@Transactional(readOnly = true)
	public FulfilmentOptionsResponse getPublicOptions() {
		return toPublicOptions(settings());
	}

	@Transactional(readOnly = true)
	public DeliveryEligibilityResponse checkDelivery(String postcode) {
		String normalizedPostcode = normalizePostcode(postcode);
		DeliveryQuoteResponse quote = quoteDelivery(new DeliveryQuoteRequest(normalizedPostcode, null, null));
		String message = quote.message() == null && quote.eligible() ? "Delivery available" : quote.message();
		return new DeliveryEligibilityResponse(quote.eligible(), normalizedPostcode, message);
	}

	@Transactional(readOnly = true)
	public DeliveryQuoteResponse quoteDelivery(DeliveryQuoteRequest request) {
		FulfilmentSettings settings = settings();
		QuoteDestination destination = resolveQuoteDestination(request);

		if (!settings.isDeliveryEnabled()) {
			return unavailable(destination.source(), destination.normalizedPostcode(), DELIVERY_DISABLED_MESSAGE);
		}

		if (settings.getDeliveryAreaMode() == DeliveryAreaMode.RADIUS) {
			return quoteRadiusDelivery(settings, destination);
		}

		return quotePostcodeRuleDelivery(settings, destination);
	}

	@Transactional(readOnly = true)
	public FulfilmentPricing calculatePricing(FulfilmentType fulfilmentType, DeliveryAddressRequest deliveryAddress,
			int subtotalPence) {
		FulfilmentSettings settings = settings();
		if (fulfilmentType == FulfilmentType.COLLECTION) {
			if (!settings.isCollectionEnabled()) {
				throw new ConflictException("Collection is currently unavailable.");
			}
			return FulfilmentPricing.collection(subtotalPence);
		}

		if (fulfilmentType != FulfilmentType.DELIVERY) {
			throw new BadRequestException("Fulfilment type is invalid");
		}

		if (!settings.isDeliveryEnabled()) {
			throw new ConflictException(DELIVERY_DISABLED_MESSAGE);
		}

		if (deliveryAddress == null) {
			throw new BadRequestException("Delivery address is required");
		}

		validateDeliveryAddress(deliveryAddress);
		String normalizedPostcode = normalizePostcode(deliveryAddress.postcode());

		if (settings.getDeliveryAreaMode() == DeliveryAreaMode.RADIUS) {
			return calculateRadiusPricing(settings, normalizedPostcode, subtotalPence);
		}

		return calculatePostcodeRulePricing(settings, normalizedPostcode, subtotalPence);
	}

	@Transactional(readOnly = true)
	public AdminFulfilmentSettingsResponse getAdminSettings() {
		return toAdminSettings(settings());
	}

	@Transactional
	public AdminFulfilmentSettingsResponse updateSettings(UpdateFulfilmentSettingsRequest request) {
		FulfilmentSettings settings = settings();
		validateSettingsRequest(request);

		settings.setCollectionEnabled(request.collectionEnabled());
		settings.setDeliveryEnabled(request.deliveryEnabled());
		settings.setMinimumDeliveryOrderPence(request.minimumDeliveryOrderPence());
		settings.setDeliveryFeePence(request.deliveryFeePence());
		settings.setFreeDeliveryThresholdPence(request.freeDeliveryThresholdPence());
		settings.setDeliveryAreaMode(request.deliveryAreaMode());
		settings.setRestaurantPostcode(normalizedOptionalPostcode(request.restaurantPostcode()));
		settings.setRestaurantLatitude(request.restaurantLatitude());
		settings.setRestaurantLongitude(request.restaurantLongitude());
		settings.setDeliveryRadiusMiles(request.deliveryRadiusMiles());
		settings.setPreparationTimeMinutes(request.preparationTimeMinutes());
		settings.setDeliveryPricingMode(request.deliveryPricingMode());
		settings.setBaseDeliveryRadiusMiles(request.baseDeliveryRadiusMiles());
		settings.setBaseDeliveryFeePence(request.baseDeliveryFeePence());
		settings.setExtraMileFeePence(request.extraMileFeePence());
		return toAdminSettings(settingsRepository.saveAndFlush(settings));
	}

	@Transactional(readOnly = true)
	public List<DeliveryPostcodeRuleResponse> getAdminPostcodeRules() {
		return postcodeRuleRepository.findAllByOrderByDisplayOrderAsc()
				.stream()
				.map(this::toPostcodeRuleResponse)
				.toList();
	}

	@Transactional
	public DeliveryPostcodeRuleResponse createPostcodeRule(CreateDeliveryPostcodeRuleRequest request) {
		String normalizedPattern = normalizePostcodePattern(request.postcodePattern());
		if (postcodeRuleRepository.existsByPostcodePattern(normalizedPattern)) {
			throw new ConflictException("Delivery postcode rule already exists.");
		}

		int displayOrder = request.displayOrder() == null
				? postcodeRuleRepository.findAllByOrderByDisplayOrderAsc().size() + 1
				: request.displayOrder();
		if (displayOrder < 1) {
			throw new BadRequestException("Display order must be at least 1.");
		}

		return toPostcodeRuleResponse(postcodeRuleRepository.save(
				new DeliveryPostcodeRule(normalizedPattern, displayOrder)
		));
	}

	@Transactional
	public DeliveryPostcodeRuleResponse updatePostcodeRuleActive(Long id, PostcodeRuleActiveUpdateRequest request) {
		DeliveryPostcodeRule rule = postcodeRuleRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Delivery postcode rule not found"));
		rule.setActive(request.active());
		return toPostcodeRuleResponse(rule);
	}

	public String normalizePostcode(String value) {
		if (value == null || value.isBlank()) {
			throw new BadRequestException("Delivery postcode is required");
		}

		String normalized = value.trim().toUpperCase(Locale.UK).replaceAll("\\s+", " ");
		String compact = compact(normalized);
		if (compact.matches("^[A-Z]{1,2}\\d[A-Z\\d]?\\d[A-Z]{2}$")) {
			return compact.substring(0, compact.length() - 3) + " " + compact.substring(compact.length() - 3);
		}

		return normalized;
	}

	private DeliveryQuoteResponse quoteRadiusDelivery(FulfilmentSettings settings, QuoteDestination destination) {
		GeoCoordinates destinationCoordinates = destination.coordinates();
		if (destinationCoordinates == null) {
			Optional<GeocodedPostcode> geocodedPostcode = geocode(destination.normalizedPostcode());
			if (geocodedPostcode.isEmpty()) {
				return unavailable(destination.source(), destination.normalizedPostcode(), POSTCODE_VERIFY_MESSAGE);
			}
			destination = new QuoteDestination(
					destination.source(),
					geocodedPostcode.get().normalizedPostcode(),
					geocodedPostcode.get().coordinates()
			);
		}

		GeoCoordinates restaurant = restaurantCoordinates(settings);
		double distanceMiles = distanceCalculator.distanceMiles(restaurant, destination.coordinates());
		Double displayDistance = roundedMiles(distanceMiles);
		if (!feeCalculator.isInsideRadius(distanceMiles, settings)) {
			return new DeliveryQuoteResponse(
					false,
					destination.source(),
					destination.normalizedPostcode(),
					displayDistance,
					null,
					settings.getPreparationTimeMinutes(),
					null,
					null,
					OUTSIDE_RADIUS_MESSAGE
			);
		}

		int deliveryFeePence = deliveryFeeForMode(settings, distanceMiles);
		int preparationMinutes = requiredPreparationMinutes(settings);
		Optional<RouteDuration> routeDuration = routeDurationProvider.estimateDrivingDuration(
				restaurant,
				destination.coordinates()
		);
		Integer travelMinutes = routeDuration.map(RouteDuration::travelMinutes).orElse(null);
		Integer estimatedDeliveryMinutes = travelMinutes == null
				? null
				: roundUpToFive(preparationMinutes + travelMinutes);

		return new DeliveryQuoteResponse(
				true,
				destination.source(),
				destination.normalizedPostcode(),
				displayDistance,
				deliveryFeePence,
				preparationMinutes,
				travelMinutes,
				estimatedDeliveryMinutes,
				"Delivery available"
		);
	}

	private DeliveryQuoteResponse quotePostcodeRuleDelivery(FulfilmentSettings settings, QuoteDestination destination) {
		if (!"POSTCODE".equals(destination.source())) {
			return unavailable(destination.source(), null, "Delivery checks currently require a postcode.");
		}

		LegacyDeliveryCheck legacyCheck = legacyDeliveryCheck(settings, destination.normalizedPostcode());
		return new DeliveryQuoteResponse(
				legacyCheck.eligible(),
				destination.source(),
				destination.normalizedPostcode(),
				null,
				legacyCheck.eligible() ? settings.getDeliveryFeePence() : null,
				null,
				null,
				null,
				legacyCheck.message()
		);
	}

	private FulfilmentPricing calculateRadiusPricing(FulfilmentSettings settings, String normalizedPostcode,
			int subtotalPence) {
		Optional<GeocodedPostcode> geocodedPostcode = geocode(normalizedPostcode);
		if (geocodedPostcode.isEmpty()) {
			throw new ConflictException(POSTCODE_VERIFY_MESSAGE);
		}

		GeoCoordinates restaurant = restaurantCoordinates(settings);
		GeoCoordinates destination = geocodedPostcode.get().coordinates();
		double distanceMiles = distanceCalculator.distanceMiles(restaurant, destination);
		if (!feeCalculator.isInsideRadius(distanceMiles, settings)) {
			throw new ConflictException(OUTSIDE_RADIUS_MESSAGE);
		}

		validateMinimumDeliveryOrder(settings, subtotalPence);
		int deliveryFeePence = deliveryFeeForMode(settings, distanceMiles);
		int preparationMinutes = requiredPreparationMinutes(settings);
		Optional<RouteDuration> routeDuration = routeDurationProvider.estimateDrivingDuration(restaurant, destination);
		Integer travelMinutes = routeDuration.map(RouteDuration::travelMinutes).orElse(null);
		Integer estimatedDeliveryMinutes = travelMinutes == null
				? null
				: roundUpToFive(preparationMinutes + travelMinutes);

		return new FulfilmentPricing(
				deliveryFeePence,
				Math.toIntExact((long) subtotalPence + deliveryFeePence),
				geocodedPostcode.get().normalizedPostcode(),
				roundedMilesBigDecimal(distanceMiles),
				preparationMinutes,
				travelMinutes,
				estimatedDeliveryMinutes
		);
	}

	private FulfilmentPricing calculatePostcodeRulePricing(FulfilmentSettings settings, String normalizedPostcode,
			int subtotalPence) {
		LegacyDeliveryCheck legacyCheck = legacyDeliveryCheck(settings, normalizedPostcode);
		if (!legacyCheck.eligible()) {
			throw new ConflictException(legacyCheck.message());
		}

		validateMinimumDeliveryOrder(settings, subtotalPence);

		Integer configuredDeliveryFee = settings.getDeliveryFeePence();
		if (configuredDeliveryFee == null) {
			throw new ConflictException("Delivery pricing is not currently configured.");
		}

		int deliveryFeePence = configuredDeliveryFee;
		Integer freeDeliveryThresholdPence = settings.getFreeDeliveryThresholdPence();
		if (freeDeliveryThresholdPence != null && subtotalPence >= freeDeliveryThresholdPence) {
			deliveryFeePence = 0;
		}

		return new FulfilmentPricing(
				deliveryFeePence,
				Math.toIntExact((long) subtotalPence + deliveryFeePence),
				normalizedPostcode,
				null,
				null,
				null,
				null
		);
	}

	private int deliveryFeeForMode(FulfilmentSettings settings, double distanceMiles) {
		if (settings.getDeliveryPricingMode() == DeliveryPricingMode.RADIUS_BANDS) {
			return feeCalculator.calculateRadiusBandFeePence(distanceMiles, settings);
		}

		Integer configuredDeliveryFee = settings.getDeliveryFeePence();
		if (configuredDeliveryFee == null) {
			throw new ConflictException("Delivery pricing is not currently configured.");
		}

		return configuredDeliveryFee;
	}

	private LegacyDeliveryCheck legacyDeliveryCheck(FulfilmentSettings settings, String normalizedPostcode) {
		Integer configuredDeliveryFee = settings.getDeliveryFeePence();
		if (configuredDeliveryFee == null) {
			return new LegacyDeliveryCheck(false, "Delivery pricing is not currently configured.");
		}

		List<DeliveryPostcodeRule> activeRules = postcodeRuleRepository.findByActiveTrueOrderByDisplayOrderAsc();
		if (activeRules.isEmpty()) {
			return new LegacyDeliveryCheck(false, DELIVERY_NOT_AVAILABLE_MESSAGE);
		}

		if (!matchesAnyRule(normalizedPostcode, activeRules)) {
			return new LegacyDeliveryCheck(false, POSTCODE_UNAVAILABLE_MESSAGE);
		}

		return new LegacyDeliveryCheck(true, "Delivery available");
	}

	private QuoteDestination resolveQuoteDestination(DeliveryQuoteRequest request) {
		if (request == null) {
			throw new BadRequestException("Delivery location is required");
		}

		boolean hasPostcode = request.postcode() != null && !request.postcode().isBlank();
		boolean hasLatitude = request.latitude() != null;
		boolean hasLongitude = request.longitude() != null;
		if (hasPostcode == (hasLatitude || hasLongitude)) {
			throw new BadRequestException("Provide either a postcode or latitude and longitude.");
		}

		if (hasPostcode) {
			return new QuoteDestination("POSTCODE", normalizePostcode(request.postcode()), null);
		}

		if (!hasLatitude || !hasLongitude) {
			throw new BadRequestException("Latitude and longitude are both required.");
		}

		return new QuoteDestination("GEOLOCATION", null, new GeoCoordinates(request.latitude(), request.longitude()));
	}

	private Optional<GeocodedPostcode> geocode(String normalizedPostcode) {
		try {
			return postcodeGeocoder.geocode(normalizedPostcode);
		} catch (DeliveryProviderException exception) {
			return Optional.empty();
		}
	}

	private GeoCoordinates restaurantCoordinates(FulfilmentSettings settings) {
		if (settings.getRestaurantLatitude() == null || settings.getRestaurantLongitude() == null) {
			throw new ConflictException("Restaurant location is not configured.");
		}

		return new GeoCoordinates(settings.getRestaurantLatitude().doubleValue(),
				settings.getRestaurantLongitude().doubleValue());
	}

	private int requiredPreparationMinutes(FulfilmentSettings settings) {
		if (settings.getPreparationTimeMinutes() == null) {
			throw new ConflictException("Delivery preparation time is not configured.");
		}

		return settings.getPreparationTimeMinutes();
	}

	private void validateMinimumDeliveryOrder(FulfilmentSettings settings, int subtotalPence) {
		Integer minimumDeliveryOrderPence = settings.getMinimumDeliveryOrderPence();
		if (minimumDeliveryOrderPence != null && subtotalPence < minimumDeliveryOrderPence) {
			throw new ConflictException("Delivery orders need a food subtotal of at least "
					+ formatPence(minimumDeliveryOrderPence) + ".");
		}
	}

	private void validateSettingsRequest(UpdateFulfilmentSettingsRequest request) {
		if (request.deliveryEnabled() && request.deliveryAreaMode() == DeliveryAreaMode.POSTCODE_RULES) {
			if (request.deliveryFeePence() == null) {
				throw new ConflictException("Set a delivery charge before enabling delivery.");
			}
			if (!postcodeRuleRepository.existsByActiveTrue()) {
				throw new ConflictException("Delivery cannot be enabled until at least one delivery postcode rule is active.");
			}
		}

		if (request.deliveryEnabled() && request.deliveryAreaMode() == DeliveryAreaMode.RADIUS) {
			validateRadiusSettings(request);
		}
	}

	private void validateRadiusSettings(UpdateFulfilmentSettingsRequest request) {
		if (request.restaurantPostcode() == null || request.restaurantPostcode().isBlank()
				|| request.restaurantLatitude() == null
				|| request.restaurantLongitude() == null
				|| request.deliveryRadiusMiles() == null
				|| request.preparationTimeMinutes() == null) {
			throw new ConflictException("Restaurant location, delivery radius and preparation time are required for radius delivery.");
		}

		if (request.deliveryPricingMode() == DeliveryPricingMode.RADIUS_BANDS
				&& (request.baseDeliveryRadiusMiles() == null
						|| request.baseDeliveryFeePence() == null
						|| request.extraMileFeePence() == null)) {
			throw new ConflictException("Radius-band delivery pricing is not fully configured.");
		}
	}

	private void validateDeliveryAddress(DeliveryAddressRequest deliveryAddress) {
		trimRequired(deliveryAddress.line1(), "Delivery address line 1 is required");
		trimRequired(deliveryAddress.city(), "Delivery city is required");
	}

	private String normalizePostcodePattern(String value) {
		String normalizedPattern = normalizePostcode(value);
		if (!normalizedPattern.matches(POSTCODE_PATTERN_REGEX)
				|| normalizedPattern.chars().noneMatch(Character::isLetter)
				|| normalizedPattern.chars().noneMatch(Character::isDigit)) {
			throw new BadRequestException("Postcode rule must be a UK postcode prefix such as DT1 or DT1 1.");
		}

		return normalizedPattern;
	}

	private String normalizedOptionalPostcode(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		return normalizePostcode(value);
	}

	private boolean matchesAnyRule(String normalizedPostcode, List<DeliveryPostcodeRule> activeRules) {
		String compactPostcode = compact(normalizedPostcode);
		return activeRules.stream()
				.anyMatch(rule -> matchesRule(normalizedPostcode, compactPostcode, rule));
	}

	private boolean matchesRule(String normalizedPostcode, String compactPostcode, DeliveryPostcodeRule rule) {
		String normalizedPattern = normalizePostcode(rule.getPostcodePattern());
		String compactPattern = compact(normalizedPattern);
		return normalizedPostcode.startsWith(normalizedPattern) || compactPostcode.startsWith(compactPattern);
	}

	private String compact(String value) {
		return value.replace(" ", "");
	}

	private FulfilmentSettings settings() {
		return settingsRepository.findFirstByOrderByIdAsc()
				.orElseThrow(() -> new ConflictException("Fulfilment settings are not configured."));
	}

	private FulfilmentOptionsResponse toPublicOptions(FulfilmentSettings settings) {
		return new FulfilmentOptionsResponse(
				settings.isCollectionEnabled(),
				settings.isDeliveryEnabled(),
				settings.getMinimumDeliveryOrderPence(),
				settings.getDeliveryFeePence(),
				settings.getFreeDeliveryThresholdPence(),
				settings.getDeliveryAreaMode(),
				settings.getRestaurantPostcode(),
				settings.getDeliveryRadiusMiles(),
				settings.getPreparationTimeMinutes(),
				settings.getDeliveryPricingMode(),
				settings.getBaseDeliveryRadiusMiles(),
				settings.getBaseDeliveryFeePence(),
				settings.getExtraMileFeePence()
		);
	}

	private AdminFulfilmentSettingsResponse toAdminSettings(FulfilmentSettings settings) {
		return new AdminFulfilmentSettingsResponse(
				settings.getId(),
				settings.isCollectionEnabled(),
				settings.isDeliveryEnabled(),
				settings.getMinimumDeliveryOrderPence(),
				settings.getDeliveryFeePence(),
				settings.getFreeDeliveryThresholdPence(),
				settings.getDeliveryAreaMode(),
				settings.getRestaurantPostcode(),
				settings.getRestaurantLatitude(),
				settings.getRestaurantLongitude(),
				settings.getDeliveryRadiusMiles(),
				settings.getPreparationTimeMinutes(),
				settings.getDeliveryPricingMode(),
				settings.getBaseDeliveryRadiusMiles(),
				settings.getBaseDeliveryFeePence(),
				settings.getExtraMileFeePence(),
				routeDurationProvider.isConfigured(),
				settings.getCreatedAt(),
				settings.getUpdatedAt()
		);
	}

	private DeliveryPostcodeRuleResponse toPostcodeRuleResponse(DeliveryPostcodeRule rule) {
		return new DeliveryPostcodeRuleResponse(
				rule.getId(),
				rule.getPostcodePattern(),
				rule.isActive(),
				rule.getDisplayOrder(),
				rule.getCreatedAt(),
				rule.getUpdatedAt()
		);
	}

	private String trimRequired(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new BadRequestException(message);
		}

		return value.trim();
	}

	private String formatPence(int amountPence) {
		NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.UK);
		return formatter.format(BigDecimal.valueOf(amountPence, 2));
	}

	private DeliveryQuoteResponse unavailable(String source, String normalizedPostcode, String message) {
		return new DeliveryQuoteResponse(false, source, normalizedPostcode, null, null, null, null, null, message);
	}

	private Double roundedMiles(double value) {
		return roundedMilesBigDecimal(value).doubleValue();
	}

	private BigDecimal roundedMilesBigDecimal(double value) {
		return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
	}

	private int roundUpToFive(int minutes) {
		return Math.toIntExact(((long) minutes + 4L) / 5L * 5L);
	}

	private record QuoteDestination(
			String source,
			String normalizedPostcode,
			GeoCoordinates coordinates
	) {
	}

	private record LegacyDeliveryCheck(
			boolean eligible,
			String message
	) {
	}
}
