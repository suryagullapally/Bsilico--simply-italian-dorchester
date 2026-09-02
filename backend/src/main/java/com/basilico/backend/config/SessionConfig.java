package com.basilico.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class SessionConfig {

	@Bean
	public CookieSerializer cookieSerializer(AdminSessionCookieProperties properties) {
		DefaultCookieSerializer serializer = new DefaultCookieSerializer();
		serializer.setCookieName("SESSION");
		serializer.setCookiePath("/");
		serializer.setUseHttpOnlyCookie(true);
		serializer.setUseSecureCookie(properties.secure());
		serializer.setSameSite(properties.sameSite());
		serializer.setCookieMaxAge(properties.maxAgeSeconds());
		serializer.setUseBase64Encoding(false);
		return serializer;
	}
}
