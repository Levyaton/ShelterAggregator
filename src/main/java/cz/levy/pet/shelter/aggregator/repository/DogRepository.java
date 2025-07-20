package cz.levy.pet.shelter.aggregator.repository;

import cz.levy.pet.shelter.aggregator.entity.DogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DogRepository extends JpaRepository<DogEntity, Long> {}
