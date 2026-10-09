DROP DATABASE IF EXISTS ticketmastery_fase1;

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
 fecha_hora DATETIME NOT NULL,
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
 precio DECIMAL(10,2) NOT NULL,
 UNIQUE KEY uq_zona_evento_nombre (id_evento,nombre),
 CONSTRAINT fk_zona_evento FOREIGN KEY (id_evento) REFERENCES Evento(id_evento)
  ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT ck_zona_nombre CHECK (CHAR_LENGTH(TRIM(nombre)) > 0),
 CONSTRAINT ck_zona_precio CHECK (precio > 0)
) ENGINE=InnoDB;

CREATE TABLE Asiento (
 id_asiento INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 id_zona INT UNSIGNED NOT NULL,
 fila VARCHAR(10) NOT NULL,
 numero SMALLINT UNSIGNED NOT NULL,
 token_reserva CHAR(32) CHARACTER SET ascii COLLATE ascii_bin NULL,
 reserva_hasta DATETIME(6) NULL,
 UNIQUE KEY uq_asiento_zona_fila_numero (id_zona,fila,numero),
 UNIQUE KEY uq_asiento_token (token_reserva),
 CONSTRAINT fk_asiento_zona FOREIGN KEY (id_zona) REFERENCES ZonaLugar(id_zona)
  ON DELETE RESTRICT ON UPDATE RESTRICT,
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
  ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_venta_usuario FOREIGN KEY (id_taquillero) REFERENCES Usuario(id_usuario)
  ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT ck_pago CHECK (
  (estado='PENDIENTE' AND referencia_pago IS NULL AND fecha_pago IS NULL) OR
  (estado IN ('PAGADA','ANULADA') AND referencia_pago IS NOT NULL AND fecha_pago IS NOT NULL)),
 CONSTRAINT ck_referencia CHECK (referencia_pago IS NULL OR CHAR_LENGTH(TRIM(referencia_pago)) > 0)
) ENGINE=InnoDB;

CREATE TABLE Boleto (
 id_boleto INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 id_venta INT UNSIGNED NOT NULL UNIQUE,
 id_asiento INT UNSIGNED NOT NULL,
 codigo CHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
 precio_pagado DECIMAL(10,2) NOT NULL,
 estado ENUM('EMITIDO','UTILIZADO','ANULADO') NOT NULL DEFAULT 'EMITIDO',
 fecha_emision DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 fecha_validacion DATETIME(6) NULL,
 id_validador INT UNSIGNED NULL,
 asiento_vigente INT UNSIGNED GENERATED ALWAYS AS
  (CASE WHEN estado <> 'ANULADO' THEN id_asiento ELSE NULL END) STORED,
 UNIQUE KEY uq_boleto_asiento_vigente (asiento_vigente),
 INDEX ix_boleto_asiento (id_asiento),
 CONSTRAINT fk_boleto_venta FOREIGN KEY (id_venta) REFERENCES VentaTransaccion(id_venta)
  ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_boleto_asiento FOREIGN KEY (id_asiento) REFERENCES Asiento(id_asiento)
  ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_boleto_validador FOREIGN KEY (id_validador) REFERENCES Usuario(id_usuario),
 CONSTRAINT ck_boleto_precio CHECK (precio_pagado > 0),
 CONSTRAINT ck_boleto_validacion CHECK (
  (estado='UTILIZADO' AND fecha_validacion IS NOT NULL AND id_validador IS NOT NULL) OR
  (estado IN ('EMITIDO','ANULADO') AND fecha_validacion IS NULL AND id_validador IS NULL))
) ENGINE=InnoDB;

USE ticketmastery_fase1;
CREATE VIEW vw_disponibilidad AS
SELECT e.id_evento,e.nombre AS evento,e.fecha_hora,e.activo,
 z.id_zona,z.nombre AS zona,z.precio,a.id_asiento,a.fila,a.numero,
 CASE WHEN e.activo=0 OR e.fecha_hora<=UTC_TIMESTAMP() THEN 'NO_DISPONIBLE'
 WHEN b.id_boleto IS NOT NULL THEN 'VENDIDO'
 WHEN a.reserva_hasta>UTC_TIMESTAMP(6) THEN 'RESERVADO'
 ELSE 'DISPONIBLE' END AS disponibilidad
