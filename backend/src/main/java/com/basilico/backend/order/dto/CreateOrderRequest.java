package com.basilico.backend.order.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.basilico.backend.order.entity.FulfilmentType;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateOrderRequest(
		@NotNull FulfilmentType fulfilmentType,
		@NotNull @Valid OrderCustomerRequest customer,
		@Valid DeliveryAddressRequest deliveryAddress,
		@NotNull @Valid OrderTimingRequest timing,
		@Size(max = 1000) String notes,
		@NotNull @Size(min = 1, max = 50) List<@Valid CreateOrderItemRequest> items
) {
}
