package cz.levy.pet.shelter.aggregator.repository;

import cz.levy.pet.shelter.aggregator.entity.ShelterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShelterRepository extends JpaRepository<ShelterEntity, Long> {}
