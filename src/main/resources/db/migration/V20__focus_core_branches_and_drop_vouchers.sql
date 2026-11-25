-- ============================================================================
-- V20: Focus on 3 Core Branches (HN, HCM, DN) & Remove Unused Voucher Schema
-- ============================================================================

-- 1. Keep only the 3 primary flagship branches active
UPDATE branches
SET active = FALSE
WHERE code NOT IN ('HN-THAIHA', 'HCM-QUAN10', 'DN-HAICHAU');

UPDATE branches
SET active = TRUE
WHERE code IN ('HN-THAIHA', 'HCM-QUAN10', 'DN-HAICHAU');

-- 2. Remove unused voucher columns from orders & drop vouchers table
DROP INDEX IF EXISTS idx_orders_voucher_id;
DROP INDEX IF EXISTS idx_vouchers_active;

ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS fk_orders_voucher;

ALTER TABLE orders
    DROP COLUMN IF EXISTS voucher_id,
    DROP COLUMN IF EXISTS voucher_code,
    DROP COLUMN IF EXISTS discount_amount;

DROP TABLE IF EXISTS vouchers;
