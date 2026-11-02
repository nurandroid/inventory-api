ALTER TABLE products
    ADD COLUMN IF NOT EXISTS price NUMERIC(12, 2);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM products
        WHERE price IS NULL OR price <= 0
    ) THEN
        RAISE EXCEPTION
            'Product prices are missing or invalid. Populate prices from an approved price list before retrying the migration.';
END IF;
END
$$;

ALTER TABLE products
    ALTER COLUMN price SET NOT NULL;
