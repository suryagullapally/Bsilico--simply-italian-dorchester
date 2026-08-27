package com.basilico.backend.menu;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.basilico.backend.menu.entity.MenuItem;
import com.basilico.backend.menu.repository.MenuCategoryRepository;
import com.basilico.backend.menu.repository.MenuItemRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminMenuApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MenuCategoryRepository categoryRepository;

	@Autowired
	private MenuItemRepository itemRepository;

	@Test
	void createMenuItemValidatesRequest() throws Exception {
		Long categoryId = categoryId("specials");

		mockMvc.perform(post("/api/admin/menu/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "categoryId": %d,
								  "slug": "test-invalid-item",
								  "name": "",
								  "description": "Invalid item",
								  "pricePence": -1,
								  "imagePath": null,
								  "productType": "STANDARD",
								  "available": true,
								  "active": true,
								  "featured": false,
								  "customizable": false,
								  "displayOrder": 99,
								  "dietaryTags": ["V"]
								}
								""".formatted(categoryId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
	}

	@Test
	void createMenuItemRejectsDuplicateSlug() throws Exception {
		Long categoryId = categoryId("specials");

		mockMvc.perform(post("/api/admin/menu/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "categoryId": %d,
								  "slug": "pizza-margherita",
								  "name": "DUPLICATE TEST ITEM",
								  "description": "Duplicate slug test",
								  "pricePence": 1000,
								  "imagePath": null,
								  "productType": "STANDARD",
								  "available": true,
								  "active": true,
								  "featured": false,
								  "customizable": false,
								  "displayOrder": 99,
								  "dietaryTags": []
								}
								""".formatted(categoryId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("CONFLICT"));
	}

	@Test
	void updateMenuItemPrice() throws Exception {
		MenuItem item = item("lasagna");

		mockMvc.perform(put("/api/admin/menu/items/{id}", item.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(updatePayload(item, 1234)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.slug").value("lasagna"))
				.andExpect(jsonPath("$.pricePence").value(1234));
	}

	@Test
	void updateAvailabilityCanMarkItemSoldOut() throws Exception {
		MenuItem item = item("lasagna");

		mockMvc.perform(patch("/api/admin/menu/items/{id}/availability", item.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "available": false
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.available").value(false));
	}

	@Test
	void updateActiveCanSoftArchiveItem() throws Exception {
		MenuItem item = item("lasagna");

		mockMvc.perform(patch("/api/admin/menu/items/{id}/active", item.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "active": false
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));
	}

	@Test
	void updateFeaturedCanMarkItemForFutureHomepageUse() throws Exception {
		MenuItem item = item("parmigiana-di-melanzane");

		mockMvc.perform(patch("/api/admin/menu/items/{id}/featured", item.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "featured": true
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.featured").value(true));
	}

	private String updatePayload(MenuItem item, int pricePence) {
		return """
				{
				  "categoryId": %d,
				  "name": "%s",
				  "description": "Updated test description",
				  "pricePence": %d,
				  "imagePath": null,
				  "productType": "%s",
				  "available": true,
				  "active": true,
				  "featured": false,
				  "customizable": false,
				  "displayOrder": %d,
				  "dietaryTags": ["V"]
				}
				""".formatted(
				item.getCategory().getId(),
				item.getName(),
				pricePence,
				item.getProductType(),
				item.getDisplayOrder()
		);
	}

	private Long categoryId(String slug) {
		return categoryRepository.findBySlug(slug)
				.orElseThrow()
				.getId();
	}

	private MenuItem item(String slug) {
		return itemRepository.findBySlug(slug)
				.orElseThrow();
	}
}
