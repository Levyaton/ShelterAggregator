ALTER TABLE dog_entity
  ALTER COLUMN sex TYPE TEXT
  USING sex::text;

DROP TYPE IF EXISTS sex_enum;