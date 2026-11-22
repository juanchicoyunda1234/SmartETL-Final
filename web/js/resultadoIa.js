import { animate, stagger } from "/recursos/anime.esm.min.js";
import { pedir } from "/web/js/api.js";
import { crear, icono, toast, cargarScript, abrirModal, color } from "/web/js/interfaz.js";

const CATEGORIAS = [
  { clase: "azul", variable: "--azul", emoji: "📥", inicio: "extract", palabras: ["extract", "extraer", "encolar", "desencolar", "cola"] },
  { clase: "morado", variable: "--morado", emoji: "🔄", inicio: "transform", palabras: ["transform", "validar", "regla"] },
  { clase: "verde", variable: "--exito", emoji: "✅", inicio: "valido", palabras: ["valido", "validos", "encontrado", "exito"] },
  { clase: "rojo", variable: "--error", emoji: "⚠️", inicio: "error", palabras: ["error", "errores", "descart", "pila", "apilar", "desapilar", "invalido"] },
  { clase: "ambar", variable: "--aviso", emoji: "💾", inicio: "load", palabras: ["load", "cargar", "guardar", "escrib"] },
];
const NODO = /(^|[\s;&>-])([A-Za-z][\w]*)(\s*)(\(\[|\[\[|\[\(|\(\(|\{\{|\[|\(|\{|>)("([^"]*)"|([^\]\)\}"]*))(\]\)|\]\]|\)\]|\)\)|\}\}|\]|\)|\})/g;

let diagramas = 0;
let manoAlzada = leerPreferencia();

function leerPreferencia() {
  try {
    return localStorage.getItem("smartetl-mano-alzada") === "si";
  } catch (error) {
    return false;
  }
}

export function usarManoAlzada(activa) {
  manoAlzada = activa;
  try {
    localStorage.setItem("smartetl-mano-alzada", activa ? "si" : "no");
  } catch (error) {
  }
}

export function manoAlzadaActiva() {
  return manoAlzada;
}

function sinTildes(texto) {
  return texto.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();
}

function sinMovimiento() {
  return matchMedia("(prefers-reduced-motion: reduce)").matches;
}

async function prepararMermaid(base = document.documentElement, animado = true) {
  const tono = (variable) => color(variable, base);
  await cargarScript("/recursos/mermaid.min.js");
  await cargarScript("/recursos/svg-pan-zoom.min.js");
  window.mermaid.initialize({
    startOnLoad: false,
    securityLevel: "strict",
    theme: "base",
    look: manoAlzada && animado ? "handDrawn" : "classic",
    maxTextSize: 5000000,
    maxEdges: 50000,
    flowchart: { htmlLabels: true, curve: "basis" },
    themeVariables: {
      darkMode: base.dataset.tema === "oscuro",
      background: tono("--superficie-2"),
      primaryColor: tono("--superficie"),
      primaryBorderColor: tono("--borde-fuerte"),
      primaryTextColor: tono("--texto"),
      secondaryColor: tono("--superficie-3"),
      tertiaryColor: tono("--superficie-2"),
      lineColor: tono("--texto-tenue"),
      textColor: tono("--texto"),
      mainBkg: tono("--superficie"),
      nodeBorder: tono("--borde-fuerte"),
      clusterBkg: tono("--superficie-3"),
      clusterBorder: tono("--borde"),
      edgeLabelBackground: tono("--superficie"),
      fontFamily: "Segoe UI, system-ui, sans-serif",
      fontSize: "14px",
    },
  });
}

function categoria(etiqueta) {
  const texto = sinTildes(etiqueta);
  return CATEGORIAS.find((c) => c.palabras.some((palabra) => texto.includes(palabra))) || null;
}

function conEmoji(etiqueta, elegida) {
  const limpia = etiqueta.trim();
  if (/^[\u{1F300}-\u{1FAFF}☀-➿✈]/u.test(limpia)) {
    return etiqueta;
  }
  if (/^[A-Z]{3}(\b|$)/.test(limpia)) {
    return "✈ " + etiqueta;
  }
  if (elegida && sinTildes(limpia).startsWith(elegida.inicio)) {
    return elegida.emoji + " " + etiqueta;
  }
  return etiqueta;
}

