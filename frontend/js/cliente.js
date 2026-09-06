(function () {
    aplicarConfiguracionSistema();
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
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">🕐 Pendientes</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">🔧 En proceso</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">✅ Cerradas</div></div>';

        try {
            const [pendientes, enProceso, cerradas] = await Promise.all([
                apiFetch('/api/solicitudes?estado=' + encodeURIComponent('Pendiente') + '&size=1'),
                apiFetch('/api/solicitudes?estado=' + encodeURIComponent('En Proceso') + '&size=1'),
                apiFetch('/api/solicitudes?estado=' + encodeURIComponent('Cerrada') + '&size=1')
            ]);

            filaMetricas.innerHTML =
                '<div class="tarjeta-metrica"><div class="valor">' + pendientes.totalElements + '</div><div class="etiqueta">🕐 Pendientes</div></div>' +
                '<div class="tarjeta-metrica"><div class="valor">' + enProceso.totalElements + '</div><div class="etiqueta">🔧 En proceso</div></div>' +
                '<div class="tarjeta-metrica"><div class="valor">' + cerradas.totalElements + '</div><div class="etiqueta">✅ Cerradas</div></div>';
        } catch (error) {
            console.error('No se pudieron cargar las métricas:', error);
        }
    }

    async function cargarGraficos() {
        const contenedorEstado = document.getElementById('grafico-mis-solicitudes-estado');
        const contenedorCategoria = document.getElementById('grafico-mis-solicitudes-categoria');
        try {
            const estadisticas = await apiFetch('/api/solicitudes/mis-estadisticas');
            contenedorEstado.innerHTML = graficoDona(estadisticas.porEstado);
            contenedorCategoria.innerHTML = graficoBarras(estadisticas.porCategoria);
        } catch (error) {
            console.error('No se pudieron cargar los gráficos:', error);
            contenedorEstado.innerHTML = '';
            contenedorCategoria.innerHTML = '';
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
        let acciones = '<button data-id="' + s.idSolicitud + '" class="btn-ver-detalle secundario btn-compacto">Ver detalles</button>';
        if (s.estado !== 'Cerrada') {
            acciones += ' <button data-id="' + s.idSolicitud + '" class="btn-adjuntos secundario btn-compacto">Adjuntos</button>';
        }
        if (s.estado === 'Resuelta - Pendiente Confirmación del Cliente') {
            acciones +=
                ' <button data-id="' + s.idSolicitud + '" data-resuelto="true" class="btn-confirmar btn-compacto">Sí, quedó resuelto</button>' +
                '<button data-id="' + s.idSolicitud + '" data-resuelto="false" class="btn-confirmar secundario btn-compacto">Sigue el problema</button>';
        }
        return '<tr>' +
            '<td>#' + s.idSolicitud + '</td>' +
            '<td>' + escaparHtml(s.descripcion) + '</td>' +
            '<td><span class="badge ' + claseBadge + '">' + escaparHtml(s.estado) + '</span></td>' +
            '<td>' + escaparHtml(s.prioridad || '—') + '</td>' +
            '<td>' + formatearFecha(s.fechaCreacion) + '</td>' +
            '<td><div class="acciones-fila">' + acciones + '</div></td>' +
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

    // El input nativo <input type="file" multiple> REEMPLAZA toda la
    // seleccion cada vez que se abre el dialogo - no la suma. Eso es normal
    // en todos los navegadores (no un bug), pero como UX confunde: el
    // usuario espera poder ir agregando de a un archivo. Por eso se
    // acumulan en este arreglo propio en vez de leer input.files
    // directamente al enviar el formulario.
    let archivosNuevaSolicitud = [];
    const inputAdjuntosNueva = document.getElementById('adjuntos-nueva-solicitud');
    const listaAdjuntosNueva = document.getElementById('lista-adjuntos-nueva-solicitud');

    function renderizarAdjuntosNuevaSolicitud() {
        if (!archivosNuevaSolicitud.length) {
            listaAdjuntosNueva.innerHTML = '';
            listaAdjuntosNueva.classList.add('oculto');
            return;
        }
        listaAdjuntosNueva.classList.remove('oculto');
        listaAdjuntosNueva.innerHTML = archivosNuevaSolicitud.map(function (archivo, indice) {
            return '<div class="fila-miembro-grupo">' +
                '<span>' + escaparHtml(archivo.name) + '</span>' +
                '<button type="button" class="secundario btn-compacto btn-quitar-adjunto-nuevo" data-indice="' + indice + '">Quitar</button>' +
                '</div>';
        }).join('');
        listaAdjuntosNueva.querySelectorAll('.btn-quitar-adjunto-nuevo').forEach(function (boton) {
            boton.addEventListener('click', function () {
                archivosNuevaSolicitud.splice(Number(boton.getAttribute('data-indice')), 1);
                renderizarAdjuntosNuevaSolicitud();
            });
        });
    }

    inputAdjuntosNueva.addEventListener('change', function () {
        ocultarMensaje(mensajeErrorCrear);
        Array.from(inputAdjuntosNueva.files).forEach(function (archivo) {
            const yaEstaba = archivosNuevaSolicitud.some(function (a) { return a.name === archivo.name && a.size === archivo.size; });
            if (!yaEstaba) archivosNuevaSolicitud.push(archivo);
        });
        // Se limpia el input para que el proximo dialogo arranque vacio -
        // la lista propia de abajo es la que manda, no el input nativo.
        inputAdjuntosNueva.value = '';

        if (archivosNuevaSolicitud.length > 5) {
            mostrarError(mensajeErrorCrear, new Error('Podés adjuntar hasta 5 archivos - se ignoraron los que sobraban.'));
            archivosNuevaSolicitud = archivosNuevaSolicitud.slice(0, 5);
        }
        renderizarAdjuntosNuevaSolicitud();
    });

    // ---------- Selector de dirección con mapa ----------
    let mapaSelector, marcadorSelector, ultimaSeleccionMapa = null;
    const overlayMapa = document.getElementById('overlay-mapa');
    const previewDireccionMapa = document.getElementById('preview-direccion-mapa');
    const btnConfirmarMapa = document.getElementById('btn-confirmar-mapa');
    const CENTRO_INICIAL_MAPA = [-2.170998, -79.922359]; // Ajustar a la ciudad de tus clientes

    document.getElementById('btn-abrir-mapa').addEventListener('click', function () {
        overlayMapa.classList.remove('oculto');
        if (!mapaSelector) {
            mapaSelector = L.map('mapa-selector').setView(CENTRO_INICIAL_MAPA, 13);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19,
                attribution: '&copy; OpenStreetMap'
            }).addTo(mapaSelector);
            mapaSelector.on('click', function (e) { marcarPuntoMapa(e.latlng.lat, e.latlng.lng); });
        } else {
            setTimeout(function () { mapaSelector.invalidateSize(); }, 50);
        }
    });

    document.getElementById('btn-cerrar-mapa').addEventListener('click', function () {
        overlayMapa.classList.add('oculto');
    });

    document.getElementById('btn-buscar-mapa').addEventListener('click', buscarDireccionMapa);
    document.getElementById('buscador-mapa').addEventListener('keydown', function (e) {
        if (e.key === 'Enter') { e.preventDefault(); buscarDireccionMapa(); }
    });

    document.getElementById('btn-mi-ubicacion').addEventListener('click', function () {
        if (!navigator.geolocation) { alert('Tu navegador no soporta geolocalización.'); return; }
        navigator.geolocation.getCurrentPosition(
            function (pos) { marcarPuntoMapa(pos.coords.latitude, pos.coords.longitude); },
            function () { alert('No se pudo obtener tu ubicación. Revisá los permisos del navegador.'); }
        );
    });

    btnConfirmarMapa.addEventListener('click', function () {
        if (!ultimaSeleccionMapa) return;
        document.getElementById('direccion').value = ultimaSeleccionMapa.direccionTexto;
        document.getElementById('lat').value = ultimaSeleccionMapa.lat;
        document.getElementById('lng').value = ultimaSeleccionMapa.lng;
        overlayMapa.classList.add('oculto');
    });

    async function marcarPuntoMapa(lat, lng) {
        if (marcadorSelector) mapaSelector.removeLayer(marcadorSelector);
        marcadorSelector = L.marker([lat, lng]).addTo(mapaSelector);
        mapaSelector.setView([lat, lng], mapaSelector.getZoom() < 15 ? 16 : mapaSelector.getZoom());
        previewDireccionMapa.textContent = 'Buscando dirección...';
        btnConfirmarMapa.disabled = true;
        try {
            const resp = await fetch('https://nominatim.openstreetmap.org/reverse?format=json&lat=' + lat + '&lon=' + lng);
            const data = await resp.json();
            const texto = data.display_name || ('Lat ' + lat.toFixed(6) + ', Lng ' + lng.toFixed(6));
            ultimaSeleccionMapa = { lat: lat, lng: lng, direccionTexto: texto };
            previewDireccionMapa.textContent = texto;
        } catch (error) {
            ultimaSeleccionMapa = { lat: lat, lng: lng, direccionTexto: 'Lat ' + lat.toFixed(6) + ', Lng ' + lng.toFixed(6) };
            previewDireccionMapa.textContent = ultimaSeleccionMapa.direccionTexto;
        }
        btnConfirmarMapa.disabled = false;
    }

    async function buscarDireccionMapa() {
        const q = document.getElementById('buscador-mapa').value.trim();
        if (!q) return;
        try {
            const resp = await fetch('https://nominatim.openstreetmap.org/search?format=json&q=' + encodeURIComponent(q) + '&limit=1');
            const data = await resp.json();
            if (data.length === 0) {
                previewDireccionMapa.textContent = 'No se encontró esa dirección. Probá con más detalle o marcá el punto en el mapa.';
                return;
            }
            marcarPuntoMapa(parseFloat(data[0].lat), parseFloat(data[0].lon));
        } catch (error) {
            previewDireccionMapa.textContent = 'Ocurrió un error buscando la dirección.';
        }
    }

    document.getElementById('form-crear').addEventListener('submit', async function (evento) {
        evento.preventDefault();
        ocultarMensaje(mensajeErrorCrear);

        const btnCrear = document.getElementById('btn-crear');
        btnCrear.disabled = true;
        btnCrear.textContent = 'Creando...';

        try {
            const creada = await apiFetch('/api/solicitudes', {
                method: 'POST',
                body: JSON.stringify({
                    descripcion: descripcion,
                    idCategoria: idCategoriaValor ? Number(idCategoriaValor) : null,
                    direccion: direccion,
                    lat: document.getElementById('lat').value ? Number(document.getElementById('lat').value) : null,
                    lng: document.getElementById('lng').value ? Number(document.getElementById('lng').value) : null
                })
            });

            // La solicitud ya quedo creada aunque alguna evidencia falle al
            // subir - perder el ticket por eso seria peor que avisar aparte,
            // asi que el error de un adjunto no bloquea la creacion.
            if (archivos.length) {
                btnCrear.textContent = 'Subiendo evidencia...';
                const resultados = await Promise.allSettled(
                    archivos.map(function (archivo) { return subirAdjunto(creada.idSolicitud, archivo); })
                );
                const fallidos = resultados.filter(function (r) { return r.status === 'rejected'; });
                if (fallidos.length) {
                    mostrarError(mensajeErrorCrear, new Error(
                        'La solicitud #' + creada.idSolicitud + ' se creó, pero ' + fallidos.length +
                        ' archivo(s) no se pudieron subir. Podés agregarlos después con el botón "Adjuntos" en la lista.'
                    ));
                }
            }

            document.getElementById('form-crear').reset();
            // El reset() del form tambien vacia direccion; se vuelve a poner
            // porque ya quedo guardada como "ultima conocida" en el servidor.
            document.getElementById('direccion').value = direccion;
            // reset() no vacia el arreglo propio de adjuntos acumulados.
            archivosNuevaSolicitud = [];
            renderizarAdjuntosNuevaSolicitud();
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
    cargarGraficos();
    cargarCategorias();
    cargarEstados();
    cargarDireccionSugerida();
    cargarSolicitudes();
    activarNavegacionPorTabs();
})();
