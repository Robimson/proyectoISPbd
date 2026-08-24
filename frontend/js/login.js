(function () {
    // Si ya hay sesion activa, saltar directo al panel del rol.
    const rolActual = obtenerRol();
    if (obtenerToken() && rolActual && PAGINA_POR_ROL[rolActual]) {
        window.location.href = PAGINA_POR_ROL[rolActual];
        return;
    }

    activarAlternarContrasena();

    const form = document.getElementById('form-login');
    const mensajeError = document.getElementById('mensaje-error');
    const btnLogin = document.getElementById('btn-login');

    form.addEventListener('submit', async function (evento) {
        evento.preventDefault();
        ocultarMensaje(mensajeError);
        btnLogin.disabled = true;
        btnLogin.textContent = 'Ingresando...';

        try {
            const correo = document.getElementById('correo').value.trim();
            const contrasena = document.getElementById('contrasena').value;

            const respuesta = await apiFetch('/api/auth/login', {
                method: 'POST',
                body: JSON.stringify({ correo, contrasena })
            });

            guardarSesion(respuesta.token, respuesta.idUsuario, respuesta.rol, respuesta.estadoPago, respuesta.idSesion);

            const pagina = PAGINA_POR_ROL[respuesta.rol];
            window.location.href = pagina || 'login.html';
        } catch (error) {
            mostrarError(mensajeError, error);
        } finally {
            btnLogin.disabled = false;
            btnLogin.textContent = 'Iniciar sesión';
        }
    });
})();
