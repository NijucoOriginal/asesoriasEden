package com.jsebastian.eden.EdenSys.repository;

import com.jsebastian.eden.EdenSys.domain.Materia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MateriaRepository extends JpaRepository<Materia, Long> {
}