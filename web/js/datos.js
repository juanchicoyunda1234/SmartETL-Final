import { pedir } from "/web/js/api.js";
import { crear, icono, toast, formatoNumero, celdaValor, pintarPaginacion } from "/web/js/interfaz.js";

const OTRA_RUTA = "__otra__";
const $ = (id) => document.getElementById(id);

let fuentes = [];
let archivos = [];
let fuenteElegida = 0;
let estructuraElegida = "enlazada";

function formatoMs(ms) {
  return formatoNumero(ms, ms < 1 ? 3 : 1) + " ms";
}

function pintarFuentes() {
  const nombresIcono = ["plane", "route", "server"];
  const grupo = $("datosFuente");
  grupo.replaceChildren();
  fuentes.forEach((fuente, indice) => {
    const boton = crear("button");
    boton.type = "button";
    boton.setAttribute("aria-pressed", String(indice === fuenteElegida));
    boton.append(icono(nombresIcono[indice] || "database"), crear("span", "", fuente.nombre));
    boton.addEventListener("click", () => elegirFuente(indice));
    grupo.append(boton);
  });
}

function elegirFuente(indice) {
  fuenteElegida = indice;
  pintarFuentes();
  const select = $("datosArchivo");
  const porDefecto = fuentes[indice].archivo;
  select.value = archivos.some((archivo) => archivo.ruta === porDefecto) ? porDefecto : OTRA_RUTA;
  $("datosRuta").value = porDefecto;
  mostrarRutaLibre();
}

function pintarArchivos() {
  const select = $("datosArchivo");
  select.replaceChildren();
  ["data", "data/demo"].forEach((carpeta) => {
    const grupo = crear("optgroup");
    grupo.label = carpeta === "data" ? "Datos reales (data/)" : "Demostración (data/demo/)";
    archivos.filter((archivo) => archivo.carpeta === carpeta).forEach((archivo) => {
      const opcion = crear("option", "", archivo.ruta.split("/").pop() + " · " + formatoNumero(archivo.bytes / 1024) + " KB");
      opcion.value = archivo.ruta;
      grupo.append(opcion);
    });
    if (grupo.children.length > 0) {
      select.append(grupo);
    }
  });
  const otra = crear("option", "", "Otra ruta…");
  otra.value = OTRA_RUTA;
  select.append(otra);
  select.addEventListener("change", mostrarRutaLibre);
}

function mostrarRutaLibre() {
  $("datosRuta").hidden = $("datosArchivo").value !== OTRA_RUTA;
}

function archivoElegido() {
  const valor = $("datosArchivo").value;
  return valor === OTRA_RUTA ? $("datosRuta").value.trim() : valor;
}

function prepararEstructura() {
  $("datosEstructura").querySelectorAll("button").forEach((boton) => {
    boton.addEventListener("click", () => {
      estructuraElegida = boton.dataset.valor;
      $("datosEstructura").querySelectorAll("button").forEach((otro) => otro.setAttribute("aria-pressed", String(otro === boton)));
    });
  });
}

function mostrarCamposDelModo() {
  const modo = $("datosModo").value;
  document.querySelectorAll("#vista-datos [data-modos]").forEach((campo) => {
    campo.hidden = !campo.dataset.modos.split(" ").includes(modo);
  });
}

function limpiarErrores() {
  document.querySelectorAll("#vista-datos .campo.invalido").forEach((campo) => {
    campo.classList.remove("invalido", "sacudir");
    const mensaje = campo.querySelector(".campo-error");
    if (mensaje) {
      mensaje.textContent = "";
    }
  });
}

function marcarError(nombreCampo, mensaje) {
  const campo = document.querySelector('#vista-datos [data-campo="' + nombreCampo + '"]');
  if (!campo) {
    toast(mensaje, "error");
    return;
  }
  campo.classList.add("invalido");
  campo.classList.remove("sacudir");
  void campo.offsetWidth;
  campo.classList.add("sacudir");
  const texto = campo.querySelector(".campo-error");
  if (texto) {
    texto.textContent = mensaje;
  }
  const control = campo.querySelector("input:not([hidden]), select");
  if (control) {
    control.focus();
  }
}

