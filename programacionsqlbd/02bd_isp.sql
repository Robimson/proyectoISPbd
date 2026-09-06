DROP FUNCTION IF EXISTS sp_crear_solicitud(BIGINT, TEXT, INTEGER, INTEGER);

CREATE OR REPLACE FUNCTION sp_crear_solicitud(
    p_id_cliente    BIGINT,
    p_descripcion   TEXT,
    p_id_categoria  INTEGER DEFAULT NULL
)
RETURNS BIGINT
LANGUAGE plpgsql
AS
$$
DECLARE
    v_id_solicitud BIGINT;
    v_id_estado_pendiente INTEGER;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM cliente WHERE id_usuario = p_id_cliente
    ) THEN
        RAISE EXCEPTION 'El cliente % no existe.', p_id_cliente;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM usuario
        WHERE id_usuario = p_id_cliente AND estado_cuenta = 'activo'
    ) THEN
        RAISE EXCEPTION 'El cliente % no tiene una cuenta activa.', p_id_cliente;
    END IF;

    IF p_descripcion IS NULL OR btrim(p_descripcion) = '' THEN
        RAISE EXCEPTION 'La descripcion no puede estar vacia.';
    END IF;

    IF p_id_categoria IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM categoria WHERE id_categoria = p_id_categoria
        ) THEN
            RAISE EXCEPTION 'La categoria % no existe.', p_id_categoria;
        END IF;
    END IF;

    v_id_estado_pendiente := fn_id_estado('Pendiente');
    IF v_id_estado_pendiente IS NULL THEN
        RAISE EXCEPTION 'El estado "Pendiente" no existe en la tabla estado.';
    END IF;

    INSERT INTO solicitud (descripcion, id_cliente, id_categoria, id_estado)
    VALUES (btrim(p_descripcion), p_id_cliente, p_id_categoria, v_id_estado_pendiente)
    RETURNING id_solicitud INTO v_id_solicitud;

    PERFORM set_config('app.usuario_actual', p_id_cliente::TEXT, true);

    RETURN v_id_solicitud;
END;
$$;





--




-- 1) Reemplaza sp_crear_solicitud: ya no recibe prioridad (la pone el admin al asignar)
DROP FUNCTION IF EXISTS sp_crear_solicitud(BIGINT, TEXT, INTEGER, INTEGER);

CREATE OR REPLACE FUNCTION sp_crear_solicitud(
    p_id_cliente    BIGINT,
    p_descripcion   TEXT,
    p_id_categoria  INTEGER DEFAULT NULL
)
RETURNS BIGINT
LANGUAGE plpgsql
AS
$$
DECLARE
    v_id_solicitud BIGINT;
    v_id_estado_pendiente INTEGER;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM cliente WHERE id_usuario = p_id_cliente) THEN
        RAISE EXCEPTION 'El cliente % no existe.', p_id_cliente;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM usuario WHERE id_usuario = p_id_cliente AND estado_cuenta = 'activo'
    ) THEN
        RAISE EXCEPTION 'El cliente % no tiene una cuenta activa.', p_id_cliente;
    END IF;

    IF p_descripcion IS NULL OR btrim(p_descripcion) = '' THEN
        RAISE EXCEPTION 'La descripcion no puede estar vacia.';
    END IF;

    IF p_id_categoria IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM categoria WHERE id_categoria = p_id_categoria) THEN
            RAISE EXCEPTION 'La categoria % no existe.', p_id_categoria;
        END IF;
    END IF;

    v_id_estado_pendiente := fn_id_estado('Pendiente');
    IF v_id_estado_pendiente IS NULL THEN
        RAISE EXCEPTION 'El estado "Pendiente" no existe en la tabla estado.';
    END IF;

    INSERT INTO solicitud (descripcion, id_cliente, id_categoria, id_estado)
    VALUES (btrim(p_descripcion), p_id_cliente, p_id_categoria, v_id_estado_pendiente)
    RETURNING id_solicitud INTO v_id_solicitud;

    PERFORM set_config('app.usuario_actual', p_id_cliente::TEXT, true);

    RETURN v_id_solicitud;
END;
$$;


-- 2) Invitar usuario (nuevo)
CREATE OR REPLACE FUNCTION sp_invitar_usuario(
    p_id_superusuario  BIGINT,
    p_nombre_usuario   VARCHAR,
    p_correo           VARCHAR,
    p_rol              rol_usuario_tipo,
    p_dias_validez     INTEGER DEFAULT 7
)
RETURNS TEXT
LANGUAGE plpgsql
AS
$$
DECLARE
    v_id_usuario BIGINT;
    v_token      TEXT;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM usuario
        WHERE id_usuario = p_id_superusuario AND rol = 'superusuario' AND estado_cuenta = 'activo'
    ) THEN
        RAISE EXCEPTION 'El usuario % no es un superusuario activo.', p_id_superusuario;
    END IF;

    IF p_nombre_usuario IS NULL OR btrim(p_nombre_usuario) = '' THEN
        RAISE EXCEPTION 'El nombre de usuario no puede estar vacio.';
    END IF;

    IF p_correo IS NULL OR btrim(p_correo) = '' THEN
        RAISE EXCEPTION 'El correo no puede estar vacio.';
    END IF;

    IF EXISTS (SELECT 1 FROM usuario WHERE correo = btrim(p_correo)) THEN
        RAISE EXCEPTION 'Ya existe un usuario con el correo %.', p_correo;
    END IF;

    IF p_dias_validez <= 0 THEN
        RAISE EXCEPTION 'La validez del token debe ser mayor que cero.';
    END IF;

    INSERT INTO usuario (nombre_usuario, correo, rol, estado_cuenta)
    VALUES (btrim(p_nombre_usuario), btrim(p_correo), p_rol, 'inactivo')
    RETURNING id_usuario INTO v_id_usuario;

    IF p_rol = 'cliente' THEN
        INSERT INTO cliente (id_usuario) VALUES (v_id_usuario);
    ELSIF p_rol = 'tecnico' THEN
        INSERT INTO tecnico (id_usuario) VALUES (v_id_usuario);
    END IF;

    v_token := encode(gen_random_bytes(32), 'hex');

    INSERT INTO token_activacion (token, id_usuario, tipo_token, fecha_expiracion)
    VALUES (v_token, v_id_usuario, 'invitacion', now() + make_interval(days => p_dias_validez));

    RETURN v_token;
END;
$$;


-- 3) Activar cuenta (nuevo)
CREATE OR REPLACE FUNCTION sp_activar_cuenta(
    p_token             TEXT,
    p_contrasena_hash   VARCHAR
)
RETURNS BIGINT
LANGUAGE plpgsql
AS
$$
DECLARE
    v_id_token   BIGINT;
    v_id_usuario BIGINT;
BEGIN
    IF p_contrasena_hash IS NULL OR btrim(p_contrasena_hash) = '' THEN
        RAISE EXCEPTION 'La contrasena es obligatoria.';
    END IF;

    SELECT id_token, id_usuario INTO v_id_token, v_id_usuario
    FROM token_activacion
    WHERE token = p_token AND tipo_token = 'invitacion' AND usado = false AND fecha_expiracion > now()
    FOR UPDATE;

    IF v_id_usuario IS NULL THEN
        RAISE EXCEPTION 'El token de activacion no existe, ya fue usado o esta vencido.';
    END IF;

    UPDATE usuario SET contrasena_hash = p_contrasena_hash, estado_cuenta = 'activo'
    WHERE id_usuario = v_id_usuario;

    UPDATE token_activacion SET usado = true WHERE id_token = v_id_token;

    RETURN v_id_usuario;
END;
$$;


-- 4) Cambiar estado de cuenta (nuevo)
CREATE OR REPLACE FUNCTION sp_cambiar_estado_cuenta(
    p_id_superusuario      BIGINT,
    p_id_usuario_objetivo  BIGINT,
    p_nuevo_estado         estado_cuenta_tipo
)
RETURNS VOID
LANGUAGE plpgsql
AS
$$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM usuario
        WHERE id_usuario = p_id_superusuario AND rol = 'superusuario' AND estado_cuenta = 'activo'
    ) THEN
        RAISE EXCEPTION 'El usuario % no es un superusuario activo.', p_id_superusuario;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM usuario WHERE id_usuario = p_id_usuario_objetivo) THEN
        RAISE EXCEPTION 'El usuario % no existe.', p_id_usuario_objetivo;
    END IF;

    IF p_id_usuario_objetivo = p_id_superusuario AND p_nuevo_estado <> 'activo' THEN
        RAISE EXCEPTION 'Un superusuario no puede cambiar su propio estado a %.', p_nuevo_estado;
    END IF;

    UPDATE usuario SET estado_cuenta = p_nuevo_estado WHERE id_usuario = p_id_usuario_objetivo;
END;
$$;



--

INSERT INTO usuario (nombre_usuario, correo, contrasena_hash, rol, estado_cuenta)
VALUES (
    'Admin Inicial',
    'admin@soportenet.local',
    '$2a$10$1ZuftM2CigzJhRYu8lwtQOn25qAGC9U5W8TUGuBdGaLgjaW/Kt6M6',
    'superusuario',
    'activo'
);



-- 1) CLIENTE
WITH nuevo AS (
    INSERT INTO usuario (nombre_usuario, correo, contrasena_hash, rol, estado_cuenta)
    VALUES ('Cliente Prueba', 'cliente@test.com', '$2a$10$8aPdrtuZKjinc8Sym4qdHuTeIzQmS6ueSP9/XL2pxpsdBtju0Uiz6', 'cliente', 'activo')
    RETURNING id_usuario
)
INSERT INTO cliente (id_usuario) SELECT id_usuario FROM nuevo;

-- 2) TECNICO
WITH nuevo AS (
    INSERT INTO usuario (nombre_usuario, correo, contrasena_hash, rol, estado_cuenta)
    VALUES ('Tecnico Prueba', 'tecnico@test.com', '$2a$10$8aPdrtuZKjinc8Sym4qdHuTeIzQmS6ueSP9/XL2pxpsdBtju0Uiz6', 'tecnico', 'activo')
    RETURNING id_usuario
)
INSERT INTO tecnico (id_usuario) SELECT id_usuario FROM nuevo;

