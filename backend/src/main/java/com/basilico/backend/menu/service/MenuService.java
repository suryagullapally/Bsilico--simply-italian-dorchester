package com.basilico.backend.menu.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.basilico.backend.common.error.ConflictException;
import com.basilico.backend.common.error.ResourceNotFoundException;
import com.basilico.backend.menu.dto.ActiveUpdateRequest;
import com.basilico.backend.menu.dto.AvailabilityUpdateRequest;
import com.basilico.backend.menu.dto.CreateMenuItemRequest;
import com.basilico.backend.menu.dto.FeaturedUpdateRequest;
import com.basilico.backend.menu.dto.MenuCategoryResponse;
import com.basilico.backend.menu.dto.MenuCustomizerResponse;
import com.basilico.backend.menu.dto.MenuItemResponse;
import com.basilico.backend.menu.dto.MenuResponse;
import com.basilico.backend.menu.dto.PizzaToppingResponse;
import com.basilico.backend.menu.dto.UpdateMenuItemRequest;
import com.basilico.backend.menu.entity.DietaryTag;
import com.basilico.backend.menu.entity.MenuCategory;
import com.basilico.backend.menu.entity.MenuCustomizer;
import com.basilico.backend.menu.entity.MenuItem;
import com.basilico.backend.menu.entity.PizzaTopping;
import com.basilico.backend.menu.repository.MenuCategoryRepository;
import com.basilico.backend.menu.repository.MenuCustomizerRepository;
import com.basilico.backend.menu.repository.MenuItemRepository;

@Service
public class MenuService {

	private static final List<DietaryTag> DIETARY_TAG_ORDER = List.of(DietaryTag.V, DietaryTag.GF, DietaryTag.VE);

	private final MenuCategoryRepository categoryRepository;
	private final MenuItemRepository itemRepository;
	private final MenuCustomizerRepository customizerRepository;

	public MenuService(MenuCategoryRepository categoryRepository, MenuItemRepository itemRepository,
			MenuCustomizerRepository customizerRepository) {
		this.categoryRepository = categoryRepository;
		this.itemRepository = itemRepository;
		this.customizerRepository = customizerRepository;
	}

	@Transactional(readOnly = true)
	public MenuResponse getPublicMenu() {
		List<MenuCategory> categories = categoryRepository.findByActiveTrueOrderByDisplayOrderAsc();
		Map<Long, List<MenuItemResponse>> itemsByCategoryId = itemRepository.findActiveItemsForPublicMenu()
				.stream()
				.collect(Collectors.groupingBy(
						item -> item.getCategory().getId(),
						LinkedHashMap::new,
						Collectors.mapping(this::toMenuItemResponse, Collectors.toList())
				));

		List<MenuCategoryResponse> categoryResponses = categories.stream()
				.map(category -> new MenuCategoryResponse(
						category.getId(),
						category.getSlug(),
						category.getName(),
						category.getDisplayOrder(),
						itemsByCategoryId.getOrDefault(category.getId(), List.of())
				))
				.toList();

		List<MenuCustomizerResponse> customizerResponses = customizerRepository.findActiveCustomizersForPublicMenu()
				.stream()
				.map(customizer -> toMenuCustomizerResponse(customizer, true))
				.toList();

		return new MenuResponse(categoryResponses, customizerResponses);
	}

	@Transactional(readOnly = true)
	public MenuItemResponse getPublicItem(String slug) {
		return itemRepository.findBySlugAndActiveTrue(slug)
				.map(this::toMenuItemResponse)
				.orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
	}

	@Transactional(readOnly = true)
	public MenuCustomizerResponse getPublicCustomizer(String slug) {
		return customizerRepository.findBySlugAndActiveTrue(slug)
				.map(customizer -> toMenuCustomizerResponse(customizer, true))
				.orElseThrow(() -> new ResourceNotFoundException("Menu customizer not found"));
	}