function chip(clase, nombreIcono, texto) {
  const elemento = crear("span", "chip " + clase);
  elemento.append(icono(nombreIcono), crear("span", "", texto));
  return elemento;
}

function pintarResumen(resultado) {
  const resumen = $("datosResumen");
  resumen.hidden = false;
  resumen.replaceChildren(
    chip("chip-acento", "timer", "Extract " + formatoMs(resultado.ms)),
    chip("chip-exito", "circle-check", formatoNumero(resultado.cargados) + " cargadas"),
    chip(resultado.noLeidas > 0 ? "chip-error" : "chip-neutro", "triangle-alert", formatoNumero(resultado.noLeidas) + " no leídas"),
    chip("chip-neutro", "layers", resultado.estructura),
    chip("chip-neutro", "file-text", resultado.archivo + " · " + resultado.modo + " · " + formatoNumero(resultado.totalLineas) + " líneas")
  );
}

function pintarColumnas(vista) {
  const select = $("datosColumna");
  const anterior = select.dataset.fuente === vista.fuente ? select.value : null;
  select.replaceChildren();
  vista.encabezados.forEach((encabezado, indice) => {
    const opcion = crear("option", "", encabezado);
    opcion.value = String(indice);
    select.append(opcion);
  });
  select.value = anterior !== null ? anterior : String(vista.columnaBusqueda);
  select.dataset.fuente = vista.fuente;
}

function pintarTabla(vista, filaResaltada = -1) {
  $("datosVacio").hidden = true;
  $("datosTabla").hidden = false;
  $("datosConsulta").hidden = false;
  const tabla = $("datosTablaHtml");
  const cabecera = crear("tr");
  cabecera.append(crear("th", "", "#"));
  vista.encabezados.forEach((encabezado) => cabecera.append(crear("th", "", encabezado)));
  tabla.tHead.replaceChildren(cabecera);
  const cuerpo = tabla.tBodies[0];
  cuerpo.classList.add("cambiando");
  setTimeout(() => {
    cuerpo.replaceChildren();
    vista.filas.forEach((valores, indice) => {
      const fila = crear("tr");
      fila.append(crear("td", "numero-fila", formatoNumero(vista.desde + indice)));
      valores.forEach((valor) => fila.append(celdaValor(valor)));
      if (indice === filaResaltada) {
        fila.classList.add("encontrada");
      }
      cuerpo.append(fila);
    });
    cuerpo.classList.remove("cambiando");
    if (filaResaltada >= 0) {
      cuerpo.rows[filaResaltada].scrollIntoView({ block: "center", behavior: "smooth" });
    }
  }, 120);
  pintarFiltro(vista);
  pintarPaginacion($("datosPaginacion"), vista, irAPagina);
}

function pintarFiltro(vista) {
  const etiqueta = $("datosFiltro");
  etiqueta.hidden = !vista.filtro;
  etiqueta.replaceChildren();
  if (vista.filtro) {
    const quitar = crear("button", "chip-quitar");
    quitar.type = "button";
    quitar.setAttribute("aria-label", "Quitar el filtro");
    quitar.append(icono("x"));
    quitar.addEventListener("click", verTodos);
    etiqueta.append(icono("filter"), crear("span", "", "Filtro: " + vista.filtro), quitar);
  }
}

async function irAPagina(numero) {
  try {
    pintarTabla(await pedir("/api/datos/pagina?n=" + numero));
  } catch (error) {
    toast(error.message, "error");
  }
}