-- 3) ADMINISTRADOR
INSERT INTO usuario (nombre_usuario, correo, contrasena_hash, rol, estado_cuenta)
VALUES ('Admin Prueba', 'admin@test.com', '$2a$10$8aPdrtuZKjinc8Sym4qdHuTeIzQmS6ueSP9/XL2pxpsdBtju0Uiz6', 'administrador', 'activo');

-- 4) SUPERUSUARIO
INSERT INTO usuario (nombre_usuario, correo, contrasena_hash, rol, estado_cuenta)
VALUES ('Super Prueba', 'super@test.com', '$2a$10$8aPdrtuZKjinc8Sym4qdHuTeIzQmS6ueSP9/XL2pxpsdBtju0Uiz6', 'superusuario', 'activo');





--23/08
ALTER TABLE adjunto ADD COLUMN id_usuario_sube BIGINT REFERENCES usuario(id_usuario);

-- 2) Nuevo procedimiento: agregar adjunto
CREATE OR REPLACE FUNCTION sp_agregar_adjunto(
    p_id_solicitud        BIGINT,
    p_id_usuario_sube     BIGINT,
    p_nombre_archivo      VARCHAR,
    p_tipo_archivo        VARCHAR,
    p_tamano_archivo      BIGINT,
    p_url_almacenamiento  VARCHAR
)
RETURNS BIGINT
LANGUAGE plpgsql
AS
$$
DECLARE
    v_estado_actual       INTEGER;
    v_id_cliente_dueno    BIGINT;
    v_id_estado_cerrada   INTEGER;
    v_id_tecnico_vigente  BIGINT;
    v_id_grupo_vigente    BIGINT;
    v_autorizado          BOOLEAN;
    v_total_actual        INTEGER;
    v_id_adjunto          BIGINT;
BEGIN
    SELECT id_estado, id_cliente
    INTO v_estado_actual, v_id_cliente_dueno
    FROM solicitud
    WHERE id_solicitud = p_id_solicitud
    FOR UPDATE;

    IF v_estado_actual IS NULL THEN
        RAISE EXCEPTION 'La solicitud % no existe.', p_id_solicitud;
    END IF;

    v_id_estado_cerrada := fn_id_estado('Cerrada');
    IF v_estado_actual = v_id_estado_cerrada THEN
        RAISE EXCEPTION 'No se pueden agregar adjuntos a una solicitud Cerrada.';
    END IF;

    -- Autorizacion: cliente dueno, o tecnico asignado (directo o por grupo vigente)
    v_autorizado := (v_id_cliente_dueno = p_id_usuario_sube);

    IF NOT v_autorizado THEN
        SELECT id_tecnico, id_grupo
        INTO v_id_tecnico_vigente, v_id_grupo_vigente
        FROM asignacion_solicitud
        WHERE id_solicitud = p_id_solicitud
          AND vigente = TRUE
        LIMIT 1;

        v_autorizado :=
            (v_id_tecnico_vigente = p_id_usuario_sube)
            OR (
                v_id_grupo_vigente IS NOT NULL
                AND EXISTS (
                    SELECT 1 FROM tecnico_grupo
                    WHERE id_usuario = p_id_usuario_sube
                      AND id_grupo = v_id_grupo_vigente
                )
            );
    END IF;

    IF NOT v_autorizado THEN
        RAISE EXCEPTION 'El usuario % no esta autorizado para agregar adjuntos a la solicitud %.',
            p_id_usuario_sube, p_id_solicitud;
    END IF;

    -- Maximo 5 adjuntos por solicitud
    SELECT count(*) INTO v_total_actual
    FROM adjunto
    WHERE id_solicitud = p_id_solicitud;

    IF v_total_actual >= 5 THEN
        RAISE EXCEPTION 'La solicitud % ya tiene el maximo de 5 adjuntos.', p_id_solicitud;
    END IF;

    IF p_nombre_archivo IS NULL OR btrim(p_nombre_archivo) = '' THEN
        RAISE EXCEPTION 'El nombre del archivo no puede estar vacio.';
    END IF;

    IF p_tamano_archivo IS NULL OR p_tamano_archivo <= 0 THEN
        RAISE EXCEPTION 'El tamano del archivo no es valido.';
    END IF;

    INSERT INTO adjunto
        (id_solicitud, id_usuario_sube, nombre_archivo, tipo_archivo, tamano_archivo, url_almacenamiento)
    VALUES
        (p_id_solicitud, p_id_usuario_sube, btrim(p_nombre_archivo), p_tipo_archivo, p_tamano_archivo, p_url_almacenamiento)
    RETURNING id_adjunto INTO v_id_adjunto;

    RETURN v_id_adjunto;
END;
$$;



CREATE OR REPLACE FUNCTION sp_cambiar_estado_pago(
    p_id_administrador BIGINT,
    p_id_cliente BIGINT,
    p_nuevo_estado_pago estado_pago_tipo
)
RETURNS VOID AS $$
DECLARE
    v_rol_admin VARCHAR;
    v_estado_admin VARCHAR;
    v_existe_cliente BOOLEAN;
BEGIN
    SELECT rol, estado_cuenta INTO v_rol_admin, v_estado_admin
    FROM usuario
    WHERE id_usuario = p_id_administrador;

    IF v_rol_admin IS NULL THEN
        RAISE EXCEPTION 'El administrador indicado no existe';
    END IF;

    IF v_rol_admin NOT IN ('administrador', 'superusuario') THEN
        RAISE EXCEPTION 'Solo un administrador o superusuario puede cambiar el estado de pago';
    END IF;

    IF v_estado_admin <> 'activo' THEN
        RAISE EXCEPTION 'La cuenta del administrador no está activa';
    END IF;

    SELECT EXISTS(SELECT 1 FROM cliente WHERE id_cliente = p_id_cliente) INTO v_existe_cliente;

    IF NOT v_existe_cliente THEN
        RAISE EXCEPTION 'El cliente indicado no existe';
    END IF;

    UPDATE cliente
    SET estado_pago = p_nuevo_estado_pago
    WHERE id_cliente = p_id_cliente;
END;
$$ LANGUAGE plpgsql;




ALTER TABLE notificacion
    ADD COLUMN IF NOT EXISTS correo_enviado BOOLEAN NOT NULL DEFAULT false;

UPDATE notificacion
SET correo_enviado = true
WHERE correo_enviado = false;




CREATE OR REPLACE FUNCTION sp_solicitar_recuperacion_contrasena(
    p_correo         VARCHAR,
    p_horas_validez  INTEGER DEFAULT 2
)
RETURNS TEXT AS $$
DECLARE
    v_id_usuario BIGINT;
    v_token      TEXT;
BEGIN
    SELECT id_usuario INTO v_id_usuario
    FROM usuario
    WHERE correo = btrim(p_correo)
      AND estado_cuenta = 'activo';

    IF v_id_usuario IS NULL THEN
        RETURN NULL;
    END IF;

    v_token := encode(gen_random_bytes(32), 'hex');

    INSERT INTO token_activacion
        (token, id_usuario, tipo_token, fecha_expiracion)
    VALUES
        (v_token, v_id_usuario, 'recuperacion', now() + make_interval(hours => p_horas_validez));

    RETURN v_token;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION sp_restablecer_contrasena(
    p_token             TEXT,
    p_contrasena_hash   VARCHAR
)
RETURNS BIGINT AS $$
DECLARE
    v_id_token   BIGINT;
    v_id_usuario BIGINT;
BEGIN
    IF p_contrasena_hash IS NULL OR btrim(p_contrasena_hash) = '' THEN
        RAISE EXCEPTION 'La contrasena es obligatoria.';
    END IF;

    SELECT id_token, id_usuario
    INTO v_id_token, v_id_usuario
    FROM token_activacion
    WHERE token = p_token
      AND tipo_token = 'recuperacion'
      AND usado = false
      AND fecha_expiracion > now()
    FOR UPDATE;

    IF v_id_usuario IS NULL THEN
        RAISE EXCEPTION 'El token de recuperacion no existe, ya fue usado o esta vencido.';
    END IF;

    UPDATE usuario
       SET contrasena_hash = p_contrasena_hash
     WHERE id_usuario = v_id_usuario;

    UPDATE token_activacion
       SET usado = true
     WHERE id_token = v_id_token;

    RETURN v_id_usuario;
END;
$$ LANGUAGE plpgsql;



--




-- ============================================================
-- 1) NUEVO: auditoría por tablas (operación interna: técnico,
--    administrador y superusuario — nunca acciones del cliente)
-- ============================================================

CREATE TYPE operacion_auditoria_tipo AS ENUM ('INSERT', 'UPDATE', 'DELETE');

CREATE TABLE auditoria_datos (
    id_auditoria             BIGSERIAL PRIMARY KEY,
    tabla_afectada           VARCHAR(100) NOT NULL,
    operacion                operacion_auditoria_tipo NOT NULL,
    datos_anteriores         JSONB,
    datos_nuevos             JSONB,
    id_usuario_responsable   BIGINT REFERENCES usuario(id_usuario),
    fecha                    TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMENT ON TABLE auditoria_datos IS 'Auditoria de cambios en tablas de operacion interna (tecnico/administrador/superusuario). No registra acciones hechas directamente por el cliente.';

CREATE INDEX idx_auditoria_tabla_fecha ON auditoria_datos (tabla_afectada, fecha DESC);
CREATE INDEX idx_auditoria_usuario     ON auditoria_datos (id_usuario_responsable);

CREATE OR REPLACE FUNCTION fn_auditar_cambio()
RETURNS TRIGGER
LANGUAGE plpgsql
AS
$$
DECLARE
    v_id_usuario_actual BIGINT;
    v_rol_actual        rol_usuario_tipo;
BEGIN
    BEGIN
        v_id_usuario_actual := NULLIF(current_setting('app.usuario_actual', true), '')::BIGINT;
    EXCEPTION
        WHEN OTHERS THEN
            v_id_usuario_actual := NULL;
    END;

    IF v_id_usuario_actual IS NOT NULL THEN
        SELECT rol INTO v_rol_actual FROM usuario WHERE id_usuario = v_id_usuario_actual;

        IF v_rol_actual = 'cliente' THEN
            RETURN COALESCE(NEW, OLD);
        END IF;
    END IF;

    INSERT INTO auditoria_datos
        (tabla_afectada, operacion, datos_anteriores, datos_nuevos, id_usuario_responsable)
    VALUES
        (
            TG_TABLE_NAME,
            TG_OP::operacion_auditoria_tipo,
            CASE WHEN TG_OP IN ('UPDATE', 'DELETE') THEN row_to_json(OLD)::JSONB ELSE NULL END,
            CASE WHEN TG_OP IN ('INSERT', 'UPDATE') THEN row_to_json(NEW)::JSONB ELSE NULL END,
            v_id_usuario_actual
        );

    RETURN COALESCE(NEW, OLD);
END;
$$;

CREATE TRIGGER trg_auditar_usuario
AFTER INSERT OR UPDATE OR DELETE ON usuario
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio();

CREATE TRIGGER trg_auditar_cliente
AFTER INSERT OR UPDATE OR DELETE ON cliente
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio();

CREATE TRIGGER trg_auditar_solicitud
AFTER INSERT OR UPDATE OR DELETE ON solicitud
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio();

CREATE TRIGGER trg_auditar_asignacion_solicitud
AFTER INSERT OR UPDATE OR DELETE ON asignacion_solicitud
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio();

CREATE TRIGGER trg_auditar_reporte_solicitud
AFTER INSERT OR UPDATE OR DELETE ON reporte_solicitud
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio();

CREATE TRIGGER trg_auditar_grupo_tecnico
AFTER INSERT OR UPDATE OR DELETE ON grupo_tecnico
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio();

CREATE TRIGGER trg_auditar_tecnico_grupo
AFTER INSERT OR UPDATE OR DELETE ON tecnico_grupo
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio();


-- ============================================================
-- 2) NUEVO: abrir/cerrar sesión (para que la auditoría de
--    sesiones que ya existía en el script base tenga datos)
-- ============================================================

