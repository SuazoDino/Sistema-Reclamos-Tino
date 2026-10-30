/*
 * Sistema de Reclamos - version web.
 *
 * Reclamo -> protocolo -> eventos -> tickets -> movimientos. El navegador
 * solo muestra y envia: todas las reglas (validacion, asignacion, toma de
 * tickets, control de calidad) las aplica el servidor Java.
 */

const vista = document.getElementById('vista');
const SELECCIONE = '— Seleccione —';

/* ---------------- utilidades ---------------- */

/*
 * Llamada a la API. Distingue tres fallas: sin conexion con el servidor, una
 * respuesta que no es JSON (el servidor se cayo a medias) y un rechazo de
 * negocio, que trae su mensaje y a veces datos para ponerse al dia (err.datos).
 */
async function api(ruta, datos) {
  const opciones = datos === undefined ? {} : {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams(datos)
  };
  let r;
  try {
    r = await fetch('/api/' + ruta, opciones);
  } catch {
    throw new Error('No hay conexión con el servidor. Verifique que esté encendido y vuelva a intentarlo.');
  }
  let json;
  try {
    json = await r.json();
  } catch {
    throw new Error('El servidor respondió algo inesperado (' + r.status + '). Vuelva a intentarlo.');
  }
  if (!r.ok) {
    const err = new Error(json.error || 'Error ' + r.status);
    err.datos = json;
    err.estado = r.status;
    throw err;
  }
  return json;
}

/* Mientras una operacion esta en curso, sus botones no se pueden volver a pulsar. */
async function ocupado(contenedor, tarea) {
  const botones = [...contenedor.querySelectorAll('button')].filter(b => !b.disabled);
  botones.forEach(b => { b.disabled = true; });
  try {
    return await tarea();
  } finally {
    botones.forEach(b => { b.disabled = false; });
  }
}

function esc(v) {
  return String(v ?? '').replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
}

const dos = n => String(n).padStart(2, '0');

function fecha(iso) {
  if (!iso) return '-';
  const d = new Date(iso);
  return `${dos(d.getDate())}/${dos(d.getMonth() + 1)}/${d.getFullYear()} ${dos(d.getHours())}:${dos(d.getMinutes())}`;
}

function hora(iso) {
  if (!iso) return '-';
  const d = new Date(iso);
  return `${dos(d.getHours())}:${dos(d.getMinutes())}`;
}

function fechaLarga(iso) {
  const d = new Date(iso);
  return `${d.toLocaleDateString('es-PE', { weekday: 'long' })} ${fecha(iso).replace(' ', ' a las ')}`;
}

function hoyIso() {
  const d = new Date();
  return `${d.getFullYear()}-${dos(d.getMonth() + 1)}-${dos(d.getDate())}`;
}

