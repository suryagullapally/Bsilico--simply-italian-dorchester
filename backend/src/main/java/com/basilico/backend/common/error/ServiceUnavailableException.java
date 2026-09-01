package com.basilico.backend.common.error;

public class ServiceUnavailableException extends RuntimeException {

	public ServiceUnavailableException(String message) {
		super(message);
	}
}
