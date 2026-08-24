// Capa de presentacion: este archivo SOLO habla con el backend y guarda la
// sesion en el navegador. No valida reglas de negocio ni decide estados de
// ticket - eso vive en los procedimientos de PostgreSQL y en el backend
// Java, tal como en el resto del sistema.

const API_BASE = 'http://localhost:8080';

function guardarSesion(token, idUsuario, rol, estadoPago, idSesion) {
    localStorage.setItem('token', token);
    localStorage.setItem('idUsuario', idUsuario);
    localStorage.setItem('rol', rol);
    if (estadoPago) {
        localStorage.setItem('estadoPago', estadoPago);
    } else {
        localStorage.removeItem('estadoPago');
    }
    if (idSesion) {
        localStorage.setItem('idSesion', idSesion);
    } else {
        localStorage.removeItem('idSesion');
    }
}

function limpiarSesion() {
    localStorage.removeItem('token');
    localStorage.removeItem('idUsuario');
    localStorage.removeItem('rol');
    localStorage.removeItem('estadoPago');
    localStorage.removeItem('idSesion');
}

function obtenerToken() {
    return localStorage.getItem('token');
}

function obtenerRol() {
    return localStorage.getItem('rol');
}

function obtenerIdUsuario() {
    return localStorage.getItem('idUsuario');
}

function obtenerEstadoPago() {
    return localStorage.getItem('estadoPago');
}

function obtenerIdSesion() {
    return localStorage.getItem('idSesion');
}

const PAGINA_POR_ROL = {
    CLIENTE: 'cliente.html',
    TECNICO: 'tecnico.html',
    ADMINISTRADOR: 'admin.html',
    SUPERUSUARIO: 'superusuario.html'
};

/**
 * Exige sesion activa; si no hay token, manda a login. Si se pasa un rol
 * esperado y no coincide, tambien manda a login (proteccion de UI: la
 * proteccion real ya la hace el backend con el JWT en cada endpoint).
 */
function exigirSesion(rolEsperado) {
    const token = obtenerToken();
    if (!token) {
        window.location.href = 'login.html';
        return false;
    }
    if (rolEsperado && obtenerRol() !== rolEsperado) {
        window.location.href = 'login.html';
        return false;
    }
    return true;
}

/**
 * Cierra el registro de auditoria_sesion (fecha_salida) antes de borrar la
 * sesion local. Es "mejor esfuerzo": si el backend no responde, igual se
 * cierra la sesion en el navegador - no vale la pena bloquear al usuario
 * por esto.
 */
async function cerrarSesion() {
    const idSesion = obtenerIdSesion();
    if (idSesion) {
        try {
            await apiFetch('/api/auth/logout', {
                method: 'POST',
                body: JSON.stringify({ idSesion: Number(idSesion) })
            });
        } catch (error) {
            console.error('No se pudo cerrar el registro de auditoria de sesion:', error);
        }
    }
    limpiarSesion();
    window.location.href = 'login.html';
}

/**
 * Wrapper central de fetch: agrega el token si existe, arma el body en
 * JSON, y convierte una respuesta no exitosa en una excepcion con el
 * mensaje real que manda el backend (GlobalExceptionHandler siempre
 * responde {"error": "..."}).
 */
async function apiFetch(path, options = {}) {
    const headers = Object.assign({ 'Content-Type': 'application/json' }, options.headers || {});
    const token = obtenerToken();
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }

    let respuesta;
    try {
        respuesta = await fetch(API_BASE + path, Object.assign({}, options, { headers }));
    } catch (error) {
        throw new Error('No se pudo conectar con el servidor. ¿Esta corriendo el backend en ' + API_BASE + '?');
    }

    // Solo se trata como "sesion vencida" si la llamada llevaba un token:
    // el propio /api/auth/login tambien responde 401 cuando la contrasena
    // es incorrecta, y ese caso NO lleva token - debe mostrarse como error
    // normal en el formulario, no mandar de vuelta a login.html (eso hacia
    // que la pantalla de login "no cargara" al fallar el intento).
    if (respuesta.status === 401 && token) {
        limpiarSesion();
        window.location.href = 'login.html';
        throw new Error('Sesion vencida o invalida.');
    }

    if (respuesta.status === 204) {
        return null;
    }

    const texto = await respuesta.text();
    let cuerpo = null;
    if (texto) {
        try {
            cuerpo = JSON.parse(texto);
        } catch (error) {
            cuerpo = texto;
        }
    }

    if (!respuesta.ok) {
        const mensaje = (cuerpo && cuerpo.error) ? cuerpo.error : ('Error ' + respuesta.status + ' del servidor.');
        throw new Error(mensaje);
    }

    return cuerpo;
}

