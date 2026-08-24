--registros cargados

BEGIN;

ALTER TABLE usuario DISABLE TRIGGER trg_auditar_usuario;
ALTER TABLE cliente DISABLE TRIGGER trg_auditar_cliente;
ALTER TABLE solicitud DISABLE TRIGGER trg_auditar_solicitud;
ALTER TABLE asignacion_solicitud DISABLE TRIGGER trg_auditar_asignacion_solicitud;
ALTER TABLE reporte_solicitud DISABLE TRIGGER trg_auditar_reporte_solicitud;
ALTER TABLE grupo_tecnico DISABLE TRIGGER trg_auditar_grupo_tecnico;
ALTER TABLE tecnico_grupo DISABLE TRIGGER trg_auditar_tecnico_grupo;

CREATE TEMP TABLE tmp_hash AS
SELECT crypt('Prueba1234', gen_salt('bf', 4)) AS hash;

INSERT INTO usuario (nombre_usuario, correo, contrasena_hash, rol, estado_cuenta)
SELECT
    (ARRAY['Juan','María','Carlos','Ana','Luis','Sofía','Pedro','Valentina','Diego','Camila',
           'Andrés','Daniela','Miguel','Paula','José','Laura','Fernando','Gabriela','Ricardo','Lucía'])[1 + (g % 20)]
    || ' ' ||
    (ARRAY['González','Rodríguez','Pérez','López','Martínez','Sánchez','Ramírez','Torres','Flores','Rivera',
           'Gómez','Díaz','Reyes','Morales','Ortiz','Vargas','Castro','Romero','Suárez','Álvarez'])[1 + ((g / 7) % 20)],
    'cliente.prueba' || g || '@soportenet-demo.com',
    h.hash,
    'cliente',
    (CASE WHEN g % 30 = 0 THEN 'suspendido' ELSE 'activo' END)::estado_cuenta_tipo
FROM generate_series(1, 100000) AS g
CROSS JOIN tmp_hash h;

INSERT INTO usuario (nombre_usuario, correo, contrasena_hash, rol, estado_cuenta)
SELECT
    (ARRAY['Juan','María','Carlos','Ana','Luis','Sofía','Pedro','Valentina','Diego','Camila'])[1 + (g % 10)]
    || ' ' ||
    (ARRAY['González','Rodríguez','Pérez','López','Martínez','Sánchez','Ramírez','Torres'])[1 + (g % 8)],
    'tecnico.prueba' || g || '@soportenet-demo.com',
    h.hash,
    'tecnico',
    'activo'::estado_cuenta_tipo
FROM generate_series(1, 3000) AS g
CROSS JOIN tmp_hash h;

INSERT INTO usuario (nombre_usuario, correo, contrasena_hash, rol, estado_cuenta)
SELECT 'Administrador Prueba ' || g, 'admin.prueba' || g || '@soportenet-demo.com', h.hash, 'administrador', 'activo'::estado_cuenta_tipo
FROM generate_series(1, 50) AS g CROSS JOIN tmp_hash h;

INSERT INTO usuario (nombre_usuario, correo, contrasena_hash, rol, estado_cuenta)
SELECT 'Superusuario Prueba ' || g, 'super.prueba' || g || '@soportenet-demo.com', h.hash, 'superusuario', 'activo'::estado_cuenta_tipo
FROM generate_series(1, 10) AS g CROSS JOIN tmp_hash h;


INSERT INTO cliente (id_usuario, direccion, estado_pago)
SELECT
    u.id_usuario,
    'Calle ' || (u.id_usuario % 200) || ', sector ' ||
        (ARRAY['Centro','Norte','Sur','Este','Oeste','San Camilo','El Guayacán','La Esperanza'])[1 + (u.id_usuario % 8)],
    (CASE WHEN u.id_usuario % 12 = 0 THEN 'moroso' ELSE 'al_dia' END)::estado_pago_tipo
FROM usuario u
WHERE u.correo LIKE 'cliente.prueba%@soportenet-demo.com';

INSERT INTO tecnico (id_usuario, especialidad, nivel, habilitado)
SELECT
    u.id_usuario,
    (ARRAY['Redes','Fibra óptica','Soporte en sitio','Configuración Wi-Fi'])[1 + (u.id_usuario % 4)],
    (ARRAY['junior','intermedio','senior'])[1 + (u.id_usuario % 3)]::nivel_tecnico_tipo,
    TRUE
FROM usuario u
WHERE u.correo LIKE 'tecnico.prueba%@soportenet-demo.com';


INSERT INTO grupo_tecnico (nombre_grupo)
SELECT 'Grupo Técnico Prueba ' || g
FROM generate_series(1, 30) AS g;

CREATE TEMP TABLE tmp_grupos AS
SELECT ROW_NUMBER() OVER (ORDER BY id_grupo) AS n, id_grupo
FROM grupo_tecnico WHERE nombre_grupo LIKE 'Grupo Técnico Prueba %';

