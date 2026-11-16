package com.jsebastian.eden.EdenSys.services.interfaces;

import com.jsebastian.eden.EdenSys.Dtos.*;
import com.jsebastian.eden.EdenSys.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    UsuarioResponse crearUsuario(CrearUsuarioDto dto);
    Optional<User> buscarPorId(Long id);
    Optional<User> buscarPorEmail(String email);
    Optional<User> buscarPorCedula(String cedula);
    List<User> obtenerTodosLosUsuarios();
    void eliminarUsuario(Long id);
    void eliminarUsuarioPorEmail(String email);
    void desvincularUsuario(String email);
    boolean existePorEmail(String email);
    boolean existePorCedula(String cedula);
    String actualizarUsuarioEmail(String email, CrearUsuarioDto dto);
    boolean activarUsuario(String codigo);
    String validarCredencialesYGenerarToken(String email, String contrasena);
    void enviarCodigoRecuperacionContrasena(SolicitarRecuperacionDto dto);
    boolean cambiarContrasenaConCodigo(CambiarContrasenaDto dto);
    void procesarContacto(ContactoDto contacto);
    String generarToken(User usuario);

    void procesarContacto(ContactoDto contacto);
}