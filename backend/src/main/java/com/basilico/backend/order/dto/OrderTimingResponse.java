package com.basilico.backend.order.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.basilico.backend.order.entity.TimingType;

public record OrderTimingResponse(
		TimingType type,
		LocalDate requestedDate,
		LocalTime requestedTime
) {
}
