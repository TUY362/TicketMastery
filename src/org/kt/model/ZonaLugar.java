package org.kt.model;

import java.math.BigDecimal;

public class ZonaLugar {

    private int idZona;
    private int idEvento;
    private String nombre;
    private BigDecimal precio;

    public ZonaLugar() {
    }

    public ZonaLugar(int idZona, int idEvento,
            String nombre, BigDecimal precio) {
        this.idZona = idZona;
        this.idEvento = idEvento;
        this.nombre = nombre;
        this.precio = precio;
    }

    public int getIdZona() {
        return idZona;
    }

    public void setIdZona(int idZona) {
        this.idZona = idZona;
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

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    @Override
    public String toString() {
        return nombre;
    }
}