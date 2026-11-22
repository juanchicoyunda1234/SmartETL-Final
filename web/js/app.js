import { pedir, seguirTarea } from "/web/js/api.js";
import { aplicarTema, temaGuardado, toast, bloquear } from "/web/js/interfaz.js";
import { iniciarEtl } from "/web/js/etl.js";
import { iniciarDatos } from "/web/js/datos.js";
import { iniciarEstructuras } from "/web/js/estructuras.js";
import { iniciarGrafo } from "/web/js/grafo.js";
import { iniciarIa } from "/web/js/ia.js";
import { iniciarReporte } from "/web/js/reporte.js";

const VISTAS = ["datos", "etl", "estructuras", "grafo", "ia", "reporte"];

function mostrarVista(nombre, enfocar = false) {
  const vista = VISTAS.includes(nombre) ? nombre : "datos";
  document.querySelectorAll(".pestana").forEach((pestana) => {
    const activa = pestana.dataset.vista === vista;
    pestana.setAttribute("aria-selected", String(activa));
    pestana.tabIndex = activa ? 0 : -1;
    if (activa && enfocar) {
      pestana.focus();
    }
  });
  document.querySelectorAll(".vista").forEach((seccion) => {
    seccion.classList.toggle("activa", seccion.id === "vista-" + vista);
  });
  if (location.hash !== "#" + vista) {
    history.replaceState(null, "", "#" + vista);
  }
  document.dispatchEvent(new CustomEvent("vista", { detail: vista }));
}

function prepararPestanas() {
  const pestanas = [...document.querySelectorAll(".pestana")];
  pestanas.forEach((pestana, indice) => {
    pestana.addEventListener("click", () => mostrarVista(pestana.dataset.vista));
    pestana.addEventListener("keydown", (evento) => {
      if (evento.key !== "ArrowRight" && evento.key !== "ArrowLeft") {
        return;
      }
      const paso = evento.key === "ArrowRight" ? 1 : -1;
      const destino = pestanas[(indice + paso + pestanas.length) % pestanas.length];
      mostrarVista(destino.dataset.vista, true);
    });
  });
  mostrarVista(location.hash.slice(1));
}

function prepararTema() {
  aplicarTema(temaGuardado());
  document.getElementById("botonTema").addEventListener("click", () => {
    aplicarTema(document.documentElement.dataset.tema === "oscuro" ? "claro" : "oscuro");
  });
}

function pintarIa(ia) {
  const punto = document.getElementById("puntoIa");
  const texto = document.getElementById("textoIa");
  punto.className = "punto";
  if (ia.modo === "Solo local") {
    texto.textContent = "IA: solo local";
    return;
  }
  if (ia.validacion === "validada") {
    punto.classList.add("ok");
    texto.textContent = ia.proveedor + " · validada";
  } else if (ia.validacion === "fallo") {
    punto.classList.add("mal");
    texto.textContent = ia.proveedor + " · error";
  } else {
    texto.textContent = "IA sin validar";
  }
}

async function cargarEstado() {
  try {
    const estado = await pedir("/api/estado");
    const carpeta = document.getElementById("carpeta");
    carpeta.textContent = estado.carpeta;
    carpeta.title = "Carpeta del proyecto: " + estado.carpeta;
    pintarIa(estado.ia);
    if (!estado.datosEncontrados) {
      toast("No se encontró data/airports.csv en " + estado.carpeta + ". Cierra el programa y vuelve a abrirlo para elegir la carpeta data.", "error");
    }
    if (estado.tareaEnCurso) {
      bloquear(true, estado.tareaEnCurso.nombre);
      seguirTarea(estado.tareaEnCurso.id, () => {}).catch(() => {}).finally(() => bloquear(false));
    }
    return estado;
  } catch (error) {
    document.getElementById("carpeta").textContent = "Sin conexión con el servidor";
    toast(error.message, "error");
    return null;
  }
}

prepararTema();
prepararPestanas();
document.getElementById("pildoraIa").addEventListener("click", () => mostrarVista("ia"));
iniciarDatos();
iniciarEstructuras();
iniciarGrafo();
iniciarIa();
iniciarReporte();
cargarEstado().then((estado) => iniciarEtl(estado));