CREATE OR REPLACE FUNCTION sp_abrir_sesion(
    p_id_usuario BIGINT,
    p_ip_origen  INET DEFAULT NULL
)
RETURNS BIGINT
LANGUAGE plpgsql
AS
$$
DECLARE
    v_id_sesion BIGINT;
BEGIN
    INSERT INTO auditoria_sesion (id_usuario, ip_origen)
    VALUES (p_id_usuario, p_ip_origen)
    RETURNING id_sesion INTO v_id_sesion;

    RETURN v_id_sesion;
END;
$$;

CREATE OR REPLACE FUNCTION sp_cerrar_sesion(
    p_id_sesion BIGINT
)
RETURNS VOID
LANGUAGE plpgsql
AS
$$
BEGIN
    UPDATE auditoria_sesion
       SET fecha_salida = now()
     WHERE id_sesion = p_id_sesion
       AND fecha_salida IS NULL;
END;
$$;



CREATE OR REPLACE FUNCTION sp_crear_solicitud(
    p_id_cliente    BIGINT,
    p_descripcion   TEXT,
    p_id_categoria  INTEGER DEFAULT NULL
)
RETURNS BIGINT
LANGUAGE plpgsql
AS
$$
DECLARE
    v_id_solicitud BIGINT;
    v_id_estado_pendiente INTEGER;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM cliente WHERE id_usuario = p_id_cliente) THEN
        RAISE EXCEPTION 'El cliente % no existe.', p_id_cliente;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM usuario WHERE id_usuario = p_id_cliente AND estado_cuenta = 'activo'
    ) THEN
        RAISE EXCEPTION 'El cliente % no tiene una cuenta activa.', p_id_cliente;
    END IF;

    IF p_descripcion IS NULL OR btrim(p_descripcion) = '' THEN
        RAISE EXCEPTION 'La descripcion no puede estar vacia.';
    END IF;

    IF p_id_categoria IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM categoria WHERE id_categoria = p_id_categoria) THEN
            RAISE EXCEPTION 'La categoria % no existe.', p_id_categoria;
        END IF;
    END IF;

    v_id_estado_pendiente := fn_id_estado('Pendiente');
    IF v_id_estado_pendiente IS NULL THEN
        RAISE EXCEPTION 'El estado "Pendiente" no existe en la tabla estado.';
    END IF;

    PERFORM set_config('app.usuario_actual', p_id_cliente::TEXT, true);

    INSERT INTO solicitud (descripcion, id_cliente, id_categoria, id_estado)
    VALUES (btrim(p_descripcion), p_id_cliente, p_id_categoria, v_id_estado_pendiente)
    RETURNING id_solicitud INTO v_id_solicitud;

    RETURN v_id_solicitud;
END;
$$;


CREATE OR REPLACE FUNCTION sp_enviar_reporte(
    p_id_solicitud     BIGINT,
    p_id_tecnico       BIGINT,
    p_detalle_reporte  TEXT
)
RETURNS BIGINT
LANGUAGE plpgsql
AS
$$
DECLARE
    v_id_reporte       BIGINT;
    v_estado_actual    INTEGER;
    v_tecnico_vigente  BIGINT;
    v_grupo_vigente    BIGINT;
    v_autorizado       BOOLEAN;
BEGIN
    SELECT id_estado INTO v_estado_actual FROM solicitud WHERE id_solicitud = p_id_solicitud FOR UPDATE;

    IF v_estado_actual IS NULL THEN
        RAISE EXCEPTION 'La solicitud % no existe.', p_id_solicitud;
    END IF;

    IF v_estado_actual <> fn_id_estado('En Proceso') THEN
        RAISE EXCEPTION 'Solo se puede enviar un reporte cuando la solicitud está En Proceso.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM tecnico WHERE id_usuario = p_id_tecnico AND habilitado = TRUE) THEN
        RAISE EXCEPTION 'El técnico % no existe o no está habilitado.', p_id_tecnico;
    END IF;

    SELECT id_tecnico, id_grupo INTO v_tecnico_vigente, v_grupo_vigente
    FROM asignacion_solicitud
    WHERE id_solicitud = p_id_solicitud AND vigente = TRUE
    LIMIT 1;

    IF v_tecnico_vigente IS NULL AND v_grupo_vigente IS NULL THEN
        RAISE EXCEPTION 'La solicitud % no tiene una asignación vigente.', p_id_solicitud;
    END IF;

    v_autorizado :=
        (v_tecnico_vigente = p_id_tecnico)
        OR (
            v_grupo_vigente IS NOT NULL
            AND EXISTS (SELECT 1 FROM tecnico_grupo WHERE id_usuario = p_id_tecnico AND id_grupo = v_grupo_vigente)
        );

    IF NOT v_autorizado THEN
        RAISE EXCEPTION 'El técnico % no está autorizado para reportar la solicitud %.', p_id_tecnico, p_id_solicitud;
    END IF;

    IF p_detalle_reporte IS NULL OR btrim(p_detalle_reporte) = '' THEN
        RAISE EXCEPTION 'El detalle del reporte no puede estar vacío.';
    END IF;

    PERFORM set_config('app.usuario_actual', p_id_tecnico::TEXT, true);

    INSERT INTO reporte_solicitud (id_solicitud, id_tecnico, detalle_reporte)
    VALUES (p_id_solicitud, p_id_tecnico, btrim(p_detalle_reporte))
    RETURNING id_reporte INTO v_id_reporte;

    UPDATE solicitud SET id_estado = fn_id_estado('Pendiente Aprobación') WHERE id_solicitud = p_id_solicitud;

    RETURN v_id_reporte;
END;
$$;


CREATE OR REPLACE FUNCTION sp_invitar_usuario(
    p_id_superusuario  BIGINT,
    p_nombre_usuario   VARCHAR,
    p_correo           VARCHAR,
    p_rol              rol_usuario_tipo,
    p_dias_validez     INTEGER DEFAULT 7
)
RETURNS TEXT
LANGUAGE plpgsql
AS
$$
DECLARE
    v_id_usuario BIGINT;
    v_token      TEXT;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM usuario WHERE id_usuario = p_id_superusuario AND rol = 'superusuario' AND estado_cuenta = 'activo'
    ) THEN
        RAISE EXCEPTION 'El usuario % no es un superusuario activo.', p_id_superusuario;
    END IF;

    IF p_nombre_usuario IS NULL OR btrim(p_nombre_usuario) = '' THEN
        RAISE EXCEPTION 'El nombre de usuario no puede estar vacio.';
    END IF;

    IF p_correo IS NULL OR btrim(p_correo) = '' THEN
        RAISE EXCEPTION 'El correo no puede estar vacio.';
    END IF;

    IF EXISTS (SELECT 1 FROM usuario WHERE correo = btrim(p_correo)) THEN
        RAISE EXCEPTION 'Ya existe un usuario con el correo %.', p_correo;
    END IF;

    IF p_dias_validez <= 0 THEN
        RAISE EXCEPTION 'La validez del token debe ser mayor que cero.';
    END IF;

    PERFORM set_config('app.usuario_actual', p_id_superusuario::TEXT, true);

    INSERT INTO usuario (nombre_usuario, correo, rol, estado_cuenta)
    VALUES (btrim(p_nombre_usuario), btrim(p_correo), p_rol, 'inactivo')
    RETURNING id_usuario INTO v_id_usuario;

    IF p_rol = 'cliente' THEN
        INSERT INTO cliente (id_usuario) VALUES (v_id_usuario);
    ELSIF p_rol = 'tecnico' THEN
        INSERT INTO tecnico (id_usuario) VALUES (v_id_usuario);
    END IF;

    v_token := encode(gen_random_bytes(32), 'hex');

    INSERT INTO token_activacion (token, id_usuario, tipo_token, fecha_expiracion)
    VALUES (v_token, v_id_usuario, 'invitacion', now() + make_interval(days => p_dias_validez));

    RETURN v_token;
END;
$$;


CREATE OR REPLACE FUNCTION sp_activar_cuenta(
    p_token             TEXT,
    p_contrasena_hash   VARCHAR
)
RETURNS BIGINT
LANGUAGE plpgsql
AS
$$
DECLARE
    v_id_token   BIGINT;
    v_id_usuario BIGINT;
