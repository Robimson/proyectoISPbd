(function () {
    if (!exigirSesion('TECNICO')) return;

    document.getElementById('texto-usuario').textContent = 'Técnico #' + obtenerIdUsuario();
    activarModalCambiarContrasena();

    let paginaActual = 0;

    const mensajeErrorLista = document.getElementById('mensaje-error-lista');
    const contenedorTabla = document.getElementById('contenedor-tabla');
    const paginacion = document.getElementById('paginacion');

    const panelReportar = document.getElementById('panel-reportar');
    const idSolicitudReportar = document.getElementById('id-solicitud-reportar');
    const mensajeErrorReportar = document.getElementById('mensaje-error-reportar');
    const formReportar = document.getElementById('form-reportar');
    const filaMetricas = document.getElementById('fila-metricas');

    async function cargarMetricas() {
        filaMetricas.innerHTML =
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">Tareas asignadas</div></div>';

        try {
            const pagina = await apiFetch('/api/solicitudes/mis-tareas?size=1');
            filaMetricas.innerHTML =
                '<div class="tarjeta-metrica"><div class="valor">' + pagina.totalElements + '</div><div class="etiqueta">Tareas asignadas</div></div>';
        } catch (error) {
            console.error('No se pudieron cargar las métricas:', error);
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
        let acciones = '<button data-id="' + s.idSolicitud + '" class="btn-adjuntos secundario">Adjuntos</button>';
        if (s.estado === 'En Proceso') {
            acciones += ' <button data-id="' + s.idSolicitud + '" class="btn-reportar">Reportar solución</button>';
        }
        return '<tr>' +
            '<td>#' + s.idSolicitud + '</td>' +
            '<td>' + escaparHtml(s.descripcion) + '</td>' +
            '<td><span class="badge ' + claseBadge + '">' + escaparHtml(s.estado) + '</span></td>' +
            '<td>' + escaparHtml(s.prioridad || '—') + '</td>' +
            '<td>' + formatearFecha(s.fechaCreacion) + '</td>' +
            '<td>' + acciones + '</td>' +
            '</tr>';
    }

    async function cargarMisTareas() {
        ocultarMensaje(mensajeErrorLista);
        contenedorTabla.innerHTML = htmlCargando();

        try {
            const pagina = await apiFetch('/api/solicitudes/mis-tareas?page=' + paginaActual + '&size=10');

            if (!pagina.content || pagina.content.length === 0) {
                contenedorTabla.innerHTML = '<div class="vacio">No tienes tareas asignadas.</div>';
                paginacion.innerHTML = '';
                return;
            }

            const filas = pagina.content.map(filaSolicitud).join('');
            contenedorTabla.innerHTML =
                '<div class="tabla-scroll"><table><thead><tr>' +
                '<th>ID</th><th>Descripción</th><th>Estado</th><th>Prioridad</th><th>Creada</th><th></th>' +
                '</tr></thead><tbody>' + filas + '</tbody></table></div>';

            renderizarPaginacion(pagina);
            contenedorTabla.querySelectorAll('.btn-reportar').forEach(function (boton) {
                boton.addEventListener('click', function () { abrirPanelReportar(boton.getAttribute('data-id')); });
            });
            contenedorTabla.querySelectorAll('.btn-adjuntos').forEach(function (boton) {
                boton.addEventListener('click', function () { abrirAdjuntos(boton.getAttribute('data-id')); });
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

    function abrirPanelReportar(idSolicitud) {
        panelReportar.classList.remove('oculto');
        idSolicitudReportar.textContent = '#' + idSolicitud;
        formReportar.dataset.idSolicitud = idSolicitud;
        document.getElementById('detalle-reporte').value = '';
        ocultarMensaje(mensajeErrorReportar);
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

            await apiFetch('/api/solicitudes/' + idSolicitud + '/reportes', {
                method: 'POST',
                body: JSON.stringify({ detalleReporte: detalleReporte })
            });

            panelReportar.classList.add('oculto');
            cargarMisTareas();
        } catch (error) {
            mostrarError(mensajeErrorReportar, error);
        } finally {
            btnEnviar.disabled = false;
            btnEnviar.textContent = 'Enviar reporte';
        }
    });

    cargarMetricas();
    cargarMisTareas();
    activarNavegacionPorTabs();
})();
