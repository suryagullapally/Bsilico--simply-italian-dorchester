package com.basilico.backend.notification.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.basilico.backend.notification.entity.CustomerNotification;
import com.basilico.backend.notification.entity.NotificationStatus;
import com.basilico.backend.notification.entity.NotificationType;

public interface CustomerNotificationRepository extends JpaRepository<CustomerNotification, Long> {

	Optional<CustomerNotification> findByDeduplicationKey(String deduplicationKey);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select notification from CustomerNotification notification where notification.id = :id")
	Optional<CustomerNotification> findByIdForUpdate(@Param("id") Long id);

	@Query("""
			select notification.id from CustomerNotification notification
			where notification.status in :statuses
			  and notification.attemptCount < :maxAttempts
			order by notification.createdAt asc, notification.id asc
			""")
	List<Long> findProcessableIds(@Param("statuses") List<NotificationStatus> statuses,
			@Param("maxAttempts") int maxAttempts,
			Pageable pageable);

	@Query("""
			select notification from CustomerNotification notification
			where (:status is null or notification.status = :status)
			  and (:type is null or notification.notificationType = :type)
			  and (:orderId is null or notification.order.id = :orderId)
			  and (:bookingId is null or notification.booking.id = :bookingId)
			""")
	Page<CustomerNotification> findForAdmin(@Param("status") NotificationStatus status,
			@Param("type") NotificationType type,
			@Param("orderId") Long orderId,
			@Param("bookingId") Long bookingId,
			Pageable pageable);
}
