const tablaSelect = document.getElementById('tablaSelect');
const cantidadInput = document.getElementById('cantidadInput');
const generarBtn = document.getElementById('generarBtn');
const insertarBtn = document.getElementById('insertarBtn');
const conectarBtn = document.getElementById('conectarBtn');
const statusEl = document.getElementById('status');
const estructuraEl = document.getElementById('estructuraContenido');
const resumenEl = document.getElementById('resumen');
const previewEl = document.getElementById('previewOutput');
const apiBaseInput = document.getElementById('input-url-backend');

let lastResponse = null;

function getApiBase() {
  const rawValue = (apiBaseInput?.value || '').trim();
  if (!rawValue) {
    return window.location.origin;
  }

  const normalized = rawValue.replace(/\/+$/, '');
  return /^https?:\/\//i.test(normalized) ? normalized : `http://${normalized}`;
}

function buildApiUrl(path) {
  return `${getApiBase()}${path.startsWith('/') ? path : `/${path}`}`;
}

function setStatus(message, type = '') {
  statusEl.textContent = message;
  statusEl.className = `status ${type}`.trim();
}

async function probarConexion() {
  try {
    const response = await fetch(buildApiUrl('/api/metadata/tablas'));
    if (!response.ok) throw new Error('No se pudo conectar con el backend');

    setStatus('Conexión establecida con el backend.', 'success');
    await cargarTablas();
  } catch (error) {
    setStatus(error.message, 'error');
  }
}

async function cargarTablas() {
  try {
    const response = await fetch(buildApiUrl('/api/metadata/tablas'));
    if (!response.ok) throw new Error('No se pudieron cargar las tablas');

    const tablas = await response.json();
    tablaSelect.innerHTML = '';

    if (!tablas.length) {
      const option = document.createElement('option');
      option.textContent = 'No hay tablas disponibles';
      tablaSelect.appendChild(option);
      return;
    }

    tablas.forEach((tabla) => {
      const option = document.createElement('option');
      option.value = tabla;
      option.textContent = tabla;
      tablaSelect.appendChild(option);
    });

    tablaSelect.value = tablas[0];
    await cargarEstructura(tablas[0]);
  } catch (error) {
    setStatus(error.message, 'error');
  }
}

async function cargarEstructura(tabla) {
  if (!tabla) return;

  try {
    const response = await fetch(buildApiUrl(`/api/metadata/estructura?tabla=${encodeURIComponent(tabla)}`));
    if (!response.ok) throw new Error('No se pudo cargar la estructura de la tabla');

    const estructura = await response.json();

    if (!estructura.length) {
      estructuraEl.innerHTML = '<div class="empty-state">No se encontró estructura para esta tabla.</div>';
      return;
    }

    estructuraEl.innerHTML = `
      <div class="column-list">
        ${estructura.map((col) => `
          <div class="column-item">
            <div><b>${col.name}</b> · ${col.type}</div>
            <div>PK: ${col.isPrimaryKey ? 'Sí' : 'No'} · Nullable: ${col.isNullable ? 'Sí' : 'No'} · AutoIncrement: ${col.isAutoIncrement ? 'Sí' : 'No'}</div>
          </div>
        `).join('')}
      </div>
    `;
  } catch (error) {
    estructuraEl.innerHTML = '<div class="empty-state">No fue posible mostrar la estructura.</div>';
    setStatus(error.message, 'error');
  }
}

async function generarDatos() {
  const tabla = tablaSelect.value;
  const cantidad = Number(cantidadInput.value);

  if (!tabla) {
    setStatus('Selecciona una tabla antes de generar datos.', 'warning');
    return;
  }

  try {
    setStatus(`Generando ${cantidad} registros para ${tabla}...`, 'warning');
    const response = await fetch(buildApiUrl(`/api/generate/datos?tabla=${encodeURIComponent(tabla)}&cantidad=${cantidad}`));
    const text = await response.text();
    let data = null;

    try {
      data = text ? JSON.parse(text) : null;
    } catch {
      data = null;
    }

    if (!response.ok) {
      throw new Error(data?.message || text || 'No se pudieron generar los datos');
    }

    lastResponse = data;
    renderPreview(data);
    setStatus(`Datos generados correctamente para ${tabla}.`, 'success');
  } catch (error) {
    setStatus(error.message, 'error');
  }
}

function renderPreview(data) {
  const summary = data?.summary || {};
  const records = data?.records || [];

  resumenEl.innerHTML = `
    <span class="pill">Solicitados: ${summary.totalRequested ?? 0}</span>
    <span class="pill">Faker: ${summary.fakerGenerated ?? 0}</span>
    <span class="pill">IA: ${summary.aiGenerated ?? 0}</span>
    <span class="pill">Registros: ${records.length}</span>
  `;

  previewEl.textContent = JSON.stringify(data, null, 2);
}

async function insertarDatos() {
  if (!lastResponse) {
    setStatus('Primero genera una vista previa de datos para insertar.', 'warning');
    return;
  }

  try {
    const tabla = tablaSelect.value;
    setStatus('Insertando registros en la base de datos...', 'warning');
    const response = await fetch(buildApiUrl(`/api/insert/ejecutar?tabla=${encodeURIComponent(tabla)}`), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        summary: lastResponse.summary,
        records: lastResponse.records
      })
    });

    const text = await response.text();
    let data = null;

    try {
      data = text ? JSON.parse(text) : null;
    } catch {
      data = null;
    }

    if (!response.ok) {
      throw new Error(data?.message || text || 'No se pudieron insertar los registros');
    }

    previewEl.textContent = JSON.stringify(data, null, 2);
    setStatus('Inserción completada correctamente.', 'success');
  } catch (error) {
    setStatus(error.message, 'error');
  }
}

tablaSelect.addEventListener('change', (event) => cargarEstructura(event.target.value));
generarBtn.addEventListener('click', generarDatos);
insertarBtn.addEventListener('click', insertarDatos);
conectarBtn.addEventListener('click', probarConexion);

window.addEventListener('DOMContentLoaded', () => {
  apiBaseInput.value = window.location.origin;
  cargarTablas();
});