function clase(texto) {
  return String(texto ?? '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/\s+/g, '-');
}

function etiqueta(texto) {
  return `<span class="etiqueta ${clase(texto)}">${esc(texto)}</span>`;
}

function opcionesHtml(valores, elegido) {
  return `<option value="">${SELECCIONE}</option>` + valores.map(v => {
    const valor = typeof v === 'object' ? v.valor : v;
    const texto = typeof v === 'object' ? v.texto : v;
    return `<option value="${esc(valor)}" ${valor === elegido ? 'selected' : ''}>${esc(texto)}</option>`;
  }).join('');
}

function aviso(tipo, html) {
  return `<div class="aviso ${tipo}" role="${tipo === 'error' ? 'alert' : 'status'}">${html}</div>`;
}

function dialogo(titulo, html) {
  document.getElementById('dialogo-titulo').textContent = titulo;
  document.getElementById('dialogo-contenido').innerHTML = html;
  document.getElementById('dialogo').showModal();
}

function tabla(columnas, filas, alElegir) {
  const id = 't' + Math.random().toString(36).slice(2);
  setTimeout(() => {
    const t = document.getElementById(id);
    if (!t || !alElegir) return;
    t.querySelectorAll('tbody tr').forEach((tr, i) => tr.addEventListener('click', () => {
      t.querySelectorAll('tr').forEach(x => x.classList.remove('seleccionada'));
      tr.classList.add('seleccionada');
      alElegir(i);
    }));
  });
  if (!filas.length) return '<p class="vacio">No hay registros.</p>';
  return `<div class="tabla-envoltura"><table id="${id}">
    <thead><tr>${columnas.map(c => `<th>${esc(c)}</th>`).join('')}</tr></thead>
    <tbody>${filas.map(f => `<tr>${f.map(c => `<td>${c}</td>`).join('')}</tr>`).join('')}</tbody>
  </table></div>`;
}

function datos(pares) {
  return `<dl class="datos">${pares.filter(Boolean).map(([k, v]) =>
    `<dt>${esc(k)}</dt><dd>${v}</dd>`).join('')}</dl>`;
}

function avance(r) {
  const total = Math.max(r.totalPasos, 1);
  const pct = Math.round(100 * r.pasosCompletados / total);
  return `<div class="avance" title="Pasos completados del protocolo">
    <div><i style="width:${pct}%"></i></div><span>${r.pasosCompletados} de ${r.totalPasos} pasos</span></div>`;
}

function lineaEventos(r) {
  if (!r.eventos.length) return '<p class="vacio">Sin eventos.</p>';
  return `<ul class="linea">${r.eventos.map(e => `<li class="${clase(e.estado)}">
    <strong>${esc(e.evento)}</strong> ${etiqueta(e.estado)}
    <small>${e.inicio ? 'Cita ' + fecha(e.inicio) + ' – ' + hora(e.fin) + ' · ' : ''}${esc(e.area)} · ${esc(e.especialista)}</small>
    ${e.tickets && e.tickets.length ? `<small>Tickets: ${e.tickets.length} turno${e.tickets.length > 1 ? 's' : ''} (${esc(e.tickets[0])}${e.tickets.length > 1 ? ' …' : ''})</small>` : ''}
    ${!e.dentroDelPlazo ? '<small>Sin capacidad dentro del plazo</small>' : ''}
    ${e.resultado ? `<small>Resultado: ${esc(e.resultado)}</small>` : ''}
  </li>`).join('')}</ul>`;
}

function tablaMovimientos(r) {
  return tabla(['Evento', 'Paso', 'Acción', 'Estimado', 'Real', 'Desvío', 'Ticket'],
    r.movimientos.map(m => [esc(m.evento), m.paso, esc(m.accion),
      `${hora(m.fecPasoEstimado)}–${hora(m.fecFinPasoEstimado)}`,
      `${hora(m.fecPaso)}–${hora(m.fecFinPaso)}`,
      `${m.desvio > 0 ? '+' : ''}${m.desvio} min`, esc(m.ticket)]));
}

/* ---------------- cliente: registrar reclamo ---------------- */

const formulario = { cliente: null, sel: {} };

function vistaRegistrar() {
  formulario.cliente = null;
  formulario.sel = {};
  vista.innerHTML = `
    <h1>Formulario de reclamo</h1>
    <p class="subtitulo">El sistema valida el reclamo, le asigna su protocolo y toma un ticket (turno) para atenderlo.</p>
    <section class="tarjeta">
      <h2>Datos del cliente</h2>
      <form id="f-cliente" class="fila">
        <div class="campo" id="c-DOCUMENTO">
          <label for="documento">DNI/RUC</label>
          <input type="text" id="documento" inputmode="numeric" placeholder="Ej. 72119474" autocomplete="off">
          <small></small>
        </div>
        <button class="primario">Validar</button>
      </form>
      <div id="datos-cliente"></div>
    </section>
    <div id="zona-reclamo"></div>`;

  document.getElementById('f-cliente').addEventListener('submit', async e => {
    e.preventDefault();
    const doc = document.getElementById('documento').value.trim();
    const campo = document.getElementById('c-DOCUMENTO');
    campo.classList.remove('invalido');
    campo.querySelector('small').textContent = '';
    try {
      formulario.cliente = await api('clientes/' + encodeURIComponent(doc));
      formulario.sel = {};
      const c = formulario.cliente;
      document.getElementById('datos-cliente').innerHTML = `<div class="rejilla" style="margin-top:12px">
        ${[['Nombre', c.nombres], ['Apellido', c.apellidos], ['Correo', c.correo],
           ['Teléfono', c.telefono], ['Tipo de cliente', c.tipo]].map(([k, v]) =>
          `<div class="campo"><label>${k}</label><input type="text" readonly value="${esc(v)}"></div>`).join('')}
      </div>`;
      await pintarReclamo();
    } catch (err) {
      formulario.cliente = null;
      document.getElementById('datos-cliente').innerHTML = '';
      document.getElementById('zona-reclamo').innerHTML = '';
      campo.classList.add('invalido');
      campo.querySelector('small').textContent = err.message.replace('DNI/RUC: ', '');
    }
  });
}

async function pintarReclamo() {
  const s = formulario.sel;
  const q = new URLSearchParams({ documento: formulario.cliente.documento });
  if (s.tipoObjeto) q.set('tipoObjeto', s.tipoObjeto);
  if (s.objeto) q.set('objeto', s.objeto);
  if (s.tipoProblema) q.set('tipoProblema', s.tipoProblema);
  const o = await api('opciones?' + q);

  const presencial = s.canal === 'PRESENCIAL';
  const compra = o.compra;
  document.getElementById('zona-reclamo').innerHTML = `
    <form id="f-reclamo">
      <section class="tarjeta">
        <h2>Datos del reclamo</h2>
        <div id="resumen-errores"></div>
        <div class="rejilla">
          <div class="campo" id="c-CANAL"><label for="canal">Canal de ingreso</label>
            <select id="canal">${opcionesHtml(o.canales, s.canal)}</select><small></small></div>
          <div class="campo" id="c-LOCAL"><label for="local">Local</label>
            <select id="local" ${presencial ? '' : 'disabled'}>${opcionesHtml(o.locales, s.local)}</select>
            <small></small></div>
          <div class="campo" id="c-TIPO_OBJETO"><label>Reclama sobre</label>
            <div class="radios">
              <label><input type="radio" name="tipoObjeto" value="PRODUCTO" ${s.tipoObjeto === 'PRODUCTO' ? 'checked' : ''}> Producto</label>
              <label><input type="radio" name="tipoObjeto" value="SERVICIO" ${s.tipoObjeto === 'SERVICIO' ? 'checked' : ''}> Servicio</label>
            </div><small></small></div>
          <div class="campo" id="c-OBJETO"><label for="objeto">Producto o servicio</label>
            <select id="objeto">${opcionesHtml(o.objetos, s.objeto)}</select><small></small></div>
          <div class="campo" id="c-TIPO_PROBLEMA"><label for="tipoProblema">Tipo de problema</label>
            <select id="tipoProblema">${opcionesHtml(o.tipos, s.tipoProblema)}</select><small></small></div>
          <div class="campo" id="c-PROBLEMA"><label for="problema">Problema</label>
            <select id="problema">${opcionesHtml(o.problemas, s.problema)}</select><small></small></div>
        </div>
        ${o.naturaleza ? `<p class="subtitulo" style="margin:12px 0 0">Se registrará como <strong>${esc(o.naturaleza)}</strong>${
          o.naturaleza === 'Queja' ? ': malestar con la atención, no con el producto o servicio.' : ': disconformidad con el producto o servicio.'}</p>` : ''}
        <div class="rejilla" style="margin-top:12px">
          <div class="campo" id="c-DETALLE" style="grid-column:1/-1"><label for="detalle">Detalle: ¿qué pasó?</label>
            <textarea id="detalle" rows="3" maxlength="${o.detalleMax}" placeholder="Cuente con sus palabras qué pasó y desde cuándo.">${esc(s.detalle || '')}</textarea><small></small></div>
          <div class="campo" id="c-PEDIDO"><label for="pedido">Pedido del consumidor</label>
            <select id="pedido">${opcionesHtml(o.pedidos, s.pedido)}</select><small></small></div>
        </div>
      </section>
      <section class="tarjeta">
        <h2>Sustento de la garantía</h2>
        ${!s.objeto ? '' : compra
          ? aviso('info', `Compra verificada por S/ ${compra.monto.toFixed(2)}.`)
          : aviso('alerta', 'Sin compra registrada: solo puede reclamar por Atención.')}
        <div class="rejilla">
          <div class="campo" id="c-COMPROBANTE"><label>Nro de comprobante</label>
            <input type="text" readonly value="${esc(compra?.comprobante)}"><small></small></div>
          <div class="campo" id="c-FECHA_COMPROBANTE"><label>Fecha del comprobante</label>
            <input type="text" readonly value="${esc(compra?.fecha)}"><small></small></div>
        </div>
        <p class="subtitulo" style="margin:12px 0 0">El comprobante sale de la compra registrada a su nombre.
          Solo los reclamos de Atención (mal trato, demora) no necesitan compra.</p>
        <div class="acciones">
          <button type="button" id="cancelar">Cancelar</button>
          <button class="primario">Registrar reclamo</button>
        </div>
      </section>
    </form>`;

  const cambiar = (clave, limpiar) => async e => {
    s[clave] = e.target.value || null;
    limpiar.forEach(k => delete s[k]);
    await pintarReclamo();
  };
  document.getElementById('canal').addEventListener('change', cambiar('canal', ['local']));
  document.getElementById('local').addEventListener('change', e => { s.local = e.target.value || null; });
  document.querySelectorAll('[name=tipoObjeto]').forEach(r =>
    r.addEventListener('change', cambiar('tipoObjeto', ['objeto', 'tipoProblema', 'problema'])));
  document.getElementById('objeto').addEventListener('change', cambiar('objeto', ['tipoProblema', 'problema']));
  document.getElementById('tipoProblema').addEventListener('change', cambiar('tipoProblema', ['problema']));
  document.getElementById('problema').addEventListener('change', e => { s.problema = e.target.value || null; });
  // el detalle se guarda al escribir: un cambio de lista repinta el formulario y no debe borrarlo
  document.getElementById('detalle').addEventListener('input', e => { s.detalle = e.target.value; });
  document.getElementById('pedido').addEventListener('change', e => { s.pedido = e.target.value || null; });
  document.getElementById('cancelar').addEventListener('click', vistaRegistrar);
  document.getElementById('f-reclamo').addEventListener('submit', registrar);
}

async function registrar(e) {
  e.preventDefault();
  const s = formulario.sel;
  const envio = { documento: formulario.cliente.documento };
  ['canal', 'local', 'tipoObjeto', 'objeto', 'tipoProblema', 'problema', 'detalle', 'pedido']
    .forEach(k => { if (s[k]) envio[k] = s[k]; });

  document.querySelectorAll('.campo').forEach(c => {
    c.classList.remove('invalido');
    const m = c.querySelector('small');
    if (m) m.textContent = '';
  });
  const resumen = document.getElementById('resumen-errores');

  let r;
  try {
    r = await ocupado(e.target, () => api('reclamos', envio));
  } catch (err) {
    resumen.innerHTML = aviso('error', esc(err.message));
    return;
  }
  const errores = Object.entries(r.errores);
  if (errores.length) {
    errores.forEach(([campo, { error }]) => {
      const c = document.getElementById('c-' + campo);
      if (c) { c.classList.add('invalido'); c.querySelector('small').textContent = error; }
    });
    resumen.innerHTML = aviso('error', 'Corrija los campos marcados en rojo:<ul>' +
      errores.map(([, x]) => `<li>${esc(x.etiqueta)}: ${esc(x.error)}</li>`).join('') + '</ul>');
    resumen.scrollIntoView({ behavior: 'smooth', block: 'center' });
    return;
  }
  if (r.rechazos.length) {
    resumen.innerHTML = aviso('error', '<strong>El reclamo no procede.</strong><ul>' +
      r.rechazos.map(x => `<li>${esc(x)}</li>`).join('') + '</ul>');
    resumen.scrollIntoView({ behavior: 'smooth', block: 'center' });
    return;
  }

  const rc = r.reclamo;
  const ev = rc.eventos.find(x => x.inicio);
  dialogo('Reclamo ' + rc.numero + ' registrado', `
    <div class="cita">Su reclamo será atendido el
      <strong>${esc(fechaLarga(ev.inicio))}</strong>por ${esc(ev.area)} (${esc(ev.especialista)}).</div>
    ${rc.sinCapacidad ? aviso('alerta', 'Sin capacidad dentro del plazo: la gerencia fue notificada.') : ''}
    ${datos([
      ['Naturaleza', esc(rc.naturaleza)],
      ['Reclama sobre', esc(rc.tipoObjeto + ': ' + rc.objeto)],
      ['Problema', esc(rc.tipoProblema + ' - ' + rc.problema)],
      ['Pedido', esc(rc.pedido)],
      ['Prioridad', etiqueta(rc.prioridad)],
      ['Protocolo', esc(`${rc.protocolo} (${rc.totalPasos} pasos)`)],
      ['Primer evento', esc(ev.evento)],
      ['Ticket', esc(`${ev.tickets[0]}${ev.tickets.length > 1 ? ` y ${ev.tickets.length - 1} más` : ''}`)],
      ['Límite de atención', fecha(rc.limiteAtencion)],
      ['Sustento', esc(rc.comprobante ? rc.comprobante + (rc.fueraDeGarantia ? ' - Fuera de garantía' : '') : 'Sin comprobante (Atención)')]
    ])}`);
  vistaRegistrar();
}

/* ---------------- cliente: estado del reclamo ---------------- */

function mensajeCliente(r) {
  const cita = r.fechaProgramada ? ` Será atendido el ${fecha(r.fechaProgramada)}.` : '';
  switch (r.estadoCodigo) {
    case 'ASIGNADO': return `Su reclamo fue asignado a ${r.area}.${cita}`;
    case 'EN_ATENCION': return `Su reclamo está en atención por ${r.area}.${cita}`;
    case 'IMPUGNADO': return 'Su impugnación fue recibida y el reclamo pasó a la siguiente instancia.';
    case 'RESUELTO': return 'Su reclamo fue resuelto y pasa a control de calidad.';
    case 'EN_REVISION': return r.esperaConformidad
      ? `Su reclamo fue atendido con ${r.solucionAplicada}. Cuéntenos si quedó solucionado.`
      : 'Su reclamo está en control de calidad. Le avisaremos el resultado.';
    case 'RECHAZADO': return r.puedeImpugnar
      ? `Su reclamo fue rechazado: ${r.motivoRechazo || 'no procede'}. Puede impugnar hasta el ${fecha(r.limiteImpugnacion)}.`
      : 'Su reclamo fue rechazado y ya no admite impugnación.';
    case 'CERRADO': return 'Su reclamo fue atendido y cerrado.';
    default: return 'Su reclamo fue registrado.';
  }
}

let documentoEstado = '';

function vistaEstado() {
  vista.innerHTML = `
    <h1>Estado de reclamo</h1>
    <p class="subtitulo">Consulte cuándo se atiende su reclamo y cuánto avanzó.</p>
    <section class="tarjeta">
      <form id="f-estado" class="fila">
        <div class="campo"><label for="doc-estado">DNI/RUC</label>
          <input type="text" id="doc-estado" value="${esc(documentoEstado)}" placeholder="Ej. 72119474"></div>
        <button class="primario">Consultar</button>
      </form>
    </section>
    <div id="estado-resultado"></div>`;
  document.getElementById('f-estado').addEventListener('submit', e => {
    e.preventDefault();
    documentoEstado = document.getElementById('doc-estado').value.trim();
    cargarEstado();
  });
  if (documentoEstado) cargarEstado();
}

async function cargarEstado(seleccion) {
  const zona = document.getElementById('estado-resultado');
  try {
    const reclamos = await api('clientes/' + encodeURIComponent(documentoEstado) + '/reclamos');
    zona.innerHTML = `<div class="dos-columnas">
      <section class="tarjeta"><h2>Sus reclamos</h2>${tabla(
        ['Reclamo', 'Objeto', 'Estado', 'Fecha programada', 'Avance'],
        reclamos.map(r => [esc(r.numero), esc(r.objeto), etiqueta(r.estado), fecha(r.fechaProgramada),
          `${r.pasosCompletados}/${r.totalPasos}`]),
        i => detalleCliente(reclamos[i]))}</section>
      <section class="tarjeta" id="detalle-cliente"><p class="vacio">Elija un reclamo para ver el detalle.</p></section>
    </div>`;
    const i = reclamos.findIndex(r => r.numero === seleccion);
    if (i >= 0) setTimeout(() => document.querySelectorAll('#estado-resultado tbody tr')[i]?.click());
  } catch (err) {
    zona.innerHTML = aviso('error', esc(err.message));
  }
}

function detalleCliente(r) {
  const zona = document.getElementById('detalle-cliente');
  zona.innerHTML = `
    <h2>Reclamo ${esc(r.numero)}</h2>
    ${aviso(r.estadoCodigo === 'RECHAZADO' ? 'alerta' : 'info', esc(mensajeCliente(r)))}
    ${avance(r)}
    ${datos([
      ['Naturaleza', esc(r.naturaleza)],
      ['Reclama sobre', esc(r.objeto)],
      ['Problema', esc(r.tipoProblema + ' - ' + r.problema)],
      ['Su pedido', esc(r.pedido)],
      ['Instancia', esc(r.instancia)],
      ['Área a cargo', esc(r.area)],
      ['Fecha programada', fecha(r.fechaProgramada)],
      ['Límite de atención', fecha(r.limiteAtencion)],
      r.solucionAplicada && ['Solución', esc(r.solucionAplicada)]
    ])}
    <h2 style="margin-top:16px">Eventos de su protocolo</h2>${lineaEventos(r)}
    <div id="accion-cliente"></div>`;

  const accion = document.getElementById('accion-cliente');
  if (r.esperaConformidad) {
    accion.innerHTML = `<form id="f-conf" class="tarjeta" style="margin:16px 0 0">
      <h2>¿Su reclamo quedó solucionado?</h2>
      <div class="radios"><label><input type="radio" name="conforme" value="true" checked> Sí</label>
        <label><input type="radio" name="conforme" value="false"> No</label></div>
      <div class="campo" style="max-width:160px;margin-top:8px"><label for="nota">Nota (1 a 5)</label>
        <select id="nota">${[5, 4, 3, 2, 1].map(n => `<option>${n}</option>`).join('')}</select></div>
      <div class="acciones"><button class="primario">Enviar</button></div></form>`;
    document.getElementById('f-conf').addEventListener('submit', e => enviarCliente(e, r, 'conformidad', {
      conforme: document.querySelector('[name=conforme]:checked').value,
      nota: document.getElementById('nota').value
    }));
  } else if (r.puedeImpugnar) {
    accion.innerHTML = `<form id="f-imp" class="tarjeta" style="margin:16px 0 0">
      <h2>Impugnar</h2><p class="subtitulo">Pasa a ${esc(r.siguienteInstancia)}.</p>
      <div class="campo"><label for="motivo-imp">Motivo</label><textarea id="motivo-imp" rows="3"></textarea></div>
      <div class="acciones"><button class="primario">Enviar impugnación</button></div></form>`;
    document.getElementById('f-imp').addEventListener('submit', e => enviarCliente(e, r, 'impugnar', {
      motivo: document.getElementById('motivo-imp').value
    }));
  }
}

async function enviarCliente(e, r, accion, envio) {
  e.preventDefault();
  try {
    const x = await api(`reclamos/${r.numero}/${accion}`, envio);
    dialogo('Listo', `<p>${esc(x.mensaje)}</p>`);
    cargarEstado(r.numero);
  } catch (err) {
    dialogo('No se pudo enviar', `<p>${esc(err.message)}</p>`);
  }
}

/* ---------------- area: atender reclamo ---------------- */

let empleadoId = '';
let eventoAbierto = '';

function vistaAtender() {
  vista.innerHTML = `
    <h1>Atender reclamo</h1>
    <p class="subtitulo">Sus tickets del parque, en orden. Ejecute los pasos del protocolo uno por uno: cada paso queda registrado con su hora real.</p>
    <section class="tarjeta">
      <form id="f-emp" class="fila">
        <div class="campo"><label for="id-emp">ID de operario</label>
          <input type="text" id="id-emp" value="${esc(empleadoId || 'E01')}"></div>
        <button class="primario">Ingresar</button>
      </form>
      <div id="emp-datos"></div>
    </section>
    <div id="agenda"></div>`;
  document.getElementById('f-emp').addEventListener('submit', e => {
    e.preventDefault();
    empleadoId = document.getElementById('id-emp').value.trim().toUpperCase();
    eventoAbierto = '';
    cargarAgenda();
  });
  if (empleadoId) cargarAgenda();
}

async function cargarAgenda(mensaje) {
  const zona = document.getElementById('agenda');
  try {
    const a = await api('empleados/' + encodeURIComponent(empleadoId) + '/agenda');
    document.getElementById('emp-datos').innerHTML = `<p class="subtitulo" style="margin:12px 0 0">
      ${esc(a.empleado.id)} · ${esc(a.empleado.tipo)} · ${esc(a.empleado.area)}</p>`;
    zona.innerHTML = `${mensaje ? aviso('exito', esc(mensaje)) : ''}<div class="dos-columnas">
      <section class="tarjeta"><h2>Mis tickets</h2>${tabla(
        ['Ticket (cita)', 'Reclamo', 'Evento', 'Cliente', 'Prioridad', 'Avance'],
        a.eventos.map(r => [fecha(r.fechaProgramada), esc(r.numero), esc(r.eventoActual), esc(r.cliente),
          etiqueta(r.prioridad), `${r.pasosCompletados}/${r.totalPasos}`]),
        i => { eventoAbierto = a.eventos[i].numero; inspeccionar(a.eventos[i], a.soluciones); })}</section>
      <section class="tarjeta" id="inspeccion"><p class="vacio">Elija un ticket para atenderlo.</p></section>
    </div>`;
    const i = a.eventos.findIndex(r => r.numero === eventoAbierto);
    if (i >= 0) setTimeout(() => document.querySelectorAll('#agenda tbody tr')[i]?.click());
  } catch (err) {
    document.getElementById('emp-datos').innerHTML = '';
    zona.innerHTML = aviso('error', esc(err.message));
  }
}

function inspeccionar(r, soluciones) {
  const zona = document.getElementById('inspeccion');
  const ev = r.eventos.find(e => e.estado === 'Programado' || e.estado === 'En curso');
  const faltanCriticos = r.acciones.some(a => a.critica && !a.hecho);
  zona.innerHTML = `
    <h2>${esc(ev.evento)} · ${esc(r.numero)}</h2>
    ${avance(r)}
    ${datos([
      ['Objeto', esc(r.objeto)], ['Problema', esc(r.tipoProblema + ' - ' + r.problema)],
      ['Lo que cuenta el cliente', `<span style="font-weight:400">${esc(r.detalle)}</span>`],
      ['Pide', esc(r.pedido)],
      ['Protocolo', esc(r.protocolo)], ['Cita', `${fecha(ev.inicio)} – ${hora(ev.fin)}`],
      ['Tickets', esc(r.tickets.length + ' turno(s): ' + r.tickets[0] + (r.tickets.length > 1 ? ' …' : ''))],
      ['Garantía', r.tipoObjeto === 'Producto' ? (r.fueraDeGarantia ? 'Fuera de garantía' : 'Vigente') : '-'],
      ['Límite', fecha(r.limiteAtencion) + (r.vencido ? ' (vencido)' : '')]
    ])}
    <h2 style="margin-top:16px">Pasos del evento</h2>
    <ol class="pasos">${r.acciones.map((a, i) => `<li class="${a.hecho ? 'hecho' : ''}">
      <span class="num">${a.hecho ? '✓' : i + 1}</span>
      <span class="texto">${esc(a.nombre)}${a.critica ? ' <span class="etiqueta alta">crítico</span>' : ''}
        <small>Estimado ${hora(a.estimado)} · ${a.minutos} min · ${esc(a.operario)}${a.hecho ? ' · Real ' + hora(a.real) : ''}</small></span>
      ${a.hecho ? '' : `<button type="button" data-paso="${esc(a.nombre)}">Ejecutar</button>`}
    </li>`).join('')}</ol>
    <form id="f-insp" style="margin-top:12px">
      ${r.preguntaReparable ? `<label class="check"><input type="checkbox" id="reparable"> El bien admite reparación (lo lee el motor de reglas)</label>` : ''}
      ${r.ultimo ? `<div class="campo" style="margin-top:8px"><label for="solucion">Solución aplicada</label>
          <select id="solucion">${soluciones.map(s => `<option ${s === r.solucionSugerida ? 'selected' : ''}>${esc(s)}</option>`).join('')}</select>
          <small style="color:var(--suave)">Sugerida por el motor: ${esc(r.preguntaReparable ? 'se calcula con la inspección' : r.solucionSugerida)}</small></div>`
        : aviso('info', `Al completar, el sistema toma solo los tickets del siguiente evento: ${esc(r.siguienteEvento)}.`)}
      ${faltanCriticos ? aviso('alerta', 'Faltan pasos críticos: no se puede completar el evento.') : ''}
      <div id="insp-error"></div>
      <div class="acciones">
        <button type="button" class="peligro" id="rechazar">Proponer rechazo</button>
        <button class="primario" ${faltanCriticos ? 'disabled' : ''}>Completar evento</button>
      </div>
    </form>`;

  zona.querySelectorAll('[data-paso]').forEach(b => b.addEventListener('click', async () => {
    try {
      const x = await api(`reclamos/${r.numero}/paso`, { empleado: empleadoId, accion: b.dataset.paso });
      cargarAgenda(x.mensaje);
    } catch (err) {
      document.getElementById('insp-error').innerHTML = aviso('error', esc(err.message));
    }
  }));
  document.getElementById('f-insp').addEventListener('submit', async e => {
    e.preventDefault();
    const envio = { empleado: empleadoId };
    if (r.preguntaReparable) envio.reparable = document.getElementById('reparable').checked;
    if (r.ultimo) envio.solucion = document.getElementById('solucion').value;
    try {
      const x = await api(`reclamos/${r.numero}/completar`, envio);
      eventoAbierto = '';
      cargarAgenda(x.mensaje);
    } catch (err) {
      document.getElementById('insp-error').innerHTML = aviso('error', esc(err.message));
    }
  });
  document.getElementById('rechazar').addEventListener('click', async () => {
    const motivo = prompt('Motivo del rechazo (lo revisa el supervisor antes de notificar al cliente):');
    if (motivo === null) return;
    try {
      const x = await api(`reclamos/${r.numero}/rechazar`, { empleado: empleadoId, motivo });
      eventoAbierto = '';
      cargarAgenda(x.mensaje);
    } catch (err) {
      document.getElementById('insp-error').innerHTML = aviso('error', esc(err.message));
    }
  });
}

/* ---------------- area: revision de calidad ---------------- */

let supervisorId = '';

function vistaRevision() {
  vista.innerHTML = `
    <h1>Revisión de calidad</h1>
    <p class="subtitulo">Reclamos observados por la revisión automática, la muestra al azar y los rechazos, antes de llegar al cliente.</p>
    <section class="tarjeta">
      <form id="f-sup" class="fila">
        <div class="campo"><label for="id-sup">ID de supervisor</label>
          <input type="text" id="id-sup" value="${esc(supervisorId || 'S01')}"></div>
        <button class="primario">Ingresar</button>
      </form>
    </section>
    <div id="revision"></div>`;
  document.getElementById('f-sup').addEventListener('submit', e => {
    e.preventDefault();
    supervisorId = document.getElementById('id-sup').value.trim().toUpperCase();
    cargarRevision();
  });
  if (supervisorId) cargarRevision();
}

async function cargarRevision(mensaje) {
  const zona = document.getElementById('revision');
  try {
    const x = await api('revision?supervisor=' + encodeURIComponent(supervisorId));
    zona.innerHTML = `${mensaje ? aviso('exito', esc(mensaje)) : ''}
      <section class="tarjeta">
        <form id="f-muestra" class="fila" style="margin-bottom:16px">
          <div class="campo" style="max-width:260px"><label for="muestra">Muestra al azar: 1 de cada</label>
            <input type="number" id="muestra" min="1" max="100" value="${x.muestraCada}"></div>
          <button>Guardar</button>
        </form>
        ${x.reclamos.length ? x.reclamos.map(r => {
          const rechazo = r.revision === 'Rechazo propuesto';
          return `<article class="tarjeta" style="box-shadow:none">
            <h2>${esc(r.numero)} · ${etiqueta(r.revision)}</h2>
            ${avance(r)}
            ${datos([
              ['Cliente', esc(r.cliente)], ['Objeto', esc(r.objeto)], ['Problema', esc(r.problema)],
              ['El cliente pidió', esc(r.pedido)],
              ['Operario', esc(r.especialista)],
              rechazo ? ['Motivo del rechazo', esc(r.motivoRechazo)] : ['Solución aplicada', esc(r.solucionAplicada)],
              !rechazo && ['Sugerida por el motor', esc(r.solucionSugerida)],
              r.observaciones.length && !rechazo && ['Observaciones', esc(r.observaciones.join('; '))]
            ])}
            <div class="acciones">
              <button type="button" class="peligro" data-devolver="${esc(r.numero)}">Devolver</button>
              <button type="button" class="primario" data-aprobar="${esc(r.numero)}">${rechazo ? 'Aprobar rechazo' : 'Aprobar'}</button>
            </div></article>`;
        }).join('') : '<p class="vacio">No hay reclamos por revisar.</p>'}
      </section>`;

    document.getElementById('f-muestra').addEventListener('submit', e => {
      e.preventDefault();
      accionSupervisor(null, 'muestra', { valor: document.getElementById('muestra').value });
    });
    zona.querySelectorAll('[data-aprobar]').forEach(b => b.addEventListener('click', () =>
      accionSupervisor(b.dataset.aprobar, 'aprobar', {})));
    zona.querySelectorAll('[data-devolver]').forEach(b => b.addEventListener('click', () => {
      const motivo = prompt('Motivo de la devolución:');
      if (motivo !== null) accionSupervisor(b.dataset.devolver, 'devolver', { motivo });
    }));
  } catch (err) {
    zona.innerHTML = aviso('error', esc(err.message));
  }
}

async function accionSupervisor(numero, accion, envio) {
  try {
    const ruta = numero ? `reclamos/${numero}/${accion}` : 'calidad/' + accion;
    const x = await api(ruta, { supervisor: supervisorId, ...envio });
    cargarRevision(x.mensaje);
  } catch (err) {
    dialogo('No se pudo completar', `<p>${esc(err.message)}</p>`);
  }
}

/* ---------------- gerencia: consulta de reclamos ---------------- */

async function vistaConsulta() {
  vista.innerHTML = `<h1>Consulta de reclamos</h1>
    <p class="subtitulo">Solo consulta: el estado lo mueve el flujo del sistema, nunca un operador a mano.</p>
    <div id="consulta"></div>`;
  const reclamos = await api('reclamos');
  document.getElementById('consulta').innerHTML = `
    <section class="tarjeta">${tabla(
      ['Reclamo', 'Cliente', 'Objeto', 'Problema', 'Prioridad', 'Estado', 'Fecha programada', 'Avance', 'Operario'],
      reclamos.map(r => [esc(r.numero), esc(r.cliente), esc(r.objeto), esc(r.problema), etiqueta(r.prioridad),
        etiqueta(r.estado), fecha(r.fechaProgramada) + (r.sinCapacidad ? ' ⚠' : ''),
        `${r.pasosCompletados}/${r.totalPasos}`, esc(r.especialista)]),
      i => fichaReclamo(reclamos[i]))}</section>
    <div id="ficha"></div>`;
}

function fichaReclamo(r) {
  document.getElementById('ficha').innerHTML = `
    <div class="dos-columnas">
      <section class="tarjeta">
        <h2>Maestra del reclamo ${esc(r.numero)}</h2>
        ${r.sinCapacidad ? aviso('alerta', 'Sin capacidad dentro del plazo: la cita quedó después del límite de atención.') : ''}
        ${avance(r)}
        ${datos([
          ['Apertura', fecha(r.apertura)], ['Canal', esc(r.canal + (r.local !== '-' ? ' - ' + r.local : ''))],
          ['Cliente', esc(`${r.cliente} (${r.tipoCliente})`)], ['Reclama sobre', esc(r.tipoObjeto + ': ' + r.objeto)],
          ['Naturaleza', esc(r.naturaleza)],
          ['Problema', esc(r.tipoProblema + ' - ' + r.problema)],
          ['Detalle', `<span style="font-weight:400">${esc(r.detalle)}</span>`], ['Pedido', esc(r.pedido)],
          ['Protocolo', esc(r.protocolo)],
          ['Instancia', esc(r.instancia)], ['Prioridad', etiqueta(r.prioridad)], ['Estado', etiqueta(r.estado)],
          ['Área / operario', esc(r.area + ' / ' + r.especialista)],
          ['Fecha programada', fecha(r.fechaProgramada)], ['Límite de atención', fecha(r.limiteAtencion)],
          ['Sustento', esc(r.comprobante ? r.comprobante + (r.fueraDeGarantia ? ' (fuera de garantía)' : '') : 'Sin comprobante')],
          ['Solución sugerida', esc(r.solucionSugerida || '-')], ['Solución aplicada', esc(r.solucionAplicada || '-')],
          ['Revisión', esc(r.revision || '-')],
          ['Conformidad', r.conforme === null ? '-' : esc((r.conforme ? 'Conforme' : 'No conforme') + ', nota ' + r.nota)],
          ['Puede pasar a', esc(r.siguientes.join(', ') || 'ningún estado')]
        ])}
        ${r.trazaMotor.length ? `<h2 style="margin-top:16px">Traza del motor de reglas</h2>
          <ul>${r.trazaMotor.map(p => `<li>${esc(p)}</li>`).join('')}</ul>` : ''}
      </section>
      <section class="tarjeta"><h2>Eventos del protocolo</h2>${lineaEventos(r)}</section>
    </div>
    <section class="tarjeta"><h2>Movimientos de solución (pasos ejecutados)</h2>${tablaMovimientos(r)}</section>
    <section class="tarjeta"><h2>Movimientos del reclamo</h2>
      ${tabla(['Momento', 'Usuario', 'Acción', 'Detalle'], r.bitacora.map(b =>
        [fecha(b.momento), esc(b.usuario), esc(b.accion), `<span style="white-space:normal">${esc(b.detalle)}</span>`]))}
    </section>`;
  document.getElementById('ficha').scrollIntoView({ behavior: 'smooth' });
}

/* ---------------- gerencia: parque de tickets ---------------- */

const parqueSel = { operario: 'E01', dia: '' };

async function vistaParque() {
  if (!parqueSel.dia) parqueSel.dia = hoyIso();
  const operarios = await api('operarios');
  vista.innerHTML = `<h1>Parque de tickets</h1>
    <p class="subtitulo">Los turnos de atención de cada operario, generados por el batch a partir del horario laboral
      (lunes a sábado, 9:00 a 18:00, sin feriados). Cada evento de un reclamo toma los tickets seguidos que necesita.</p>
    <section class="tarjeta">
      <form id="f-parque" class="fila">
        <div class="campo"><label for="p-op">Operario</label>
          <select id="p-op">${operarios.map(o => `<option value="${esc(o.id)}" ${o.id === parqueSel.operario ? 'selected' : ''}>
            ${esc(o.id)} · ${esc(o.area)}</option>`).join('')}</select></div>
        <div class="campo"><label for="p-dia">Día</label><input type="date" id="p-dia" value="${parqueSel.dia}"></div>
        <button class="primario">Ver</button>
        <button type="button" id="batch">Ejecutar batch (30 días)</button>
      </form>
      <div id="batch-resultado"></div>
    </section>
    <div id="parque"></div>`;
  document.getElementById('f-parque').addEventListener('submit', e => {
    e.preventDefault();
    parqueSel.operario = document.getElementById('p-op').value;
    parqueSel.dia = document.getElementById('p-dia').value || hoyIso();
    cargarParque();
  });
  document.getElementById('batch').addEventListener('click', async () => {
    const c = await api('parque/generar', { dias: 30 });
    document.getElementById('batch-resultado').innerHTML = aviso('exito',
      `${esc(c.mensaje)} Cuadre: ${c.operarios} operarios, ${c.dias} días, ${c.ticketsGenerados} tickets nuevos
       (los que ya existían no se duplican).`);
    cargarParque();
  });
  cargarParque();
}

async function cargarParque() {
  const zona = document.getElementById('parque');
  try {
    const p = await api(`parque?operario=${encodeURIComponent(parqueSel.operario)}&dia=${parqueSel.dia}`);
    const ahora = new Date();
    const tomados = p.tickets.filter(t => !t.disponible).length;
    zona.innerHTML = `<section class="tarjeta">
      <h2>${esc(p.operario.id)} · ${esc(p.operario.area)}</h2>
      ${!p.laborable ? aviso('info', 'Día no laborable (domingo o feriado): no hay tickets.') : `
      <div class="leyenda"><span>Disponible (${p.disponibles})</span><span class="l-tomado">Tomado (${tomados})</span></div>
      <div class="parque">${p.tickets.map(t => `<div class="turno ${t.disponible ? '' : 'tomado'} ${new Date(t.fin) < ahora ? 'pasado' : ''}"
          title="${esc(t.codigo)}${t.reclamo ? ' · ' + esc(t.reclamo) + ' · ' + esc(t.evento) : ''}">
          <b>${hora(t.inicio)}</b>${t.disponible ? 'Libre' : esc(t.reclamo) + '<br>' + esc(t.evento)}</div>`).join('')}</div>`}
    </section>`;
  } catch (err) {
    zona.innerHTML = aviso('error', esc(err.message));
  }
}

/* ---------------- gerencia: indicadores ---------------- */

function barras(mapa) {
  const max = Math.max(1, ...Object.values(mapa));
  return `<div class="barras">${Object.entries(mapa).map(([k, v]) => `<div class="barra">
    <span>${esc(k)}</span><div><i style="width:${(100 * v / max).toFixed(0)}%"></i></div><b>${v}</b></div>`).join('')}</div>`;
}

async function vistaIndicadores() {
  vista.innerHTML = '<h1>Indicadores</h1><p class="subtitulo">Calculados con los reclamos y movimientos de la sesión.</p><div id="ind"></div>';
  const d = await api('indicadores');
  document.getElementById('ind').innerHTML = `
    <div class="kpis">
      <div class="kpi"><span>Reclamos</span><strong>${d.total}</strong></div>
      <div class="kpi"><span>Satisfacción del cliente</span><strong>${d.satisfaccion === null ? '—' : d.satisfaccion.toFixed(1) + ' / 5'}</strong></div>
      <div class="kpi"><span>% devueltos por calidad</span><strong>${d.devueltos === null ? '—' : d.devueltos.toFixed(0) + ' %'}</strong></div>
      <div class="kpi"><span>Pasos ejecutados</span><strong>${d.pasosEjecutados}</strong></div>
      <div class="kpi"><span>Desvío promedio por paso</span><strong>${d.desvioPromedio === null ? '—' : Math.round(d.desvioPromedio) + ' min'}</strong></div>
      <div class="kpi"><span>Sin capacidad en plazo</span><strong>${d.sinCapacidad}</strong></div>
    </div>
    <div class="dos-columnas">
      <section class="tarjeta"><h2>Reclamos por estado</h2>${barras(d.porEstado)}</section>
      <section class="tarjeta"><h2>Carga por operario (eventos pendientes)</h2>${barras(d.carga)}</section>
    </div>
    <section class="tarjeta"><h2>Reclamos por área</h2>${barras(d.porArea)}</section>`;
}

/* ---------------- area: reporte de atencion ---------------- */

let reporteId = 'E01';

async function vistaReporte() {
  const operarios = await api('operarios');
  vista.innerHTML = `<h1>Reporte de atención</h1>
    <p class="subtitulo">Avance de los reclamos que el sistema le asignó a cada usuario del área.</p>
    <section class="tarjeta">
      <form id="f-rep" class="fila">
        <div class="campo"><label for="rep-emp">ID de empleado</label>
          <select id="rep-emp">${operarios.map(o => `<option value="${esc(o.id)}" ${o.id === reporteId ? 'selected' : ''}>
            ${esc(o.id)} · ${esc(o.tipo)} · ${esc(o.area)}</option>`).join('')}</select></div>
        <button class="primario">Generar</button>
      </form>
    </section>
    <div id="reporte"></div>`;
  document.getElementById('f-rep').addEventListener('submit', e => {
    e.preventDefault();
    reporteId = document.getElementById('rep-emp').value;
    cargarReporte();
  });
  cargarReporte();
}

async function cargarReporte() {
  const zona = document.getElementById('reporte');
  try {
    const r = await api('empleados/' + encodeURIComponent(reporteId) + '/reporte');
    zona.innerHTML = `<div class="kpis">
        <div class="kpi"><span>Reclamos asignados</span><strong>${r.total}</strong></div>
        <div class="kpi"><span>Abiertos</span><strong>${r.abiertos}</strong></div>
        <div class="kpi"><span>Cerrados</span><strong>${r.cerrados}</strong></div>
        <div class="kpi"><span>Fuera de plazo</span><strong>${r.vencidos}</strong></div>
      </div>
      <section class="tarjeta"><h2>${esc(r.empleado.id)} · ${esc(r.empleado.area)} · ${esc(fecha(new Date().toISOString()).slice(0, 10))}</h2>${tabla(
        ['Reclamo', 'Apertura', 'Cliente', 'Problema', 'Prioridad', 'Estado', 'Fecha programada', 'Avance', 'Vencido'],
        r.reclamos.map(x => [esc(x.numero), fecha(x.apertura), esc(x.cliente), esc(x.problema), etiqueta(x.prioridad),
          etiqueta(x.estado), fecha(x.fechaProgramada), `${x.pasosCompletados}/${x.totalPasos}`, x.vencido ? 'Sí' : 'No']))}
      </section>`;
  } catch (err) {
    zona.innerHTML = aviso('error', esc(err.message));
  }
}

/* ---------------- seguridad: perfiles y accesos ---------------- */

let perfilElegido = '';

async function vistaSeguridad(mensaje) {
  const s = await api('seguridad');
  if (!s.perfiles.some(p => p.nombre === perfilElegido)) perfilElegido = s.perfiles[0]?.nombre || '';
  const perfil = s.perfiles.find(p => p.nombre === perfilElegido);
  vista.innerHTML = `<h1>Perfiles y accesos</h1>
    <p class="subtitulo">Qué pantallas puede abrir cada perfil de usuario del sistema.</p>
    ${mensaje ? aviso('exito', esc(mensaje)) : ''}
    <div class="seguridad">
      <section class="tarjeta"><h2>Perfiles</h2>
        <ul class="perfiles">${s.perfiles.map(p => `<li><button type="button" data-perfil="${esc(p.nombre)}"
          aria-pressed="${p.nombre === perfilElegido}">${esc(p.nombre)}<small>${p.accesos.length}</small></button></li>`).join('')}</ul>
        <form id="f-perfil" class="ingresar">
          <div class="campo"><label for="nuevo-perfil">Nuevo perfil</label><input type="text" id="nuevo-perfil"></div>
          <button>Agregar</button>
        </form>
        <div id="perfil-error"></div>
      </section>
      <form id="f-accesos" class="tarjeta">
        <h2>Accesos del perfil ${esc(perfilElegido)}</h2>
        <p class="subtitulo" style="margin-bottom:12px">${perfil.accesos.length} de ${s.accesos.length} accesos concedidos.</p>
        <div class="accesos">${s.accesos.map(a => `<label class="check"><input type="checkbox" name="acceso" value="${esc(a)}"
          ${perfil.accesos.includes(a) ? 'checked' : ''}> ${esc(a)}</label>`).join('')}</div>
        <h2 style="margin-top:16px">Permisos</h2>
        ${s.permisos.map(p => `<label class="check"><input type="checkbox"> ${esc(p)}</label>`).join('')}
        <div class="acciones">
          <button type="button" id="todos">Marcar todos</button>
          <button type="button" id="ninguno">Desmarcar todos</button>
          <button class="primario">Guardar</button>
        </div>
      </form>
    </div>`;

  vista.querySelectorAll('[data-perfil]').forEach(b => b.addEventListener('click', () => {
    perfilElegido = b.dataset.perfil;
    vistaSeguridad();
  }));
  const casillas = () => vista.querySelectorAll('[name=acceso]');
  document.getElementById('todos').addEventListener('click', () => casillas().forEach(c => { c.checked = true; }));
  document.getElementById('ninguno').addEventListener('click', () => casillas().forEach(c => { c.checked = false; }));
  document.getElementById('f-perfil').addEventListener('submit', async e => {
    e.preventDefault();
    const nombre = document.getElementById('nuevo-perfil').value.trim();
    try {
      await api('seguridad/perfiles', { nombre });
      perfilElegido = nombre;
      vistaSeguridad('Perfil "' + nombre + '" creado. Asígnele sus accesos.');
    } catch (err) {
      document.getElementById('perfil-error').innerHTML = aviso('error', esc(err.message));
    }
  });
  document.getElementById('f-accesos').addEventListener('submit', async e => {
    e.preventDefault();
    const envio = new URLSearchParams({ perfil: perfilElegido });
    casillas().forEach(c => { if (c.checked) envio.append('acceso', c.value); });
    const x = await api('seguridad/accesos', envio);
    vistaSeguridad(x.mensaje);
  });
}

/* ---------------- seguridad: diario de operaciones ---------------- */

async function vistaDiario(mensaje) {
  const d = await api('auditoria');
  vista.innerHTML = `<h1>Diario de operaciones</h1>
    <p class="subtitulo">Todo cambio a la configuración queda registrado, también los intentos rechazados.
      La última operación vigente se puede revertir: el sistema vuelve a como estaba antes de ella.</p>
    ${mensaje ? aviso(mensaje.startsWith('!') ? 'error' : 'exito', esc(mensaje.replace(/^!/, ''))) : ''}
    <section class="tarjeta">${d.operaciones.length ? `<div class="tabla-envoltura"><table class="diario">
      <thead><tr>${['N°', 'Momento', 'Usuario', 'Módulo', 'Operación', 'Detalle', 'Resultado', ''].map(c => `<th>${c}</th>`).join('')}</tr></thead>
      <tbody>${d.operaciones.map(o => `<tr>
        <td>${o.numero}</td><td>${fecha(o.momento)}</td><td>${esc(o.usuario)}</td><td>${esc(o.modulo)}</td>
        <td>${esc(o.operacion)}</td><td>${esc(o.detalle)}</td>
        <td>${etiqueta(o.resultado)}</td>
        <td>${o.numero === d.revertible ? `<button type="button" class="peligro" data-revertir="${o.numero}">Revertir</button>` : ''}</td>
      </tr>`).join('')}</tbody></table></div>` : '<p class="vacio">Todavía no hay operaciones.</p>'}
      <p class="nota">Una reversa devuelve la configuración; los reclamos que ya se atendieron con ella no cambian.</p>
    </section>`;
  vista.querySelector('[data-revertir]')?.addEventListener('click', async e => {
    const n = e.target.dataset.revertir;
    if (!confirm(`¿Revertir la operación ${n}? La configuración volverá a como estaba antes de ella.`)) return;
    try {
      const r = await ocupado(vista, () => api(`auditoria/${n}/revertir`, {}));
      vistaDiario(r.mensaje);
    } catch (err) { vistaDiario('!' + err.message); }
  });
}

/* ---------------- gerencial: mantenimiento de parametros ---------------- */

/*
 * Todas las pantallas de mantenimiento llegan del servidor como secciones con
 * bloques (tabla, parametros, lista, simulador). Aqui solo se dibujan. Todo lo
 * que se edita se guarda en el servidor, porque lo lee el flujo automatico.
 */

async function vistaMantenimiento(clave) {
  const p = await api('mantenimiento/' + clave);
  let activa = 0;
  // el estado de cada bloque vive mientras no se salga de la pantalla
  const estados = p.secciones.map(s => s.bloques.map(nuevoEstado));

  const pintar = () => {
    const secciones = p.pestanias ? [activa] : p.secciones.map((_, i) => i);
    vista.innerHTML = `<h1>${esc(p.titulo)}</h1><p class="subtitulo">${esc(p.subtitulo)}</p>
      ${p.pestanias ? `<div class="pestanias" role="tablist">${p.secciones.map((s, i) =>
        `<button type="button" role="tab" data-pestania="${i}" aria-selected="${i === activa}">${esc(s.titulo)}</button>`).join('')}</div>` : ''}
      <div id="secciones"></div>`;
    const zona = document.getElementById('secciones');
    secciones.forEach(i => {
      const s = p.secciones[i];
      if (!p.pestanias && s.titulo) zona.insertAdjacentHTML('beforeend', `<h2 class="titulo-seccion">${esc(s.titulo)}</h2>`);
      s.bloques.forEach((b, j) => {
        const caja = document.createElement('section');
        caja.className = 'tarjeta';
        zona.appendChild(caja);
        // un bloque desconocido (servidor de otra version) no tumba toda la pantalla
        if (PINTORES[b.tipo]) PINTORES[b.tipo](caja, b, estados[i][j]);
        else caja.innerHTML = aviso('alerta', `No se reconoce el bloque "${esc(b.tipo)}": reinicie el servidor para usar la versión actual.`);
      });
    });
    vista.querySelectorAll('[data-pestania]').forEach(b => b.addEventListener('click', () => {
      activa = Number(b.dataset.pestania);
      pintar();
    }));
  };
  pintar();
}

function nuevoEstado(b) {
  if (b.tipo === 'tabla' || b.tipo === 'parametros') return { filas: b.filas.map(f => [...f]), filtro: {}, sel: -1, mensaje: '' };
  return {};
}

const PINTORES = { tabla: pintarTabla, parametros: pintarParametros, lista: pintarLista, simulador: pintarSimulador };

const mensajeDe = st => st.mensaje ? aviso(st.mensaje.startsWith('!') ? 'error' : 'exito', esc(st.mensaje.replace(/^!/, ''))) : '';

/*
 * Una tabla de catalogo. Si trae "catalogo", cada cambio (agregar, habilitar,
 * quitar) va al servidor, que lo valida y devuelve las filas como quedaron:
 * es la misma lista que lee el flujo automatico.
 */
function pintarTabla(caja, b, st) {
  const visibles = st.filas.map((f, i) => [f, i])
    .filter(([f]) => Object.entries(st.filtro).every(([c, v]) => !v || f[c] === v));
  const celda = (v, c) => c === b.estado ? etiqueta(v) : esc(v);
  caja.innerHTML = `<h2>${esc(b.titulo)}</h2>
    ${b.filtros ? `<div class="filtros">${b.filtros.map(c => {
      const valores = [...new Set(st.filas.map(f => f[c]))];
      return `<div class="campo"><label>${esc(b.columnas[c])}</label><select data-filtro="${c}">
        <option value="">Todos</option>${valores.map(v => `<option ${st.filtro[c] === v ? 'selected' : ''}>${esc(v)}</option>`).join('')}
      </select></div>`;
    }).join('')}</div>` : ''}
    ${tabla(b.columnas, visibles.map(([f]) => f.map(celda)), k => { st.sel = visibles[k][1]; })}
    ${b.estado !== undefined || b.quitar ? `<div class="barra-tabla">
      ${b.estado !== undefined ? `<button type="button" data-estado="Habilitado">Habilitar</button>
      <button type="button" data-estado="Deshabilitado">Deshabilitar</button>` : ''}
      ${b.quitar ? '<button type="button" class="peligro" data-quitar>Quitar</button>' : ''}</div>` : ''}
    ${mensajeDe(st)}
    ${b.alta ? `<form class="form-alta"><h3>Agregar o reemplazar</h3><div class="rejilla">${b.alta.map(c =>
      `<div class="campo"><label>${esc(c.etiqueta)}</label>${c.opciones
        ? `<select name="valor">${opcionesHtml(c.opciones)}</select>`
        : '<input type="text" name="valor">'}</div>`).join('')}</div>
      <div class="acciones"><button class="primario">Guardar</button></div></form>` : ''}
    ${b.nota ? `<p class="nota">${esc(b.nota)}</p>` : ''}`;

  // la fila elegida sigue marcada al repintar
  const k = visibles.findIndex(([, i]) => i === st.sel);
  if (k >= 0) caja.querySelectorAll('tbody tr')[k]?.classList.add('seleccionada');

  const repintar = mensaje => { st.mensaje = mensaje || ''; pintarTabla(caja, b, st); };
  const enviar = async (accion, datos, exito) => {
    try {
      const r = await ocupado(caja, () => api(`catalogos/${b.catalogo}/${accion}`, datos));
      st.filas = r.filas;
      repintar(r.mensaje || exito);
    } catch (err) {
      // si otro usuario cambio el catalogo, el servidor manda las filas al dia
      if (err.datos?.filas) { st.filas = err.datos.filas; st.sel = -1; }
      repintar('!' + err.message);
    }
  };
  const comoLaVeia = i => st.filas[i].join('|');
  caja.querySelectorAll('[data-filtro]').forEach(s => s.addEventListener('change', () => {
    st.filtro[s.dataset.filtro] = s.value;
    repintar();
  }));
  caja.querySelectorAll('[data-estado]').forEach(btn => btn.addEventListener('click', () => {
    if (st.sel < 0) return repintar('!Seleccione una fila de la tabla.');
    const nuevo = btn.dataset.estado;
    enviar('estado', { fila: st.sel, actual: comoLaVeia(st.sel), estado: nuevo }, `${st.filas[st.sel][0]}: ${nuevo.toLowerCase()}.`);
  }));
  caja.querySelector('[data-quitar]')?.addEventListener('click', () => {
    if (st.sel < 0) return repintar('!Seleccione una fila de la tabla.');
    const nombre = st.filas[st.sel].slice(0, 2).join(' · ');
    const fila = st.sel;
    const actual = comoLaVeia(fila);
    st.sel = -1;
    enviar('quitar', { fila, actual }, `${nombre}: quitado.`);
  });
  caja.querySelector('.form-alta')?.addEventListener('submit', e => {
    e.preventDefault();
    const valores = [...e.target.querySelectorAll('[name=valor]')].map(x => x.value.trim());
    if (valores.some(v => !v)) return repintar('!Complete todos los campos.');
    const envio = new URLSearchParams();
    valores.forEach(v => envio.append('valor', v));
    enviar('agregar', envio, `${valores[0]} guardado.`);
  });
}

/* Generalidades del sistema: se elige un parametro y se le da un valor nuevo. */
function pintarParametros(caja, b, st) {
  const p = st.sel >= 0 ? st.filas[st.sel] : null;
  caja.innerHTML = `<h2>${esc(b.titulo)}</h2>
    ${tabla(b.columnas, st.filas.map(f => f.map(esc)), k => { st.sel = k; pintarParametros(caja, b, st); })}
    ${mensajeDe(st)}
    ${p ? `<form class="form-alta fila"><div class="campo"><label for="valor-param">${esc(p[0])} (${esc(p[2])})</label>
      <input type="text" id="valor-param" value="${esc(p[1])}"></div><button class="primario">Guardar</button></form>`
      : '<p class="nota">Elija un parámetro para cambiar su valor.</p>'}`;
  if (st.sel >= 0) caja.querySelectorAll('tbody tr')[st.sel]?.classList.add('seleccionada');
  caja.querySelector('form')?.addEventListener('submit', async e => {
    e.preventDefault();
    try {
      const r = await ocupado(caja, () => api('parametros', { parametro: p[0], valor: document.getElementById('valor-param').value }));
      st.filas = r.filas;
      st.mensaje = r.mensaje;
    } catch (err) { st.mensaje = '!' + err.message; }
    pintarParametros(caja, b, st);
  });
}

function pintarLista(caja, b) {
  caja.innerHTML = `<h2>${esc(b.titulo)}</h2><ul class="lista-simple">${b.valores.map(v => `<li>${esc(v)}</li>`).join('')}</ul>
    ${b.nota ? `<p class="nota">${esc(b.nota)}</p>` : ''}`;
}

function pintarSimulador(caja, b) {
  caja.innerHTML = `<h2>${esc(b.titulo)}</h2>
    <p class="subtitulo">Corre la cadena de condiciones con un caso, igual que el sistema al registrar un reclamo.</p>
    <form><div class="rejilla">
      <div class="campo"><label for="sim-inicio">Condición inicial</label>
        <select id="sim-inicio" name="inicio">${b.inicios.map(c => `<option>${esc(c)}</option>`).join('')}</select></div>
      ${b.variables.map(v => `<div class="campo">
      <label>${esc(v.nombre)}</label><input type="text" name="${esc(v.nombre)}" value="${esc(v.valor)}">
      <small style="color:var(--suave)">${esc(v.origen)}</small></div>`).join('')}</div>
      <div class="acciones"><button class="primario">Evaluar cadena</button></div></form>
    <div id="sim-resultado"></div>`;
  caja.querySelector('form').addEventListener('submit', async e => {
    e.preventDefault();
    const r = await api('reglas/evaluar', new URLSearchParams(new FormData(e.target)));
    caja.querySelector('#sim-resultado').innerHTML = `
      ${aviso(r.valido ? 'exito' : 'error', r.valido ? 'Solución sugerida: <strong>' + esc(r.accion) + '</strong>'
        : 'La cadena no pudo resolverse: ' + esc(r.error))}
      ${tabla(['Condición', 'Comparación', 'Resultado', 'Salida'], r.traza.map(t =>
        [esc(t.codigo), esc(t.comparacion), t.resultado ? 'Verdadero' : 'Falso', esc(t.salida)]))}`;
  });
}

/* ---------------- navegacion ---------------- */

const MANTENIMIENTO = ['general', 'reclamos-eventos', 'productos', 'servicios', 'problemas', 'clientes',
  'protocolos', 'reglas', 'politicas', 'atencion'];

const VISTAS = {
  seguridad: () => vistaSeguridad(), diario: () => vistaDiario(),
  registrar: vistaRegistrar, estado: vistaEstado, atender: vistaAtender, reporte: vistaReporte,
  revision: vistaRevision, consulta: vistaConsulta, parque: vistaParque, indicadores: vistaIndicadores,
  ...Object.fromEntries(MANTENIMIENTO.map(c => [c, () => vistaMantenimiento(c)]))
};

async function navegar() {
  const clave = location.hash.slice(1) || 'registrar';
  document.querySelectorAll('.menu a').forEach(a =>
    a.classList.toggle('activo', a.getAttribute('href') === '#' + clave));
  try {
    await (VISTAS[clave] || vistaRegistrar)();
  } catch (err) {
    vista.innerHTML = aviso('error', 'No se pudo cargar la pantalla: ' + esc(err.message));
  }
  vista.focus({ preventScroll: true });
}

window.addEventListener('hashchange', navegar);
navegar();