FROM Evento e JOIN ZonaLugar z ON z.id_evento=e.id_evento
JOIN Asiento a ON a.id_zona=z.id_zona
LEFT JOIN Boleto b ON b.asiento_vigente=a.id_asiento;

CREATE VIEW vw_ventas AS
SELECT v.id_venta,v.fecha_creacion,v.estado,v.referencia_pago,v.fecha_pago,
 c.id_cliente,c.nombre AS cliente,u.nombre AS taquillero,
 b.id_boleto,b.codigo,b.estado AS estado_boleto,b.precio_pagado AS total_gtq,
 e.nombre AS evento,z.nombre AS zona,a.fila,a.numero
FROM VentaTransaccion v JOIN Cliente c ON c.id_cliente=v.id_cliente
JOIN Usuario u ON u.id_usuario=v.id_taquillero
LEFT JOIN Boleto b ON b.id_venta=v.id_venta
LEFT JOIN Asiento a ON a.id_asiento=b.id_asiento
LEFT JOIN ZonaLugar z ON z.id_zona=a.id_zona
LEFT JOIN Evento e ON e.id_evento=z.id_evento;

USE ticketmastery_fase1;
SET NAMES utf8mb4;
SET time_zone = '+00:00';
DELIMITER $$
CREATE PROCEDURE sp_insertar_evento(IN p_nombre VARCHAR(120), IN p_fecha_hora DATETIME, IN p_lugar VARCHAR(150), IN p_activo BOOLEAN)
BEGIN
 INSERT INTO Evento (nombre, fecha_hora, lugar, activo) VALUES (p_nombre, p_fecha_hora, p_lugar, p_activo);
 SELECT LAST_INSERT_ID() AS id_evento;
END$$

CREATE PROCEDURE sp_actualizar_evento(IN p_id INT UNSIGNED, IN p_nombre VARCHAR(120), IN p_fecha_hora DATETIME, IN p_lugar VARCHAR(150), IN p_activo BOOLEAN)
BEGIN
 IF NOT EXISTS (SELECT 1 FROM Evento WHERE id_evento=p_id) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Registro inexistente'; END IF;
 UPDATE Evento SET nombre=p_nombre, fecha_hora=p_fecha_hora, lugar=p_lugar, activo=p_activo WHERE id_evento=p_id;
END$$

CREATE PROCEDURE sp_eliminar_evento(IN p_id INT UNSIGNED)
BEGIN
 DELETE FROM Evento WHERE id_evento=p_id;
 IF ROW_COUNT()=0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Registro inexistente'; END IF;
END$$

CREATE PROCEDURE sp_insertar_zonalugar(IN p_id_evento INT UNSIGNED, IN p_nombre VARCHAR(80), IN p_precio DECIMAL(10,2))
BEGIN
 INSERT INTO ZonaLugar (id_evento, nombre, precio) VALUES (p_id_evento, p_nombre, p_precio);
 SELECT LAST_INSERT_ID() AS id_zona;
END$$

CREATE PROCEDURE sp_actualizar_zonalugar(IN p_id INT UNSIGNED, IN p_nombre VARCHAR(80), IN p_precio DECIMAL(10,2))
BEGIN
 IF NOT EXISTS (SELECT 1 FROM ZonaLugar WHERE id_zona=p_id) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Registro inexistente'; END IF;
 UPDATE ZonaLugar SET nombre=p_nombre, precio=p_precio WHERE id_zona=p_id;
END$$

CREATE PROCEDURE sp_eliminar_zonalugar(IN p_id INT UNSIGNED)
BEGIN
 DELETE FROM ZonaLugar WHERE id_zona=p_id;
 IF ROW_COUNT()=0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Registro inexistente'; END IF;
END$$