function mostrarError(elementoMensaje, error) {
    elementoMensaje.textContent = error.message || String(error);
    elementoMensaje.classList.remove('oculto');
}

function ocultarMensaje(elementoMensaje) {
    elementoMensaje.textContent = '';
    elementoMensaje.classList.add('oculto');
}

function claseBadgeEstado(nombreEstado) {
    const mapa = {
        'Pendiente': 'badge-pendiente',
        'En Proceso': 'badge-en-proceso',
        'Pendiente Aprobación': 'badge-pendiente-aprobacion',
        'Resuelta - Pendiente Confirmación del Cliente': 'badge-resuelta',
        'Cerrada': 'badge-cerrada'
    };
    return mapa[nombreEstado] || '';
}

function claseBadgeEstadoCuenta(estadoCuenta) {
    const mapa = {
        'activo': 'badge-activo',
        'suspendido': 'badge-suspendido',
        'inactivo': 'badge-inactivo'
    };
    return mapa[estadoCuenta] || '';
}

function claseBadgeEstadoPago(estadoPago) {
    return estadoPago === 'moroso' ? 'badge-moroso' : 'badge-al_dia';
}

function claseBadgeOperacion(operacion) {
    const mapa = {
        'INSERT': 'badge-activo',
        'UPDATE': 'badge-en-proceso',
        'DELETE': 'badge-moroso'
    };
    return mapa[operacion] || '';
}

function escaparHtml(texto) {
    if (texto === null || texto === undefined) return '';
    const div = document.createElement('div');
    div.textContent = String(texto);
    return div.innerHTML;
}

function formatearFecha(fechaIso) {
    if (!fechaIso) return '—';
    const fecha = new Date(fechaIso);
    if (isNaN(fecha.getTime())) return fechaIso;
    return fecha.toLocaleString('es-EC', { dateStyle: 'medium', timeStyle: 'short' });
}

/**
 * Convierte la barra lateral en pestañas reales: al hacer clic en un link
 * se muestra SOLO esa seccion (#id) y se ocultan las demas, en vez de
 * simplemente saltar con scroll. Por defecto queda visible la primera
 * seccion (el resumen de metricas de cada panel). Se llama una vez al
 * cargar cada panel; no hace nada si la pagina no tiene barra lateral.
 */
function activarNavegacionPorTabs() {
    const enlaces = document.querySelectorAll('.sidebar-nav a[href^="#"]');
    const secciones = Array.from(enlaces)
        .map(function (enlace) { return document.querySelector(enlace.getAttribute('href')); })
        .filter(Boolean);

    if (secciones.length === 0) return;

    function mostrarSeccion(idObjetivo) {
        enlaces.forEach(function (enlace) {
            enlace.classList.toggle('activo', enlace.getAttribute('href') === '#' + idObjetivo);
        });
        secciones.forEach(function (seccion) {
            seccion.classList.toggle('oculto', seccion.id !== idObjetivo);
        });
    }

    enlaces.forEach(function (enlace) {
        enlace.addEventListener('click', function (evento) {
            evento.preventDefault();
            mostrarSeccion(enlace.getAttribute('href').slice(1));
        });
    });

    mostrarSeccion(secciones[0].id);
}

const ICONO_OJO = '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7z"/><circle cx="12" cy="12" r="3"/></svg>';
const ICONO_OJO_TACHADO = '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.94 10.94 0 0112 19c-7 0-11-7-11-7a20.3 20.3 0 015.06-5.94M9.9 4.24A10.4 10.4 0 0112 4c7 0 11 7 11 7a20.3 20.3 0 01-3.22 4.06M14.12 14.12a3 3 0 11-4.24-4.24"/><path d="M1 1l22 22"/></svg>';

/**
 * Agrega el boton de "mostrar/ocultar contraseña" a cada campo marcado con
 * la clase campo-con-icono. Cambia el type del input entre password/text.
 */
function activarAlternarContrasena() {
    document.querySelectorAll('.alternar-contrasena').forEach(function (boton) {
        boton.innerHTML = ICONO_OJO;
        boton.addEventListener('click', function () {
            const input = document.getElementById(boton.getAttribute('data-target'));
            const mostrando = input.type === 'text';
            input.type = mostrando ? 'password' : 'text';
            boton.innerHTML = mostrando ? ICONO_OJO : ICONO_OJO_TACHADO;
            boton.setAttribute('aria-label', mostrando ? 'Mostrar contraseña' : 'Ocultar contraseña');
        });
    });
}

/** HTML de un estado "cargando" con spinner, para usar en listas mientras llega la respuesta. */
function htmlCargando(texto) {
    return '<div class="estado-cargando"><span class="spinner"></span>' + escaparHtml(texto || 'Cargando...') + '</div>';
}

