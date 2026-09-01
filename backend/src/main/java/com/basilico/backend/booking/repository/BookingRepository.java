package com.basilico.backend.booking.repository;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.basilico.backend.booking.entity.Booking;
import com.basilico.backend.booking.entity.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {

	boolean existsByBookingReference(String bookingReference);

	@Query("""
			select booking from Booking booking
			where (:date is null or booking.bookingDate = :date)
			  and (:status is null or booking.status = :status)
			""")
	Page<Booking> findForAdmin(@Param("date") LocalDate date, @Param("status") BookingStatus status,
			Pageable pageable);
}
