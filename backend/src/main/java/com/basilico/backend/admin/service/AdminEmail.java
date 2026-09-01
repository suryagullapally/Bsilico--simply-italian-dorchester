package com.basilico.backend.admin.service;

import java.util.Locale;

public final class AdminEmail {

	private AdminEmail() {
	}

	public static String normalize(String email) {
		if (email == null) {
			return "";
		}

		return email.trim().toLowerCase(Locale.ROOT);
	}
}
