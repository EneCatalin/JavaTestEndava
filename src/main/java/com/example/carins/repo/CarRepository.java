package com.example.carins.repo;

import com.example.carins.model.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
    // TODO: enforce unique VIN at DB and via validation (exercise)
    @EntityGraph(attributePaths = {"owner"})
    List<Car> findAll();
    Optional<Car> findByVin(String vin);
}