CREATE TEMP TABLE tmp_tecnicos AS
SELECT ROW_NUMBER() OVER (ORDER BY t.id_usuario) AS n, t.id_usuario
FROM tecnico t
JOIN usuario u ON u.id_usuario = t.id_usuario
WHERE u.correo LIKE 'tecnico.prueba%@soportenet-demo.com';

INSERT INTO tecnico_grupo (id_usuario, id_grupo)
SELECT DISTINCT t.id_usuario, g.id_grupo
FROM tmp_tecnicos t
JOIN tmp_grupos g ON g.n = 1 + (t.n % (SELECT count(*) FROM tmp_grupos));


CREATE TEMP TABLE tmp_clientes AS
SELECT ROW_NUMBER() OVER (ORDER BY c.id_usuario) AS n, c.id_usuario
FROM cliente c
JOIN usuario u ON u.id_usuario = c.id_usuario
WHERE u.correo LIKE 'cliente.prueba%@soportenet-demo.com';

CREATE TEMP TABLE tmp_categorias AS
SELECT ROW_NUMBER() OVER (ORDER BY id_categoria) AS n, id_categoria FROM categoria;

CREATE TEMP TABLE tmp_prioridades AS
SELECT ROW_NUMBER() OVER (ORDER BY id_prioridad) AS n, id_prioridad FROM prioridad;

DO $$
DECLARE
    v_id_pendiente INTEGER := fn_id_estado('Pendiente');
BEGIN
    INSERT INTO solicitud (descripcion, id_cliente, id_categoria, id_prioridad, id_estado, fecha_creacion)
    SELECT
        (ARRAY[
            'Sin conexión a Internet desde hace varias horas.',
            'El servicio presenta lentitud en horas pico.',
            'La conexión se interrumpe varias veces al día.',
            'El router está encendido pero no hay señal.',
            'La ONT muestra luz roja y no hay servicio.',
            'Baja cobertura de la red Wi-Fi en el domicilio.',
            'Se solicita revisión técnica del enlace de fibra.',
            'Consulta sobre valores reflejados en la factura.',
            'Se requiere reconfigurar el router del cliente.',
            'Alta latencia y pérdida de paquetes reportada.',
            'Solicitud de nueva instalación del servicio.',
            'El equipo dejó de funcionar tras un corte eléctrico.'
        ])[1 + (g % 12)] || ' (ticket de prueba #' || g || ')',
        c.id_usuario,
        cat.id_categoria,
        pri.id_prioridad,
        v_id_pendiente,
        now() - ((g % 540) * INTERVAL '1 day') - ((g % 86400) * INTERVAL '1 second')
    FROM generate_series(1, 1000000) AS g
    JOIN tmp_clientes c ON c.n = 1 + (g % 100000)
    JOIN tmp_categorias cat ON cat.n = 1 + (g % (SELECT count(*) FROM tmp_categorias))
    JOIN tmp_prioridades pri ON pri.n = 1 + (g % (SELECT count(*) FROM tmp_prioridades));
END $$;


CREATE TEMP TABLE tmp_asignadas AS
SELECT id_solicitud, ROW_NUMBER() OVER (ORDER BY id_solicitud) AS n
FROM solicitud
WHERE descripcion LIKE '%(ticket de prueba %'
ORDER BY random()
LIMIT 300000;

CREATE TEMP TABLE tmp_admins AS
SELECT ROW_NUMBER() OVER (ORDER BY id_usuario) AS n, id_usuario
FROM usuario WHERE correo LIKE 'admin.prueba%@soportenet-demo.com';

INSERT INTO asignacion_solicitud (id_solicitud, id_tecnico, id_usuario_asigna, motivo_reasignacion, es_reasignacion)
SELECT
    a.id_solicitud,
    t.id_usuario,
    ad.id_usuario,
    NULL,
    FALSE
FROM tmp_asignadas a
JOIN tmp_tecnicos t ON t.n = 1 + (a.n % (SELECT count(*) FROM tmp_tecnicos))
JOIN tmp_admins ad ON ad.n = 1 + (a.n % (SELECT count(*) FROM tmp_admins));

UPDATE solicitud SET id_estado = fn_id_estado('En Proceso')
WHERE id_solicitud IN (SELECT id_solicitud FROM tmp_asignadas);


CREATE TEMP TABLE tmp_reportadas AS
SELECT a.id_solicitud, a.id_tecnico
FROM asignacion_solicitud a
JOIN tmp_asignadas ta ON ta.id_solicitud = a.id_solicitud
ORDER BY random()
LIMIT 150000;

INSERT INTO reporte_solicitud (id_solicitud, id_tecnico, detalle_reporte)
SELECT id_solicitud, id_tecnico, 'Se realizó revisión técnica y se aplicó la solución correspondiente (registro de prueba).'
FROM tmp_reportadas;

UPDATE solicitud SET id_estado = fn_id_estado('Pendiente Aprobación')
WHERE id_solicitud IN (SELECT id_solicitud FROM tmp_reportadas);


CREATE TEMP TABLE tmp_resueltas AS
SELECT id_solicitud FROM tmp_reportadas ORDER BY random() LIMIT 100000;

