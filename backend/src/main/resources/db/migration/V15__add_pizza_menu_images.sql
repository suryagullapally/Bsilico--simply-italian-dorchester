with pizza_images(slug, image_path) as (
    values
        ('lerrore-the-mistake', '/images/menu/pizzas/lerrore-the-mistake.png'),
        ('piccante-formaggiata', '/images/menu/pizzas/piccante-formaggiata.png'),
        ('tofu-mediterraneo', '/images/menu/pizzas/tofu-mediterraneo.png'),
        ('a-bella-figliola', '/images/menu/pizzas/a-bella-figliola.png'),
        ('pizza-e-fantasia', '/images/menu/pizzas/pizza-e-fantasia.png'),
        ('pizza-cacio-e-pepe', '/images/menu/pizzas/pizza-cacio-e-pepe.png'),
        ('alicella-sagliuta', '/images/menu/pizzas/alicella-sagliuta.png'),
        ('verdure-fresche', '/images/menu/pizzas/verdure-fresche.png'),
        ('pepperoni-doppio-formaggio', '/images/menu/pizzas/pepperoni-doppio-formaggio.png'),
        ('o-core-e-napule', '/images/menu/pizzas/o-core-e-napule.png'),
        ('pizza-do-putecaro', '/images/menu/pizzas/pizza-do-putecaro.png'),
        ('pizza-bufala', '/images/menu/pizzas/pizza-bufala.png'),
        ('lazzarella', '/images/menu/pizzas/lazzarella.png'),
        ('pollo-funghi-dolce', '/images/menu/pizzas/pollo-funghi-dolce.png')
)
update menu_items
set image_path = pizza_images.image_path,
    updated_at = now()
from pizza_images
where menu_items.slug = pizza_images.slug
  and menu_items.category_id = (
      select id
      from menu_categories
      where slug = 'sourdough-pizza-calzone'
  );

do $$
declare
    matched_count integer;
begin
    with expected_pizza_images(slug, image_path) as (
        values
            ('pizza-margherita', '/images/menu/margherita.png'),
            ('lerrore-the-mistake', '/images/menu/pizzas/lerrore-the-mistake.png'),
            ('piccante-formaggiata', '/images/menu/pizzas/piccante-formaggiata.png'),
            ('tofu-mediterraneo', '/images/menu/pizzas/tofu-mediterraneo.png'),
            ('a-bella-figliola', '/images/menu/pizzas/a-bella-figliola.png'),
            ('pizza-e-fantasia', '/images/menu/pizzas/pizza-e-fantasia.png'),
            ('pizza-cacio-e-pepe', '/images/menu/pizzas/pizza-cacio-e-pepe.png'),
            ('alicella-sagliuta', '/images/menu/pizzas/alicella-sagliuta.png'),
            ('verdure-fresche', '/images/menu/pizzas/verdure-fresche.png'),
            ('pepperoni-doppio-formaggio', '/images/menu/pizzas/pepperoni-doppio-formaggio.png'),
            ('o-core-e-napule', '/images/menu/pizzas/o-core-e-napule.png'),
            ('pizza-do-putecaro', '/images/menu/pizzas/pizza-do-putecaro.png'),
            ('pizza-bufala', '/images/menu/pizzas/pizza-bufala.png'),
            ('lazzarella', '/images/menu/pizzas/lazzarella.png'),
            ('pollo-funghi-dolce', '/images/menu/pizzas/pollo-funghi-dolce.png')
    )
    select count(*)
    into matched_count
    from menu_items
    join expected_pizza_images
      on menu_items.slug = expected_pizza_images.slug
     and menu_items.image_path = expected_pizza_images.image_path
    join menu_categories
      on menu_categories.id = menu_items.category_id
    where menu_categories.slug = 'sourdough-pizza-calzone';

    if matched_count <> 15 then
        raise exception 'Expected to map 15 pizza image paths, mapped %', matched_count;
    end if;
end $$;
