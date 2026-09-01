package com.basilico.backend.fulfilment.dto;

import jakarta.validation.constraints.NotNull;

public record PostcodeRuleActiveUpdateRequest(
		@NotNull Boolean active
) {
}
