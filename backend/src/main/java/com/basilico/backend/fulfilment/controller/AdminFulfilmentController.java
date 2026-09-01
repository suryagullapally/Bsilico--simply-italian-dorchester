package com.basilico.backend.fulfilment.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.basilico.backend.fulfilment.dto.AdminFulfilmentSettingsResponse;
import com.basilico.backend.fulfilment.dto.CreateDeliveryPostcodeRuleRequest;
import com.basilico.backend.fulfilment.dto.DeliveryPostcodeRuleResponse;
import com.basilico.backend.fulfilment.dto.PostcodeRuleActiveUpdateRequest;
import com.basilico.backend.fulfilment.dto.UpdateFulfilmentSettingsRequest;
import com.basilico.backend.fulfilment.service.FulfilmentService;

@RestController
@RequestMapping("/api/admin/fulfilment")
public class AdminFulfilmentController {

	private final FulfilmentService fulfilmentService;

	public AdminFulfilmentController(FulfilmentService fulfilmentService) {
		this.fulfilmentService = fulfilmentService;
	}

	@GetMapping("/settings")
	public AdminFulfilmentSettingsResponse getSettings() {
		return fulfilmentService.getAdminSettings();
	}

	@PutMapping("/settings")
	public AdminFulfilmentSettingsResponse updateSettings(
			@Valid @RequestBody UpdateFulfilmentSettingsRequest request) {
		return fulfilmentService.updateSettings(request);
	}

	@GetMapping("/postcode-rules")
	public List<DeliveryPostcodeRuleResponse> getPostcodeRules() {
		return fulfilmentService.getAdminPostcodeRules();
	}

	@PostMapping("/postcode-rules")
	@ResponseStatus(HttpStatus.CREATED)
	public DeliveryPostcodeRuleResponse createPostcodeRule(
			@Valid @RequestBody CreateDeliveryPostcodeRuleRequest request) {
		return fulfilmentService.createPostcodeRule(request);
	}

	@PatchMapping("/postcode-rules/{id}/active")
	public DeliveryPostcodeRuleResponse updatePostcodeRuleActive(@PathVariable Long id,
			@Valid @RequestBody PostcodeRuleActiveUpdateRequest request) {
		return fulfilmentService.updatePostcodeRuleActive(id, request);
	}
}