BEGIN
    IF p_contrasena_hash IS NULL OR btrim(p_contrasena_hash) = '' THEN
        RAISE EXCEPTION 'La contrasena es obligatoria.';
    END IF;

    SELECT id_token, id_usuario INTO v_id_token, v_id_usuario
    FROM token_activacion
    WHERE token = p_token AND tipo_token = 'invitacion' AND usado = false AND fecha_expiracion > now()
    FOR UPDATE;

    IF v_id_usuario IS NULL THEN
        RAISE EXCEPTION 'El token de activacion no existe, ya fue usado o esta vencido.';
    END IF;

    PERFORM set_config('app.usuario_actual', v_id_usuario::TEXT, true);

    UPDATE usuario
       SET contrasena_hash = p_contrasena_hash, estado_cuenta = 'activo'
     WHERE id_usuario = v_id_usuario;

    UPDATE token_activacion SET usado = true WHERE id_token = v_id_token;

    RETURN v_id_usuario;
END;
$$;


CREATE OR REPLACE FUNCTION sp_cambiar_estado_cuenta(
    p_id_superusuario      BIGINT,
    p_id_usuario_objetivo  BIGINT,
    p_nuevo_estado         estado_cuenta_tipo
)
RETURNS VOID
LANGUAGE plpgsql
AS
$$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM usuario WHERE id_usuario = p_id_superusuario AND rol = 'superusuario' AND estado_cuenta = 'activo'
    ) THEN
        RAISE EXCEPTION 'El usuario % no es un superusuario activo.', p_id_superusuario;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM usuario WHERE id_usuario = p_id_usuario_objetivo) THEN
        RAISE EXCEPTION 'El usuario % no existe.', p_id_usuario_objetivo;
    END IF;

    IF p_id_usuario_objetivo = p_id_superusuario AND p_nuevo_estado <> 'activo' THEN
        RAISE EXCEPTION 'Un superusuario no puede cambiar su propio estado a %.', p_nuevo_estado;
    END IF;

    PERFORM set_config('app.usuario_actual', p_id_superusuario::TEXT, true);

    UPDATE usuario SET estado_cuenta = p_nuevo_estado WHERE id_usuario = p_id_usuario_objetivo;
END;
$$;


CREATE OR REPLACE FUNCTION sp_cambiar_estado_pago(
    p_id_administrador   BIGINT,
    p_id_cliente         BIGINT,
    p_nuevo_estado_pago  estado_pago_tipo
)
RETURNS VOID
LANGUAGE plpgsql
AS
$$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM usuario WHERE id_usuario = p_id_administrador AND rol IN ('administrador', 'superusuario') AND estado_cuenta = 'activo'
    ) THEN
        RAISE EXCEPTION 'El usuario % no es un administrador o superusuario activo.', p_id_administrador;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM cliente WHERE id_usuario = p_id_cliente) THEN
        RAISE EXCEPTION 'El cliente % no existe.', p_id_cliente;
    END IF;

    PERFORM set_config('app.usuario_actual', p_id_administrador::TEXT, true);

    UPDATE cliente SET estado_pago = p_nuevo_estado_pago WHERE id_usuario = p_id_cliente;
END;
$$;


CREATE OR REPLACE FUNCTION sp_cambiar_contrasena(
    p_id_usuario             BIGINT,
    p_contrasena_hash_nueva  VARCHAR
)
RETURNS VOID
LANGUAGE plpgsql
AS
$$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM usuario WHERE id_usuario = p_id_usuario AND estado_cuenta = 'activo') THEN
        RAISE EXCEPTION 'El usuario % no existe o no tiene una cuenta activa.', p_id_usuario;
    END IF;

    IF p_contrasena_hash_nueva IS NULL OR btrim(p_contrasena_hash_nueva) = '' THEN
        RAISE EXCEPTION 'La contrasena no puede estar vacia.';
    END IF;

    PERFORM set_config('app.usuario_actual', p_id_usuario::TEXT, true);

    UPDATE usuario SET contrasena_hash = p_contrasena_hash_nueva WHERE id_usuario = p_id_usuario;
END;
$$;


CREATE OR REPLACE FUNCTION sp_restablecer_contrasena(
    p_token             TEXT,
    p_contrasena_hash   VARCHAR
)
RETURNS BIGINT
LANGUAGE plpgsql
AS
$$
DECLARE
    v_id_token   BIGINT;
    v_id_usuario BIGINT;
BEGIN
    IF p_contrasena_hash IS NULL OR btrim(p_contrasena_hash) = '' THEN
        RAISE EXCEPTION 'La contrasena es obligatoria.';
    END IF;

    SELECT id_token, id_usuario INTO v_id_token, v_id_usuario
    FROM token_activacion
    WHERE token = p_token AND tipo_token = 'recuperacion' AND usado = false AND fecha_expiracion > now()
    FOR UPDATE;

    IF v_id_usuario IS NULL THEN
        RAISE EXCEPTION 'El token de recuperacion no existe, ya fue usado o esta vencido.';
    END IF;

    PERFORM set_config('app.usuario_actual', v_id_usuario::TEXT, true);

    UPDATE usuario SET contrasena_hash = p_contrasena_hash WHERE id_usuario = v_id_usuario;

    UPDATE token_activacion SET usado = true WHERE id_token = v_id_token;

    RETURN v_id_usuario;
END;
$$;




SELECT
    u.id_usuario AS id_tecnico,
    u.nombre_usuario,
    u.correo,
    t.nivel,
    t.habilitado
FROM tecnico_grupo tg
JOIN grupo_tecnico gt ON gt.id_grupo = tg.id_grupo
JOIN tecnico t ON t.id_usuario = tg.id_usuario
JOIN usuario u ON u.id_usuario = t.id_usuario
WHERE gt.nombre_grupo ILIKE '%delta%'
ORDER BY u.nombre_usuario;


select * from usuario




--04/09


-- ============================================================
-- 1) NUEVO: auditoría por tablas (operación interna: técnico

CREATE TYPE operacion_auditoria_tipo AS ENUM ('INSERT', 'UPDATE', 'DELETE');

CREATE TABLE auditoria_datos (
    id_auditoria             BIGSERIAL PRIMARY KEY,
    tabla_afectada           VARCHAR(100) NOT NULL,
    operacion                operacion_auditoria_tipo NOT NULL,
    datos_anteriores         JSONB,
    datos_nuevos             JSONB,
    id_usuario_responsable   BIGINT REFERENCES usuario(id_usuario),
    fecha                    TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMENT ON TABLE auditoria_datos IS 'Auditoria de cambios en tablas de operacion interna (tecnico/administrador/superusuario). No registra acciones hechas directamente por el cliente.';

CREATE INDEX idx_auditoria_tabla_fecha ON auditoria_datos (tabla_afectada, fecha DESC);
CREATE INDEX idx_auditoria_usuario     ON auditoria_datos (id_usuario_responsable);

CREATE OR REPLACE FUNCTION fn_auditar_cambio()
RETURNS TRIGGER
LANGUAGE plpgsql
AS
$$
DECLARE
    v_id_usuario_actual BIGINT;
    v_rol_actual        rol_usuario_tipo;
BEGIN
    BEGIN
        v_id_usuario_actual := NULLIF(current_setting('app.usuario_actual', true), '')::BIGINT;
    EXCEPTION
        WHEN OTHERS THEN
            v_id_usuario_actual := NULL;
    END;

    IF v_id_usuario_actual IS NOT NULL THEN
        SELECT rol INTO v_rol_actual FROM usuario WHERE id_usuario = v_id_usuario_actual;

        IF v_rol_actual = 'cliente' THEN
            RETURN COALESCE(NEW, OLD);
        END IF;
    END IF;

    INSERT INTO auditoria_datos
        (tabla_afectada, operacion, datos_anteriores, datos_nuevos, id_usuario_responsable)
    VALUES
        (
            TG_TABLE_NAME,
            TG_OP::operacion_auditoria_tipo,
            CASE WHEN TG_OP IN ('UPDATE', 'DELETE') THEN row_to_json(OLD)::JSONB ELSE NULL END,
            CASE WHEN TG_OP IN ('INSERT', 'UPDATE') THEN row_to_json(NEW)::JSONB ELSE NULL END,
            v_id_usuario_actual
        );

    RETURN COALESCE(NEW, OLD);
END;
$$;

CREATE TRIGGER trg_auditar_usuario
AFTER INSERT OR UPDATE OR DELETE ON usuario
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio();

CREATE TRIGGER trg_auditar_cliente
AFTER INSERT OR UPDATE OR DELETE ON cliente
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio();

CREATE TRIGGER trg_auditar_solicitud
AFTER INSERT OR UPDATE OR DELETE ON solicitud
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio();

CREATE TRIGGER trg_auditar_asignacion_solicitud
AFTER INSERT OR UPDATE OR DELETE ON asignacion_solicitud
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio();

CREATE TRIGGER trg_auditar_reporte_solicitud
AFTER INSERT OR UPDATE OR DELETE ON reporte_solicitud
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio();

CREATE TRIGGER trg_auditar_grupo_tecnico
AFTER INSERT OR UPDATE OR DELETE ON grupo_tecnico
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio();

CREATE TRIGGER trg_auditar_tecnico_grupo
AFTER INSERT OR UPDATE OR DELETE ON tecnico_grupo
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio();


-- ============================================================
-- 2) NUEVO: abrir/cerrar sesión (para que la auditoría de
--    sesiones que ya existía en el script base tenga datos)
-- ============================================================

CREATE OR REPLACE FUNCTION sp_abrir_sesion(
    p_id_usuario BIGINT,
    p_ip_origen  INET DEFAULT NULL
)
RETURNS BIGINT
LANGUAGE plpgsql
AS
$$
DECLARE
    v_id_sesion BIGINT;
BEGIN
    INSERT INTO auditoria_sesion (id_usuario, ip_origen)
    VALUES (p_id_usuario, p_ip_origen)
    RETURNING id_sesion INTO v_id_sesion;

    RETURN v_id_sesion;
END;
$$;

CREATE OR REPLACE FUNCTION sp_cerrar_sesion(
    p_id_sesion BIGINT
)
RETURNS VOID
LANGUAGE plpgsql
AS
$$
BEGIN
    UPDATE auditoria_sesion
       SET fecha_salida = now()
     WHERE id_sesion = p_id_sesion
       AND fecha_salida IS NULL;
END;
$$;



CREATE OR REPLACE FUNCTION sp_crear_solicitud(
    p_id_cliente    BIGINT,
    p_descripcion   TEXT,
    p_id_categoria  INTEGER DEFAULT NULL
)
RETURNS BIGINT
LANGUAGE plpgsql
AS
$$
DECLARE
    v_id_solicitud BIGINT;
    v_id_estado_pendiente INTEGER;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM cliente WHERE id_usuario = p_id_cliente) THEN
        RAISE EXCEPTION 'El cliente % no existe.', p_id_cliente;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM usuario WHERE id_usuario = p_id_cliente AND estado_cuenta = 'activo'
    ) THEN
        RAISE EXCEPTION 'El cliente % no tiene una cuenta activa.', p_id_cliente;
    END IF;

    IF p_descripcion IS NULL OR btrim(p_descripcion) = '' THEN
        RAISE EXCEPTION 'La descripcion no puede estar vacia.';
    END IF;

    IF p_id_categoria IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM categoria WHERE id_categoria = p_id_categoria) THEN
            RAISE EXCEPTION 'La categoria % no existe.', p_id_categoria;
        END IF;
    END IF;

    v_id_estado_pendiente := fn_id_estado('Pendiente');
    IF v_id_estado_pendiente IS NULL THEN
        RAISE EXCEPTION 'El estado "Pendiente" no existe en la tabla estado.';
    END IF;

    PERFORM set_config('app.usuario_actual', p_id_cliente::TEXT, true);

    INSERT INTO solicitud (descripcion, id_cliente, id_categoria, id_estado)
    VALUES (btrim(p_descripcion), p_id_cliente, p_id_categoria, v_id_estado_pendiente)
    RETURNING id_solicitud INTO v_id_solicitud;

    RETURN v_id_solicitud;
