package com.basilico.backend.menu;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MenuPublicApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void publicMenuReturnsSeededMenu() throws Exception {
		mockMvc.perform(get("/api/menu"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.categories.length()").value(5))
				.andExpect(jsonPath("$.categories[0].slug").value("bites-to-start"))
				.andExpect(jsonPath("$.categories[0].items.length()").value(8))
				.andExpect(jsonPath("$.categories[1].slug").value("sourdough-pizza-calzone"))
				.andExpect(jsonPath("$.categories[1].items.length()").value(15))
				.andExpect(jsonPath("$.categories[2].items.length()").value(4))
				.andExpect(jsonPath("$.categories[3].items.length()").value(3))
				.andExpect(jsonPath("$.categories[4].items.length()").value(2))
				.andExpect(jsonPath("$.customizers.length()").value(1));
	}

	@Test
	void publicItemReturnsMargheritaWithPriceAndTags() throws Exception {
		mockMvc.perform(get("/api/menu/items/pizza-margherita"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("PIZZA MARGHERITA"))
				.andExpect(jsonPath("$.pricePence").value(899))
				.andExpect(jsonPath("$.imagePath").value("/images/menu/margherita.png"))
				.andExpect(jsonPath("$.productType").value("PIZZA"))
				.andExpect(jsonPath("$.dietaryTags[0]").value("V"))
				.andExpect(jsonPath("$.dietaryTags[1]").value("GF"))
				.andExpect(jsonPath("$.dietaryTags[2]").value("VE"));
	}

	@Test
	void unknownPublicItemReturnsNotFound() throws Exception {
		mockMvc.perform(get("/api/menu/items/not-a-real-dish"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("NOT_FOUND"));
	}

	@Test
	void createYourOwnCustomizerReturnsPricingAndToppings() throws Exception {
		mockMvc.perform(get("/api/menu/customizers/create-your-own"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.slug").value("create-your-own"))
				.andExpect(jsonPath("$.name").value("CREATE YOUR OWN"))
				.andExpect(jsonPath("$.basePricePence").value(700))
				.andExpect(jsonPath("$.extraToppingPricePence").value(125))
				.andExpect(jsonPath("$.toppings.length()").value(28))
				.andExpect(jsonPath("$.toppings[0].name").value("Anchovies"))
				.andExpect(jsonPath("$.toppings[27].name").value("Spicy jalapeños"));
	}
}
