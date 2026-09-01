package com.basilico.backend.notification.service;

public record EmailContent(
		String subject,
		String text,
		String html
) {
}
