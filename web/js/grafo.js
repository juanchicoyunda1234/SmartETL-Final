import { animate, stagger } from "/recursos/anime.esm.min.js";
import { pedir } from "/web/js/api.js";
import { ejecutarTarea, crear, icono, toast, formatoNumero, tarjetaKpi, segmentado, color, cargarScript } from "/web/js/interfaz.js";
import { ETAPAS_ETL } from "/web/js/etl.js";

const $ = (id) => document.getElementById(id);
const NS = "http://www.w3.org/2000/svg";
const COLORES_NIVEL = ["--aviso", "--acento", "--exito", "--morado", "--azul", "--error"];

const estado = { datosListos: false, paises: [], elegidos: new Set(), algoritmo: "BFS" };
const redes = {};
const dibujos = {};
let reloj = null;
let graficoNiveles = null;

function sinMovimiento() {
  return matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function colorNivel(nivel) {
  return color(COLORES_NIVEL[nivel % COLORES_NIVEL.length]);
}

function sinTildes(texto) {
  return texto.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();
}

function iata(id) {
  return $(id).value.trim().toUpperCase();
}

function habilitar() {
  document.querySelectorAll("#vista-grafo [data-requiere]").forEach((boton) => {
    boton.dataset.deshabilitado = estado.datosListos ? "no" : "si";
    boton.disabled = !estado.datosListos;
  });
  $("grafoDatos").hidden = estado.datosListos;
}

function pintarSeleccion() {
  $("grafoSeleccion").textContent = estado.elegidos.size + (estado.elegidos.size === 1 ? " seleccionado" : " seleccionados");
  $("grafoSeleccion").className = "chip " + (estado.elegidos.size ? "chip-acento" : "chip-neutro");
}

function pintarLista() {
  const filtro = sinTildes($("grafoBuscador").value.trim());
  const filas = estado.paises.filter((pais) => sinTildes(pais.nombre).includes(filtro)).map((pais) => {
    const fila = crear("label", "pais-fila");
    const casilla = crear("input");
    casilla.type = "checkbox";
    casilla.checked = estado.elegidos.has(pais.nombre);
    casilla.addEventListener("change", () => {
      if (casilla.checked) {
        estado.elegidos.add(pais.nombre);
      } else {
        estado.elegidos.delete(pais.nombre);
      }
      pintarSeleccion();
    });
    fila.append(casilla, crear("span", "", pais.nombre));
    if (pais.construido) {
      fila.append(crear("small", pais.vertices === 0 ? "sin-rutas" : "", pais.vertices === 0 ? "✓ sin rutas" : "✓ " + formatoNumero(pais.vertices) + " aerop."));
    }
    return fila;
  });
  if (filas.length === 0) {
    filas.push(crear("p", "nota", estado.datosListos ? "Ningún país coincide con la búsqueda." : "Prepara los datos para ver los países."));
  }
  $("grafoLista").replaceChildren(...filas);
  pintarSeleccion();
}

async function cargarPaises() {
  try {
    const r = await pedir("/api/grafo/paises");
    estado.datosListos = r.datosListos;
    estado.paises = r.paises;
    $("grafoPaisesDatalist").replaceChildren(...r.paises.map((pais) => {
      const opcion = crear("option");
      opcion.value = pais.nombre;
      return opcion;
    }));
    habilitar();
    pintarLista();
  } catch (error) {
    toast(error.message, "error");
  }
}

async function prepararDatos() {
  try {
    await ejecutarTarea({ ruta: "/api/etl/ejecutar", cuerpo: { modo: "real" }, titulo: "Preparando los datos (ETL real)", etapas: ETAPAS_ETL });
    document.dispatchEvent(new CustomEvent("etl-actualizado"));
    toast("Datos listos: ya puedes construir los grafos", "exito");
  } catch (error) {
  }
}

function kpis(lista) {
  const caja = crear("div", "kpis");
  caja.append(...lista);
  return caja;
}

async function construir(todos) {
  if (!todos && estado.elegidos.size === 0) {
    toast("Selecciona al menos un país de la lista", "error");
    return;
  }
  try {
    const final = await ejecutarTarea(todos
      ? { ruta: "/api/grafo/construir-todos", cuerpo: {}, titulo: "Construyendo los grafos de todos los países" }
      : { ruta: "/api/grafo/construir", cuerpo: { paises: [...estado.elegidos] }, titulo: "Construyendo " + estado.elegidos.size + " grafos" });
    const r = final.datos;
    const bloque = crear("div", "resultado-bloque");
    if (todos) {
      bloque.append(kpis([
        tarjetaKpi("layers", "Países", r.paises, 0, "", "con al menos un aeropuerto con rutas, de " + r.enArbol + " en el BST"),
        tarjetaKpi("plane", "Vértices", r.vertices, 0, "", "aeropuertos con rutas"),
        tarjetaKpi("route", "Aristas", r.aristas, 0, "", "rutas internas sin repetir"),
        tarjetaKpi("timer", "Tiempo", r.ms, 1, " ms", r.ms < 10000 ? "Cumple: menos de 10 s" : "Supera los 10 s"),
      ]));
    } else {
      bloque.append(kpis([
        tarjetaKpi("layers", "Países", r.cantidad, 0, "", "construidos en esta acción"),
        tarjetaKpi("plane", "Vértices", r.vertices, 0, "", "aeropuertos con rutas"),
        tarjetaKpi("route", "Aristas", r.aristas, 0, "", "rutas internas"),
        tarjetaKpi("timer", "Tiempo", r.ms, 1, " ms", "suma del tiempo de cada país"),
      ]));
      const chips = crear("div", "kpis-externos");
      r.paises.forEach((pais) => {
        const chip = crear("span", "chip chip-neutro", pais.nombre + ": " + pais.vertices + " aeropuertos · " + pais.internas + " internas · " + pais.salidas + " salen");
        chip.title = formatoNumero(pais.ms, 1) + " ms";
        chips.append(chip);
      });
      bloque.append(chips);
    }
    $("grafoKpis").replaceChildren(bloque);
    await cargarPaises();
  } catch (error) {
  }
}

function mostrarSub(nombre) {
  document.querySelectorAll("#grafoPestanas button").forEach((boton) => boton.setAttribute("aria-selected", String(boton.dataset.sub === nombre)));
  document.querySelectorAll("#vista-grafo .subvista").forEach((panel) => panel.classList.toggle("activa", panel.id === "grafo-" + nombre));
  Object.values(redes).forEach((red) => red.redraw());
}

async function vis() {
  await cargarScript("/recursos/vis-network.min.js");
  return window.vis;
}

function opcionesRed(fisica) {
  return {
    autoResize: true,
    interaction: { hover: true, tooltipDelay: 120, navigationButtons: false, zoomView: true },
    physics: fisica === false ? false : { solver: "forceAtlas2Based", forceAtlas2Based: { gravitationalConstant: -60, springLength: 90 }, stabilization: { iterations: 160 } },
    nodes: { shape: "dot", borderWidth: 2, font: { color: color("--texto"), face: "Cascadia Mono, Consolas, monospace", size: 13, strokeWidth: 3, strokeColor: color("--superficie-2") } },
    edges: { color: { color: color("--borde-fuerte"), highlight: color("--acento") }, width: 1.4, smooth: { type: "continuous" } },
  };
}

function nodo(id, etiqueta, fondo, extra) {
  return Object.assign({ id, label: etiqueta, color: { background: fondo, border: fondo, highlight: { background: fondo, border: color("--texto") }, hover: { background: fondo, border: color("--texto") } } }, extra || {});
}

function crearRed(clave, contenedor, nodos, aristas, opciones) {
  if (redes[clave]) {
    redes[clave].destroy();
  }
  contenedor.hidden = false;
  redes[clave] = new window.vis.Network(contenedor, { nodes: nodos, edges: aristas }, opciones);
  return redes[clave];
}

function titulo(aeropuerto) {
  return aeropuerto.iata + " · " + aeropuerto.nombre + " (" + aeropuerto.ciudad + ", " + aeropuerto.pais + ")";
}

function dibujarConexiones(r) {
  const nodos = [nodo(r.aeropuerto.iata, r.aeropuerto.iata, color("--aviso"), { size: 26, title: titulo(r.aeropuerto), font: { size: 16 } })];
  const aristas = [];
  r.internas.forEach((a) => {
    nodos.push(nodo(a.iata, a.iata, color("--acento"), { size: 14, title: titulo(a) }));
    aristas.push({ from: r.aeropuerto.iata, to: a.iata });
  });
  r.externos.forEach((grupo, indice) => {
    const id = "pais-" + indice;
    nodos.push(nodo(id, grupo.pais + " (" + grupo.conteo + ")", color("--morado"), { shape: "square", size: 8 + Math.min(18, grupo.conteo * 2), title: grupo.codigos }));
    aristas.push({ from: r.aeropuerto.iata, to: id, dashes: true, color: { color: color("--morado") } });
  });
  crearRed("conexiones", $("grafoRedConexiones"), nodos, aristas, opcionesRed());
  $("grafoLeyendaConexiones").hidden = false;
}

async function verConexiones() {
  try {
    const r = await pedir("/api/grafo/conexiones?iata=" + encodeURIComponent(iata("grafoIata")));
    await vis();
    const info = crear("div", "resultado-bloque");
    info.append(kpis([
      tarjetaKpi("plane", "Rutas internas", r.totalInternas, 0, "", "vecinos en el grafo de " + r.aeropuerto.pais),
      tarjetaKpi("route", "Rutas externas", r.totalExternas, 0, "", "hacia " + r.externos.length + " países"),
      tarjetaKpi("arrow-right", "Llegan del exterior", r.llegadas, 0, "", "rutas que entran desde otros países"),
    ]));
    info.append(crear("p", "nota", titulo(r.aeropuerto) + (r.internas.length < r.totalInternas ? ". Se dibujan los primeros " + r.internas.length + " vecinos internos." : "")));
    $("grafoConexionesInfo").replaceChildren(info);
    dibujos.conexiones = () => dibujarConexiones(r);
    dibujos.conexiones();
  } catch (error) {
    toast(error.message, "error");
  }
}

function leyendaNiveles(r) {
  const caja = $("grafoLeyendaRecorrido");
  if (r.algoritmo !== "BFS") {
    caja.replaceChildren(crear("span", "", "Los números indican el orden en que la Pila los visitó; el color, la profundidad."));
    return;
  }
  caja.replaceChildren(...r.niveles.slice(0, 7).map((n) => {
    const item = crear("span");
    const punto = crear("i");
    punto.style.setProperty("--color", colorNivel(n.nivel));
    item.append(punto, document.createTextNode("Nivel " + n.nivel + ": " + formatoNumero(n.cantidad)));
    return item;
  }));
}

function animarRecorrido(r) {
  clearInterval(reloj);
  const nodos = new window.vis.DataSet();
  const aristas = new window.vis.DataSet();
  const opciones = opcionesRed();
  opciones.physics.stabilization = false;
  crearRed("recorrido", $("grafoRedRecorrido"), nodos, aristas, opciones);
  const agregar = (indice) => {
    const v = r.nodos[indice];
    const etiqueta = r.algoritmo === "BFS" ? v.id : indice + 1 + "·" + v.id;
    nodos.add(nodo(v.id, etiqueta, colorNivel(v.nivel), { size: indice === 0 ? 24 : 13, title: v.id + " · " + v.nombre + " (" + v.ciudad + ", " + v.pais + ") · nivel " + v.nivel + " · visita " + (indice + 1) }));
    if (v.padre && nodos.get(v.padre)) {
      aristas.add({ from: v.padre, to: v.id, arrows: { to: { enabled: true, scaleFactor: 0.5 } } });
    }
  };
  if (sinMovimiento()) {
    r.nodos.forEach((v, indice) => agregar(indice));
    return;
  }
  let indice = 0;
  const paso = Math.max(12, Math.min(60, 4000 / r.nodos.length));
  reloj = setInterval(() => {
    if (indice >= r.nodos.length) {
      clearInterval(reloj);
      redes.recorrido.fit({ animation: { duration: 500 } });
      return;
    }
    agregar(indice);
    indice++;
  }, paso);
}

function graficarNiveles(r) {
  if (graficoNiveles) {
    graficoNiveles.destroy();
    graficoNiveles = null;
  }
  $("grafoNivelesCaja").hidden = r.algoritmo !== "BFS";
  if (r.algoritmo !== "BFS") {
    return;
  }
  graficoNiveles = new window.Chart($("grafoGraficoNiveles"), {
    type: "bar",
    data: {
      labels: r.niveles.map((n) => "Nivel " + n.nivel),
      datasets: [{ label: "Aeropuertos", data: r.niveles.map((n) => n.cantidad), backgroundColor: r.niveles.map((n) => colorNivel(n.nivel)), borderRadius: 6 }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      animation: sinMovimiento() ? false : { duration: 700 },
      plugins: { legend: { display: false }, tooltip: { callbacks: { afterLabel: (c) => r.niveles[c.dataIndex].paises + " países" } } },
      scales: {
        x: { ticks: { color: color("--texto-suave") }, grid: { display: false } },
        y: { beginAtZero: true, title: { display: true, text: "aeropuertos visitados", color: color("--texto-suave") }, ticks: { color: color("--texto-suave") }, grid: { color: color("--borde") } },
      },
    },
  });
}

function ordenVisita(r) {
  const caja = $("grafoOrdenVisita");
  if (r.algoritmo !== "DFS") {
    caja.replaceChildren();
    return;
  }
  caja.replaceChildren(...r.nodos.slice(0, 60).map((v, indice) => {
    const chip = crear("span");
    chip.append(crear("small", "", String(indice + 1)), document.createTextNode(v.id));
    return chip;
  }));
  if (!sinMovimiento()) {
    animate(caja.children, { opacity: { from: 0, to: 1 }, delay: stagger(25), duration: 220 });
  }
}

function enteroOpcional(id) {
  const texto = $(id).value.trim();
  return texto === "" ? -1 : Math.max(0, Number(texto));
}

async function recorrer() {
  const origen = iata("grafoOrigen");
  try {
    await vis();
    const final = await ejecutarTarea({
      ruta: "/api/grafo/recorrido",
      cuerpo: { algoritmo: estado.algoritmo, origen, nivelMaximo: enteroOpcional("grafoNivelMaximo"), nivelDiagrama: Math.max(1, Number($("grafoNivelDiagrama").value) || 1), fronteras: $("grafoFronteras").checked },
      titulo: estado.algoritmo + " desde " + origen,
    });
    const r = final.datos;
    const info = crear("div", "resultado-bloque");
    info.append(kpis([
      tarjetaKpi("plane", "Visitados", r.cantidad, 0, "", r.fronteras ? "cruzando fronteras" : "solo rutas internas"),
      tarjetaKpi("layers", r.algoritmo === "BFS" ? "Niveles" : "Profundidad máxima", r.nivelMaximo, 0, "", r.algoritmo === "BFS" ? "capas desde " + r.origen : "desde " + r.origen),
      tarjetaKpi("timer", "Tiempo", r.ms, 1, " ms", formatoNumero(r.eventos) + " eventos registrados para la pestaña IA"),
    ]));
    if (r.mostrados < r.cantidad) {
      info.append(crear("p", "nota", "Se dibujan los primeros " + r.mostrados + " aeropuertos en el orden de visita; el resto se cuenta en los números."));
    }
    $("grafoRecorridoInfo").replaceChildren(info);
    $("grafoRecorridoCabecera").hidden = false;
    leyendaNiveles(r);
    graficarNiveles(r);
    ordenVisita(r);
    dibujos.recorrido = () => {
      animarRecorrido(r);
      graficarNiveles(r);
    };
    animarRecorrido(r);
  } catch (error) {
  }
}

function dibujarRuta(r) {
  const enRuta = new Set(r.ruta.map((a) => a.iata));
  const nodos = new Map();
  const aristas = [];
  r.ruta.forEach((a, indice) => {
    const extremo = indice === 0 || indice === r.ruta.length - 1;
    nodos.set(a.iata, nodo(a.iata, a.iata, extremo ? color("--exito") : color("--acento"), { size: extremo ? 22 : 17, title: titulo(a), font: { size: 15 } }));
    if (indice > 0) {
      aristas.push({ from: r.ruta[indice - 1].iata, to: a.iata, width: 4, color: { color: color("--exito") }, arrows: { to: { enabled: true, scaleFactor: 0.7 } } });
    }
    a.vecinos.filter((codigo) => !enRuta.has(codigo)).forEach((codigo) => {
      if (!nodos.has(codigo)) {
        nodos.set(codigo, nodo(codigo, codigo, color("--borde-fuerte"), { size: 9, font: { size: 11, color: color("--texto-tenue") } }));
      }
      aristas.push({ from: a.iata, to: codigo, color: { color: color("--borde"), opacity: 0.8 } });
    });
  });
  crearRed("ruta", $("grafoRedRuta"), [...nodos.values()], aristas, opcionesRed());
}

async function buscarRuta() {
  try {
    const r = await pedir("/api/grafo/ruta", { metodo: "POST", cuerpo: { desde: iata("grafoDesde"), hasta: iata("grafoHasta") } });
    if (!r.existe) {
      $("grafoRutaInfo").replaceChildren(crear("p", "nota", "No existe ruta de " + r.desde + " a " + r.hasta + "."));
      $("grafoRedRuta").hidden = true;
      return;
    }
    await vis();
    const cadena = crear("div", "cadena-vuelos");
    r.ruta.forEach((a, indice) => {
      if (indice > 0) {
        cadena.append(crear("span", "avion", "✈"));
      }
      const escala = crear("div", "escala" + (indice === 0 || indice === r.ruta.length - 1 ? " extremo" : ""));
      escala.title = titulo(a);
      escala.append(crear("b", "", a.iata), crear("small", "", a.ciudad + ", " + a.pais));
      cadena.append(escala);
    });
    cadena.append(crear("span", "chip chip-exito", r.vuelos + (r.vuelos === 1 ? " vuelo" : " vuelos")));
    if (!sinMovimiento()) {
      animate(cadena.children, { opacity: { from: 0, to: 1 }, translateX: { from: -10, to: 0 }, delay: stagger(90), duration: 300, ease: "outCubic" });
    }
    $("grafoRutaInfo").replaceChildren(cadena, crear("p", "nota", "BFS con registro de padre: puede haber otras rutas con la misma cantidad de vuelos; se muestra la primera que encuentra."));
    dibujos.ruta = () => dibujarRuta(r);
    dibujos.ruta();
  } catch (error) {
    toast(error.message, "error");
  }
}

function svg(etiqueta, atributos) {
  const elemento = document.createElementNS(NS, etiqueta);
  Object.entries(atributos || {}).forEach(([clave, valor]) => elemento.setAttribute(clave, valor));
  return elemento;
}

function dibujarAnillo(codigos) {
  const n = codigos.length;
  const centro = 170;
  const radio = n <= 4 ? 105 : 125;
  const lienzo = svg("svg", { viewBox: "0 0 340 340", class: "anillo-svg", role: "img", "aria-label": "Ciclo " + codigos.join(" → ") });
  const definiciones = svg("defs");
  const marcador = svg("marker", { id: "flechaCiclo", viewBox: "0 0 10 10", refX: "9", refY: "5", markerWidth: "7", markerHeight: "7", orient: "auto-start-reverse" });
  marcador.append(svg("path", { d: "M0,0 L10,5 L0,10 z" }));
  definiciones.append(marcador);
  lienzo.append(definiciones);
  const punto = (indice, ajuste) => {
    const angulo = -Math.PI / 2 + (2 * Math.PI * indice) / n + ajuste;
    return [centro + radio * Math.cos(angulo), centro + radio * Math.sin(angulo)];
  };
  const separacion = 22 / radio;
  const arcos = [];
  for (let i = 0; i < n; i++) {
    const [x1, y1] = punto(i, separacion);
    const [x2, y2] = punto(i + 1, -separacion);
    const arco = svg("path", { d: "M" + x1 + "," + y1 + " A" + radio + "," + radio + " 0 0 1 " + x2 + "," + y2, "marker-end": "url(#flechaCiclo)" });
    arcos.push(arco);
    lienzo.append(arco);
  }
  const grupos = codigos.map((codigo, i) => {
    const [x, y] = punto(i, 0);
    const grupo = svg("g");
    grupo.append(svg("circle", { class: "nodo", cx: x, cy: y, r: 21 }));
    const texto = svg("text", { x, y });
    texto.textContent = codigo;
    grupo.append(texto);
    lienzo.append(grupo);
    return grupo;
  });
  if (!sinMovimiento()) {
    arcos.forEach((arco, i) => {
      const largo = arco.getTotalLength ? 400 : 0;
      arco.style.strokeDasharray = largo;
      arco.style.strokeDashoffset = largo;
      animate(arco, { strokeDashoffset: { from: largo, to: 0 }, delay: 250 + i * 260, duration: 260, ease: "linear" });
    });
    animate(grupos, { opacity: { from: 0, to: 1 }, delay: stagger(260), duration: 240 });
  }
  return lienzo;
}

async function detectarCiclo() {
  const pais = $("grafoPaisCiclo").value.trim();
  if (!pais) {
    toast("Escribe el nombre de un país", "error");
    return;
  }
  try {
    const r = await pedir("/api/grafo/ciclo", { metodo: "POST", cuerpo: { pais } });
    if (!r.existe) {
      $("grafoCicloInfo").replaceChildren(crear("p", "nota", "No hay ciclos de longitud 3 o más en las rutas internas de " + r.pais + "."));
      return;
    }
    const codigos = r.ciclo.slice(0, -1).map((a) => a.iata);
    const bloque = crear("div", "resultado-bloque");
    const chips = crear("div", "resultado-ia-chips");
    chips.append(crear("span", "chip chip-acento", "Ciclo de longitud " + r.longitud + " en " + r.pais), crear("span", "chip chip-neutro", r.ciclo.map((a) => a.iata).join(" → ")));
    bloque.append(chips, dibujarAnillo(codigos), crear("p", "nota", "DFS con estados blanco, gris y negro: al volver a un vértice gris se cierra el ciclo. Se ignoran las idas y vueltas A → B → A."));
    $("grafoCicloInfo").replaceChildren(bloque);
    await cargarPaises();
  } catch (error) {
    toast(error.message, "error");
  }
}

function mostrarMarco(ruta, nombre) {
  const marco = $("grafoMarco");
  marco.hidden = false;
  marco.src = ruta;
  $("grafoMarcoCabecera").hidden = false;
  $("grafoMarcoTitulo").textContent = nombre;
  $("grafoMarcoAbrir").href = ruta;
  $("grafoMarcoVolver").hidden = true;
  $("grafoMarcoVolver").dataset.ruta = ruta;
  marco.scrollIntoView({ block: "nearest", behavior: sinMovimiento() ? "auto" : "smooth" });
}

function seguirMarco() {
  const marco = $("grafoMarco");
  try {
    const ruta = marco.contentWindow.location.pathname;
    const enGeneral = ruta.includes("general");
    $("grafoMarcoVolver").hidden = enGeneral || !$("grafoMarcoVolver").dataset.ruta.includes("general");
    $("grafoMarcoAbrir").href = ruta;
    if (ruta.startsWith("/diagramas/grafo_")) {
      $("grafoMarcoTitulo").textContent = "Grafo de " + decodeURIComponent(ruta.slice(17, -5)).replace(/_/g, " ");
    } else if (enGeneral) {
      $("grafoMarcoTitulo").textContent = "Diagrama general: clic en un país para ver su grafo";
    }
  } catch (error) {
  }
}

async function diagramaGeneral() {
  try {
    const final = await ejecutarTarea({ ruta: "/api/grafo/diagrama-general", cuerpo: { nivelDiagrama: Math.max(1, Number($("grafoNivelGeneral").value) || 1) }, titulo: "Generando el diagrama general" });
    mostrarMarco(final.datos.html + "?v=" + Date.now(), "Diagrama general: clic en un país para ver su grafo");
  } catch (error) {
  }
}

async function visor() {
  try {
    const final = await ejecutarTarea({ ruta: "/api/grafo/visor", cuerpo: { origen: iata("grafoOrigen") || "UIO" }, titulo: "Generando el visor interactivo" });
    mostrarMarco(final.datos.html + "?v=" + Date.now(), "Visor interactivo (vis-network)");
  } catch (error) {
  }
}

export function iniciarGrafo() {
  document.querySelectorAll("#grafoPestanas button").forEach((boton) => boton.addEventListener("click", () => mostrarSub(boton.dataset.sub)));
  segmentado($("grafoAlgoritmo"), [{ valor: "BFS", texto: "BFS (Cola)" }, { valor: "DFS", texto: "DFS (Pila)" }], estado.algoritmo, (valor) => { estado.algoritmo = valor; });
  $("grafoBuscador").addEventListener("input", pintarLista);
  $("grafoLimpiar").addEventListener("click", () => {
    estado.elegidos.clear();
    pintarLista();
  });
  $("grafoPreparar").addEventListener("click", prepararDatos);
  $("grafoConstruir").addEventListener("click", () => construir(false));
  $("grafoTodos").addEventListener("click", () => construir(true));
  $("grafoVerConexiones").addEventListener("click", verConexiones);
  $("grafoRecorrer").addEventListener("click", recorrer);
  $("grafoRepetir").addEventListener("click", () => dibujos.recorrido && dibujos.recorrido());
  $("grafoBuscarRuta").addEventListener("click", buscarRuta);
  $("grafoDetectarCiclo").addEventListener("click", detectarCiclo);
  $("grafoGeneral").addEventListener("click", diagramaGeneral);
  $("grafoVisor").addEventListener("click", visor);
  $("grafoMarcoVolver").addEventListener("click", () => mostrarMarco($("grafoMarcoVolver").dataset.ruta, "Diagrama general: clic en un país para ver su grafo"));
  $("grafoMarco").addEventListener("load", seguirMarco);
  [["grafoIata", verConexiones], ["grafoOrigen", recorrer], ["grafoHasta", buscarRuta], ["grafoPaisCiclo", detectarCiclo]].forEach(([id, accion]) => {
    $(id).addEventListener("keydown", (evento) => {
      if (evento.key === "Enter") {
        accion();
      }
    });
  });
  document.addEventListener("etl-actualizado", cargarPaises);
  document.addEventListener("tema", () => Object.values(dibujos).forEach((dibujar) => dibujar()));
  document.addEventListener("vista", (evento) => {
    if (evento.detail === "grafo") {
      cargarPaises();
    }
  });
  habilitar();
  cargarPaises();
}
