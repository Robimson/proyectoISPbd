(function () {
    aplicarConfiguracionSistema();
    if (!exigirSesion('TECNICO')) return;

    document.getElementById('texto-usuario').textContent = 'Técnico #' + obtenerIdUsuario();
    activarModalCambiarContrasena();

    let paginaActual = 0;
    let ultimaPaginaTareas = [];

    const mensajeErrorLista = document.getElementById('mensaje-error-lista');
    const contenedorTabla = document.getElementById('contenedor-tabla');
    const paginacion = document.getElementById('paginacion');
    const filtroEstado = document.getElementById('filtro-estado');

    const panelReportar = document.getElementById('panel-reportar');
    const idSolicitudReportar = document.getElementById('id-solicitud-reportar');
    const mensajeErrorReportar = document.getElementById('mensaje-error-reportar');
    const formReportar = document.getElementById('form-reportar');
    const filaMetricas = document.getElementById('fila-metricas');

    async function cargarMetricas() {
        filaMetricas.innerHTML =
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">🔧 En proceso</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">📨 Pendiente aprobación</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">✅ Resueltas hoy</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">📁 Total cerradas</div></div>';

        try {
            const r = await apiFetch('/api/solicitudes/mis-tareas/resumen');
            filaMetricas.innerHTML =
                '<div class="tarjeta-metrica"><div class="valor">' + r.enProceso + '</div><div class="etiqueta">🔧 En proceso</div></div>' +
                '<div class="tarjeta-metrica"><div class="valor">' + r.pendienteAprobacion + '</div><div class="etiqueta">📨 Pendiente aprobación</div></div>' +
                '<div class="tarjeta-metrica"><div class="valor">' + r.resueltasHoy + '</div><div class="etiqueta">✅ Resueltas hoy</div></div>' +
                '<div class="tarjeta-metrica"><div class="valor">' + r.totalCerradas + '</div><div class="etiqueta">📁 Total cerradas</div></div>';
        } catch (error) {
            console.error('No se pudieron cargar las métricas:', error);
        }
    }

    async function cargarGraficos() {
        const contenedorPrioridad = document.getElementById('grafico-mis-tareas-prioridad');
        const contenedorReportes = document.getElementById('grafico-mis-reportes');
        try {
            const estadisticas = await apiFetch('/api/solicitudes/mis-tareas/estadisticas');
            contenedorPrioridad.innerHTML = graficoBarras(estadisticas.porPrioridad);
            contenedorReportes.innerHTML = graficoDona(estadisticas.misReportes);
        } catch (error) {
            console.error('No se pudieron cargar los gráficos:', error);
            contenedorPrioridad.innerHTML = '';
            contenedorReportes.innerHTML = '';
        }
    }

    const abrirAdjuntos = activarPanelAdjuntos({
        idPanel: 'panel-adjuntos',
        idSpanSolicitud: 'id-solicitud-adjuntos',
        idMensajeError: 'mensaje-error-adjuntos',
        idLista: 'lista-adjuntos',
        idForm: 'form-adjuntos',
        idInputArchivo: 'archivo-adjunto',
        idBtnSubir: 'btn-subir-adjunto',
        idBtnCerrar: 'btn-cerrar-adjuntos'
    });

    function filaSolicitud(s) {
        const claseBadge = claseBadgeEstado(s.estado);
        let acciones = '<button data-id="' + s.idSolicitud + '" class="btn-ver-detalle secundario btn-compacto">Ver detalles</button>';
        if (s.estado !== 'Cerrada') {
            acciones += ' <button data-id="' + s.idSolicitud + '" class="btn-adjuntos secundario btn-compacto">Adjuntos</button>';
        }
        if (s.estado === 'En Proceso') {
            acciones += ' <button data-id="' + s.idSolicitud + '" class="btn-reportar btn-compacto">Reportar solución</button>';
        }
        return '<tr>' +
            '<td>#' + s.idSolicitud + '</td>' +
            '<td>' + escaparHtml(s.descripcion) + '</td>' +
            '<td>' + escaparHtml(s.direccion || '—') + '</td>' +
            '<td><span class="badge ' + claseBadge + '">' + escaparHtml(s.estado) + '</span></td>' +
            '<td>' + escaparHtml(s.prioridad || '—') + '</td>' +
            '<td>' + formatearFecha(s.fechaCreacion) + '</td>' +
            '<td><div class="acciones-fila">' + acciones + '</div></td>' +
            '</tr>';
    }

    async function cargarEstados() {
        try {
            const estados = await apiFetch('/api/estados');
            estados.forEach(function (e) {
                const opcion = document.createElement('option');
                opcion.value = e.nombreEstado;
                opcion.textContent = e.nombreEstado;
                filtroEstado.appendChild(opcion);
            });
        } catch (error) {
            console.error('No se pudieron cargar los estados:', error);
        }
    }

    async function cargarMisTareas() {
        ocultarMensaje(mensajeErrorLista);
        contenedorTabla.innerHTML = htmlCargando();

        try {
            let ruta = '/api/solicitudes/mis-tareas?page=' + paginaActual + '&size=10';
            if (filtroEstado.value) {
                ruta += '&estado=' + encodeURIComponent(filtroEstado.value);
            }
            const pagina = await apiFetch(ruta);

            if (!pagina.content || pagina.content.length === 0) {
                contenedorTabla.innerHTML = '<div class="vacio">No tienes tareas asignadas.</div>';
                paginacion.innerHTML = '';
                return;
            }

            ultimaPaginaTareas = pagina.content;

            const filas = pagina.content.map(filaSolicitud).join('');
            contenedorTabla.innerHTML =
                '<div class="tabla-scroll"><table><thead><tr>' +
                '<th>ID</th><th>Descripción</th><th>Dirección</th><th>Estado</th><th>Prioridad</th><th>Creada</th><th></th>' +
                '</tr></thead><tbody>' + filas + '</tbody></table></div>';

            renderizarPaginacion(pagina);
            contenedorTabla.querySelectorAll('.btn-reportar').forEach(function (boton) {
                boton.addEventListener('click', function () {
                    const idSolicitud = boton.getAttribute('data-id');
                    const solicitud = ultimaPaginaTareas.find(function (s) { return String(s.idSolicitud) === idSolicitud; });
                    abrirPanelReportar(idSolicitud, solicitud);
                });
            });
            contenedorTabla.querySelectorAll('.btn-adjuntos').forEach(function (boton) {
                boton.addEventListener('click', function () { abrirAdjuntos(boton.getAttribute('data-id')); });
            });
            contenedorTabla.querySelectorAll('.btn-ver-detalle').forEach(function (boton) {
                boton.addEventListener('click', function () { abrirModalDetalleSolicitud(boton.getAttribute('data-id')); });
            });
        } catch (error) {
            contenedorTabla.innerHTML = '';
            mostrarError(mensajeErrorLista, error);
        }
    }

    function renderizarPaginacion(pagina) {
        if (pagina.totalPages <= 1) {
            paginacion.innerHTML = '';
            return;
        }
        paginacion.innerHTML =
            '<button class="secundario" id="btn-anterior" ' + (pagina.first ? 'disabled' : '') + '>Anterior</button>' +
            '<span>Página ' + (pagina.number + 1) + ' de ' + pagina.totalPages + '</span>' +
            '<button class="secundario" id="btn-siguiente" ' + (pagina.last ? 'disabled' : '') + '>Siguiente</button>';

        const btnAnterior = document.getElementById('btn-anterior');
        const btnSiguiente = document.getElementById('btn-siguiente');
        if (btnAnterior) btnAnterior.addEventListener('click', function () { paginaActual--; cargarMisTareas(); });
        if (btnSiguiente) btnSiguiente.addEventListener('click', function () { paginaActual++; cargarMisTareas(); });
    }

    // ---------- Evidencia adjunta al reportar la solución ----------
    let archivosReporte = [];
    const inputAdjuntosReporte = document.getElementById('adjuntos-reporte');
    const listaAdjuntosReporte = document.getElementById('lista-adjuntos-reporte');

    function renderizarAdjuntosReporte() {
        if (!archivosReporte.length) {
            listaAdjuntosReporte.innerHTML = '';
            listaAdjuntosReporte.classList.add('oculto');
            return;
        }
        listaAdjuntosReporte.classList.remove('oculto');
        listaAdjuntosReporte.innerHTML = archivosReporte.map(function (archivo, indice) {
            return '<div class="fila-miembro-grupo">' +
                '<span>' + escaparHtml(archivo.name) + '</span>' +
                '<button type="button" class="secundario btn-compacto btn-quitar-adjunto-reporte" data-indice="' + indice + '">Quitar</button>' +
                '</div>';
        }).join('');
        listaAdjuntosReporte.querySelectorAll('.btn-quitar-adjunto-reporte').forEach(function (boton) {
            boton.addEventListener('click', function () {
                archivosReporte.splice(Number(boton.getAttribute('data-indice')), 1);
                renderizarAdjuntosReporte();
            });
        });
    }

    inputAdjuntosReporte.addEventListener('change', function () {
        ocultarMensaje(mensajeErrorReportar);
        Array.from(inputAdjuntosReporte.files).forEach(function (archivo) {
            const yaEstaba = archivosReporte.some(function (a) { return a.name === archivo.name && a.size === archivo.size; });
            if (!yaEstaba) archivosReporte.push(archivo);
        });
        inputAdjuntosReporte.value = '';

        if (archivosReporte.length > 5) {
            mostrarError(mensajeErrorReportar, new Error('Podés adjuntar hasta 5 archivos - se ignoraron los que sobraban.'));
            archivosReporte = archivosReporte.slice(0, 5);
        }
        renderizarAdjuntosReporte();
    });

    const resumenSolicitudReportar = document.getElementById('resumen-solicitud-reportar');

    function abrirPanelReportar(idSolicitud, solicitud) {
        panelReportar.classList.remove('oculto');
        idSolicitudReportar.textContent = '#' + idSolicitud;
        formReportar.dataset.idSolicitud = idSolicitud;
        document.getElementById('detalle-reporte').value = '';
        archivosReporte = [];
        renderizarAdjuntosReporte();
        ocultarMensaje(mensajeErrorReportar);

        // Resumen de la solicitud arriba del formulario - para que el
        // tecnico no tenga que ir y volver a "Ver detalles" en el celular.
        if (solicitud) {
            resumenSolicitudReportar.innerHTML =
                '<div class="fila-miembro-grupo"><span><strong>Descripción:</strong> ' + escaparHtml(solicitud.descripcion) + '</span></div>' +
                '<div class="fila-miembro-grupo"><span><strong>Dirección:</strong> ' + escaparHtml(solicitud.direccion || '—') + '</span></div>' +
                '<div class="fila-miembro-grupo"><span><strong>Prioridad:</strong> ' + escaparHtml(solicitud.prioridad || '—') + '</span></div>';
            resumenSolicitudReportar.classList.remove('oculto');
        } else {
            resumenSolicitudReportar.innerHTML = '';
            resumenSolicitudReportar.classList.add('oculto');
        }

        panelReportar.scrollIntoView({ behavior: 'smooth' });
    }

    document.getElementById('btn-cancelar-reporte').addEventListener('click', function () {
        panelReportar.classList.add('oculto');
    });

    formReportar.addEventListener('submit', async function (evento) {
        evento.preventDefault();
        ocultarMensaje(mensajeErrorReportar);

        const btnEnviar = document.getElementById('btn-enviar-reporte');
        btnEnviar.disabled = true;
        btnEnviar.textContent = 'Enviando...';

        try {
            const idSolicitud = formReportar.dataset.idSolicitud;
            const detalleReporte = document.getElementById('detalle-reporte').value.trim();
            const archivos = archivosReporte;

            await apiFetch('/api/solicitudes/' + idSolicitud + '/reportes', {
                method: 'POST',
                body: JSON.stringify({ detalleReporte: detalleReporte })
            });

            // El reporte ya quedo enviado aunque alguna evidencia falle al
            // subir - perder el reporte por eso seria peor que avisar aparte.
            if (archivos.length) {
                btnEnviar.textContent = 'Subiendo evidencia...';
                const resultados = await Promise.allSettled(
                    archivos.map(function (archivo) { return subirAdjunto(idSolicitud, archivo); })
                );
                const fallidos = resultados.filter(function (r) { return r.status === 'rejected'; });
                if (fallidos.length) {
                    mostrarError(mensajeErrorReportar, new Error(
                        'El reporte se envió, pero ' + fallidos.length +
                        ' archivo(s) no se pudieron subir. Podés agregarlos desde "Adjuntos".'
                    ));
                }
            }

            panelReportar.classList.add('oculto');
            cargarMisTareas();
            mostrarToast('Reporte enviado correctamente.', 'exito');
        } catch (error) {
            mostrarError(mensajeErrorReportar, error);
        } finally {
            btnEnviar.disabled = false;
            btnEnviar.textContent = 'Enviar reporte';
        }
    });

    filtroEstado.addEventListener('change', function () {
        paginaActual = 0;
        cargarMisTareas();
    });

    cargarAnunciosActivos('banner-anuncios');
    cargarMetricas();
    cargarGraficos();
    cargarEstados();
    cargarMisTareas();
    activarNavegacionPorTabs();
})();
