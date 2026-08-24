-- ============================================================================
-- SISTEMA DE SOPORTE TÉCNICO (ISP)
-- Script DDL completo para PostgreSQL — tablas, constraints, índices,
-- triggers y procedimientos almacenados.
--
-- Cubre:
--   - Ciclo de vida del ticket con doble validación (técnica + cliente)
--   - Reasignación con historial completo (nunca se sobrescribe)
--   - Bloqueo de edición de tickets cerrados (trigger + excepción administrativa)
--   - Concurrencia optimista (columna version, estilo @Version de JPA)
--   - Índices pensados para >1 millón de registros (sin sembrar el millón aún)
--
-- Probado end-to-end en PostgreSQL 16.
-- Orden de ejecución: de arriba hacia abajo, un solo archivo.
-- ============================================================================
-- ============================================================================
-- SISTEMA DE SOPORTE TÉCNICO (ISP) — SCRIPT DDL POSTGRESQL
-- ============================================================================
-- Basado en el análisis funcional: roles, ciclo de vida del ticket (con
-- confirmación del cliente y reapertura), reasignación con historial,
-- bloqueo de edición de tickets cerrados, concurrencia optimista,
-- e índices pensados para >1 millón de registros.
-- Probado en PostgreSQL 16.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0. EXTENSIONES
-- ----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pgcrypto; -- gen_random_bytes() para tokens

-- ----------------------------------------------------------------------------
-- 1. TIPOS ENUMERADOS
-- ----------------------------------------------------------------------------
CREATE TYPE rol_usuario_tipo        AS ENUM ('cliente', 'tecnico', 'administrador', 'superusuario');
CREATE TYPE estado_cuenta_tipo      AS ENUM ('activo', 'suspendido', 'inactivo');
CREATE TYPE nivel_tecnico_tipo      AS ENUM ('junior', 'intermedio', 'senior');
CREATE TYPE estado_aprobacion_tipo  AS ENUM ('pendiente', 'aprobado', 'rechazado');
CREATE TYPE tipo_token_tipo         AS ENUM ('invitacion', 'recuperacion');
CREATE TYPE estado_pago_tipo        AS ENUM ('al_dia', 'moroso'); -- ver 7.4, alcance opcional

-- ----------------------------------------------------------------------------
-- 2. TABLAS DE CATÁLOGO (lookup tables — bajo volumen, alta lectura)
-- ----------------------------------------------------------------------------

CREATE TABLE categoria (
    id_categoria     SERIAL PRIMARY KEY,
    nombre_categoria VARCHAR(100) NOT NULL UNIQUE
);
COMMENT ON TABLE categoria IS 'Tipo de problema reportado por el cliente.';

CREATE TABLE prioridad (
    id_prioridad     SERIAL PRIMARY KEY,
    nombre_prioridad VARCHAR(20) NOT NULL UNIQUE
        CHECK (nombre_prioridad IN ('Baja', 'Normal', 'Alta', 'Urgente')),
    orden            SMALLINT NOT NULL UNIQUE -- 1=Baja ... 4=Urgente, útil para ORDER BY
);
COMMENT ON TABLE prioridad IS 'Nivel de urgencia de una solicitud.';

CREATE TABLE estado (
    id_estado    SERIAL PRIMARY KEY,
    nombre_estado VARCHAR(60) NOT NULL UNIQUE
        CHECK (nombre_estado IN (
            'Pendiente',
            'En Proceso',
            'Pendiente Aprobación',
            'Resuelta - Pendiente Confirmación del Cliente',
            'Cerrada'
        ))
);
COMMENT ON TABLE estado IS 'Fase actual del ciclo de vida del ticket (sección 3).';

-- ----------------------------------------------------------------------------
-- 3. USUARIOS Y ROLES (una tabla base + tablas de extensión por rol)
-- ----------------------------------------------------------------------------

CREATE TABLE usuario (
    id_usuario       BIGSERIAL PRIMARY KEY,
    nombre_usuario   VARCHAR(150) NOT NULL,
    correo           VARCHAR(255) NOT NULL UNIQUE,
    contrasena_hash  VARCHAR(255),              -- NULL hasta que el usuario activa su cuenta
    rol              rol_usuario_tipo NOT NULL,
    estado_cuenta    estado_cuenta_tipo NOT NULL DEFAULT 'inactivo',
    fecha_creacion   TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMENT ON TABLE usuario IS 'Tabla base para los cuatro roles. Nunca se elimina, solo cambia estado_cuenta.';
COMMENT ON COLUMN usuario.contrasena_hash IS 'Hash de la contraseña (nunca texto plano). NULL hasta activar cuenta vía invitación.';

CREATE TABLE cliente (
    id_usuario   BIGINT PRIMARY KEY REFERENCES usuario(id_usuario),
    direccion    VARCHAR(255),
    estado_pago  estado_pago_tipo NOT NULL DEFAULT 'al_dia' -- opcional, ver 7.4
);
COMMENT ON TABLE cliente IS 'Extiende usuario con datos propios del cliente (PK compartida con usuario).';

CREATE TABLE grupo_tecnico (
    id_grupo     BIGSERIAL PRIMARY KEY,
    nombre_grupo VARCHAR(100) NOT NULL UNIQUE
);
COMMENT ON TABLE grupo_tecnico IS 'Agrupación de técnicos administrada por el superusuario.';

CREATE TABLE tecnico (
    id_usuario    BIGINT PRIMARY KEY REFERENCES usuario(id_usuario),
    especialidad  VARCHAR(150),
    nivel         nivel_tecnico_tipo NOT NULL DEFAULT 'junior',
    habilitado    BOOLEAN NOT NULL DEFAULT true
);
COMMENT ON TABLE tecnico IS 'Extiende usuario con datos del personal técnico. nivel se usa para sugerir aptitud (sección 5.3).';

CREATE TABLE tecnico_grupo (
    id_usuario     BIGINT NOT NULL REFERENCES tecnico(id_usuario),
    id_grupo       BIGINT NOT NULL REFERENCES grupo_tecnico(id_grupo),
    fecha_ingreso  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id_usuario, id_grupo)
);
COMMENT ON TABLE tecnico_grupo IS 'Relación N:M — un técnico puede pertenecer a varios grupos.';
-- ----------------------------------------------------------------------------
-- 4. SOLICITUD (entidad central del sistema)
-- ----------------------------------------------------------------------------

CREATE TABLE solicitud (
    id_solicitud              BIGSERIAL PRIMARY KEY,
    descripcion               TEXT NOT NULL,
    fecha_creacion             TIMESTAMPTZ NOT NULL DEFAULT now(),
    id_cliente                BIGINT NOT NULL REFERENCES cliente(id_usuario),
    id_categoria               INTEGER REFERENCES categoria(id_categoria),
    id_prioridad               INTEGER REFERENCES prioridad(id_prioridad),
    id_estado                  INTEGER NOT NULL REFERENCES estado(id_estado),

    -- Sugerencias del modelo de clasificación por IA (sección 9.1) — el
    -- administrador las confirma o cambia; no se aplican automáticamente.
    categoria_sugerida_ia      INTEGER REFERENCES categoria(id_categoria),
    prioridad_sugerida_ia      INTEGER REFERENCES prioridad(id_prioridad),
    complejidad_sugerida_ia    nivel_tecnico_tipo,

    -- Plazo para que el cliente confirme la solución (sección 3.1); se llena
    -- solo mientras el ticket está en "Resuelta - Pendiente Confirmación".
    fecha_limite_confirmacion  TIMESTAMPTZ,

    -- Bloqueo optimista (sección 7.2) — equivalente a @Version de JPA.
    version                    INTEGER NOT NULL DEFAULT 0
);

