package com.jsebastian.eden.EdenSys.repository;

import com.jsebastian.eden.EdenSys.domain.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResenaRepository extends JpaRepository<Resena, Long> {

    Optional<Resena> findByAsesoriaId(Long asesoriaId);

    boolean existsByAsesoriaId(Long asesoriaId);

    // Reseñas recibidas por un tutor, a través de la asesoría
    List<Resena> findByAsesoriaAsesorIdOrderByIdDesc(Long asesorId);
}