ALTER TABLE dog_entity 
ADD COLUMN is_dog_available BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_dog_entity_is_dog_available ON dog_entity(is_dog_available);