COMMENT ON TABLE solicitud IS 'Ticket de soporte: entidad central del sistema.';
COMMENT ON COLUMN solicitud.version IS 'Incrementada automáticamente en cada UPDATE (ver trigger). La app debe incluir WHERE version = <valor leído> para detectar conflictos de concurrencia.';
COMMENT ON COLUMN solicitud.fecha_limite_confirmacion IS 'Si vence sin respuesta del cliente, sp_cierre_automatico_por_vencimiento cierra el ticket.';

-- ----------------------------------------------------------------------------
-- 5. HISTORIAL DE ESTADO (auditoría de cada transición — sección 5.4)
-- ----------------------------------------------------------------------------

CREATE TABLE historial_estado (
    id_historial            BIGSERIAL PRIMARY KEY,
    id_solicitud            BIGINT NOT NULL REFERENCES solicitud(id_solicitud),
    estado_anterior         INTEGER REFERENCES estado(id_estado), -- NULL en la creación
    estado_nuevo            INTEGER NOT NULL REFERENCES estado(id_estado),
    fecha_cambio            TIMESTAMPTZ NOT NULL DEFAULT now(),
    id_usuario_responsable  BIGINT REFERENCES usuario(id_usuario) -- NULL = acción automática del sistema
);
COMMENT ON TABLE historial_estado IS 'Tabla de auditoría append-only; nunca se actualiza ni se borra.';

-- ----------------------------------------------------------------------------
-- 6. ASIGNACIÓN DE SOLICITUDES (historial de asignaciones/reasignaciones — 3.2)
-- ----------------------------------------------------------------------------

CREATE TABLE asignacion_solicitud (
    id_asignacion         BIGSERIAL PRIMARY KEY,
    id_solicitud          BIGINT NOT NULL REFERENCES solicitud(id_solicitud),
    id_tecnico            BIGINT REFERENCES tecnico(id_usuario),
    id_grupo              BIGINT REFERENCES grupo_tecnico(id_grupo),
    fecha_asignacion      TIMESTAMPTZ NOT NULL DEFAULT now(),
    motivo_reasignacion   TEXT,                 -- NULL solo en la asignación inicial
    es_reasignacion       BOOLEAN NOT NULL DEFAULT false,
    id_usuario_asigna     BIGINT REFERENCES usuario(id_usuario),

    -- Marca la fila "activa" para esta solicitud; mantenida por trigger.
    -- Permite responder rápido "¿quién tiene este ticket ahora?" sin escanear
    -- todo el historial, clave para el panel "Mis tareas" a 1M+ filas.
    vigente               BOOLEAN NOT NULL DEFAULT true,

    CONSTRAINT chk_asignacion_destino CHECK (
        (id_tecnico IS NOT NULL AND id_grupo IS NULL) OR
        (id_tecnico IS NULL AND id_grupo IS NOT NULL)
    ),
    CONSTRAINT chk_motivo_reasignacion CHECK (
        es_reasignacion = false OR motivo_reasignacion IS NOT NULL
    )
);
COMMENT ON TABLE asignacion_solicitud IS 'Historial completo de asignaciones; nunca se sobrescribe una fila anterior (append-only).';

-- ----------------------------------------------------------------------------
-- 7. ADJUNTOS (solo referencia; el binario nunca vive en la base — sección 6.1)
-- ----------------------------------------------------------------------------

