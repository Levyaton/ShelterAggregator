-- Insert 5 shelters
INSERT INTO shelter_entity (name, address, url, email, is_non_profit, bank_account_number)
SELECT
  'Shelter ' || i,
  '1234 Elm St, City' || i,
  'http://shelter' || i || '.example.com',
  'contact' || i || '@shelter.example.com',
  FALSE,
  '000-000' || i
FROM generate_series(1, 5) AS s(i);

-- Insert 30 dogs per shelter
INSERT INTO dog_entity (
  external_id, shelter_url, name, description, breed_guess,
  sex, estimated_age_in_years, current_weight,
  estimated_final_weight_min, estimated_final_weight_max,
  dog_address, shelter_id
)
SELECT
  'EXT-' || s.id || '-DOG-' || d,
  'http://shelter' || s.id || '.example.com/dogs/' || d,
  'Dog ' || d || ' of Shelter ' || s.id,
  'Description for dog ' || d,
  'Breed' || (s.id + d),
  'UNKNOWN',
  round(random() * 10)::REAL,
  round(random() * 30)::REAL,
  round(random() * 20)::REAL,
  round(random() * 40 + 20)::REAL,
  '456 Oak Ave, City' || s.id,
  s.id
FROM shelter_entity s
CROSS JOIN generate_series(1, 30) AS d;

-- Insert 0–5 unique images per dog
INSERT INTO dog_image_urls (dog_id, image_url)
SELECT
  d.id,
  '/images/default_' || img.n || '.jpg'
FROM dog_entity d
JOIN generate_series(1, 5) AS img(n) ON random() < 0.5;