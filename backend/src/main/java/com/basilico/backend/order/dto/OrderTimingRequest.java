package com.basilico.backend.order.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

import com.basilico.backend.order.entity.TimingType;

public record OrderTimingRequest(
		@NotNull TimingType type,
		LocalDate requestedDate,
		LocalTime requestedTime
) {
}
