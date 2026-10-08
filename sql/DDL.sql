-- Manuel de Jesús Tavico Ramos | Carné: 2026272
-- Proyecto 9: Taquilla TicketMastery | Fase 1
-- COMMIT 1: DDL (estructura). Ejecutar primero en una base nueva.
-- Proyecto 9: Taquilla TicketMastery / Fase 1
-- MySQL 8.0.16 o superior. Ejecutar UNA VEZ en una instalación nueva.
-- No elimina ni sobrescribe bases existentes: CREATE DATABASE fallará si ya existe.
CREATE DATABASE ticketmastery_fase1 CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE ticketmastery_fase1;
SET NAMES utf8mb4;

CREATE TABLE Usuario (
 id_usuario INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 nombre VARCHAR(100) NOT NULL,
 username VARCHAR(50) NOT NULL UNIQUE,
 password_hash VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 rol ENUM('TAQUILLERO','ORGANIZADOR','VALIDADOR') NOT NULL,
 activo BOOLEAN NOT NULL DEFAULT TRUE,
 CONSTRAINT ck_usuario_nombre CHECK (CHAR_LENGTH(TRIM(nombre)) > 0),
 CONSTRAINT ck_usuario_activo CHECK (activo IN (0,1))
) ENGINE=InnoDB;

CREATE TABLE Evento (
 id_evento INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 nombre VARCHAR(120) NOT NULL,
 fecha_hora DATETIME NOT NULL COMMENT 'Fecha y hora UTC',
 lugar VARCHAR(150) NOT NULL,
 activo BOOLEAN NOT NULL DEFAULT TRUE,
 INDEX ix_evento_fecha (fecha_hora),
 CONSTRAINT ck_evento_nombre CHECK (CHAR_LENGTH(TRIM(nombre)) > 0),
 CONSTRAINT ck_evento_lugar CHECK (CHAR_LENGTH(TRIM(lugar)) > 0),
 CONSTRAINT ck_evento_activo CHECK (activo IN (0,1))
) ENGINE=InnoDB;

CREATE TABLE ZonaLugar (
 id_zona INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 id_evento INT UNSIGNED NOT NULL,
 nombre VARCHAR(80) NOT NULL,
 precio DECIMAL(10,2) NOT NULL COMMENT 'Precio actual en GTQ',
 UNIQUE KEY uq_zona_evento_nombre (id_evento,nombre),
 CONSTRAINT fk_zona_evento FOREIGN KEY (id_evento) REFERENCES Evento(id_evento)
  ON DELETE RESTRICT ON UPDATE CASCADE,
 CONSTRAINT ck_zona_nombre CHECK (CHAR_LENGTH(TRIM(nombre)) > 0),
 CONSTRAINT ck_zona_precio CHECK (precio > 0)
) ENGINE=InnoDB;

CREATE TABLE Asiento (
 id_asiento INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 id_zona INT UNSIGNED NOT NULL,
 fila VARCHAR(10) NOT NULL,
 numero SMALLINT UNSIGNED NOT NULL,
 token_reserva CHAR(32) CHARACTER SET ascii COLLATE ascii_bin NULL,
 reserva_hasta DATETIME(6) NULL COMMENT 'UTC; caduca después de 3 minutos',
 UNIQUE KEY uq_asiento_zona_fila_numero (id_zona,fila,numero),
 UNIQUE KEY uq_asiento_token (token_reserva),
 CONSTRAINT fk_asiento_zona FOREIGN KEY (id_zona) REFERENCES ZonaLugar(id_zona)
  ON DELETE RESTRICT ON UPDATE CASCADE,
 CONSTRAINT ck_asiento_fila CHECK (CHAR_LENGTH(TRIM(fila)) > 0),
 CONSTRAINT ck_asiento_numero CHECK (numero > 0),
 CONSTRAINT ck_reserva_completa CHECK (
  (token_reserva IS NULL AND reserva_hasta IS NULL) OR
  (token_reserva IS NOT NULL AND reserva_hasta IS NOT NULL))
) ENGINE=InnoDB;

