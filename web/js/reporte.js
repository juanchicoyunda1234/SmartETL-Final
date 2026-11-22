import { pedir } from "/web/js/api.js";
import { crear, toast, formatoNumero, color } from "/web/js/interfaz.js";
import { svgEstatico, textoLegible } from "/web/js/resultadoIa.js";

const $ = (id) => document.getElementById(id);
let armando = false;

function hoja() {
  return $("reporteHoja");
}

function tono(variable) {
  return color(variable, hoja());
}

function logo(ruta, texto, clase) {
  const imagen = crear("img", clase);
  imagen.src = ruta;
  imagen.alt = texto;
  return imagen;
}

function membrete() {
  const caja = crear("div", "membrete");
  const textos = crear("div", "membrete-texto");
  textos.append(crear("b", "", "Universidad Técnica de Ambato"), crear("span", "", "FISEI · Estructura de Datos · SmartETL-DS"));
  caja.append(logo("/recursos/logos/uta.png", "Escudo de la UTA", "membrete-uta"), textos,
    logo("/recursos/logos/fisei.png", "Logo de la FISEI", "membrete-fisei"));
  return caja;
}

function seccion(titulo, nuevaPagina) {
  const caja = crear("section", "reporte-seccion" + (nuevaPagina ? " nueva-pagina" : ""));
  if (nuevaPagina) {
    caja.append(membrete());
  }
  caja.append(crear("h2", "", titulo));
  return caja;
}

function noEjecutado(texto) {
  return crear("p", "no-ejecutado", "No ejecutado en esta sesión" + (texto ? ": " + texto : "."));
}

function bloque(titulo, ...hijos) {
  const caja = crear("div", "reporte-bloque");
  if (titulo) {
    caja.append(crear("h3", "", titulo));
  }
  caja.append(...hijos);
  return caja;
}

function kpis(lista) {
  const caja = crear("div", "reporte-kpis");
  lista.forEach(([etiqueta, valor]) => {
    const kpi = crear("div", "reporte-kpi");
    kpi.append(crear("small", "", etiqueta), crear("b", "", valor));
    caja.append(kpi);
  });
  return caja;
}

function tabla(cabeceras, filas, numericas) {
  const t = crear("table", "reporte-tabla");
  const cabeza = crear("tr");
  cabeceras.forEach((texto) => cabeza.append(crear("th", "", texto)));
  const thead = crear("thead");
  thead.append(cabeza);
  const cuerpo = crear("tbody");
  filas.forEach((fila) => {
    const tr = crear("tr");
    fila.forEach((valor, i) => tr.append(crear("td", numericas && numericas.includes(i) ? "numero" : "", String(valor))));
    cuerpo.append(tr);
  });
  t.append(thead, cuerpo);
  return t;
}

function n(valor, decimales = 0) {
  return formatoNumero(valor, decimales);
}

function grafico(titulo, configuracion) {
  const figura = crear("figure", "reporte-grafico");
  const lienzo = crear("canvas");
  lienzo.width = 380;
  lienzo.height = 230;
  configuracion.options = Object.assign({ responsive: false, animation: false, devicePixelRatio: 3 }, configuracion.options || {});
  const chart = new window.Chart(lienzo, configuracion);
  const imagen = crear("img");
  imagen.src = chart.toBase64Image("image/png", 1);
  imagen.alt = titulo;
  chart.destroy();
  figura.append(imagen, crear("figcaption", "", titulo));
  return figura;
}

function ejes(tituloY, logaritmico) {
  return {
    x: { ticks: { color: tono("--texto-suave") }, grid: { display: false } },
    y: Object.assign({ beginAtZero: !logaritmico, ticks: { color: tono("--texto-suave") }, grid: { color: tono("--borde") },
      title: { display: true, text: tituloY, color: tono("--texto-suave") } }, logaritmico ? { type: "logarithmic", min: 1 } : {}),
  };
}

