package com.basilico.backend.common;

import java.time.DayOfWeek;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class RestaurantSchedule {

	public static final ZoneId RESTAURANT_ZONE = ZoneId.of("Europe/London");
	public static final String RESTAURANT_TIMEZONE = RESTAURANT_ZONE.getId();
	public static final LocalTime OPENING_TIME = LocalTime.of(12, 0);
	public static final LocalTime CLOSING_TIME = LocalTime.of(23, 0);
	public static final LocalTime LAST_REQUEST_TIME = LocalTime.of(22, 45);
	public static final int SLOT_INTERVAL_MINUTES = 15;
	public static final int PREORDER_HORIZON_DAYS = 14;

	private final Clock clock;

	public RestaurantSchedule(Clock clock) {
		this.clock = clock.withZone(RESTAURANT_ZONE);
	}

	public ZonedDateTime now() {
		return ZonedDateTime.now(clock).withZoneSameInstant(RESTAURANT_ZONE);
	}

	public LocalDate currentDate() {
		return now().toLocalDate();
	}

	public boolean isOpenNow() {
		return isAsapAvailableNow();
	}

	public boolean isAsapAvailableNow() {
		return isAsapAvailableAt(now());
	}

	public boolean isAsapAvailableAt(ZonedDateTime dateTime) {
		ZonedDateTime restaurantTime = dateTime.withZoneSameInstant(RESTAURANT_ZONE);
		return isOpenDate(restaurantTime.toLocalDate())
				&& isWithinRequestWindow(restaurantTime.toLocalTime());
	}

	public List<LocalDate> validOrderDates() {
		List<LocalDate> dates = new ArrayList<>();
		LocalDate today = currentDate();
		for (int offset = 0; offset < PREORDER_HORIZON_DAYS; offset++) {
			LocalDate date = today.plusDays(offset);
			if (!validScheduledSlots(date).isEmpty()) {
				dates.add(date);
			}
		}
		return List.copyOf(dates);
	}

	public List<LocalTime> validScheduledSlots(LocalDate date) {
		if (date == null || date.isBefore(currentDate()) || !isOpenDate(date)) {
			return List.of();
		}

		LocalTime firstSlot = OPENING_TIME;
		if (date.equals(currentDate())) {
			firstSlot = latest(OPENING_TIME, nextFutureSlot(now().toLocalTime()));
		}

		if (firstSlot.isAfter(LAST_REQUEST_TIME)) {
			return List.of();
		}

		List<LocalTime> slots = new ArrayList<>();
		for (LocalTime slot = firstSlot; !slot.isAfter(LAST_REQUEST_TIME); slot = slot.plusMinutes(SLOT_INTERVAL_MINUTES)) {
			slots.add(slot);
		}
		return List.copyOf(slots);
	}

	public Optional<OrderSlot> nextValidScheduledSlot() {
		LocalDate today = currentDate();
		for (int offset = 0; offset < PREORDER_HORIZON_DAYS + 7; offset++) {
			LocalDate date = today.plusDays(offset);
			List<LocalTime> slots = validScheduledSlots(date);
			if (!slots.isEmpty()) {
				return Optional.of(new OrderSlot(date, slots.getFirst()));
			}
		}
		return Optional.empty();
	}

	public ScheduleValidation validateScheduledOrder(LocalDate date, LocalTime time) {
		if (date == null || time == null) {
			return ScheduleValidation.invalid("Scheduled orders require a requested date and time");
		}

		if (date.isBefore(currentDate())) {
			return ScheduleValidation.invalid("Scheduled order date cannot be in the past");
		}

		if (!isOpenDate(date)) {
			return ScheduleValidation.invalid("Basilico is closed on Tuesdays");
		}

		if (!isWithinRequestWindow(time)) {
			return ScheduleValidation.invalid("Requested order time must be between 12:00 and 22:45");
		}

		if (!isFifteenMinuteInterval(time)) {
			return ScheduleValidation.invalid("Requested order time must use 15-minute intervals");
		}

		if (date.equals(currentDate()) && !time.isAfter(now().toLocalTime())) {
			return ScheduleValidation.invalid("Requested order time is no longer available. Please choose a later time.");
		}

		return ScheduleValidation.success();
	}

	public static boolean isClosed(LocalDate date) {
		return date != null && !isOpenDate(date);
	}

	public static boolean isOpenDate(LocalDate date) {
		return date != null && date.getDayOfWeek() != DayOfWeek.TUESDAY;
	}

	public static boolean isWithinRequestWindow(LocalTime time) {
		return time != null && !time.isBefore(OPENING_TIME) && !time.isAfter(LAST_REQUEST_TIME);
	}

	public static boolean isFifteenMinuteInterval(LocalTime time) {
		return time != null && time.getMinute() % 15 == 0 && time.getSecond() == 0 && time.getNano() == 0;
	}

	private LocalTime nextFutureSlot(LocalTime time) {
		long intervalNanos = SLOT_INTERVAL_MINUTES * 60L * 1_000_000_000L;
		long nextSlotNanos = ((time.toNanoOfDay() / intervalNanos) + 1) * intervalNanos;
		long nanosInDay = 24L * 60L * 60L * 1_000_000_000L;
		if (nextSlotNanos >= nanosInDay) {
			return LocalTime.MAX;
		}
		return LocalTime.ofNanoOfDay(nextSlotNanos);
	}

	private LocalTime latest(LocalTime first, LocalTime second) {
		return first.isAfter(second) ? first : second;
	}

	public record OrderSlot(LocalDate date, LocalTime time) {

		public ZonedDateTime atRestaurantTime() {
			return ZonedDateTime.of(date, time, RESTAURANT_ZONE);
		}
	}

	public record ScheduleValidation(boolean valid, String message) {

		public static ScheduleValidation success() {
			return new ScheduleValidation(true, null);
		}

		public static ScheduleValidation invalid(String message) {
			return new ScheduleValidation(false, message);
		}
	}
}