UPDATE solicitud
   SET id_estado = fn_id_estado('Resuelta - Pendiente Confirmación del Cliente'),
       fecha_limite_confirmacion = now() + INTERVAL '3 days'
 WHERE id_solicitud IN (SELECT id_solicitud FROM tmp_resueltas);


CREATE TEMP TABLE tmp_cerradas AS
SELECT id_solicitud FROM tmp_resueltas ORDER BY random() LIMIT 70000;

UPDATE solicitud SET id_estado = fn_id_estado('Cerrada')
WHERE id_solicitud IN (SELECT id_solicitud FROM tmp_cerradas);


INSERT INTO notificacion (id_usuario_destino, tipo_evento, mensaje, id_solicitud_relacionada, correo_enviado)
SELECT a.id_tecnico, 'ticket_asignado', 'Se le ha asignado la solicitud #' || a.id_solicitud || '.', a.id_solicitud, TRUE
FROM tmp_asignadas ta JOIN asignacion_solicitud a ON a.id_solicitud = ta.id_solicitud;

INSERT INTO notificacion (id_usuario_destino, tipo_evento, mensaje, id_solicitud_relacionada, correo_enviado)
SELECT s.id_cliente, 'ticket_resuelto_pendiente_confirmacion',
       'Su solicitud #' || s.id_solicitud || ' fue resuelta. Por favor confirme si el problema quedó solucionado.',
       s.id_solicitud, TRUE
FROM solicitud s
JOIN tmp_resueltas r ON r.id_solicitud = s.id_solicitud;


CREATE TEMP TABLE tmp_con_adjunto AS
SELECT id_solicitud FROM tmp_asignadas ORDER BY random() LIMIT 80000;

INSERT INTO adjunto (id_solicitud, id_usuario_sube, nombre_archivo, tipo_archivo, tamano_archivo, url_almacenamiento)
SELECT
    s.id_solicitud,
    s.id_cliente,
    'evidencia_' || s.id_solicitud || '.jpg',
    'image/jpeg',
    250000 + (s.id_solicitud % 500000),
    '/uploads/adjuntos/demo/evidencia_' || s.id_solicitud || '.jpg'
FROM solicitud s
JOIN tmp_con_adjunto ca ON ca.id_solicitud = s.id_solicitud;


CREATE TEMP TABLE tmp_todos_usuarios_prueba AS
SELECT ROW_NUMBER() OVER (ORDER BY id_usuario) AS n, id_usuario
FROM usuario
WHERE correo LIKE '%@soportenet-demo.com';

INSERT INTO auditoria_sesion (id_usuario, fecha_entrada, ultima_actividad, fecha_salida, ip_origen)
SELECT
    tu.id_usuario,
    e,
    e + (floor(random() * 40) * INTERVAL '1 minute'),
    CASE WHEN g % 20 = 0 THEN NULL ELSE e + (floor(random() * 45) * INTERVAL '1 minute') END,
    ('192.168.' || (1 + (g % 254)) || '.' || (1 + ((g * 7) % 254)))::inet
FROM generate_series(1, 20000) AS g
JOIN tmp_todos_usuarios_prueba tu ON tu.n = 1 + (g % (SELECT count(*) FROM tmp_todos_usuarios_prueba))
CROSS JOIN LATERAL (SELECT now() - ((g % 180) * INTERVAL '1 day') - ((g % 86400) * INTERVAL '1 second') AS e) fecha;


ALTER TABLE usuario ENABLE TRIGGER trg_auditar_usuario;
ALTER TABLE cliente ENABLE TRIGGER trg_auditar_cliente;
ALTER TABLE solicitud ENABLE TRIGGER trg_auditar_solicitud;
ALTER TABLE asignacion_solicitud ENABLE TRIGGER trg_auditar_asignacion_solicitud;
ALTER TABLE reporte_solicitud ENABLE TRIGGER trg_auditar_reporte_solicitud;
ALTER TABLE grupo_tecnico ENABLE TRIGGER trg_auditar_grupo_tecnico;
ALTER TABLE tecnico_grupo ENABLE TRIGGER trg_auditar_tecnico_grupo;

COMMIT;

SELECT 'usuario' AS tabla, count(*) FROM usuario
UNION ALL SELECT 'cliente', count(*) FROM cliente
UNION ALL SELECT 'tecnico', count(*) FROM tecnico
UNION ALL SELECT 'grupo_tecnico', count(*) FROM grupo_tecnico
UNION ALL SELECT 'tecnico_grupo', count(*) FROM tecnico_grupo
UNION ALL SELECT 'solicitud', count(*) FROM solicitud
UNION ALL SELECT 'historial_estado', count(*) FROM historial_estado
UNION ALL SELECT 'asignacion_solicitud', count(*) FROM asignacion_solicitud
UNION ALL SELECT 'reporte_solicitud', count(*) FROM reporte_solicitud
UNION ALL SELECT 'notificacion', count(*) FROM notificacion
UNION ALL SELECT 'adjunto', count(*) FROM adjunto
UNION ALL SELECT 'auditoria_sesion', count(*) FROM auditoria_sesion
UNION ALL SELECT 'auditoria_datos', count(*) FROM auditoria_datos
ORDER BY 1;
