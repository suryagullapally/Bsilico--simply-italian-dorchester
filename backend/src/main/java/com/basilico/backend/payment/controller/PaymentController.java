package com.basilico.backend.payment.controller;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.basilico.backend.payment.dto.CheckoutSessionResponse;
import com.basilico.backend.payment.dto.CheckoutSessionStatusResponse;
import com.basilico.backend.payment.dto.CreateCheckoutSessionRequest;
import com.basilico.backend.payment.dto.WebhookResponse;
import com.basilico.backend.payment.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

	private final PaymentService paymentService;

	public PaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@PostMapping("/checkout-session")
	public CheckoutSessionResponse createCheckoutSession(@Valid @RequestBody CreateCheckoutSessionRequest request) {
		return paymentService.createCheckoutSession(request);
	}

	@GetMapping("/checkout-session/{sessionId}/status")
	public CheckoutSessionStatusResponse getCheckoutSessionStatus(@PathVariable String sessionId) {
		return paymentService.getCheckoutSessionStatus(sessionId);
	}

	@PostMapping(
			value = "/stripe/webhook",
			consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, MediaType.ALL_VALUE }
	)
	public WebhookResponse stripeWebhook(@RequestBody String payload,
			@RequestHeader(name = "Stripe-Signature", required = false) String signatureHeader) {
		return paymentService.handleStripeWebhook(payload, signatureHeader);
	}
}
