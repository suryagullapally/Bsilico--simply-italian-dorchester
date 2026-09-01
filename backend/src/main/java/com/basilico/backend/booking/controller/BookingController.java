package com.basilico.backend.booking.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.basilico.backend.booking.dto.BookingResponse;
import com.basilico.backend.booking.dto.CreateBookingRequest;
import com.basilico.backend.booking.service.BookingService;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

	private final BookingService bookingService;

	public BookingController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BookingResponse createBooking(@Valid @RequestBody CreateBookingRequest request) {
		return bookingService.createBooking(request);
	}
}
