ALTER TABLE products
    ALTER COLUMN name SET NOT NULL,
    ADD CONSTRAINT chk_products_name_not_blank
        CHECK (btrim(name) <> ''),
    ADD CONSTRAINT chk_products_quantity_non_negative
        CHECK (quantity >= 0),
    ADD CONSTRAINT chk_products_price_positive
        CHECK (price > 0);