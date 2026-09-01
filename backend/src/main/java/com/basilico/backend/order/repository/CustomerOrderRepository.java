package com.basilico.backend.order.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.basilico.backend.order.entity.CustomerOrder;
import com.basilico.backend.order.entity.FulfilmentType;
import com.basilico.backend.order.entity.OrderStatus;
import com.basilico.backend.order.entity.PaymentStatus;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

	boolean existsByOrderReference(String orderReference);

	@Query("""
			select customerOrder from CustomerOrder customerOrder
			where (:status is null or customerOrder.status = :status)
			  and (:paymentStatus is null or customerOrder.paymentStatus = :paymentStatus)
			  and (:fulfilmentType is null or customerOrder.fulfilmentType = :fulfilmentType)
			""")
	Page<CustomerOrder> findForAdmin(@Param("status") OrderStatus status,
			@Param("paymentStatus") PaymentStatus paymentStatus,
			@Param("fulfilmentType") FulfilmentType fulfilmentType,
			Pageable pageable);

	@Query("select customerOrder from CustomerOrder customerOrder where customerOrder.id = :id")
	Optional<CustomerOrder> findDetailedById(@Param("id") Long id);

	@Query("select customerOrder from CustomerOrder customerOrder where customerOrder.orderReference = :orderReference")
	Optional<CustomerOrder> findDetailedByOrderReference(@Param("orderReference") String orderReference);
}
