package com.jsebastian.eden.EdenSys.services;

import com.jsebastian.eden.EdenSys.Dtos.TutorResponse;
import com.jsebastian.eden.EdenSys.domain.EstadoAsesoria;
import com.jsebastian.eden.EdenSys.domain.EstadoUsuario;
import com.jsebastian.eden.EdenSys.domain.Materia;
import com.jsebastian.eden.EdenSys.domain.Resena;
import com.jsebastian.eden.EdenSys.domain.User;
import com.jsebastian.eden.EdenSys.repository.ResenaRepository;
import com.jsebastian.eden.EdenSys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TutorService {

    private final UserRepository userRepository;
    private final ResenaRepository resenaRepository;

    @Transactional(readOnly = true)
    public List<TutorResponse> listar() {
        return userRepository.findByEstadoAndMateriasQueDictaIsNotEmpty(EstadoUsuario.ACTIVO)
                .stream()
                .map(this::aResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TutorResponse> obtener(Long id) {
        return userRepository.findById(id)
                .filter(u -> u.getEstado() == EstadoUsuario.ACTIVO && !u.getMateriasQueDicta().isEmpty())
                .map(this::aResponse);
    }

    private TutorResponse aResponse(User u) {
        List<String> materias = u.getMateriasQueDicta().stream()
                .map(Materia::getNombreMateria)
                .sorted()
                .toList();

        double horas = u.getAsesoriasDadas().stream()
                .filter(a -> a.getEstado() == EstadoAsesoria.FINALIZADA
                        && a.getFechaInicioConfirmada() != null
                        && a.getFechaCierreConfirmada() != null)
                .mapToDouble(a -> Duration.between(a.getFechaInicioConfirmada(),
                        a.getFechaCierreConfirmada()).toMinutes() / 60.0)
                .sum();

        List<Resena> resenas = resenaRepository.findByAsesoriaAsesorIdOrderByIdDesc(u.getId());
        Double promedio = resenas.isEmpty() ? null
                : Math.round(resenas.stream().mapToInt(Resena::getCalificacion).average().orElse(0.0) * 10) / 10.0;

        String carrera = u.getProgramaAcademico() != null
                ? u.getProgramaAcademico().getNombrePrograma()
                : null;

        return new TutorResponse(
                u.getId(),
                u.getNombre(),
                u.getApellido(),
                carrera,
                u.getImagenPerfil(),
                materias,
                Math.round(horas * 10) / 10.0,
                promedio,
                resenas.size());
    }
}