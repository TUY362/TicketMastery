package org.kt.model;

import java.time.LocalDateTime;

public class VentaTransaccion {

    private int idVenta;
    private int idCliente;
    private int idTaquillero;
    private LocalDateTime fechaCreacion;
    private String estado;
    private String referenciaPago;
    private LocalDateTime fechaPago;

    public VentaTransaccion() {
    }

    public VentaTransaccion(int idVenta, int idCliente,
            int idTaquillero, LocalDateTime fechaCreacion,
            String estado, String referenciaPago,
            LocalDateTime fechaPago) {
        this.idVenta = idVenta;
        this.idCliente = idCliente;
        this.idTaquillero = idTaquillero;
        this.fechaCreacion = fechaCreacion;
        this.estado = estado;
        this.referenciaPago = referenciaPago;
        this.fechaPago = fechaPago;
    }

    public int getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(int idVenta) {
        this.idVenta = idVenta;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public int getIdTaquillero() {
        return idTaquillero;
    }

    public void setIdTaquillero(int idTaquillero) {
        this.idTaquillero = idTaquillero;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getReferenciaPago() {
        return referenciaPago;
    }

    public void setReferenciaPago(String referenciaPago) {
        this.referenciaPago = referenciaPago;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    @Override
    public String toString() {
        return "Venta " + idVenta + " - " + estado;
    }
}