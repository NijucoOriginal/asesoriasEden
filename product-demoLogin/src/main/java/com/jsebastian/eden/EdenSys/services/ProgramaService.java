package com.jsebastian.eden.EdenSys.services;

import com.jsebastian.eden.EdenSys.Dtos.ProgramaResponse;
import com.jsebastian.eden.EdenSys.domain.Programa;
import com.jsebastian.eden.EdenSys.domain.User;
import com.jsebastian.eden.EdenSys.exceptions.ValueConflictException;
import com.jsebastian.eden.EdenSys.repository.AsesoriaRepository;
import com.jsebastian.eden.EdenSys.repository.ProgramaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgramaService {

    private final ProgramaRepository programaRepository;

    @Transactional(readOnly = true)
    public List<ProgramaResponse> catalogo() {
        return programaRepository.findAll().stream()
                .sorted(Comparator.comparing(Programa::getNombrePrograma))
                .map(p -> new ProgramaResponse(
                        p.getId(), p.getNombrePrograma(),
                        p.getPensum().size(), p.getCantidadCreditos()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Programa buscarObligatorio(Long id) {
        if (id == null) throw new ValueConflictException("Debes seleccionar tu carrera.");
        return programaRepository.findById(id)
                .orElseThrow(() -> new ValueConflictException("La carrera seleccionada no existe."));
    }

    @Transactional
    public void cambiarCarrera(User u, Long programaId, AsesoriaRepository asesorias) {
        Programa nuevo = buscarObligatorio(programaId);
        if (u.getProgramaAcademico() != null
                && u.getProgramaAcademico().getId().equals(nuevo.getId())) {
            return;
        }
        // Materias que enseña y que no son de la nueva carrera
        boolean conflicto = u.getMateriasQueDicta().stream()
                .anyMatch(m -> m.getPrograma() == null || !m.getPrograma().getId().equals(nuevo.getId()));
        if (conflicto) {
            throw new ValueConflictException(
                    "Tienes materias de otra carrera en tu oferta. Quítalas antes de cambiar de carrera.");
        }
        u.setProgramaAcademico(nuevo);
    }
}