function seccionEtl(r) {
  const caja = seccion("1. Proceso ETL", true);
  const etl = r.etl;
  if (!etl) {
    caja.append(noEjecutado("ejecuta el ETL en la pestaña ETL"));
    return caja;
  }
  caja.append(bloque(null, crear("p", "", "Modo " + (etl.modo === "REAL" ? "real (archivos de OpenFlights)" : "demostración") + "."),
    kpis([["Procesados", n(etl.procesados)], ["Válidos", n(etl.validos)], ["Errores", n(etl.errores)], ["Tiempo", n(etl.ms) + " ms"]])));
  caja.append(bloque("Por fuente", tabla(["Fuente", "Procesados", "Válidos", "Inválidos"],
    etl.fuentes.map((f) => [f.fuente, n(f.procesados), n(f.validos), n(f.invalidos)]), [1, 2, 3])));
  const tipos = (etl.tipos || []).filter((t) => t.total > 0);
  const graficos = crear("div", "reporte-graficos");
  graficos.append(grafico("Válidos e inválidos por fuente", {
    type: "bar",
    data: { labels: etl.fuentes.map((f) => f.fuente), datasets: [
      { label: "Válidos", data: etl.fuentes.map((f) => f.validos), backgroundColor: tono("--exito") },
      { label: "Inválidos", data: etl.fuentes.map((f) => f.invalidos), backgroundColor: tono("--error") }] },
    options: { plugins: { legend: { labels: { color: tono("--texto") } } }, scales: ejes("registros") },
  }));
  if (tipos.length) {
    const paleta = ["--error", "--aviso", "--morado", "--azul", "--exito", "--acento", "--texto-tenue"];
    graficos.append(grafico("Errores por tipo", {
      type: "doughnut",
      data: { labels: tipos.map((t) => t.tipo), datasets: [{ data: tipos.map((t) => t.total), backgroundColor: tipos.map((t, i) => tono(paleta[i % paleta.length])), borderColor: tono("--superficie") }] },
      options: { plugins: { legend: { position: "right", labels: { color: tono("--texto"), boxWidth: 12 } } } },
    }));
  }
  caja.append(bloque("Gráficos", graficos));
  return caja;
}

function seccionEstructuras(r) {
  const caja = seccion("2. Estructuras de datos", true);
  const b = r.busqueda;
  if (b) {
    caja.append(bloque("Búsqueda de " + b.iata + (b.aeropuerto ? " · " + b.aeropuerto.nombre : " (no existe)"),
      tabla(["Estructura", "Comparaciones", "Tiempo (ms)", "Detalle"], [
        ["Secuencial (lista enlazada)", n(b.secuencial.comparaciones), n(b.secuencial.ms, 3), b.secuencial.posicion > 0 ? "posición " + n(b.secuencial.posicion) : "recorre todo"],
        ["Binaria (lista secuencial ordenada)", n(b.binaria.comparaciones), n(b.binaria.ms, 3), "mitades sucesivas"],
        ["BST por IATA", n(b.bst.comparaciones), n(b.bst.ms, 3), b.bst.profundidad >= 0 ? "profundidad " + b.bst.profundidad : "hijo vacío"],
      ], [1, 2]),
      grafico("Comparaciones por estructura (escala logarítmica)", {
        type: "bar",
        data: { labels: ["Secuencial", "Binaria", "BST"], datasets: [{ data: [b.secuencial.comparaciones, b.binaria.comparaciones, b.bst.comparaciones], backgroundColor: [tono("--error"), tono("--acento"), tono("--exito")] }] },
        options: { plugins: { legend: { display: false } }, scales: ejes("comparaciones", true) },
      })));
  } else {
    caja.append(bloque("Búsquedas", noEjecutado("usa Estructuras › Búsquedas")));
  }
  const c = r.comparacionOrdenamiento;
  const o = r.ordenamiento;
  if (c) {
    const menos = 100 - (c.insercion.comparaciones * 100) / c.burbuja.comparaciones;
    caja.append(bloque("Burbuja vs inserción por " + c.campo,
      tabla(["Algoritmo", "Comparaciones", "Movimientos", "Tiempo (ms)"], [
        ["Burbuja", n(c.burbuja.comparaciones), n(c.burbuja.movimientos), n(c.burbuja.ms, 1)],
        ["Inserción", n(c.insercion.comparaciones), n(c.insercion.movimientos), n(c.insercion.ms, 1)],
      ], [1, 2, 3]),
      crear("p", "", "Inserción hace " + n(menos, 1) + " % menos comparaciones sobre " + n(c.burbuja.elementos) + " aeropuertos.")));
  } else if (o) {
    caja.append(bloque("Ordenamiento", crear("p", "", o.algoritmo + " por " + o.campo + ": " + n(o.comparaciones) + " comparaciones, " + n(o.movimientos) + " " + o.nombreMovimientos + ", " + n(o.ms, 1) + " ms.")));
  } else {
    caja.append(bloque("Ordenamiento", noEjecutado("usa Estructuras › Ordenamiento")));
  }
  const bst = r.bst;
  caja.append(bloque("Altura del BST por IATA", bst
    ? kpis([["Nodos", n(bst.nodos)], ["Altura", n(bst.altura)], ["Altura ideal", n(bst.alturaIdeal)], ["Promedio comp.", n(bst.promedio, 2)]])
    : noEjecutado("usa Estructuras › BST › Altura y nodos")));
  const d = r.desbalance;
  caja.append(bloque("Desbalance", d
    ? tabla(["Árbol", "Nodos", "Altura", "Altura ideal"], [
      ["Por ID ascendente (claves ordenadas)", n(d.porId.nodos), n(d.porId.altura), n(d.porId.alturaIdeal)],
      ["Global por IATA", n(d.porIata.nodos), n(d.porIata.altura), n(d.porIata.alturaIdeal)],
    ], [1, 2, 3])
    : noEjecutado("usa Estructuras › Desbalance")));
  return caja;
}