CREATE PROCEDURE sp_insertar_asiento(IN p_id_zona INT UNSIGNED, IN p_fila VARCHAR(10), IN p_numero SMALLINT UNSIGNED)
BEGIN
 INSERT INTO Asiento (id_zona, fila, numero) VALUES (p_id_zona, p_fila, p_numero);
 SELECT LAST_INSERT_ID() AS id_asiento;
END$$

CREATE PROCEDURE sp_actualizar_asiento(IN p_id INT UNSIGNED, IN p_fila VARCHAR(10), IN p_numero SMALLINT UNSIGNED)
BEGIN
 DECLARE v_id INT UNSIGNED;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 START TRANSACTION;
 SELECT id_asiento INTO v_id FROM Asiento WHERE id_asiento=p_id FOR UPDATE;
 IF v_id IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Asiento inexistente'; END IF; IF EXISTS (SELECT 1 FROM Boleto WHERE id_asiento=p_id) OR EXISTS (SELECT 1 FROM Asiento WHERE id_asiento=p_id AND reserva_hasta>UTC_TIMESTAMP(6)) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='No se puede renumerar un asiento reservado o con historial'; END IF;
 UPDATE Asiento SET fila=p_fila, numero=p_numero WHERE id_asiento=p_id;
 COMMIT;
END$$

CREATE PROCEDURE sp_eliminar_asiento(IN p_id INT UNSIGNED)
BEGIN
 DECLARE v_id INT UNSIGNED;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 START TRANSACTION;
 SELECT id_asiento INTO v_id FROM Asiento WHERE id_asiento=p_id FOR UPDATE;
 IF v_id IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Asiento inexistente'; END IF; IF EXISTS (SELECT 1 FROM Asiento WHERE id_asiento=p_id AND reserva_hasta>UTC_TIMESTAMP(6)) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Asiento con reserva activa'; END IF;
 DELETE FROM Asiento WHERE id_asiento=p_id;
 IF ROW_COUNT()=0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Registro inexistente'; END IF;
 COMMIT;
END$$

CREATE PROCEDURE sp_insertar_cliente(IN p_nombre VARCHAR(100), IN p_correo VARCHAR(150), IN p_telefono VARCHAR(25))
BEGIN
 INSERT INTO Cliente (nombre, correo, telefono) VALUES (p_nombre, p_correo, p_telefono);
 SELECT LAST_INSERT_ID() AS id_cliente;
END$$

CREATE PROCEDURE sp_actualizar_cliente(IN p_id INT UNSIGNED, IN p_nombre VARCHAR(100), IN p_correo VARCHAR(150), IN p_telefono VARCHAR(25))
BEGIN
 IF NOT EXISTS (SELECT 1 FROM Cliente WHERE id_cliente=p_id) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Registro inexistente'; END IF;
 UPDATE Cliente SET nombre=p_nombre, correo=p_correo, telefono=p_telefono WHERE id_cliente=p_id;
END$$

CREATE PROCEDURE sp_eliminar_cliente(IN p_id INT UNSIGNED)
BEGIN
 DELETE FROM Cliente WHERE id_cliente=p_id;
 IF ROW_COUNT()=0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Registro inexistente'; END IF;
END$$

CREATE PROCEDURE sp_insertar_usuario(IN p_nombre VARCHAR(100), IN p_username VARCHAR(50), IN p_password_hash VARCHAR(255), IN p_rol VARCHAR(20), IN p_activo BOOLEAN)
BEGIN
 INSERT INTO Usuario (nombre, username, password_hash, rol, activo) VALUES (p_nombre, p_username, p_password_hash, p_rol, p_activo);
 SELECT LAST_INSERT_ID() AS id_usuario;
END$$

CREATE PROCEDURE sp_actualizar_usuario(IN p_id INT UNSIGNED, IN p_nombre VARCHAR(100), IN p_username VARCHAR(50), IN p_password_hash VARCHAR(255), IN p_rol VARCHAR(20), IN p_activo BOOLEAN)
BEGIN
 IF NOT EXISTS (SELECT 1 FROM Usuario WHERE id_usuario=p_id) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Registro inexistente'; END IF;
 UPDATE Usuario SET nombre=p_nombre, username=p_username, password_hash=p_password_hash, rol=p_rol, activo=p_activo WHERE id_usuario=p_id;
