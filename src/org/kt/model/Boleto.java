package org.kt.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Boleto {

    private int idBoleto;
    private int idVenta;
    private int idAsiento;
    private String codigo;
    private BigDecimal precioPagado;
    private String estado;
    private LocalDateTime fechaEmision;
    private LocalDateTime fechaValidacion;
    private Integer idValidador;

    public Boleto() {
    }

    public Boleto(int idBoleto, int idVenta, int idAsiento,
            String codigo, BigDecimal precioPagado, String estado,
            LocalDateTime fechaEmision, LocalDateTime fechaValidacion,
            Integer idValidador) {
        this.idBoleto = idBoleto;
        this.idVenta = idVenta;
        this.idAsiento = idAsiento;
        this.codigo = codigo;
        this.precioPagado = precioPagado;
        this.estado = estado;
        this.fechaEmision = fechaEmision;
        this.fechaValidacion = fechaValidacion;
        this.idValidador = idValidador;
    }

    public int getIdBoleto() {
        return idBoleto;
    }

    public void setIdBoleto(int idBoleto) {
        this.idBoleto = idBoleto;
    }

    public int getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(int idVenta) {
        this.idVenta = idVenta;
    }

    public int getIdAsiento() {
        return idAsiento;
    }

    public void setIdAsiento(int idAsiento) {
        this.idAsiento = idAsiento;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public BigDecimal getPrecioPagado() {
        return precioPagado;
    }

    public void setPrecioPagado(BigDecimal precioPagado) {
        this.precioPagado = precioPagado;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public LocalDateTime getFechaValidacion() {
        return fechaValidacion;
    }

    public void setFechaValidacion(LocalDateTime fechaValidacion) {
        this.fechaValidacion = fechaValidacion;
    }

    public Integer getIdValidador() {
        return idValidador;
    }

    public void setIdValidador(Integer idValidador) {
        this.idValidador = idValidador;
    }

    @Override
    public String toString() {
        return codigo;
    }
}