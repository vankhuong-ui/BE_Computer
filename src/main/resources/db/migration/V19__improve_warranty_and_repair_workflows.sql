ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP;

UPDATE orders o
SET paid_at = COALESCE(
    (
        SELECT MIN(p.created_at)
        FROM payments p
        WHERE p.order_id = o.id
          AND UPPER(p.status) = 'SUCCESS'
    ),
    o.created_at,
    CURRENT_TIMESTAMP
)
WHERE UPPER(o.status) IN ('COMPLETED', 'PAID')
  AND o.paid_at IS NULL;

UPDATE warranty_claims wc
SET warranty_expire_date = o.paid_at + wc.warranty_months * INTERVAL '1 month'
FROM orders o
WHERE wc.order_id = o.id
  AND o.paid_at IS NOT NULL
  AND wc.warranty_months IS NOT NULL;

ALTER TABLE warranty_claims
    ADD COLUMN IF NOT EXISTS unit_number INT NOT NULL DEFAULT 1;

WITH active_claim_units AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY order_id, product_id
               ORDER BY created_at, id
           )::INT AS unit_number
    FROM warranty_claims
    WHERE status IN ('PENDING', 'RECEIVED', 'REPAIRING')
)
UPDATE warranty_claims wc
SET unit_number = active_claim_units.unit_number
FROM active_claim_units
WHERE wc.id = active_claim_units.id;

UPDATE warranty_claims
SET serial_code = 'BH-' || order_id::TEXT || '-' || product_id::TEXT || '-' || unit_number::TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS uk_warranty_active_claim_per_unit
    ON warranty_claims (order_id, product_id, unit_number)
    WHERE status IN ('PENDING', 'RECEIVED', 'REPAIRING');

CREATE INDEX IF NOT EXISTS idx_repair_bookings_schedule
    ON repair_bookings (appointment_date, appointment_time, status);
