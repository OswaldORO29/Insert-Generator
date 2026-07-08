// ==========================================
// CONFIGURACIÓN Y ELEMENTOS DEL DOM GLOBALES
// ==========================================
const inputBackendUrl = document.getElementById('backend-url');
const btnConectar = document.getElementById('btn-conectar');
const tablaSelect = document.getElementById('tabla-select');
const btnAnalizar = document.getElementById('btn-analizar');
const btnEjecutar = document.getElementById('btn-ejecutar');
const cantidadRegistros = document.getElementById('cantidad-registros');
const tablaVacia = document.getElementById('tabla-vacia');
const tablaEstructuraContainer = document.getElementById('tabla-estructura-container');
const tablaEstructuraBody = document.querySelector('#tabla-estructura tbody');
const progressContainer = document.getElementById('progress-container');
const progressBar = document.getElementById('progress-bar');
const alertBox = document.getElementById('alert-box');

// ====================================================================
// 1. CONECTAR AL SERVIDOR Y TRAER LISTA DE TABLAS (GET /api/metadata/tablas)
// ====================================================================
btnConectar.addEventListener('click', async () => {
    const apiBase = inputBackendUrl.value.trim();
    if (!apiBase) return alert('Por favor, ingresa una URL de API válida');

    try {
        btnConectar.textContent = 'Conectando...';
        const response = await fetch(`${apiBase}/api/metadata/tablas`);
        if (!response.ok) throw new Error('No se pudo conectar al servidor de Spring Boot');

        const tablas = await response.json();

        // Limpiar y llenar el menú desplegable con las tablas reales de Aiven Cloud
        tablaSelect.innerHTML = '<option value="">-- Selecciona una tabla --</option>';
        tablas.forEach(tabla => {
            const option = document.createElement('option');
            option.value = tabla;
            option.textContent = tabla;
            tablaSelect.appendChild(option);
        });

        // Habilitar los controles de la interfaz
        tablaSelect.disabled = false;
        btnAnalizar.disabled = false;
        btnConectar.textContent = 'Conectado ✓';
        mostrarAlerta('Conexión con el Backend establecida con éxito.', 'success');
    } catch (error) {
        btnConectar.textContent = 'Conectar y Cargar Tablas';
        mostrarAlerta(`Error al conectar: ${error.message}. Asegúrate de que tu Backend de Java esté corriendo.`, 'error');
    }
});

// ============================================================================
// 2. ANALIZAR ESTRUCTURA MEDIANTE INTROSPECCIÓN (GET /api/metadata/estructura)
// ============================================================================
btnAnalizar.addEventListener('click', async () => {
    const apiBase = inputBackendUrl.value.trim();
    const tabla = tablaSelect.value;
    if (!tabla) return;

    try {
        btnAnalizar.disabled = true;
        const response = await fetch(`${apiBase}/api/metadata/estructura?tabla=${encodeURIComponent(tabla)}`);
        if (!response.ok) throw new Error('Error al extraer los metadatos de la tabla');

        const columnas = await response.json(); // Cumple tu Contrato 1 de Metadata

        // Ocultar placeholder y mostrar contenedor de la tabla
        tablaVacia.style.display = 'none';
        tablaEstructuraContainer.style.display = 'block';
        tablaEstructuraBody.innerHTML = '';

        // Pintar dinámicamente el Diccionario de Datos en pantalla
        columnas.forEach(col => {
            const tr = document.createElement('tr');

            // Determinar etiquetas visuales de restricciones SQL
            let restriccionLabel = 'Ninguna';
            if (col.isPrimaryKey) restriccionLabel = 'PRIMARY KEY 🔑';
            else if (col.isForeignKey) restriccionLabel = 'FOREIGN KEY 🔗';

            // Indicar el comportamiento de simulación en base a los metadatos
            let estadoSimulacion = col.isAutoIncrement ? 'Ignorado (AutoIncrement)' : 'Listo para IA / Faker';

            tr.innerHTML = `
                <td><strong>${col.name}</strong></td>
                <td><span class="badge badge-info">${col.type}</span></td>
                <td><span class="badge ${col.isPrimaryKey || col.isForeignKey ? 'badge-alert' : ''}">${restriccionLabel}</span></td>
                <td><span class="status-indicator">${estadoSimulacion}</span></td>
            `;
            tablaEstructuraBody.appendChild(tr);
        });

        btnEjecutar.disabled = false;
        mostrarAlerta(`Estructura de la tabla '${tabla}' mapeada dinámicamente de forma correcta.`, 'success');
    } catch (error) {
        mostrarAlerta(`Error de metadatos: ${error.message}`, 'error');
    } finally {
        btnAnalizar.disabled = false;
    }
});

// =======================================================================
// 3. GENERAR E INSERTAR DATOS DE FORMA ROBUSTA (POST /api/insert/ejecutar)
// =======================================================================
btnEjecutar.addEventListener('click', async () => {
    const apiBase = inputBackendUrl.value.trim();
    const tabla = tablaSelect.value;
    const cantidad = parseInt(cantidadRegistros.value) || 5;

    if (!tabla) return;

    // Inicializar estados de animación y alertas
    alertBox.style.display = 'none';
    progressContainer.style.display = 'block';
    progressBar.style.width = '40%';
    btnEjecutar.disabled = true;

    try {
        progressBar.style.width = '70%';

        // Construcción limpia bajo las reglas de tu CONTRATO 2
        // Se le manda la instrucción al Backend para que tu lógica en Java genere datos coherentes
        const solicitudCarga = {
            summary: {
                totalRequested: cantidad,
                fakerGenerated: cantidad, // Indica que use Faker/IA para poblar los registros
                aiGenerated: 0
            },
            records: [] // El Backend de Java rellenará esta lista analizando la metadata e integridad referencial
        };

        // Petición HTTP POST al motor dinámico de persistencia en Java
        const response = await fetch(`${apiBase}/api/insert/ejecutar?tabla=${encodeURIComponent(tabla)}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(solicitudCarga)
        });

        if (!response.ok) throw new Error('El backend falló al procesar, validar o insertar el lote de datos');

        const resultado = await response.json(); // Cumple tu Contrato 3 de Respuesta

        progressBar.style.width = '100%';
        setTimeout(() => {
            // Validar si el motor transaccional de Java reportó algún error de MySQL
            if (resultado.status === 'completed_with_errors' && resultado.insertedCount === 0) {
                mostrarAlerta(`Aviso: El backend procesó la petición pero MySQL rechazó la inserción masiva. Verifica las restricciones de integridad o datos duplicados en la consola.`, 'error');
            } else {
                mostrarAlerta(`¡Éxito Absoluto! Estatus: ${resultado.status}. Se insertaron correctamente ${resultado.insertedCount} registros de forma dinámica en la tabla '${tabla}'.`, 'success');
            }
            progressContainer.style.display = 'none';
            btnEjecutar.disabled = false;
        }, 400);

    } catch (error) {
        progressContainer.style.display = 'none';
        btnEjecutar.disabled = false;
        mostrarAlerta(`Falla en el puente de inserción masiva: ${error.message}`, 'error');
    }
});

// ==========================================
// FUNCIÓN AUXILIAR PARA MOSTRAR ALERTAS
// ==========================================
function mostrarAlerta(mensaje, tipo) {
    alertBox.textContent = mensaje;
    alertBox.style.display = 'block';
    alertBox.className = `alert alert-${tipo}`;
}