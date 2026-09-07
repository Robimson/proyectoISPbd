(function () {
    aplicarConfiguracionSistema();
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

    // Grupos y prioridades se guardan tal cual llegan (no en un selector ya
    // armado) porque el formulario de Asignar/Reasignar ahora vive DENTRO del
    // modal de "Ver detalles" - se recrea cada vez que se abre, asi que cada
    // apertura arma sus propios selectores con este mismo catalogo.
    let catalogoGrupos = [];
    let catalogoPrioridades = [];

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
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">🕐 Pendientes por asignar</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">🔧 En proceso</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">📨 Reportes por aprobar</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">⏳ Esperando confirmación del cliente</div></div>';

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
        // Umbrales de color solo en las 2 tarjetas que son cola de trabajo
        // real del admin (algo que el ESPERA que haga) - las otras 2 son
        // informativas, no necesitan alarmar.
        const claseValorPendientes = claseAlertaPorValor(pendientesPorAsignar, 6, 15);
        const claseValorReportes = claseAlertaPorValor(reportesPendientes, 4, 10);
        filaMetricas.innerHTML =
            '<div class="tarjeta-metrica"><div class="valor ' + claseValorPendientes + '">' + pendientesPorAsignar + '</div><div class="etiqueta">🕐 Pendientes por asignar</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">' + enProceso + '</div><div class="etiqueta">🔧 En proceso</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor ' + claseValorReportes + '">' + reportesPendientes + '</div><div class="etiqueta">📨 Reportes por aprobar</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">' + esperandoConfirmacion + '</div><div class="etiqueta">⏳ Esperando confirmación del cliente</div></div>';
    }

    async function cargarGraficos() {
        const contenedores = {
            estado: document.getElementById('grafico-solicitudes-estado'),
            prioridad: document.getElementById('grafico-solicitudes-prioridad'),
            categoria: document.getElementById('grafico-solicitudes-categoria'),
            aprobacion: document.getElementById('grafico-tasa-aprobacion'),
            carga: document.getElementById('grafico-carga-tecnicos')
        };
        try {
            const estadisticas = await apiFetch('/api/dashboard/administrador');
            contenedores.estado.innerHTML = graficoBarras(estadisticas.porEstado);
            contenedores.prioridad.innerHTML = graficoBarras(estadisticas.porPrioridad);
            contenedores.categoria.innerHTML = graficoBarras(estadisticas.porCategoria);
            contenedores.aprobacion.innerHTML = graficoDona(estadisticas.tasaAprobacionReportes);
            contenedores.carga.innerHTML = graficoBarras(estadisticas.cargaTecnicos);
        } catch (error) {
            console.error('No se pudieron cargar los gráficos:', error);
            Object.values(contenedores).forEach(function (c) { c.innerHTML = ''; });
        }
    }

    // ---------- Catalogos para los formularios ----------

    async function cargarCatalogos() {
        try {
            const [grupos, prioridades, estados] = await Promise.all([
                apiFetch('/api/grupos-tecnicos'),
                apiFetch('/api/prioridades'),
                apiFetch('/api/estados')
            ]);

            catalogoGrupos = grupos;
            catalogoPrioridades = prioridades;

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

    // ---------- Solicitudes ----------

    function filaSolicitud(s) {
        const claseBadge = claseBadgeEstado(s.estado);
        let acciones = '<button data-id="' + s.idSolicitud + '" class="btn-ver-detalle secundario btn-compacto">Ver detalles</button>';
        if (s.estado === 'Cerrada') {
            acciones += ' <button data-id="' + s.idSolicitud + '" class="btn-reabrir secundario btn-compacto">Reabrir</button>';
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
            contenedorTablaSolicitudes.querySelectorAll('.btn-reabrir').forEach(function (boton) {
                boton.addEventListener('click', function () { manejarReabrir(boton); });
            });
            contenedorTablaSolicitudes.querySelectorAll('.btn-ver-detalle').forEach(function (boton) {
                boton.addEventListener('click', function () {
                    abrirModalDetalleSolicitud(boton.getAttribute('data-id'), {
                        extraHtml: htmlFormularioAsignar,
                        alRenderizar: wireFormularioAsignar
                    });
                });
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
            cargarGraficos();
            mostrarToast('Solicitud #' + idSolicitud + ' reabierta.', 'exito');
        } catch (error) {
            mostrarError(mensajeErrorSolicitudes, error);
            boton.disabled = false;
        }
    }

    
    // "Pendiente Aprobación" no esta aca a proposito: mientras hay un
    // reporte esperando revision, primero hay que aprobarlo o rechazarlo
    // (el backend tambien lo bloquea, esto es solo para no mostrar un
    // formulario que despues va a fallar al enviarlo).
    const ESTADOS_ASIGNABLES = ['Pendiente', 'En Proceso'];

   
    function htmlFormularioAsignar(detalle) {
        if (ESTADOS_ASIGNABLES.indexOf(detalle.estado) === -1) return '';

        const esReasignacion = detalle.estado !== 'Pendiente';
        const titulo = esReasignacion ? 'Reasignar' : 'Asignar';

        return '<h4>' + titulo + '</h4>' +
            (esReasignacion
                ? '<p class="subtitulo" style="margin-top:-6px;">Usa esto solo para escalar - por ejemplo, si el reporte del técnico dice que el problema es más grande de lo que puede resolver solo.</p>'
                : '') +
            '<div id="mensaje-error-asignar-modal" class="mensaje-error oculto"></div>' +
            '<form id="form-asignar-modal">' +
            '<div class="campo">' +
            '<label>Asignar a</label>' +
            '<label style="font-weight: normal; display: inline-block; margin-right: 16px;">' +
            '<input type="radio" name="tipo-destino-modal" value="tecnico" checked> Técnico</label>' +
            '<label style="font-weight: normal; display: inline-block;">' +
            '<input type="radio" name="tipo-destino-modal" value="grupo"> Grupo técnico</label>' +
            '</div>' +
            '<div class="campo" id="campo-tecnico-modal">' +
            '<label for="buscar-tecnico-modal">Técnico</label>' +
            '<input type="text" id="buscar-tecnico-modal" placeholder="Buscar por nombre o correo (opcional)..." autocomplete="off">' +
            '<p class="subtitulo" style="margin:4px 0;">Se muestran los 8 más libres. Hacé clic en uno para elegirlo.</p>' +
            '<div id="lista-tecnicos-modal" class="lista-miembros-grupo"></div>' +
            '</div>' +
            '<div class="campo oculto" id="campo-grupo-modal">' +
            '<label for="buscar-grupo-modal">Grupo técnico</label>' +
            '<input type="text" id="buscar-grupo-modal" placeholder="Buscar grupo (opcional)..." autocomplete="off">' +
            '<div id="lista-grupos-modal" class="lista-miembros-grupo"></div>' +
            '</div>' +
            '<div class="campo" style="max-width: 240px;">' +
            '<label for="select-prioridad-modal">Prioridad (opcional)</label>' +
            '<select id="select-prioridad-modal"><option value="">Sin cambiar</option></select>' +
            '</div>' +
            (esReasignacion
                ? '<div class="campo"><label for="motivo-reasignacion-modal">Motivo de la reasignación</label>' +
                  '<input type="text" id="motivo-reasignacion-modal" required placeholder="¿Por qué hace falta reasignar?"></div>'
                : '') +
            '<button type="submit" id="btn-confirmar-asignar-modal">' + titulo + '</button>' +
            '</form>';
    }

   
    function wireFormularioAsignar(cuerpo, detalle, cerrar) {
        const form = cuerpo.querySelector('#form-asignar-modal');
        if (!form) return;

        const mensajeErrorAsignarModal = cuerpo.querySelector('#mensaje-error-asignar-modal');
        const campoTecnico = cuerpo.querySelector('#campo-tecnico-modal');
        const campoGrupo = cuerpo.querySelector('#campo-grupo-modal');
        const selectPrioridadModal = cuerpo.querySelector('#select-prioridad-modal');

        catalogoPrioridades.forEach(function (p) {
            const opcion = document.createElement('option');
            opcion.value = p.idPrioridad;
            opcion.textContent = p.nombrePrioridad;
            selectPrioridadModal.appendChild(opcion);
        });

        let idTecnicoElegido = null;
        let idGrupoElegido = null;

        function colorPorCarga(carga) {
            if (carga === 0) return 'var(--color-exito)';
            if (carga <= 5) return 'var(--color-advertencia)';
            return 'var(--color-peligro)';
        }

        const listaTecnicosModal = cuerpo.querySelector('#lista-tecnicos-modal');
        const inputBuscarTecnicoModal = cuerpo.querySelector('#buscar-tecnico-modal');

        async function cargarTecnicosModal(termino) {
            listaTecnicosModal.innerHTML = htmlCargando();
            try {
                const ruta = '/api/tecnicos/buscar' + (termino ? '?nombre=' + encodeURIComponent(termino) : '');
                const tecnicos = await apiFetch(ruta);

                listaTecnicosModal.innerHTML = tecnicos.length
                    ? tecnicos.map(function (t) {
                        const seleccionado = String(t.idUsuario) === String(idTecnicoElegido);
                        return '<div class="fila-miembro-grupo fila-seleccionable' + (seleccionado ? ' seleccionada' : '') + '" data-id="' + t.idUsuario + '">' +
                            '<span><strong>' + escaparHtml(t.nombreUsuario) + '</strong> · ' + escaparHtml(t.correo) +
                            ' · <span style="color:' + colorPorCarga(t.cargaActual) + '; font-weight:600;">' +
                            t.cargaActual + ' activa' + (t.cargaActual === 1 ? '' : 's') + '</span></span>' +
                            '</div>';
                    }).join('')
                    : '<div class="vacio">Sin coincidencias.</div>';

                listaTecnicosModal.querySelectorAll('[data-id]').forEach(function (fila) {
                    fila.addEventListener('click', function () {
                        idTecnicoElegido = fila.getAttribute('data-id');
                        listaTecnicosModal.querySelectorAll('.fila-seleccionable').forEach(function (f) { f.classList.remove('seleccionada'); });
                        fila.classList.add('seleccionada');
                    });
                });
            } catch (error) {
                listaTecnicosModal.innerHTML = '';
                mostrarError(mensajeErrorAsignarModal, error);
            }
        }

        let temporizadorTecnicoModal = null;
        inputBuscarTecnicoModal.addEventListener('input', function () {
            clearTimeout(temporizadorTecnicoModal);
            temporizadorTecnicoModal = setTimeout(function () {
                cargarTecnicosModal(inputBuscarTecnicoModal.value.trim());
            }, 300);
        });
        cargarTecnicosModal('');

        const listaGruposModal = cuerpo.querySelector('#lista-grupos-modal');
        const inputBuscarGrupoModal = cuerpo.querySelector('#buscar-grupo-modal');

        function renderizarGruposModal(filtro) {
            const f = (filtro || '').trim().toLowerCase();
            const grupos = f
                ? catalogoGrupos.filter(function (g) { return g.nombreGrupo.toLowerCase().includes(f); })
                : catalogoGrupos;

            listaGruposModal.innerHTML = grupos.length
                ? grupos.map(function (g) {
                    const vacio = !g.totalTecnicos || g.totalTecnicos === 0;
                    const seleccionado = String(g.idGrupo) === String(idGrupoElegido);
                    return '<div class="fila-miembro-grupo fila-seleccionable' + (seleccionado ? ' seleccionada' : '') + '" data-id="' + g.idGrupo + '">' +
                        '<span><strong>' + escaparHtml(g.nombreGrupo) + '</strong> · ' +
                        (vacio
                            ? '<span style="color:var(--color-peligro); font-weight:600;">vacío, sin técnicos</span>'
                            : g.totalTecnicos + ' técnico' + (g.totalTecnicos === 1 ? '' : 's')) +
                        '</span></div>';
                }).join('')
                : '<div class="vacio">No hay grupos técnicos creados todavía.</div>';

            listaGruposModal.querySelectorAll('[data-id]').forEach(function (fila) {
                fila.addEventListener('click', function () {
                    idGrupoElegido = fila.getAttribute('data-id');
                    listaGruposModal.querySelectorAll('.fila-seleccionable').forEach(function (f) { f.classList.remove('seleccionada'); });
                    fila.classList.add('seleccionada');
                });
            });
        }

        inputBuscarGrupoModal.addEventListener('input', function () { renderizarGruposModal(inputBuscarGrupoModal.value); });
        renderizarGruposModal('');

        cuerpo.querySelectorAll('input[name="tipo-destino-modal"]').forEach(function (radio) {
            radio.addEventListener('change', function () {
                const esTecnico = cuerpo.querySelector('input[name="tipo-destino-modal"]:checked').value === 'tecnico';
                campoTecnico.classList.toggle('oculto', !esTecnico);
                campoGrupo.classList.toggle('oculto', esTecnico);
            });
        });

        form.addEventListener('submit', async function (evento) {
            evento.preventDefault();
            ocultarMensaje(mensajeErrorAsignarModal);

            const btnConfirmar = cuerpo.querySelector('#btn-confirmar-asignar-modal');
            const textoOriginal = btnConfirmar.textContent;
            btnConfirmar.disabled = true;
            btnConfirmar.textContent = 'Guardando...';

            try {
                const esTecnico = cuerpo.querySelector('input[name="tipo-destino-modal"]:checked').value === 'tecnico';
                const prioridad = selectPrioridadModal.value;
                const idTecnico = idTecnicoElegido;
                const idGrupo = idGrupoElegido;
                const campoMotivo = cuerpo.querySelector('#motivo-reasignacion-modal');
                const motivo = campoMotivo ? campoMotivo.value.trim() : '';

                if (esTecnico && !idTecnico) {
                    throw new Error('Elegí un técnico de la lista (hacé clic en uno).');
                }
                if (!esTecnico && !idGrupo) {
                    throw new Error('Elegí un grupo de la lista (hacé clic en uno).');
                }

                await apiFetch('/api/solicitudes/' + detalle.idSolicitud + '/asignaciones', {
                    method: 'POST',
                    body: JSON.stringify({
                        idTecnico: esTecnico ? Number(idTecnico) : null,
                        idGrupo: esTecnico ? null : Number(idGrupo),
                        idPrioridad: prioridad ? Number(prioridad) : null,
                        motivoReasignacion: motivo || null
                    })
                });

                cerrar();
                cargarSolicitudes();
                cargarMetricas();
                cargarGraficos();
                mostrarToast(
                    detalle.estado !== 'Pendiente' ? 'Solicitud reasignada correctamente.' : 'Solicitud asignada correctamente.',
                    'exito'
                );
            } catch (error) {
                mostrarError(mensajeErrorAsignarModal, error);
                btnConfirmar.disabled = false;
                btnConfirmar.textContent = textoOriginal;
            }
        });
    }

    filtroEstado.addEventListener('change', function () { paginaSolicitudes = 0; cargarSolicitudes(); });

    // ---------- Reportes pendientes ----------

    function filaReporte(r) {
        return '<tr>' +
            '<td>#' + r.idReporte + '</td>' +
            '<td>Solicitud #' + r.idSolicitud +
            '<br><span class="subtitulo" style="font-size:0.78rem;">' +
            escaparHtml(r.solicitudDescripcion || '—') + ' · ' + escaparHtml(r.solicitudDireccion || '—') +
            (r.solicitudCategoria ? ' · ' + escaparHtml(r.solicitudCategoria) : '') +
            (r.solicitudPrioridad ? ' · ' + escaparHtml(r.solicitudPrioridad) : '') +
            '</span></td>' +
            '<td>' + escaparHtml(r.tecnicoNombre || ('Técnico #' + r.idTecnico)) + '</td>' +
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

        try {
            await cargarListaAdjuntos(idSolicitud, lista);
        } catch (error) {
            lista.innerHTML = '';
            mostrarError(mensajeError, error);
        }
    }

    function actualizarTarjetaReportesPendientes() {
        const tarjetas = filaMetricas.querySelectorAll('.tarjeta-metrica');
        if (tarjetas[2] && ultimoTotalReportesPendientes !== null) {
            const elementoValor = tarjetas[2].querySelector('.valor');
            elementoValor.textContent = ultimoTotalReportesPendientes;
            elementoValor.className = 'valor ' + claseAlertaPorValor(ultimoTotalReportesPendientes, 4, 10);
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
            cargarGraficos();
            mostrarToast('Reporte #' + idReporte + ' aprobado.', 'exito');
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
            cargarGraficos();
            mostrarToast('Reporte #' + idReporte + ' rechazado.', 'exito');
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
            mostrarToast('Estado de pago actualizado.', 'exito');
        } catch (error) {
            mostrarError(mensajeErrorClientes, error);
        } finally {
            boton.disabled = false;
        }
    }

   

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

    function abrirModalAnuncios() {
        const overlay = document.createElement('div');
        overlay.className = 'overlay-modal';
        overlay.innerHTML =
            '<div class="modal modal-ancho-xl">' +
            '<h3>Anuncios</h3>' +
            '<div id="mensaje-error-anuncio" class="mensaje-error oculto"></div>' +
            '<form id="form-anuncio" class="detalle-form" style="margin-bottom: 16px;">' +
            '<div class="campo" style="flex: 1; min-width: 180px;">' +
            '<label for="titulo-anuncio">Título</label>' +
            '<input type="text" id="titulo-anuncio" required maxlength="200">' +
            '</div>' +
            '<div class="campo" style="flex: 2; min-width: 220px;">' +
            '<label for="mensaje-anuncio">Mensaje</label>' +
            '<textarea id="mensaje-anuncio" required></textarea>' +
            '</div>' +
            '<div class="campo" style="min-width: 180px;">' +
            '<label for="expiracion-anuncio">Vence el (opcional)</label>' +
            '<input type="datetime-local" id="expiracion-anuncio">' +
            '</div>' +
            '<button type="submit" style="align-self: flex-end;">Publicar</button>' +
            '</form>' +
            '<div id="lista-anuncios-admin" class="lista-miembros-grupo">Cargando anuncios...</div>' +
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

        const mensajeError = overlay.querySelector('#mensaje-error-anuncio');
        const lista = overlay.querySelector('#lista-anuncios-admin');

        async function cargarAnunciosAdmin() {
            try {
                const anuncios = await apiFetch('/api/anuncios/todos');

                lista.innerHTML = anuncios.length
                    ? '<div class="tabla-scroll"><table><thead><tr>' +
                      '<th>ID</th><th>Título / mensaje</th><th>Creado</th><th>Vence</th><th>Estado</th><th></th>' +
                      '</tr></thead><tbody>' + anuncios.map(filaAnuncio).join('') + '</tbody></table></div>'
                    : '<div class="vacio">Todavía no hay anuncios.</div>';

                lista.querySelectorAll('.btn-desactivar-anuncio').forEach(function (boton) {
                    boton.addEventListener('click', async function () {
                        boton.disabled = true;
                        try {
                            await apiFetch('/api/anuncios/' + boton.getAttribute('data-id') + '/desactivacion', { method: 'POST' });
                            cargarAnunciosAdmin();
                            cargarAnunciosActivos('banner-anuncios');
                            mostrarToast('Anuncio desactivado.', 'exito');
                        } catch (error) {
                            mostrarError(mensajeError, error);
                            boton.disabled = false;
                        }
                    });
                });
            } catch (error) {
                lista.innerHTML = '';
                mostrarError(mensajeError, error);
            }
        }

        overlay.querySelector('#form-anuncio').addEventListener('submit', async function (evento) {
            evento.preventDefault();
            ocultarMensaje(mensajeError);

            const btnPublicar = evento.target.querySelector('button[type="submit"]');
            btnPublicar.disabled = true;
            btnPublicar.textContent = 'Publicando...';

            try {
                const titulo = overlay.querySelector('#titulo-anuncio').value.trim();
                const mensaje = overlay.querySelector('#mensaje-anuncio').value.trim();
                const valorExpiracion = overlay.querySelector('#expiracion-anuncio').value;

                await apiFetch('/api/anuncios', {
                    method: 'POST',
                    body: JSON.stringify({
                        titulo: titulo,
                        mensaje: mensaje,
                        fechaExpiracion: valorExpiracion ? new Date(valorExpiracion).toISOString() : null
                    })
                });

                overlay.querySelector('#form-anuncio').reset();
                cargarAnunciosAdmin();
                cargarAnunciosActivos('banner-anuncios');
                mostrarToast('Anuncio publicado.', 'exito');
            } catch (error) {
                mostrarError(mensajeError, error);
            } finally {
                btnPublicar.disabled = false;
                btnPublicar.textContent = 'Publicar';
            }
        });

        cargarAnunciosAdmin();
    }

    document.getElementById('btn-abrir-anuncios').addEventListener('click', abrirModalAnuncios);

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
    cargarGraficos();
    cargarCatalogos();
    cargarSolicitudes();
    cargarReportesPendientes();
    cargarClientes();
    activarNavegacionPorTabs();
})();