export function vestirMermaid(codigo, base = document.documentElement) {
  const lineas = codigo.split("\n");
  const primera = lineas.find((linea) => linea.trim() !== "") || "";
  if (!/^\s*(flowchart|graph)\b/.test(primera)) {
    return codigo;
  }
  const clases = new Map();
  const vestido = lineas.map((linea) => {
    if (/^\s*(classDef|class|style|linkStyle|click|%%|subgraph|end\b|flowchart|graph)/.test(linea)) {
      return linea;
    }
    return linea.replace(NODO, (todo, antes, id, espacio, abre, cuerpo, conComillas, sinComillas, cierra) => {
      const etiqueta = conComillas !== undefined ? conComillas : sinComillas || "";
      if (!etiqueta.trim()) {
        return todo;
      }
      const elegida = categoria(etiqueta);
      if (elegida && !clases.has(id)) {
        clases.set(id, elegida.clase);
      }
      const nueva = conEmoji(etiqueta, elegida).replace(/"/g, "'");
      return antes + id + espacio + abre + "\"" + nueva + "\"" + cierra;
    });
  });
  const usadas = new Set(clases.values());
  CATEGORIAS.filter((c) => usadas.has(c.clase)).forEach((c) => {
    const tono = color(c.variable, base);
    vestido.push("    classDef " + c.clase + " fill:" + tono + "26,stroke:" + tono + ",stroke-width:2px,color:" + color("--texto", base));
  });
  clases.forEach((clase, id) => vestido.push("    class " + id + " " + clase));
  return vestido.join("\n");
}

export async function svgEstatico(codigo, base) {
  await prepararMermaid(base, false);
  try {
    return await renderizar(vestirMermaid(codigo, base));
  } catch (error) {
    return renderizar(codigo);
  }
}

async function renderizar(codigo) {
  diagramas++;
  const { svg } = await window.mermaid.render("diagrama-" + diagramas, codigo);
  return svg;
}

export async function dibujarMermaid(lienzo, codigo) {
  await prepararMermaid();
  let svg;
  try {
    svg = await renderizar(vestirMermaid(codigo));
  } catch (error) {
    svg = await renderizar(codigo);
  }
  lienzo.innerHTML = svg;
  const grafico = lienzo.querySelector("svg");
  grafico.removeAttribute("style");
  grafico.setAttribute("width", "100%");
  grafico.setAttribute("height", "100%");
  prepararResaltado(lienzo, grafico);
  if (!sinMovimiento()) {
    animate([...grafico.querySelectorAll(".node")].slice(0, 80), { opacity: { from: 0, to: 1 }, delay: stagger(35), duration: 320, ease: "outCubic" });
  }
  const zoom = window.svgPanZoom(grafico, { fit: true, center: true, minZoom: 0.1, maxZoom: 30, zoomScaleSensitivity: 0.3, dblClickZoomEnabled: false });
  ajustarAlAncho(zoom);
  return zoom;
}

function prepararResaltado(lienzo, grafico) {
  const nodos = new Map();
  grafico.querySelectorAll(".node").forEach((nodo) => {
    const id = nodo.dataset.id || (nodo.id || "").replace(/^flowchart-/, "").replace(/-\d+$/, "");
    nodos.set(id, nodo);
  });
  const aristas = [];
  grafico.querySelectorAll(".flowchart-link").forEach((camino) => {
    const id = camino.dataset.id || camino.id || "";
    for (const desde of nodos.keys()) {
      if (id.startsWith("L_" + desde + "_")) {
        const hasta = id.slice(desde.length + 3).replace(/_\d+$/, "");
        if (nodos.has(hasta)) {
          aristas.push({ camino, desde, hasta });
          break;
        }
      }
    }
  });
  const limpiar = () => {
    lienzo.classList.remove("con-foco");
    grafico.querySelectorAll(".resaltado").forEach((elemento) => elemento.classList.remove("resaltado"));
  };
  nodos.forEach((nodo, id) => {
    nodo.addEventListener("click", (evento) => {
      evento.stopPropagation();
      limpiar();
      lienzo.classList.add("con-foco");
      nodo.classList.add("resaltado");
      aristas.filter((a) => a.desde === id || a.hasta === id).forEach((a) => {
        a.camino.classList.add("resaltado");
        nodos.get(a.desde).classList.add("resaltado");
        nodos.get(a.hasta).classList.add("resaltado");
      });
    });
  });
  grafico.addEventListener("click", limpiar);
}

function ajustarAlAncho(zoom) {
  const tamanos = zoom.getSizes();
  const porAncho = tamanos.width / tamanos.viewBox.width;
  const porAlto = tamanos.height / tamanos.viewBox.height;
  if (porAncho > porAlto * 1.6) {
    zoom.zoom(Math.min(porAncho / porAlto, 2.5) * 0.92);
    const ajustado = zoom.getSizes();
    zoom.pan({ x: (ajustado.width - ajustado.viewBox.width * ajustado.realZoom) / 2, y: 16 });
  }
}

async function copiar(texto) {
  try {
    await navigator.clipboard.writeText(texto);
    toast("Copiado al portapapeles", "exito");
  } catch (falla) {
    toast("No se pudo copiar automáticamente; selecciona el texto y usa Ctrl+C", "error");
  }
}

function botonCopiar(texto) {
  const boton = crear("button", "boton");
  boton.type = "button";
  boton.append(icono("copy"), crear("span", "", "Copiar"));
  boton.addEventListener("click", () => copiar(texto));
  return boton;
}

async function verEvidencia(nombre) {
  try {
    const evidencia = await pedir("/api/evidencias/" + encodeURIComponent(nombre));
    abrirModal("Evidencia: " + nombre, crear("pre", "texto-mono", evidencia.contenido));
  } catch (error) {
    toast(error.message, "error");
  }
}

function chipOrigen(r) {
  const origen = crear("span", "chip " + (r.usoApi ? "chip-exito" : "chip-aviso"));
  origen.append(icono(r.usoApi ? "sparkles" : "server"), crear("span", "", r.usoApi ? "IA · " + r.proveedor + (r.modelo ? " (" + r.modelo + ")" : "") : "RESPALDO LOCAL"));
  return origen;
}

function enlaceEvidencia(nombre) {
  const enlace = crear("button", "enlace");
  enlace.type = "button";
  enlace.append(icono("file-text"), crear("span", "", "Ver evidencia"));
  enlace.addEventListener("click", () => verEvidencia(nombre));
  return enlace;
}

function chipsResultado(r) {
  const chips = crear("div", "resultado-ia-chips");
  chips.append(chipOrigen(r));
  if (r.esDiagrama) {
    const coherencia = crear("span", "chip " + (r.coherente ? "chip-exito" : "chip-aviso"));
    coherencia.append(icono(r.coherente ? "circle-check" : "triangle-alert"), crear("span", "", r.coherente ? "✓ coherente" : r.coherencia.replace("Coherencia: ", "")));
    coherencia.title = r.coherencia;
    chips.append(coherencia);
  }
  if (r.evidencia) {
    chips.append(enlaceEvidencia(r.evidencia));
  }
  return chips;
}

function vistaDiagrama(r) {
  const caja = crear("div");
  const pestanas = crear("div", "subpestanas");
  pestanas.setAttribute("role", "tablist");
  const lienzo = crear("div", "lienzo-diagrama");
  const codigo = crear("div", "codigo-caja");
  codigo.hidden = true;
  codigo.append(crear("pre", "texto-mono", r.contenido), botonCopiar(r.contenido));
  [["Diagrama", lienzo], ["Código", codigo]].forEach(([nombre, panel], indice) => {
    const boton = crear("button", "", nombre);
    boton.type = "button";
    boton.setAttribute("role", "tab");
    boton.setAttribute("aria-selected", String(indice === 0));
    boton.addEventListener("click", () => {
      pestanas.querySelectorAll("button").forEach((otro) => otro.setAttribute("aria-selected", String(otro === boton)));
      lienzo.hidden = panel !== lienzo;
      codigo.hidden = panel !== codigo;
    });
    pestanas.append(boton);
  });
  caja.append(pestanas, lienzo, codigo);
  dibujarMermaid(lienzo, r.contenido).then((zoom) => {
    const acciones = crear("div", "lienzo-acciones");
    [["zoom-in", "Acercar", () => zoom.zoomIn()], ["zoom-out", "Alejar", () => zoom.zoomOut()], ["refresh-cw", "Ajustar", () => { zoom.resize(); zoom.fit(); zoom.center(); }]].forEach(([nombreIcono, etiqueta, accion]) => {
      const boton = crear("button", "boton-icono");
      boton.type = "button";
      boton.title = etiqueta;
      boton.setAttribute("aria-label", etiqueta);
      boton.append(icono(nombreIcono));
      boton.addEventListener("click", accion);
      acciones.append(boton);
    });
    lienzo.append(acciones);
  }).catch((error) => {
    lienzo.replaceChildren(crear("pre", "texto-mono", "No se pudo dibujar el diagrama: " + error.message));
  });
  return caja;
}

function enLinea(texto) {
  const fragmento = document.createDocumentFragment();
  texto.split(/(\*\*[^*]+\*\*|`[^`]+`)/).forEach((parte) => {
    if (parte.startsWith("**") && parte.endsWith("**") && parte.length > 4) {
      fragmento.append(crear("strong", "", parte.slice(2, -2)));
    } else if (parte.startsWith("`") && parte.endsWith("`") && parte.length > 2) {
      fragmento.append(crear("code", "", parte.slice(1, -1)));
    } else if (parte) {
      fragmento.append(document.createTextNode(parte));
    }
  });
  return fragmento;
}

export function textoLegible(contenido) {
  const caja = crear("div", "texto-ia");
  let lista = null;
  let parrafo = [];
  const cerrarParrafo = () => {
    if (parrafo.length) {
      const p = crear("p");
      p.append(enLinea(parrafo.join(" ")));
      caja.append(p);
      parrafo = [];
    }
  };
  contenido.split("\n").forEach((cruda) => {
    const linea = cruda.trim();
    const vineta = linea.match(/^[-*•]\s+(.*)$/);
    const numerada = linea.match(/^\d+[.)]\s+(.*)$/);
    if (!linea) {
      cerrarParrafo();
      lista = null;
    } else if (/^#{1,6}\s/.test(linea)) {
      cerrarParrafo();
      lista = null;
      const titulo = crear("h4");
      titulo.append(enLinea(linea.replace(/^#+\s*/, "")));
      caja.append(titulo);
    } else if (vineta || numerada) {
      cerrarParrafo();
      const etiqueta = vineta ? "ul" : "ol";
      if (!lista || lista.tagName.toLowerCase() !== etiqueta) {
        lista = crear(etiqueta);
        caja.append(lista);
      }
      const item = crear("li");
      item.append(enLinea((vineta || numerada)[1]));
      lista.append(item);
    } else {
      lista = null;
      parrafo.push(linea);
    }
  });
  cerrarParrafo();
  return caja;
}

function vistaTexto(r) {
  const caja = crear("div", "codigo-caja");
  caja.append(textoLegible(r.contenido), botonCopiar(r.contenido));
  return caja;
}

function vistaResumen(resumen) {
  const caja = crear("div", "resumen-diagrama");
  const titulo = crear("h4");
  titulo.append(icono("sparkles"), crear("span", "", "Resumen del diagrama"));
  const chips = crear("div", "resultado-ia-chips");
  chips.append(chipOrigen(resumen));
  if (resumen.evidencia) {
    chips.append(enlaceEvidencia(resumen.evidencia));
  }
  caja.append(titulo, chips, vistaTexto(resumen));
  return caja;
}

export function mostrarResultadoIa(contenedor, r) {
  const caja = crear("div", "resultado-ia");
  caja.append(chipsResultado(r));
  if (r.aviso) {
    caja.append(crear("div", "resultado-ia-aviso", r.aviso));
  }
  caja.append(r.esDiagrama ? vistaDiagrama(r) : vistaTexto(r));
  if (r.resumen) {
    caja.append(vistaResumen(r.resumen));
  }
  contenedor.replaceChildren(caja);
}
