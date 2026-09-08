// Capa de presentacion: este archivo SOLO habla con el backend y guarda la
// sesion en el navegador. No valida reglas de negocio ni decide estados de
// ticket - eso vive en los procedimientos de PostgreSQL y en el backend
// Java, tal como en el resto del sistema.

const API_BASE = 'http://localhost:8080';

// sessionStorage (no localStorage): la sesion vive solo mientras la
// pestaña/navegador esta abierto. Al cerrarlo, se pierde sola - no hace
// falta esperar a que venza el token para que quede "cerrada".
function guardarSesion(token, idUsuario, rol, estadoPago, idSesion) {
    sessionStorage.setItem('token', token);
    sessionStorage.setItem('idUsuario', idUsuario);
    sessionStorage.setItem('rol', rol);
    if (estadoPago) {
        sessionStorage.setItem('estadoPago', estadoPago);
    } else {
        sessionStorage.removeItem('estadoPago');
    }
    if (idSesion) {
        sessionStorage.setItem('idSesion', idSesion);
    } else {
        sessionStorage.removeItem('idSesion');
    }
}

function limpiarSesion() {
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('idUsuario');
    sessionStorage.removeItem('rol');
    sessionStorage.removeItem('estadoPago');
    sessionStorage.removeItem('idSesion');
}

function obtenerToken() {
    return sessionStorage.getItem('token');
}

function obtenerRol() {
    return sessionStorage.getItem('rol');
}

function obtenerIdUsuario() {
    return sessionStorage.getItem('idUsuario');
}

function obtenerEstadoPago() {
    return sessionStorage.getItem('estadoPago');
}

function obtenerIdSesion() {
    return sessionStorage.getItem('idSesion');
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
    mostrarToast(error.message || String(error), 'error');
}

function ocultarMensaje(elementoMensaje) {
    elementoMensaje.textContent = '';
    elementoMensaje.classList.add('oculto');
}


function contenedorToasts() {
    let contenedor = document.getElementById('contenedor-toasts');
    if (!contenedor) {
        contenedor = document.createElement('div');
        contenedor.id = 'contenedor-toasts';
        document.body.appendChild(contenedor);
    }
    return contenedor;
}

function mostrarToast(mensaje, tipo) {
    tipo = tipo || 'exito';
    const contenedor = contenedorToasts();

    const toast = document.createElement('div');
    toast.className = 'toast toast-' + tipo;
    toast.textContent = mensaje;

    contenedor.appendChild(toast);

    const duracion = tipo === 'error' ? 6000 : 4000;
    setTimeout(function () {
        toast.classList.add('toast-saliendo');
        setTimeout(function () {
            toast.remove();
        }, 300);
    }, duracion);
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


function activarBusquedaRemota(idInput, idSugerencias, fnBuscar, callbacks) {
    const input = document.getElementById(idInput);
    const sugerencias = document.getElementById(idSugerencias);
    const onSeleccionar = (callbacks && callbacks.onSeleccionar) || function () {};
    const onLimpiar = (callbacks && callbacks.onLimpiar) || function () {};
    const renderExtra = (callbacks && callbacks.renderExtra) || function () { return ''; };
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
                            '<span>#' + u.idUsuario + ' · ' + escaparHtml(u.correo) + renderExtra(u) + '</span>' +
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


/*function activarPanelAdjuntos(config) {
    const panel = document.getElementById(config.idPanel);
    const idSolicitudSpan = document.getElementById(config.idSpanSolicitud);
    const mensajeError = document.getElementById(config.idMensajeError);
    const listaDiv = document.getElementById(config.idLista);
    const form = document.getElementById(config.idForm);
    const inputArchivo = document.getElementById(config.idInputArchivo);
    const btnSubir = document.getElementById(config.idBtnSubir);

    let idSolicitudActual = null;*/

function activarPanelAdjuntos(config) {
    const panel = document.getElementById(config.idPanel);
    const idSolicitudSpan = document.getElementById(config.idSpanSolicitud);
    const mensajeError = document.getElementById(config.idMensajeError);
    const listaDiv = document.getElementById(config.idLista);
    const form = document.getElementById(config.idForm);
    const inputArchivo = document.getElementById(config.idInputArchivo);
    const btnSubir = document.getElementById(config.idBtnSubir);

    // Modo solo-lectura: el tecnico ya no sube evidencia desde este panel -
    // la sube al momento de "Reportar solucion" (mientras la solicitud
    // sigue En Proceso, ver tecnico.js). El formulario de subida ni
    // siquiera se muestra, sin importar el estado de la solicitud. El
    // cliente sigue usando este mismo panel sin esta opcion, tal cual antes.
    const soloLectura = config.soloLectura === true;
    if (soloLectura) {
        if (inputArchivo) {
            const contenedorInput = inputArchivo.closest('.campo') || inputArchivo.parentElement;
            if (contenedorInput) contenedorInput.classList.add('oculto');
        }
        if (btnSubir) btnSubir.classList.add('oculto');
    }

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
            mostrarToast('Archivo subido correctamente.', 'exito');
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


function iconoParaTipoAdjunto(tipo) {
    if (!tipo) return '📎';
    if (tipo.startsWith('image/')) return '🖼️';
    if (tipo === 'application/pdf') return '📄';
    return '📎';
}


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
            '<h4>Dirección</h4><p>' + escaparHtml(detalle.direccion || 'No registrada.') +
            (typeof detalle.lat === 'number' && typeof detalle.lng === 'number'
                ? ' · <a href="https://www.google.com/maps?q=' + detalle.lat + ',' + detalle.lng + '" target="_blank" rel="noopener">📍 Ver ubicación en el mapa</a>'
                : '') +
            '</p>' +

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
