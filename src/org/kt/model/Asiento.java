package org.kt.model;

import java.time.LocalDateTime;

public class Asiento {

    private int idAsiento;
    private int idZona;
    private String fila;
    private int numero;
    private String tokenReserva;
    private LocalDateTime reservaHasta;

    public Asiento() {
    }

    public Asiento(int idAsiento, int idZona, String fila,
            int numero, String tokenReserva,
            LocalDateTime reservaHasta) {
        this.idAsiento = idAsiento;
        this.idZona = idZona;
        this.fila = fila;
        this.numero = numero;
        this.tokenReserva = tokenReserva;
        this.reservaHasta = reservaHasta;
    }

    public int getIdAsiento() {
        return idAsiento;
    }

    public void setIdAsiento(int idAsiento) {
        this.idAsiento = idAsiento;
    }

    public int getIdZona() {
        return idZona;
    }

    public void setIdZona(int idZona) {
        this.idZona = idZona;
    }

    public String getFila() {
        return fila;
    }

    public void setFila(String fila) {
        this.fila = fila;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public String getTokenReserva() {
        return tokenReserva;
    }

    public void setTokenReserva(String tokenReserva) {
        this.tokenReserva = tokenReserva;
    }

    public LocalDateTime getReservaHasta() {
        return reservaHasta;
    }

    public void setReservaHasta(LocalDateTime reservaHasta) {
        this.reservaHasta = reservaHasta;
    }

    @Override
    public String toString() {
        return fila + "-" + numero;
    }
}