	@Transactional(readOnly = true)
	public List<MenuItemResponse> getAdminItems() {
		return itemRepository.findAllItemsForAdmin()
				.stream()
				.map(this::toMenuItemResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public MenuItemResponse getAdminItem(Long id) {
		return toMenuItemResponse(findItem(id));
	}

	@Transactional
	public MenuItemResponse createItem(CreateMenuItemRequest request) {
		String slug = request.slug().trim();
		if (itemRepository.existsBySlug(slug)) {
			throw new ConflictException("Menu item slug already exists");
		}

		MenuCategory category = findCategory(request.categoryId());
		MenuItem item = new MenuItem(category, slug, request.name().trim(), request.pricePence(),
				request.productType(), request.displayOrder());

		applyEditableFields(item, request.description(), request.imagePath(), request.available(), request.active(),
				request.featured(), request.customizable(), orderedTags(request.dietaryTags()));

		return toMenuItemResponse(itemRepository.save(item));
	}

	@Transactional
	public MenuItemResponse updateItem(Long id, UpdateMenuItemRequest request) {
		MenuItem item = findItem(id);
		item.setCategory(findCategory(request.categoryId()));
		item.setName(request.name().trim());
		item.setPricePence(request.pricePence());
		item.setProductType(request.productType());
		item.setDisplayOrder(request.displayOrder());

		applyEditableFields(item, request.description(), request.imagePath(), request.available(), request.active(),
				request.featured(), request.customizable(), orderedTags(request.dietaryTags()));

		return toMenuItemResponse(item);
	}

	@Transactional
	public MenuItemResponse updateAvailability(Long id, AvailabilityUpdateRequest request) {
		MenuItem item = findItem(id);
		item.setAvailable(request.available());
		return toMenuItemResponse(item);
	}

	@Transactional
	public MenuItemResponse updateActive(Long id, ActiveUpdateRequest request) {
		MenuItem item = findItem(id);
		item.setActive(request.active());
		return toMenuItemResponse(item);
	}

	@Transactional
	public MenuItemResponse updateFeatured(Long id, FeaturedUpdateRequest request) {
		MenuItem item = findItem(id);
		item.setFeatured(request.featured());
		return toMenuItemResponse(item);
	}

	private void applyEditableFields(MenuItem item, String description, String imagePath, Boolean available,
			Boolean active, Boolean featured, Boolean customizable, Set<DietaryTag> dietaryTags) {
		item.setDescription(blankToNull(description));
		item.setImagePath(blankToNull(imagePath));
		item.setAvailable(available);
		item.setActive(active);
		item.setFeatured(featured);
		item.setCustomizable(customizable);
		item.setDietaryTags(dietaryTags);
	}

	private MenuCategory findCategory(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Menu category not found"));
	}

	private MenuItem findItem(Long id) {
		return itemRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
	}

	private MenuItemResponse toMenuItemResponse(MenuItem item) {
		MenuCategory category = item.getCategory();
		return new MenuItemResponse(
				item.getId(),
				item.getSlug(),
				item.getName(),
				item.getDescription(),
				item.getPricePence(),
				item.getImagePath(),
				item.getProductType(),
				item.isAvailable(),
				item.isActive(),
				item.isFeatured(),
				item.isCustomizable(),
				item.getDisplayOrder(),
				category.getSlug(),
				category.getName(),
				orderedTagList(item.getDietaryTags())
		);
	}

	private MenuCustomizerResponse toMenuCustomizerResponse(MenuCustomizer customizer, boolean onlyAvailableToppings) {
		MenuCategory category = customizer.getCategory();
		List<PizzaToppingResponse> toppings = customizer.getToppings()
				.stream()
				.filter(topping -> !onlyAvailableToppings || topping.isAvailable())
				.map(this::toPizzaToppingResponse)
				.toList();

		return new MenuCustomizerResponse(
				customizer.getId(),
				customizer.getSlug(),
				customizer.getName(),
				customizer.getBasePricePence(),
				customizer.getExtraToppingPricePence(),
				customizer.isActive(),
				customizer.getDisplayOrder(),
				category.getSlug(),
				category.getName(),
				toppings
		);
	}

	private PizzaToppingResponse toPizzaToppingResponse(PizzaTopping topping) {
		return new PizzaToppingResponse(
				topping.getId(),
				topping.getName(),
				topping.getPriceOverridePence(),
				topping.isAvailable(),
				topping.getDisplayOrder()
		);
	}

	private Set<DietaryTag> orderedTags(Set<DietaryTag> tags) {
		return DIETARY_TAG_ORDER.stream()
				.filter(tags::contains)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private List<DietaryTag> orderedTagList(Set<DietaryTag> tags) {
		return DIETARY_TAG_ORDER.stream()
				.filter(tags::contains)
				.toList();
	}

	private String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		return value.trim();
	}
}
