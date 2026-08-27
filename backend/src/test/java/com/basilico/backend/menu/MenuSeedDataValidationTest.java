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
		MenuItem margherita = item("pizza-margherita");
		MenuItem schiacciatella = item("schiacciatella");
		MenuItem lasagna = item("lasagna");
		MenuItem nutella = item("calzone-alla-nutella");

		assertThat(margherita.getImagePath()).isEqualTo("/images/menu/margherita.png");
		assertThat(schiacciatella.getImagePath()).isEqualTo("/images/menu/schiacciatella.png");
		assertThat(lasagna.getImagePath()).isEqualTo("/images/menu/lasagna.png");
		assertThat(nutella.getImagePath()).isEqualTo("/images/menu/calzoneallanutella.png");

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

	private Long queryForLong(String sql) {
		return jdbcTemplate.queryForObject(sql, Long.class);
	}
}
