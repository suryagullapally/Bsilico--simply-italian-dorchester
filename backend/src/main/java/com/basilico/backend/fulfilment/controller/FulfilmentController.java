package com.basilico.backend.fulfilment.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.basilico.backend.fulfilment.dto.CheckDeliveryRequest;
import com.basilico.backend.fulfilment.dto.DeliveryEligibilityResponse;
import com.basilico.backend.fulfilment.dto.DeliveryQuoteRequest;
import com.basilico.backend.fulfilment.dto.DeliveryQuoteResponse;
import com.basilico.backend.fulfilment.dto.FulfilmentOptionsResponse;
import com.basilico.backend.fulfilment.service.FulfilmentService;

@RestController
@RequestMapping("/api/fulfilment")
public class FulfilmentController {

	private final FulfilmentService fulfilmentService;

	public FulfilmentController(FulfilmentService fulfilmentService) {
		this.fulfilmentService = fulfilmentService;
	}

	@GetMapping("/options")
	public FulfilmentOptionsResponse getOptions() {
		return fulfilmentService.getPublicOptions();
	}

	@PostMapping("/check-delivery")
	public DeliveryEligibilityResponse checkDelivery(@Valid @RequestBody CheckDeliveryRequest request) {
		return fulfilmentService.checkDelivery(request.postcode());
	}

	@PostMapping("/delivery-quote")
	public DeliveryQuoteResponse quoteDelivery(@Valid @RequestBody DeliveryQuoteRequest request) {
		return fulfilmentService.quoteDelivery(request);
	}
}