function recorrido(nombre, r) {
  if (!r) {
    return bloque(nombre, noEjecutado("usa Grafo › BFS y DFS"));
  }
  const partes = [kpis([["Origen", r.origen], ["Visitados", n(r.cantidad)], [r.algoritmo === "BFS" ? "Niveles" : "Profundidad", n(r.nivelMaximo)], ["Tiempo", n(r.ms, 1) + " ms"]]),
    crear("p", "", r.fronteras ? "Cruzando fronteras (rutas externas)." : "Solo rutas internas del país.")];
  if (r.algoritmo === "BFS") {
    partes.push(grafico("Aeropuertos por nivel del BFS desde " + r.origen, {
      type: "bar",
      data: { labels: r.niveles.map((x) => "Nivel " + x.nivel), datasets: [{ data: r.niveles.map((x) => x.cantidad), backgroundColor: tono("--acento") }] },
      options: { plugins: { legend: { display: false } }, scales: ejes("aeropuertos") },
    }));
  } else {
    partes.push(crear("p", "reporte-cadena", r.nodos.slice(0, 30).map((v) => v.id).join(" → ") + (r.cantidad > 30 ? " → …" : "")));
  }
  return bloque(nombre + " desde " + r.origen, ...partes);
}

function seccionGrafo(r) {
  const caja = seccion("3. Grafo de rutas aéreas", true);
  if (r.grafos) {
    caja.append(bloque("Todos los países", kpis([["Países", n(r.grafos.paises)], ["Vértices", n(r.grafos.vertices)], ["Aristas", n(r.grafos.aristas)], ["Tiempo", n(r.grafos.ms, 1) + " ms"]])));
  }
  if (r.construidos) {
    caja.append(bloque("Países construidos", tabla(["País", "Aeropuertos", "Rutas internas", "Salen", "Llegan"],
      r.construidos.paises.map((p) => [p.nombre, n(p.vertices), n(p.internas), n(p.salidas), n(p.llegadas)]), [1, 2, 3, 4])));
  }
  if (!r.grafos && !r.construidos) {
    caja.append(bloque("Países", noEjecutado("usa Grafo › Construir")));
  }
  caja.append(recorrido("BFS", r.bfs), recorrido("DFS", r.dfs));
  caja.append(bloque("Rutas consultadas", r.rutas.length
    ? tabla(["De", "A", "Vuelos", "Ruta"], r.rutas.map((x) => [x.desde, x.hasta, x.existe ? String(x.vuelos) : "—", x.existe ? x.ruta.map((a) => a.iata).join(" ✈ ") : "no existe"]), [2])
    : noEjecutado("usa Grafo › Ruta y ciclo")));
  const c = r.ciclo;
  caja.append(bloque("Ciclo", c
    ? crear("p", "reporte-cadena", c.existe ? c.pais + ": " + c.ciclo.map((a) => a.iata).join(" → ") + " (longitud " + c.longitud + ")" : "Sin ciclos en " + c.pais)
    : noEjecutado("usa Grafo › Detectar ciclo")));
  return caja;
}

