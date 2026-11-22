import { animate, stagger } from "/recursos/anime.esm.min.js";
import { pedir } from "/web/js/api.js";
import {
  ejecutarTarea, crear, icono, toast, formatoNumero, tarjetaKpi, segmentado, color,
} from "/web/js/interfaz.js";
import { ETAPAS_ETL } from "/web/js/etl.js";

const $ = (id) => document.getElementById(id);
const NS = "http://www.w3.org/2000/svg";

const estado = { datosListos: false, arbol: null, algoritmo: "Burbuja", recorrido: "Inorden" };
const graficos = {};
const vis = {
  muestra: [], semilla: 0, cantidad: 12, campo: "IATA", algoritmo: "Burbuja", java: null,
  orden: [], pasos: [], indice: 0, comparaciones: 0, movimientos: 0, reloj: null, cajas: [],
};

function sinMovimiento() {
  return matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function textoCampo(campo) {
  return campo === "Pais" ? "País" : campo;
}

function habilitarSegunEstado() {
  document.querySelectorAll("#vista-estructuras [data-requiere]").forEach((boton) => {
    const listo = boton.dataset.requiere === "datos" ? estado.datosListos : estado.datosListos && estado.arbol !== null;
    boton.dataset.deshabilitado = listo ? "no" : "si";
    boton.disabled = !listo;
  });
  $("estDatos").hidden = estado.datosListos;
  $("estCopia").textContent = estado.arbol ? "Copia de trabajo: " + formatoNumero(estado.arbol.nodos) + " nodos · altura " + formatoNumero(estado.arbol.altura) : "Sin construir";
  $("estCopia").className = "chip " + (estado.arbol ? "chip-acento" : "chip-neutro");
}

async function refrescarEstado() {
  try {
    const datos = await pedir("/api/estructuras/estado");
    estado.datosListos = datos.datosListos;
    estado.arbol = datos.arbol;
    habilitarSegunEstado();
    if (estado.datosListos && vis.muestra.length === 0) {
      nuevaMuestra();
    }
  } catch (error) {
    toast(error.message, "error");
  }
}

async function prepararDatos() {
  try {
    await ejecutarTarea({ ruta: "/api/etl/ejecutar", cuerpo: { modo: "real" }, titulo: "Preparando los datos (ETL real)", etapas: ETAPAS_ETL });
    document.dispatchEvent(new CustomEvent("etl-actualizado"));
    await refrescarEstado();
    toast("Datos listos: aeropuertos limpios del ETL real", "exito");
  } catch (error) {
  }
}

function mostrarSub(nombre) {
  document.querySelectorAll("#estPestanas button").forEach((boton) => boton.setAttribute("aria-selected", String(boton.dataset.sub === nombre)));
  document.querySelectorAll("#vista-estructuras .subvista").forEach((panel) => panel.classList.toggle("activa", panel.id === "est-" + nombre));
  if (nombre === "ordenamiento") {
    posicionarCajas();
  }
}

function tablaAeropuertos(aeropuertos) {
  const tabla = crear("table", "tabla tabla-compacta");
  const cabecera = crear("tr");
  ["#", "IATA", "Nombre", "Ciudad", "País"].forEach((texto) => cabecera.append(crear("th", "", texto)));
  const thead = crear("thead");
  thead.append(cabecera);
  const cuerpo = crear("tbody");
  aeropuertos.forEach((a, i) => {
    const fila = crear("tr");
    [String(i + 1), a.iata, a.nombre, a.ciudad, a.pais].forEach((valor, c) => fila.append(crear("td", c === 0 ? "numero-fila" : "", valor)));
    cuerpo.append(fila);
  });
  tabla.append(thead, cuerpo);
  const contenedor = crear("div", "tabla-contenedor");
  contenedor.append(tabla);
  return contenedor;
}

function pintarOrden(r) {
  const kpis = crear("div", "kpis");
  const movimientos = r.nombreMovimientos.charAt(0).toUpperCase() + r.nombreMovimientos.slice(1);
  kpis.append(
    tarjetaKpi("gauge", "Comparaciones", r.comparaciones, 0, "", r.algoritmo + " por " + r.campo + " sobre " + formatoNumero(r.elementos) + " aeropuertos"),
    tarjetaKpi("shuffle", movimientos, r.movimientos, 0, "", r.algoritmo === "Burbuja" ? "pares vecinos intercambiados" : "elementos corridos a la derecha"),
    tarjetaKpi("timer", "Tiempo", r.ms, 1, " ms", r.ordenada ? "Verificado: la copia quedó ordenada" : "La copia no quedó ordenada")
  );
  const bloque = crear("div", "resultado-bloque");
  bloque.append(kpis, crear("div", "panel-titulo sin-margen", "Primeros 15 después de ordenar"), tablaAeropuertos(r.primeros));
  $("estOrdenResultado").replaceChildren(bloque);
}

async function ordenar() {
  const campo = $("estCampo").value;
  try {
    const final = await ejecutarTarea({ ruta: "/api/estructuras/ordenar", cuerpo: { algoritmo: estado.algoritmo, campo }, titulo: "Ordenando con " + estado.algoritmo + " por " + textoCampo(campo) });
    pintarOrden(final.datos);
  } catch (error) {
  }
}

function opcionesBase() {
  return {
    responsive: true,
    maintainAspectRatio: false,
    animation: sinMovimiento() ? false : { duration: 700, easing: "easeOutQuart" },
    plugins: { legend: { labels: { color: color("--texto-suave"), boxWidth: 12 } } },
  };
}

function ejes(extra) {
  const base = {
    x: { ticks: { color: color("--texto-suave") }, grid: { display: false } },
    y: { beginAtZero: true, ticks: { color: color("--texto-suave") }, grid: { color: color("--borde") } },
  };
  return Object.assign(base, extra || {});
}

function graficoComparacion(datos) {
  if (graficos.orden) {
    graficos.orden.destroy();
  }
  const opciones = opcionesBase();
  opciones.scales = ejes({
    y: { beginAtZero: true, title: { display: true, text: "comparaciones", color: color("--texto-suave") }, ticks: { color: color("--texto-suave") }, grid: { color: color("--borde") } },
    y1: { beginAtZero: true, position: "right", title: { display: true, text: "milisegundos", color: color("--texto-suave") }, ticks: { color: color("--texto-suave") }, grid: { display: false } },
  });
  graficos.orden = new window.Chart($("estGraficoOrden"), {
    type: "bar",
    data: {
      labels: ["Burbuja", "Inserción"],
      datasets: [
        { label: "Comparaciones", data: [datos.burbuja.comparaciones, datos.insercion.comparaciones], backgroundColor: color("--aviso"), borderRadius: 6, yAxisID: "y" },
        { label: "Tiempo (ms)", data: [datos.burbuja.ms, datos.insercion.ms], backgroundColor: color("--acento"), borderRadius: 6, yAxisID: "y1" },
      ],
    },
    options: opciones,
  });
}

async function comparar() {
  const campo = $("estCampo").value;
  try {
    const final = await ejecutarTarea({
      ruta: "/api/estructuras/comparar-ordenamientos",
      cuerpo: { campo },
      titulo: "Burbuja vs inserción por " + textoCampo(campo),
      etapas: [{ texto: "Burbuja", prefijo: "Burbuja" }, { texto: "Inserción", prefijo: "Inserci" }],
    });
    const datos = final.datos;
    $("estComparacion").hidden = false;
    const menos = 100 - (datos.insercion.comparaciones * 100) / datos.burbuja.comparaciones;
    $("estComparacionNota").textContent = "Inserción hace " + formatoNumero(menos, 1) + " % menos comparaciones por " + textoCampo(campo);
    graficoComparacion(datos);
    $("estComparacion").scrollIntoView({ block: "nearest", behavior: sinMovimiento() ? "auto" : "smooth" });
  } catch (error) {
  }
}

function claveDe(aeropuerto) {
  return vis.campo === "IATA" ? aeropuerto.iata : vis.campo === "Nombre" ? aeropuerto.nombre : aeropuerto.pais;
}

function comparar2(x, y) {
  return x < y ? -1 : x > y ? 1 : 0;
}

function generarPasos() {
  const claves = vis.muestra.map(claveDe);
  const orden = claves.map((clave, indice) => indice);
  const pasos = [];
  const n = orden.length;
  if (vis.algoritmo === "Burbuja") {
    for (let i = 0; i < n - 1; i++) {
      let huboIntercambio = false;
      for (let j = 0; j < n - 1 - i; j++) {
        pasos.push({ tipo: "compara", a: j, b: j + 1 });
        if (comparar2(claves[orden[j]], claves[orden[j + 1]]) > 0) {
          [orden[j], orden[j + 1]] = [orden[j + 1], orden[j]];
          pasos.push({ tipo: "cambia", a: j, b: j + 1 });
          huboIntercambio = true;
        }
      }
      pasos.push({ tipo: "fija", posicion: n - 1 - i });
      if (!huboIntercambio) {
        break;
      }
    }
  } else {
    for (let i = 1; i < n; i++) {
      let j = i - 1;
      while (j >= 0) {
        pasos.push({ tipo: "compara", a: j, b: j + 1 });
        if (comparar2(claves[orden[j]], claves[orden[j + 1]]) <= 0) {
          break;
        }
        [orden[j], orden[j + 1]] = [orden[j + 1], orden[j]];
        pasos.push({ tipo: "cambia", a: j, b: j + 1 });
        j--;
      }
    }
  }
  pasos.push({ tipo: "listo" });
  return pasos;
}

function demora() {
  return 1100 - Number($("visVelocidad").value) * 100;
}

function posicionarCajas() {
  const escenario = $("visEscenario");
  const n = vis.cajas.length;
  if (n === 0 || escenario.clientWidth === 0) {
    return;
  }
  const ancho = escenario.clientWidth / n;
  escenario.style.setProperty("--vis-duracion", Math.min(260, demora() * 0.8) + "ms");
  vis.cajas.forEach((caja, indice) => {
    caja.style.width = ancho - 6 + "px";
    caja.style.transform = "translateX(" + vis.orden.indexOf(indice) * ancho + "px)";
  });
}

function pintarContadores() {
  $("visComparaciones").textContent = formatoNumero(vis.comparaciones);
  $("visMovimientos").textContent = formatoNumero(vis.movimientos);
  $("visNombreMovimientos").textContent = vis.algoritmo === "Burbuja" ? "Intercambios" : "Desplazamientos";
}

function reiniciarVisualizador() {
  pausar();
  vis.pasos = generarPasos();
  vis.indice = 0;
  vis.comparaciones = 0;
  vis.movimientos = 0;
  vis.orden = vis.muestra.map((aeropuerto, indice) => indice);
  const escenario = $("visEscenario");
  vis.cajas = vis.muestra.map((aeropuerto) => {
    const caja = crear("div", "vis-caja");
    caja.title = aeropuerto.iata + " · " + aeropuerto.nombre + " · " + aeropuerto.pais;
    caja.append(crear("b", "", aeropuerto.iata));
    if (vis.campo !== "IATA") {
      caja.append(crear("span", "", claveDe(aeropuerto)));
    }
    return caja;
  });
  escenario.replaceChildren(...vis.cajas);
  posicionarCajas();
  pintarContadores();
  $("visValidacion").replaceChildren();
}

function cajaEn(posicion) {
  return vis.cajas[vis.orden[posicion]];
}

function aplicar(paso) {
  vis.cajas.forEach((caja) => caja.classList.remove("comparando", "cambiando"));
  if (paso.tipo === "compara") {
    vis.comparaciones++;
    cajaEn(paso.a).classList.add("comparando");
    cajaEn(paso.b).classList.add("comparando");
  } else if (paso.tipo === "cambia") {
    vis.movimientos++;
    [vis.orden[paso.a], vis.orden[paso.b]] = [vis.orden[paso.b], vis.orden[paso.a]];
    posicionarCajas();
    cajaEn(paso.a).classList.add("cambiando");
    cajaEn(paso.b).classList.add("cambiando");
  } else if (paso.tipo === "fija") {
    cajaEn(paso.posicion).classList.add("ordenado");
  } else {
    vis.cajas.forEach((caja) => caja.classList.add("ordenado"));
    validarContraJava();
  }
  pintarContadores();
}

function validarContraJava() {
  const java = vis.algoritmo === "Burbuja" ? vis.java.burbuja : vis.java.insercion;
  const coincide = java.comparaciones === vis.comparaciones && java.movimientos === vis.movimientos;
  const nombre = vis.algoritmo === "Burbuja" ? "intercambios" : "desplazamientos";
  const chip = crear("span", "chip " + (coincide ? "chip-exito" : "chip-error"));
  chip.append(icono(coincide ? "circle-check" : "circle-alert"), crear("span", "",
    "Animación: " + vis.comparaciones + " comparaciones · " + vis.movimientos + " " + nombre
    + "   ·   Java: " + java.comparaciones + " comparaciones · " + java.movimientos + " " + nombre + (coincide ? "  ✓" : "  ✗")));
  $("visValidacion").replaceChildren(chip);
  pausar();
}

function avanzar() {
  if (vis.indice >= vis.pasos.length) {
    pausar();
    return;
  }
  aplicar(vis.pasos[vis.indice]);
  vis.indice++;
}

function pausar() {
  clearInterval(vis.reloj);
  vis.reloj = null;
  const boton = $("visReproducir");
  boton.querySelector(".icono").style.setProperty("--icono", "url(/recursos/iconos/play.svg)");
  boton.lastElementChild.textContent = vis.indice >= vis.pasos.length && vis.pasos.length > 0 ? "Terminado" : "Reproducir";
}

function reproducir() {
  if (vis.reloj) {
    pausar();
    return;
  }
  if (vis.indice >= vis.pasos.length) {
    reiniciarVisualizador();
  }
  vis.reloj = setInterval(avanzar, demora());
  const boton = $("visReproducir");
  boton.querySelector(".icono").style.setProperty("--icono", "url(/recursos/iconos/pause.svg)");
  boton.lastElementChild.textContent = "Pausa";
}

function unPaso() {
  pausar();
  avanzar();
}

function cambiarVelocidad() {
  posicionarCajas();
  if (vis.reloj) {
    clearInterval(vis.reloj);
    vis.reloj = setInterval(avanzar, demora());
  }
}

async function cargarMuestra() {
  try {
    const datos = await pedir("/api/estructuras/muestra", { metodo: "POST", cuerpo: { cantidad: vis.cantidad, campo: vis.campo, semilla: vis.semilla } });
    vis.muestra = datos.muestra;
    vis.java = datos.java;
    reiniciarVisualizador();
  } catch (error) {
    $("visEscenario").replaceChildren(crear("p", "nota", error.message));
  }
}

function nuevaMuestra() {
  vis.semilla = Math.floor(Math.random() * 100000);
  cargarMuestra();
}

function grafoBusqueda(r) {
  if (graficos.busqueda) {
    graficos.busqueda.destroy();
  }
  const lienzo = crear("canvas");
  lienzo.setAttribute("aria-label", "Comparaciones de cada búsqueda en escala logarítmica");
  const opciones = opcionesBase();
  opciones.plugins.legend = { display: false };
  opciones.scales = ejes({ y: { type: "logarithmic", min: 1, title: { display: true, text: "comparaciones (escala logarítmica)", color: color("--texto-suave") }, ticks: { color: color("--texto-suave") }, grid: { color: color("--borde") } } });
  setTimeout(() => {
    graficos.busqueda = new window.Chart(lienzo, {
      type: "bar",
      data: {
        labels: ["Secuencial (lista enlazada)", "Binaria (lista secuencial)", "BST por IATA"],
        datasets: [{ data: [r.secuencial.comparaciones, r.binaria.comparaciones, r.bst.comparaciones], backgroundColor: [color("--error"), color("--acento"), color("--exito")], borderRadius: 6 }],
      },
      options: opciones,
    });
  }, 0);
  const caja = crear("div", "grafico grafico-alto");
  caja.append(lienzo);
  return caja;
}

function metrica(nombreIcono, titulo, detalle, comparaciones, ms) {
  const fila = crear("div", "metrica");
  const textos = crear("div");
  textos.append(crear("div", "", titulo), crear("small", "", detalle));
  const numeros = crear("div");
  numeros.style.textAlign = "right";
  numeros.append(crear("b", "", formatoNumero(comparaciones)), crear("small", "", "comparaciones · " + formatoNumero(ms, ms < 1 ? 3 : 2) + " ms"));
  fila.append(icono(nombreIcono), textos, numeros);
  return fila;
}

function pintarCamino(camino, final) {
  const caja = crear("div", "camino");
  camino.forEach((paso, indice) => {
    const esUltimo = indice === camino.length - 1;
    const clase = paso.lado === "encontrado" ? " encontrado" : paso.lado === "vacío" ? " vacio" : "";
    const nodo = crear("span", "camino-nodo" + clase);
    nodo.append(crear("span", "", paso.lado === "vacío" ? "∅ no existe" : paso.codigo));
    if (!esUltimo) {
      nodo.append(crear("small", "", "comparación " + (indice + 1)));
    } else if (final) {
      nodo.append(crear("small", "", final));
    }
    caja.append(nodo);
    if (!esUltimo) {
      const lado = paso.lado === "derecha" ? "derecha" : "izquierda";
      caja.append(crear("span", "camino-paso " + lado, lado === "derecha" ? "↘ derecha (es mayor)" : "↙ izquierda (es menor)"));
    }
  });
  return caja;
}

function pintarBusqueda(r) {
  const izquierda = crear("div", "tarjeta");
  const titulo = crear("div", "panel-titulo");
  titulo.append(icono("gauge"), crear("span", "", "Comparaciones para encontrar " + r.iata));
  const metricas = crear("div", "metricas-busqueda");
  metricas.append(
    metrica("list", "Secuencial", r.secuencial.posicion > 0 ? "recorre la lista enlazada hasta la posición " + formatoNumero(r.secuencial.posicion) : "recorre los " + formatoNumero(r.elementos) + " nodos y no lo encuentra", r.secuencial.comparaciones, r.secuencial.ms),
    metrica("binary", "Binaria", "parte la lista ordenada a la mitad en cada paso", r.binaria.comparaciones, r.binaria.ms),
    metrica("git-fork", "BST por IATA", r.bst.profundidad >= 0 ? "baja hasta la profundidad " + r.bst.profundidad : "llega a un hijo vacío", r.bst.comparaciones, r.bst.ms)
  );
  izquierda.append(titulo, grafoBusqueda(r), metricas);
  if (r.ordenoAhora) {
    izquierda.append(crear("p", "nota", "Antes de la binaria se ordenó la lista por IATA con inserción (no se había ordenado por IATA en esta sesión)."));
  }
  const derecha = crear("div", "tarjeta");
  const tituloCamino = crear("div", "panel-titulo");
  tituloCamino.append(icono("git-fork"), crear("span", "", "Camino en el BST: " + r.bst.comparaciones + " comparaciones"));
  derecha.append(tituloCamino);
  if (r.aeropuerto) {
    derecha.append(crear("p", "nota", r.aeropuerto.iata + " · " + r.aeropuerto.nombre + " (" + r.aeropuerto.ciudad + ", " + r.aeropuerto.pais + ")"));
  }
  derecha.append(pintarCamino(r.camino, r.encontrado ? "encontrado" : ""));
  const rejilla = crear("div", "rejilla-2");
  rejilla.append(izquierda, derecha);
  $("estBusquedaResultado").replaceChildren(rejilla);
}

async function buscar() {
  const iata = $("estIata").value.trim().toUpperCase();
  if (!iata) {
    toast("Escribe un código IATA", "error");
    return;
  }
  try {
    pintarBusqueda(await pedir("/api/estructuras/buscar", { metodo: "POST", cuerpo: { iata } }));
  } catch (error) {
    toast(error.message, "error");
  }
}

async function construir() {
  try {
    const r = await pedir("/api/estructuras/bst/construir", { metodo: "POST" });
    estado.arbol = r.arbol;
    habilitarSegunEstado();
    toast("BST construido en " + formatoNumero(r.ms, 1) + " ms: " + formatoNumero(r.arbol.nodos) + " nodos, altura " + r.arbol.altura, "exito");
  } catch (error) {
    toast(error.message, "error");
  }
}

function textoMedida(medida) {
  return formatoNumero(medida.nodos) + " nodos · altura " + formatoNumero(medida.altura);
}

async function operar(operacion) {
  const iata = $("estClave").value.trim().toUpperCase();
  if (!iata) {
    toast("Escribe un código IATA", "error");
    return;
  }
  try {
    const r = await pedir("/api/estructuras/bst/" + operacion, { metodo: "POST", cuerpo: { iata } });
    estado.arbol = r.despues;
    habilitarSegunEstado();
    const bloque = crear("div", "resultado-bloque");
    const chips = crear("div", "resultado-ia-chips");
    const resultado = crear("span", "chip " + (r.exito ? "chip-exito" : "chip-aviso"));
    resultado.append(icono(r.exito ? "circle-check" : "info"), crear("span", "", r.mensaje));
    const cambio = crear("span", "chip chip-neutro", textoMedida(r.antes) + "  →  " + textoMedida(r.despues));
    chips.append(resultado, cambio);
    if (r.comparaciones >= 0) {
      chips.append(crear("span", "chip chip-acento", r.comparaciones + " comparaciones"));
    }
    bloque.append(chips, pintarCamino(r.camino, operacion === "insertar" && r.exito ? "nueva hoja" : r.exito ? "encontrado" : ""));
    $("estBstResultado").replaceChildren(bloque);
  } catch (error) {
    toast(error.message, "error");
  }
}

function svg(etiqueta, atributos) {
  const elemento = document.createElementNS(NS, etiqueta);
  Object.entries(atributos || {}).forEach(([clave, valor]) => elemento.setAttribute(clave, valor));
  return elemento;
}

function dibujarArbol(raiz, niveles) {
  const ancho = 800;
  const alto = 34 + niveles * 70;
  const lienzo = svg("svg", { viewBox: "0 0 " + ancho + " " + alto, class: "arbol-svg", role: "img", "aria-label": "Primeros niveles del BST" });
  const lineas = svg("g");
  const nodos = svg("g");
  const poner = (nodo, nivel, posicion, padre) => {
    if (!nodo) {
      return;
    }
    const x = ((posicion + 0.5) * ancho) / 2 ** nivel;
    const y = 28 + nivel * 70;
    if (nodo.oculto) {
      lineas.append(svg("line", { x1: padre.x, y1: padre.y, x2: x, y2: y - 24 }));
      const puntos = svg("text", { x, y: y - 14, class: "oculto" });
      puntos.textContent = "…";
      nodos.append(puntos);
      return;
    }
    if (padre) {
      lineas.append(svg("line", { x1: padre.x, y1: padre.y, x2: x, y2: y }));
    }
    const grupo = svg("g", { class: nivel === 0 ? "raiz" : "" });
    grupo.append(svg("circle", { cx: x, cy: y, r: 19 }));
    const texto = svg("text", { x, y });
    texto.textContent = nodo.c;
    grupo.append(texto);
    nodos.append(grupo);
    poner(nodo.i, nivel + 1, posicion * 2, { x, y });
    poner(nodo.d, nivel + 1, posicion * 2 + 1, { x, y });
  };
  poner(raiz, 0, 0, null);
  lienzo.append(lineas, nodos);
  return lienzo;
}

async function medidas() {
  try {
    const r = await pedir("/api/estructuras/bst/medidas");
    const bloque = crear("div", "resultado-bloque");
    const kpis = crear("div", "kpis");
    kpis.append(
      tarjetaKpi("layers", "Nodos", r.nodos, 0, "", "contarNodos() recorre el árbol con una pila"),
      tarjetaKpi("git-fork", "Altura", r.altura, 0, "", "raíz sola = 0; un árbol balanceado tendría " + r.alturaIdeal),
      tarjetaKpi("gauge", "Promedio de comparaciones", r.promedio, 2, "", "para encontrar una clave que existe")
    );
    const arbol = crear("div", "tarjeta");
    const titulo = crear("div", "panel-titulo");
    titulo.append(icono("git-fork"), crear("span", "", "Primeros 4 niveles (los puntos indican que el árbol sigue)"));
    arbol.append(titulo, dibujarArbol(r.niveles, 4));
    bloque.append(kpis, arbol);
    $("estMedidasResultado").replaceChildren(bloque);
  } catch (error) {
    toast(error.message, "error");
  }
}

async function recorrer() {
  const limite = Number($("estLimite").value) || 24;
  try {
    const r = await pedir("/api/estructuras/bst/recorrido?tipo=" + encodeURIComponent(estado.recorrido) + "&limite=" + limite);
    const caja = $("estRecorridoChips");
    caja.replaceChildren(...r.codigos.map((codigo, indice) => {
      const chip = crear("span");
      chip.append(crear("small", "", String(indice + 1)), document.createTextNode(codigo));
      return chip;
    }));
    $("estRecorridoNota").textContent = r.tipo + ": primeros " + r.codigos.length + " de " + formatoNumero(r.nodos) + " nodos, en el orden en que se visitan.";
    if (!sinMovimiento()) {
      animate(caja.children, { opacity: { from: 0, to: 1 }, scale: { from: 0.6, to: 1 }, delay: stagger(40), duration: 260, ease: "outBack" });
    }
  } catch (error) {
    toast(error.message, "error");
  }
}

function dibujarCadena(ids) {
  const lienzo = svg("svg", { viewBox: "0 0 420 290", class: "arbol-svg cadena-svg", role: "img", "aria-label": "BST degenerado en cadena" });
  const lineas = svg("g");
  const nodos = svg("g");
  ids.forEach((id, indice) => {
    const x = 40 + indice * 62;
    const y = 30 + indice * 42;
    if (indice > 0) {
      lineas.append(svg("line", { x1: x - 62, y1: y - 42, x2: x, y2: y }));
    }
    const grupo = svg("g");
    grupo.append(svg("circle", { cx: x, cy: y, r: 19 }));
    const texto = svg("text", { x, y });
    texto.textContent = id;
    grupo.append(texto);
    nodos.append(grupo);
  });
  const resto = svg("text", { x: 300, y: 280, class: "oculto" });
  resto.textContent = "… sigue en línea recta";
  lienzo.append(lineas, nodos, resto);
  return lienzo;
}

function barra(etiqueta, valor, maximo, variable) {
  const fila = crear("div", "barra-fila");
  const pista = crear("div", "barra-pista");
  const relleno = crear("div", "barra-relleno");
  relleno.style.background = color(variable);
  pista.append(relleno);
  fila.append(crear("span", "", etiqueta), pista, crear("b", "", formatoNumero(valor)));
  requestAnimationFrame(() => requestAnimationFrame(() => {
    relleno.style.width = Math.max(1, (valor * 100) / maximo) + "%";
  }));
  return fila;
}

async function desbalance() {
  try {
    const r = await pedir("/api/estructuras/desbalance", { metodo: "POST" });
    const izquierda = crear("div", "tarjeta");
    const t1 = crear("div", "panel-titulo");
    t1.append(icono("triangle-alert"), crear("span", "", "BST por ID ascendente (1.500 aeropuertos)"));
    izquierda.append(t1, dibujarCadena(r.porId.cadena), crear("p", "nota", "Cada ID es mayor que el anterior, así que siempre se cuelga a la derecha: el árbol se vuelve una lista. Buscar el último cuesta " + formatoNumero(r.porId.comparacionesUltimo) + " comparaciones."));
    const derecha = crear("div", "tarjeta");
    const t2 = crear("div", "panel-titulo");
    t2.append(icono("git-fork"), crear("span", "", "BST global por IATA (" + formatoNumero(r.porIata.nodos) + " aeropuertos)"));
    derecha.append(t2, dibujarArbol(r.porIata.niveles, 3), crear("p", "nota", "Los códigos IATA llegan desordenados, así que el árbol se ramifica. Promedio: " + formatoNumero(r.porIata.promedio, 2) + " comparaciones por búsqueda."));
    const rejilla = crear("div", "rejilla-2");
    rejilla.append(izquierda, derecha);
    const alturas = crear("div", "tarjeta");
    const t3 = crear("div", "panel-titulo");
    t3.append(icono("gauge"), crear("span", "", "Altura (raíz sola = 0)"));
    const maximo = r.porId.altura;
    const barras = crear("div", "barras-altura");
    barras.append(
      barra("Por ID, 1.500 claves ordenadas", r.porId.altura, maximo, "--error"),
      barra("Ideal para 1.500 (balanceado)", r.porId.alturaIdeal, maximo, "--exito"),
      barra("Por IATA, 6.071 claves", r.porIata.altura, maximo, "--acento"),
      barra("Ideal para 6.071 (balanceado)", r.porIata.alturaIdeal, maximo, "--exito")
    );
    alturas.append(t3, barras, crear("p", "nota", "Por eso insertar, buscar, eliminar y calcular la altura son iterativos: con los 7.698 IDs la recursión llegaría a 7.697 llamadas."));
    const bloque = crear("div", "resultado-bloque");
    bloque.append(rejilla, alturas);
    $("estDesbalanceResultado").replaceChildren(bloque);
  } catch (error) {
    toast(error.message, "error");
  }
}

export function iniciarEstructuras() {
  document.querySelectorAll("#estPestanas button").forEach((boton) => boton.addEventListener("click", () => mostrarSub(boton.dataset.sub)));
  segmentado($("estAlgoritmo"), [{ valor: "Burbuja", texto: "Burbuja" }, { valor: "Inserción", texto: "Inserción" }], estado.algoritmo, (valor) => { estado.algoritmo = valor; });
  segmentado($("visAlgoritmo"), [{ valor: "Burbuja", texto: "Burbuja" }, { valor: "Inserción", texto: "Inserción" }], vis.algoritmo, (valor) => {
    vis.algoritmo = valor;
    reiniciarVisualizador();
  });
  segmentado($("visCantidad"), [{ valor: 12, texto: "12" }, { valor: 20, texto: "20" }], vis.cantidad, (valor) => {
    vis.cantidad = valor;
    nuevaMuestra();
  });
  segmentado($("estTipoRecorrido"), ["Inorden", "Preorden", "Postorden"].map((tipo) => ({ valor: tipo, texto: tipo })), estado.recorrido, (valor) => { estado.recorrido = valor; });
  $("estPreparar").addEventListener("click", prepararDatos);
  $("estOrdenar").addEventListener("click", ordenar);
  $("estComparar").addEventListener("click", comparar);
  $("visNueva").addEventListener("click", nuevaMuestra);
  $("visCampo").addEventListener("change", () => {
    vis.campo = $("visCampo").value === "Pais" ? "Pais" : $("visCampo").value;
    cargarMuestra();
  });
  $("visReproducir").addEventListener("click", reproducir);
  $("visPaso").addEventListener("click", unPaso);
  $("visReiniciar").addEventListener("click", reiniciarVisualizador);
  $("visVelocidad").addEventListener("input", cambiarVelocidad);
  $("estBuscar").addEventListener("click", buscar);
  $("estIata").addEventListener("keydown", (evento) => {
    if (evento.key === "Enter") {
      buscar();
    }
  });
  $("estConstruir").addEventListener("click", construir);
  document.querySelectorAll("#vista-estructuras [data-operacion]").forEach((boton) => boton.addEventListener("click", () => operar(boton.dataset.operacion)));
  $("estMedidas").addEventListener("click", medidas);
  $("estRecorrer").addEventListener("click", recorrer);
  $("estDesbalance").addEventListener("click", desbalance);
  window.addEventListener("resize", posicionarCajas);
  document.addEventListener("etl-actualizado", refrescarEstado);
  document.addEventListener("vista", (evento) => {
    if (evento.detail === "estructuras") {
      refrescarEstado();
      posicionarCajas();
    }
  });
  habilitarSegunEstado();
  refrescarEstado();
}
