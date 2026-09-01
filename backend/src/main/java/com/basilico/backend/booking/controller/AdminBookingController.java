package com.basilico.backend.booking.controller;

import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.basilico.backend.booking.dto.AdminBookingResponse;
import com.basilico.backend.booking.dto.AdminBookingSummaryResponse;
import com.basilico.backend.booking.dto.BookingStatusUpdateRequest;
import com.basilico.backend.booking.entity.BookingStatus;
import com.basilico.backend.booking.service.BookingService;
import com.basilico.backend.common.dto.PageResponse;

@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

	private final BookingService bookingService;

	public AdminBookingController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@GetMapping
	public PageResponse<AdminBookingSummaryResponse> getBookings(
			@RequestParam(required = false) LocalDate date,
			@RequestParam(required = false) BookingStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return bookingService.getAdminBookings(date, status, page, size);
	}

	@GetMapping("/{id}")
	public AdminBookingResponse getBooking(@PathVariable Long id) {
		return bookingService.getAdminBooking(id);
	}

	@PatchMapping("/{id}/status")
	public AdminBookingResponse updateStatus(@PathVariable Long id,
			@Valid @RequestBody BookingStatusUpdateRequest request) {
		return bookingService.updateStatus(id, request.status());
	}
}
