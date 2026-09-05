# SoporteNet — Sistema de Soporte Técnico para ISP

Sistema de gestión de tickets de soporte técnico para un proveedor de servicios de
Internet. Cubre el ciclo completo: el cliente reporta un problema, un administrador
lo asigna a un técnico (o grupo técnico), el técnico reporta la solución, el
administrador la aprueba, y el cliente confirma que el problema quedó resuelto.

## Stack técnico

- **Backend**: Spring Boot 3.3.4, Java 17, Spring Security (JWT), Spring Data JPA
- **Base de datos**: PostgreSQL — toda la lógica de negocio vive en procedimientos
  almacenados (`sp_*`); el backend Java solo los invoca vía consultas nativas. Las
  reglas de negocio quedan centralizadas en la base y no se pueden saltar, sin
  importar desde dónde se acceda a los datos.
- **Frontend**: HTML/CSS/JavaScript plano, sin frameworks ni build tools

## Roles del sistema

| Rol | Puede hacer |
|---|---|
| Cliente | Crear solicitudes, adjuntar evidencia, confirmar si el problema quedó resuelto, ver su estado de pago |
| Técnico | Ver sus tareas asignadas, adjuntar evidencia, reportar la solución |
| Administrador | Asignar/reasignar solicitudes, aprobar/rechazar reportes, gestionar estado de pago de clientes, reabrir tickets cerrados |
| Superusuario | Invitar usuarios, activar/suspender/desactivar cuentas, gestionar grupos técnicos, ver auditoría de sesiones y de datos |

Autenticación con JWT y contraseñas hasheadas con BCrypt; cada endpoint valida el
rol en el backend (no es solo una restricción visual del frontend).

## Funcionalidades implementadas

- Ciclo de vida completo del ticket (Pendiente → En Proceso → Pendiente Aprobación
  → Resuelta - Pendiente Confirmación → Cerrada), con historial de cada transición
- Asignación a técnico individual o a grupo técnico
- Adjuntos/evidencia (fotos, PDF) — sube el cliente dueño o el técnico asignado
- Notificaciones automáticas y envío de correo real (invitaciones, avisos de ticket)
- Invitación de usuarios con activación por token (nunca se genera contraseña desde el backend)
- Cambiar contraseña (usuario logeado) y recuperar contraseña olvidada (por correo)
- Gestión de estado de pago del cliente (informativo, no bloquea el servicio)
- Reapertura administrativa de tickets cerrados
- Auditoría de sesiones (quién entró, cuándo, desde qué IP) y auditoría de cambios
  por tabla (qué se modificó, quién lo hizo, valores antes/después) — filtrada para
  cubrir solo operación interna (técnico/administrador/superusuario), no acciones
  del cliente
- Cierre automático de tickets vencidos sin confirmación del cliente (job programado)
- Bloqueo optimista en `solicitud` (control de concurrencia)

## Cómo levantar el proyecto

1. **Base de datos**: crea una base PostgreSQL vacía y corre el script
   `SoporteNet_bd.sql` completo (crea tablas, tipos, funciones, triggers e índices).
2. **Backend**:
   - Copia `soporte-tecnico/src/main/resources/application.properties.example` a
     `application.properties` en la misma carpeta, y completa tus credenciales
     reales (base de datos, JWT secret propio, y opcionalmente una cuenta de Gmail
     con contraseña de aplicación si quieres correo real).
   - Abre la carpeta `soporte-tecnico` (la que tiene `pom.xml`) en IntelliJ y corre
     `SoporteTecnicoApplication`.
   - Por defecto queda escuchando en `http://localhost:8080`.
3. **Frontend**: sirve la carpeta `frontend/` con Live Server (VS Code) u otro
   servidor estático, y abre `views/login.html`.

## Cómo verificar que funciona

- Crea un superusuario directo en la base (`INSERT INTO usuario...` con rol
  `superusuario` y una contraseña ya hasheada), o pide el token de invitación a
  alguien que ya tenga una cuenta de superusuario.
- Loguéate y prueba: invitar un usuario nuevo → activar la cuenta con el token
  recibido → crear una solicitud como cliente → asignarla como administrador →
  reportarla como técnico → aprobarla como administrador → confirmarla como
  cliente.
- El panel de Superusuario → "Auditoría" muestra en vivo cada cambio que se hizo
  durante esa prueba.

## Datos de prueba (volumen)

La base se cargó con más de 3.7 millones de registros de prueba, repartidos entre
las tablas principales del sistema (usuarios, solicitudes, historial de estado,
asignaciones, reportes, notificaciones, adjuntos, sesiones), simulando el ciclo de
vida real de los tickets. Todas las cuentas de prueba usan el correo con dominio
`@soportenet-demo.com` y la contraseña `Prueba1234` (por ejemplo,
`cliente.prueba1@soportenet-demo.com`).
