package com.basilico.backend.common.error;

public record ApiErrorResponse(int status, String error, String message) {
}
