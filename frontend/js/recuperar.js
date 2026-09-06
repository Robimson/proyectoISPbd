(function () {
    aplicarConfiguracionSistema();
    const form = document.getElementById('form-recuperar');
    const mensajeError = document.getElementById('mensaje-error');
    const mensajeExito = document.getElementById('mensaje-exito');
    const btnRecuperar = document.getElementById('btn-recuperar');

    form.addEventListener('submit', async function (evento) {
        evento.preventDefault();
        ocultarMensaje(mensajeError);
        mensajeExito.classList.add('oculto');

        btnRecuperar.disabled = true;
        btnRecuperar.textContent = 'Enviando...';

        try {
            const correo = document.getElementById('correo').value.trim();

            const respuesta = await apiFetch('/api/auth/recuperacion', {
                method: 'POST',
                body: JSON.stringify({ correo })
            });

            form.classList.add('oculto');
            mensajeExito.textContent = respuesta.mensaje;
            mensajeExito.classList.remove('oculto');
        } catch (error) {
            mostrarError(mensajeError, error);
            btnRecuperar.disabled = false;
            btnRecuperar.textContent = 'Enviar enlace';
        }
    });
})();
