(function () {
    activarAlternarContrasena();

    const form = document.getElementById('form-restablecer');
    const mensajeError = document.getElementById('mensaje-error');
    const mensajeExito = document.getElementById('mensaje-exito');
    const btnRestablecer = document.getElementById('btn-restablecer');

    // Si se llego por el link del correo de recuperacion (?token=...), rellenar solo.
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

        btnRestablecer.disabled = true;
        btnRestablecer.textContent = 'Restableciendo...';

        try {
            const token = document.getElementById('token').value.trim();

            const respuesta = await apiFetch('/api/auth/restablecer', {
                method: 'POST',
                body: JSON.stringify({ token, contrasena })
            });

            form.classList.add('oculto');
            mensajeExito.textContent = respuesta.mensaje + ' Ya puedes iniciar sesión.';
            mensajeExito.classList.remove('oculto');
        } catch (error) {
            mostrarError(mensajeError, error);
            btnRestablecer.disabled = false;
            btnRestablecer.textContent = 'Restablecer contraseña';
        }
    });
})();