async function cargar() {
  limpiarErrores();
  const cuerpo = {
    fuente: fuenteElegida,
    archivo: archivoElegido(),
    estructura: estructuraElegida,
    modo: $("datosModo").value,
    cantidad: $("datosCantidad").value,
    desde: $("datosDesde").value,
    hasta: $("datosHasta").value,
    semilla: $("datosSemilla").value,
  };
  const boton = $("botonCargarDatos");
  boton.disabled = true;
  try {
    const resultado = await pedir("/api/datos/cargar", { metodo: "POST", cuerpo });
    pintarResumen(resultado);
    pintarColumnas(resultado.vista);
    pintarTabla(resultado.vista);
    $("datosResultado").textContent = "";
  } catch (error) {
    if (error.campo) {
      marcarError(error.campo, error.message);
    } else {
      toast(error.message, "error");
    }
  } finally {
    boton.disabled = false;
  }
}

function consulta() {
  return { columna: Number($("datosColumna").value), valor: $("datosValor").value };
}

function mostrarResultadoConsulta(partes) {
  const caja = $("datosResultado");
  caja.replaceChildren();
  partes.forEach((parte) => caja.append(typeof parte === "string" ? document.createTextNode(parte) : parte));
}

async function buscar() {
  limpiarErrores();
  if (!$("datosValor").value.trim()) {
    marcarError("valor", "Escribe el valor que quieres buscar");
    return;
  }
  try {
    const r = await pedir("/api/datos/buscar", { metodo: "POST", cuerpo: consulta() });
    pintarTabla(r.vista, r.filaEnPagina);
    if (r.encontrado) {
      mostrarResultadoConsulta([r.columna + " = \"" + r.valor + "\": ", crear("b", "", "Posición " + formatoNumero(r.posicion)), " · " + formatoNumero(r.comparaciones) + " comparaciones · " + formatoMs(r.ms) + ". Se recorrió la lista nodo por nodo desde el inicio."]);
    } else {
      mostrarResultadoConsulta([r.columna + " = \"" + r.valor + "\": ", crear("b", "", "no existe"), ". Se recorrió toda la lista: " + formatoNumero(r.comparaciones) + " comparaciones · " + formatoMs(r.ms) + "."]);
    }
  } catch (error) {
    toast(error.message, "error");
  }
}

async function filtrar() {
  limpiarErrores();
  if (!$("datosValor").value.trim()) {
    marcarError("valor", "Escribe el texto para filtrar");
    return;
  }
  try {
    const r = await pedir("/api/datos/filtrar", { metodo: "POST", cuerpo: consulta() });
    pintarTabla(r.vista);
    mostrarResultadoConsulta([crear("b", "", formatoNumero(r.coincidencias) + " coincidencias"), " de " + formatoNumero(r.vista.totalCargados) + " registros · " + formatoNumero(r.comparaciones) + " comparaciones · " + formatoMs(r.ms) + "."]);
  } catch (error) {
    toast(error.message, "error");
  }
}

async function verTodos() {
  try {
    const r = await pedir("/api/datos/todos", { metodo: "POST" });
    pintarTabla(r.vista);
    mostrarResultadoConsulta(["Mostrando los " + formatoNumero(r.vista.total) + " registros cargados."]);
  } catch (error) {
    toast(error.message, "error");
  }
}

export async function iniciarDatos() {
  try {
    [fuentes, archivos] = await Promise.all([pedir("/api/datos/fuentes"), pedir("/api/datos/archivos")]);
  } catch (error) {
    toast("No se pudieron leer las fuentes de datos: " + error.message, "error");
    return;
  }
  pintarFuentes();
  pintarArchivos();
  elegirFuente(0);
  prepararEstructura();
  mostrarCamposDelModo();
  $("datosModo").addEventListener("change", mostrarCamposDelModo);
  $("botonCargarDatos").addEventListener("click", cargar);
  $("botonBuscarDatos").addEventListener("click", buscar);
  $("botonFiltrarDatos").addEventListener("click", filtrar);
  $("botonTodosDatos").addEventListener("click", verTodos);
  $("datosValor").addEventListener("keydown", (evento) => {
    if (evento.key === "Enter") {
      buscar();
    }
  });
  document.querySelectorAll("#vista-datos input").forEach((campo) => {
    campo.addEventListener("input", () => campo.closest(".campo").classList.remove("invalido"));
  });
}