CREATE TABLE Cliente (
 id_cliente INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 nombre VARCHAR(100) NOT NULL,
 correo VARCHAR(150) NOT NULL UNIQUE,
 telefono VARCHAR(25) NULL,
 CONSTRAINT ck_cliente_nombre CHECK (CHAR_LENGTH(TRIM(nombre)) > 0),
 CONSTRAINT ck_cliente_correo CHECK (CHAR_LENGTH(TRIM(correo)) > 3)
) ENGINE=InnoDB;

CREATE TABLE VentaTransaccion (
 id_venta INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 id_cliente INT UNSIGNED NOT NULL,
 id_taquillero INT UNSIGNED NOT NULL,
 fecha_creacion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 estado ENUM('PENDIENTE','PAGADA','ANULADA') NOT NULL DEFAULT 'PENDIENTE',
 referencia_pago VARCHAR(100) NULL UNIQUE,
 fecha_pago DATETIME(6) NULL,
 INDEX ix_venta_estado_fecha (estado,fecha_creacion),
 CONSTRAINT fk_venta_cliente FOREIGN KEY (id_cliente) REFERENCES Cliente(id_cliente)
  ON DELETE RESTRICT ON UPDATE CASCADE,
 CONSTRAINT fk_venta_usuario FOREIGN KEY (id_taquillero) REFERENCES Usuario(id_usuario)
  ON DELETE RESTRICT ON UPDATE CASCADE,
 CONSTRAINT ck_pago CHECK (
  (estado='PENDIENTE' AND referencia_pago IS NULL AND fecha_pago IS NULL) OR
  (estado IN ('PAGADA','ANULADA') AND referencia_pago IS NOT NULL AND fecha_pago IS NOT NULL)),
 CONSTRAINT ck_referencia CHECK (referencia_pago IS NULL OR CHAR_LENGTH(TRIM(referencia_pago)) > 0)
) ENGINE=InnoDB;

CREATE TABLE Boleto (
 id_boleto INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 id_venta INT UNSIGNED NOT NULL UNIQUE COMMENT 'Alcance: un boleto por transacción',
 id_asiento INT UNSIGNED NOT NULL,
 codigo CHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
 precio_pagado DECIMAL(10,2) NOT NULL COMMENT 'Importe histórico en GTQ',
 estado ENUM('EMITIDO','UTILIZADO','ANULADO') NOT NULL DEFAULT 'EMITIDO',
 fecha_emision DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 fecha_validacion DATETIME(6) NULL,
 id_validador INT UNSIGNED NULL,
 -- NULL no colisiona en un índice UNIQUE: permite reventa tras anulación.
 asiento_vigente INT UNSIGNED GENERATED ALWAYS AS
  (CASE WHEN estado <> 'ANULADO' THEN id_asiento ELSE NULL END) STORED,
 UNIQUE KEY uq_boleto_asiento_vigente (asiento_vigente),
 INDEX ix_boleto_asiento (id_asiento),
 CONSTRAINT fk_boleto_venta FOREIGN KEY (id_venta) REFERENCES VentaTransaccion(id_venta)
  ON DELETE RESTRICT ON UPDATE CASCADE,
 CONSTRAINT fk_boleto_asiento FOREIGN KEY (id_asiento) REFERENCES Asiento(id_asiento)
  ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_boleto_validador FOREIGN KEY (id_validador) REFERENCES Usuario(id_usuario)
  ON DELETE RESTRICT ON UPDATE CASCADE,
 CONSTRAINT ck_boleto_precio CHECK (precio_pagado > 0),
 CONSTRAINT ck_boleto_validacion CHECK (
  (estado='UTILIZADO' AND fecha_validacion IS NOT NULL AND id_validador IS NOT NULL) OR
  (estado IN ('EMITIDO','ANULADO') AND fecha_validacion IS NULL AND id_validador IS NULL))
) ENGINE=InnoDB;
