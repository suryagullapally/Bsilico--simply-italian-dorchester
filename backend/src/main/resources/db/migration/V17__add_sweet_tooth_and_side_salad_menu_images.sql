UPDATE menu_items
SET image_path = '/images/menu/sweet-tooth/tiramisu.png',
    updated_at = now()
WHERE slug = 'tiramisu';

UPDATE menu_items
SET image_path = '/images/menu/sweet-tooth/triple-chocolate-brownie.png',
    updated_at = now()
WHERE slug = 'triple-chocolate-brownie';

UPDATE menu_items
SET image_path = '/images/menu/sweet-tooth/calzone-alla-nutella.png',
    updated_at = now()
WHERE slug = 'calzone-alla-nutella';

UPDATE menu_items
SET image_path = '/images/menu/side-salads/mixed-salad.png',
    updated_at = now()
WHERE slug = 'mixed-sides';

UPDATE menu_items
SET image_path = '/images/menu/side-salads/rocket-side.png',
    updated_at = now()
WHERE slug = 'rocket-side';
