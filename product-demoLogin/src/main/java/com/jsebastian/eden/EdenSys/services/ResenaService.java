package com.jsebastian.eden.EdenSys.services;

import com.jsebastian.eden.EdenSys.domain.Asesoria;
import com.jsebastian.eden.EdenSys.domain.Resena;
import com.jsebastian.eden.EdenSys.exceptions.ValueConflictException;
import com.jsebastian.eden.EdenSys.repository.AsesoriaRepository;
import com.jsebastian.eden.EdenSys.repository.ResenaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResenaService {

    private final ResenaRepository resenaRepository;
    private final AsesoriaRepository asesoriaRepository;
    private final AsesoriaService asesoriaService;

    @Transactional
    public Resena crear(String email, Long asesoriaId, int calificacion, String descripcion) {
        asesoriaService.validarPuedeResenar(email, asesoriaId);
        validarCalificacion(calificacion);

        if (resenaRepository.existsByAsesoriaId(asesoriaId)) {
            throw new ValueConflictException("Esta asesoría ya tiene una reseña. Puedes editarla.");
        }

        Asesoria a = asesoriaRepository.findById(asesoriaId)
                .orElseThrow(() -> new ValueConflictException("Asesoría no encontrada."));

        Resena r = new Resena();
        r.setAsesoria(a);
        r.setCalificacion(calificacion);
        r.setDescripcion(normalizarDescripcion(descripcion));
        return resenaRepository.save(r);
    }

    @Transactional
    public Resena editar(String email, Long asesoriaId, int calificacion, String descripcion) {
        asesoriaService.validarPuedeResenar(email, asesoriaId);
        validarCalificacion(calificacion);

        Resena r = resenaRepository.findByAsesoriaId(asesoriaId)
                .orElseThrow(() -> new ValueConflictException("La asesoría no tiene reseña."));

        // Sobrescribe, sin historial
        r.setCalificacion(calificacion);
        r.setDescripcion(normalizarDescripcion(descripcion));
        return resenaRepository.save(r);
    }

    @Transactional
    public void eliminar(String email, Long asesoriaId) {
        asesoriaService.validarPuedeResenar(email, asesoriaId);

        Resena r = resenaRepository.findByAsesoriaId(asesoriaId)
                .orElseThrow(() -> new ValueConflictException("La asesoría no tiene reseña."));
        resenaRepository.delete(r);
    }

    @Transactional(readOnly = true)
    public List<Resena> listarDeTutor(Long tutorId) {
        return resenaRepository.findByAsesoriaAsesorIdOrderByIdDesc(tutorId);
    }

    private void validarCalificacion(int c) {
        if (c < 1 || c > 5) {
            throw new ValueConflictException("La calificación debe estar entre 1 y 5.");
        }
    }

    private String normalizarDescripcion(String d) {
        if (d == null || d.isBlank()) return null;
        return d.trim();
    }
}