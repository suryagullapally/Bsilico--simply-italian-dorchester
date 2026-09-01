package com.basilico.backend.fulfilment.service;

public class DeliveryProviderException extends RuntimeException {

	public DeliveryProviderException(String message) {
		super(message);
	}

	public DeliveryProviderException(String message, Throwable cause) {
		super(message, cause);
	}
}
