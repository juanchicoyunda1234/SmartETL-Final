import { animate, stagger } from "/recursos/anime.esm.min.js";
import autoAnimate from "/recursos/auto-animate.mjs";
import { pedir } from "/web/js/api.js";
import {
  ejecutarTarea, crear, icono, tarjetaKpi, toast, formatoNumero, formatoBytes, celdaValor, pintarPaginacion, abrirModal, color,
} from "/web/js/interfaz.js";
import { mostrarResultadoIa } from "/web/js/resultadoIa.js";

const $ = (id) => document.getElementById(id);

export const ETAPAS_ETL = [
  { texto: "Extract: leer airports.csv y encolar", prefijo: "Extract: leyendo airports" },
  { texto: "Transform de aeropuertos", prefijo: "Transform de aeropuertos" },
  { texto: "Extract: leer routes.csv y encolar", prefijo: "Extract: leyendo routes" },
  { texto: "Transform de rutas", prefijo: "Transform de rutas" },
  { texto: "Extract: leer airlines.csv y encolar", prefijo: "Extract: leyendo airlines" },
  { texto: "Transform de aerolíneas", prefijo: "Transform de aerol" },
];

const COLORES_TIPO = {
  CAMPO_OBLIGATORIO: "--azul",
  ID_INVALIDO: "#ec4899",
  RANGO_INVALIDO: "#f97316",
  DUPLICADO: "--morado",
  REFERENCIA_ROTA: "--error",
  ID_NULO_NO_RECUPERABLE: "--aviso",
};

const NOMBRES_FUENTE = ["Aeropuertos", "Rutas", "Aerolíneas"];

let resultado = null;
let colaElegida = 0;
let validosElegidos = 0;
let tipoElegido = "TODOS";
let tramo = null;
let tiempos = { extract: 0, transform: 0 };
const graficos = {};

