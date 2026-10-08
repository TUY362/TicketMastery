package org.kt.util;

import java.time.LocalDateTime;

public final class SessionContext {

    private static SessionContext instancia;

    private Integer idUsuario;
    private String nombreUsuario;
    private String rol;
    private LocalDateTime horaInicio;

    private SessionContext() {
    }

    public static synchronized SessionContext getInstancia() {
        if (instancia == null) {
            instancia = new SessionContext();
        }
        return instancia;
    }

    public synchronized void iniciarSesion(
            int idUsuario, String nombreUsuario, String rol) {

        if (idUsuario <= 0) {
            throw new IllegalArgumentException("El usuario no es válido.");
        }

        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            throw new IllegalArgumentException(
                "El nombre de usuario es obligatorio."
            );
        }

        if (!"TAQUILLERO".equals(rol)
                && !"ORGANIZADOR".equals(rol)
                && !"VALIDADOR".equals(rol)) {
            throw new IllegalArgumentException("El rol no es válido.");
        }

        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.rol = rol;
        this.horaInicio = LocalDateTime.now();
    }

    public synchronized void cerrarSesion() {
        idUsuario = null;
        nombreUsuario = null;
        rol = null;
        horaInicio = null;
    }

    public synchronized boolean haySesionActiva() {
        return idUsuario != null;
    }

    public synchronized Integer getIdUsuario() {
        return idUsuario;
    }

    public synchronized String getNombreUsuario() {
        return nombreUsuario;
    }

    public synchronized String getRol() {
        return rol;
    }

    public synchronized LocalDateTime getHoraInicio() {
        return horaInicio;
    }
}