END;
$$;


CREATE OR REPLACE FUNCTION sp_enviar_reporte(
    p_id_solicitud     BIGINT,
    p_id_tecnico       BIGINT,
    p_detalle_reporte  TEXT
)
RETURNS BIGINT
LANGUAGE plpgsql
AS
$$
DECLARE
    v_id_reporte       BIGINT;
    v_estado_actual    INTEGER;
    v_tecnico_vigente  BIGINT;
    v_grupo_vigente    BIGINT;
    v_autorizado       BOOLEAN;
BEGIN
    SELECT id_estado INTO v_estado_actual FROM solicitud WHERE id_solicitud = p_id_solicitud FOR UPDATE;

    IF v_estado_actual IS NULL THEN
        RAISE EXCEPTION 'La solicitud % no existe.', p_id_solicitud;
    END IF;

    IF v_estado_actual <> fn_id_estado('En Proceso') THEN
        RAISE EXCEPTION 'Solo se puede enviar un reporte cuando la solicitud está En Proceso.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM tecnico WHERE id_usuario = p_id_tecnico AND habilitado = TRUE) THEN
        RAISE EXCEPTION 'El técnico % no existe o no está habilitado.', p_id_tecnico;
    END IF;

    SELECT id_tecnico, id_grupo INTO v_tecnico_vigente, v_grupo_vigente
    FROM asignacion_solicitud
    WHERE id_solicitud = p_id_solicitud AND vigente = TRUE
    LIMIT 1;

    IF v_tecnico_vigente IS NULL AND v_grupo_vigente IS NULL THEN
        RAISE EXCEPTION 'La solicitud % no tiene una asignación vigente.', p_id_solicitud;
    END IF;

    v_autorizado :=
        (v_tecnico_vigente = p_id_tecnico)
        OR (
            v_grupo_vigente IS NOT NULL
            AND EXISTS (SELECT 1 FROM tecnico_grupo WHERE id_usuario = p_id_tecnico AND id_grupo = v_grupo_vigente)
        );

    IF NOT v_autorizado THEN
        RAISE EXCEPTION 'El técnico % no está autorizado para reportar la solicitud %.', p_id_tecnico, p_id_solicitud;
    END IF;

    IF p_detalle_reporte IS NULL OR btrim(p_detalle_reporte) = '' THEN
        RAISE EXCEPTION 'El detalle del reporte no puede estar vacío.';
    END IF;

    PERFORM set_config('app.usuario_actual', p_id_tecnico::TEXT, true);

    INSERT INTO reporte_solicitud (id_solicitud, id_tecnico, detalle_reporte)
    VALUES (p_id_solicitud, p_id_tecnico, btrim(p_detalle_reporte))
    RETURNING id_reporte INTO v_id_reporte;

    UPDATE solicitud SET id_estado = fn_id_estado('Pendiente Aprobación') WHERE id_solicitud = p_id_solicitud;

    RETURN v_id_reporte;
END;
$$;


CREATE OR REPLACE FUNCTION sp_invitar_usuario(
    p_id_superusuario  BIGINT,
    p_nombre_usuario   VARCHAR,
    p_correo           VARCHAR,
    p_rol              rol_usuario_tipo,
    p_dias_validez     INTEGER DEFAULT 7
)
RETURNS TEXT
LANGUAGE plpgsql
AS
$$
DECLARE
    v_id_usuario BIGINT;
    v_token      TEXT;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM usuario WHERE id_usuario = p_id_superusuario AND rol = 'superusuario' AND estado_cuenta = 'activo'
    ) THEN
        RAISE EXCEPTION 'El usuario % no es un superusuario activo.', p_id_superusuario;
    END IF;

    IF p_nombre_usuario IS NULL OR btrim(p_nombre_usuario) = '' THEN
        RAISE EXCEPTION 'El nombre de usuario no puede estar vacio.';
    END IF;

    IF p_correo IS NULL OR btrim(p_correo) = '' THEN
        RAISE EXCEPTION 'El correo no puede estar vacio.';
    END IF;

    IF EXISTS (SELECT 1 FROM usuario WHERE correo = btrim(p_correo)) THEN
        RAISE EXCEPTION 'Ya existe un usuario con el correo %.', p_correo;
    END IF;

    IF p_dias_validez <= 0 THEN
        RAISE EXCEPTION 'La validez del token debe ser mayor que cero.';
    END IF;

    PERFORM set_config('app.usuario_actual', p_id_superusuario::TEXT, true);

    INSERT INTO usuario (nombre_usuario, correo, rol, estado_cuenta)
    VALUES (btrim(p_nombre_usuario), btrim(p_correo), p_rol, 'inactivo')
    RETURNING id_usuario INTO v_id_usuario;

    IF p_rol = 'cliente' THEN
        INSERT INTO cliente (id_usuario) VALUES (v_id_usuario);
    ELSIF p_rol = 'tecnico' THEN
        INSERT INTO tecnico (id_usuario) VALUES (v_id_usuario);
    END IF;

    v_token := encode(gen_random_bytes(32), 'hex');

    INSERT INTO token_activacion (token, id_usuario, tipo_token, fecha_expiracion)
    VALUES (v_token, v_id_usuario, 'invitacion', now() + make_interval(days => p_dias_validez));

    RETURN v_token;
END;
$$;


CREATE OR REPLACE FUNCTION sp_activar_cuenta(
    p_token             TEXT,
    p_contrasena_hash   VARCHAR
)
RETURNS BIGINT
LANGUAGE plpgsql
AS
$$
DECLARE
    v_id_token   BIGINT;
    v_id_usuario BIGINT;
BEGIN
    IF p_contrasena_hash IS NULL OR btrim(p_contrasena_hash) = '' THEN
        RAISE EXCEPTION 'La contrasena es obligatoria.';
    END IF;

    SELECT id_token, id_usuario INTO v_id_token, v_id_usuario
    FROM token_activacion
    WHERE token = p_token AND tipo_token = 'invitacion' AND usado = false AND fecha_expiracion > now()
    FOR UPDATE;

    IF v_id_usuario IS NULL THEN
        RAISE EXCEPTION 'El token de activacion no existe, ya fue usado o esta vencido.';
    END IF;

    PERFORM set_config('app.usuario_actual', v_id_usuario::TEXT, true);

    UPDATE usuario
       SET contrasena_hash = p_contrasena_hash, estado_cuenta = 'activo'
     WHERE id_usuario = v_id_usuario;

    UPDATE token_activacion SET usado = true WHERE id_token = v_id_token;

    RETURN v_id_usuario;
END;
$$;


CREATE OR REPLACE FUNCTION sp_cambiar_estado_cuenta(
    p_id_superusuario      BIGINT,
    p_id_usuario_objetivo  BIGINT,
    p_nuevo_estado         estado_cuenta_tipo
)
RETURNS VOID
LANGUAGE plpgsql
AS
$$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM usuario WHERE id_usuario = p_id_superusuario AND rol = 'superusuario' AND estado_cuenta = 'activo'
    ) THEN
        RAISE EXCEPTION 'El usuario % no es un superusuario activo.', p_id_superusuario;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM usuario WHERE id_usuario = p_id_usuario_objetivo) THEN
        RAISE EXCEPTION 'El usuario % no existe.', p_id_usuario_objetivo;
    END IF;

    IF p_id_usuario_objetivo = p_id_superusuario AND p_nuevo_estado <> 'activo' THEN
        RAISE EXCEPTION 'Un superusuario no puede cambiar su propio estado a %.', p_nuevo_estado;
    END IF;

    PERFORM set_config('app.usuario_actual', p_id_superusuario::TEXT, true);

    UPDATE usuario SET estado_cuenta = p_nuevo_estado WHERE id_usuario = p_id_usuario_objetivo;
END;
$$;


CREATE OR REPLACE FUNCTION sp_cambiar_estado_pago(
    p_id_administrador   BIGINT,
    p_id_cliente         BIGINT,
    p_nuevo_estado_pago  estado_pago_tipo
)
RETURNS VOID
LANGUAGE plpgsql
AS
$$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM usuario WHERE id_usuario = p_id_administrador AND rol IN ('administrador', 'superusuario') AND estado_cuenta = 'activo'
    ) THEN
        RAISE EXCEPTION 'El usuario % no es un administrador o superusuario activo.', p_id_administrador;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM cliente WHERE id_usuario = p_id_cliente) THEN
        RAISE EXCEPTION 'El cliente % no existe.', p_id_cliente;
    END IF;

    PERFORM set_config('app.usuario_actual', p_id_administrador::TEXT, true);

    UPDATE cliente SET estado_pago = p_nuevo_estado_pago WHERE id_usuario = p_id_cliente;
END;
$$;