function sinMovimiento() {
  return matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function colorTipo(tipo) {
  const valor = COLORES_TIPO[tipo] || "--texto-tenue";
  return valor.startsWith("--") ? color(valor) : valor;
}

function nodo(etapa) {
  return document.querySelector('#etlPipeline [data-etapa="' + etapa + '"]');
}

function ponerNodo(etapa, estado, valor) {
  const elemento = nodo(etapa);
  elemento.classList.remove("activo", "hecho");
  if (estado) {
    elemento.classList.add(estado);
  }
  if (valor !== undefined) {
    elemento.querySelector(".nodo-valor").textContent = valor;
  }
}

function ponerFlecha(hacia, estado) {
  const flecha = document.querySelector('#etlPipeline [data-hacia="' + hacia + '"]');
  flecha.classList.remove("llena", "fluye");
  if (estado) {
    flecha.classList.add(estado);
  }
}

function reiniciarPipeline() {
  ["extract", "cola", "transform", "validos", "pila", "load"].forEach((etapa) => ponerNodo(etapa, "", "—"));
  ["cola", "transform", "validos", "load"].forEach((hacia) => ponerFlecha(hacia, ""));
  tramo = null;
  tiempos = { extract: 0, transform: 0 };
}

function avanzarPipeline(datos) {
  const categoria = datos.mensaje.startsWith("Extract") ? "extract" : datos.mensaje.startsWith("Transform") ? "transform" : null;
  if (tramo) {
    tiempos[tramo.categoria] += datos.ms - tramo.desde;
  }
  tramo = categoria ? { categoria, desde: datos.ms } : null;
  if (categoria === "extract") {
    ponerNodo("extract", "activo", formatoNumero(tiempos.extract) + " ms");
    ponerNodo("cola", "activo", "llenando");
    ponerFlecha("cola", "fluye");
    ponerFlecha("transform", tiempos.transform > 0 ? "llena" : "");
  }
  if (categoria === "transform") {
    ponerNodo("extract", "hecho", formatoNumero(tiempos.extract) + " ms");
    ponerNodo("cola", "activo", "vaciando");
    ponerNodo("transform", "activo", formatoNumero(tiempos.transform) + " ms");
    ponerNodo("validos", "activo");
    ponerNodo("pila", "activo");
    ponerFlecha("cola", "llena");
    ponerFlecha("transform", "fluye");
    ponerFlecha("validos", "fluye");
  }
}

function pipelineCompleto(r) {
  const encolados = r.fuentes.reduce((suma, fuente) => suma + fuente.encolados, 0);
  ponerNodo("extract", "hecho", formatoNumero(r.msExtract) + " ms");
  ponerNodo("cola", "hecho", formatoNumero(encolados) + " encolados");
  ponerNodo("transform", "hecho", formatoNumero(r.msTransform) + " ms");
  ponerNodo("validos", "hecho", formatoNumero(r.validos));
  ponerNodo("pila", "hecho", formatoNumero(r.errores));
  ["cola", "transform", "validos"].forEach((hacia) => ponerFlecha(hacia, "llena"));
  if (r.archivos.length > 0) {
    ponerNodo("load", "hecho", r.archivos.length + " archivos");
    ponerFlecha("load", "llena");
  } else {
    ponerNodo("load", "", "pendiente");
  }
}

function pintarKpis(r) {
  $("kpisEtl").replaceChildren(
    tarjetaKpi("database", "Procesados", r.procesados, 0, "", r.fuentes.map((f) => formatoNumero(f.procesados) + " " + f.fuente).join(" · ")),
    tarjetaKpi("circle-check", "Válidos", r.validos, 0, "", "Pasan a las listas de válidos"),
    tarjetaKpi("triangle-alert", "Errores", r.errores, 0, "", "Apilados en la pila de errores"),
    tarjetaKpi("timer", "Tiempo", (r.msExtract + r.msTransform) / 1000, 2, " s", "Extract " + formatoNumero(r.msExtract) + " ms · Transform " + formatoNumero(r.msTransform) + " ms")
  );
}

function segmentado(caja, elegido, alElegir) {
  caja.replaceChildren();
  NOMBRES_FUENTE.forEach((nombre, indice) => {
    const boton = crear("button", "", nombre);
    boton.type = "button";
    boton.setAttribute("aria-pressed", String(indice === elegido));
    boton.addEventListener("click", () => alElegir(indice));
    caja.append(boton);
  });
}

function etiquetaCola(texto) {
  const aeropuerto = texto.match(/^Aeropuerto\[(\d+), ([^,]*),.*IATA=([^\]]*)\]$/);
  if (aeropuerto) {
    return { titulo: aeropuerto[3], detalle: aeropuerto[2] };
  }
  const ruta = texto.match(/^Ruta\[([^:]*): (\S+) \(.*\) -> (\S+) \(.*\)\]$/);
  if (ruta) {
    return { titulo: ruta[2] + "→" + ruta[3], detalle: "aerolínea " + ruta[1] };
  }
  const aerolinea = texto.match(/^Aerolinea\[([^,]*), ([^,]*),/);
  if (aerolinea) {
    return { titulo: aerolinea[2], detalle: "id " + aerolinea[1] };
  }
  return { titulo: texto, detalle: "" };
}

function pintarCola(indice) {
  colaElegida = indice;
  segmentado($("etlColaFuentes"), indice, pintarCola);
  const fuente = resultado.fuentes[indice];
  const cola = $("etlCola");
  cola.replaceChildren(crear("span", "cola-extremo", "frente →"));
  fuente.cola.forEach((texto, posicion) => {
    const etiqueta = etiquetaCola(texto);
    const caja = crear("div", "cola-caja" + (posicion === 0 ? " frente" : ""));
    caja.title = texto;
    caja.append(crear("b", "", etiqueta.titulo), crear("span", "", etiqueta.detalle));
    cola.append(caja);
  });
  cola.append(crear("span", "cola-extremo", fuente.encolados > fuente.cola.length ? "… ← final" : "← final"));
  $("etlColaNota").textContent = "Primeros " + fuente.cola.length + " de " + formatoNumero(fuente.encolados) + " registros encolados desde " + fuente.archivo + ". Transform los desencola en este orden.";
  if (!sinMovimiento()) {
    animate(cola.querySelectorAll(".cola-caja"), { translateX: { from: 36, to: 0 }, opacity: { from: 0, to: 1 }, delay: stagger(30), duration: 320, ease: "outCubic" });
  }
}

function pintarFiltros() {
  const caja = $("etlPilaFiltros");
  caja.replaceChildren();
  const opciones = [{ tipo: "TODOS", total: resultado.errores }].concat(resultado.tipos.filter((t) => t.total > 0));
  opciones.forEach((opcion) => {
    const boton = crear("button", "filtro" + (opcion.tipo === "TODOS" ? "" : " tipo-" + opcion.tipo));
    boton.type = "button";
    boton.setAttribute("aria-pressed", String(opcion.tipo === tipoElegido));
    boton.append(crear("i"), crear("span", "", (opcion.tipo === "TODOS" ? "Todos" : opcion.tipo) + " · " + formatoNumero(opcion.total)));
    boton.addEventListener("click", () => {
      tipoElegido = opcion.tipo;
      pintarFiltros();
      cargarPila(1);
    });
    caja.append(boton);
  });
}

function tarjetaError(error) {
  const tarjeta = crear("div", "error-tarjeta tipo-" + error.tipo);
  const cabecera = crear("div", "error-cabecera");
  cabecera.append(crear("span", "error-tipo", error.tipo), crear("span", "", error.fuente + " · línea " + formatoNumero(error.linea)), crear("small", "", "#" + error.posicion));
  tarjeta.append(cabecera, crear("b", "", error.identificador), crear("span", "error-motivo", error.motivo + " (valor: " + error.valorOriginal + ")"));
  return tarjeta;
}

async function cargarPila(pagina) {
  try {
    const datos = await pedir("/api/etl/errores?tipo=" + encodeURIComponent(tipoElegido) + "&pagina=" + pagina);
    const pila = $("etlPila");
    const elementos = [];
    if (datos.pagina === 1) {
      elementos.push(crear("div", "pila-extremo", "▼ cima (último error encontrado)"));
    }
    datos.errores.forEach((error) => elementos.push(tarjetaError(error)));
    if (datos.pagina === datos.paginas && datos.total > 0) {
      elementos.push(crear("div", "pila-extremo", "▲ fondo (primer error encontrado)"));
    }
    if (datos.total === 0) {
      elementos.push(crear("p", "nota", "La pila está vacía: ningún registro falló."));
    }
    pila.replaceChildren(...elementos);
    pila.scrollTop = 0;
    const desde = datos.total === 0 ? 0 : (datos.pagina - 1) * 20 + 1;
    pintarPaginacion($("etlPilaPaginacion"), { pagina: datos.pagina, paginas: datos.paginas, total: datos.total, desde, hasta: desde + datos.errores.length - 1 }, cargarPila, "Errores");
  } catch (error) {
    toast(error.message, "error");
  }
}

function pintarTablaFuentes(r) {
  const tabla = $("etlTablaFuentes");
  const cabecera = crear("tr");
  ["Fuente", "Procesados", "Válidos", "Inválidos"].forEach((texto, i) => cabecera.append(crear("th", i > 0 ? "numero" : "", texto)));
  const cuerpo = crear("tbody");
  r.fuentes.forEach((fuente) => {
    const fila = crear("tr");
    fila.append(crear("td", "", fuente.fuente), crear("td", "numero", formatoNumero(fuente.procesados)), crear("td", "numero", formatoNumero(fuente.validos)), crear("td", "numero", formatoNumero(fuente.invalidos)));
    cuerpo.append(fila);
  });
  const total = crear("tr");
  total.append(crear("td", "", "Total"), crear("td", "numero", formatoNumero(r.procesados)), crear("td", "numero", formatoNumero(r.validos)), crear("td", "numero", formatoNumero(r.errores)));
  total.style.fontWeight = "700";
  cuerpo.append(total);
  const thead = crear("thead");
  thead.append(cabecera);
  tabla.replaceChildren(thead, cuerpo);
}

function opcionesGrafico() {
  return {
    responsive: true,
    maintainAspectRatio: false,
    animation: sinMovimiento() ? false : { duration: 700, easing: "easeOutQuart" },
    plugins: { legend: { labels: { color: color("--texto-suave"), boxWidth: 12 } } },
  };
}

function pintarGraficos(r) {
  if (!window.Chart) {
    return;
  }
  Object.values(graficos).forEach((grafico) => grafico.destroy());
  graficos.dona = new window.Chart($("etlDona"), {
    type: "doughnut",
    data: { labels: ["Válidos", "Inválidos"], datasets: [{ data: [r.validos, r.errores], backgroundColor: [color("--exito"), color("--error")], borderColor: color("--superficie"), borderWidth: 2 }] },
    options: Object.assign(opcionesGrafico(), { cutout: "62%" }),
  });
  const tipos = r.tipos.filter((t) => t.total > 0);
  const opciones = opcionesGrafico();
  opciones.plugins.legend = { display: false };
  opciones.scales = {
    x: { ticks: { color: color("--texto-suave"), font: { size: 10 } }, grid: { display: false } },
    y: { beginAtZero: true, ticks: { color: color("--texto-suave"), precision: 0 }, grid: { color: color("--borde") } },
  };
  graficos.barras = new window.Chart($("etlBarras"), {
    type: "bar",
    data: { labels: tipos.map((t) => t.tipo.replace(/_/g, " ")), datasets: [{ label: "Errores", data: tipos.map((t) => t.total), backgroundColor: tipos.map((t) => colorTipo(t.tipo)), borderRadius: 6 }] },
    options: opciones,
  });
}

async function cargarValidos(fuente, pagina) {
  validosElegidos = fuente;
  segmentado($("etlValidosFuentes"), fuente, (indice) => cargarValidos(indice, 1));
  try {
    const vista = await pedir("/api/etl/validos?fuente=" + fuente + "&pagina=" + pagina);
    const tabla = $("etlValidosTabla");
    const cabecera = crear("tr");
    cabecera.append(crear("th", "", "#"));
    vista.encabezados.forEach((encabezado) => cabecera.append(crear("th", "", encabezado)));
    tabla.tHead.replaceChildren(cabecera);
    const cuerpo = tabla.tBodies[0];
    cuerpo.replaceChildren();
    vista.filas.forEach((valores, indice) => {
      const fila = crear("tr");
      fila.append(crear("td", "numero-fila", formatoNumero(vista.desde + indice)));
      valores.forEach((valor) => fila.append(celdaValor(valor)));
      cuerpo.append(fila);
    });
    pintarPaginacion($("etlValidosPaginacion"), vista, (n) => cargarValidos(validosElegidos, n));
  } catch (error) {
    toast(error.message, "error");
  }
}

function pintarArchivos(archivos) {
  $("etlArchivos").hidden = archivos.length === 0;
  const lista = $("etlListaArchivos");
  lista.replaceChildren();
  archivos.forEach((archivo, indice) => {
    const fila = crear("li");
    fila.style.animationDelay = indice * 50 + "ms";
    const ver = crear("button", "boton");
    ver.type = "button";
    ver.append(icono("eye"), crear("span", "", "Ver"));
    ver.addEventListener("click", () => verArchivo(archivo.nombre));
    fila.append(icono("file-text"), crear("span", "archivo-nombre", "output/" + archivo.nombre), crear("span", "archivo-tamanio", formatoBytes(archivo.bytes)), ver);
    lista.append(fila);
  });
}

async function verArchivo(nombre) {
  try {
    const archivo = await pedir("/api/output/" + encodeURIComponent(nombre));
    const caja = crear("div");
    const aviso = archivo.totalLineas > archivo.mostradas ? "Se muestran las primeras " + formatoNumero(archivo.mostradas) + " de " + formatoNumero(archivo.totalLineas) + " líneas." : formatoNumero(archivo.totalLineas) + " líneas.";
    caja.append(crear("p", "nota", aviso + " Tamaño: " + formatoBytes(archivo.bytes) + "."), crear("pre", "texto-mono", archivo.contenido));
    abrirModal("output/" + nombre, caja);
  } catch (error) {
    toast(error.message, "error");
  }
}

function habilitar(id, activo) {
  const boton = $(id);
  boton.dataset.deshabilitado = activo ? "no" : "si";
  boton.disabled = !activo;
}

function pintarFuenteChip(modo) {
  $("etlFuente").lastElementChild.textContent = modo === "DEMO"
    ? "Fuente: archivos sucios de data/demo (aerolíneas reales)"
    : "Fuente: archivos completos (airports, routes, airlines)";
}

async function mostrarResultado() {
  resultado = await pedir("/api/etl/resultado");
  $("vacioEtl").hidden = true;
  $("etlContenido").hidden = false;
  pintarFuenteChip(resultado.modo);
  pipelineCompleto(resultado);
  pintarKpis(resultado);
  const modo = crear("span", "chip " + (resultado.modo === "REAL" ? "chip-acento" : "chip-aviso"), resultado.modo);
  modo.style.gridColumn = "1 / -1";
  modo.style.justifySelf = "start";
  $("kpisEtl").prepend(modo);
  pintarCola(colaElegida);
  tipoElegido = "TODOS";
  pintarFiltros();
  cargarPila(1);
  pintarTablaFuentes(resultado);
  pintarGraficos(resultado);
  cargarValidos(0, 1);
  $("etlReporteTexto").textContent = resultado.reporte;
  pintarArchivos(resultado.archivos);
  habilitar("botonEtlCargar", true);
  habilitar("botonEtlResumenIa", true);
  habilitar("botonEtlFlujoIa", true);
}

async function ejecutar(modo) {
  reiniciarPipeline();
  try {
    const final = await ejecutarTarea({
      ruta: "/api/etl/ejecutar",
      cuerpo: { modo },
      titulo: modo === "demo" ? "ETL de demostración" : "ETL con datos reales",
      contenedor: $("resultadosEtl"),
      etapas: ETAPAS_ETL,
      alAviso: avanzarPipeline,
    });
    $("etlIaResultado").replaceChildren();
    await mostrarResultado();
    toast("ETL " + (modo === "demo" ? "de demostración" : "real") + " terminado en " + formatoNumero(final.ms) + " ms: " + formatoNumero(final.datos.errores) + " errores en la pila", "exito");
  } catch (error) {
    reiniciarPipeline();
  }
}

async function cargar() {
  ponerNodo("load", "activo", "escribiendo");
  ponerFlecha("load", "fluye");
  try {
    const final = await ejecutarTarea({ ruta: "/api/etl/cargar", titulo: "Load: escribir output/", contenedor: $("resultadosEtl") });
    ponerNodo("load", "hecho", final.datos.archivos.length + " archivos · " + formatoNumero(final.datos.ms) + " ms");
    ponerFlecha("load", "llena");
    pintarArchivos(final.datos.archivos);
    $("etlArchivos").scrollIntoView({ block: "nearest", behavior: sinMovimiento() ? "auto" : "smooth" });
    toast("Load terminado: " + final.datos.archivos.length + " archivos en output/", "exito");
  } catch (error) {
    ponerNodo("load", "", "pendiente");
    ponerFlecha("load", "");
  }
}

async function pedirIa(funcion, titulo) {
  try {
    const final = await ejecutarTarea({ ruta: "/api/ia/funcion", cuerpo: { funcion }, titulo });
    mostrarResultadoIa($("etlIaResultado"), final.datos.resultado);
  } catch (error) {
  }
}

export async function iniciarEtl(estado) {
  $("botonEtlReal").addEventListener("click", () => ejecutar("real"));
  $("botonEtlDemo").addEventListener("click", () => ejecutar("demo"));
  $("botonEtlCargar").addEventListener("click", cargar);
  document.querySelector('[data-accion="etl-real"]').addEventListener("click", () => ejecutar("real"));
  $("botonEtlResumenIa").addEventListener("click", () => pedirIa("errores_etl", "Resumen de errores con IA"));
  $("botonEtlFlujoIa").addEventListener("click", () => pedirIa("flujo_etl", "Flujograma del ETL con IA"));
  habilitar("botonEtlResumenIa", false);
  habilitar("botonEtlFlujoIa", false);
  autoAnimate($("etlPila"));
  document.addEventListener("tema", () => {
    if (resultado) {
      pintarGraficos(resultado);
    }
  });
  document.addEventListener("etl-actualizado", () => {
    reiniciarPipeline();
    mostrarResultado().catch((error) => toast(error.message, "error"));
  });
  if (estado && estado.etlEjecutado) {
    try {
      await mostrarResultado();
    } catch (error) {
      toast(error.message, "error");
    }
  }
}
