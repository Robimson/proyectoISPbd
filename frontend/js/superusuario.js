(function () {
    if (!exigirSesion('SUPERUSUARIO')) return;

    document.getElementById('texto-usuario').textContent = 'Superusuario #' + obtenerIdUsuario();
    activarModalCambiarContrasena();

    let paginaUsuarios = 0;

    // ---------- Resumen ----------

    const filaMetricas = document.getElementById('fila-metricas');

    async function cargarMetricas() {
        filaMetricas.innerHTML =
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">Usuarios totales</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">Clientes</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">Técnicos</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">Grupos técnicos</div></div>';

        try {
            const [total, clientes, tecnicos, grupos] = await Promise.all([
                apiFetch('/api/usuarios?size=1'),
                apiFetch('/api/usuarios?rol=cliente&size=1'),
                apiFetch('/api/usuarios?rol=tecnico&size=1'),
                apiFetch('/api/grupos-tecnicos')
            ]);

            filaMetricas.innerHTML =
                '<div class="tarjeta-metrica"><div class="valor">' + total.totalElements + '</div><div class="etiqueta">Usuarios totales</div></div>' +
                '<div class="tarjeta-metrica"><div class="valor">' + clientes.totalElements + '</div><div class="etiqueta">Clientes</div></div>' +
                '<div class="tarjeta-metrica"><div class="valor">' + tecnicos.totalElements + '</div><div class="etiqueta">Técnicos</div></div>' +
                '<div class="tarjeta-metrica"><div class="valor">' + grupos.length + '</div><div class="etiqueta">Grupos técnicos</div></div>';
        } catch (error) {
            console.error('No se pudieron cargar las métricas:', error);
        }
    }

    // ---------- Invitar usuario ----------

    const mensajeErrorInvitar = document.getElementById('mensaje-error-invitar');
    const tokenGenerado = document.getElementById('token-generado');

    document.getElementById('form-invitar').addEventListener('submit', async function (evento) {
        evento.preventDefault();
        ocultarMensaje(mensajeErrorInvitar);
        tokenGenerado.classList.add('oculto');

        const btnInvitar = document.getElementById('btn-invitar');
        btnInvitar.disabled = true;
        btnInvitar.textContent = 'Enviando...';

        try {
            const nombreUsuario = document.getElementById('nombre-usuario').value.trim();
            const correo = document.getElementById('correo-invitar').value.trim();
            const rol = document.getElementById('rol-invitar').value;

            const respuesta = await apiFetch('/api/usuarios/invitaciones', {
                method: 'POST',
                body: JSON.stringify({ nombreUsuario: nombreUsuario, correo: correo, rol: rol })
            });

            document.getElementById('form-invitar').reset();
            tokenGenerado.innerHTML =
                'Invitación creada para <strong>' + escaparHtml(respuesta.correo) + '</strong>. ' +
                'Se envió un correo con el enlace de activación. Si no le llega (revisa spam) o quieres ' +
                'pasárselo tú directamente, este es el token para <em>activar.html</em>:<br><br>' + escaparHtml(respuesta.token);
            tokenGenerado.classList.remove('oculto');

            cargarUsuarios();
        } catch (error) {
            mostrarError(mensajeErrorInvitar, error);
        } finally {
            btnInvitar.disabled = false;
            btnInvitar.textContent = 'Enviar invitación';
        }
    });

    // ---------- Listado de usuarios ----------

    const mensajeErrorUsuarios = document.getElementById('mensaje-error-usuarios');
    const contenedorTablaUsuarios = document.getElementById('contenedor-tabla-usuarios');
    const paginacionUsuarios = document.getElementById('paginacion-usuarios');
    const filtroRol = document.getElementById('filtro-rol');

    const ESTADOS_CUENTA = ['activo', 'suspendido', 'inactivo'];

    function filaUsuario(u) {
        const opcionesEstado = ESTADOS_CUENTA.map(function (e) {
            return '<option value="' + e + '" ' + (e === u.estadoCuenta ? 'selected' : '') + '>' + e + '</option>';
        }).join('');

        return '<tr>' +
            '<td>#' + u.idUsuario + '</td>' +
            '<td>' + escaparHtml(u.nombreUsuario) + '</td>' +
            '<td>' + escaparHtml(u.correo) + '</td>' +
            '<td>' + escaparHtml(u.rol) + '</td>' +
            '<td><span class="badge ' + claseBadgeEstadoCuenta(u.estadoCuenta) + '">' + escaparHtml(u.estadoCuenta) + '</span></td>' +
            '<td><div class="acciones-fila">' +
            '<select class="select-nuevo-estado" data-id="' + u.idUsuario + '">' + opcionesEstado + '</select>' +
            '<button class="btn-cambiar-estado secundario" data-id="' + u.idUsuario + '">Cambiar</button>' +
            '</div></td>' +
            '</tr>';
    }

    async function cargarUsuarios() {
        ocultarMensaje(mensajeErrorUsuarios);
        contenedorTablaUsuarios.innerHTML = htmlCargando();

        try {
            let ruta = '/api/usuarios?page=' + paginaUsuarios + '&size=10';
            if (filtroRol.value) {
                ruta += '&rol=' + encodeURIComponent(filtroRol.value);
            }

            const pagina = await apiFetch(ruta);

            if (!pagina.content || pagina.content.length === 0) {
                contenedorTablaUsuarios.innerHTML = '<div class="vacio">No hay usuarios.</div>';
                paginacionUsuarios.innerHTML = '';
                return;
            }

            contenedorTablaUsuarios.innerHTML =
                '<div class="tabla-scroll"><table><thead><tr>' +
                '<th>ID</th><th>Nombre</th><th>Correo</th><th>Rol</th><th>Estado</th><th></th>' +
                '</tr></thead><tbody>' + pagina.content.map(filaUsuario).join('') + '</tbody></table></div>';

            renderizarPaginacion(pagina, paginacionUsuarios, function (nueva) { paginaUsuarios = nueva; cargarUsuarios(); });

            contenedorTablaUsuarios.querySelectorAll('.btn-cambiar-estado').forEach(function (boton) {
                boton.addEventListener('click', manejarCambiarEstado);
            });
        } catch (error) {
            contenedorTablaUsuarios.innerHTML = '';
            mostrarError(mensajeErrorUsuarios, error);
        }
    }

    async function manejarCambiarEstado(evento) {
        const boton = evento.currentTarget;
        const id = boton.getAttribute('data-id');
        const select = contenedorTablaUsuarios.querySelector('.select-nuevo-estado[data-id="' + id + '"]');
        const nuevoEstado = select.value;

        boton.disabled = true;
        try {
            await apiFetch('/api/usuarios/' + id + '/estado', {
                method: 'POST',
                body: JSON.stringify({ estadoCuenta: nuevoEstado })
            });
            cargarUsuarios();
        } catch (error) {
            mostrarError(mensajeErrorUsuarios, error);
        } finally {
            boton.disabled = false;
        }
    }

    filtroRol.addEventListener('change', function () { paginaUsuarios = 0; cargarUsuarios(); });

    // ---------- Grupos tecnicos ----------

    const mensajeErrorGrupo = document.getElementById('mensaje-error-grupo');
    const mensajeErrorMiembro = document.getElementById('mensaje-error-miembro');
    const contenedorTablaGrupos = document.getElementById('contenedor-tabla-grupos');
    const selectGrupoMiembro = document.getElementById('select-grupo-miembro');
    const selectTecnicoMiembro = document.getElementById('select-tecnico-miembro');

    async function cargarGrupos() {
        try {
            const grupos = await apiFetch('/api/grupos-tecnicos');

            contenedorTablaGrupos.innerHTML = grupos.length
                ? '<div class="tabla-scroll"><table><thead><tr><th>ID</th><th>Nombre</th></tr></thead><tbody>' +
                  grupos.map(function (g) { return '<tr><td>#' + g.idGrupo + '</td><td>' + escaparHtml(g.nombreGrupo) + '</td></tr>'; }).join('') +
                  '</tbody></table></div>'
                : '<div class="vacio">Todavía no hay grupos técnicos.</div>';

            selectGrupoMiembro.innerHTML = grupos.map(function (g) {
                return '<option value="' + g.idGrupo + '">' + escaparHtml(g.nombreGrupo) + '</option>';
            }).join('') || '<option value="">Crea un grupo primero</option>';
        } catch (error) {
            console.error('No se pudieron cargar los grupos técnicos:', error);
        }
    }

    async function cargarTecnicosParaMiembro() {
        try {
            const tecnicos = await apiFetch('/api/tecnicos');
            selectTecnicoMiembro.innerHTML = tecnicos.map(function (t) {
                return '<option value="' + t.idUsuario + '">' + escaparHtml(t.nombreUsuario) + '</option>';
            }).join('') || '<option value="">No hay técnicos habilitados</option>';
        } catch (error) {
            console.error('No se pudieron cargar los técnicos:', error);
        }
    }

    document.getElementById('form-grupo').addEventListener('submit', async function (evento) {
        evento.preventDefault();
        ocultarMensaje(mensajeErrorGrupo);

        try {
            const nombreGrupo = document.getElementById('nombre-grupo').value.trim();
            await apiFetch('/api/grupos-tecnicos', {
                method: 'POST',
                body: JSON.stringify({ nombreGrupo: nombreGrupo })
            });
            document.getElementById('form-grupo').reset();
            cargarGrupos();
        } catch (error) {
            mostrarError(mensajeErrorGrupo, error);
        }
    });

    document.getElementById('form-miembro').addEventListener('submit', async function (evento) {
        evento.preventDefault();
        ocultarMensaje(mensajeErrorMiembro);

        try {
            const idGrupo = selectGrupoMiembro.value;
            const idTecnico = selectTecnicoMiembro.value;

            if (!idGrupo || !idTecnico) {
                throw new Error('Selecciona un grupo y un técnico.');
            }

            await apiFetch('/api/grupos-tecnicos/' + idGrupo + '/miembros', {
                method: 'POST',
                body: JSON.stringify({ idTecnico: Number(idTecnico) })
            });

            mensajeErrorMiembro.textContent = 'Técnico agregado al grupo.';
            mensajeErrorMiembro.classList.remove('mensaje-error');
            mensajeErrorMiembro.classList.add('mensaje-info');
            mensajeErrorMiembro.classList.remove('oculto');
        } catch (error) {
            mensajeErrorMiembro.classList.remove('mensaje-info');
            mensajeErrorMiembro.classList.add('mensaje-error');
            mostrarError(mensajeErrorMiembro, error);
        }
    });

    // ---------- Auditoria de sesiones (seccion 11) ----------

    const mensajeErrorSesiones = document.getElementById('mensaje-error-sesiones');
    const contenedorTablaSesiones = document.getElementById('contenedor-tabla-sesiones');
    const paginacionSesiones = document.getElementById('paginacion-sesiones');
    const filtroUsuarioSesiones = document.getElementById('filtro-usuario-sesiones');

    let paginaSesiones = 0;

    function filaSesion(s) {
        return '<tr>' +
            '<td>#' + s.idSesion + '</td>' +
            '<td>Usuario #' + s.idUsuario + '</td>' +
            '<td>' + formatearFecha(s.fechaEntrada) + '</td>' +
            '<td>' + formatearFecha(s.ultimaActividad) + '</td>' +
            '<td>' + (s.fechaSalida ? formatearFecha(s.fechaSalida) : '<span class="badge badge-activo">activa</span>') + '</td>' +
            '<td>' + escaparHtml(s.ipOrigen || '—') + '</td>' +
            '</tr>';
    }

    async function cargarSesiones() {
        ocultarMensaje(mensajeErrorSesiones);
        contenedorTablaSesiones.innerHTML = htmlCargando();

        try {
            let ruta = '/api/auditoria/sesiones?page=' + paginaSesiones + '&size=10';
            if (filtroUsuarioSesiones.value) {
                ruta += '&idUsuario=' + encodeURIComponent(filtroUsuarioSesiones.value);
            }

            const pagina = await apiFetch(ruta);

            if (!pagina.content || pagina.content.length === 0) {
                contenedorTablaSesiones.innerHTML = '<div class="vacio">No hay sesiones registradas.</div>';
                paginacionSesiones.innerHTML = '';
                return;
            }

            contenedorTablaSesiones.innerHTML =
                '<div class="tabla-scroll"><table><thead><tr>' +
                '<th>ID</th><th>Usuario</th><th>Entrada</th><th>Última actividad</th><th>Salida</th><th>IP</th>' +
                '</tr></thead><tbody>' + pagina.content.map(filaSesion).join('') + '</tbody></table></div>';

            renderizarPaginacion(pagina, paginacionSesiones, function (nueva) { paginaSesiones = nueva; cargarSesiones(); });
        } catch (error) {
            contenedorTablaSesiones.innerHTML = '';
            mostrarError(mensajeErrorSesiones, error);
        }
    }

    let temporizadorFiltroSesiones = null;
    filtroUsuarioSesiones.addEventListener('input', function () {
        clearTimeout(temporizadorFiltroSesiones);
        temporizadorFiltroSesiones = setTimeout(function () { paginaSesiones = 0; cargarSesiones(); }, 400);
    });

    // ---------- Auditoria de datos (operacion interna: tecnico/admin/superusuario) ----------

    const mensajeErrorDatos = document.getElementById('mensaje-error-datos');
    const contenedorTablaDatos = document.getElementById('contenedor-tabla-datos');
    const paginacionDatos = document.getElementById('paginacion-datos');
    const filtroTablaDatos = document.getElementById('filtro-tabla-datos');

    let paginaDatos = 0;
    let ultimaPaginaDatos = [];

    function filaDatos(a) {
        return '<tr>' +
            '<td>' + formatearFecha(a.fecha) + '</td>' +
            '<td>' + escaparHtml(a.tablaAfectada) + '</td>' +
            '<td><span class="badge ' + claseBadgeOperacion(a.operacion) + '">' + escaparHtml(a.operacion) + '</span></td>' +
            '<td>' + (a.idUsuarioResponsable ? 'Usuario #' + a.idUsuarioResponsable : '—') + '</td>' +
            '<td><button class="secundario btn-ver-cambio" data-id="' + a.idAuditoria + '">Ver</button></td>' +
            '</tr>';
    }

    async function cargarDatos() {
        ocultarMensaje(mensajeErrorDatos);
        contenedorTablaDatos.innerHTML = htmlCargando();

        try {
            let ruta = '/api/auditoria/datos?page=' + paginaDatos + '&size=10';
            if (filtroTablaDatos.value) {
                ruta += '&tabla=' + encodeURIComponent(filtroTablaDatos.value);
            }

            const pagina = await apiFetch(ruta);
            ultimaPaginaDatos = pagina.content || [];

            if (ultimaPaginaDatos.length === 0) {
                contenedorTablaDatos.innerHTML = '<div class="vacio">No hay cambios registrados.</div>';
                paginacionDatos.innerHTML = '';
                return;
            }

            contenedorTablaDatos.innerHTML =
                '<div class="tabla-scroll"><table><thead><tr>' +
                '<th>Fecha</th><th>Tabla</th><th>Operación</th><th>Responsable</th><th></th>' +
                '</tr></thead><tbody>' + ultimaPaginaDatos.map(filaDatos).join('') + '</tbody></table></div>';

            renderizarPaginacion(pagina, paginacionDatos, function (nueva) { paginaDatos = nueva; cargarDatos(); });

            contenedorTablaDatos.querySelectorAll('.btn-ver-cambio').forEach(function (boton) {
                boton.addEventListener('click', function () {
                    const id = Number(boton.getAttribute('data-id'));
                    const fila = ultimaPaginaDatos.find(function (a) { return a.idAuditoria === id; });
                    if (fila) mostrarDetalleAuditoria(fila);
                });
            });
        } catch (error) {
            contenedorTablaDatos.innerHTML = '';
            mostrarError(mensajeErrorDatos, error);
        }
    }

    filtroTablaDatos.addEventListener('change', function () { paginaDatos = 0; cargarDatos(); });

    function formatearJson(texto) {
        if (!texto) return '—';
        try {
            return JSON.stringify(JSON.parse(texto), null, 2);
        } catch (error) {
            return texto;
        }
    }

    function mostrarDetalleAuditoria(fila) {
        const overlay = document.createElement('div');
        overlay.className = 'overlay-modal';
        overlay.innerHTML =
            '<div class="modal" style="max-width: 640px;">' +
            '<h3>' + escaparHtml(fila.tablaAfectada) + ' — ' + escaparHtml(fila.operacion) + '</h3>' +
            '<p class="subtitulo" style="margin-top: -6px;">' + formatearFecha(fila.fecha) +
            (fila.idUsuarioResponsable ? ' · Usuario #' + fila.idUsuarioResponsable : '') + '</p>' +
            (fila.datosAnteriores ? '<p style="font-weight:600; margin-bottom:4px;">Antes</p>' +
                '<pre style="background: var(--color-fondo); padding:10px; border-radius:8px; overflow:auto; max-height:180px; font-size:0.8rem;">' +
                escaparHtml(formatearJson(fila.datosAnteriores)) + '</pre>' : '') +
            (fila.datosNuevos ? '<p style="font-weight:600; margin-bottom:4px; margin-top:12px;">Después</p>' +
                '<pre style="background: var(--color-fondo); padding:10px; border-radius:8px; overflow:auto; max-height:180px; font-size:0.8rem;">' +
                escaparHtml(formatearJson(fila.datosNuevos)) + '</pre>' : '') +
            '<div class="modal-acciones" style="margin-top:16px;"><button type="button" class="secundario" data-accion="cerrar">Cerrar</button></div>' +
            '</div>';

        function cerrar() {
            document.body.removeChild(overlay);
            document.removeEventListener('keydown', alPresionarTecla);
        }
        function alPresionarTecla(evento) {
            if (evento.key === 'Escape') cerrar();
        }
        overlay.addEventListener('click', function (evento) { if (evento.target === overlay) cerrar(); });
        overlay.querySelector('[data-accion="cerrar"]').addEventListener('click', cerrar);
        document.addEventListener('keydown', alPresionarTecla);

        document.body.appendChild(overlay);
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
    cargarUsuarios();
    cargarGrupos();
    cargarTecnicosParaMiembro();
    cargarSesiones();
    cargarDatos();
    activarNavegacionPorTabs();
})();