END$$

CREATE PROCEDURE sp_eliminar_usuario(IN p_id INT UNSIGNED)
BEGIN
 DELETE FROM Usuario WHERE id_usuario=p_id;
 IF ROW_COUNT()=0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Registro inexistente'; END IF;
END$$

CREATE PROCEDURE sp_insertar_ventatransaccion(IN p_cliente INT UNSIGNED, IN p_taquillero INT UNSIGNED)
BEGIN
 IF NOT EXISTS (SELECT 1 FROM Usuario WHERE id_usuario=p_taquillero AND activo=1 AND rol='TAQUILLERO') THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Usuario no es taquillero activo'; END IF;
 INSERT INTO VentaTransaccion(id_cliente,id_taquillero,fecha_creacion)
 VALUES(p_cliente,p_taquillero,UTC_TIMESTAMP(6));
 SELECT LAST_INSERT_ID() AS id_venta;
END$$

CREATE PROCEDURE sp_actualizar_ventatransaccion(IN p_id INT UNSIGNED, IN p_cliente INT UNSIGNED)
BEGIN
 DECLARE v_estado VARCHAR(12);
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 START TRANSACTION;

 SELECT estado INTO v_estado FROM VentaTransaccion WHERE id_venta=p_id FOR UPDATE;
 IF v_estado IS NULL OR v_estado<>'PENDIENTE' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Solo se puede editar una venta pendiente'; END IF;
 UPDATE VentaTransaccion SET id_cliente=p_cliente WHERE id_venta=p_id;

 COMMIT;
END$$

CREATE PROCEDURE sp_eliminar_ventatransaccion(IN p_id INT UNSIGNED)
BEGIN

 DELETE FROM VentaTransaccion WHERE id_venta=p_id AND estado='PENDIENTE';
 IF ROW_COUNT()=0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Venta inexistente o no pendiente; anular su boleto si corresponde'; END IF;
END$$

CREATE PROCEDURE sp_reservar_asiento(IN p_asiento INT UNSIGNED)
BEGIN
 DECLARE v_id INT UNSIGNED;
 DECLARE v_hasta DATETIME(6);
 DECLARE v_token CHAR(32);
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 START TRANSACTION;

 SELECT id_asiento,reserva_hasta INTO v_id,v_hasta FROM Asiento
 WHERE id_asiento=p_asiento FOR UPDATE;
 IF v_id IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Asiento inexistente'; END IF; IF v_hasta>UTC_TIMESTAMP(6) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Asiento reservado por otro checkout'; END IF; IF EXISTS (SELECT 1 FROM Boleto WHERE asiento_vigente=p_asiento) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Asiento ya vendido'; END IF; IF NOT EXISTS (SELECT 1 FROM Asiento a JOIN ZonaLugar z ON z.id_zona=a.id_zona
 JOIN Evento e ON e.id_evento=z.id_evento
 WHERE a.id_asiento=p_asiento AND e.activo=1 AND e.fecha_hora>UTC_TIMESTAMP()) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Evento no disponible'; END IF;
 SET v_token=HEX(RANDOM_BYTES(16));
 UPDATE Asiento SET token_reserva=v_token,reserva_hasta=UTC_TIMESTAMP(6)+INTERVAL 3 MINUTE
 WHERE id_asiento=p_asiento;
 SELECT id_asiento,token_reserva,reserva_hasta FROM Asiento WHERE id_asiento=p_asiento;

 COMMIT;
END$$

CREATE PROCEDURE sp_liberar_reserva(IN p_asiento INT UNSIGNED, IN p_token CHAR(32))
BEGIN

 UPDATE Asiento SET token_reserva=NULL,reserva_hasta=NULL
 WHERE id_asiento=p_asiento AND BINARY token_reserva=BINARY p_token;
 IF ROW_COUNT()=0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Reserva inexistente o token incorrecto'; END IF;
END$$

