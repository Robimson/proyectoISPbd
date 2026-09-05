(function () {
    if (!exigirSesion('CLIENTE')) return;

    document.getElementById('texto-usuario').textContent = 'Cliente #' + obtenerIdUsuario();
    activarModalCambiarContrasena();

    if (obtenerEstadoPago() === 'moroso') {
        document.getElementById('banner-moroso').classList.remove('oculto');
    }

    let paginaActual = 0;

    const mensajeErrorCrear = document.getElementById('mensaje-error-crear');
    const mensajeErrorLista = document.getElementById('mensaje-error-lista');
    const contenedorTabla = document.getElementById('contenedor-tabla');
    const paginacion = document.getElementById('paginacion');
    const filtroEstado = document.getElementById('filtro-estado');
    const filaMetricas = document.getElementById('fila-metricas');

    async function cargarMetricas() {
        filaMetricas.innerHTML =
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">Pendientes</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">En proceso</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">Cerradas</div></div>';

        try {
            const [pendientes, enProceso, cerradas] = await Promise.all([
                apiFetch('/api/solicitudes?estado=' + encodeURIComponent('Pendiente') + '&size=1'),
                apiFetch('/api/solicitudes?estado=' + encodeURIComponent('En Proceso') + '&size=1'),
                apiFetch('/api/solicitudes?estado=' + encodeURIComponent('Cerrada') + '&size=1')
            ]);

            filaMetricas.innerHTML =
                '<div class="tarjeta-metrica"><div class="valor">' + pendientes.totalElements + '</div><div class="etiqueta">Pendientes</div></div>' +
                '<div class="tarjeta-metrica"><div class="valor">' + enProceso.totalElements + '</div><div class="etiqueta">En proceso</div></div>' +
                '<div class="tarjeta-metrica"><div class="valor">' + cerradas.totalElements + '</div><div class="etiqueta">Cerradas</div></div>';
        } catch (error) {
            console.error('No se pudieron cargar las métricas:', error);
        }
    }

    async function cargarCategorias() {
        try {
            const categorias = await apiFetch('/api/categorias');
            const select = document.getElementById('categoria');
            categorias.forEach(function (c) {
                const opcion = document.createElement('option');
                opcion.value = c.idCategoria;
                opcion.textContent = c.nombreCategoria;
                select.appendChild(opcion);
            });
        } catch (error) {
            // No bloquea la creacion de tickets si el catalogo no carga.
            console.error('No se pudieron cargar las categorías:', error);
        }
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

    /** Precarga el campo Dirección con la última que el cliente uso (sp_crear_solicitud la va guardando en cada ticket). */
    async function cargarDireccionSugerida() {
        try {
            const perfil = await apiFetch('/api/clientes/mi-perfil');
            if (perfil.direccion) {
                document.getElementById('direccion').value = perfil.direccion;
            }
        } catch (error) {
            // No bloquea la creacion de tickets si no se pudo precargar.
            console.error('No se pudo precargar la dirección:', error);
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
        let acciones = s.estado !== 'Cerrada'
            ? '<button data-id="' + s.idSolicitud + '" class="btn-adjuntos secundario">Adjuntos</button>'
            : '';
        if (s.estado === 'Resuelta - Pendiente Confirmación del Cliente') {
            acciones +=
                ' <div class="acciones-fila" style="display:inline-flex;">' +
                '<button data-id="' + s.idSolicitud + '" data-resuelto="true" class="btn-confirmar">Sí, quedó resuelto</button>' +
                '<button data-id="' + s.idSolicitud + '" data-resuelto="false" class="btn-confirmar secundario">Sigue el problema</button>' +
                '</div>';
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

    async function cargarSolicitudes() {
        ocultarMensaje(mensajeErrorLista);
        contenedorTabla.innerHTML = htmlCargando();

        try {
            let ruta = '/api/solicitudes?page=' + paginaActual + '&size=10';
            if (filtroEstado.value) {
                ruta += '&estado=' + encodeURIComponent(filtroEstado.value);
            }

            const pagina = await apiFetch(ruta);

            if (!pagina.content || pagina.content.length === 0) {
                contenedorTabla.innerHTML = '<div class="vacio">No tienes solicitudes todavía.</div>';
                paginacion.innerHTML = '';
                return;
            }

            const filas = pagina.content.map(filaSolicitud).join('');
            contenedorTabla.innerHTML =
                '<div class="tabla-scroll"><table><thead><tr>' +
                '<th>ID</th><th>Descripción</th><th>Estado</th><th>Prioridad</th><th>Creada</th><th></th>' +
                '</tr></thead><tbody>' + filas + '</tbody></table></div>';

            renderizarPaginacion(pagina);
            contenedorTabla.querySelectorAll('.btn-confirmar').forEach(function (boton) {
                boton.addEventListener('click', manejarConfirmar);
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
        if (btnAnterior) btnAnterior.addEventListener('click', function () { paginaActual--; cargarSolicitudes(); });
        if (btnSiguiente) btnSiguiente.addEventListener('click', function () { paginaActual++; cargarSolicitudes(); });
    }

    async function manejarConfirmar(evento) {
        const boton = evento.currentTarget;
        const id = boton.getAttribute('data-id');
        const resuelto = boton.getAttribute('data-resuelto') === 'true';

        boton.disabled = true;
        try {
            await apiFetch('/api/solicitudes/' + id + '/confirmacion', {
                method: 'POST',
                body: JSON.stringify({ problemaResuelto: resuelto })
            });
            cargarSolicitudes();
        } catch (error) {
            mostrarError(mensajeErrorLista, error);
            boton.disabled = false;
        }
    }

    document.getElementById('form-crear').addEventListener('submit', async function (evento) {
        evento.preventDefault();
        ocultarMensaje(mensajeErrorCrear);

        const btnCrear = document.getElementById('btn-crear');
        btnCrear.disabled = true;
        btnCrear.textContent = 'Creando...';

        try {
            const descripcion = document.getElementById('descripcion').value.trim();
            const direccion = document.getElementById('direccion').value.trim();
            const idCategoriaValor = document.getElementById('categoria').value;

            await apiFetch('/api/solicitudes', {
                method: 'POST',
                body: JSON.stringify({
                    descripcion: descripcion,
                    idCategoria: idCategoriaValor ? Number(idCategoriaValor) : null,
                    direccion: direccion
                })
            });

            document.getElementById('form-crear').reset();
            // El reset() del form tambien vacia direccion; se vuelve a poner
            // porque ya quedo guardada como "ultima conocida" en el servidor.
            document.getElementById('direccion').value = direccion;
            paginaActual = 0;
            cargarSolicitudes();
        } catch (error) {
            mostrarError(mensajeErrorCrear, error);
        } finally {
            btnCrear.disabled = false;
            btnCrear.textContent = 'Crear solicitud';
        }
    });

    filtroEstado.addEventListener('change', function () {
        paginaActual = 0;
        cargarSolicitudes();
    });

    cargarAnunciosActivos('banner-anuncios');
    cargarMetricas();
    cargarCategorias();
    cargarEstados();
    cargarDireccionSugerida();
    cargarSolicitudes();
    activarNavegacionPorTabs();
})();
