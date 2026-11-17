package com.jsebastian.eden.EdenSys.services;

import com.jsebastian.eden.EdenSys.Dtos.MateriaResponse;
import com.jsebastian.eden.EdenSys.Dtos.MateriasTutorRequest;
import com.jsebastian.eden.EdenSys.domain.EstadoUsuario;
import com.jsebastian.eden.EdenSys.domain.Materia;
import com.jsebastian.eden.EdenSys.domain.User;
import com.jsebastian.eden.EdenSys.exceptions.ValueConflictException;
import com.jsebastian.eden.EdenSys.repository.AsesoriaRepository;
import com.jsebastian.eden.EdenSys.repository.MateriaRepository;
import com.jsebastian.eden.EdenSys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MateriaService {

    private final MateriaRepository materiaRepository;
    private final UserRepository userRepository;
    private final AsesoriaRepository asesoriaRepository;

    /** Catálogo completo de materias con sus temas. */
    @Transactional(readOnly = true)
    public List<MateriaResponse> catalogo() {
        return materiaRepository.findAll().stream()
                .sorted(Comparator.comparing(Materia::getNombreMateria))
                .map(this::aResponse)
                .toList();
    }

    /** Materias que dicta el usuario logueado. */
    @Transactional(readOnly = true)
    public List<MateriaResponse> materiasDe(String email) {
        User u = buscarActivo(email);
        return u.getMateriasQueDicta().stream()
                .sorted(Comparator.comparing(Materia::getNombreMateria))
                .map(this::aResponse)
                .toList();
    }

    /**
     * Reemplaza la lista de materias que dicta el usuario.
     * No permite quitar materias con asesorías abiertas como tutor.
     */
    @Transactional
    public List<MateriaResponse> reemplazar(String email, MateriasTutorRequest req) {
        User u = buscarActivo(email);

        Set<Long> nuevas = new HashSet<>(req.materiaIds() == null ? List.of() : req.materiaIds());
        List<Materia> encontradas = materiaRepository.findAllById(nuevas);
        if (encontradas.size() != nuevas.size()) {
            throw new ValueConflictException("Alguna materia seleccionada no existe.");
        }

        Set<Long> actuales = new HashSet<>();
        u.getMateriasQueDicta().forEach(m -> actuales.add(m.getId()));

        Set<Long> quitadas = new HashSet<>(actuales);
        quitadas.removeAll(nuevas);

        if (!quitadas.isEmpty()
                && asesoriaRepository.contarAbiertasDeMateriasDe(u, quitadas) > 0) {
            throw new ValueConflictException(
                    "No puedes quitar una materia que tiene asesorías pendientes, en contrapropuesta o aceptadas.");
        }

        u.getMateriasQueDicta().clear();
        u.getMateriasQueDicta().addAll(encontradas);
        userRepository.save(u);

        return u.getMateriasQueDicta().stream()
                .sorted(Comparator.comparing(Materia::getNombreMateria))
                .map(this::aResponse)
                .toList();
    }

    private User buscarActivo(String email) {
        User u = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ValueConflictException("Usuario no encontrado."));
        if (u.getEstado() != EstadoUsuario.ACTIVO) {
            throw new ValueConflictException("Tu cuenta no está activa.");
        }
        return u;
    }

    private MateriaResponse aResponse(Materia m) {
        return new MateriaResponse(
                m.getId(),
                m.getNombreMateria(),
                m.getPrograma() != null ? m.getPrograma().getId() : null,
                m.getPrograma() != null ? m.getPrograma().getNombrePrograma() : null,
                m.getTemas().stream()
                        .map(t -> new MateriaResponse.TemaResumen(t.getId(), t.getNombreTema()))
                        .toList());
    }
}