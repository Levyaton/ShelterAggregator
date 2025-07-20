CREATE TYPE sex_enum AS ENUM (
  'MALE',
  'FEMALE',
  'UNKNOWN'
);

CREATE TABLE shelter_entity (
  id   SERIAL PRIMARY KEY,
   name TEXT NOT NULL
        CHECK (char_length(name) > 0),
    address TEXT,
    phone_number TEXT,
    url TEXT,
    email TEXT
);

CREATE TABLE dog_entity (
  id                             SERIAL PRIMARY KEY,
  external_id                    TEXT       NOT NULL
                                      CHECK (char_length(external_id) > 0),
  shelter_url                    TEXT,
  name                           TEXT       NOT NULL
                                      CHECK (char_length(name) > 0),
  description                    TEXT,
  breed_guess                    TEXT,
  sex                            sex_enum   NOT NULL,
  estimated_age_in_years         REAL,
  current_weight                 REAL,
  estimated_final_weight_min     REAL,
  estimated_final_weight_max     REAL,
  dog_address                    TEXT,
    shelter_id INTEGER NOT NULL
      REFERENCES shelter_entity(id)
        ON DELETE RESTRICT,
  CONSTRAINT weight_range CHECK (
    estimated_final_weight_min <= estimated_final_weight_max
  ),
  CONSTRAINT ux_dog_entity_external_id_shelter_id UNIQUE (external_id, shelter_id)
);

CREATE TABLE dog_entity_image (
  id              SERIAL PRIMARY KEY,
  dog_entity_id  INTEGER   NOT NULL
                   REFERENCES dog_entity(id)
                     ON DELETE CASCADE,
  image_data      BYTEA     NOT NULL
);