package com.basilico.backend.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.basilico.backend.admin.dto.AdminUserResponse;
import com.basilico.backend.admin.dto.CsrfTokenResponse;
import com.basilico.backend.admin.dto.LoginRequest;
import com.basilico.backend.admin.security.AdminUserPrincipal;
import com.basilico.backend.admin.service.AdminAuthService;
import com.basilico.backend.admin.service.AdminEmail;
import com.basilico.backend.common.error.UnauthorizedException;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;
	private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
	private final AdminAuthService adminAuthService;

	public AdminAuthController(AuthenticationManager authenticationManager,
			SecurityContextRepository securityContextRepository,
			SessionAuthenticationStrategy sessionAuthenticationStrategy,
			AdminAuthService adminAuthService) {
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
		this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
		this.adminAuthService = adminAuthService;
	}

	@GetMapping("/csrf")
	public CsrfTokenResponse csrf(CsrfToken csrfToken) {
		return new CsrfTokenResponse(csrfToken.getToken(), csrfToken.getHeaderName());
	}

	@PostMapping("/login")
	public AdminUserResponse login(@Valid @RequestBody LoginRequest request,
			HttpServletRequest servletRequest,
			HttpServletResponse servletResponse) {
		try {
			Authentication authentication = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(
							AdminEmail.normalize(request.email()),
							request.password()
					)
			);

			sessionAuthenticationStrategy.onAuthentication(authentication, servletRequest, servletResponse);

			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(authentication);
			SecurityContextHolder.setContext(context);
			securityContextRepository.saveContext(context, servletRequest, servletResponse);

			return adminAuthService.markLoginAndRespond((AdminUserPrincipal) authentication.getPrincipal());
		} catch (AuthenticationException exception) {
			throw new UnauthorizedException("Invalid email or password.");
		}
	}

	@GetMapping("/me")
	public AdminUserResponse me(@AuthenticationPrincipal AdminUserPrincipal principal) {
		if (principal == null) {
			throw new UnauthorizedException("Authentication is required.");
		}

		return adminAuthService.toResponse(principal);
	}
}
