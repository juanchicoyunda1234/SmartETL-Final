import { animate } from "/recursos/anime.esm.min.js";
import { pedir, seguirTarea } from "/web/js/api.js";

const MOSTRAR_DESDE = 500;
const RELOJ_DESDE = 3000;

export function icono(nombre) {
  const span = document.createElement("span");
  span.className = "icono";
  span.style.setProperty("--icono", "url(/recursos/iconos/" + nombre + ".svg)");
  return span;
}

export function crear(etiqueta, clase, texto) {
  const elemento = document.createElement(etiqueta);
  if (clase) {
    elemento.className = clase;
  }
  if (texto !== undefined) {
    elemento.textContent = texto;
  }
  return elemento;
}

export function toast(texto, tipo = "info") {
  const caja = document.getElementById("toasts");
  const aviso = crear("div", "toast " + tipo);
  aviso.setAttribute("role", tipo === "error" ? "alert" : "status");
  aviso.append(icono(tipo === "error" ? "circle-alert" : tipo === "exito" ? "circle-check" : "info"), crear("div", "", texto));
  caja.append(aviso);
  const duracion = tipo === "error" ? 7000 : 4000;
  setTimeout(() => {
    aviso.classList.add("saliendo");
    setTimeout(() => aviso.remove(), 320);
  }, duracion);
}

export function formatoNumero(valor, decimales = 0) {
  return Number(valor).toLocaleString("es-EC", { minimumFractionDigits: decimales, maximumFractionDigits: decimales });
}

export function contar(elemento, valorFinal, decimales = 0, sufijo = "") {
  const estado = { valor: 0 };
  if (matchMedia("(prefers-reduced-motion: reduce)").matches) {
    elemento.textContent = formatoNumero(valorFinal, decimales) + sufijo;
    return;
  }
  animate(estado, {
    valor: valorFinal,
    duration: 900,
    ease: "outExpo",
    onUpdate: () => {
      elemento.textContent = formatoNumero(estado.valor, decimales) + sufijo;
    },
  });
}

export function bloquear(activo, nombre = "") {
  document.querySelectorAll("[data-tarea]").forEach((boton) => {
    boton.disabled = activo || boton.dataset.deshabilitado === "si";
  });
  const indicador = document.getElementById("estadoTarea");
  indicador.hidden = !activo;
  indicador.replaceChildren();
  if (activo) {
    indicador.append(crear("span", "giro"), crear("span", "", nombre));
  }
}

class Carga {
  constructor(titulo, contenedor, etapas) {
    this.titulo = titulo;
    this.contenedor = contenedor;
    this.inicio = performance.now();
    this.etapas = (etapas || []).map((etapa) => ({ texto: etapa.texto, prefijo: etapa.prefijo, estado: "pendiente" }));
    this.dinamicas = this.etapas.length === 0;
    this.mensaje = "Preparando…";
    this.temporizador = setTimeout(() => this.mostrar(), MOSTRAR_DESDE);
  }

  mostrar() {
    this.velo = crear("div", "velo" + (this.contenedor ? " local" : ""));
    const tarjeta = crear("div", "tarjeta-carga");
    tarjeta.setAttribute("role", "status");
    const cabecera = crear("div", "carga-cabecera");
    const textos = crear("div");
    this.nodoMensaje = crear("div", "carga-mensaje", this.mensaje);
    textos.append(crear("div", "carga-titulo", this.titulo), this.nodoMensaje);
    this.reloj = crear("span", "carga-reloj");
    cabecera.append(crear("span", "giro"), textos, this.reloj);
    this.lista = crear("ul", "etapas");
    tarjeta.append(cabecera, this.lista);
    this.velo.append(tarjeta);
    (this.contenedor || document.body).append(this.velo);
    this.pintarEtapas();
    this.intervalo = setInterval(() => this.pintarReloj(), 250);
  }

  pintarReloj() {
    const transcurrido = performance.now() - this.inicio;
    if (this.reloj) {
      this.reloj.textContent = transcurrido >= RELOJ_DESDE ? (transcurrido / 1000).toFixed(1) + " s" : "";
    }
  }

