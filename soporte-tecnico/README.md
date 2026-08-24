# Sistema de Soporte Técnico — Backend (Spring Boot)

## Qué incluye este proyecto

- Conexión configurada a tu base de datos PostgreSQL (`SoporteNet_bd.sql`).
- Entidades JPA: `Categoria`, `Prioridad`, `Estado`, `Usuario`, `Cliente`, `Tecnico`, `Solicitud`.
- Un flujo funcional completo: **crear una solicitud** desde la API, invocando
  directamente tu procedimiento `sp_crear_solicitud` en PostgreSQL.

## Cómo abrirlo en IntelliJ

1. Abre IntelliJ → **File → Open** → selecciona la carpeta `soporte-tecnico` (la que contiene `pom.xml`).
2. IntelliJ va a detectar que es un proyecto Maven y va a descargar las dependencias automáticamente
   (esto puede tardar 1-2 minutos la primera vez). Verás una barra de progreso abajo a la derecha.
3. Abre `src/main/resources/application.properties` y ajusta:
   - `spring.datasource.url` → el nombre real de tu base de datos (si no se llama `soportenet`, cámbialo)
   - `spring.datasource.username` / `password` → tus credenciales reales de PostgreSQL
4. Abre `SoporteTecnicoApplication.java` y dale click al botón ▶️ verde (Run).
5. Si todo está bien, en la consola verás algo como:
   `Tomcat started on port 8080` y `Started SoporteTecnicoApplication`.

## Cómo probarlo

Con el proyecto corriendo, usa Postman, curl, o el archivo `.http` de IntelliJ (clic derecho en el
proyecto → New → HTTP Request) para probar:

### Crear una solicitud

```http
POST http://localhost:8080/api/solicitudes
Content-Type: application/json

{
  "idCliente": 1,
  "descripcion": "No tengo conexión desde ayer",
  "idCategoria": 1
}
```

**Importante:** `idCliente` debe ser un `id_usuario` que ya exista en tu tabla `cliente`
(con `estado_cuenta = 'activo'` en `usuario`), porque `sp_crear_solicitud` lo valida.
Si no tienes clientes de prueba, créalos primero directamente en la base:

```sql
INSERT INTO usuario (nombre_usuario, correo, contrasena_hash, rol, estado_cuenta)
VALUES ('Cliente Prueba', 'cliente@test.com', 'hash', 'cliente', 'activo')
RETURNING id_usuario;

-- usa el id_usuario que te devolvió arriba:
INSERT INTO cliente (id_usuario) VALUES (<id_usuario>);
```

### Consultar una solicitud

```http
GET http://localhost:8080/api/solicitudes/1
```

## Si algo falla

- **Error de conexión al arrancar**: revisa que PostgreSQL esté corriendo y que
  usuario/contraseña/nombre de BD en `application.properties` sean correctos.
- **`relation "solicitud" does not exist`**: significa que apuntaste a una base de datos
  vacía. Verifica que corriste `SoporteNet_bd.sql` sobre la base a la que se conecta esta app.
- **Error 400 al crear una solicitud**: lee el campo `error` del JSON de respuesta — ahí sale
  el mensaje exacto que lanzó `RAISE EXCEPTION` en PostgreSQL (ej. "El cliente no existe").

## Próximos pasos sugeridos

1. Agregar `AsignacionSolicitudController` que invoque `sp_asignar_solicitud`.
2. Agregar `ReporteController` que invoque `sp_enviar_reporte`, `sp_aprobar_reporte`, `sp_rechazar_reporte`.
3. Agregar `sp_confirmar_cliente` para el paso de confirmación del cliente.
4. Recién ahí pensar en autenticación (Spring Security) — no antes, para no bloquear
   el desarrollo del flujo principal.