/**
 * Modal de confirmacion propio (reemplaza el confirm() nativo del
 * navegador, que no se puede estilizar). Devuelve una Promise<boolean>:
 * true si el usuario confirma, false si cancela o cierra.
 */
function confirmarAccion(titulo, mensaje, textoConfirmar) {
    return new Promise(function (resolve) {
        const overlay = document.createElement('div');
        overlay.className = 'overlay-modal';
        overlay.innerHTML =
            '<div class="modal">' +
            '<h3>' + escaparHtml(titulo) + '</h3>' +
            '<p>' + escaparHtml(mensaje) + '</p>' +
            '<div class="modal-acciones">' +
            '<button type="button" class="secundario" data-accion="cancelar">Cancelar</button>' +
            '<button type="button" data-accion="confirmar">' + escaparHtml(textoConfirmar || 'Confirmar') + '</button>' +
            '</div></div>';

        function cerrar(resultado) {
            document.body.removeChild(overlay);
            document.removeEventListener('keydown', alPresionarTecla);
            resolve(resultado);
        }

        function alPresionarTecla(evento) {
            if (evento.key === 'Escape') cerrar(false);
        }

        overlay.addEventListener('click', function (evento) {
            if (evento.target === overlay) cerrar(false);
        });
        overlay.querySelector('[data-accion="cancelar"]').addEventListener('click', function () { cerrar(false); });
        overlay.querySelector('[data-accion="confirmar"]').addEventListener('click', function () { cerrar(true); });
        document.addEventListener('keydown', alPresionarTecla);

        document.body.appendChild(overlay);
    });
}

/**
 * Engancha el boton "Cambiar contraseña" del sidebar-pie (presente en los
 * cuatro paneles) a un modal propio. No necesita config por pagina porque
 * siempre habla del usuario logeado (sale del JWT en el backend).
 */
function activarModalCambiarContrasena() {
    const boton = document.getElementById('btn-cambiar-contrasena');
    if (!boton) return;

    boton.addEventListener('click', function () {
        const overlay = document.createElement('div');
        overlay.className = 'overlay-modal';
        overlay.innerHTML =
            '<div class="modal">' +
            '<h3>Cambiar contraseña</h3>' +
            '<div id="mensaje-error-contrasena" class="mensaje-error oculto"></div>' +
            '<form id="form-cambiar-contrasena">' +
            '<div class="campo">' +
            '<label for="contrasena-actual-modal">Contraseña actual</label>' +
            '<div class="campo-con-icono">' +
            '<input type="password" id="contrasena-actual-modal" required autocomplete="current-password">' +
            '<button type="button" class="alternar-contrasena" data-target="contrasena-actual-modal" aria-label="Mostrar contraseña"></button>' +
            '</div></div>' +
            '<div class="campo">' +
            '<label for="contrasena-nueva-modal">Contraseña nueva</label>' +
            '<div class="campo-con-icono">' +
            '<input type="password" id="contrasena-nueva-modal" required minlength="8" autocomplete="new-password">' +
            '<button type="button" class="alternar-contrasena" data-target="contrasena-nueva-modal" aria-label="Mostrar contraseña"></button>' +
            '</div></div>' +
            '<div class="modal-acciones">' +
            '<button type="button" class="secundario" data-accion="cancelar">Cancelar</button>' +
            '<button type="submit" id="btn-confirmar-contrasena">Guardar</button>' +
            '</div>' +
            '</form></div>';

        function cerrar() {
            document.body.removeChild(overlay);
            document.removeEventListener('keydown', alPresionarTecla);
        }

        function alPresionarTecla(evento) {
            if (evento.key === 'Escape') cerrar();
        }

        overlay.addEventListener('click', function (evento) {
            if (evento.target === overlay) cerrar();
        });
        overlay.querySelector('[data-accion="cancelar"]').addEventListener('click', cerrar);
        document.addEventListener('keydown', alPresionarTecla);

        overlay.querySelectorAll('.alternar-contrasena').forEach(function (botonOjo) {
            botonOjo.innerHTML = ICONO_OJO;
            botonOjo.addEventListener('click', function () {
                const input = document.getElementById(botonOjo.getAttribute('data-target'));
                const mostrando = input.type === 'text';
                input.type = mostrando ? 'password' : 'text';
                botonOjo.innerHTML = mostrando ? ICONO_OJO : ICONO_OJO_TACHADO;
            });
        });

        const mensajeError = overlay.querySelector('#mensaje-error-contrasena');
        overlay.querySelector('#form-cambiar-contrasena').addEventListener('submit', async function (evento) {
            evento.preventDefault();
            ocultarMensaje(mensajeError);

            const btnGuardar = overlay.querySelector('#btn-confirmar-contrasena');
            btnGuardar.disabled = true;
            btnGuardar.textContent = 'Guardando...';

            try {
                await apiFetch('/api/usuarios/contrasena', {
                    method: 'POST',
                    body: JSON.stringify({
                        contrasenaActual: overlay.querySelector('#contrasena-actual-modal').value,
                        contrasenaNueva: overlay.querySelector('#contrasena-nueva-modal').value
                    })
                });
                cerrar();
            } catch (error) {
                mostrarError(mensajeError, error);
                btnGuardar.disabled = false;
                btnGuardar.textContent = 'Guardar';
            }
        });

        document.body.appendChild(overlay);
    });
}

