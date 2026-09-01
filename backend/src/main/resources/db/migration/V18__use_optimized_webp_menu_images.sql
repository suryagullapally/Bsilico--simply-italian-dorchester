with optimized_menu_images(slug, image_path) as (
    values
        ('olive-di-nocellara', '/images/menu/starters/olive-di-nocellara.webp'),
        ('schiacciatella', '/images/menu/starters/schiacciatella.webp'),
        ('bruschetta', '/images/menu/starters/bruschetta.webp'),
        ('schiacciata-al-formaggio', '/images/menu/starters/schiacciata-al-formaggio.webp'),
        ('oppane-the-bread', '/images/menu/starters/oppane-the-bread.webp'),
        ('piadina-e-crema-di-pomodoro', '/images/menu/starters/piadina-e-crema-di-pomodoro.webp'),
        ('sfizio-al-pomodoro', '/images/menu/starters/sfizio-al-pomodoro.webp'),
        ('tagliere-misto-sharing-platter-for-2', '/images/menu/starters/tagliere-misto.webp'),
        ('pizza-margherita', '/images/menu/margherita.webp'),
        ('lerrore-the-mistake', '/images/menu/pizzas/lerrore-the-mistake.webp'),
        ('piccante-formaggiata', '/images/menu/pizzas/piccante-formaggiata.webp'),
        ('tofu-mediterraneo', '/images/menu/pizzas/tofu-mediterraneo.webp'),
        ('a-bella-figliola', '/images/menu/pizzas/a-bella-figliola.webp'),
        ('pizza-e-fantasia', '/images/menu/pizzas/pizza-e-fantasia.webp'),
        ('pizza-cacio-e-pepe', '/images/menu/pizzas/pizza-cacio-e-pepe.webp'),
        ('alicella-sagliuta', '/images/menu/pizzas/alicella-sagliuta.webp'),
        ('verdure-fresche', '/images/menu/pizzas/verdure-fresche.webp'),
        ('pepperoni-doppio-formaggio', '/images/menu/pizzas/pepperoni-doppio-formaggio.webp'),
        ('o-core-e-napule', '/images/menu/pizzas/o-core-e-napule.webp'),
        ('pizza-do-putecaro', '/images/menu/pizzas/pizza-do-putecaro.webp'),
        ('pizza-bufala', '/images/menu/pizzas/pizza-bufala.webp'),
        ('lazzarella', '/images/menu/pizzas/lazzarella.webp'),
        ('pollo-funghi-dolce', '/images/menu/pizzas/pollo-funghi-dolce.webp'),
        ('lasagna', '/images/menu/lasagna.webp'),
        ('tiramisu', '/images/menu/sweet-tooth/tiramisu.webp'),
        ('triple-chocolate-brownie', '/images/menu/sweet-tooth/triple-chocolate-brownie.webp'),
        ('calzone-alla-nutella', '/images/menu/sweet-tooth/calzone-alla-nutella.webp'),
        ('mixed-sides', '/images/menu/side-salads/mixed-salad.webp'),
        ('rocket-side', '/images/menu/side-salads/rocket-side.webp')
)
update menu_items
set image_path = optimized_menu_images.image_path,
    updated_at = now()
from optimized_menu_images
where menu_items.slug = optimized_menu_images.slug;

do $$
declare
    matched_count integer;
begin
    with optimized_menu_images(slug, image_path) as (
        values
            ('olive-di-nocellara', '/images/menu/starters/olive-di-nocellara.webp'),
            ('schiacciatella', '/images/menu/starters/schiacciatella.webp'),
            ('bruschetta', '/images/menu/starters/bruschetta.webp'),
            ('schiacciata-al-formaggio', '/images/menu/starters/schiacciata-al-formaggio.webp'),
            ('oppane-the-bread', '/images/menu/starters/oppane-the-bread.webp'),
            ('piadina-e-crema-di-pomodoro', '/images/menu/starters/piadina-e-crema-di-pomodoro.webp'),
            ('sfizio-al-pomodoro', '/images/menu/starters/sfizio-al-pomodoro.webp'),
            ('tagliere-misto-sharing-platter-for-2', '/images/menu/starters/tagliere-misto.webp'),
            ('pizza-margherita', '/images/menu/margherita.webp'),
            ('lerrore-the-mistake', '/images/menu/pizzas/lerrore-the-mistake.webp'),
            ('piccante-formaggiata', '/images/menu/pizzas/piccante-formaggiata.webp'),
            ('tofu-mediterraneo', '/images/menu/pizzas/tofu-mediterraneo.webp'),
            ('a-bella-figliola', '/images/menu/pizzas/a-bella-figliola.webp'),
            ('pizza-e-fantasia', '/images/menu/pizzas/pizza-e-fantasia.webp'),
            ('pizza-cacio-e-pepe', '/images/menu/pizzas/pizza-cacio-e-pepe.webp'),
            ('alicella-sagliuta', '/images/menu/pizzas/alicella-sagliuta.webp'),
            ('verdure-fresche', '/images/menu/pizzas/verdure-fresche.webp'),
            ('pepperoni-doppio-formaggio', '/images/menu/pizzas/pepperoni-doppio-formaggio.webp'),
            ('o-core-e-napule', '/images/menu/pizzas/o-core-e-napule.webp'),
            ('pizza-do-putecaro', '/images/menu/pizzas/pizza-do-putecaro.webp'),
            ('pizza-bufala', '/images/menu/pizzas/pizza-bufala.webp'),
            ('lazzarella', '/images/menu/pizzas/lazzarella.webp'),
            ('pollo-funghi-dolce', '/images/menu/pizzas/pollo-funghi-dolce.webp'),
            ('lasagna', '/images/menu/lasagna.webp'),
            ('tiramisu', '/images/menu/sweet-tooth/tiramisu.webp'),
            ('triple-chocolate-brownie', '/images/menu/sweet-tooth/triple-chocolate-brownie.webp'),
            ('calzone-alla-nutella', '/images/menu/sweet-tooth/calzone-alla-nutella.webp'),
            ('mixed-sides', '/images/menu/side-salads/mixed-salad.webp'),
            ('rocket-side', '/images/menu/side-salads/rocket-side.webp')
    )
    select count(*)
    into matched_count
    from menu_items
    join optimized_menu_images
      on menu_items.slug = optimized_menu_images.slug
     and menu_items.image_path = optimized_menu_images.image_path;

    if matched_count <> 29 then
        raise exception 'Expected to map 29 optimized menu image paths, mapped %', matched_count;
    end if;
end $$;
