package com.basilico.backend.booking.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.basilico.backend.common.dto.PageResponse;
import com.basilico.backend.booking.dto.AdminBookingResponse;
import com.basilico.backend.booking.dto.AdminBookingSummaryResponse;
import com.basilico.backend.booking.dto.BookingResponse;
import com.basilico.backend.booking.dto.CreateBookingRequest;
import com.basilico.backend.booking.entity.Booking;
import com.basilico.backend.booking.entity.BookingStatus;
import com.basilico.backend.booking.repository.BookingRepository;
import com.basilico.backend.common.RestaurantSchedule;
import com.basilico.backend.common.error.BadRequestException;
import com.basilico.backend.common.error.ConflictException;
import com.basilico.backend.common.error.ResourceNotFoundException;
import com.basilico.backend.notification.service.CustomerNotificationService;

@Service
public class BookingService {

	private static final int MAX_ADMIN_PAGE_SIZE = 100;
	private static final int REFERENCE_RANDOM_LENGTH = 5;
	private static final char[] REFERENCE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
	private static final DateTimeFormatter REFERENCE_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

	private final BookingRepository bookingRepository;
	private final CustomerNotificationService notificationService;
	private final SecureRandom secureRandom = new SecureRandom();

	public BookingService(BookingRepository bookingRepository,
			CustomerNotificationService notificationService) {
		this.bookingRepository = bookingRepository;
		this.notificationService = notificationService;
	}

	@Transactional
	public BookingResponse createBooking(CreateBookingRequest request) {
		validateBookingDateTime(request);

		Booking booking = new Booking(
				generateBookingReference(),
				request.date(),
				request.time(),
				request.partySize(),
				trimRequired(request.firstName(), "First name is required"),
				trimRequired(request.lastName(), "Last name is required"),
				trimRequired(request.phone(), "Phone is required"),
				trimRequired(request.email(), "Email is required")
		);
		booking.setSpecialRequests(blankToNull(request.specialRequests()));

		Booking savedBooking = bookingRepository.save(booking);
		notificationService.queueBookingRequestReceived(savedBooking);
		return toBookingResponse(savedBooking);
	}

	@Transactional(readOnly = true)
	public PageResponse<AdminBookingSummaryResponse> getAdminBookings(LocalDate date, BookingStatus status, int page,
			int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_ADMIN_PAGE_SIZE);
		PageRequest pageRequest = PageRequest.of(safePage, safeSize,
				Sort.by(Sort.Direction.ASC, "bookingDate", "bookingTime"));
		return PageResponse.from(bookingRepository.findForAdmin(date, status, pageRequest)
				.map(this::toAdminBookingSummaryResponse));
	}

	@Transactional(readOnly = true)
	public AdminBookingResponse getAdminBooking(Long id) {
		return bookingRepository.findById(id)
				.map(this::toAdminBookingResponse)
				.orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
	}

	@Transactional
	public AdminBookingResponse updateStatus(Long id, BookingStatus status) {
		Booking booking = bookingRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
		BookingStatus previousStatus = booking.getStatus();
		booking.setStatus(status);
		notificationService.queueBookingStatusNotification(booking, previousStatus);
		return toAdminBookingResponse(booking);
	}

	private void validateBookingDateTime(CreateBookingRequest request) {
		if (request.date().isBefore(LocalDate.now())) {
			throw new BadRequestException("Booking date cannot be in the past");
		}

		if (RestaurantSchedule.isClosed(request.date())) {
			throw new BadRequestException("Basilico is closed on Tuesdays. Please choose another day.");
		}

		if (!RestaurantSchedule.isWithinRequestWindow(request.time())) {
			throw new BadRequestException("Booking time must be between 12:00 and 22:45");
		}

		if (!RestaurantSchedule.isFifteenMinuteInterval(request.time())) {
			throw new BadRequestException("Booking time must use 15-minute intervals");
		}
	}

	private String generateBookingReference() {
		for (int attempt = 0; attempt < 10; attempt++) {
			String reference = "BKG-" + LocalDate.now().format(REFERENCE_DATE_FORMAT) + "-" + randomReferenceSuffix();
			if (!bookingRepository.existsByBookingReference(reference)) {
				return reference;
			}
		}

		throw new ConflictException("Could not create a unique booking reference");
	}

	private String randomReferenceSuffix() {
		StringBuilder value = new StringBuilder(REFERENCE_RANDOM_LENGTH);
		for (int index = 0; index < REFERENCE_RANDOM_LENGTH; index++) {
			value.append(REFERENCE_ALPHABET[secureRandom.nextInt(REFERENCE_ALPHABET.length)]);
		}
		return value.toString();
	}

	private BookingResponse toBookingResponse(Booking booking) {
		return new BookingResponse(
				booking.getBookingReference(),
				booking.getStatus(),
				booking.getBookingDate(),
				booking.getBookingTime(),
				booking.getPartySize()
		);
	}

	private AdminBookingSummaryResponse toAdminBookingSummaryResponse(Booking booking) {
		return new AdminBookingSummaryResponse(
				booking.getId(),
				booking.getBookingReference(),
				booking.getStatus(),
				booking.getBookingDate(),
				booking.getBookingTime(),
				booking.getPartySize(),
				booking.getFirstName() + " " + booking.getLastName(),
				booking.getCreatedAt(),
				booking.getUpdatedAt()
		);
	}

	private AdminBookingResponse toAdminBookingResponse(Booking booking) {
		return new AdminBookingResponse(
				booking.getId(),
				booking.getBookingReference(),
				booking.getStatus(),
				booking.getBookingDate(),
				booking.getBookingTime(),
				booking.getPartySize(),
				booking.getFirstName(),
				booking.getLastName(),
				booking.getPhone(),
				booking.getEmail(),
				booking.getSpecialRequests(),
				booking.getCreatedAt(),
				booking.getUpdatedAt()
		);
	}

	private String trimRequired(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new BadRequestException(message);
		}

		return value.trim();
	}

	private String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		return value.trim();
	}
}
