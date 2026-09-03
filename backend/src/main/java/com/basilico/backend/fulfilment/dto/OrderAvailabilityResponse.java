package com.basilico.backend.fulfilment.dto;

import java.util.List;

public record OrderAvailabilityResponse(
		String restaurantTimezone,
		boolean restaurantOpenNow,
		boolean asapAvailable,
		String nextAvailableDate,
		String nextAvailableTime,
		String nextAvailableAt,
		String statusMessage,
		List<OrderAvailabilityDateResponse> validOrderDates
) {
}
