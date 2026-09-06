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

/**
 * Clase de color para el numero grande de una tarjeta de metrica, segun 2
 * umbrales ("atencion" en ambar, "alerta" en rojo) - para que un numero alto
 * salte a la vista sin tener que leer la etiqueta. Sin umbral superado
 * devuelve '' (color normal).
 */
function claseAlertaPorValor(valor, umbralAtencion, umbralAlerta) {
    if (typeof valor !== 'number') return '';
    if (valor >= umbralAlerta) return 'valor-alerta';
    if (valor >= umbralAtencion) return 'valor-atencion';
    return '';
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
        // Paneles flotantes (panel-asignar, panel-rechazar, panel-reportar,
        // panel-adjuntos...) no son parte del menu - se abren aparte al
        // hacer clic en una fila. Si quedan abiertos y cambias de pestaña,
        // se quedaban pegados arriba de la seccion nueva porque nadie los
        // volvia a ocultar. Se cierran todos al cambiar de pestaña.
        document.querySelectorAll('[id^="panel-"]').forEach(function (panel) {
            panel.classList.add('oculto');
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
 * Convierte un <input type="text"> + un <div> de sugerencias en un selector
 * "buscable" que filtra EN EL CLIENTE sobre una lista ya cargada - sin ir al
 * servidor por cada letra. Pensado para listas chicas (grupos técnicos,
 * categorías, etc.), nunca para miles de registros: ahí lo que hace falta es
 * buscar en el servidor, ver activarBusquedaRemota().
 *
 * Uso: const selector = activarSelectorBuscable('input-id', 'sugerencias-id');
 * selector.setOpciones([{valor: '1', etiqueta: 'Grupo Alfa'}, ...]);
 * selector.valor() -> el valor elegido, o null si todavía no eligió nada.
 */
function activarSelectorBuscable(idInput, idSugerencias) {
    const input = document.getElementById(idInput);
    const sugerencias = document.getElementById(idSugerencias);
    let opciones = [];
    let valorSeleccionado = null;

    function coincidencias(termino) {
        const t = termino.trim().toLowerCase();
        return t ? opciones.filter(function (o) { return o.etiqueta.toLowerCase().includes(t); }) : opciones;
    }

    function render(lista) {
        sugerencias.innerHTML = lista.length
            ? lista.map(function (o) {
                return '<button type="button" class="sugerencia-usuario" data-valor="' + o.valor + '">' + escaparHtml(o.etiqueta) + '</button>';
            }).join('')
            : '<div class="sugerencia-vacia">Sin coincidencias</div>';
        sugerencias.classList.remove('oculto');
    }

    input.addEventListener('input', function () {
        valorSeleccionado = null;
        const termino = input.value.trim();
        if (!termino) {
            sugerencias.classList.add('oculto');
            sugerencias.innerHTML = '';
            return;
        }
        render(coincidencias(termino));
    });

    sugerencias.addEventListener('click', function (evento) {
        const boton = evento.target.closest('[data-valor]');
        if (!boton) return;
        valorSeleccionado = boton.getAttribute('data-valor');
        input.value = boton.textContent;
        sugerencias.classList.add('oculto');
    });

    document.addEventListener('click', function (evento) {
        if (evento.target !== input && !sugerencias.contains(evento.target)) {
            sugerencias.classList.add('oculto');
        }
    });

    return {
        setOpciones: function (lista) { opciones = lista; },
        valor: function () { return valorSeleccionado; },
        limpiar: function () { input.value = ''; valorSeleccionado = null; sugerencias.classList.add('oculto'); }
    };
}

/**
 * Igual que activarSelectorBuscable(), pero para listas grandes: en vez de
 * filtrar sobre una lista ya cargada, pide al servidor con un debounce de
 * 300ms (para no mandar una petición por cada letra) usando la funcion
 * `fnBuscar(termino)` que se le pasa. Cada resultado debe traer al menos
 * {idUsuario, nombreUsuario, correo} (la forma que ya devuelven
 * /api/tecnicos/buscar y /api/auditoria/usuarios/buscar).
 */
function activarBusquedaRemota(idInput, idSugerencias, fnBuscar, callbacks) {
    const input = document.getElementById(idInput);
    const sugerencias = document.getElementById(idSugerencias);
    const onSeleccionar = (callbacks && callbacks.onSeleccionar) || function () {};
    const onLimpiar = (callbacks && callbacks.onLimpiar) || function () {};
    let valorSeleccionado = null;
    let temporizador = null;

    input.addEventListener('input', function () {
        valorSeleccionado = null;
        const termino = input.value.trim();
        clearTimeout(temporizador);

        if (termino.length < 2) {
            sugerencias.classList.add('oculto');
            sugerencias.innerHTML = '';
            onLimpiar();
            return;
        }

        temporizador = setTimeout(async function () {
            try {
                const resultados = await fnBuscar(termino);
                sugerencias.innerHTML = resultados.length
                    ? resultados.map(function (u) {
                        return '<button type="button" class="sugerencia-usuario" data-id="' + u.idUsuario + '" data-nombre="' + escaparHtml(u.nombreUsuario) + '">' +
                            '<strong>' + escaparHtml(u.nombreUsuario) + '</strong>' +
                            '<span>#' + u.idUsuario + ' · ' + escaparHtml(u.correo) + '</span>' +
                            '</button>';
                    }).join('')
                    : '<div class="sugerencia-vacia">Sin coincidencias</div>';
                sugerencias.classList.remove('oculto');
            } catch (error) {
                console.error('No se pudo buscar:', error);
            }
        }, 300);
    });

    sugerencias.addEventListener('click', function (evento) {
        const boton = evento.target.closest('.sugerencia-usuario');
        if (!boton) return;
        valorSeleccionado = boton.getAttribute('data-id');
        input.value = boton.getAttribute('data-nombre') + ' (#' + valorSeleccionado + ')';
        sugerencias.classList.add('oculto');
        onSeleccionar(valorSeleccionado);
    });

    document.addEventListener('click', function (evento) {
        if (evento.target !== input && !sugerencias.contains(evento.target)) {
            sugerencias.classList.add('oculto');
        }
    });

    return {
        valor: function () { return valorSeleccionado; },
        limpiar: function () { input.value = ''; valorSeleccionado = null; sugerencias.classList.add('oculto'); }
    };
}

/**
 * Muestra un adjunto en un visor flotante (modal) en vez de una pestaña
 * nueva - una imagen se ve directo, un PDF se embebe en un iframe. El
 * archivo se pide con fetch autenticado (un <img>/<iframe> comun no manda el
 * header Authorization, asi que no puede apuntar directo a la URL del
 * backend) y se muestra como blob. Se usa desde todos los lugares donde se
 * lista evidencia: el panel de Adjuntos de cliente/tecnico, "Ver evidencias"
 * de administrador y "Ver detalles de solicitud" de los 3 roles - todos
 * pasan por esta misma funcion, asi que el comportamiento queda igual en
 * cualquier pantalla.
 */
async function abrirVisorArchivo(rutaApi, nombreArchivo, tipoArchivo) {
    const overlay = document.createElement('div');
    overlay.className = 'overlay-modal';
    overlay.innerHTML =
        '<div class="modal modal-ancho-xl">' +
        '<h3>' + escaparHtml(nombreArchivo || 'Archivo') + '</h3>' +
        '<div id="cuerpo-visor-archivo">' + htmlCargando('Cargando archivo...') + '</div>' +
        '<div class="modal-acciones">' +
        '<button type="button" class="secundario" data-accion="cerrar">Cerrar</button>' +
        '</div>' +
        '</div>';

    let urlBlob = null;

    function cerrar() {
        document.body.removeChild(overlay);
        document.removeEventListener('keydown', alPresionarTecla);
        if (urlBlob) URL.revokeObjectURL(urlBlob);
    }

    function alPresionarTecla(evento) {
        if (evento.key === 'Escape') cerrar();
    }

    document.body.appendChild(overlay);
    overlay.addEventListener('click', function (evento) {
        if (evento.target === overlay) cerrar();
    });
    overlay.querySelector('[data-accion="cerrar"]').addEventListener('click', cerrar);
    document.addEventListener('keydown', alPresionarTecla);

    const cuerpo = overlay.querySelector('#cuerpo-visor-archivo');

    try {
        const headers = {};
        const token = obtenerToken();
        if (token) headers['Authorization'] = 'Bearer ' + token;

        const respuesta = await fetch(API_BASE + rutaApi, { headers: headers });

        if (respuesta.status === 401) {
            cerrar();
            limpiarSesion();
            window.location.href = 'login.html';
            return;
        }
        if (!respuesta.ok) {
            throw new Error('No se pudo abrir el archivo (Error ' + respuesta.status + ').');
        }

        const blob = await respuesta.blob();
        urlBlob = URL.createObjectURL(blob);

        cuerpo.innerHTML = tipoArchivo === 'application/pdf'
            ? '<iframe src="' + urlBlob + '" style="width:100%; height:75vh; border:1px solid var(--color-borde); border-radius: var(--radio);"></iframe>'
            : '<img src="' + urlBlob + '" alt="' + escaparHtml(nombreArchivo || 'evidencia') + '" style="max-width:100%; max-height:75vh; display:block; margin:0 auto; border-radius: var(--radio);">';
    } catch (error) {
        cuerpo.innerHTML = '<div class="mensaje-error">' + escaparHtml(error.message || 'No se pudo abrir el archivo.') + '</div>';
    }
}

/**
 * Sube un archivo de evidencia a una solicitud. Fetch crudo (no apiFetch)
 * porque el archivo va como FormData: el navegador debe fijar el
 * Content-Type con el boundary el mismo, no se puede fijar a mano. La usan
 * tanto activarPanelAdjuntos() (subir evidencia a una solicitud ya creada)
 * como el formulario de "Nueva solicitud" (adjuntar evidencia al crearla).
 */
async function subirAdjunto(idSolicitud, archivo) {
    const datosFormulario = new FormData();
    datosFormulario.append('archivo', archivo);

    const headers = {};
    const token = obtenerToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;

    const respuesta = await fetch(API_BASE + '/api/solicitudes/' + idSolicitud + '/adjuntos', {
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

    return cuerpo;
}

/**
 * Conecta un panel de adjuntos (subir/listar/descargar evidencia) a los ids
 * que se le pasen en `config`. Se usa igual desde cliente.js y tecnico.js -
 * ambos roles pueden subir evidencia a una solicitud. Devuelve una funcion
 * `abrir(idSolicitud)` para invocar desde el boton "Adjuntos" de cada fila.
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
                        '<button type="button" class="secundario btn-ver-adjunto" data-id="' + a.idAdjunto + '" data-nombre="' + escaparHtml(a.nombreArchivo) + '" data-tipo="' + escaparHtml(a.tipoArchivo || '') + '">Ver</button>' +
                        '</li>';
                }).join('') + '</ul>';

            listaDiv.querySelectorAll('.btn-ver-adjunto').forEach(function (boton) {
                boton.addEventListener('click', function () { abrirArchivoAdjunto(boton); });
            });
        } catch (error) {
            listaDiv.innerHTML = '';
            mostrarError(mensajeError, error);
        }
    }

    /**
     * Muestra el adjunto en el visor flotante (abrirVisorArchivo) en vez de
     * una pestaña nueva - se ve mejor y no depende de que el navegador
     * permita ventanas emergentes.
     */
    async function abrirArchivoAdjunto(boton) {
        const idAdjunto = boton.getAttribute('data-id');
        await abrirVisorArchivo(
            '/api/adjuntos/' + idAdjunto + '/archivo',
            boton.getAttribute('data-nombre'),
            boton.getAttribute('data-tipo')
        );
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
            await subirAdjunto(idSolicitudActual, archivo);
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

/**
 * Icono segun el tipo MIME de un adjunto - lo usan tanto el modal de
 * evidencias del administrador como el de "Ver detalles de solicitud".
 */
function iconoParaTipoAdjunto(tipo) {
    if (!tipo) return '📎';
    if (tipo.startsWith('image/')) return '🖼️';
    if (tipo === 'application/pdf') return '📄';
    return '📎';
}

/**
 * Carga y dibuja, dentro de `contenedor`, la lista de adjuntos (evidencia)
 * de una solicitud con un boton "Ver" en cada uno. Factorizado de
 * abrirModalEvidencias (admin.js) para que "Ver detalles de solicitud" no
 * tenga que repetir la misma logica.
 */
async function cargarListaAdjuntos(idSolicitud, contenedor) {
    const adjuntos = await apiFetch('/api/solicitudes/' + idSolicitud + '/adjuntos');

    contenedor.innerHTML = adjuntos.length
        ? adjuntos.map(function (a) {
            return '<div class="fila-miembro-grupo">' +
                '<span>' + iconoParaTipoAdjunto(a.tipoArchivo) + ' <strong>' + escaparHtml(a.nombreArchivo) + '</strong> · ' + escaparHtml(a.tipoArchivo || '') + '</span>' +
                '<button type="button" class="btn-ver-evidencia secundario btn-compacto" data-id="' + a.idAdjunto + '" data-nombre="' + escaparHtml(a.nombreArchivo) + '" data-tipo="' + escaparHtml(a.tipoArchivo || '') + '">Ver</button>' +
                '</div>';
        }).join('')
        : '<div class="vacio">No hay evidencias adjuntas.</div>';

    contenedor.querySelectorAll('.btn-ver-evidencia').forEach(function (boton) {
        boton.addEventListener('click', function () {
            abrirVisorArchivo(
                '/api/adjuntos/' + boton.getAttribute('data-id') + '/archivo',
                boton.getAttribute('data-nombre'),
                boton.getAttribute('data-tipo')
            );
        });
    });
}

/**
 * Modal "Ver detalles de solicitud": trae todo lo que hay sobre un ticket en
 * una sola pantalla (datos generales, cliente, quien la tiene asignada,
 * historial de reportes de solucion y evidencias adjuntas) - antes esta
 * informacion estaba repartida y bastante de ella no se podia ver desde
 * ninguna pantalla (GET /api/solicitudes/{id} ya existia y ya tenia toda la
 * autorizacion por rol, pero ninguna pagina lo llamaba). Se usa igual desde
 * admin.js, tecnico.js y cliente.js - el backend decide que puede ver cada
 * rol, aca solo se dibuja lo que llegue.
 *
 * `opciones` (todas opcionales) permite que una pagina agregue contenido
 * propio SIN que este archivo tenga que saber nada especifico de un rol:
 *   - extraHtml(detalle): string de HTML que se agrega al final del cuerpo.
 *   - alRenderizar(cuerpo, detalle, cerrar): corre despues de pintar todo
 *     (incluidas las evidencias) - aca la pagina que llamo engancha sus
 *     propios listeners contra lo que agrego en extraHtml. Recibe `cerrar`
 *     para poder cerrar el modal el mismo (ej. al confirmar una asignacion).
 * Lo usa admin.js para meter el formulario de Asignar/Reasignar adentro del
 * mismo modal - antes vivia aparte, sin ver la descripcion de la solicitud.
 */
async function abrirModalDetalleSolicitud(idSolicitud, opciones) {
    opciones = opciones || {};
    const overlay = document.createElement('div');
    overlay.className = 'overlay-modal';
    overlay.innerHTML =
        '<div class="modal modal-ancho-xl">' +
        '<h3>Solicitud #' + idSolicitud + '</h3>' +
        '<div id="mensaje-error-detalle" class="mensaje-error oculto"></div>' +
        '<div id="cuerpo-detalle-solicitud">' + htmlCargando('Cargando detalle...') + '</div>' +
        '<div class="modal-acciones">' +
        '<button type="button" class="secundario" data-accion="cerrar">Cerrar</button>' +
        '</div>' +
        '</div>';

    function cerrar() {
        document.body.removeChild(overlay);
        document.removeEventListener('keydown', alPresionarTecla);
    }

    function alPresionarTecla(evento) {
        if (evento.key === 'Escape') cerrar();
    }

    document.body.appendChild(overlay);
    overlay.addEventListener('click', function (evento) {
        if (evento.target === overlay) cerrar();
    });
    overlay.querySelector('[data-accion="cerrar"]').addEventListener('click', cerrar);
    document.addEventListener('keydown', alPresionarTecla);

    const mensajeError = overlay.querySelector('#mensaje-error-detalle');
    const cuerpo = overlay.querySelector('#cuerpo-detalle-solicitud');

    function filaDato(etiqueta, valor) {
        return '<div class="fila-miembro-grupo"><span><strong>' + escaparHtml(etiqueta) + ':</strong> ' + valor + '</span></div>';
    }

    const MAPA_BADGE_APROBACION = { pendiente: 'badge-en-proceso', aprobado: 'badge-activo', rechazado: 'badge-moroso' };

    function filaReporte(r) {
        const claseBadge = MAPA_BADGE_APROBACION[r.estadoAprobacion] || '';
        let extra = '<div>' + escaparHtml(r.detalleReporte) + '</div>';
        if (r.estadoAprobacion === 'rechazado' && r.comentarioRechazo) {
            extra += '<div style="color:var(--color-peligro,#b91c1c);">Motivo del rechazo: ' + escaparHtml(r.comentarioRechazo) + '</div>';
        }
        return '<div class="fila-miembro-grupo" style="flex-direction:column; align-items:stretch; gap:4px;">' +
            '<span><span class="badge ' + claseBadge + '">' + escaparHtml(r.estadoAprobacion) + '</span> ' +
            '<strong>' + escaparHtml(r.tecnicoNombre || ('Técnico #' + r.idTecnico)) + '</strong> · ' + formatearFecha(r.fechaEnvio) + '</span>' +
            extra + '</div>';
    }

    try {
        const detalle = await apiFetch('/api/solicitudes/' + idSolicitud);

        let asignacionHtml;
        if (detalle.tecnicoAsignadoNombre) {
            asignacionHtml = 'Técnico: <strong>' + escaparHtml(detalle.tecnicoAsignadoNombre) + '</strong> (' + escaparHtml(detalle.tecnicoAsignadoCorreo || '—') + ')' +
                ' — asignado el ' + formatearFecha(detalle.fechaAsignacion);
        } else if (detalle.grupoAsignadoNombre) {
            asignacionHtml = 'Grupo: <strong>' + escaparHtml(detalle.grupoAsignadoNombre) + '</strong> — asignado el ' + formatearFecha(detalle.fechaAsignacion);
        } else {
            asignacionHtml = 'Todavía no ha sido asignada.';
        }
        if (detalle.esReasignacion && detalle.motivoReasignacion) {
            asignacionHtml += '<br>Motivo de la reasignación: ' + escaparHtml(detalle.motivoReasignacion);
        }

        cuerpo.innerHTML =
            '<div class="fila-metricas" style="margin-bottom:14px;">' +
            '<div class="tarjeta-metrica"><div class="valor" style="font-size:1rem;"><span class="badge ' + claseBadgeEstado(detalle.estado) + '">' + escaparHtml(detalle.estado) + '</span></div><div class="etiqueta">Estado</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor" style="font-size:1rem;">' + escaparHtml(detalle.prioridad || '—') + '</div><div class="etiqueta">Prioridad</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor" style="font-size:1rem;">' + escaparHtml(detalle.categoria || '—') + '</div><div class="etiqueta">Categoría</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor" style="font-size:1rem;">' + formatearFecha(detalle.fechaCreacion) + '</div><div class="etiqueta">Creada</div></div>' +
            '</div>' +

            '<h4>Descripción</h4><p>' + escaparHtml(detalle.descripcion) + '</p>' +
            '<h4>Dirección</h4><p>' + escaparHtml(detalle.direccion || 'No registrada.') + '</p>' +

            '<h4>Cliente</h4>' +
            filaDato('Nombre', escaparHtml(detalle.clienteNombre || '—')) +
            filaDato('Correo', escaparHtml(detalle.clienteCorreo || '—')) +
            (detalle.clienteEstadoPago ? filaDato('Estado de pago', '<span class="badge ' + claseBadgeEstadoPago(detalle.clienteEstadoPago) + '">' + escaparHtml(detalle.clienteEstadoPago) + '</span>') : '') +

            '<h4>Asignación actual</h4><p>' + asignacionHtml + '</p>' +

            '<h4>Reportes de solución</h4>' +
            '<div class="lista-miembros-grupo">' +
            (detalle.reportes && detalle.reportes.length
                ? detalle.reportes.map(filaReporte).join('')
                : '<div class="vacio">El técnico todavía no ha enviado un reporte de solución.</div>') +
            '</div>' +

            '<h4>Evidencias adjuntas</h4>' +
            '<div class="lista-miembros-grupo" id="lista-evidencias-detalle">' + htmlCargando('Cargando evidencias...') + '</div>' +

            (opciones.extraHtml ? opciones.extraHtml(detalle) : '');

        await cargarListaAdjuntos(idSolicitud, cuerpo.querySelector('#lista-evidencias-detalle'));

        if (opciones.alRenderizar) {
            opciones.alRenderizar(cuerpo, detalle, cerrar);
        }
    } catch (error) {
        cuerpo.innerHTML = '';
        mostrarError(mensajeError, error);
    }
}

/**
 * Carga y muestra los anuncios globales activos (banner arriba de cada
 * panel) - se llama igual desde los 4 roles, todos los ven. GET /api/anuncios
 * ya filtra por esta_activo=true y no vencidos, asi que aca solo se dibuja
 * lo que venga.
 */
async function cargarAnunciosActivos(idContenedor) {
    const contenedor = document.getElementById(idContenedor);
    if (!contenedor) return;

    try {
        const anuncios = await apiFetch('/api/anuncios');

        contenedor.innerHTML = anuncios.map(function (a) {
            return '<div class="banner-anuncio">' +
                '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 8h.01M11 12h1v4h1"/></svg>' +
                '<div><strong>' + escaparHtml(a.titulo) + '</strong><br>' + escaparHtml(a.mensaje) + '</div>' +
                '</div>';
        }).join('');
    } catch (error) {
        console.error('No se pudieron cargar los anuncios:', error);
    }
}

/**
 * Aclara (porcentaje > 0, hacia blanco) u oscurece (porcentaje < 0, hacia
 * negro) un color hexadecimal - para derivar el "hover" y la version
 * "suave" del color de marca a partir del unico color que elige el
 * Superusuario.
 */
function ajustarColor(hex, porcentaje) {
    const numero = parseInt(hex.slice(1), 16);
    let r = (numero >> 16) & 255, g = (numero >> 8) & 255, b = numero & 255;

    if (porcentaje >= 0) {
        r += (255 - r) * porcentaje;
        g += (255 - g) * porcentaje;
        b += (255 - b) * porcentaje;
    } else {
        r *= (1 + porcentaje);
        g *= (1 + porcentaje);
        b *= (1 + porcentaje);
    }

    return '#' + [r, g, b].map(function (v) {
        return Math.max(0, Math.min(255, Math.round(v))).toString(16).padStart(2, '0');
    }).join('');
}

/**
 * Aplica la configuracion de marca (nombre del negocio, logo, color) a la
 * pagina actual - se llama igual desde las 8 paginas, incluido login.html
 * (por eso usa fetch crudo, no apiFetch: tiene que funcionar SIN sesion).
 * Nunca bloquea la carga de la pagina si falla - es cosmetico, no critico.
 */
async function aplicarConfiguracionSistema() {
    try {
        const respuesta = await fetch(API_BASE + '/api/configuracion');
        if (!respuesta.ok) return;
        const config = await respuesta.json();

        document.querySelectorAll('.nombre-negocio-texto').forEach(function (el) {
            el.textContent = config.nombreNegocio;
        });
        if (document.title.includes('SoporteNet')) {
            document.title = document.title.replace('SoporteNet', config.nombreNegocio);
        }
        if (config.categoria) {
            document.querySelectorAll('.categoria-negocio-texto').forEach(function (el) {
                el.textContent = config.categoria;
            });
        }
        if (config.eslogan) {
            document.querySelectorAll('.eslogan-negocio-texto').forEach(function (el) {
                el.textContent = config.eslogan;
            });
        }

        if (config.logoUrl) {
            const version = config.fechaModificacion ? '?v=' + encodeURIComponent(config.fechaModificacion) : '';
            document.querySelectorAll('.logo-negocio').forEach(function (img) {
                img.src = API_BASE + config.logoUrl + version;
                // .auth-logo y .sidebar-marca .logo img tienen un filtro
                // (brightness(0) invert(1)) pensado para el icono por
                // defecto (lo vuelve blanco solido sobre el fondo oscuro).
                // Aplicado a un logo real subido por el usuario, lo vuelve
                // un cuadrado blanco sin contenido - se quita para el logo
                // personalizado, que se muestra con sus colores reales.
                img.style.filter = 'none';
            });
        }

        if (config.colorPrimario) {
            let estilo = document.getElementById('estilo-marca-dinamico');
            if (!estilo) {
                estilo = document.createElement('style');
                estilo.id = 'estilo-marca-dinamico';
                document.head.appendChild(estilo);
            }
            estilo.textContent = ':root { --color-primario: ' + config.colorPrimario +
                '; --color-primario-hover: ' + ajustarColor(config.colorPrimario, -0.12) +
                '; --color-primario-suave: ' + ajustarColor(config.colorPrimario, 0.92) + '; }';
        }
    } catch (error) {
        console.error('No se pudo cargar la configuracion del sistema:', error);
    }
}

/**
 * Dibuja un grafico de barras horizontal simple - un <div> por barra, con su
 * ancho como porcentaje del valor mas alto del propio conjunto. Sin ninguna
 * libreria (mismo criterio "todo hecho a mano" del resto del proyecto).
 * `datos` es un arreglo de ConteoProjection ({etiqueta, valor}) tal cual
 * vienen del backend. Se usa para comparar cantidades entre pocas categorias
 * (prioridad, categoria, carga de trabajo...).
 */
function graficoBarras(datos) {
    if (!datos || !datos.length || datos.every(function (d) { return Number(d.valor) === 0; })) {
        return '<div class="vacio">Todavía no hay datos suficientes.</div>';
    }
    const maximo = Math.max.apply(null, datos.map(function (d) { return Number(d.valor); }).concat([1]));
    return '<div class="grafico-barras">' +
        datos.map(function (d) {
            const porcentaje = Math.round((Number(d.valor) / maximo) * 100);
            return '<div class="fila-barra">' +
                '<span class="etiqueta-barra">' + escaparHtml(d.etiqueta) + '</span>' +
                '<div class="pista-barra"><div class="relleno-barra" style="width:' + porcentaje + '%"></div></div>' +
                '<span class="valor-barra">' + d.valor + '</span>' +
                '</div>';
        }).join('') +
        '</div>';
}

const PALETA_DONA = ['#0f766e', '#2563eb', '#d97706', '#dc2626', '#7c3aed', '#059669'];

/**
 * Dibuja un grafico de dona (circular) con un conic-gradient de CSS - sin
 * ninguna libreria. `datos` es un arreglo de ConteoProjection. Pensado para
 * pocas categorias (2 a 4) donde la proporcion del total comunica mejor que
 * comparar cantidades (ej. aprobados vs rechazados, tecnicos por nivel).
 */
function graficoDona(datos) {
    const total = (datos || []).reduce(function (suma, d) { return suma + Number(d.valor); }, 0);
    if (!total) {
        return '<div class="vacio">Todavía no hay datos suficientes.</div>';
    }

    let acumulado = 0;
    const segmentos = datos
        .filter(function (d) { return Number(d.valor) > 0; })
        .map(function (d) {
            const indiceColor = datos.indexOf(d);
            const color = PALETA_DONA[indiceColor % PALETA_DONA.length];
            const desde = (acumulado / total) * 360;
            acumulado += Number(d.valor);
            const hasta = (acumulado / total) * 360;
            return color + ' ' + desde + 'deg ' + hasta + 'deg';
        }).join(', ');

    const leyenda = datos.map(function (d, indice) {
        const color = PALETA_DONA[indice % PALETA_DONA.length];
        const porcentaje = Math.round((Number(d.valor) / total) * 100);
        return '<div class="fila-leyenda-dona"><span class="punto-leyenda" style="background:' + color + '"></span>' +
            escaparHtml(d.etiqueta) + ': <strong>' + d.valor + '</strong> (' + porcentaje + '%)</div>';
    }).join('');

    return '<div class="grafico-dona-contenedor">' +
        '<div class="grafico-dona" style="background: conic-gradient(' + segmentos + ');"></div>' +
        '<div class="leyenda-dona">' + leyenda + '</div>' +
        '</div>';
}
