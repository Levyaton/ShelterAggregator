CREATE TABLE dog_image_urls (
    dog_id         INTEGER NOT NULL
                       REFERENCES dog_entity(id)
                         ON DELETE CASCADE,
    image_url      VARCHAR(2048) NOT NULL,
    CONSTRAINT pk_dog_image_urls PRIMARY KEY (dog_id, image_url)
);

DROP TABLE IF EXISTS dog_entity_image;