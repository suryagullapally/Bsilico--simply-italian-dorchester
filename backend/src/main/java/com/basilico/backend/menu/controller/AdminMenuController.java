package com.basilico.backend.menu.controller;

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

import com.basilico.backend.menu.dto.ActiveUpdateRequest;
import com.basilico.backend.menu.dto.AvailabilityUpdateRequest;
import com.basilico.backend.menu.dto.CreateMenuItemRequest;
import com.basilico.backend.menu.dto.FeaturedUpdateRequest;
import com.basilico.backend.menu.dto.MenuItemResponse;
import com.basilico.backend.menu.dto.UpdateMenuItemRequest;
import com.basilico.backend.menu.service.MenuService;

@RestController
@RequestMapping("/api/admin/menu/items")
public class AdminMenuController {

	private final MenuService menuService;

	public AdminMenuController(MenuService menuService) {
		this.menuService = menuService;
	}

	@GetMapping
	public List<MenuItemResponse> getItems() {
		return menuService.getAdminItems();
	}

	@GetMapping("/{id}")
	public MenuItemResponse getItem(@PathVariable Long id) {
		return menuService.getAdminItem(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MenuItemResponse createItem(@Valid @RequestBody CreateMenuItemRequest request) {
		return menuService.createItem(request);
	}

	@PutMapping("/{id}")
	public MenuItemResponse updateItem(@PathVariable Long id, @Valid @RequestBody UpdateMenuItemRequest request) {
		return menuService.updateItem(id, request);
	}

	@PatchMapping("/{id}/availability")
	public MenuItemResponse updateAvailability(@PathVariable Long id,
			@Valid @RequestBody AvailabilityUpdateRequest request) {
		return menuService.updateAvailability(id, request);
	}

	@PatchMapping("/{id}/active")
	public MenuItemResponse updateActive(@PathVariable Long id, @Valid @RequestBody ActiveUpdateRequest request) {
		return menuService.updateActive(id, request);
	}

	@PatchMapping("/{id}/featured")
	public MenuItemResponse updateFeatured(@PathVariable Long id, @Valid @RequestBody FeaturedUpdateRequest request) {
		return menuService.updateFeatured(id, request);
	}
}