  aviso(datos) {
    this.mensaje = datos.mensaje;
    const actual = this.etapas.findIndex((etapa) => etapa.estado === "actual");
    let siguiente = -1;
    if (this.dinamicas) {
      this.etapas.push({ texto: datos.mensaje, estado: "pendiente" });
      siguiente = this.etapas.length - 1;
    } else {
      siguiente = this.etapas.findIndex((etapa, i) => i > actual && datos.mensaje.startsWith(etapa.prefijo));
    }
    if (siguiente >= 0) {
      for (let i = 0; i < siguiente; i++) {
        this.cerrarEtapa(i, datos.ms);
      }
      this.etapas[siguiente].estado = "actual";
      this.etapas[siguiente].desde = datos.ms;
    }
    if (this.nodoMensaje) {
      this.nodoMensaje.textContent = datos.mensaje;
      this.pintarEtapas();
    }
  }

  cerrarEtapa(indice, hasta) {
    const etapa = this.etapas[indice];
    if (etapa.estado !== "hecha") {
      etapa.estado = "hecha";
      etapa.duracion = etapa.desde === undefined ? null : hasta - etapa.desde;
    }
  }

  pintarEtapas() {
    if (!this.lista) {
      return;
    }
    this.lista.replaceChildren();
    this.etapas.forEach((etapa) => {
      const fila = crear("li", "etapa " + (etapa.estado === "pendiente" ? "" : etapa.estado));
      const marca = crear("span", "etapa-marca", etapa.estado === "hecha" ? "✓" : "");
      fila.append(marca, crear("span", "", etapa.texto));
      if (etapa.estado === "hecha" && etapa.duracion !== null) {
        fila.append(crear("span", "etapa-tiempo", formatoNumero(etapa.duracion) + " ms"));
      }
      this.lista.append(fila);
    });
  }

  terminar(msTotal) {
    clearTimeout(this.temporizador);
    clearInterval(this.intervalo);
    this.etapas.forEach((etapa, i) => {
      if (etapa.estado === "actual") {
        this.cerrarEtapa(i, msTotal);
      }
    });
    if (!this.velo) {
      return;
    }
    this.pintarEtapas();
    const velo = this.velo;
    setTimeout(() => {
      velo.classList.add("saliendo");
      setTimeout(() => velo.remove(), 200);
    }, 350);
  }
}

export async function ejecutarTarea({ ruta, cuerpo, titulo, contenedor, etapas, alAviso }) {
  bloquear(true, titulo);
  const carga = new Carga(titulo, contenedor, etapas);
  try {
    const tarea = await pedir(ruta, { metodo: "POST", cuerpo: cuerpo || {} });
    const final = await seguirTarea(tarea.id, (datos) => {
      carga.aviso(datos);
      if (alAviso) {
        alAviso(datos);
      }
    });
    carga.terminar(final.ms);
    return final;
  } catch (error) {
    carga.terminar(0);
    toast(error.message, "error");
    throw error;
  } finally {
    bloquear(false);
  }
}

export function aplicarTema(tema) {
  document.documentElement.dataset.tema = tema;
  document.dispatchEvent(new CustomEvent("tema", { detail: tema }));
  try {
    localStorage.setItem("smartetl-tema", tema);
  } catch (falla) {
  }
}

export function temaGuardado() {
  try {
    return localStorage.getItem("smartetl-tema") || "oscuro";
  } catch (falla) {
    return "oscuro";
  }
}

const scriptsCargados = {};

export function cargarScript(ruta) {
  if (!scriptsCargados[ruta]) {
    scriptsCargados[ruta] = new Promise((listo, falla) => {
      const script = document.createElement("script");
      script.src = ruta;
      script.onload = listo;
      script.onerror = () => falla(new Error("No se pudo cargar " + ruta));
      document.head.append(script);
    });
  }
  return scriptsCargados[ruta];
}

export function color(variable, elemento = document.documentElement) {
  return getComputedStyle(elemento).getPropertyValue(variable).trim();
}

export function celdaValor(valor) {
  const td = crear("td");
  if (valor === "\\N") {
    td.append(crear("span", "etiqueta-nulo", "\\N"));
  } else if (valor === "") {
    td.append(crear("span", "etiqueta-vacio", "vacío"));
  } else {
    td.textContent = valor;
    td.title = valor;
  }
  return td;
}

function botonPagina(nombreIcono, etiqueta, pagina, activo, alCambiar) {
  const boton = crear("button", "boton");
  boton.type = "button";
  boton.setAttribute("aria-label", etiqueta);
  boton.title = etiqueta;
  boton.append(icono(nombreIcono));
  boton.disabled = !activo;
  boton.addEventListener("click", () => alCambiar(pagina));
  return boton;
}

