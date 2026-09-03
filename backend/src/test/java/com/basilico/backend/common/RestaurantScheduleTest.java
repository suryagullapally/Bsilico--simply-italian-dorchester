package com.basilico.backend.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

class RestaurantScheduleTest {

	@Test
	void mondayBeforeOpeningIsClosedAndNextSlotIsNoonToday() {
		RestaurantSchedule schedule = scheduleAt("2026-08-31T09:00:00+01:00[Europe/London]");

		assertThat(schedule.isOpenNow()).isFalse();
		assertThat(schedule.isAsapAvailableNow()).isFalse();
		assertThat(schedule.nextValidScheduledSlot()).hasValueSatisfying(slot -> {
			assertThat(slot.date()).isEqualTo(LocalDate.parse("2026-08-31"));
			assertThat(slot.time()).isEqualTo(LocalTime.parse("12:00"));
		});
	}

	@Test
	void mondayAtNoonAcceptsAsap() {
		RestaurantSchedule schedule = scheduleAt("2026-08-31T12:00:00+01:00[Europe/London]");

		assertThat(schedule.isOpenNow()).isTrue();
		assertThat(schedule.isAsapAvailableNow()).isTrue();
	}

	@Test
	void sameDayPastScheduledSlotsAreRejected() {
		RestaurantSchedule schedule = scheduleAt("2026-08-31T18:32:00+01:00[Europe/London]");

		RestaurantSchedule.ScheduleValidation validation = schedule.validateScheduledOrder(
				LocalDate.parse("2026-08-31"),
				LocalTime.parse("18:30")
		);

		assertThat(validation.valid()).isFalse();
		assertThat(validation.message()).contains("no longer available");
		assertThat(schedule.validScheduledSlots(LocalDate.parse("2026-08-31")).getFirst())
				.isEqualTo(LocalTime.parse("18:45"));
	}

	@Test
	void mondayFinalOrderCutoffAllowsAsapButNoScheduledSlotAtThatInstant() {
		RestaurantSchedule schedule = scheduleAt("2026-08-31T22:45:00+01:00[Europe/London]");

		assertThat(schedule.isAsapAvailableNow()).isTrue();
		assertThat(schedule.validScheduledSlots(LocalDate.parse("2026-08-31"))).isEmpty();
	}

	@Test
	void mondayAfterCutoffSkipsTuesdayAndFindsWednesdayNoon() {
		RestaurantSchedule schedule = scheduleAt("2026-08-31T22:46:00+01:00[Europe/London]");

		assertThat(schedule.isAsapAvailableNow()).isFalse();
		assertThat(schedule.validScheduledSlots(LocalDate.parse("2026-08-31"))).isEmpty();
		assertThat(schedule.nextValidScheduledSlot()).hasValueSatisfying(slot -> {
			assertThat(slot.date()).isEqualTo(LocalDate.parse("2026-09-02"));
			assertThat(slot.time()).isEqualTo(LocalTime.parse("12:00"));
		});
	}

	@Test
	void tuesdayIsClosedAndNextSlotIsWednesdayNoon() {
		RestaurantSchedule schedule = scheduleAt("2026-09-01T13:00:00+01:00[Europe/London]");

		assertThat(schedule.isAsapAvailableNow()).isFalse();
		assertThat(schedule.validScheduledSlots(LocalDate.parse("2026-09-01"))).isEmpty();
		assertThat(schedule.nextValidScheduledSlot()).hasValueSatisfying(slot -> {
			assertThat(slot.date()).isEqualTo(LocalDate.parse("2026-09-02"));
			assertThat(slot.time()).isEqualTo(LocalTime.parse("12:00"));
		});
	}

	@Test
	void sundayAfterCloseFindsMondayNoon() {
		RestaurantSchedule schedule = scheduleAt("2026-09-06T23:01:00+01:00[Europe/London]");

		assertThat(schedule.isAsapAvailableNow()).isFalse();
		assertThat(schedule.nextValidScheduledSlot()).hasValueSatisfying(slot -> {
			assertThat(slot.date()).isEqualTo(LocalDate.parse("2026-09-07"));
			assertThat(slot.time()).isEqualTo(LocalTime.parse("12:00"));
		});
	}

	@Test
	void scheduledValidationRejectsPastAndClosedDates() {
		RestaurantSchedule schedule = scheduleAt("2026-08-31T09:00:00+01:00[Europe/London]");

		assertThat(schedule.validateScheduledOrder(LocalDate.parse("2026-08-30"), LocalTime.parse("18:30")).valid())
				.isFalse();
		assertThat(schedule.validateScheduledOrder(LocalDate.parse("2026-09-01"), LocalTime.parse("18:30")).valid())
				.isFalse();
		assertThat(schedule.validateScheduledOrder(LocalDate.parse("2026-09-02"), LocalTime.parse("12:00")).valid())
				.isTrue();
	}

	@Test
	void dstChangeStillUsesEuropeLondonRules() {
		RestaurantSchedule schedule = scheduleAt("2026-03-29T09:00:00+01:00[Europe/London]");

		assertThat(schedule.nextValidScheduledSlot()).hasValueSatisfying(slot -> {
			assertThat(slot.date()).isEqualTo(LocalDate.parse("2026-03-29"));
			assertThat(slot.time()).isEqualTo(LocalTime.parse("12:00"));
		});
	}

	private RestaurantSchedule scheduleAt(String zonedDateTime) {
		ZonedDateTime dateTime = ZonedDateTime.parse(zonedDateTime);
		return new RestaurantSchedule(Clock.fixed(dateTime.toInstant(), RestaurantSchedule.RESTAURANT_ZONE));
	}
}
