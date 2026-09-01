package com.basilico.backend.menu;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.basilico.backend.menu.entity.DietaryTag;
import com.basilico.backend.menu.entity.MenuItem;
import com.basilico.backend.menu.repository.MenuCategoryRepository;
import com.basilico.backend.menu.repository.MenuCustomizerRepository;
import com.basilico.backend.menu.repository.MenuItemRepository;
import com.basilico.backend.menu.repository.PizzaToppingRepository;

@SpringBootTest
class MenuSeedDataValidationTest {

	@Autowired
	private MenuCategoryRepository categoryRepository;

	@Autowired
	private MenuItemRepository itemRepository;

	@Autowired
	private MenuCustomizerRepository customizerRepository;

	@Autowired
	private PizzaToppingRepository toppingRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void seededMenuHasExpectedCounts() {
		assertThat(categoryRepository.count()).isEqualTo(5);
		assertThat(itemRepository.count()).isEqualTo(32);
		assertThat(itemRepository.countByCategorySlug("bites-to-start")).isEqualTo(8);
		assertThat(itemRepository.countByCategorySlug("sourdough-pizza-calzone")).isEqualTo(15);
		assertThat(itemRepository.countByCategorySlug("specials")).isEqualTo(4);
		assertThat(itemRepository.countByCategorySlug("sweet-tooth")).isEqualTo(3);
		assertThat(itemRepository.countByCategorySlug("side-salad")).isEqualTo(2);
		assertThat(customizerRepository.countBySlug("create-your-own")).isEqualTo(1);
		assertThat(toppingRepository.countByCustomizerSlug("create-your-own")).isEqualTo(28);
		assertThat(toppingRepository.existsByName("Rooker Shaved parmesan")).isFalse();
	}

	@Test
	void seededImagesAndDietaryTagsMatchSourceData() {
		MenuItem olives = item("olive-di-nocellara");
		MenuItem bruschetta = item("bruschetta");
		MenuItem margherita = item("pizza-margherita");
		MenuItem schiacciatella = item("schiacciatella");
		MenuItem formaggio = item("schiacciata-al-formaggio");
		MenuItem bread = item("oppane-the-bread");
		MenuItem piadina = item("piadina-e-crema-di-pomodoro");
		MenuItem sfizio = item("sfizio-al-pomodoro");
		MenuItem tagliere = item("tagliere-misto-sharing-platter-for-2");
		MenuItem lasagna = item("lasagna");
		MenuItem tiramisu = item("tiramisu");
		MenuItem brownie = item("triple-chocolate-brownie");
		MenuItem nutella = item("calzone-alla-nutella");
		MenuItem mixedSides = item("mixed-sides");
		MenuItem rocketSide = item("rocket-side");

		assertThat(olives.getImagePath()).isEqualTo("/images/menu/starters/olive-di-nocellara.webp");
		assertThat(schiacciatella.getImagePath()).isEqualTo("/images/menu/starters/schiacciatella.webp");
		assertThat(bruschetta.getImagePath()).isEqualTo("/images/menu/starters/bruschetta.webp");
		assertThat(formaggio.getImagePath()).isEqualTo("/images/menu/starters/schiacciata-al-formaggio.webp");
		assertThat(bread.getImagePath()).isEqualTo("/images/menu/starters/oppane-the-bread.webp");
		assertThat(piadina.getImagePath()).isEqualTo("/images/menu/starters/piadina-e-crema-di-pomodoro.webp");
		assertThat(sfizio.getImagePath()).isEqualTo("/images/menu/starters/sfizio-al-pomodoro.webp");
		assertThat(tagliere.getImagePath()).isEqualTo("/images/menu/starters/tagliere-misto.webp");
		assertThat(margherita.getImagePath()).isEqualTo("/images/menu/margherita.webp");
		assertImagePath("lerrore-the-mistake", "/images/menu/pizzas/lerrore-the-mistake.webp");
		assertImagePath("piccante-formaggiata", "/images/menu/pizzas/piccante-formaggiata.webp");
		assertImagePath("tofu-mediterraneo", "/images/menu/pizzas/tofu-mediterraneo.webp");
		assertImagePath("a-bella-figliola", "/images/menu/pizzas/a-bella-figliola.webp");
		assertImagePath("pizza-e-fantasia", "/images/menu/pizzas/pizza-e-fantasia.webp");
		assertImagePath("pizza-cacio-e-pepe", "/images/menu/pizzas/pizza-cacio-e-pepe.webp");
		assertImagePath("alicella-sagliuta", "/images/menu/pizzas/alicella-sagliuta.webp");
		assertImagePath("verdure-fresche", "/images/menu/pizzas/verdure-fresche.webp");
		assertImagePath("pepperoni-doppio-formaggio", "/images/menu/pizzas/pepperoni-doppio-formaggio.webp");
		assertImagePath("o-core-e-napule", "/images/menu/pizzas/o-core-e-napule.webp");
		assertImagePath("pizza-do-putecaro", "/images/menu/pizzas/pizza-do-putecaro.webp");
		assertImagePath("pizza-bufala", "/images/menu/pizzas/pizza-bufala.webp");
		assertImagePath("lazzarella", "/images/menu/pizzas/lazzarella.webp");
		assertImagePath("pollo-funghi-dolce", "/images/menu/pizzas/pollo-funghi-dolce.webp");
		assertThat(lasagna.getImagePath()).isEqualTo("/images/menu/lasagna.webp");
		assertThat(tiramisu.getImagePath()).isEqualTo("/images/menu/sweet-tooth/tiramisu.webp");
		assertThat(brownie.getImagePath()).isEqualTo("/images/menu/sweet-tooth/triple-chocolate-brownie.webp");
		assertThat(nutella.getImagePath()).isEqualTo("/images/menu/sweet-tooth/calzone-alla-nutella.webp");
		assertThat(mixedSides.getImagePath()).isEqualTo("/images/menu/side-salads/mixed-salad.webp");
		assertThat(rocketSide.getImagePath()).isEqualTo("/images/menu/side-salads/rocket-side.webp");

		assertThat(margherita.getDietaryTags()).containsExactlyInAnyOrder(DietaryTag.V, DietaryTag.GF, DietaryTag.VE);
		assertThat(lasagna.getDietaryTags()).isEmpty();
		assertThat(nutella.getDietaryTags()).isEmpty();
	}

	@Test
	void databaseConstraintsHoldForSeededMenu() {
		assertThat(queryForLong("""
				select count(*)
				from (
				    select slug
				    from menu_items
				    group by slug
				    having count(*) > 1
				) duplicate_slugs
				""")).isZero();

		assertThat(queryForLong("""
				select count(*)
				from menu_item_dietary_tags
				where tag not in ('V', 'GF', 'VE')
				""")).isZero();

		assertThat(queryForLong("select count(*) from menu_items where price_pence < 0")).isZero();
	}

	private MenuItem item(String slug) {
		return itemRepository.findBySlug(slug)
				.orElseThrow();
	}

	private void assertImagePath(String slug, String imagePath) {
		assertThat(item(slug).getImagePath()).isEqualTo(imagePath);
	}

	private Long queryForLong(String sql) {
		return jdbcTemplate.queryForObject(sql, Long.class);
	}
}
