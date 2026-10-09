-- Legacy time ranges retain their original meaning; do not infer reservations.
ALTER TABLE plan_anchors ADD COLUMN fixed_time TIME;
