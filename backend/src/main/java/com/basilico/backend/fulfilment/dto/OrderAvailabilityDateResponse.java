package com.basilico.backend.fulfilment.dto;

import java.util.List;

public record OrderAvailabilityDateResponse(
		String date,
		String label,
		List<String> timeSlots
) {
}
