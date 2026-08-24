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
    const selectTecnico = document.getElementById('select-tecnico');
    const selectGrupo = document.getElementById('select-grupo');
    const selectPrioridad = document.getElementById('select-prioridad');

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
            const [tecnicos, grupos, prioridades, estados] = await Promise.all([
                apiFetch('/api/tecnicos'),
                apiFetch('/api/grupos-tecnicos'),
                apiFetch('/api/prioridades'),
                apiFetch('/api/estados')
            ]);

            selectTecnico.innerHTML = tecnicos.map(function (t) {
                return '<option value="' + t.idUsuario + '">' + escaparHtml(t.nombreUsuario) + ' (' + escaparHtml(t.nivel) + ')</option>';
            }).join('') || '<option value="">No hay técnicos habilitados</option>';

            selectGrupo.innerHTML = grupos.map(function (g) {
                return '<option value="' + g.idGrupo + '">' + escaparHtml(g.nombreGrupo) + '</option>';
            }).join('') || '<option value="">No hay grupos creados</option>';

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

            await apiFetch('/api/solicitudes/' + idSolicitud + '/asignaciones', {
                method: 'POST',
                body: JSON.stringify({
                    idTecnico: esTecnico ? Number(selectTecnico.value) : null,
                    idGrupo: esTecnico ? null : Number(selectGrupo.value),
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
            '<button data-id="' + r.idReporte + '" class="btn-aprobar">Aprobar</button>' +
            '<button data-id="' + r.idReporte + '" class="btn-rechazar secundario">Rechazar</button>' +
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
        } catch (error) {
            contenedorTablaReportes.innerHTML = '';
            mostrarError(mensajeErrorReportes, error);
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

    cargarMetricas();
    cargarCatalogos();
    cargarSolicitudes();
    cargarReportesPendientes();
    cargarClientes();
    activarNavegacionPorTabs();
})();
