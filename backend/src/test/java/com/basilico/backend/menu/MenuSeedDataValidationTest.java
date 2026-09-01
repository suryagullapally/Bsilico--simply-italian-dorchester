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

		assertThat(olives.getImagePath()).isEqualTo("/images/menu/starters/olive-di-nocellara.png");
		assertThat(schiacciatella.getImagePath()).isEqualTo("/images/menu/starters/schiacciatella.png");
		assertThat(bruschetta.getImagePath()).isEqualTo("/images/menu/starters/bruschetta.png");
		assertThat(formaggio.getImagePath()).isEqualTo("/images/menu/starters/schiacciata-al-formaggio.png");
		assertThat(bread.getImagePath()).isEqualTo("/images/menu/starters/oppane-the-bread.png");
		assertThat(piadina.getImagePath()).isEqualTo("/images/menu/starters/piadina-e-crema-di-pomodoro.png");
		assertThat(sfizio.getImagePath()).isEqualTo("/images/menu/starters/sfizio-al-pomodoro.png");
		assertThat(tagliere.getImagePath()).isEqualTo("/images/menu/starters/tagliere-misto.png");
		assertThat(margherita.getImagePath()).isEqualTo("/images/menu/margherita.png");
		assertImagePath("lerrore-the-mistake", "/images/menu/pizzas/lerrore-the-mistake.png");
		assertImagePath("piccante-formaggiata", "/images/menu/pizzas/piccante-formaggiata.png");
		assertImagePath("tofu-mediterraneo", "/images/menu/pizzas/tofu-mediterraneo.png");
		assertImagePath("a-bella-figliola", "/images/menu/pizzas/a-bella-figliola.png");
		assertImagePath("pizza-e-fantasia", "/images/menu/pizzas/pizza-e-fantasia.png");
		assertImagePath("pizza-cacio-e-pepe", "/images/menu/pizzas/pizza-cacio-e-pepe.png");
		assertImagePath("alicella-sagliuta", "/images/menu/pizzas/alicella-sagliuta.png");
		assertImagePath("verdure-fresche", "/images/menu/pizzas/verdure-fresche.png");
		assertImagePath("pepperoni-doppio-formaggio", "/images/menu/pizzas/pepperoni-doppio-formaggio.png");
		assertImagePath("o-core-e-napule", "/images/menu/pizzas/o-core-e-napule.png");
		assertImagePath("pizza-do-putecaro", "/images/menu/pizzas/pizza-do-putecaro.png");
		assertImagePath("pizza-bufala", "/images/menu/pizzas/pizza-bufala.png");
		assertImagePath("lazzarella", "/images/menu/pizzas/lazzarella.png");
		assertImagePath("pollo-funghi-dolce", "/images/menu/pizzas/pollo-funghi-dolce.png");
		assertThat(lasagna.getImagePath()).isEqualTo("/images/menu/lasagna.png");
		assertThat(tiramisu.getImagePath()).isEqualTo("/images/menu/sweet-tooth/tiramisu.png");
		assertThat(brownie.getImagePath()).isEqualTo("/images/menu/sweet-tooth/triple-chocolate-brownie.png");
		assertThat(nutella.getImagePath()).isEqualTo("/images/menu/sweet-tooth/calzone-alla-nutella.png");
		assertThat(mixedSides.getImagePath()).isEqualTo("/images/menu/side-salads/mixed-salad.png");
		assertThat(rocketSide.getImagePath()).isEqualTo("/images/menu/side-salads/rocket-side.png");

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
