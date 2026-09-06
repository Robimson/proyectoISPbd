(function () {
    aplicarConfiguracionSistema();
    activarAlternarContrasena();

    const form = document.getElementById('form-activar');
    const mensajeError = document.getElementById('mensaje-error');
    const mensajeExito = document.getElementById('mensaje-exito');
    const btnActivar = document.getElementById('btn-activar');

    // Si se llego por el link del correo de invitacion (?token=...), rellenar solo.
    const tokenDeLaUrl = new URLSearchParams(window.location.search).get('token');
    if (tokenDeLaUrl) {
        document.getElementById('token').value = tokenDeLaUrl;
    }

    form.addEventListener('submit', async function (evento) {
        evento.preventDefault();
        ocultarMensaje(mensajeError);
        mensajeExito.classList.add('oculto');

        const contrasena = document.getElementById('contrasena').value;
        const confirmar = document.getElementById('confirmar').value;

        if (contrasena !== confirmar) {
            mostrarError(mensajeError, new Error('Las contraseñas no coinciden.'));
            return;
        }

        btnActivar.disabled = true;
        btnActivar.textContent = 'Activando...';

        try {
            const token = document.getElementById('token').value.trim();

            const usuario = await apiFetch('/api/usuarios/activacion', {
                method: 'POST',
                body: JSON.stringify({ token, contrasena })
            });

            form.classList.add('oculto');
            mensajeExito.textContent = 'Cuenta activada para ' + usuario.correo + '. Ya puedes iniciar sesión.';
            mensajeExito.classList.remove('oculto');
        } catch (error) {
            mostrarError(mensajeError, error);
            btnActivar.disabled = false;
            btnActivar.textContent = 'Activar cuenta';
        }
    });
})();