CREATE OR REPLACE FUNCTION sp_cambiar_contrasena(
    p_id_usuario             BIGINT,
    p_contrasena_hash_nueva  VARCHAR
)
RETURNS VOID
LANGUAGE plpgsql
AS
$$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM usuario WHERE id_usuario = p_id_usuario AND estado_cuenta = 'activo') THEN
        RAISE EXCEPTION 'El usuario % no existe o no tiene una cuenta activa.', p_id_usuario;
    END IF;

    IF p_contrasena_hash_nueva IS NULL OR btrim(p_contrasena_hash_nueva) = '' THEN
        RAISE EXCEPTION 'La contrasena no puede estar vacia.';
    END IF;

    PERFORM set_config('app.usuario_actual', p_id_usuario::TEXT, true);

    UPDATE usuario SET contrasena_hash = p_contrasena_hash_nueva WHERE id_usuario = p_id_usuario;
END;
$$;


CREATE OR REPLACE FUNCTION sp_restablecer_contrasena(
    p_token             TEXT,
    p_contrasena_hash   VARCHAR
)
RETURNS BIGINT
LANGUAGE plpgsql
AS
$$
DECLARE
    v_id_token   BIGINT;
    v_id_usuario BIGINT;
BEGIN
    IF p_contrasena_hash IS NULL OR btrim(p_contrasena_hash) = '' THEN
        RAISE EXCEPTION 'La contrasena es obligatoria.';
    END IF;

    SELECT id_token, id_usuario INTO v_id_token, v_id_usuario
    FROM token_activacion
    WHERE token = p_token AND tipo_token = 'recuperacion' AND usado = false AND fecha_expiracion > now()
    FOR UPDATE;

    IF v_id_usuario IS NULL THEN
        RAISE EXCEPTION 'El token de recuperacion no existe, ya fue usado o esta vencido.';
    END IF;

    PERFORM set_config('app.usuario_actual', v_id_usuario::TEXT, true);

    UPDATE usuario SET contrasena_hash = p_contrasena_hash WHERE id_usuario = v_id_usuario;

    UPDATE token_activacion SET usado = true WHERE id_token = v_id_token;

    RETURN v_id_usuario;
END;
$$;




SELECT
    u.id_usuario AS id_tecnico,
    u.nombre_usuario,
    u.correo,
    t.nivel,
    t.habilitado
FROM tecnico_grupo tg
JOIN grupo_tecnico gt ON gt.id_grupo = tg.id_grupo
JOIN tecnico t ON t.id_usuario = tg.id_usuario
JOIN usuario u ON u.id_usuario = t.id_usuario
WHERE gt.nombre_grupo ILIKE '%delta%'
ORDER BY u.nombre_usuario;


select * from usuario


--04/09


CREATE OR REPLACE FUNCTION fn_tecnico_tiene_acceso(
    p_id_solicitud BIGINT,
    p_id_tecnico   BIGINT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN EXISTS (
        SELECT 1
        FROM asignacion_solicitud a
        LEFT JOIN tecnico_grupo tg
            ON tg.id_grupo = a.id_grupo
           AND tg.id_usuario = p_id_tecnico
        WHERE a.id_solicitud = p_id_solicitud
          AND a.vigente = true
          AND (a.id_tecnico = p_id_tecnico OR tg.id_usuario = p_id_tecnico)
    );
END;
$$;




CREATE OR REPLACE FUNCTION fn_resumen_tecnico(
    p_id_tecnico BIGINT
)
RETURNS TABLE (
    en_proceso            BIGINT,
    pendiente_aprobacion  BIGINT,
    resueltas_hoy         BIGINT,
    total_cerradas        BIGINT
)
LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN QUERY
    SELECT
        (
            SELECT count(*)
            FROM solicitud s
            JOIN asignacion_solicitud a ON a.id_solicitud = s.id_solicitud AND a.vigente = true
            LEFT JOIN tecnico_grupo tg ON tg.id_grupo = a.id_grupo AND tg.id_usuario = p_id_tecnico
            JOIN estado e ON e.id_estado = s.id_estado
            WHERE (a.id_tecnico = p_id_tecnico OR tg.id_usuario = p_id_tecnico)
              AND e.nombre_estado = 'En Proceso'
        ),
        (
            SELECT count(*)
            FROM solicitud s
            JOIN asignacion_solicitud a ON a.id_solicitud = s.id_solicitud AND a.vigente = true
            LEFT JOIN tecnico_grupo tg ON tg.id_grupo = a.id_grupo AND tg.id_usuario = p_id_tecnico
            JOIN estado e ON e.id_estado = s.id_estado
            WHERE (a.id_tecnico = p_id_tecnico OR tg.id_usuario = p_id_tecnico)
              AND e.nombre_estado = 'Pendiente Aprobación'
        ),
        (
            SELECT count(DISTINCT h.id_solicitud)
            FROM historial_estado h
            JOIN estado e ON e.id_estado = h.estado_nuevo
            JOIN reporte_solicitud r ON r.id_solicitud = h.id_solicitud AND r.id_tecnico = p_id_tecnico
            WHERE e.nombre_estado = 'Cerrada'
              AND h.fecha_cambio >= date_trunc('day', now())
        ),
        (
            SELECT count(DISTINCT h.id_solicitud)
            FROM historial_estado h
            JOIN estado e ON e.id_estado = h.estado_nuevo
            JOIN reporte_solicitud r ON r.id_solicitud = h.id_solicitud AND r.id_tecnico = p_id_tecnico
            WHERE e.nombre_estado = 'Cerrada'
        );
END;
$$;


CREATE OR REPLACE FUNCTION fn_resumen_auditoria()
RETURNS TABLE (
    sesiones_activas  BIGINT,
    cambios_hoy       BIGINT,
    inserts           BIGINT,
    updates           BIGINT,
    eliminaciones     BIGINT,
    acciones_sistema  BIGINT
)
LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN QUERY
    SELECT
        (SELECT count(*) FROM auditoria_sesion WHERE fecha_salida IS NULL),
        (SELECT count(*) FROM auditoria_datos WHERE fecha >= date_trunc('day', now())),
        (SELECT count(*) FROM auditoria_datos WHERE operacion = 'INSERT' AND fecha >= date_trunc('day', now())),
        (SELECT count(*) FROM auditoria_datos WHERE operacion = 'UPDATE' AND fecha >= date_trunc('day', now())),
        (SELECT count(*) FROM auditoria_datos WHERE operacion = 'DELETE' AND fecha >= date_trunc('day', now())),
        (SELECT count(*) FROM auditoria_datos WHERE id_usuario_responsable IS NULL AND fecha >= date_trunc('day', now()));
END;
$$;


CREATE OR REPLACE FUNCTION fn_buscar_usuarios(
    p_termino VARCHAR
)
RETURNS TABLE (
    id_usuario      BIGINT,
    nombre_usuario  VARCHAR,
    correo          VARCHAR,
    rol             VARCHAR
)
LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN QUERY
    SELECT u.id_usuario, u.nombre_usuario, u.correo, u.rol::VARCHAR
    FROM usuario u
    WHERE u.nombre_usuario ILIKE '%' || p_termino || '%'
       OR u.correo ILIKE '%' || p_termino || '%'
    ORDER BY u.nombre_usuario
    LIMIT 8;
END;
$$;



-------

-- 1) Nueva columna: direccion del servicio para ESTE ticket (no el perfil)
ALTER TABLE solicitud ADD COLUMN direccion VARCHAR(255);
COMMENT ON COLUMN solicitud.direccion IS 'Direccion del servicio para ESTE ticket (no la del perfil del cliente - un cliente puede tener mas de una propiedad).';

-- 2) tecnico nunca tuvo trigger de auditoria (solo tecnico_grupo lo tenia)
CREATE TRIGGER trg_auditar_tecnico
AFTER INSERT OR UPDATE OR DELETE ON tecnico
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio();

-- 3) sp_crear_solicitud ahora exige/gestiona direccion
DROP FUNCTION IF EXISTS sp_crear_solicitud(BIGINT, TEXT, INTEGER);

CREATE OR REPLACE FUNCTION sp_crear_solicitud(
    p_id_cliente    BIGINT,
    p_descripcion   TEXT,
    p_id_categoria  INTEGER DEFAULT NULL,
    p_direccion     VARCHAR DEFAULT NULL
)
RETURNS BIGINT
LANGUAGE plpgsql
AS
$$
DECLARE
    v_id_solicitud BIGINT;
    v_id_estado_pendiente INTEGER;
    v_direccion_final VARCHAR;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM cliente WHERE id_usuario = p_id_cliente) THEN
        RAISE EXCEPTION 'El cliente % no existe.', p_id_cliente;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM usuario WHERE id_usuario = p_id_cliente AND estado_cuenta = 'activo') THEN
        RAISE EXCEPTION 'El cliente % no tiene una cuenta activa.', p_id_cliente;
    END IF;

    IF p_descripcion IS NULL OR btrim(p_descripcion) = '' THEN
        RAISE EXCEPTION 'La descripcion no puede estar vacia.';
    END IF;

    IF p_direccion IS NOT NULL AND btrim(p_direccion) <> '' THEN
        v_direccion_final := btrim(p_direccion);
    ELSE
        SELECT direccion INTO v_direccion_final FROM cliente WHERE id_usuario = p_id_cliente;
    END IF;

    IF v_direccion_final IS NULL OR btrim(v_direccion_final) = '' THEN
        RAISE EXCEPTION 'La direccion no puede estar vacia.';
    END IF;

    IF p_id_categoria IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM categoria WHERE id_categoria = p_id_categoria) THEN
            RAISE EXCEPTION 'La categoria % no existe.', p_id_categoria;
        END IF;
    END IF;

    v_id_estado_pendiente := fn_id_estado('Pendiente');
    IF v_id_estado_pendiente IS NULL THEN
        RAISE EXCEPTION 'El estado "Pendiente" no existe en la tabla estado.';
    END IF;

    PERFORM set_config('app.usuario_actual', p_id_cliente::TEXT, true);

    UPDATE cliente SET direccion = v_direccion_final WHERE id_usuario = p_id_cliente;

    INSERT INTO solicitud (descripcion, id_cliente, id_categoria, id_estado, direccion)
    VALUES (btrim(p_descripcion), p_id_cliente, p_id_categoria, v_id_estado_pendiente, v_direccion_final)
    RETURNING id_solicitud INTO v_id_solicitud;

    RETURN v_id_solicitud;
END;
$$;

