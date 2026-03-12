-- 14 EU allergens (Regulation EU 1169/2011)
INSERT INTO allergen (code) VALUES
    ('GLUTEN'),
    ('CRUSTACEANS'),
    ('EGGS'),
    ('FISH'),
    ('PEANUTS'),
    ('SOYA'),
    ('MILK'),
    ('TREE_NUTS'),
    ('CELERY'),
    ('MUSTARD'),
    ('SESAME'),
    ('SULPHITES'),
    ('LUPIN'),
    ('MOLLUSCS');

-- Spanish translations
INSERT INTO allergen_translation (allergen_id, locale, name, description)
SELECT id, 'es', name_es, desc_es FROM (VALUES
    ('GLUTEN', 'Gluten', 'Cereales que contienen gluten y productos derivados'),
    ('CRUSTACEANS', 'Crustáceos', 'Crustáceos y productos a base de crustáceos'),
    ('EGGS', 'Huevos', 'Huevos y productos a base de huevo'),
    ('FISH', 'Pescado', 'Pescado y productos a base de pescado'),
    ('PEANUTS', 'Cacahuetes', 'Cacahuetes y productos a base de cacahuetes'),
    ('SOYA', 'Soja', 'Soja y productos a base de soja'),
    ('MILK', 'Leche', 'Leche y sus derivados (incluida la lactosa)'),
    ('TREE_NUTS', 'Frutos de cáscara', 'Frutos de cáscara y productos derivados'),
    ('CELERY', 'Apio', 'Apio y productos derivados'),
    ('MUSTARD', 'Mostaza', 'Mostaza y productos derivados'),
    ('SESAME', 'Sésamo', 'Granos de sésamo y productos a base de granos de sésamo'),
    ('SULPHITES', 'Dióxido de azufre y sulfitos', 'Dióxido de azufre y sulfitos en concentraciones superiores a 10 mg/kg'),
    ('LUPIN', 'Altramuces', 'Altramuces y productos a base de altramuces'),
    ('MOLLUSCS', 'Moluscos', 'Moluscos y productos a base de moluscos')
) AS t(code_val, name_es, desc_es)
JOIN allergen a ON a.code = t.code_val;

-- English translations
INSERT INTO allergen_translation (allergen_id, locale, name, description)
SELECT id, 'en', name_en, desc_en FROM (VALUES
    ('GLUTEN', 'Gluten', 'Cereals containing gluten and products thereof'),
    ('CRUSTACEANS', 'Crustaceans', 'Crustaceans and products thereof'),
    ('EGGS', 'Eggs', 'Eggs and products thereof'),
    ('FISH', 'Fish', 'Fish and products thereof'),
    ('PEANUTS', 'Peanuts', 'Peanuts and products thereof'),
    ('SOYA', 'Soybeans', 'Soybeans and products thereof'),
    ('MILK', 'Milk', 'Milk and products thereof (including lactose)'),
    ('TREE_NUTS', 'Nuts', 'Nuts and products thereof'),
    ('CELERY', 'Celery', 'Celery and products thereof'),
    ('MUSTARD', 'Mustard', 'Mustard and products thereof'),
    ('SESAME', 'Sesame', 'Sesame seeds and products thereof'),
    ('SULPHITES', 'Sulphur dioxide and sulphites', 'Sulphur dioxide and sulphites at concentrations of more than 10 mg/kg'),
    ('LUPIN', 'Lupin', 'Lupin and products thereof'),
    ('MOLLUSCS', 'Molluscs', 'Molluscs and products thereof')
) AS t(code_val, name_en, desc_en)
JOIN allergen a ON a.code = t.code_val;

-- Catalan translations
INSERT INTO allergen_translation (allergen_id, locale, name)
SELECT id, 'ca', name_ca FROM (VALUES
    ('GLUTEN', 'Gluten'),
    ('CRUSTACEANS', 'Crustacis'),
    ('EGGS', 'Ous'),
    ('FISH', 'Peix'),
    ('PEANUTS', 'Cacauets'),
    ('SOYA', 'Soja'),
    ('MILK', 'Llet'),
    ('TREE_NUTS', 'Fruits de closca'),
    ('CELERY', 'Api'),
    ('MUSTARD', 'Mostassa'),
    ('SESAME', 'Sèsam'),
    ('SULPHITES', 'Diòxid de sofre i sulfits'),
    ('LUPIN', 'Tramussos'),
    ('MOLLUSCS', 'Mol·luscs')
) AS t(code_val, name_ca)
JOIN allergen a ON a.code = t.code_val;

-- Basque translations
INSERT INTO allergen_translation (allergen_id, locale, name)
SELECT id, 'eu', name_eu FROM (VALUES
    ('GLUTEN', 'Glutena'),
    ('CRUSTACEANS', 'Krustazeo'),
    ('EGGS', 'Arrautzak'),
    ('FISH', 'Arraina'),
    ('PEANUTS', 'Kakahueteak'),
    ('SOYA', 'Soja'),
    ('MILK', 'Esnea'),
    ('TREE_NUTS', 'Fruitu lehorrak'),
    ('CELERY', 'Apio-belarra'),
    ('MUSTARD', 'Ziapea'),
    ('SESAME', 'Sesamo'),
    ('SULPHITES', 'Sufre dioxidoa eta sulfitoak'),
    ('LUPIN', 'Lupinuak'),
    ('MOLLUSCS', 'Moluskuak')
) AS t(code_val, name_eu)
JOIN allergen a ON a.code = t.code_val;

-- Galician translations
INSERT INTO allergen_translation (allergen_id, locale, name)
SELECT id, 'gl', name_gl FROM (VALUES
    ('GLUTEN', 'Glute'),
    ('CRUSTACEANS', 'Crustáceos'),
    ('EGGS', 'Ovos'),
    ('FISH', 'Peixe'),
    ('PEANUTS', 'Cacahuetes'),
    ('SOYA', 'Soia'),
    ('MILK', 'Leite'),
    ('TREE_NUTS', 'Froitas de casca'),
    ('CELERY', 'Apio'),
    ('MUSTARD', 'Mostaza'),
    ('SESAME', 'Sésamo'),
    ('SULPHITES', 'Dióxido de xofre e sulfitos'),
    ('LUPIN', 'Altramuz'),
    ('MOLLUSCS', 'Moluscos')
) AS t(code_val, name_gl)
JOIN allergen a ON a.code = t.code_val;