CREATE PROCEDURE sp_insertar_boleto(IN p_venta INT UNSIGNED, IN p_asiento INT UNSIGNED, IN p_token CHAR(32), IN p_referencia VARCHAR(100))
BEGIN
 DECLARE v_id INT UNSIGNED;
 DECLARE v_boleto INT UNSIGNED;
 DECLARE v_token CHAR(32);
 DECLARE v_codigo CHAR(32);
 DECLARE v_hasta DATETIME(6);
 DECLARE v_estado VARCHAR(12);
 DECLARE v_precio DECIMAL(10,2);
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 START TRANSACTION;
 SELECT id_asiento,token_reserva,reserva_hasta INTO v_id,v_token,v_hasta
 FROM Asiento WHERE id_asiento=p_asiento FOR UPDATE;
 IF v_id IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Asiento inexistente'; END IF; IF p_token IS NULL OR v_token IS NULL OR BINARY p_token<>BINARY v_token OR v_hasta<=UTC_TIMESTAMP(6) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Reserva inexistente, ajena o vencida'; END IF; IF EXISTS (SELECT 1 FROM Boleto WHERE asiento_vigente=p_asiento) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Asiento ya vendido'; END IF;
 SELECT estado INTO v_estado FROM VentaTransaccion WHERE id_venta=p_venta FOR UPDATE;
 IF v_estado IS NULL OR v_estado<>'PENDIENTE' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Venta inexistente o no pendiente'; END IF; IF NOT EXISTS (SELECT 1 FROM VentaTransaccion v JOIN Usuario u ON u.id_usuario=v.id_taquillero WHERE v.id_venta=p_venta AND u.activo=1 AND u.rol='TAQUILLERO') THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Taquillero no autorizado'; END IF; IF p_referencia IS NULL OR CHAR_LENGTH(TRIM(p_referencia))=0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='La referencia del pago confirmado es obligatoria'; END IF;
 SELECT z.precio INTO v_precio FROM Asiento a JOIN ZonaLugar z ON z.id_zona=a.id_zona
 JOIN Evento e ON e.id_evento=z.id_evento
 WHERE a.id_asiento=p_asiento AND e.activo=1 AND e.fecha_hora>UTC_TIMESTAMP();
 IF v_precio IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Evento no disponible'; END IF;
 SET v_codigo=HEX(RANDOM_BYTES(16));
 UPDATE VentaTransaccion SET estado='PAGADA',referencia_pago=p_referencia,fecha_pago=UTC_TIMESTAMP(6)
 WHERE id_venta=p_venta;
 INSERT INTO Boleto(id_venta,id_asiento,codigo,precio_pagado,fecha_emision)
 VALUES(p_venta,p_asiento,v_codigo,v_precio,UTC_TIMESTAMP(6));
 SET v_boleto=LAST_INSERT_ID();
 UPDATE Asiento SET token_reserva=NULL,reserva_hasta=NULL WHERE id_asiento=p_asiento;
 SELECT v_boleto AS id_boleto,v_codigo AS codigo_para_qr,v_precio AS precio_pagado;

 COMMIT;
END$$

CREATE PROCEDURE sp_actualizar_boleto(IN p_codigo CHAR(32), IN p_validador INT UNSIGNED)
BEGIN
 IF NOT EXISTS (SELECT 1 FROM Usuario WHERE id_usuario=p_validador AND rol='VALIDADOR' AND activo=1) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Usuario no es validador activo'; END IF;
 UPDATE Boleto SET estado='UTILIZADO',fecha_validacion=UTC_TIMESTAMP(6),id_validador=p_validador
 WHERE codigo=BINARY p_codigo AND estado='EMITIDO';
 IF ROW_COUNT()=0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Codigo inexistente, anulado o ya utilizado'; END IF;
 SELECT id_boleto,codigo,estado,fecha_validacion FROM Boleto WHERE codigo=BINARY p_codigo;
END$$