-- 4) Nuevo: editar especialidad/nivel del tecnico (solo Superusuario)
CREATE OR REPLACE FUNCTION sp_editar_perfil_tecnico(
    p_id_superusuario  BIGINT,
    p_id_tecnico       BIGINT,
    p_especialidad     VARCHAR,
    p_nivel            nivel_tecnico_tipo
)
RETURNS VOID
LANGUAGE plpgsql
AS
$$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM usuario WHERE id_usuario = p_id_superusuario AND rol = 'superusuario' AND estado_cuenta = 'activo') THEN
        RAISE EXCEPTION 'El usuario % no es un superusuario activo.', p_id_superusuario;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM tecnico WHERE id_usuario = p_id_tecnico) THEN
        RAISE EXCEPTION 'El tecnico % no existe.', p_id_tecnico;
    END IF;

    IF p_nivel IS NULL THEN
        RAISE EXCEPTION 'El nivel es obligatorio.';
    END IF;

    PERFORM set_config('app.usuario_actual', p_id_superusuario::TEXT, true);

    UPDATE tecnico
       SET especialidad = NULLIF(btrim(p_especialidad), ''),
           nivel        = p_nivel
     WHERE id_usuario = p_id_tecnico;
END;
$$;


----

-- 1) fn_buscar_usuarios ahora acepta filtrar por rol (para el buscador de técnicos)
DROP FUNCTION IF EXISTS fn_buscar_usuarios(VARCHAR);

CREATE OR REPLACE FUNCTION fn_buscar_usuarios(
    p_termino VARCHAR,
    p_rol     VARCHAR DEFAULT NULL
)
RETURNS TABLE (
    id_usuario      BIGINT,
    nombre_usuario  VARCHAR,
    correo          VARCHAR,
    rol             VARCHAR
)
LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN QUERY
    SELECT u.id_usuario, u.nombre_usuario, u.correo, u.rol::VARCHAR
    FROM usuario u
    WHERE (u.nombre_usuario ILIKE '%' || p_termino || '%'
       OR u.correo ILIKE '%' || p_termino || '%')
      AND (p_rol IS NULL OR u.rol::VARCHAR = p_rol)
    ORDER BY u.nombre_usuario
    LIMIT 8;
END;
$$;

-- 2) Nuevo: cada grupo con cuantos tecnicos tiene
CREATE OR REPLACE FUNCTION fn_grupos_tecnicos_con_conteo()
RETURNS TABLE (
    id_grupo        BIGINT,
    nombre_grupo    VARCHAR,
    total_tecnicos  BIGINT
)
LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN QUERY
    SELECT g.id_grupo, g.nombre_grupo, COUNT(tg.id_usuario)
    FROM grupo_tecnico g
    LEFT JOIN tecnico_grupo tg ON tg.id_grupo = g.id_grupo
    GROUP BY g.id_grupo, g.nombre_grupo
    ORDER BY g.nombre_grupo;
END;
$$;


--05/09


CREATE TABLE IF NOT EXISTS configuracion_sistema (
    id_configuracion         SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id_configuracion = 1),
    nombre_negocio           VARCHAR(150) NOT NULL DEFAULT 'SoporteNet',
    logo_url                 VARCHAR(500),
    color_primario           VARCHAR(7),
    id_administrador_modifico BIGINT REFERENCES usuario(id_usuario),
    fecha_modificacion       TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMENT ON TABLE configuracion_sistema IS 'Configuracion global de marca (nombre/logo/color) - fila unica, editable por el Superusuario. Publica sin login porque hasta la pantalla de login la necesita.';

INSERT INTO configuracion_sistema (id_configuracion)
VALUES (1)
ON CONFLICT (id_configuracion) DO NOTHING;



DROP FUNCTION IF EXISTS fn_obtener_configuracion_sistema();

CREATE OR REPLACE FUNCTION fn_obtener_configuracion_sistema()
RETURNS TABLE (
    nombre_negocio      VARCHAR,
    categoria           VARCHAR,
    eslogan             VARCHAR,
    logo_url            VARCHAR,
    color_primario      VARCHAR,
    fecha_modificacion  TIMESTAMPTZ
)
LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN QUERY
    SELECT c.nombre_negocio, c.categoria, c.eslogan, c.logo_url, c.color_primario, c.fecha_modificacion
    FROM configuracion_sistema c
    WHERE c.id_configuracion = 1;
END;
$$;

DROP FUNCTION IF EXISTS sp_actualizar_configuracion_sistema(BIGINT, VARCHAR, VARCHAR);

CREATE OR REPLACE FUNCTION sp_actualizar_configuracion_sistema(
    p_id_superusuario  BIGINT,
    p_nombre_negocio   VARCHAR,
    p_categoria        VARCHAR,
    p_eslogan          VARCHAR,
    p_color_primario   VARCHAR
)
RETURNS VOID
LANGUAGE plpgsql
AS
$$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM usuario
        WHERE id_usuario = p_id_superusuario
          AND rol = 'superusuario'
          AND estado_cuenta = 'activo'
    ) THEN
        RAISE EXCEPTION 'El usuario % no es un superusuario activo.', p_id_superusuario;
    END IF;

    IF p_nombre_negocio IS NULL OR btrim(p_nombre_negocio) = '' THEN
        RAISE EXCEPTION 'El nombre del negocio no puede estar vacio.';
    END IF;

    IF p_categoria IS NULL OR btrim(p_categoria) = '' THEN
        RAISE EXCEPTION 'La categoria no puede estar vacia.';
    END IF;

    IF p_eslogan IS NULL OR btrim(p_eslogan) = '' THEN
        RAISE EXCEPTION 'El eslogan no puede estar vacio.';
    END IF;

    IF p_color_primario IS NOT NULL AND p_color_primario !~ '^#[0-9A-Fa-f]{6}$' THEN
        RAISE EXCEPTION 'El color debe ser un codigo hexadecimal valido (ej: #0d9488).';
    END IF;

    PERFORM set_config('app.usuario_actual', p_id_superusuario::TEXT, true);

    UPDATE configuracion_sistema
       SET nombre_negocio = btrim(p_nombre_negocio),
           categoria = btrim(p_categoria),
           eslogan = btrim(p_eslogan),
           color_primario = p_color_primario,
           id_administrador_modifico = p_id_superusuario,
           fecha_modificacion = now()
     WHERE id_configuracion = 1;
END;
$$;



CREATE OR REPLACE FUNCTION sp_actualizar_logo_sistema(
    p_id_superusuario  BIGINT,
    p_logo_url         VARCHAR
)
RETURNS VOID
LANGUAGE plpgsql
AS
$$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM usuario
        WHERE id_usuario = p_id_superusuario
          AND rol = 'superusuario'
          AND estado_cuenta = 'activo'
    ) THEN
        RAISE EXCEPTION 'El usuario % no es un superusuario activo.', p_id_superusuario;
    END IF;

    PERFORM set_config('app.usuario_actual', p_id_superusuario::TEXT, true);

    UPDATE configuracion_sistema
       SET logo_url = p_logo_url,
           id_administrador_modifico = p_id_superusuario,
           fecha_modificacion = now()
     WHERE id_configuracion = 1;
END;
$$;

CREATE TRIGGER trg_auditar_configuracion_sistema
AFTER INSERT OR UPDATE ON configuracion_sistema
FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio();


----------------------------------------




ALTER TABLE configuracion_sistema ADD COLUMN IF NOT EXISTS categoria VARCHAR(150) NOT NULL DEFAULT 'Soporte Técnico · Internet ISP';
ALTER TABLE configuracion_sistema ADD COLUMN IF NOT EXISTS eslogan VARCHAR(300) NOT NULL DEFAULT 'Del primer reporte a la solución confirmada, sin perder el hilo de ningún ticket.';

CREATE OR REPLACE FUNCTION fn_todas_ordenado_admin()
RETURNS SETOF solicitud
LANGUAGE sql STABLE
AS $$
    SELECT s.* FROM solicitud s
    ORDER BY s.id_prioridad DESC NULLS FIRST, s.fecha_creacion ASC;
$$;

CREATE OR REPLACE FUNCTION fn_todas_ordenado_admin_conteo()
RETURNS BIGINT
LANGUAGE sql STABLE
AS $$
    SELECT count(*) FROM solicitud;
$$;

CREATE OR REPLACE FUNCTION fn_por_estado_ordenado_admin(p_estado VARCHAR)
RETURNS SETOF solicitud
LANGUAGE sql STABLE
AS $$
    SELECT s.* FROM solicitud s
    LEFT JOIN estado e ON e.id_estado = s.id_estado
    WHERE e.nombre_estado = p_estado
    ORDER BY s.id_prioridad DESC NULLS FIRST, s.fecha_creacion ASC;
$$;

CREATE OR REPLACE FUNCTION fn_por_estado_ordenado_admin_conteo(p_estado VARCHAR)
RETURNS BIGINT
LANGUAGE sql STABLE
AS $$
    SELECT count(*) FROM solicitud s
    LEFT JOIN estado e ON e.id_estado = s.id_estado
    WHERE e.nombre_estado = p_estado;
$$;

CREATE OR REPLACE FUNCTION fn_mis_tareas(p_id_tecnico BIGINT, p_estado VARCHAR)
RETURNS SETOF solicitud
LANGUAGE sql STABLE
AS $$
    SELECT s.* FROM (
        SELECT a.id_solicitud FROM asignacion_solicitud a
        WHERE a.id_tecnico = p_id_tecnico AND a.vigente = true
        UNION
        SELECT a.id_solicitud FROM asignacion_solicitud a
        JOIN tecnico_grupo tg ON tg.id_grupo = a.id_grupo AND tg.id_usuario = p_id_tecnico
        WHERE a.vigente = true
    ) mis_asignaciones
    JOIN solicitud s ON s.id_solicitud = mis_asignaciones.id_solicitud
    LEFT JOIN prioridad p ON p.id_prioridad = s.id_prioridad
    LEFT JOIN estado e ON e.id_estado = s.id_estado
    WHERE (p_estado IS NULL OR e.nombre_estado = p_estado)
    ORDER BY COALESCE(p.orden, 0) DESC, s.fecha_creacion ASC;
$$;

