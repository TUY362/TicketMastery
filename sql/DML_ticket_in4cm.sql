USE ticket_in4cm;
SET NAMES utf8mb4;
SET time_zone = '+00:00';
START TRANSACTION;
INSERT INTO Usuario(id_usuario,nombre,username,password_hash,rol) VALUES(1,'Ana Taquilla','taquilla1','pbkdf2_sha256$600000$1JWMGFGiJp1O4OzqT5l5eg==$OTJjKAqHQkOX2hY2m+zyDv2mCS5Ul4RZFOo8nkl79h4=','TAQUILLERO');
INSERT INTO Usuario(id_usuario,nombre,username,password_hash,rol) VALUES(2,'Luis Taquilla','taquilla2','pbkdf2_sha256$600000$eoSnNDS+zutE72wYFbyMvw==$L6mEq++HQVGvRxaVDALF96T2LdiuEPiUGccQOT+mGDk=','TAQUILLERO');
INSERT INTO Usuario(id_usuario,nombre,username,password_hash,rol) VALUES(3,'Marta Organiza','organizador1','pbkdf2_sha256$600000$POEpntwFIHgEVG3xlwW6AA==$2jm5RJpXWjL8yDErnqK7ebMQ0OrY8PklZVP5xI9uy/E=','ORGANIZADOR');
INSERT INTO Usuario(id_usuario,nombre,username,password_hash,rol) VALUES(4,'Carlos Entrada','validador1','pbkdf2_sha256$600000$ldbLgDzxfv0xH+zP5sp5pw==$lBgY6U3/tZjfLuEsW1QCGBLr1L2nGYF7P0Ngh06iZIo=','VALIDADOR');
INSERT INTO Usuario(id_usuario,nombre,username,password_hash,rol) VALUES(5,'Sofia Entrada','validador2','pbkdf2_sha256$600000$Jh3+Fpnpu5OvRaiaMmwtQQ==$+Y2hBCRoyFP1B13YkWfhNZZrb+EA232BLMJeXmDvka0=','VALIDADOR');
INSERT INTO Evento(id_evento,nombre,fecha_hora,lugar) VALUES(1,'Rock de Primavera',UTC_TIMESTAMP()+INTERVAL 31 DAY,'Auditorio Demo 1');
INSERT INTO ZonaLugar(id_zona,id_evento,nombre,precio) VALUES(1,1,'Preferencial',125.00);
INSERT INTO Asiento(id_asiento,id_zona,fila,numero) VALUES(1,1,'A',1);
INSERT INTO Asiento(id_asiento,id_zona,fila,numero) VALUES(2,1,'A',2);
INSERT INTO Cliente(id_cliente,nombre,correo,telefono) VALUES(1,'Cliente Demo 1','cliente1@example.com','5550-0001');
INSERT INTO VentaTransaccion(id_venta,id_cliente,id_taquillero,fecha_creacion,estado,referencia_pago,fecha_pago) VALUES(1,1,1,UTC_TIMESTAMP(6),'PAGADA','DEMO-PAGO-001',UTC_TIMESTAMP(6));
INSERT INTO Boleto(id_boleto,id_venta,id_asiento,codigo,precio_pagado,fecha_emision) VALUES(1,1,1,'00000000000000000000000000000001',125.00,UTC_TIMESTAMP(6));
INSERT INTO Evento(id_evento,nombre,fecha_hora,lugar) VALUES(2,'Noche de Jazz',UTC_TIMESTAMP()+INTERVAL 32 DAY,'Auditorio Demo 2');
INSERT INTO ZonaLugar(id_zona,id_evento,nombre,precio) VALUES(2,2,'Preferencial',150.00);
INSERT INTO Asiento(id_asiento,id_zona,fila,numero) VALUES(3,2,'A',1);
INSERT INTO Asiento(id_asiento,id_zona,fila,numero) VALUES(4,2,'A',2);
INSERT INTO Cliente(id_cliente,nombre,correo,telefono) VALUES(2,'Cliente Demo 2','cliente2@example.com','5550-0002');
INSERT INTO VentaTransaccion(id_venta,id_cliente,id_taquillero,fecha_creacion,estado,referencia_pago,fecha_pago) VALUES(2,2,2,UTC_TIMESTAMP(6),'PAGADA','DEMO-PAGO-002',UTC_TIMESTAMP(6));
INSERT INTO Boleto(id_boleto,id_venta,id_asiento,codigo,precio_pagado,fecha_emision) VALUES(2,2,3,'00000000000000000000000000000002',150.00,UTC_TIMESTAMP(6));
INSERT INTO Evento(id_evento,nombre,fecha_hora,lugar) VALUES(3,'Festival Pop',UTC_TIMESTAMP()+INTERVAL 33 DAY,'Auditorio Demo 3');
INSERT INTO ZonaLugar(id_zona,id_evento,nombre,precio) VALUES(3,3,'Preferencial',175.00);
INSERT INTO Asiento(id_asiento,id_zona,fila,numero) VALUES(5,3,'A',1);
INSERT INTO Asiento(id_asiento,id_zona,fila,numero) VALUES(6,3,'A',2);
INSERT INTO Cliente(id_cliente,nombre,correo,telefono) VALUES(3,'Cliente Demo 3','cliente3@example.com','5550-0003');
INSERT INTO VentaTransaccion(id_venta,id_cliente,id_taquillero,fecha_creacion,estado,referencia_pago,fecha_pago) VALUES(3,3,1,UTC_TIMESTAMP(6),'PAGADA','DEMO-PAGO-003',UTC_TIMESTAMP(6));
INSERT INTO Boleto(id_boleto,id_venta,id_asiento,codigo,precio_pagado,fecha_emision) VALUES(3,3,5,'00000000000000000000000000000003',175.00,UTC_TIMESTAMP(6));
INSERT INTO Evento(id_evento,nombre,fecha_hora,lugar) VALUES(4,'Concierto Sinfonico',UTC_TIMESTAMP()+INTERVAL 34 DAY,'Auditorio Demo 4');
INSERT INTO ZonaLugar(id_zona,id_evento,nombre,precio) VALUES(4,4,'Preferencial',200.00);
INSERT INTO Asiento(id_asiento,id_zona,fila,numero) VALUES(7,4,'A',1);
INSERT INTO Asiento(id_asiento,id_zona,fila,numero) VALUES(8,4,'A',2);
INSERT INTO Cliente(id_cliente,nombre,correo,telefono) VALUES(4,'Cliente Demo 4','cliente4@example.com','5550-0004');
INSERT INTO VentaTransaccion(id_venta,id_cliente,id_taquillero,fecha_creacion,estado,referencia_pago,fecha_pago) VALUES(4,4,2,UTC_TIMESTAMP(6),'PAGADA','DEMO-PAGO-004',UTC_TIMESTAMP(6));
INSERT INTO Boleto(id_boleto,id_venta,id_asiento,codigo,precio_pagado,fecha_emision) VALUES(4,4,7,'00000000000000000000000000000004',200.00,UTC_TIMESTAMP(6));
INSERT INTO Evento(id_evento,nombre,fecha_hora,lugar) VALUES(5,'Musica Latina',UTC_TIMESTAMP()+INTERVAL 35 DAY,'Auditorio Demo 5');
INSERT INTO ZonaLugar(id_zona,id_evento,nombre,precio) VALUES(5,5,'Preferencial',225.00);
INSERT INTO Asiento(id_asiento,id_zona,fila,numero) VALUES(9,5,'A',1);
INSERT INTO Asiento(id_asiento,id_zona,fila,numero) VALUES(10,5,'A',2);
INSERT INTO Cliente(id_cliente,nombre,correo,telefono) VALUES(5,'Cliente Demo 5','cliente5@example.com','5550-0005');
INSERT INTO VentaTransaccion(id_venta,id_cliente,id_taquillero,fecha_creacion,estado,referencia_pago,fecha_pago) VALUES(5,5,1,UTC_TIMESTAMP(6),'PAGADA','DEMO-PAGO-005',UTC_TIMESTAMP(6));
INSERT INTO Boleto(id_boleto,id_venta,id_asiento,codigo,precio_pagado,fecha_emision) VALUES(5,5,9,'00000000000000000000000000000005',225.00,UTC_TIMESTAMP(6));
COMMIT;

SELECT 'Evento' AS tabla, COUNT(*) AS registros FROM Evento
UNION ALL
SELECT 'ZonaLugar' AS tabla, COUNT(*) AS registros FROM ZonaLugar
UNION ALL
SELECT 'Asiento' AS tabla, COUNT(*) AS registros FROM Asiento
UNION ALL
SELECT 'Cliente' AS tabla, COUNT(*) AS registros FROM Cliente
UNION ALL
SELECT 'Usuario' AS tabla, COUNT(*) AS registros FROM Usuario
UNION ALL
SELECT 'VentaTransaccion' AS tabla, COUNT(*) AS registros FROM VentaTransaccion
UNION ALL
SELECT 'Boleto' AS tabla, COUNT(*) AS registros FROM Boleto;

CALL sp_listar_boleto();