CREATE PROCEDURE sp_eliminar_boleto(IN p_id INT UNSIGNED)
BEGIN
 DECLARE v_asiento INT UNSIGNED;
 DECLARE v_venta INT UNSIGNED;
 DECLARE v_lock INT UNSIGNED;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 START TRANSACTION;
 SELECT id_asiento,id_venta INTO v_asiento,v_venta FROM Boleto WHERE id_boleto=p_id;
 IF v_asiento IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Boleto inexistente'; END IF;
 SELECT id_asiento INTO v_lock FROM Asiento WHERE id_asiento=v_asiento FOR UPDATE;
 SELECT id_venta INTO v_lock FROM VentaTransaccion WHERE id_venta=v_venta FOR UPDATE;
 UPDATE Boleto SET estado='ANULADO' WHERE id_boleto=p_id AND estado='EMITIDO';
 IF ROW_COUNT()=0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Solo se puede anular un boleto emitido no utilizado'; END IF;
 UPDATE VentaTransaccion SET estado='ANULADA' WHERE id_venta=v_venta;

 COMMIT;
END$$

CREATE PROCEDURE sp_listar_evento()
BEGIN
 SELECT * FROM Evento ORDER BY id_evento;
END$$

CREATE PROCEDURE sp_buscar_evento(IN p_id INT UNSIGNED)
BEGIN
 SELECT * FROM Evento WHERE id_evento=p_id;
END$$

CREATE PROCEDURE sp_listar_zonalugar()
BEGIN
 SELECT * FROM ZonaLugar ORDER BY id_zona;
END$$

CREATE PROCEDURE sp_buscar_zonalugar(IN p_id INT UNSIGNED)
BEGIN
 SELECT * FROM ZonaLugar WHERE id_zona=p_id;
END$$

CREATE PROCEDURE sp_listar_asiento()
BEGIN
 SELECT * FROM Asiento ORDER BY id_asiento;
END$$

CREATE PROCEDURE sp_buscar_asiento(IN p_id INT UNSIGNED)
BEGIN
 SELECT * FROM Asiento WHERE id_asiento=p_id;
END$$

CREATE PROCEDURE sp_listar_cliente()
BEGIN
 SELECT * FROM Cliente ORDER BY id_cliente;
END$$

CREATE PROCEDURE sp_buscar_cliente(IN p_id INT UNSIGNED)
BEGIN
 SELECT * FROM Cliente WHERE id_cliente=p_id;
END$$

CREATE PROCEDURE sp_listar_usuario()
BEGIN
 SELECT id_usuario,nombre,username,rol,activo FROM Usuario ORDER BY id_usuario;
END$$

CREATE PROCEDURE sp_buscar_usuario(IN p_id INT UNSIGNED)
BEGIN
 SELECT id_usuario,nombre,username,rol,activo FROM Usuario WHERE id_usuario=p_id;
END$$

CREATE PROCEDURE sp_listar_ventatransaccion()
BEGIN
 SELECT * FROM VentaTransaccion ORDER BY id_venta;
END$$

CREATE PROCEDURE sp_buscar_ventatransaccion(IN p_id INT UNSIGNED)
BEGIN
 SELECT * FROM VentaTransaccion WHERE id_venta=p_id;
END$$

CREATE PROCEDURE sp_listar_boleto()
BEGIN
 SELECT * FROM Boleto ORDER BY id_boleto;
END$$

CREATE PROCEDURE sp_buscar_boleto(IN p_id INT UNSIGNED)
BEGIN
 SELECT * FROM Boleto WHERE id_boleto=p_id;
END$$

CREATE PROCEDURE sp_buscar_usuario_login(IN p_username VARCHAR(50))
BEGIN
 SELECT id_usuario,nombre,username,password_hash,rol FROM Usuario
 WHERE username=p_username AND activo=1;
END$$

CREATE PROCEDURE sp_consultar_disponibilidad(IN p_evento INT UNSIGNED)
BEGIN
 SELECT * FROM vw_disponibilidad WHERE id_evento=p_evento ORDER BY id_zona,fila,numero;
END$$

CREATE PROCEDURE sp_consultar_ventas()
BEGIN
 SELECT * FROM vw_ventas ORDER BY id_venta;
END$$

DELIMITER ;
