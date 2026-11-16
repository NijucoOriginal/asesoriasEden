package com.jsebastian.eden.EdenSys.repository;

import com.jsebastian.eden.EdenSys.domain.Asesoria;
import com.jsebastian.eden.EdenSys.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AsesoriaRepository extends JpaRepository<Asesoria, Long> {

    List<Asesoria> findByAsesorOrParticipanteOrderByActualizadaEnDesc(User asesor, User participante);

    @Query("""
        SELECT a FROM Asesoria a
        WHERE (a.asesor = :u OR a.participante = :u)
          AND a.estado IN (com.jsebastian.eden.EdenSys.domain.EstadoAsesoria.PENDIENTE,
                           com.jsebastian.eden.EdenSys.domain.EstadoAsesoria.CONTRAPROPUESTA,
                           com.jsebastian.eden.EdenSys.domain.EstadoAsesoria.ACEPTADA)
    """)
    List<Asesoria> findAbiertasDeUsuario(@Param("u") User u);

    @Query("""
        SELECT a.id FROM Asesoria a
        WHERE a.estado IN (com.jsebastian.eden.EdenSys.domain.EstadoAsesoria.PENDIENTE,
                           com.jsebastian.eden.EdenSys.domain.EstadoAsesoria.CONTRAPROPUESTA)
          AND a.actualizadaEn < :limite
    """)
    List<Long> findIdsVencidasParaExpirar(@Param("limite") LocalDateTime limite);

    @Query("""
        SELECT a.id FROM Asesoria a
        WHERE a.estado = com.jsebastian.eden.EdenSys.domain.EstadoAsesoria.ACEPTADA
          AND a.fechaCierreConfirmada < :ahora
    """)
    List<Long> findIdsAceptadasParaFinalizar(@Param("ahora") LocalDateTime ahora);
}