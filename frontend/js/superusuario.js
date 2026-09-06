(function () {
    aplicarConfiguracionSistema();
    if (!exigirSesion('SUPERUSUARIO')) return;

    document.getElementById('texto-usuario').textContent = 'Superusuario #' + obtenerIdUsuario();
    activarModalCambiarContrasena();

    let paginaUsuarios = 0;

    // ---------- Resumen ----------

    const filaMetricas = document.getElementById('fila-metricas');

    async function cargarMetricas() {
        filaMetricas.innerHTML =
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">👥 Usuarios totales</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">🧑 Clientes</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">🛠️ Técnicos</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">📁 Grupos técnicos</div></div>';

        try {
            const [total, clientes, tecnicos, grupos] = await Promise.all([
                apiFetch('/api/usuarios?size=1'),
                apiFetch('/api/usuarios?rol=cliente&size=1'),
                apiFetch('/api/usuarios?rol=tecnico&size=1'),
                apiFetch('/api/grupos-tecnicos')
            ]);

            filaMetricas.innerHTML =
                '<div class="tarjeta-metrica"><div class="valor">' + total.totalElements + '</div><div class="etiqueta">👥 Usuarios totales</div></div>' +
                '<div class="tarjeta-metrica"><div class="valor">' + clientes.totalElements + '</div><div class="etiqueta">🧑 Clientes</div></div>' +
                '<div class="tarjeta-metrica"><div class="valor">' + tecnicos.totalElements + '</div><div class="etiqueta">🛠️ Técnicos</div></div>' +
                '<div class="tarjeta-metrica"><div class="valor">' + grupos.length + '</div><div class="etiqueta">📁 Grupos técnicos</div></div>';
        } catch (error) {
            console.error('No se pudieron cargar las métricas:', error);
        }
    }

    async function cargarGraficos() {
        const contenedorNivel = document.getElementById('grafico-tecnicos-nivel');
        const contenedorRol = document.getElementById('grafico-usuarios-rol');
        const contenedorGrupos = document.getElementById('grafico-miembros-grupo');
        try {
            const [estadisticas, grupos] = await Promise.all([
                apiFetch('/api/dashboard/superusuario'),
                // "Miembros por grupo" ya viene con el conteo en este mismo
                // endpoint (GrupoTecnicoConteoProjection) - no hace falta un
                // endpoint aparte solo para el grafico.
                apiFetch('/api/grupos-tecnicos')
            ]);
            contenedorNivel.innerHTML = graficoDona(estadisticas.tecnicosPorNivel);
            contenedorRol.innerHTML = graficoDona(estadisticas.usuariosPorRol);
            contenedorGrupos.innerHTML = graficoBarras(grupos.map(function (g) {
                return { etiqueta: g.nombreGrupo, valor: g.totalTecnicos };
            }));
        } catch (error) {
            console.error('No se pudieron cargar los gráficos:', error);
            contenedorNivel.innerHTML = '';
            contenedorRol.innerHTML = '';
            contenedorGrupos.innerHTML = '';
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

        const btnPerfilTecnico = u.rol === 'tecnico'
            ? '<button class="btn-perfil-tecnico secundario" data-id="' + u.idUsuario + '" data-nombre="' + escaparHtml(u.nombreUsuario) + '">Perfil técnico</button>'
            : '';

        return '<tr>' +
            '<td>#' + u.idUsuario + '</td>' +
            '<td>' + escaparHtml(u.nombreUsuario) + '</td>' +
            '<td>' + escaparHtml(u.correo) + '</td>' +
            '<td>' + escaparHtml(u.rol) + '</td>' +
            '<td><span class="badge ' + claseBadgeEstadoCuenta(u.estadoCuenta) + '">' + escaparHtml(u.estadoCuenta) + '</span></td>' +
            '<td><div class="acciones-fila">' +
            '<select class="select-nuevo-estado" data-id="' + u.idUsuario + '">' + opcionesEstado + '</select>' +
            '<button class="btn-cambiar-estado secundario" data-id="' + u.idUsuario + '">Cambiar</button>' +
            btnPerfilTecnico +
            '</div></td>' +
            '</tr>';
    }

    /**
     * Especialidad y nivel quedan en NULL / 'junior' desde que se invita al
     * tecnico (sp_invitar_usuario) - este modal es el unico lugar que los
     * llena o actualiza despues (sigue el mismo patron dinamico que
     * activarModalCambiarContrasena en api.js).
     */
    async function abrirModalPerfilTecnico(idTecnico, nombreTecnico) {
        let tecnicoActual = null;
        try {
            tecnicoActual = await apiFetch('/api/tecnicos/' + idTecnico);
        } catch (error) {
            console.error('No se pudo cargar el perfil actual del técnico:', error);
        }

        const nivelActual = tecnicoActual ? tecnicoActual.nivel : 'junior';
        const especialidadActual = tecnicoActual ? (tecnicoActual.especialidad || '') : '';

        const overlay = document.createElement('div');
        overlay.className = 'overlay-modal';
        overlay.innerHTML =
            '<div class="modal">' +
            '<h3>Perfil técnico — ' + escaparHtml(nombreTecnico) + '</h3>' +
            '<div id="mensaje-error-perfil-tecnico" class="mensaje-error oculto"></div>' +
            '<form id="form-perfil-tecnico">' +
            '<div class="campo">' +
            '<label for="especialidad-modal">Especialidad</label>' +
            '<input type="text" id="especialidad-modal" placeholder="Ej: Fibra óptica, redes internas...">' +
            '</div>' +
            '<div class="campo">' +
            '<label for="nivel-modal">Nivel</label>' +
            '<select id="nivel-modal">' +
            '<option value="junior">Junior</option>' +
            '<option value="intermedio">Intermedio</option>' +
            '<option value="senior">Senior</option>' +
            '</select>' +
            '</div>' +
            '<div class="modal-acciones">' +
            '<button type="button" class="secundario" data-accion="cancelar">Cancelar</button>' +
            '<button type="submit" id="btn-confirmar-perfil-tecnico">Guardar</button>' +
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

        overlay.querySelector('#especialidad-modal').value = especialidadActual;
        overlay.querySelector('#nivel-modal').value = nivelActual;

        const mensajeError = overlay.querySelector('#mensaje-error-perfil-tecnico');
        overlay.querySelector('#form-perfil-tecnico').addEventListener('submit', async function (evento) {
            evento.preventDefault();
            ocultarMensaje(mensajeError);

            const btnGuardar = overlay.querySelector('#btn-confirmar-perfil-tecnico');
            btnGuardar.disabled = true;
            btnGuardar.textContent = 'Guardando...';

            try {
                await apiFetch('/api/tecnicos/' + idTecnico + '/perfil', {
                    method: 'POST',
                    body: JSON.stringify({
                        especialidad: overlay.querySelector('#especialidad-modal').value.trim(),
                        nivel: overlay.querySelector('#nivel-modal').value
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
            contenedorTablaUsuarios.querySelectorAll('.btn-perfil-tecnico').forEach(function (boton) {
                boton.addEventListener('click', function () {
                    abrirModalPerfilTecnico(boton.getAttribute('data-id'), boton.getAttribute('data-nombre'));
                });
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
    const contenedorTablaGrupos = document.getElementById('contenedor-tabla-grupos');

    async function cargarGrupos() {
        try {
            const grupos = await apiFetch('/api/grupos-tecnicos');

            contenedorTablaGrupos.innerHTML = grupos.length
                ? '<div class="tabla-scroll"><table><thead><tr><th>ID</th><th>Nombre</th><th># Técnicos</th><th></th></tr></thead><tbody>' +
                  grupos.map(function (g) {
                      return '<tr><td>#' + g.idGrupo + '</td><td>' + escaparHtml(g.nombreGrupo) + '</td><td>' + g.totalTecnicos + '</td>' +
                          '<td><button class="btn-editar-grupo secundario" data-id="' + g.idGrupo + '" data-nombre="' + escaparHtml(g.nombreGrupo) + '">Editar</button></td></tr>';
                  }).join('') +
                  '</tbody></table></div>'
                : '<div class="vacio">Todavía no hay grupos técnicos.</div>';

            contenedorTablaGrupos.querySelectorAll('.btn-editar-grupo').forEach(function (boton) {
                boton.addEventListener('click', function () {
                    abrirModalEditarGrupo(boton.getAttribute('data-id'), boton.getAttribute('data-nombre'));
                });
            });
        } catch (error) {
            console.error('No se pudieron cargar los grupos técnicos:', error);
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

    /**
     * Modal "Editar grupo": dos pantallas dentro del mismo modal, no todo
     * junto (mezclar "quién ya está" con "buscar para agregar" en un solo
     * bloque resultaba confuso). Por defecto se ve la lista de miembros
     * (con "Quitar" - DELETE .../miembros/{id} ya existía en el backend
     * pero no se podía usar desde ninguna pantalla); "+ Agregar técnico"
     * cambia a la pantalla de búsqueda, que reemplaza a la lista mientras
     * está activa. "Volver" regresa a la lista ya actualizada.
     */
    async function abrirModalEditarGrupo(idGrupo, nombreGrupo) {
        const overlay = document.createElement('div');
        overlay.className = 'overlay-modal';
        overlay.innerHTML =
            '<div class="modal modal-ancho">' +
            '<h3>Grupo — ' + escaparHtml(nombreGrupo) + '</h3>' +
            '<div id="mensaje-error-editar-grupo" class="mensaje-error oculto"></div>' +

            '<div id="vista-miembros-grupo">' +
            '<div id="lista-miembros-grupo" class="lista-miembros-grupo">Cargando miembros...</div>' +
            '<button type="button" id="btn-ir-a-agregar" class="secundario" style="width: 100%; margin-top: 12px;">+ Agregar técnico</button>' +
            '</div>' +

            '<div id="vista-agregar-grupo" class="oculto">' +
            '<div class="campo" style="position: relative;">' +
            '<label for="buscar-tecnico-grupo">Buscar técnico</label>' +
            '<input type="text" id="buscar-tecnico-grupo" placeholder="Nombre o correo..." autocomplete="off">' +
            '<div id="sugerencias-tecnico-grupo" class="sugerencias-usuario oculto"></div>' +
            '</div>' +
            '<div id="mensaje-agregado-grupo" class="mensaje-info oculto" style="margin-bottom: 12px;"></div>' +
            '<div style="display: flex; gap: 10px;">' +
            '<button type="button" id="btn-volver-a-miembros" class="secundario" style="flex: 1;">← Volver a la lista</button>' +
            '<button type="button" id="btn-agregar-miembro-grupo" style="flex: 1;">Agregar</button>' +
            '</div>' +
            '</div>' +

            '<div class="modal-acciones">' +
            '<button type="button" class="secundario" data-accion="cerrar">Cerrar</button>' +
            '</div>' +
            '</div>';

        function cerrar() {
            document.body.removeChild(overlay);
            document.removeEventListener('keydown', alPresionarTecla);
            cargarGrupos();
        }

        function alPresionarTecla(evento) {
            if (evento.key === 'Escape') cerrar();
        }

        // Se agrega al documento ANTES de conectar activarBusquedaRemota():
        // esa funcion busca sus elementos con document.getElementById(), que
        // no los encuentra mientras el modal solo existe como overlay.innerHTML
        // (todavia no forma parte del documento vivo).
        document.body.appendChild(overlay);

        overlay.addEventListener('click', function (evento) {
            if (evento.target === overlay) cerrar();
        });
        overlay.querySelector('[data-accion="cerrar"]').addEventListener('click', cerrar);
        document.addEventListener('keydown', alPresionarTecla);

        const mensajeError = overlay.querySelector('#mensaje-error-editar-grupo');
        const mensajeAgregado = overlay.querySelector('#mensaje-agregado-grupo');
        const listaMiembros = overlay.querySelector('#lista-miembros-grupo');
        const vistaMiembros = overlay.querySelector('#vista-miembros-grupo');
        const vistaAgregar = overlay.querySelector('#vista-agregar-grupo');

        async function cargarMiembros() {
            listaMiembros.textContent = 'Cargando miembros...';
            try {
                const miembros = await apiFetch('/api/grupos-tecnicos/' + idGrupo + '/miembros');
                listaMiembros.innerHTML = miembros.length
                    ? miembros.map(function (m) {
                        return '<div class="fila-miembro-grupo">' +
                            '<span><strong>' + escaparHtml(m.nombreUsuario) + '</strong> · ' + escaparHtml(m.correo) + '</span>' +
                            '<button type="button" class="btn-quitar-miembro secundario" data-id="' + m.idUsuario + '">Quitar</button>' +
                            '</div>';
                    }).join('')
                    : '<div class="vacio">Este grupo todavía no tiene técnicos.</div>';

                listaMiembros.querySelectorAll('.btn-quitar-miembro').forEach(function (boton) {
                    boton.addEventListener('click', async function () {
                        boton.disabled = true;
                        try {
                            await apiFetch('/api/grupos-tecnicos/' + idGrupo + '/miembros/' + boton.getAttribute('data-id'), { method: 'DELETE' });
                            cargarMiembros();
                            cargarGraficos();
                        } catch (error) {
                            mostrarError(mensajeError, error);
                            boton.disabled = false;
                        }
                    });
                });
            } catch (error) {
                listaMiembros.innerHTML = '';
                mostrarError(mensajeError, error);
            }
        }

        const selectorTecnico = activarBusquedaRemota(
            'buscar-tecnico-grupo',
            'sugerencias-tecnico-grupo',
            function (termino) { return apiFetch('/api/tecnicos/buscar?nombre=' + encodeURIComponent(termino)); }
        );

        function irAVistaAgregar() {
            ocultarMensaje(mensajeError);
            ocultarMensaje(mensajeAgregado);
            selectorTecnico.limpiar();
            vistaMiembros.classList.add('oculto');
            vistaAgregar.classList.remove('oculto');
        }

        function volverAVistaMiembros() {
            vistaAgregar.classList.add('oculto');
            vistaMiembros.classList.remove('oculto');
            cargarMiembros();
            cargarGraficos();
        }

        overlay.querySelector('#btn-ir-a-agregar').addEventListener('click', irAVistaAgregar);
        overlay.querySelector('#btn-volver-a-miembros').addEventListener('click', volverAVistaMiembros);

        overlay.querySelector('#btn-agregar-miembro-grupo').addEventListener('click', async function () {
            ocultarMensaje(mensajeError);
            const idTecnico = selectorTecnico.valor();
            if (!idTecnico) {
                mostrarError(mensajeError, new Error('Elegí un técnico de la lista de sugerencias (no alcanza con escribir el nombre).'));
                return;
            }
            try {
                await apiFetch('/api/grupos-tecnicos/' + idGrupo + '/miembros', {
                    method: 'POST',
                    body: JSON.stringify({ idTecnico: Number(idTecnico) })
                });
                // Se queda en esta pantalla para poder agregar varios seguidos
                // sin ir y volver cada vez; "Volver a la lista" ya refresca.
                mensajeAgregado.textContent = 'Técnico agregado. Podés seguir agregando más.';
                mensajeAgregado.classList.remove('oculto');
                selectorTecnico.limpiar();
            } catch (error) {
                mostrarError(mensajeError, error);
            }
        });

        cargarMiembros();
    }

    // ---------- Resumen de auditoria ----------

    const filaMetricasAuditoria = document.getElementById('fila-metricas-auditoria');

    async function cargarResumenAuditoria() {
        filaMetricasAuditoria.innerHTML =
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">Sesiones activas</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">Cambios hoy</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">Inserciones hoy</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">Actualizaciones hoy</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">Eliminaciones hoy</div></div>' +
            '<div class="tarjeta-metrica"><div class="valor">—</div><div class="etiqueta">Acciones del sistema hoy</div></div>';

        try {
            const r = await apiFetch('/api/auditoria/resumen');
            filaMetricasAuditoria.innerHTML =
                '<div class="tarjeta-metrica"><div class="valor">' + r.sesionesActivas + '</div><div class="etiqueta">Sesiones activas</div></div>' +
                '<div class="tarjeta-metrica"><div class="valor">' + r.cambiosHoy + '</div><div class="etiqueta">Cambios hoy</div></div>' +
                '<div class="tarjeta-metrica"><div class="valor">' + r.inserts + '</div><div class="etiqueta">Inserciones hoy</div></div>' +
                '<div class="tarjeta-metrica"><div class="valor">' + r.updates + '</div><div class="etiqueta">Actualizaciones hoy</div></div>' +
                '<div class="tarjeta-metrica"><div class="valor">' + r.eliminaciones + '</div><div class="etiqueta">Eliminaciones hoy</div></div>' +
                '<div class="tarjeta-metrica"><div class="valor">' + r.accionesSistema + '</div><div class="etiqueta">Acciones del sistema hoy</div></div>';
        } catch (error) {
            console.error('No se pudo cargar el resumen de auditoría:', error);
        }
    }

    // ---------- Auditoria de sesiones (seccion 11) ----------

    const mensajeErrorSesiones = document.getElementById('mensaje-error-sesiones');
    const contenedorTablaSesiones = document.getElementById('contenedor-tabla-sesiones');
    const paginacionSesiones = document.getElementById('paginacion-sesiones');

    let paginaSesiones = 0;
    let idUsuarioSesionesFiltro = null;

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
            if (idUsuarioSesionesFiltro) {
                ruta += '&idUsuario=' + encodeURIComponent(idUsuarioSesionesFiltro);
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

    activarBusquedaRemota(
        'buscar-usuario-sesiones',
        'sugerencias-usuario',
        function (termino) { return apiFetch('/api/auditoria/usuarios/buscar?nombre=' + encodeURIComponent(termino)); },
        {
            onSeleccionar: function (id) { idUsuarioSesionesFiltro = id; paginaSesiones = 0; cargarSesiones(); },
            onLimpiar: function () { idUsuarioSesionesFiltro = null; paginaSesiones = 0; cargarSesiones(); }
        }
    );

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

    // ---------- Configuracion del sistema (nombre/logo/color de marca) ----------
    // No es algo que se toque a diario (a lo sumo cuando el negocio cambia
    // de nombre o quiere personalizar colores), por eso vive detras del
    // icono de engranaje en vez de una pestana fija del menu.

    function abrirModalConfiguracion() {
        const overlay = document.createElement('div');
        overlay.className = 'overlay-modal';
        overlay.innerHTML =
            '<div class="modal">' +
            '<h3>Configuración del sistema</h3>' +
            '<div id="mensaje-error-configuracion" class="mensaje-error oculto"></div>' +
            '<div id="mensaje-exito-configuracion" class="mensaje-info oculto"></div>' +

            '<div class="campo">' +
            '<label>Logo</label>' +
            '<div style="display:flex; align-items:center; gap:12px; margin-bottom:8px;">' +
            '<img id="logo-configuracion-preview" src="" alt="Logo actual" style="max-height:48px; max-width:120px; object-fit:contain; display:none;">' +
            '<span id="logo-configuracion-vacio" class="subtitulo" style="margin:0;">Usando el logo por defecto.</span>' +
            '</div>' +
            '<input type="file" id="input-logo-configuracion" accept="image/png">' +
            '<button type="button" id="btn-subir-logo-configuracion" class="secundario" style="margin-top:8px;">Subir logo</button>' +
            '</div>' +

            '<form id="form-configuracion">' +
            '<div class="campo">' +
            '<label for="nombre-negocio-input">Nombre del negocio</label>' +
            '<input type="text" id="nombre-negocio-input" required maxlength="150">' +
            '</div>' +
            '<div class="campo">' +
            '<label for="categoria-input">Categoría (debajo del logo)</label>' +
            '<input type="text" id="categoria-input" required maxlength="150">' +
            '</div>' +
            '<div class="campo">' +
            '<label for="eslogan-input">Eslogan (pantalla de inicio de sesión)</label>' +
            '<textarea id="eslogan-input" required maxlength="300"></textarea>' +
            '</div>' +
            '<div class="campo">' +
            '<label for="color-primario-input">Color de marca</label>' +
            '<input type="color" id="color-primario-input" style="width:60px; height:36px; padding:2px; cursor:pointer;">' +
            '</div>' +
            '<div class="modal-acciones">' +
            '<button type="button" class="secundario" data-accion="cerrar">Cerrar</button>' +
            '<button type="submit">Guardar</button>' +
            '</div>' +
            '</form>' +
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

        const mensajeError = overlay.querySelector('#mensaje-error-configuracion');
        const mensajeExito = overlay.querySelector('#mensaje-exito-configuracion');
        const imgPreview = overlay.querySelector('#logo-configuracion-preview');
        const spanVacio = overlay.querySelector('#logo-configuracion-vacio');
        const inputNombre = overlay.querySelector('#nombre-negocio-input');
        const inputCategoria = overlay.querySelector('#categoria-input');
        const inputEslogan = overlay.querySelector('#eslogan-input');
        const inputColor = overlay.querySelector('#color-primario-input');

        async function cargarConfiguracionActual() {
            try {
                const respuesta = await fetch(API_BASE + '/api/configuracion');
                const config = await respuesta.json();

                inputNombre.value = config.nombreNegocio || '';
                inputCategoria.value = config.categoria || '';
                inputEslogan.value = config.eslogan || '';
                inputColor.value = config.colorPrimario || '#0d9488';

                if (config.logoUrl) {
                    const version = config.fechaModificacion ? '?v=' + encodeURIComponent(config.fechaModificacion) : '';
                    imgPreview.src = API_BASE + config.logoUrl + version;
                    imgPreview.style.display = '';
                    spanVacio.style.display = 'none';
                } else {
                    imgPreview.style.display = 'none';
                    spanVacio.style.display = '';
                }
            } catch (error) {
                mostrarError(mensajeError, error);
            }
        }

        overlay.querySelector('#btn-subir-logo-configuracion').addEventListener('click', async function () {
            const boton = this;
            const inputArchivo = overlay.querySelector('#input-logo-configuracion');
            const archivo = inputArchivo.files[0];
            if (!archivo) {
                mostrarError(mensajeError, new Error('Elegí un archivo primero.'));
                return;
            }

            ocultarMensaje(mensajeError);
            ocultarMensaje(mensajeExito);
            boton.disabled = true;
            boton.textContent = 'Subiendo...';

            try {
                const datosFormulario = new FormData();
                datosFormulario.append('archivo', archivo);

                const headers = {};
                const token = obtenerToken();
                if (token) headers['Authorization'] = 'Bearer ' + token;

                const respuesta = await fetch(API_BASE + '/api/configuracion/logo', {
                    method: 'POST',
                    headers: headers,
                    body: datosFormulario
                });

                if (!respuesta.ok) {
                    const cuerpo = await respuesta.json().catch(function () { return null; });
                    throw new Error((cuerpo && cuerpo.error) || 'No se pudo subir el logo.');
                }

                inputArchivo.value = '';
                await cargarConfiguracionActual();
                aplicarConfiguracionSistema();
                mensajeExito.textContent = 'Logo actualizado.';
                mensajeExito.classList.remove('oculto');
            } catch (error) {
                mostrarError(mensajeError, error);
            } finally {
                boton.disabled = false;
                boton.textContent = 'Subir logo';
            }
        });

        overlay.querySelector('#form-configuracion').addEventListener('submit', async function (evento) {
            evento.preventDefault();
            ocultarMensaje(mensajeError);
            ocultarMensaje(mensajeExito);

            const btnGuardar = evento.target.querySelector('button[type="submit"]');
            btnGuardar.disabled = true;
            btnGuardar.textContent = 'Guardando...';

            try {
                await apiFetch('/api/configuracion', {
                    method: 'POST',
                    body: JSON.stringify({
                        nombreNegocio: inputNombre.value.trim(),
                        categoria: inputCategoria.value.trim(),
                        eslogan: inputEslogan.value.trim(),
                        colorPrimario: inputColor.value
                    })
                });

                aplicarConfiguracionSistema();
                mensajeExito.textContent = 'Configuración guardada.';
                mensajeExito.classList.remove('oculto');
            } catch (error) {
                mostrarError(mensajeError, error);
            } finally {
                btnGuardar.disabled = false;
                btnGuardar.textContent = 'Guardar';
            }
        });

        cargarConfiguracionActual();
    }

    document.getElementById('btn-abrir-configuracion').addEventListener('click', abrirModalConfiguracion);

    cargarAnunciosActivos('banner-anuncios');
    cargarMetricas();
    cargarGraficos();
    cargarUsuarios();
    cargarGrupos();
    cargarResumenAuditoria();
    cargarSesiones();
    cargarDatos();
    activarNavegacionPorTabs();
})();
