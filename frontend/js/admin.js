(function () {
    if (!exigirSesion('ADMINISTRADOR')) return;

    document.getElementById('texto-usuario').textContent = 'Administrador #' + obtenerIdUsuario();
    activarModalCambiarContrasena();

    let paginaSolicitudes = 0;
    let paginaReportes = 0;
    let paginaClientes = 0;
    let ultimoTotalReportesPendientes = null;

    const mensajeErrorClientes = document.getElementById('mensaje-error-clientes');
    const contenedorTablaClientes = document.getElementById('contenedor-tabla-clientes');
    const paginacionClientes = document.getElementById('paginacion-clientes');

    const filaMetricas = document.getElementById('fila-metricas');

    const mensajeErrorSolicitudes = document.getElementById('mensaje-error-solicitudes');
    const contenedorTablaSolicitudes = document.getElementById('contenedor-tabla-solicitudes');
    const paginacionSolicitudes = document.getElementById('paginacion-solicitudes');
    const filtroEstado = document.getElementById('filtro-estado');

    const mensajeErrorReportes = document.getElementById('mensaje-error-reportes');
    const contenedorTablaReportes = document.getElementById('contenedor-tabla-reportes');
    const paginacionReportes = document.getElementById('paginacion-reportes');

    const panelAsignar = document.getElementById('panel-asignar');
    const idSolicitudAsignar = document.getElementById('id-solicitud-asignar');
    const mensajeErrorAsignar = document.getElementById('mensaje-error-asignar');
    const formAsignar = document.getElementById('form-asignar');
    const selectPrioridad = document.getElementById('select-prioridad');

    // "Técnico" busca en el servidor (miles de registros, no se puede traer
    // todo de antemano); "Grupo" filtra en el cliente (lista chica, se
    // recarga entera cada vez). Antes ambos eran <select> con TODO el
    // contenido - con ~3000 técnicos de prueba, el de técnico era imposible
    // de usar.
    const selectorTecnicoAsignar = activarBusquedaRemota(
        'buscar-tecnico',
        'sugerencias-tecnico',
        function (termino) { return apiFetch('/api/tecnicos/buscar?nombre=' + encodeURIComponent(termino)); }
    );
    const selectorGrupoAsignar = activarSelectorBuscable('buscar-grupo', 'sugerencias-grupo');

    const panelRechazar = document.getElementById('panel-rechazar');
    const idReporteRechazar = document.getElementById('id-reporte-rechazar');
    const mensajeErrorRechazar = document.getElementById('mensaje-error-rechazar');
    const formRechazar = document.getElementById('form-rechazar');

    // ---------- Metricas clave del dashboard (caso de uso 4.3.2) ----------
    // No hay endpoint de conteo dedicado: se reutilizan los mismos endpoints
    // paginados pidiendo size=1 y leyendo totalElements, para no tener que
    // tocar el backend solo para esto.

    async function cargarMetricas() {
        filaMetricas.innerHTML =
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">Pendientes por asignar</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">En proceso</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">Reportes por aprobar</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">Esperando confirmación del cliente</div></div>';

        try {
            const [pendientes, enProceso, resueltas] = await Promise.all([
                apiFetch('/api/solicitudes?estado=' + encodeURIComponent('Pendiente') + '&size=1'),
                apiFetch('/api/solicitudes?estado=' + encodeURIComponent('En Proceso') + '&size=1'),
                apiFetch('/api/solicitudes?estado=' + encodeURIComponent('Resuelta - Pendiente Confirmación del Cliente') + '&size=1')
            ]);

            renderizarMetricas(pendientes.totalElements, enProceso.totalElements, resueltas.totalElements);
        } catch (error) {
            console.error('No se pudieron cargar las métricas:', error);
        }
    }

    function renderizarMetricas(pendientesPorAsignar, enProceso, esperandoConfirmacion) {
        const reportesPendientes = ultimoTotalReportesPendientes !== null ? ultimoTotalReportesPendientes : '—';
        filaMetricas.innerHTML =
            '<div class="tarjeta-metrica"><div class="valor">' + pendientesPorAsignar + '</div><div class="etiqueta">Pendientes por asignar</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">' + enProceso + '</div><div class="etiqueta">En proceso</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">' + reportesPendientes + '</div><div class="etiqueta">Reportes por aprobar</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">' + esperandoConfirmacion + '</div><div class="etiqueta">Esperando confirmación del cliente</div></div>';
    }

    // ---------- Catalogos para los formularios ----------

    async function cargarCatalogos() {
        try {
            const [grupos, prioridades, estados] = await Promise.all([
                apiFetch('/api/grupos-tecnicos'),
                apiFetch('/api/prioridades'),
                apiFetch('/api/estados')
            ]);

            selectorGrupoAsignar.setOpciones(grupos.map(function (g) {
                return { valor: g.idGrupo, etiqueta: g.nombreGrupo };
            }));

            prioridades.forEach(function (p) {
                const opcion = document.createElement('option');
                opcion.value = p.idPrioridad;
                opcion.textContent = p.nombrePrioridad;
                selectPrioridad.appendChild(opcion);
            });

            estados.forEach(function (e) {
                const opcion = document.createElement('option');
                opcion.value = e.nombreEstado;
                opcion.textContent = e.nombreEstado;
                filtroEstado.appendChild(opcion);
            });
        } catch (error) {
            console.error('No se pudieron cargar los catálogos:', error);
        }
    }

    document.querySelectorAll('input[name="tipo-destino"]').forEach(function (radio) {
        radio.addEventListener('change', function () {
            const esTecnico = document.querySelector('input[name="tipo-destino"]:checked').value === 'tecnico';
            document.getElementById('campo-tecnico').classList.toggle('oculto', !esTecnico);
            document.getElementById('campo-grupo').classList.toggle('oculto', esTecnico);
        });
    });

    // ---------- Solicitudes ----------

    function filaSolicitud(s) {
        const claseBadge = claseBadgeEstado(s.estado);
        let acciones = s.estado === 'Cerrada'
            ? '<button data-id="' + s.idSolicitud + '" class="btn-reabrir secundario">Reabrir</button>'
            : '<button data-id="' + s.idSolicitud + '" class="btn-asignar">Asignar</button>';
        return '<tr>' +
            '<td>#' + s.idSolicitud + '</td>' +
            '<td>' + escaparHtml(s.descripcion) + '</td>' +
            '<td><span class="badge ' + claseBadge + '">' + escaparHtml(s.estado) + '</span></td>' +
            '<td>' + escaparHtml(s.prioridad || '—') + '</td>' +
            '<td>' + formatearFecha(s.fechaCreacion) + '</td>' +
            '<td>' + acciones + '</td>' +
            '</tr>';
    }

    async function cargarSolicitudes() {
        ocultarMensaje(mensajeErrorSolicitudes);
        contenedorTablaSolicitudes.innerHTML = htmlCargando();

        try {
            let ruta = '/api/solicitudes?page=' + paginaSolicitudes + '&size=10';
            if (filtroEstado.value) {
                ruta += '&estado=' + encodeURIComponent(filtroEstado.value);
            }

            const pagina = await apiFetch(ruta);

            if (!pagina.content || pagina.content.length === 0) {
                contenedorTablaSolicitudes.innerHTML = '<div class="vacio">No hay solicitudes.</div>';
                paginacionSolicitudes.innerHTML = '';
                return;
            }

            contenedorTablaSolicitudes.innerHTML =
                '<div class="tabla-scroll"><table><thead><tr>' +
                '<th>ID</th><th>Descripción</th><th>Estado</th><th>Prioridad</th><th>Creada</th><th></th>' +
                '</tr></thead><tbody>' + pagina.content.map(filaSolicitud).join('') + '</tbody></table></div>';

            renderizarPaginacion(pagina, paginacionSolicitudes, function (nueva) { paginaSolicitudes = nueva; cargarSolicitudes(); });
            contenedorTablaSolicitudes.querySelectorAll('.btn-asignar').forEach(function (boton) {
                boton.addEventListener('click', function () { abrirPanelAsignar(boton.getAttribute('data-id')); });
            });
            contenedorTablaSolicitudes.querySelectorAll('.btn-reabrir').forEach(function (boton) {
                boton.addEventListener('click', function () { manejarReabrir(boton); });
            });
        } catch (error) {
            contenedorTablaSolicitudes.innerHTML = '';
            mostrarError(mensajeErrorSolicitudes, error);
        }
    }

    async function manejarReabrir(boton) {
        const idSolicitud = boton.getAttribute('data-id');
        const confirmado = await confirmarAccion(
            'Reabrir solicitud #' + idSolicitud,
            'La solicitud vuelve a "En Proceso" para que se le siga dando seguimiento.',
            'Reabrir'
        );
        if (!confirmado) return;

        boton.disabled = true;
        try {
            await apiFetch('/api/solicitudes/' + idSolicitud + '/reapertura', { method: 'POST' });
            cargarSolicitudes();
            cargarMetricas();
        } catch (error) {
            mostrarError(mensajeErrorSolicitudes, error);
            boton.disabled = false;
        }
    }

    function abrirPanelAsignar(idSolicitud) {
        panelAsignar.classList.remove('oculto');
        idSolicitudAsignar.textContent = '#' + idSolicitud;
        formAsignar.dataset.idSolicitud = idSolicitud;
        document.getElementById('motivo-reasignacion').value = '';
        selectPrioridad.value = '';
        selectorTecnicoAsignar.limpiar();
        selectorGrupoAsignar.limpiar();
        ocultarMensaje(mensajeErrorAsignar);
        panelAsignar.scrollIntoView({ behavior: 'smooth' });
    }

    document.getElementById('btn-cancelar-asignar').addEventListener('click', function () {
        panelAsignar.classList.add('oculto');
    });

    formAsignar.addEventListener('submit', async function (evento) {
        evento.preventDefault();
        ocultarMensaje(mensajeErrorAsignar);

        const btnConfirmar = document.getElementById('btn-confirmar-asignar');
        btnConfirmar.disabled = true;
        btnConfirmar.textContent = 'Asignando...';

        try {
            const idSolicitud = formAsignar.dataset.idSolicitud;
            const esTecnico = document.querySelector('input[name="tipo-destino"]:checked').value === 'tecnico';
            const motivo = document.getElementById('motivo-reasignacion').value.trim();
            const prioridad = selectPrioridad.value;
            const idTecnico = selectorTecnicoAsignar.valor();
            const idGrupo = selectorGrupoAsignar.valor();

            if (esTecnico && !idTecnico) {
                throw new Error('Elegí un técnico de la lista de sugerencias (no alcanza con escribir el nombre).');
            }
            if (!esTecnico && !idGrupo) {
                throw new Error('Elegí un grupo de la lista de sugerencias (no alcanza con escribir el nombre).');
            }

            await apiFetch('/api/solicitudes/' + idSolicitud + '/asignaciones', {
                method: 'POST',
                body: JSON.stringify({
                    idTecnico: esTecnico ? Number(idTecnico) : null,
                    idGrupo: esTecnico ? null : Number(idGrupo),
                    idPrioridad: prioridad ? Number(prioridad) : null,
                    motivoReasignacion: motivo || null
                })
            });

            panelAsignar.classList.add('oculto');
            cargarSolicitudes();
            cargarMetricas();
        } catch (error) {
            mostrarError(mensajeErrorAsignar, error);
        } finally {
            btnConfirmar.disabled = false;
            btnConfirmar.textContent = 'Asignar';
        }
    });

    filtroEstado.addEventListener('change', function () { paginaSolicitudes = 0; cargarSolicitudes(); });

    // ---------- Reportes pendientes ----------

    function filaReporte(r) {
        return '<tr>' +
            '<td>#' + r.idReporte + '</td>' +
            '<td>Solicitud #' + r.idSolicitud + '</td>' +
            '<td>Técnico #' + r.idTecnico + '</td>' +
            '<td>' + escaparHtml(r.detalleReporte) + '</td>' +
            '<td>' + formatearFecha(r.fechaEnvio) + '</td>' +
            '<td><div class="acciones-fila">' +
            '<button data-id-solicitud="' + r.idSolicitud + '" class="btn-ver-evidencias secundario btn-compacto">Ver evidencias</button>' +
            '<button data-id="' + r.idReporte + '" class="btn-aprobar btn-compacto">Aprobar</button>' +
            '<button data-id="' + r.idReporte + '" class="btn-rechazar secundario btn-compacto">Rechazar</button>' +
            '</div></td>' +
            '</tr>';
    }

    async function cargarReportesPendientes() {
        ocultarMensaje(mensajeErrorReportes);
        contenedorTablaReportes.innerHTML = htmlCargando();

        try {
            const pagina = await apiFetch('/api/reportes?estado=pendiente&page=' + paginaReportes + '&size=10');
            ultimoTotalReportesPendientes = pagina.totalElements;
            actualizarTarjetaReportesPendientes();

            if (!pagina.content || pagina.content.length === 0) {
                contenedorTablaReportes.innerHTML = '<div class="vacio">No hay reportes pendientes de aprobación.</div>';
                paginacionReportes.innerHTML = '';
                return;
            }

            contenedorTablaReportes.innerHTML =
                '<div class="tabla-scroll"><table><thead><tr>' +
                '<th>ID</th><th>Solicitud</th><th>Técnico</th><th>Detalle</th><th>Enviado</th><th></th>' +
                '</tr></thead><tbody>' + pagina.content.map(filaReporte).join('') + '</tbody></table></div>';

            renderizarPaginacion(pagina, paginacionReportes, function (nueva) { paginaReportes = nueva; cargarReportesPendientes(); });

            contenedorTablaReportes.querySelectorAll('.btn-aprobar').forEach(function (boton) {
                boton.addEventListener('click', function () { manejarAprobar(boton); });
            });
            contenedorTablaReportes.querySelectorAll('.btn-rechazar').forEach(function (boton) {
                boton.addEventListener('click', function () { abrirPanelRechazar(boton.getAttribute('data-id')); });
            });
            contenedorTablaReportes.querySelectorAll('.btn-ver-evidencias').forEach(function (boton) {
                boton.addEventListener('click', function () { abrirModalEvidencias(boton.getAttribute('data-id-solicitud')); });
            });
        } catch (error) {
            contenedorTablaReportes.innerHTML = '';
            mostrarError(mensajeErrorReportes, error);
        }
    }

    /**
     * Antes de esta funcion, el Administrador aprobaba o rechazaba un
     * reporte sin ver ninguna evidencia (ni la del cliente al crear el
     * ticket, ni la del tecnico al resolverlo) - aprobaba a ciegas, solo con
     * el texto del reporte. El backend (AdjuntoController) ya dejaba pasar a
     * ADMINISTRADOR/SUPERUSUARIO para cualquier solicitud, asi que esto es
     * pura pantalla nueva, sin tocar SQL ni backend.
     */
    async function abrirModalEvidencias(idSolicitud) {
        const overlay = document.createElement('div');
        overlay.className = 'overlay-modal';
        overlay.innerHTML =
            '<div class="modal modal-ancho">' +
            '<h3>Evidencias — solicitud #' + idSolicitud + '</h3>' +
            '<p>Del cliente (al crear el ticket) y del técnico (al reportar la solución).</p>' +
            '<div id="mensaje-error-evidencias" class="mensaje-error oculto"></div>' +
            '<div id="lista-evidencias" class="lista-miembros-grupo">Cargando evidencias...</div>' +
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

        const mensajeError = overlay.querySelector('#mensaje-error-evidencias');
        const lista = overlay.querySelector('#lista-evidencias');

        function iconoParaTipo(tipo) {
            if (!tipo) return '📎';
            if (tipo.startsWith('image/')) return '🖼️';
            if (tipo === 'application/pdf') return '📄';
            return '📎';
        }

        try {
            const adjuntos = await apiFetch('/api/solicitudes/' + idSolicitud + '/adjuntos');

            lista.innerHTML = adjuntos.length
                ? adjuntos.map(function (a) {
                    return '<div class="fila-miembro-grupo">' +
                        '<span>' + iconoParaTipo(a.tipoArchivo) + ' <strong>' + escaparHtml(a.nombreArchivo) + '</strong> · ' + escaparHtml(a.tipoArchivo || '') + '</span>' +
                        '<button type="button" class="btn-ver-evidencia secundario" data-id="' + a.idAdjunto + '">Ver</button>' +
                        '</div>';
                }).join('')
                : '<div class="vacio">Esta solicitud no tiene evidencias adjuntas.</div>';

            lista.querySelectorAll('.btn-ver-evidencia').forEach(function (boton) {
                boton.addEventListener('click', async function () {
                    const textoOriginal = boton.textContent;
                    boton.disabled = true;
                    boton.textContent = 'Abriendo...';
                    await abrirArchivoConAutenticacion('/api/adjuntos/' + boton.getAttribute('data-id') + '/archivo');
                    boton.disabled = false;
                    boton.textContent = textoOriginal;
                });
            });
        } catch (error) {
            lista.innerHTML = '';
            mostrarError(mensajeError, error);
        }
    }

    function actualizarTarjetaReportesPendientes() {
        const tarjetas = filaMetricas.querySelectorAll('.tarjeta-metrica');
        if (tarjetas[2] && ultimoTotalReportesPendientes !== null) {
            tarjetas[2].querySelector('.valor').textContent = ultimoTotalReportesPendientes;
        }
    }

    async function manejarAprobar(boton) {
        const idReporte = boton.getAttribute('data-id');
        const confirmado = await confirmarAccion(
            'Aprobar reporte #' + idReporte,
            'El cliente será notificado para confirmar si el problema quedó resuelto.',
            'Aprobar'
        );
        if (!confirmado) return;

        boton.disabled = true;
        try {
            await apiFetch('/api/reportes/' + idReporte + '/aprobacion', {
                method: 'POST',
                body: JSON.stringify({})
            });
            cargarReportesPendientes();
            cargarSolicitudes();
            cargarMetricas();
        } catch (error) {
            mostrarError(mensajeErrorReportes, error);
            boton.disabled = false;
        }
    }

    function abrirPanelRechazar(idReporte) {
        panelRechazar.classList.remove('oculto');
        idReporteRechazar.textContent = '#' + idReporte;
        formRechazar.dataset.idReporte = idReporte;
        document.getElementById('comentario-rechazo').value = '';
        ocultarMensaje(mensajeErrorRechazar);
        panelRechazar.scrollIntoView({ behavior: 'smooth' });
    }

    document.getElementById('btn-cancelar-rechazar').addEventListener('click', function () {
        panelRechazar.classList.add('oculto');
    });

    formRechazar.addEventListener('submit', async function (evento) {
        evento.preventDefault();
        ocultarMensaje(mensajeErrorRechazar);

        const btnConfirmar = document.getElementById('btn-confirmar-rechazar');
        btnConfirmar.disabled = true;
        btnConfirmar.textContent = 'Rechazando...';

        try {
            const idReporte = formRechazar.dataset.idReporte;
            const comentario = document.getElementById('comentario-rechazo').value.trim();

            await apiFetch('/api/reportes/' + idReporte + '/rechazo', {
                method: 'POST',
                body: JSON.stringify({ comentarioRechazo: comentario })
            });

            panelRechazar.classList.add('oculto');
            cargarReportesPendientes();
            cargarSolicitudes();
            cargarMetricas();
        } catch (error) {
            mostrarError(mensajeErrorRechazar, error);
        } finally {
            btnConfirmar.disabled = false;
            btnConfirmar.textContent = 'Rechazar reporte';
        }
    });

    // ---------- Clientes: estado de pago (seccion 7.4) ----------

    const ESTADOS_PAGO = ['al_dia', 'moroso'];

    function filaCliente(c) {
        const opciones = ESTADOS_PAGO.map(function (e) {
            return '<option value="' + e + '" ' + (e === c.estadoPago ? 'selected' : '') + '>' + e + '</option>';
        }).join('');

        return '<tr>' +
            '<td>#' + c.idUsuario + '</td>' +
            '<td>' + escaparHtml(c.nombreUsuario) + '</td>' +
            '<td>' + escaparHtml(c.correo) + '</td>' +
            '<td><span class="badge ' + claseBadgeEstadoPago(c.estadoPago) + '">' + escaparHtml(c.estadoPago) + '</span></td>' +
            '<td><div class="acciones-fila">' +
            '<select class="select-nuevo-estado-pago" data-id="' + c.idUsuario + '">' + opciones + '</select>' +
            '<button class="btn-cambiar-estado-pago secundario" data-id="' + c.idUsuario + '">Cambiar</button>' +
            '</div></td>' +
            '</tr>';
    }

    async function cargarClientes() {
        ocultarMensaje(mensajeErrorClientes);
        contenedorTablaClientes.innerHTML = htmlCargando();

        try {
            const pagina = await apiFetch('/api/clientes?page=' + paginaClientes + '&size=10');

            if (!pagina.content || pagina.content.length === 0) {
                contenedorTablaClientes.innerHTML = '<div class="vacio">No hay clientes registrados.</div>';
                paginacionClientes.innerHTML = '';
                return;
            }

            contenedorTablaClientes.innerHTML =
                '<div class="tabla-scroll"><table><thead><tr>' +
                '<th>ID</th><th>Nombre</th><th>Correo</th><th>Estado de pago</th><th></th>' +
                '</tr></thead><tbody>' + pagina.content.map(filaCliente).join('') + '</tbody></table></div>';

            renderizarPaginacion(pagina, paginacionClientes, function (nueva) { paginaClientes = nueva; cargarClientes(); });

            contenedorTablaClientes.querySelectorAll('.btn-cambiar-estado-pago').forEach(function (boton) {
                boton.addEventListener('click', manejarCambiarEstadoPago);
            });
        } catch (error) {
            contenedorTablaClientes.innerHTML = '';
            mostrarError(mensajeErrorClientes, error);
        }
    }

    async function manejarCambiarEstadoPago(evento) {
        const boton = evento.currentTarget;
        const id = boton.getAttribute('data-id');
        const select = contenedorTablaClientes.querySelector('.select-nuevo-estado-pago[data-id="' + id + '"]');
        const nuevoEstado = select.value;

        boton.disabled = true;
        try {
            await apiFetch('/api/clientes/' + id + '/estado-pago', {
                method: 'POST',
                body: JSON.stringify({ estadoPago: nuevoEstado })
            });
            cargarClientes();
        } catch (error) {
            mostrarError(mensajeErrorClientes, error);
        } finally {
            boton.disabled = false;
        }
    }

    // ---------- Anuncios globales (seccion 2.3) ----------
    // La tabla ya existia en el esquema original pero nunca se habia
    // conectado a ningun procedimiento ni pantalla - el objetivo es que un
    // corte masivo se comunique una sola vez en vez de que cada cliente cree
    // su propio ticket duplicado.

    const mensajeErrorAnuncio = document.getElementById('mensaje-error-anuncio');
    const contenedorTablaAnuncios = document.getElementById('contenedor-tabla-anuncios');

    function filaAnuncio(a) {
        const claseBadge = a.estaActivo ? 'badge-activo' : 'badge-inactivo';
        const textoBadge = a.estaActivo ? 'activo' : 'inactivo';
        const vence = a.fechaExpiracion ? formatearFecha(a.fechaExpiracion) : '—';
        const accion = a.estaActivo
            ? '<button class="btn-desactivar-anuncio secundario" data-id="' + a.idAnuncio + '">Desactivar</button>'
            : '';

        return '<tr>' +
            '<td>#' + a.idAnuncio + '</td>' +
            '<td><strong>' + escaparHtml(a.titulo) + '</strong><br><span style="color:var(--color-texto-suave);font-size:0.85rem;">' + escaparHtml(a.mensaje) + '</span></td>' +
            '<td>' + formatearFecha(a.fechaCreacion) + '</td>' +
            '<td>' + vence + '</td>' +
            '<td><span class="badge ' + claseBadge + '">' + textoBadge + '</span></td>' +
            '<td>' + accion + '</td>' +
            '</tr>';
    }

    async function cargarAnunciosAdmin() {
        try {
            const anuncios = await apiFetch('/api/anuncios/todos');

            contenedorTablaAnuncios.innerHTML = anuncios.length
                ? '<div class="tabla-scroll"><table><thead><tr>' +
                  '<th>ID</th><th>Título / mensaje</th><th>Creado</th><th>Vence</th><th>Estado</th><th></th>' +
                  '</tr></thead><tbody>' + anuncios.map(filaAnuncio).join('') + '</tbody></table></div>'
                : '<div class="vacio">Todavía no hay anuncios.</div>';

            contenedorTablaAnuncios.querySelectorAll('.btn-desactivar-anuncio').forEach(function (boton) {
                boton.addEventListener('click', async function () {
                    boton.disabled = true;
                    try {
                        await apiFetch('/api/anuncios/' + boton.getAttribute('data-id') + '/desactivacion', { method: 'POST' });
                        cargarAnunciosAdmin();
                        cargarAnunciosActivos('banner-anuncios');
                    } catch (error) {
                        mostrarError(mensajeErrorAnuncio, error);
                        boton.disabled = false;
                    }
                });
            });
        } catch (error) {
            contenedorTablaAnuncios.innerHTML = '';
            mostrarError(mensajeErrorAnuncio, error);
        }
    }

    document.getElementById('form-anuncio').addEventListener('submit', async function (evento) {
        evento.preventDefault();
        ocultarMensaje(mensajeErrorAnuncio);

        const btnPublicar = evento.target.querySelector('button[type="submit"]');
        btnPublicar.disabled = true;
        btnPublicar.textContent = 'Publicando...';

        try {
            const titulo = document.getElementById('titulo-anuncio').value.trim();
            const mensaje = document.getElementById('mensaje-anuncio').value.trim();
            const valorExpiracion = document.getElementById('expiracion-anuncio').value;

            await apiFetch('/api/anuncios', {
                method: 'POST',
                body: JSON.stringify({
                    titulo: titulo,
                    mensaje: mensaje,
                    fechaExpiracion: valorExpiracion ? new Date(valorExpiracion).toISOString() : null
                })
            });

            document.getElementById('form-anuncio').reset();
            cargarAnunciosAdmin();
            cargarAnunciosActivos('banner-anuncios');
        } catch (error) {
            mostrarError(mensajeErrorAnuncio, error);
        } finally {
            btnPublicar.disabled = false;
            btnPublicar.textContent = 'Publicar';
        }
    });

    // ---------- Utilidad compartida de paginacion ----------

    function renderizarPaginacion(pagina, contenedor, alCambiar) {
        if (pagina.totalPages <= 1) {
            contenedor.innerHTML = '';
            return;
        }
        const idAnterior = 'ant-' + Math.random().toString(36).slice(2);
        const idSiguiente = 'sig-' + Math.random().toString(36).slice(2);
        contenedor.innerHTML =
            '<button class="secundario" id="' + idAnterior + '" ' + (pagina.first ? 'disabled' : '') + '>Anterior</button>' +
            '<span>Página ' + (pagina.number + 1) + ' de ' + pagina.totalPages + '</span>' +
            '<button class="secundario" id="' + idSiguiente + '" ' + (pagina.last ? 'disabled' : '') + '>Siguiente</button>';

        const btnAnterior = document.getElementById(idAnterior);
        const btnSiguiente = document.getElementById(idSiguiente);
        if (btnAnterior) btnAnterior.addEventListener('click', function () { alCambiar(pagina.number - 1); });
        if (btnSiguiente) btnSiguiente.addEventListener('click', function () { alCambiar(pagina.number + 1); });
    }

    cargarAnunciosActivos('banner-anuncios');
    cargarMetricas();
    cargarCatalogos();
    cargarSolicitudes();
    cargarReportesPendientes();
    cargarClientes();
    cargarAnunciosAdmin();
    activarNavegacionPorTabs();
})();
