package com.basilico.backend.common;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public final class RestaurantSchedule {

	public static final LocalTime OPENING_TIME = LocalTime.of(12, 0);
	public static final LocalTime LAST_REQUEST_TIME = LocalTime.of(22, 45);

	private RestaurantSchedule() {
	}

	public static boolean isClosed(LocalDate date) {
		return date != null && date.getDayOfWeek() == DayOfWeek.TUESDAY;
	}

	public static boolean isWithinRequestWindow(LocalTime time) {
		return time != null && !time.isBefore(OPENING_TIME) && !time.isAfter(LAST_REQUEST_TIME);
	}

	public static boolean isFifteenMinuteInterval(LocalTime time) {
		return time != null && time.getMinute() % 15 == 0 && time.getSecond() == 0 && time.getNano() == 0;
	}
}
