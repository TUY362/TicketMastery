# TicketMastery

Proyecto 9: taquilla para eventos y conciertos. Desarrollado con Java, JavaFX, MySQL y arquitectura MVC con DAO.

## Requisitos

- JDK 21.
- Apache NetBeans con soporte para proyectos Java con Ant.
- MySQL 8.0.16 o superior.
- MySQL Workbench para ejecutar los scripts.
- JavaFX SDK 21 para el sistema operativo utilizado.
- MySQL Connector/J 8.
- ZXing `core-3.5.3.jar`, ubicado en `lib`.

## Preparar la base de datos

Ejecutar los archivos completos en este orden:

1. `sql/DDL_ticket_in4cm.sql`
2. `sql/DML_ticket_in4cm.sql`

El DDL elimina y reconstruye `ticket_in4cm`. Al ejecutarlo se pierden los datos anteriores de esa base. Respaldar los datos que se necesiten conservar.

La cuenta utilizada para ejecutar los scripts debe tener los permisos necesarios para crear la base, las tablas, las vistas y los procedimientos. La cuenta de la aplicación necesita permisos para ejecutar los procedimientos y acceder a los objetos según la configuración de MySQL.

El conjunto inicial contiene siete tablas: Usuario, Evento, ZonaLugar, Asiento, Cliente, VentaTransaccion y Boleto. Incluye cinco registros por tabla, excepto Asiento, que contiene diez; también incluye cuarenta procedimientos y dos vistas.

## Configurar la conexión

Crear o editar `src/db.properties`, ubicado en el paquete sin nombre de NetBeans:

```properties
db.url=jdbc:mysql://localhost:3306/ticket_in4cm
db.user=IN4CM
db.password=TU_CONTRASENA_LOCAL
```

Reemplazar los valores por las credenciales autorizadas de la instalación local. No publicar contraseñas reales en el repositorio.

El archivo debe estar disponible como `/db.properties` en el classpath. Si se modifica después de compilar, volver a construir el proyecto.

## Abrir y compilar en NetBeans

1. Abrir el proyecto TicketMastery mediante File > Open Project.
2. Seleccionar JDK 21 como plataforma Java.
3. En Properties > Libraries, configurar las bibliotecas JavaFX y MySQL Connector/J utilizadas por el proyecto.
4. Agregar `lib/core-3.5.3.jar` al Classpath con una referencia relativa.
5. En Properties > Run, establecer `org.kt.main.Main` como Main Class.
6. Configurar VM Options con la ruta real al directorio `lib` del SDK JavaFX:

```text
--module-path "C:\ruta\javafx-sdk-21\lib" --add-modules javafx.controls,javafx.fxml
```

7. Ejecutar Clean and Build. Revisar la salida y resolver los errores o advertencias antes de la entrega.
8. Ejecutar Run para abrir el login.

Las bibliotecas de NetBeans y la ruta del SDK JavaFX se deben configurar en cada equipo; una ruta de otra computadora puede no existir localmente.

## Usuarios de demostración

Estas cuentas pertenecen a la aplicación y se crean al ejecutar el DML. Son distintas de la cuenta de conexión a MySQL.

| Usuario | Contraseña | Rol |
| --- | --- | --- |
| taquilla1 | DemoKinal2026! | Taquillero |
| taquilla2 | DemoKinal2026! | Taquillero |
| organizador1 | DemoKinal2026! | Organizador |
| validador1 | DemoKinal2026! | Validador |
| validador2 | DemoKinal2026! | Validador |

Las contraseñas de aplicación se almacenan mediante PBKDF2 con HMAC-SHA256.

## Funciones por rol

- Organizador: administrar eventos, zonas, precios y asientos.
- Taquillero: administrar clientes, reservar asientos y emitir boletos al registrar la confirmación del pago.
- Validador: registrar el ingreso de un boleto y rechazar su reutilización.

La confirmación del pago es manual. No existe integración con un banco ni una pasarela de pagos.

## Flujo de venta

1. Iniciar sesión como taquillero.
2. Elegir cliente, evento, zona y asiento disponible.
3. Reservar el asiento durante tres minutos.
4. Ingresar una referencia de pago única y confirmar que el pago fue recibido.
5. Emitir el boleto, que contiene un código alfanumérico único y su imagen QR.
6. Iniciar sesión como validador y pegar el código o usar un lector que lo introduzca como teclado.
7. Validar el boleto. Un segundo intento con el mismo código debe rechazarse.

La aplicación no incluye captura de QR mediante cámara. Las fechas de eventos y vencimientos se manejan en UTC. La base verifica la vigencia de las reservas y evita boletos vigentes duplicados para un mismo asiento.

## Estructura

| Ubicación | Contenido |
| --- | --- |
| src/org/kt/main | Inicio de JavaFX |
| src/org/kt/model | Entidades |
| src/org/kt/dao | Interfaces de persistencia |
| src/org/kt/dao/impl | Llamadas a procedimientos almacenados |
| src/org/kt/controller | Controladores de las pantallas |
| src/org/kt/view | Archivos FXML |
| src/org/kt/util | Conexión, sesión, permisos y utilidades |
| src/org/kt/exceptions | Excepciones de la aplicación |
| sql | DDL y DML |
| lib | Dependencias locales incluidas en el proyecto |

## Ejecutar el JAR

Clean and Build genera `dist/TicketMastery.jar`. Conservar también las dependencias generadas en `dist/lib` y tener MySQL disponible.

Desde la raíz del proyecto, ajustar la ruta JavaFX y ejecutar:

```text
java --module-path "C:\ruta\javafx-sdk-21\lib" --add-modules javafx.controls,javafx.fxml -jar dist/TicketMastery.jar
```

Verificar este comando en el equipo de entrega antes de publicar la versión final. El JAR puede contener la copia de `db.properties` utilizada al compilar; revisar sus credenciales antes de compartirlo.

## Pruebas de entrega

- Compilación completa del proyecto TicketMastery.
- Login correcto e incorrecto, permisos por rol y cierre de sesión.
- Alta, modificación y eliminación de registros nuevos en los módulos CRUD.
- Protección de registros con relaciones existentes.
- Reserva, cancelación y vencimiento de asientos.
- Emisión de un boleto y lectura del mismo código desde su QR.
- Validación de una entrada y rechazo del segundo ingreso.
- Ejecución del JAR fuera de NetBeans.

Las pruebas deben realizarse sobre la instalación de entrega. Esta lista no constituye evidencia de que hayan sido ejecutadas.

Link del video:https://youtu.be/Jdsltr5_hr8