/**
 * Conecta un panel de adjuntos (subir/listar/descargar evidencia) a los ids
 * que se le pasen en `config`. Se usa igual desde cliente.js y tecnico.js -
 * ambos roles pueden subir evidencia a una solicitud. Devuelve una funcion
 * `abrir(idSolicitud)` para invocar desde el boton "Adjuntos" de cada fila.
 *
 * La subida usa fetch crudo (no apiFetch) porque un archivo va como
 * FormData, no como JSON: el navegador debe fijar el Content-Type con el
 * boundary el mismo, no se puede fijar a mano.
 */
function activarPanelAdjuntos(config) {
    const panel = document.getElementById(config.idPanel);
    const idSolicitudSpan = document.getElementById(config.idSpanSolicitud);
    const mensajeError = document.getElementById(config.idMensajeError);
    const listaDiv = document.getElementById(config.idLista);
    const form = document.getElementById(config.idForm);
    const inputArchivo = document.getElementById(config.idInputArchivo);
    const btnSubir = document.getElementById(config.idBtnSubir);

    let idSolicitudActual = null;

    async function cargarLista() {
        listaDiv.innerHTML = htmlCargando();
        try {
            const adjuntos = await apiFetch('/api/solicitudes/' + idSolicitudActual + '/adjuntos');

            if (!adjuntos.length) {
                listaDiv.innerHTML = '<div class="vacio">Todavía no hay adjuntos.</div>';
                return;
            }

            listaDiv.innerHTML = '<ul style="list-style:none; padding:0; margin:0;">' +
                adjuntos.map(function (a) {
                    const icono = a.tipoArchivo === 'application/pdf' ? '📄' : '🖼️';
                    return '<li style="display:flex; justify-content:space-between; align-items:center; padding:9px 0; border-bottom:1px solid var(--color-borde);">' +
                        '<span>' + icono + ' ' + escaparHtml(a.nombreArchivo) + '</span>' +
                        '<a href="' + API_BASE + '/api/adjuntos/' + a.idAdjunto + '/archivo" target="_blank" rel="noopener">Ver / descargar</a>' +
                        '</li>';
                }).join('') + '</ul>';
        } catch (error) {
            listaDiv.innerHTML = '';
            mostrarError(mensajeError, error);
        }
    }

    form.addEventListener('submit', async function (evento) {
        evento.preventDefault();
        ocultarMensaje(mensajeError);

        const archivo = inputArchivo.files[0];
        if (!archivo) {
            mostrarError(mensajeError, new Error('Selecciona un archivo primero.'));
            return;
        }

        btnSubir.disabled = true;
        btnSubir.textContent = 'Subiendo...';

        try {
            const datosFormulario = new FormData();
            datosFormulario.append('archivo', archivo);

            const headers = {};
            const token = obtenerToken();
            if (token) headers['Authorization'] = 'Bearer ' + token;

            const respuesta = await fetch(API_BASE + '/api/solicitudes/' + idSolicitudActual + '/adjuntos', {
                method: 'POST',
                headers: headers,
                body: datosFormulario
            });

            const texto = await respuesta.text();
            let cuerpo = null;
            if (texto) {
                try { cuerpo = JSON.parse(texto); } catch (e) { cuerpo = texto; }
            }

            if (!respuesta.ok) {
                throw new Error((cuerpo && cuerpo.error) ? cuerpo.error : ('Error ' + respuesta.status + ' al subir el archivo.'));
            }

            inputArchivo.value = '';
            cargarLista();
        } catch (error) {
            mostrarError(mensajeError, error);
        } finally {
            btnSubir.disabled = false;
            btnSubir.textContent = 'Subir';
        }
    });

    document.getElementById(config.idBtnCerrar).addEventListener('click', function () {
        panel.classList.add('oculto');
    });

    return function abrir(idSolicitud) {
        idSolicitudActual = idSolicitud;
        idSolicitudSpan.textContent = '#' + idSolicitud;
        ocultarMensaje(mensajeError);
        inputArchivo.value = '';
        panel.classList.remove('oculto');
        panel.scrollIntoView({ behavior: 'smooth' });
        cargarLista();
    };
}
