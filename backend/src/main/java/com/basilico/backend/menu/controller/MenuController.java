package com.basilico.backend.menu.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.basilico.backend.menu.dto.MenuCustomizerResponse;
import com.basilico.backend.menu.dto.MenuItemResponse;
import com.basilico.backend.menu.dto.MenuResponse;
import com.basilico.backend.menu.service.MenuService;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

	private final MenuService menuService;

	public MenuController(MenuService menuService) {
		this.menuService = menuService;
	}

	@GetMapping
	public MenuResponse getMenu() {
		return menuService.getPublicMenu();
	}

	@GetMapping("/items/{slug}")
	public MenuItemResponse getItem(@PathVariable String slug) {
		return menuService.getPublicItem(slug);
	}

	@GetMapping("/customizers/{slug}")
	public MenuCustomizerResponse getCustomizer(@PathVariable String slug) {
		return menuService.getPublicCustomizer(slug);
	}
}