export function pintarPaginacion(caja, vista, alCambiar, nombreFilas = "Filas") {
  const controles = crear("div", "acciones");
  controles.append(
    botonPagina("chevrons-left", "Primera página", 1, vista.pagina > 1, alCambiar),
    botonPagina("chevron-left", "Página anterior", vista.pagina - 1, vista.pagina > 1, alCambiar),
    crear("span", "paginacion-actual", "Página " + formatoNumero(vista.pagina) + " de " + formatoNumero(vista.paginas)),
    botonPagina("chevron-right", "Página siguiente", vista.pagina + 1, vista.pagina < vista.paginas, alCambiar),
    botonPagina("chevrons-right", "Última página", vista.paginas, vista.pagina < vista.paginas, alCambiar)
  );
  const rango = vista.total === 0 ? "Sin filas" : nombreFilas + " " + formatoNumero(vista.desde) + "–" + formatoNumero(vista.hasta) + " de " + formatoNumero(vista.total);
  caja.replaceChildren(crear("span", "", rango), controles);
}

export function abrirModal(titulo, contenido, acciones = []) {
  const velo = crear("div", "velo modal-velo");
  const ventana = crear("div", "modal");
  ventana.setAttribute("role", "dialog");
  ventana.setAttribute("aria-modal", "true");
  ventana.setAttribute("aria-label", titulo);
  const cabecera = crear("div", "modal-cabecera");
  const cerrar = crear("button", "boton-icono");
  cerrar.type = "button";
  cerrar.setAttribute("aria-label", "Cerrar");
  cerrar.append(icono("x"));
  cabecera.append(crear("h2", "", titulo), cerrar);
  const pie = crear("div", "modal-pie");
  const quitar = () => {
    velo.classList.add("saliendo");
    document.removeEventListener("keydown", alTeclado);
    setTimeout(() => velo.remove(), 160);
  };
  const alTeclado = (evento) => {
    if (evento.key === "Escape") {
      quitar();
    }
  };
  acciones.forEach((accion) => {
    const boton = crear("button", "boton" + (accion.principal ? " boton-primario" : "") + (accion.peligro ? " boton-peligro" : ""));
    boton.type = "button";
    boton.textContent = accion.texto;
    boton.addEventListener("click", () => {
      quitar();
      if (accion.alPulsar) {
        accion.alPulsar();
      }
    });
    pie.append(boton);
  });
  const cuerpo = crear("div", "modal-cuerpo");
  cuerpo.append(contenido);
  ventana.append(cabecera, cuerpo);
  if (acciones.length > 0) {
    ventana.append(pie);
  }
  velo.append(ventana);
  velo.addEventListener("click", (evento) => {
    if (evento.target === velo) {
      quitar();
    }
  });
  cerrar.addEventListener("click", quitar);
  document.addEventListener("keydown", alTeclado);
  document.body.append(velo);
  cerrar.focus();
  return quitar;
}

export function formatoBytes(bytes) {
  if (bytes < 1024) {
    return formatoNumero(bytes) + " B";
  }
  if (bytes < 1024 * 1024) {
    return formatoNumero(bytes / 1024, 1) + " KB";
  }
  return formatoNumero(bytes / 1024 / 1024, 1) + " MB";
}

export function tarjetaKpi(nombreIcono, etiqueta, valor, decimales, sufijo, detalle) {
  const tarjeta = crear("div", "tarjeta kpi");
  const cabecera = crear("div", "kpi-etiqueta");
  cabecera.append(icono(nombreIcono), crear("span", "", etiqueta));
  const numero = crear("div", "kpi-valor", "0");
  tarjeta.append(cabecera, numero, crear("div", "kpi-detalle", detalle));
  contar(numero, valor, decimales, sufijo);
  return tarjeta;
}

export function segmentado(caja, opciones, elegida, alElegir) {
  caja.replaceChildren();
  opciones.forEach((opcion) => {
    const boton = crear("button", "", opcion.texto);
    boton.type = "button";
    boton.setAttribute("aria-pressed", String(opcion.valor === elegida));
    boton.addEventListener("click", () => {
      caja.querySelectorAll("button").forEach((otro) => otro.setAttribute("aria-pressed", String(otro === boton)));
      alElegir(opcion.valor);
    });
    caja.append(boton);
  });
}
