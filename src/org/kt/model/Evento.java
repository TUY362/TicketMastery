package org.kt.model;

import java.time.LocalDateTime;

public class Evento {

    private int idEvento;
    private String nombre;
    private LocalDateTime fechaHora;
    private String lugar;
    private boolean activo;

    public Evento() {
    }

    public Evento(int idEvento, String nombre,
            LocalDateTime fechaHora, String lugar, boolean activo) {
        this.idEvento = idEvento;
        this.nombre = nombre;
        this.fechaHora = fechaHora;
        this.lugar = lugar;
        this.activo = activo;
    }

    public int getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(int idEvento) {
        this.idEvento = idEvento;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getLugar() {
        return lugar;
    }

    public void setLugar(String lugar) {
        this.lugar = lugar;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return nombre;
    }
}