CREATE OR REPLACE FUNCTION fn_mis_tareas_conteo(p_id_tecnico BIGINT, p_estado VARCHAR)
RETURNS BIGINT
LANGUAGE sql STABLE
AS $$
    SELECT count(*) FROM (
        SELECT a.id_solicitud FROM asignacion_solicitud a
        WHERE a.id_tecnico = p_id_tecnico AND a.vigente = true
        UNION
        SELECT a.id_solicitud FROM asignacion_solicitud a
        JOIN tecnico_grupo tg ON tg.id_grupo = a.id_grupo AND tg.id_usuario = p_id_tecnico
        WHERE a.vigente = true
    ) mis_asignaciones
    JOIN solicitud s ON s.id_solicitud = mis_asignaciones.id_solicitud
    LEFT JOIN estado e ON e.id_estado = s.id_estado
    WHERE (p_estado IS NULL OR e.nombre_estado = p_estado);
$$;

CREATE OR REPLACE FUNCTION fn_conteo_solicitudes_estado_cliente(p_id_cliente BIGINT)
RETURNS TABLE(etiqueta VARCHAR, valor BIGINT)
LANGUAGE sql STABLE
AS $$
    SELECT e.nombre_estado, count(s.id_solicitud)
    FROM estado e
    LEFT JOIN solicitud s ON s.id_estado = e.id_estado AND s.id_cliente = p_id_cliente
    GROUP BY e.id_estado, e.nombre_estado
    ORDER BY e.id_estado;
$$;

CREATE OR REPLACE FUNCTION fn_conteo_solicitudes_categoria_cliente(p_id_cliente BIGINT)
RETURNS TABLE(etiqueta VARCHAR, valor BIGINT)
LANGUAGE sql STABLE
AS $$
    SELECT COALESCE(c.nombre_categoria, 'Sin categoría'::varchar), count(*)
    FROM solicitud s
    LEFT JOIN categoria c ON c.id_categoria = s.id_categoria
    WHERE s.id_cliente = p_id_cliente
    GROUP BY COALESCE(c.nombre_categoria, 'Sin categoría'::varchar)
    ORDER BY 2 DESC;
$$;

CREATE OR REPLACE FUNCTION fn_conteo_solicitudes_prioridad_tecnico(p_id_tecnico BIGINT)
RETURNS TABLE(etiqueta VARCHAR, valor BIGINT)
LANGUAGE sql STABLE
AS $$
    SELECT p.nombre_prioridad, count(s.id_solicitud)
    FROM prioridad p
    LEFT JOIN solicitud s ON s.id_prioridad = p.id_prioridad
        AND s.id_solicitud IN (
            SELECT a.id_solicitud FROM asignacion_solicitud a
            WHERE a.id_tecnico = p_id_tecnico AND a.vigente = true
            UNION
            SELECT a.id_solicitud FROM asignacion_solicitud a
            JOIN tecnico_grupo tg ON tg.id_grupo = a.id_grupo AND tg.id_usuario = p_id_tecnico
            WHERE a.vigente = true
        )
    GROUP BY p.id_prioridad, p.nombre_prioridad
    ORDER BY p.orden;
$$;

CREATE OR REPLACE FUNCTION fn_conteo_solicitudes_estado()
RETURNS TABLE(etiqueta VARCHAR, valor BIGINT)
LANGUAGE sql STABLE
AS $$
    SELECT e.nombre_estado, count(s.id_solicitud)
    FROM estado e
    LEFT JOIN solicitud s ON s.id_estado = e.id_estado
    GROUP BY e.id_estado, e.nombre_estado
    ORDER BY e.id_estado;
$$;

CREATE OR REPLACE FUNCTION fn_conteo_solicitudes_prioridad()
RETURNS TABLE(etiqueta VARCHAR, valor BIGINT)
LANGUAGE sql STABLE
AS $$
    SELECT etiqueta, valor FROM (
        SELECT p.orden AS orden_fila, p.nombre_prioridad AS etiqueta, count(s.id_solicitud) AS valor
        FROM prioridad p
        LEFT JOIN solicitud s ON s.id_prioridad = p.id_prioridad
        GROUP BY p.id_prioridad, p.nombre_prioridad, p.orden
        UNION ALL
        SELECT -1, 'Sin asignar'::varchar, count(*) FROM solicitud WHERE id_prioridad IS NULL
    ) t ORDER BY orden_fila;
$$;

CREATE OR REPLACE FUNCTION fn_conteo_solicitudes_categoria()
RETURNS TABLE(etiqueta VARCHAR, valor BIGINT)
LANGUAGE sql STABLE
AS $$
    SELECT etiqueta, valor FROM (
        SELECT c.nombre_categoria AS etiqueta, count(s.id_solicitud) AS valor
        FROM categoria c
        LEFT JOIN solicitud s ON s.id_categoria = c.id_categoria
        GROUP BY c.id_categoria, c.nombre_categoria
        UNION ALL
        SELECT 'Sin categoría'::varchar, count(*) FROM solicitud WHERE id_categoria IS NULL
    ) t ORDER BY valor DESC;
$$;

-- ---------- ReporteSolicitudRepository ----------

CREATE OR REPLACE FUNCTION fn_conteo_aprobacion_tecnico(p_id_tecnico BIGINT)
RETURNS TABLE(etiqueta VARCHAR, valor BIGINT)
LANGUAGE sql STABLE
AS $$
    SELECT v.etiqueta, count(r.id_reporte)
    FROM (VALUES ('aprobado','Aprobados'), ('rechazado','Rechazados')) AS v(clave, etiqueta)
    LEFT JOIN reporte_solicitud r ON r.estado_aprobacion::text = v.clave AND r.id_tecnico = p_id_tecnico
    GROUP BY v.etiqueta, v.clave
    ORDER BY v.clave;
$$;

CREATE OR REPLACE FUNCTION fn_conteo_aprobacion()
RETURNS TABLE(etiqueta VARCHAR, valor BIGINT)
LANGUAGE sql STABLE
AS $$
    SELECT v.etiqueta, count(r.id_reporte)
    FROM (VALUES ('aprobado','Aprobados'), ('rechazado','Rechazados')) AS v(clave, etiqueta)
    LEFT JOIN reporte_solicitud r ON r.estado_aprobacion::text = v.clave
    GROUP BY v.etiqueta, v.clave
    ORDER BY v.clave;
$$;

-- ---------- TecnicoRepository ----------

CREATE OR REPLACE FUNCTION fn_conteo_tecnicos_nivel()
RETURNS TABLE(etiqueta VARCHAR, valor BIGINT)
LANGUAGE sql STABLE
AS $$
    SELECT v.etiqueta, count(t.id_usuario)
    FROM (VALUES ('junior','Junior'), ('intermedio','Intermedio'), ('senior','Senior')) AS v(clave, etiqueta)
    LEFT JOIN tecnico t ON t.nivel::text = v.clave
    GROUP BY v.etiqueta, v.clave
    ORDER BY v.clave;
$$;

CREATE OR REPLACE FUNCTION fn_conteo_carga_trabajo_tecnicos()
RETURNS TABLE(etiqueta VARCHAR, valor BIGINT)
LANGUAGE sql STABLE
AS $$
    SELECT etiqueta, count(*) FROM (
        SELECT
            CASE
                WHEN carga.total IS NULL OR carga.total = 0 THEN 1
                WHEN carga.total <= 2 THEN 2
                WHEN carga.total <= 5 THEN 3
                ELSE 4
            END AS orden_fila,
            CASE
                WHEN carga.total IS NULL OR carga.total = 0 THEN 'Libre'
                WHEN carga.total <= 2 THEN 'Carga baja'
                WHEN carga.total <= 5 THEN 'Carga media'
                ELSE 'Sobrecargado'
            END AS etiqueta
        FROM tecnico t
        LEFT JOIN (
            SELECT id_tecnico, count(*) AS total FROM asignacion_solicitud
            WHERE vigente = true AND id_tecnico IS NOT NULL
            GROUP BY id_tecnico
        ) carga ON carga.id_tecnico = t.id_usuario
        WHERE t.habilitado = true
    ) clasificados
    GROUP BY orden_fila, etiqueta
    ORDER BY orden_fila;
$$;

-- ---------- UsuarioRepository ----------

CREATE OR REPLACE FUNCTION fn_conteo_usuarios_rol()
RETURNS TABLE(etiqueta VARCHAR, valor BIGINT)
LANGUAGE sql STABLE
AS $$
    SELECT v.etiqueta, count(u.id_usuario)
    FROM (VALUES ('cliente','Clientes'), ('tecnico','Técnicos'), ('administrador','Administradores'), ('superusuario','Superusuarios')) AS v(clave, etiqueta)
    LEFT JOIN usuario u ON u.rol::text = v.clave
    GROUP BY v.etiqueta, v.clave
    ORDER BY v.clave;
$$;

-- ---------- AdjuntoRepository ----------

CREATE OR REPLACE FUNCTION fn_puede_acceder_a_solicitud(p_id_solicitud BIGINT, p_id_usuario BIGINT)
RETURNS BOOLEAN
LANGUAGE sql STABLE
AS $$
    SELECT EXISTS (
        SELECT 1 FROM solicitud WHERE id_solicitud = p_id_solicitud AND id_cliente = p_id_usuario
        UNION ALL
        SELECT 1 FROM asignacion_solicitud WHERE id_solicitud = p_id_solicitud AND vigente = true AND id_tecnico = p_id_usuario
        UNION ALL
        SELECT 1 FROM asignacion_solicitud a JOIN tecnico_grupo tg ON tg.id_grupo = a.id_grupo
        WHERE a.id_solicitud = p_id_solicitud AND a.vigente = true AND tg.id_usuario = p_id_usuario
    );
$$;

-- ---------- GrupoTecnicoRepository (eran escrituras, van como sp_) ----------

CREATE OR REPLACE FUNCTION sp_agregar_miembro_grupo(p_id_tecnico BIGINT, p_id_grupo BIGINT)
RETURNS VOID
LANGUAGE sql
AS $$
    INSERT INTO tecnico_grupo (id_usuario, id_grupo) VALUES (p_id_tecnico, p_id_grupo);
$$;

CREATE OR REPLACE FUNCTION sp_quitar_miembro_grupo(p_id_tecnico BIGINT, p_id_grupo BIGINT)
RETURNS VOID
LANGUAGE sql
AS $$
    DELETE FROM tecnico_grupo WHERE id_usuario = p_id_tecnico AND id_grupo = p_id_grupo;
$$;