function chip(texto, clase) {
  return crear("span", "chip " + clase, texto);
}

async function tarjetaIa(r) {
  const caja = crear("div", "reporte-ia");
  caja.append(crear("h3", "", r.titulo));
  const chips = crear("div", "resultado-ia-chips");
  chips.append(chip(r.usoApi ? "IA · " + r.proveedor + (r.modelo ? " (" + r.modelo + ")" : "") : "RESPALDO LOCAL", r.usoApi ? "chip-exito" : "chip-aviso"));
  if (r.esDiagrama) {
    chips.append(chip(r.coherente ? "✓ coherente" : r.coherencia.replace("Coherencia: ", ""), r.coherente ? "chip-exito" : "chip-aviso"));
  }
  if (r.evidencia) {
    chips.append(chip("Evidencia: " + r.evidencia, "chip-neutro"));
  }
  caja.append(chips);
  if (r.esDiagrama) {
    const lienzo = crear("div", "reporte-diagrama");
    try {
      lienzo.innerHTML = await svgEstatico(r.contenido, hoja());
      const svg = lienzo.querySelector("svg");
      svg.removeAttribute("style");
      svg.removeAttribute("height");
    } catch (error) {
      lienzo.append(crear("pre", "texto-mono", r.contenido));
    }
    caja.append(lienzo);
    if (r.resumen) {
      caja.append(crear("h4", "", "Resumen (" + (r.resumen.usoApi ? "IA" : "respaldo local") + ")"), textoLegible(r.resumen.contenido));
    }
  } else {
    caja.append(textoLegible(r.contenido));
  }
  return caja;
}

async function seccionIa(r) {
  const caja = seccion("4. Inteligencia artificial", true);
  const ultimos = new Map();
  r.ia.forEach((item) => ultimos.set(item.resultado.funcion, item.resultado));
  if (ultimos.size === 0) {
    caja.append(noEjecutado("genera flujogramas o explicaciones en la pestaña IA"));
    return caja;
  }
  caja.append(crear("p", "", "Se incluye el último resultado de cada función (" + ultimos.size + "). Los diagramas salen de eventos reales de la ejecución."));
  for (const resultado of ultimos.values()) {
    caja.append(await tarjetaIa(resultado));
  }
  return caja;
}

async function armar() {
  if (armando) {
    return;
  }
  armando = true;
  $("reporteEstado").textContent = "Armando el reporte…";
  try {
    const [r, estado] = await Promise.all([pedir("/api/reporte"), pedir("/api/estado")]);
    $("reporteFecha").textContent = new Date(r.generado).toLocaleString("es-EC", { dateStyle: "long", timeStyle: "short" });
    $("reporteCarpeta").textContent = estado.carpeta;
    const cuerpo = crear("div");
    cuerpo.append(seccionEtl(r), seccionEstructuras(r), seccionGrafo(r), await seccionIa(r));
    $("reporteCuerpo").replaceChildren(cuerpo);
    const hechas = [r.etl, r.busqueda || r.ordenamiento || r.comparacionOrdenamiento || r.bst || r.desbalance, r.grafos || r.construidos || r.bfs || r.dfs || r.rutas.length || r.ciclo, r.ia.length].filter(Boolean).length;
    $("reporteEstado").textContent = hechas + " de 4 secciones con datos de esta sesión. Lo demás aparece como «No ejecutado en esta sesión».";
  } catch (error) {
    toast(error.message, "error");
    $("reporteEstado").textContent = "No se pudo armar el reporte.";
  } finally {
    armando = false;
  }
}

export function iniciarReporte() {
  $("reporteActualizar").addEventListener("click", armar);
  $("reporteImprimir").addEventListener("click", async () => {
    await armar();
    window.print();
  });
  document.addEventListener("vista", (evento) => {
    if (evento.detail === "reporte") {
      armar();
    }
  });
  if (location.hash === "#reporte") {
    armar();
  }
}
