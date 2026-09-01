with starter_images(slug, image_path) as (
    values
        ('olive-di-nocellara', '/images/menu/starters/olive-di-nocellara.png'),
        ('schiacciatella', '/images/menu/starters/schiacciatella.png'),
        ('bruschetta', '/images/menu/starters/bruschetta.png'),
        ('schiacciata-al-formaggio', '/images/menu/starters/schiacciata-al-formaggio.png'),
        ('oppane-the-bread', '/images/menu/starters/oppane-the-bread.png'),
        ('piadina-e-crema-di-pomodoro', '/images/menu/starters/piadina-e-crema-di-pomodoro.png'),
        ('sfizio-al-pomodoro', '/images/menu/starters/sfizio-al-pomodoro.png'),
        ('tagliere-misto-sharing-platter-for-2', '/images/menu/starters/tagliere-misto.png')
)
update menu_items
set image_path = starter_images.image_path,
    updated_at = now()
from starter_images
where menu_items.slug = starter_images.slug
  and menu_items.category_id = (
      select id
      from menu_categories
      where slug = 'bites-to-start'
  );

do $$
declare
    matched_count integer;
begin
    with starter_images(slug, image_path) as (
        values
            ('olive-di-nocellara', '/images/menu/starters/olive-di-nocellara.png'),
            ('schiacciatella', '/images/menu/starters/schiacciatella.png'),
            ('bruschetta', '/images/menu/starters/bruschetta.png'),
            ('schiacciata-al-formaggio', '/images/menu/starters/schiacciata-al-formaggio.png'),
            ('oppane-the-bread', '/images/menu/starters/oppane-the-bread.png'),
            ('piadina-e-crema-di-pomodoro', '/images/menu/starters/piadina-e-crema-di-pomodoro.png'),
            ('sfizio-al-pomodoro', '/images/menu/starters/sfizio-al-pomodoro.png'),
            ('tagliere-misto-sharing-platter-for-2', '/images/menu/starters/tagliere-misto.png')
    )
    select count(*)
    into matched_count
    from menu_items
    join starter_images
      on menu_items.slug = starter_images.slug
     and menu_items.image_path = starter_images.image_path
    join menu_categories
      on menu_categories.id = menu_items.category_id
    where menu_categories.slug = 'bites-to-start';

    if matched_count <> 8 then
        raise exception 'Expected to map 8 starter image paths, mapped %', matched_count;
    end if;
end $$;