CREATE TABLE adjunto (
    id_adjunto          BIGSERIAL PRIMARY KEY,
    id_solicitud        BIGINT NOT NULL REFERENCES solicitud(id_solicitud),
    nombre_archivo      VARCHAR(255) NOT NULL,
    tipo_archivo        VARCHAR(50) NOT NULL,
    tamano_archivo      BIGINT NOT NULL CHECK (tamano_archivo > 0 AND tamano_archivo <= 10485760), -- límite 10MB
    url_almacenamiento  VARCHAR(500) NOT NULL,
    fecha_subida        TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMENT ON COLUMN adjunto.url_almacenamiento IS 'Ruta en disco o en almacenamiento de objetos (S3 o equivalente). El binario nunca se guarda en PostgreSQL.';

-- ----------------------------------------------------------------------------
-- 8. REPORTE DE SOLUCIÓN
-- ----------------------------------------------------------------------------

CREATE TABLE reporte_solicitud (
    id_reporte               BIGSERIAL PRIMARY KEY,
    id_solicitud              BIGINT NOT NULL REFERENCES solicitud(id_solicitud),
    id_tecnico                BIGINT NOT NULL REFERENCES tecnico(id_usuario),
    detalle_reporte           TEXT NOT NULL,
    estado_aprobacion         estado_aprobacion_tipo NOT NULL DEFAULT 'pendiente',
    fecha_envio               TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_revision            TIMESTAMPTZ,
    id_administrador_revisa   BIGINT REFERENCES usuario(id_usuario),
    comentario_rechazo        TEXT
);
COMMENT ON TABLE reporte_solicitud IS 'El técnico no puede cerrar el ticket por sí mismo; requiere aprobación (sección 2.2, 2.3).';
-- ----------------------------------------------------------------------------
-- 9. ANUNCIOS GLOBALES
-- ----------------------------------------------------------------------------

CREATE TABLE anuncio (
    id_anuncio         BIGSERIAL PRIMARY KEY,
    titulo             VARCHAR(200) NOT NULL,
    mensaje            TEXT NOT NULL,
    fecha_creacion     TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_expiracion   TIMESTAMPTZ,
    esta_activo        BOOLEAN NOT NULL DEFAULT true,
    id_administrador   BIGINT REFERENCES usuario(id_usuario)
);

-- ----------------------------------------------------------------------------
-- 10. TOKENS DE ACTIVACIÓN / RECUPERACIÓN
-- ----------------------------------------------------------------------------

CREATE TABLE token_activacion (
    id_token          BIGSERIAL PRIMARY KEY,
    token             VARCHAR(255) NOT NULL UNIQUE,
    id_usuario        BIGINT NOT NULL REFERENCES usuario(id_usuario),
    tipo_token        tipo_token_tipo NOT NULL,
    fecha_creacion    TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_expiracion  TIMESTAMPTZ NOT NULL,
    usado             BOOLEAN NOT NULL DEFAULT false
);
COMMENT ON TABLE token_activacion IS 'Token de un solo uso; el superusuario nunca crea contraseñas directamente (sección 2.4).';

-- ----------------------------------------------------------------------------
-- 11. AUDITORÍA DE SESIONES
-- ----------------------------------------------------------------------------

CREATE TABLE auditoria_sesion (
    id_sesion         BIGSERIAL PRIMARY KEY,
    id_usuario        BIGINT NOT NULL REFERENCES usuario(id_usuario),
    fecha_entrada     TIMESTAMPTZ NOT NULL DEFAULT now(),
    ultima_actividad  TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_salida      TIMESTAMPTZ,
    ip_origen         INET
);

-- ----------------------------------------------------------------------------
-- 12. NOTIFICACIONES (sección 8)
-- ----------------------------------------------------------------------------

CREATE TABLE notificacion (
    id_notificacion          BIGSERIAL PRIMARY KEY,
    id_usuario_destino        BIGINT NOT NULL REFERENCES usuario(id_usuario),
    tipo_evento               VARCHAR(100) NOT NULL,
    mensaje                    TEXT NOT NULL,
    fecha_envio                TIMESTAMPTZ NOT NULL DEFAULT now(),
    leido                      BOOLEAN NOT NULL DEFAULT false,
    id_solicitud_relacionada   BIGINT REFERENCES solicitud(id_solicitud)
);

-- ----------------------------------------------------------------------------
-- 13. CHAT INTERNO ENTRE TÉCNICOS (extensión opcional — sección 7.5)
-- ----------------------------------------------------------------------------

CREATE TABLE mensaje_grupo (
    id_mensaje    BIGSERIAL PRIMARY KEY,
    id_grupo      BIGINT NOT NULL REFERENCES grupo_tecnico(id_grupo),
    id_usuario    BIGINT NOT NULL REFERENCES usuario(id_usuario),
    contenido     TEXT NOT NULL,
    fecha         TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMENT ON TABLE mensaje_grupo IS 'Extensión opcional (7.5). Evaluar solo tras cerrar el flujo principal.';
-- ----------------------------------------------------------------------------
-- 14. ÍNDICES DE RENDIMIENTO (sección 6.2 — pensado para 1M+ registros)
-- ----------------------------------------------------------------------------

-- Columnas más filtradas en solicitud
CREATE INDEX idx_solicitud_cliente   ON solicitud (id_cliente);
CREATE INDEX idx_solicitud_estado    ON solicitud (id_estado);
CREATE INDEX idx_solicitud_prioridad ON solicitud (id_prioridad);

-- Índice compuesto para el panel del administrador (filtra por estado Y ordena/filtra por prioridad)
CREATE INDEX idx_solicitud_estado_prioridad ON solicitud (id_estado, id_prioridad);

-- Reportes por rango de fechas
CREATE INDEX idx_solicitud_fecha_creacion ON solicitud (fecha_creacion);

-- Cierre automático: localizar rápidamente los tickets vencidos sin escanear toda la tabla
CREATE INDEX idx_solicitud_fecha_limite ON solicitud (fecha_limite_confirmacion)
    WHERE fecha_limite_confirmacion IS NOT NULL;

-- Asignaciones: historial completo por solicitud
CREATE INDEX idx_asignacion_solicitud ON asignacion_solicitud (id_solicitud);

-- "Mis tareas" del técnico / del grupo — solo la asignación vigente importa en el 99% de las consultas,
-- por eso el índice es parcial (mucho más liviano que indexar todo el historial).
CREATE INDEX idx_asignacion_tecnico_vigente
    ON asignacion_solicitud (id_tecnico)
    WHERE vigente = true AND id_tecnico IS NOT NULL;

CREATE INDEX idx_asignacion_grupo_vigente
    ON asignacion_solicitud (id_grupo)
    WHERE vigente = true AND id_grupo IS NOT NULL;

-- Historial de estado por ticket (pantalla de detalle / trazabilidad)
CREATE INDEX idx_historial_solicitud ON historial_estado (id_solicitud);

-- Notificaciones no leídas por usuario (bandeja)
CREATE INDEX idx_notificacion_usuario_leido ON notificacion (id_usuario_destino, leido);

-- Reportes pendientes de aprobar (dashboard del administrador, sección 4.3.2)
CREATE INDEX idx_reporte_estado ON reporte_solicitud (estado_aprobacion)
    WHERE estado_aprobacion = 'pendiente';

CREATE INDEX idx_reporte_solicitud ON reporte_solicitud (id_solicitud);

-- Tokens válidos (activación / recuperación)
CREATE INDEX idx_token_usuario ON token_activacion (id_usuario);
CREATE UNIQUE INDEX idx_token_no_usado ON token_activacion (token) WHERE usado = false;

-- Sesiones activas (fecha_salida NULL) — auditoría del superusuario
CREATE INDEX idx_sesion_usuario ON auditoria_sesion (id_usuario);
CREATE INDEX idx_sesion_activa  ON auditoria_sesion (id_usuario) WHERE fecha_salida IS NULL;

-- Adjuntos por ticket
CREATE INDEX idx_adjunto_solicitud ON adjunto (id_solicitud);

-- Mensajes de grupo, orden cronológico
CREATE INDEX idx_mensaje_grupo_grupo_fecha ON mensaje_grupo (id_grupo, fecha);

--------------------------------------------------------------------------

INSERT INTO prioridad (nombre_prioridad, orden) VALUES
    ('Baja', 1),
    ('Normal', 2),
    ('Alta', 3),
    ('Urgente', 4);

INSERT INTO estado (nombre_estado) VALUES
    ('Pendiente'),
    ('En Proceso'),
    ('Pendiente Aprobación'),
    ('Resuelta - Pendiente Confirmación del Cliente'),
    ('Cerrada');

-- Categorías iniciales de ejemplo (ajustar según el dominio real del ISP)
INSERT INTO categoria (nombre_categoria) VALUES
    ('Sin conexión'),
    ('Lentitud del servicio'),
    ('Facturación'),
    ('Configuración de equipo'),
    ('Corte por mantenimiento'),
    ('Otro');

-- ============================================================================
-- 16. FUNCIONES, TRIGGERS Y PROCEDIMIENTOS ALMACENADOS
-- ============================================================================



/*==============================================================*/
/*              FUNCION: OBTENER ID DE ESTADO                   */
/*==============================================================*/

CREATE OR REPLACE FUNCTION fn_id_estado(
    p_nombre_estado VARCHAR
)
RETURNS INTEGER

LANGUAGE sql
STABLE

AS
$$

    SELECT id_estado

    FROM estado

    WHERE UPPER(BTRIM(nombre_estado))
        = UPPER(BTRIM(p_nombre_estado));

$$;


/*==============================================================*/
/*         FUNCION PREVIA A UPDATE DE SOLICITUD                 */
/*==============================================================*/

CREATE OR REPLACE FUNCTION fn_pre_update_solicitud()

RETURNS TRIGGER

LANGUAGE plpgsql

AS
$$

DECLARE

    v_estado_cerrada INTEGER;

    v_bypass_cierre TEXT;

BEGIN

    --------------------------------------------------------
    -- Obtener el estado CERRADA
    --------------------------------------------------------

    v_estado_cerrada := fn_id_estado('Cerrada');


    --------------------------------------------------------
    -- Obtener permiso interno de reapertura
    --------------------------------------------------------

    v_bypass_cierre := current_setting(
        'app.bypass_cierre',
        true
    );


    --------------------------------------------------------
    -- Bloquear modificaciones de solicitudes cerradas
    --------------------------------------------------------

    IF OLD.id_estado = v_estado_cerrada

       AND COALESCE(v_bypass_cierre, 'false') <> 'true'

    THEN

        RAISE EXCEPTION
            'La solicitud % está cerrada y no puede modificarse.',
            OLD.id_solicitud

        USING ERRCODE = '23514';

    END IF;


    --------------------------------------------------------
    -- Control de concurrencia optimista
    --------------------------------------------------------

    IF NEW.version <> OLD.version THEN

        RAISE EXCEPTION
            'Conflicto de concurrencia. Otro usuario modificó la solicitud %.',
            OLD.id_solicitud

        USING ERRCODE = '40001';

    END IF;


    --------------------------------------------------------
    -- Incrementar automáticamente la versión
    --------------------------------------------------------

    NEW.version := OLD.version + 1;


    --------------------------------------------------------
    -- Retornar registro actualizado
    --------------------------------------------------------

    RETURN NEW;

END;

$$;


/*==============================================================*/
/*          FUNCION: REGISTRAR HISTORIAL DE ESTADO              */
/*==============================================================*/

CREATE OR REPLACE FUNCTION fn_registrar_historial_estado()

RETURNS TRIGGER

LANGUAGE plpgsql

AS
$$

DECLARE

    v_usuario_actual BIGINT;

BEGIN

    --------------------------------------------------------
    -- Solo registrar cuando realmente cambia el estado
    --------------------------------------------------------

    IF NEW.id_estado IS DISTINCT FROM OLD.id_estado THEN

        ----------------------------------------------------
        -- Obtener usuario responsable
        ----------------------------------------------------

        BEGIN

            v_usuario_actual :=
                NULLIF(
                    current_setting(
                        'app.usuario_actual',
                        true
                    ),
                    ''
                )::BIGINT;

        EXCEPTION

            WHEN OTHERS THEN

                v_usuario_actual := NULL;

        END;

        ----------------------------------------------------
        -- Registrar cambio de estado
        ----------------------------------------------------

        INSERT INTO historial_estado
        (
            id_solicitud,
            estado_anterior,
            estado_nuevo,
            id_usuario_responsable
        )

        VALUES
        (
            NEW.id_solicitud,
            OLD.id_estado,
            NEW.id_estado,
            v_usuario_actual
        );

    END IF;

    RETURN NEW;

END;

$$;


/*==============================================================*/
/*        FUNCION: REGISTRAR HISTORIAL DE CREACION              */
/*==============================================================*/

CREATE OR REPLACE FUNCTION fn_registrar_historial_creacion()

RETURNS TRIGGER

LANGUAGE plpgsql

AS
$$

BEGIN

    --------------------------------------------------------
    -- Registrar estado inicial de la solicitud
    --------------------------------------------------------

    INSERT INTO historial_estado
    (
        id_solicitud,
        estado_anterior,
        estado_nuevo,
        id_usuario_responsable
    )

    VALUES
    (
        NEW.id_solicitud,
        NULL,
        NEW.id_estado,
        NEW.id_cliente
    );

    RETURN NEW;

END;

$$;



/*==============================================================*/
/*       FUNCION: ACTUALIZAR VIGENCIA DE ASIGNACION             */
/*==============================================================*/

CREATE OR REPLACE FUNCTION fn_actualizar_vigencia_asignacion()

RETURNS TRIGGER

LANGUAGE plpgsql

AS
$$

BEGIN

    --------------------------------------------------------
    -- Desactivar asignaciones anteriores
    --------------------------------------------------------

    UPDATE asignacion_solicitud

       SET vigente = FALSE

     WHERE id_solicitud = NEW.id_solicitud

       AND vigente = TRUE;

    --------------------------------------------------------
    -- La nueva asignacion queda vigente
    -- mediante el INSERT que disparo este trigger
    --------------------------------------------------------

    RETURN NEW;

END;

$$;




/*==============================================================*/
/*          FUNCION: ACTUALIZAR ACTIVIDAD DE SESION             */
/*==============================================================*/

CREATE OR REPLACE FUNCTION fn_tocar_actividad_sesion(
    p_id_sesion BIGINT
)

RETURNS VOID

LANGUAGE plpgsql

AS
$$

BEGIN

    UPDATE auditoria_sesion

       SET ultima_actividad = CURRENT_TIMESTAMP

     WHERE id_sesion = p_id_sesion;

END;

$$;




/*==============================================================*/
/*         FUNCION: MARCAR NOTIFICACION COMO LEIDA              */
/*==============================================================*/

CREATE OR REPLACE FUNCTION fn_marcar_notificacion_leida(
    p_id_notificacion BIGINT
)

RETURNS VOID

LANGUAGE plpgsql

AS
$$

BEGIN

    UPDATE notificacion

       SET leida = TRUE

     WHERE id_notificacion = p_id_notificacion;

END;

$$;




-------------------------------------------------------------------------------------------------------------
-------------------------------------------------------------------------------------------------------------
-------------------------------------------------------------------------------------------------------------

/*==============================================================*/
/*         TRIGGER PRE UPDATE SOLICITUD                         */
/*==============================================================*/
--Bloqueo de edición de solicitudes cerradas

DROP TRIGGER IF EXISTS trg_a_bloquear_edicion_cerrada
ON solicitud;

DROP TRIGGER IF EXISTS trg_b_incrementar_version
ON solicitud;

CREATE TRIGGER trg_pre_update_solicitud

BEFORE UPDATE

ON solicitud

FOR EACH ROW

EXECUTE FUNCTION fn_pre_update_solicitud();


COMMENT ON TRIGGER trg_pre_update_solicitud

ON solicitud

IS

'Centraliza las validaciones previas a una actualizacion de la solicitud: bloqueo de tickets cerrados, control de concurrencia, incremento de version y fecha de actualizacion.';



/*==============================================================*/
/*       TRIGGER HISTORIAL DE CAMBIO DE ESTADO                  */
/*==============================================================*/
--Historial de cambios de estado

DROP TRIGGER IF EXISTS trg_c_registrar_historial_estado
ON solicitud;

CREATE TRIGGER trg_c_registrar_historial_estado

AFTER UPDATE

ON solicitud

FOR EACH ROW

EXECUTE FUNCTION fn_registrar_historial_estado();


COMMENT ON TRIGGER trg_c_registrar_historial_estado

ON solicitud

IS

'Registra automaticamente cada cambio de estado de una solicitud en HistorialEstado.';



/*==============================================================*/
/*       TRIGGER HISTORIAL DE CREACION DE SOLICITUD             */
/*==============================================================*/
--Cuando se crea una solicitud, el flujo comienza en Pendiente; posteriormente el ticket 
--pasa por los demás estados del ciclo de vida

DROP TRIGGER IF EXISTS trg_registrar_historial_creacion
ON solicitud;

CREATE TRIGGER trg_registrar_historial_creacion

AFTER INSERT

ON solicitud

FOR EACH ROW

EXECUTE FUNCTION fn_registrar_historial_creacion();


COMMENT ON TRIGGER trg_registrar_historial_creacion

ON solicitud

IS

'Registra automaticamente el estado inicial de una nueva solicitud en HistorialEstado.';




/*==============================================================*/
/*       TRIGGER ACTUALIZAR VIGENCIA DE ASIGNACION              */
/*==============================================================*/
--permite múltiples filas para conservar el historial de asignaciones y 
--reasignaciones, en lugar de sobrescribir la anterior.

DROP TRIGGER IF EXISTS trg_actualizar_vigencia_asignacion
ON asignacion_solicitud;

CREATE TRIGGER trg_actualizar_vigencia_asignacion

BEFORE INSERT

ON asignacion_solicitud

FOR EACH ROW

EXECUTE FUNCTION fn_actualizar_vigencia_asignacion();


COMMENT ON TRIGGER trg_actualizar_vigencia_asignacion

ON asignacion_solicitud

IS

'Desactiva la asignacion vigente anterior cuando se registra una nueva asignacion o reasignacion.';



-------------------------------------------------------------------------------------------------------------
-------------------------------------------------------------------------------------------------------------
---------------------------------------------------------------------------------------------------------------
--PROCEDURES


/*==============================================================*/
/*              PROCEDURE: CREAR SOLICITUD                      */
/*==============================================================*/

CREATE OR REPLACE FUNCTION sp_crear_solicitud(
    p_id_cliente    BIGINT,
    p_descripcion   TEXT,
    p_id_categoria  INTEGER DEFAULT NULL,
    p_id_prioridad  INTEGER DEFAULT NULL
)
RETURNS BIGINT

LANGUAGE plpgsql

AS
$$

DECLARE

    v_id_solicitud BIGINT;
    v_id_estado_pendiente INTEGER;

BEGIN

    --------------------------------------------------------
    -- Validar que el cliente exista
    --------------------------------------------------------

    IF NOT EXISTS (
        SELECT 1
        FROM cliente
        WHERE id_usuario = p_id_cliente
    ) THEN

        RAISE EXCEPTION
            'El cliente % no existe.',
            p_id_cliente;

    END IF;


    --------------------------------------------------------
    -- Validar que la cuenta del cliente este activa
    --------------------------------------------------------

    IF NOT EXISTS (
        SELECT 1
        FROM usuario
        WHERE id_usuario = p_id_cliente
          AND estado_cuenta = 'activo'
    ) THEN

        RAISE EXCEPTION
            'El cliente % no tiene una cuenta activa.',
            p_id_cliente;

    END IF;


    --------------------------------------------------------
    -- Validar descripcion
    --------------------------------------------------------

    IF p_descripcion IS NULL
       OR btrim(p_descripcion) = ''
    THEN

        RAISE EXCEPTION
            'La descripcion no puede estar vacia.';

    END IF;


    --------------------------------------------------------
    -- Validar categoria cuando se proporciona
    --------------------------------------------------------

    IF p_id_categoria IS NOT NULL THEN

        IF NOT EXISTS (
            SELECT 1
            FROM categoria
            WHERE id_categoria = p_id_categoria
        ) THEN

            RAISE EXCEPTION
                'La categoria % no existe.',
                p_id_categoria;

        END IF;

    END IF;


    --------------------------------------------------------
    -- Validar prioridad cuando se proporciona
    --------------------------------------------------------

    IF p_id_prioridad IS NOT NULL THEN

        IF NOT EXISTS (
            SELECT 1
            FROM prioridad
            WHERE id_prioridad = p_id_prioridad
        ) THEN

            RAISE EXCEPTION
                'La prioridad % no existe.',
                p_id_prioridad;

        END IF;

    END IF;


    --------------------------------------------------------
    -- Obtener estado inicial: PENDIENTE
    --------------------------------------------------------

    v_id_estado_pendiente := fn_id_estado('Pendiente');

    IF v_id_estado_pendiente IS NULL THEN

        RAISE EXCEPTION
            'El estado "Pendiente" no existe en la tabla estado.';

    END IF;


    --------------------------------------------------------
    -- Crear solicitud
    --------------------------------------------------------

    INSERT INTO solicitud
    (
        descripcion,
        id_cliente,
        id_categoria,
        id_prioridad,
        id_estado
    )

    VALUES
    (
        btrim(p_descripcion),
        p_id_cliente,
        p_id_categoria,
        p_id_prioridad,
        v_id_estado_pendiente
    )

    RETURNING id_solicitud
    INTO v_id_solicitud;


    --------------------------------------------------------
    -- Registrar usuario responsable para auditoria
    --------------------------------------------------------

    PERFORM set_config(
        'app.usuario_actual',
        p_id_cliente::TEXT,
        true
    );


    --------------------------------------------------------
    -- Retornar ID de la solicitud creada
    --------------------------------------------------------

    RETURN v_id_solicitud;

END;

$$;




/*==============================================================*/
/*              PROCEDURE: ASIGNAR SOLICITUD                    */
/*==============================================================*/

CREATE OR REPLACE FUNCTION sp_asignar_solicitud(
    p_id_solicitud         BIGINT,
    p_id_administrador     BIGINT,
    p_id_tecnico           BIGINT DEFAULT NULL,
    p_id_grupo             BIGINT DEFAULT NULL,
    p_id_prioridad         INTEGER DEFAULT NULL,
    p_motivo_reasignacion  TEXT DEFAULT NULL
)
RETURNS VOID

LANGUAGE plpgsql

AS
$$

DECLARE

    v_estado_actual       INTEGER;
    v_es_reasignacion     BOOLEAN;
    v_id_estado_cerrada   INTEGER;

BEGIN

    --------------------------------------------------------
    -- Validar que se indique exactamente tecnico o grupo
    --------------------------------------------------------

    IF (p_id_tecnico IS NULL) = (p_id_grupo IS NULL) THEN

        RAISE EXCEPTION
            'Debe indicar exactamente un tecnico o un grupo tecnico (no ambos, no ninguno).';

    END IF;


    --------------------------------------------------------
    -- Validar administrador
    --------------------------------------------------------

    IF NOT EXISTS (
        SELECT 1
        FROM usuario
        WHERE id_usuario = p_id_administrador
          AND rol = 'administrador'
          AND estado_cuenta = 'activo'
    ) THEN

        RAISE EXCEPTION
            'El usuario % no es un administrador activo.',
            p_id_administrador;

    END IF;


    --------------------------------------------------------
    -- Obtener solicitud y bloquearla
    --------------------------------------------------------

    SELECT id_estado

    INTO v_estado_actual

    FROM solicitud

    WHERE id_solicitud = p_id_solicitud

    FOR UPDATE;


    --------------------------------------------------------
    -- Validar existencia de solicitud
    --------------------------------------------------------

    IF v_estado_actual IS NULL THEN

        RAISE EXCEPTION
            'La solicitud % no existe.',
            p_id_solicitud;

    END IF;


    --------------------------------------------------------
    -- Obtener estado cerrada
    --------------------------------------------------------

    v_id_estado_cerrada := fn_id_estado('Cerrada');


    --------------------------------------------------------
    -- No permitir asignar solicitudes cerradas
    --------------------------------------------------------

    IF v_estado_actual = v_id_estado_cerrada THEN

        RAISE EXCEPTION
            'No se puede asignar una solicitud Cerrada.';

    END IF;


    --------------------------------------------------------
    -- Validar tecnico
    --------------------------------------------------------

    IF p_id_tecnico IS NOT NULL THEN

        IF NOT EXISTS (
            SELECT 1
            FROM tecnico
            WHERE id_usuario = p_id_tecnico
              AND habilitado = TRUE
        ) THEN

            RAISE EXCEPTION
                'El tecnico % no existe o no esta habilitado.',
                p_id_tecnico;

        END IF;

    END IF;


    --------------------------------------------------------
    -- Validar grupo
    --------------------------------------------------------

    IF p_id_grupo IS NOT NULL THEN

        IF NOT EXISTS (
            SELECT 1
            FROM grupo_tecnico
            WHERE id_grupo = p_id_grupo
        ) THEN

            RAISE EXCEPTION
                'El grupo tecnico % no existe.',
                p_id_grupo;

        END IF;

    END IF;


    --------------------------------------------------------
    -- Validar prioridad
    --------------------------------------------------------

    IF p_id_prioridad IS NOT NULL THEN

        IF NOT EXISTS (
            SELECT 1
            FROM prioridad
            WHERE id_prioridad = p_id_prioridad
        ) THEN

            RAISE EXCEPTION
                'La prioridad % no existe.',
                p_id_prioridad;

        END IF;

    END IF;


    --------------------------------------------------------
    -- Determinar si es asignacion o reasignacion
    --------------------------------------------------------

    v_es_reasignacion := EXISTS (
        SELECT 1
        FROM asignacion_solicitud
        WHERE id_solicitud = p_id_solicitud
    );


    --------------------------------------------------------
    -- Validar motivo de reasignacion
    --------------------------------------------------------

    IF v_es_reasignacion
       AND btrim(coalesce(p_motivo_reasignacion, '')) = ''
    THEN

        RAISE EXCEPTION
            'Debe indicar un motivo al reasignar una solicitud ya asignada.';

    END IF;


    --------------------------------------------------------
    -- Registrar usuario responsable
    --------------------------------------------------------

    PERFORM set_config(
        'app.usuario_actual',
        p_id_administrador::TEXT,
        true
    );


    --------------------------------------------------------
    -- Registrar asignacion
    --------------------------------------------------------

    INSERT INTO asignacion_solicitud
    (
        id_solicitud,
        id_tecnico,
        id_grupo,
        motivo_reasignacion,
        es_reasignacion,
        id_usuario_asigna
    )

    VALUES
    (
        p_id_solicitud,
        p_id_tecnico,
        p_id_grupo,
        CASE
            WHEN v_es_reasignacion
            THEN btrim(p_motivo_reasignacion)
            ELSE NULL
        END,
        v_es_reasignacion,
        p_id_administrador
    );


    --------------------------------------------------------
    -- Cambiar prioridad y estado
    --------------------------------------------------------

    UPDATE solicitud

    SET id_prioridad = COALESCE(
            p_id_prioridad,
            id_prioridad
        ),

        id_estado = fn_id_estado('En Proceso')

    WHERE id_solicitud = p_id_solicitud;


    --------------------------------------------------------
    -- Notificar al tecnico cuando la asignacion es directa
    --------------------------------------------------------

    IF p_id_tecnico IS NOT NULL THEN

        INSERT INTO notificacion
        (
            id_usuario_destino,
            tipo_evento,
            mensaje,
            id_solicitud_relacionada
        )

        VALUES
        (
            p_id_tecnico,

            CASE
                WHEN v_es_reasignacion
                THEN 'ticket_reasignado'
                ELSE 'ticket_asignado'
            END,

            CASE
                WHEN v_es_reasignacion
                THEN format(
                    'Se le ha reasignado la solicitud #%s.',
                    p_id_solicitud
                )
                ELSE format(
                    'Se le ha asignado la solicitud #%s.',
                    p_id_solicitud
                )
            END,

            p_id_solicitud
        );

    END IF;

END;

$$;




/*==============================================================*/
/*              PROCEDURE: ENVIAR REPORTE                       */
/*==============================================================*/
--La regla de negocio dice que el técnico únicamente puede enviar el reporte cuando está trabajando la solicitud, 
--y al enviarlo pasa a Pendiente Aprobación.

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

    --------------------------------------------------------
    -- Validar solicitud y bloquearla
    --------------------------------------------------------

    SELECT id_estado

    INTO v_estado_actual

    FROM solicitud

    WHERE id_solicitud = p_id_solicitud

    FOR UPDATE;


    IF v_estado_actual IS NULL THEN

        RAISE EXCEPTION
            'La solicitud % no existe.',
            p_id_solicitud;

    END IF;


    --------------------------------------------------------
    -- Solo se puede reportar en En Proceso
    --------------------------------------------------------

    IF v_estado_actual <> fn_id_estado('En Proceso') THEN

        RAISE EXCEPTION
            'Solo se puede enviar un reporte cuando la solicitud está En Proceso.';

    END IF;


    --------------------------------------------------------
    -- Validar técnico habilitado
    --------------------------------------------------------

    IF NOT EXISTS (
        SELECT 1
        FROM tecnico
        WHERE id_usuario = p_id_tecnico
          AND habilitado = TRUE
    ) THEN

        RAISE EXCEPTION
            'El técnico % no existe o no está habilitado.',
            p_id_tecnico;

    END IF;


    --------------------------------------------------------
    -- Obtener asignación vigente
    --------------------------------------------------------

    SELECT id_tecnico,
           id_grupo

    INTO v_tecnico_vigente,
         v_grupo_vigente

    FROM asignacion_solicitud

    WHERE id_solicitud = p_id_solicitud

      AND vigente = TRUE

    LIMIT 1;


    IF v_tecnico_vigente IS NULL
       AND v_grupo_vigente IS NULL
    THEN

        RAISE EXCEPTION
            'La solicitud % no tiene una asignación vigente.',
            p_id_solicitud;

    END IF;


    --------------------------------------------------------
    -- Verificar autorización del técnico
    --------------------------------------------------------

    v_autorizado :=
        (v_tecnico_vigente = p_id_tecnico)

        OR

        (
            v_grupo_vigente IS NOT NULL

            AND EXISTS (
                SELECT 1

                FROM tecnico_grupo

                WHERE id_usuario = p_id_tecnico

                  AND id_grupo = v_grupo_vigente
            )
        );


    IF NOT v_autorizado THEN

        RAISE EXCEPTION
            'El técnico % no está autorizado para reportar la solicitud %.',
            p_id_tecnico,
            p_id_solicitud;

    END IF;


    --------------------------------------------------------
    -- Validar detalle del reporte
    --------------------------------------------------------

    IF p_detalle_reporte IS NULL
       OR btrim(p_detalle_reporte) = ''
    THEN

        RAISE EXCEPTION
            'El detalle del reporte no puede estar vacío.';

    END IF;


    --------------------------------------------------------
    -- Crear reporte
    --------------------------------------------------------

    INSERT INTO reporte_solicitud
    (
        id_solicitud,
        id_tecnico,
        detalle_reporte
    )

    VALUES
    (
        p_id_solicitud,
        p_id_tecnico,
        btrim(p_detalle_reporte)
    )

    RETURNING id_reporte

    INTO v_id_reporte;


    --------------------------------------------------------
    -- Registrar usuario responsable
    --------------------------------------------------------

    PERFORM set_config(
        'app.usuario_actual',
        p_id_tecnico::TEXT,
        true
    );


    --------------------------------------------------------
    -- Pasar solicitud a Pendiente Aprobación
    --------------------------------------------------------

    UPDATE solicitud

       SET id_estado = fn_id_estado('Pendiente Aprobación')

     WHERE id_solicitud = p_id_solicitud;


    --------------------------------------------------------
    -- Retornar reporte creado
    --------------------------------------------------------

    RETURN v_id_reporte;

END;

$$;




/*==============================================================*/
/*              PROCEDURE: APROBAR REPORTE                      */
/*==============================================================*/
--El Administrador es quien revisa y aprueba el reporte, pero aprobar no significa cerrar inmediatamente. Debe pasar a:
--Resuelta - Pendiente Confirmación del Cliente

CREATE OR REPLACE FUNCTION sp_aprobar_reporte(
    p_id_reporte              BIGINT,
    p_id_administrador        BIGINT,
    p_dias_plazo_confirmacion INTEGER DEFAULT 3
)
RETURNS VOID

LANGUAGE plpgsql

AS
$$

DECLARE

    v_id_solicitud BIGINT;
    v_id_cliente   BIGINT;

BEGIN

    --------------------------------------------------------
    -- Validar administrador activo
    --------------------------------------------------------

    IF NOT EXISTS (
        SELECT 1
        FROM usuario
        WHERE id_usuario = p_id_administrador
          AND rol = 'administrador'
          AND estado_cuenta = 'activo'
    ) THEN

        RAISE EXCEPTION
            'El usuario % no es un administrador activo.',
            p_id_administrador;

    END IF;


    --------------------------------------------------------
    -- Validar plazo
    --------------------------------------------------------

    IF p_dias_plazo_confirmacion <= 0 THEN

        RAISE EXCEPTION
            'El plazo de confirmación debe ser mayor que cero.';

    END IF;


    --------------------------------------------------------
    -- Obtener reporte pendiente y bloquearlo
    --------------------------------------------------------

    SELECT id_solicitud

    INTO v_id_solicitud

    FROM reporte_solicitud

    WHERE id_reporte = p_id_reporte

      AND estado_aprobacion = 'pendiente'

    FOR UPDATE;


    IF v_id_solicitud IS NULL THEN

        RAISE EXCEPTION
            'El reporte % no existe o ya fue revisado.',
            p_id_reporte;

    END IF;


    --------------------------------------------------------
    -- Obtener cliente de la solicitud
    --------------------------------------------------------

    SELECT id_cliente

    INTO v_id_cliente

    FROM solicitud

    WHERE id_solicitud = v_id_solicitud

    FOR UPDATE;


    IF v_id_cliente IS NULL THEN

        RAISE EXCEPTION
            'La solicitud asociada al reporte no existe.';

    END IF;


    --------------------------------------------------------
    -- Registrar administrador responsable
    --------------------------------------------------------

    PERFORM set_config(
        'app.usuario_actual',
        p_id_administrador::TEXT,
        true
    );


    --------------------------------------------------------
    -- Aprobar reporte
    --------------------------------------------------------

    UPDATE reporte_solicitud

       SET estado_aprobacion = 'aprobado',
           fecha_revision = CURRENT_TIMESTAMP,
           id_administrador_revisa = p_id_administrador

     WHERE id_reporte = p_id_reporte;


    --------------------------------------------------------
    -- Pasar solicitud a confirmación del cliente
    --------------------------------------------------------

    UPDATE solicitud

       SET id_estado =
               fn_id_estado(
                   'Resuelta - Pendiente Confirmación del Cliente'
               ),

           fecha_limite_confirmacion =
               CURRENT_TIMESTAMP
               + make_interval(
                   days => p_dias_plazo_confirmacion
               )

     WHERE id_solicitud = v_id_solicitud;


    --------------------------------------------------------
    -- Notificar al cliente
    --------------------------------------------------------

    INSERT INTO notificacion
    (
        id_usuario_destino,
        tipo_evento,
        mensaje,
        id_solicitud_relacionada
    )

    VALUES
    (
        v_id_cliente,

        'ticket_resuelto_pendiente_confirmacion',

        format(
            'Su solicitud #%s fue resuelta. Por favor confirme si el problema quedó solucionado.',
            v_id_solicitud
        ),

        v_id_solicitud
    );

END;

$$;




/*==============================================================*/
/*              PROCEDURE: RECHAZAR REPORTE                     */
/*==============================================================*/
--El rechazo debe devolver la solicitud a En Proceso, porque el técnico debe continuar trabajando. 
--Eso coincide con el flujo del sistema: el administrador puede rechazar el reporte y regresarlo al técnico.

CREATE OR REPLACE FUNCTION sp_rechazar_reporte(
    p_id_reporte         BIGINT,
    p_id_administrador   BIGINT,
    p_comentario_rechazo TEXT
)
RETURNS VOID

LANGUAGE plpgsql

AS
$$

DECLARE

    v_id_solicitud BIGINT;
    v_id_tecnico   BIGINT;

BEGIN

    --------------------------------------------------------
    -- Validar administrador activo
    --------------------------------------------------------

    IF NOT EXISTS (
        SELECT 1
        FROM usuario
        WHERE id_usuario = p_id_administrador
          AND rol = 'administrador'
          AND estado_cuenta = 'activo'
    ) THEN

        RAISE EXCEPTION
            'El usuario % no es un administrador activo.',
            p_id_administrador;

    END IF;


    --------------------------------------------------------
    -- Validar comentario
    --------------------------------------------------------

    IF p_comentario_rechazo IS NULL
       OR btrim(p_comentario_rechazo) = ''
    THEN

        RAISE EXCEPTION
            'Debe indicar el motivo del rechazo del reporte.';

    END IF;


    --------------------------------------------------------
    -- Obtener reporte pendiente
    --------------------------------------------------------

    SELECT id_solicitud,
           id_tecnico

    INTO v_id_solicitud,
         v_id_tecnico

    FROM reporte_solicitud

    WHERE id_reporte = p_id_reporte

      AND estado_aprobacion = 'pendiente'

    FOR UPDATE;


    IF v_id_solicitud IS NULL THEN

        RAISE EXCEPTION
            'El reporte % no existe o ya fue revisado.',
            p_id_reporte;

    END IF;


    --------------------------------------------------------
    -- Registrar administrador responsable
    --------------------------------------------------------

    PERFORM set_config(
        'app.usuario_actual',
        p_id_administrador::TEXT,
        true
    );


    --------------------------------------------------------
    -- Rechazar reporte
    --------------------------------------------------------

    UPDATE reporte_solicitud

       SET estado_aprobacion = 'rechazado',
           fecha_revision = CURRENT_TIMESTAMP,
           id_administrador_revisa = p_id_administrador,
           comentario_rechazo = btrim(p_comentario_rechazo)

     WHERE id_reporte = p_id_reporte;


    --------------------------------------------------------
    -- Regresar solicitud a En Proceso
    --------------------------------------------------------

    UPDATE solicitud

       SET id_estado = fn_id_estado('En Proceso')

     WHERE id_solicitud = v_id_solicitud;


    --------------------------------------------------------
    -- Notificar al técnico
    --------------------------------------------------------

    INSERT INTO notificacion
    (
        id_usuario_destino,
        tipo_evento,
        mensaje,
        id_solicitud_relacionada
    )

    VALUES
    (
        v_id_tecnico,

        'reporte_rechazado',

        format(
            'Su reporte para la solicitud #%s fue rechazado: %s',
            v_id_solicitud,
            btrim(p_comentario_rechazo)
        ),

        v_id_solicitud
    );

END;

$$;



/*==============================================================*/
/*              PROCEDURE: CONFIRMAR CLIENTE                    */
/*==============================================================*/
--El cliente solo puede confirmar su propia solicitud y únicamente cuando está en:
--Resuelta - Pendiente Confirmación del Cliente

CREATE OR REPLACE FUNCTION sp_confirmar_cliente(
    p_id_solicitud      BIGINT,
    p_id_cliente        BIGINT,
    p_problema_resuelto BOOLEAN
)
RETURNS VOID

LANGUAGE plpgsql

AS
$$

DECLARE

    v_estado_actual INTEGER;
    v_cliente_dueno BIGINT;

BEGIN

    --------------------------------------------------------
    -- Obtener solicitud y bloquearla
    --------------------------------------------------------

    SELECT id_estado,
           id_cliente

    INTO v_estado_actual,
         v_cliente_dueno

    FROM solicitud

    WHERE id_solicitud = p_id_solicitud

    FOR UPDATE;


    IF v_estado_actual IS NULL THEN

        RAISE EXCEPTION
            'La solicitud % no existe.',
            p_id_solicitud;

    END IF;


    --------------------------------------------------------
    -- Validar propietario
    --------------------------------------------------------

    IF v_cliente_dueno <> p_id_cliente THEN

        RAISE EXCEPTION
            'El cliente % no es propietario de la solicitud %.',
            p_id_cliente,
            p_id_solicitud;

    END IF;


    --------------------------------------------------------
    -- Validar cuenta activa
    --------------------------------------------------------

    IF NOT EXISTS (
        SELECT 1
        FROM usuario
        WHERE id_usuario = p_id_cliente
          AND rol = 'cliente'
          AND estado_cuenta = 'activo'
    ) THEN

        RAISE EXCEPTION
            'El cliente % no tiene una cuenta activa.',
            p_id_cliente;

    END IF;


    --------------------------------------------------------
    -- Validar estado
    --------------------------------------------------------

    IF v_estado_actual <>
       fn_id_estado(
           'Resuelta - Pendiente Confirmación del Cliente'
       )
    THEN

        RAISE EXCEPTION
            'La solicitud no está pendiente de confirmación del cliente.';

    END IF;


    --------------------------------------------------------
    -- Registrar usuario responsable
    --------------------------------------------------------

    PERFORM set_config(
        'app.usuario_actual',
        p_id_cliente::TEXT,
        true
    );


    --------------------------------------------------------
    -- Problema resuelto
    --------------------------------------------------------

    IF p_problema_resuelto THEN

        UPDATE solicitud

           SET id_estado = fn_id_estado('Cerrada'),
               fecha_limite_confirmacion = NULL

         WHERE id_solicitud = p_id_solicitud;


    --------------------------------------------------------
    -- Problema NO resuelto
    --------------------------------------------------------

    ELSE

        UPDATE solicitud

           SET id_estado = fn_id_estado('En Proceso'),
               fecha_limite_confirmacion = NULL

         WHERE id_solicitud = p_id_solicitud;


        ----------------------------------------------------
        -- Notificar al técnico asignado directamente
        ----------------------------------------------------

        INSERT INTO notificacion
        (
            id_usuario_destino,
            tipo_evento,
            mensaje,
            id_solicitud_relacionada
        )

        SELECT
            id_tecnico,

            'ticket_reabierto',

            format(
                'El cliente indicó que la solicitud #%s no quedó resuelta.',
                p_id_solicitud
            ),

            p_id_solicitud

        FROM asignacion_solicitud

        WHERE id_solicitud = p_id_solicitud

          AND vigente = TRUE

          AND id_tecnico IS NOT NULL;

    END IF;

END;

$$;



/*==============================================================*/
/*       PROCEDURE: CIERRE AUTOMATICO POR VENCIMIENTO            */
/*==============================================================*/

CREATE OR REPLACE FUNCTION sp_cierre_automatico_por_vencimiento()

RETURNS INTEGER

LANGUAGE plpgsql

AS
$$

DECLARE

    v_total INTEGER := 0;

    v_estado_pendiente_confirmacion INTEGER;
    v_estado_cerrada INTEGER;

BEGIN

    --------------------------------------------------------
    -- La accion sera registrada como realizada por el sistema
    --------------------------------------------------------

    PERFORM set_config(
        'app.usuario_actual',
        '',
        true
    );


    --------------------------------------------------------
    -- Obtener estados necesarios
    --------------------------------------------------------

    v_estado_pendiente_confirmacion :=
        fn_id_estado(
            'Resuelta - Pendiente Confirmación del Cliente'
        );

    v_estado_cerrada :=
        fn_id_estado(
            'Cerrada'
        );


    --------------------------------------------------------
    -- Validar que existan los estados
    --------------------------------------------------------

    IF v_estado_pendiente_confirmacion IS NULL THEN

        RAISE EXCEPTION
            'No existe el estado "Resuelta - Pendiente Confirmación del Cliente".';

    END IF;


    IF v_estado_cerrada IS NULL THEN

        RAISE EXCEPTION
            'No existe el estado "Cerrada".';

    END IF;


    --------------------------------------------------------
    -- Seleccionar tickets vencidos
    --
    -- SKIP LOCKED evita esperar por solicitudes que estén
    -- siendo modificadas por otra transacción.
    --------------------------------------------------------

    WITH vencidas AS
    (
        SELECT s.id_solicitud

        FROM solicitud s

        WHERE s.id_estado =
              v_estado_pendiente_confirmacion

          AND s.fecha_limite_confirmacion IS NOT NULL

          AND s.fecha_limite_confirmacion < CURRENT_TIMESTAMP

        FOR UPDATE SKIP LOCKED
    )

    UPDATE solicitud s

       SET id_estado = v_estado_cerrada,

           fecha_limite_confirmacion = NULL

    FROM vencidas v

    WHERE s.id_solicitud = v.id_solicitud;


    --------------------------------------------------------
    -- Obtener cantidad de tickets cerrados
    --------------------------------------------------------

    GET DIAGNOSTICS
        v_total = ROW_COUNT;


    --------------------------------------------------------
    -- Retornar cantidad procesada
    --------------------------------------------------------

    RETURN v_total;

END;

$$;


COMMENT ON FUNCTION sp_cierre_automatico_por_vencimiento()

IS

'CIERRA automaticamente las solicitudes en estado Resuelta - Pendiente Confirmacion del Cliente cuyo plazo de confirmacion haya vencido. Diseñada para ejecucion periodica mediante pg_cron o un scheduler externo.';




/*==============================================================*/
/*     PROCEDURE: REABRIR TICKET CERRADO ADMINISTRATIVO         */
/*==============================================================*/
--Este procedimiento sí intenta modificar una solicitud cuyo estado actual es Cerrada.
--Por tanto, con nuestra nueva función: fn_pre_update_solicitud()

CREATE OR REPLACE FUNCTION sp_reabrir_ticket_cerrado_administrativo(
    p_id_solicitud BIGINT,
    p_id_administrador BIGINT
)

RETURNS VOID

LANGUAGE plpgsql

AS
$$

DECLARE

    v_id_estado_cerrada INTEGER;
    v_id_estado_en_proceso INTEGER;
    v_id_estado_actual INTEGER;

BEGIN

    --------------------------------------------------------
    -- Validar administrador
    --------------------------------------------------------

    IF NOT EXISTS (
        SELECT 1
        FROM usuario
        WHERE id_usuario = p_id_administrador
          AND rol = 'administrador'
          AND estado_cuenta = 'activo'
    ) THEN

        RAISE EXCEPTION
            'El usuario % no es un administrador activo.',
            p_id_administrador;

    END IF;


    --------------------------------------------------------
    -- Obtener estados
    --------------------------------------------------------

    v_id_estado_cerrada :=
        fn_id_estado('Cerrada');

    v_id_estado_en_proceso :=
        fn_id_estado('En Proceso');


    --------------------------------------------------------
    -- Validar existencia de estados
    --------------------------------------------------------

    IF v_id_estado_cerrada IS NULL THEN

        RAISE EXCEPTION
            'No existe el estado "Cerrada".';

    END IF;


    IF v_id_estado_en_proceso IS NULL THEN

        RAISE EXCEPTION
            'No existe el estado "En Proceso".';

    END IF;


    --------------------------------------------------------
    -- Obtener y bloquear la solicitud
    --------------------------------------------------------

    SELECT id_estado

      INTO v_id_estado_actual

      FROM solicitud

     WHERE id_solicitud = p_id_solicitud

     FOR UPDATE;


    --------------------------------------------------------
    -- Validar existencia de solicitud
    --------------------------------------------------------

    IF v_id_estado_actual IS NULL THEN

        RAISE EXCEPTION
            'La solicitud % no existe.',
            p_id_solicitud;

    END IF;


    --------------------------------------------------------
    -- Verificar que realmente este cerrada
    --------------------------------------------------------

    IF v_id_estado_actual <> v_id_estado_cerrada THEN

        RAISE EXCEPTION
            'La solicitud % no esta Cerrada y no puede ser reabierta.',
            p_id_solicitud;

    END IF;


    --------------------------------------------------------
    -- Registrar usuario responsable
    --------------------------------------------------------

    PERFORM set_config(
        'app.usuario_actual',
        p_id_administrador::TEXT,
        true
    );


    --------------------------------------------------------
    -- Permitir temporalmente la reapertura
    --
    -- fn_pre_update_solicitud() detectara este valor y
    -- permitira modificar una solicitud Cerrada.
    --------------------------------------------------------

    PERFORM set_config(
        'app.bypass_cierre',
        'true',
        true
    );


    BEGIN

        ----------------------------------------------------
        -- Reabrir solicitud
        ----------------------------------------------------

        UPDATE solicitud

           SET id_estado = v_id_estado_en_proceso

         WHERE id_solicitud = p_id_solicitud;


        ----------------------------------------------------
        -- Registrar notificacion al administrador
        ----------------------------------------------------

        INSERT INTO notificacion
        (
            id_usuario_destino,
            tipo_evento,
            mensaje,
            id_solicitud_relacionada
        )

        VALUES
        (
            p_id_administrador,
            'ticket_reabierto',
            format(
                'La solicitud #%s ha sido reabierta administrativamente.',
                p_id_solicitud
            ),
            p_id_solicitud
        );


    EXCEPTION

        WHEN OTHERS THEN

            ------------------------------------------------
            -- Limpiar permiso temporal antes de propagar
            -- el error.
            ------------------------------------------------

            PERFORM set_config(
                'app.bypass_cierre',
                'false',
                true
            );

            RAISE;

    END;


    --------------------------------------------------------
    -- Revocar inmediatamente la excepción de reapertura
    --------------------------------------------------------

    PERFORM set_config(
        'app.bypass_cierre',
        'false',
        true
    );


END;

$$;











