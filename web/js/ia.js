import autoAnimate from "/recursos/auto-animate.mjs";
import { pedir, seguirTarea } from "/web/js/api.js";
import { ejecutarTarea, crear, icono, toast, segmentado, bloquear, abrirModal } from "/web/js/interfaz.js";
import { mostrarResultadoIa, usarManoAlzada, manoAlzadaActiva } from "/web/js/resultadoIa.js";

const $ = (id) => document.getElementById(id);
const MODO_API = "API en vivo + respaldo local";
const MODO_LOCAL = "Solo local";
const COLORES = { Gemini: "--azul", Claude: "--aviso", OpenAI: "--exito", GLM: "--morado" };
const NOMBRES = {
  flujo_etl: "Flujograma ETL", errores_etl: "Analizar errores ETL", flujo_cola: "Flujograma Cola", flujo_pila: "Flujograma Pila",
  flujo_lista: "Flujograma Lista enlazada", flujo_bst: "Flujograma BST", flujo_bfs: "Flujograma BFS", flujo_dfs: "Flujograma DFS",
  explicar_arbol: "Explicar árbol", explicar_grafo: "Explicar grafo",
};

const estado = { config: null, modo: MODO_API, apis: [], historial: [], abierto: null };

function proveedorActual() {
  return estado.config.proveedores.find((p) => p.nombre === $("iaProveedor").value) || estado.config.proveedores[0];
}

function colorProveedor(nombre) {
  return "var(" + (COLORES[nombre] || "--acento") + ")";
}

function pintarIndicador(estadoValidacion, detalle) {
  const indicador = $("iaIndicador");
  const local = estado.modo === MODO_LOCAL;
  indicador.dataset.estado = local ? "local" : estadoValidacion;
  const textos = {
    sin_validar: "Sin validar",
    validando: "Validando…",
    validada: "✓ Validada" + (detalle ? ": " + detalle : ""),
    fallo: "✗ " + (detalle || "La API no respondió"),
  };
  $("iaIndicadorTexto").textContent = local ? "Solo local: no usa la API" : textos[estadoValidacion] || textos.sin_validar;
  const proveedor = $("iaProveedor").value;
  const modelo = $("iaModelo").value.trim();
  $("iaResumenConexion").textContent = local ? "Respaldo local" : proveedor + (modelo ? " · " + modelo : " · sin modelo");
  pintarPildora(local ? "local" : estadoValidacion, proveedor);
}

function pintarPildora(estadoValidacion, proveedor) {
  const punto = $("puntoIa");
  const texto = $("textoIa");
  punto.className = "punto" + (estadoValidacion === "validada" ? " ok" : estadoValidacion === "fallo" ? " mal" : estadoValidacion === "validando" ? " validando" : "");
  texto.textContent = estadoValidacion === "local" ? "IA: solo local" : estadoValidacion === "validada" ? proveedor + " · validada"
    : estadoValidacion === "fallo" ? proveedor + " · error" : estadoValidacion === "validando" ? "Validando " + proveedor : "IA sin validar";
}

function plegar(plegado) {
  $("iaFormulario").hidden = plegado;
  const boton = $("iaPlegar");
  boton.setAttribute("aria-expanded", String(!plegado));
  boton.lastElementChild.textContent = plegado ? "Editar conexión" : "Ocultar";
  boton.querySelector(".icono").style.setProperty("--icono", "url(/recursos/iconos/" + (plegado ? "pencil" : "chevron-down") + ".svg)");
}

function pintarOrigenClave() {
  const chip = $("iaOrigenClave");
  const proveedor = proveedorActual();
  const actual = estado.config.actual;
  const mismo = actual.proveedor === proveedor.nombre;
  let texto = "";
  if ($("iaClave").value.trim()) {
    texto = "Se enviará al servidor al validar o usar una función";
  } else if (mismo && actual.tieneClave) {
    const origen = actual.origenClave || "";
    texto = origen.startsWith("entorno:") ? "Usando " + origen.slice(8) + " del sistema"
      : origen.startsWith("guardada:") ? "Clave de la API guardada «" + origen.slice(9) + "»"
        : "Clave en la memoria del servidor";
  } else if (proveedor.enEntorno) {
    texto = "Se usará " + proveedor.variable + " del sistema";
  } else {
    texto = "Sin clave: escríbela, usa una API guardada o el modo Solo local";
  }
  chip.textContent = texto;
  chip.hidden = false;
  $("iaClave").placeholder = mismo && actual.tieneClave ? "•••••••• (en el servidor)" : proveedor.enEntorno ? "vacío = " + proveedor.variable : "pega aquí la clave";
}

function pintarAyudas() {
  const proveedor = proveedorActual();
  $("iaAyudaModelo").textContent = "Modelo: " + proveedor.ayudaModelo + ".";
  $("iaAyudaClave").textContent = "Clave: " + proveedor.ayudaClave + ". Variable de entorno: " + proveedor.variable + ".";
}

function pintarFormulario() {
  const c = estado.config;
  const select = $("iaProveedor");
  if (select.options.length === 0) {
    c.proveedores.forEach((p) => select.append(new Option(p.nombre, p.nombre)));
  }
  select.value = c.actual.proveedor;
  $("iaModelo").value = c.actual.modelo;
  $("iaUrl").value = c.actual.url;
  $("iaClave").value = "";
  estado.modo = c.actual.modo;
  segmentado($("iaModo"), [{ valor: MODO_API, texto: "API + respaldo" }, { valor: MODO_LOCAL, texto: "Solo local" }], estado.modo, (valor) => {
    estado.modo = valor;
    pintarIndicador("sin_validar", "");
  });
  pintarAyudas();
  pintarOrigenClave();
  pintarIndicador(c.validacion.estado, c.validacion.detalle);
}

async function cargarConfig() {
  try {
    estado.config = await pedir("/api/ia/config");
    pintarFormulario();
    plegar(estado.config.validacion.estado === "validada");
  } catch (error) {
    toast(error.message, "error");
  }
}

function marcarCambio() {
  pintarIndicador("sin_validar", "");
  pintarOrigenClave();
}

function alCambiarProveedor() {
  $("iaUrl").value = proveedorActual().url;
  $("iaClave").value = "";
  $("iaGuardada").value = "";
  pintarAyudas();
  marcarCambio();
}

async function aplicarConfig() {
  const proveedor = $("iaProveedor").value;
  const clave = $("iaClave").value.trim();
  estado.config = await pedir("/api/ia/config", {
    metodo: "POST",
    cuerpo: { proveedor, modelo: $("iaModelo").value.trim(), url: $("iaUrl").value.trim(), modo: estado.modo, clave, conservarClave: clave === "" && proveedor === estado.config.actual.proveedor },
  });
  $("iaClave").value = "";
  pintarOrigenClave();
  pintarIndicador(estado.config.validacion.estado, estado.config.validacion.detalle);
}

async function validar() {
  try {
    await aplicarConfig();
  } catch (error) {
    toast(error.message, "error");
    return;
  }
  if (estado.modo === MODO_LOCAL) {
    toast("En modo Solo local no hace falta validar: todo se genera con el respaldo local", "info");
    return;
  }
  pintarIndicador("validando", "");
  bloquear(true, "Validando la API");
  try {
    const tarea = await pedir("/api/ia/validar", { metodo: "POST", cuerpo: {} });
    const final = await seguirTarea(tarea.id, () => {});
    pintarIndicador("validada", final.datos.respuesta ? "respondió en " + final.datos.ms + " ms" : "");
    toast(final.datos.proveedor + " (" + final.datos.modelo + ") respondió: " + final.datos.respuesta, "exito");
    setTimeout(() => plegar(true), 700);
  } catch (error) {
    pintarIndicador("fallo", error.message);
    plegar(false);
  } finally {
    bloquear(false);
    estado.config = await pedir("/api/ia/config").catch(() => estado.config);
  }
}

function marcarFuncion(funcion) {
  document.querySelectorAll("#iaFunciones .funcion-ia").forEach((boton) => boton.classList.toggle("activa", boton.dataset.funcion === funcion));
}

async function ejecutarFuncion(funcion) {
  try {
    await aplicarConfig();
  } catch (error) {
    toast(error.message, "error");
    return;
  }
  marcarFuncion(funcion);
  try {
    const final = await ejecutarTarea({
      ruta: "/api/ia/funcion",
      cuerpo: { funcion, incluirResumen: $("iaIncluirResumen").checked },
      titulo: NOMBRES[funcion] + (estado.modo === MODO_LOCAL ? " (respaldo local)" : " con " + $("iaProveedor").value),
    });
    const r = final.datos.resultado;
    estado.abierto = final.datos.historial;
    mostrarResultadoIa($("iaResultado"), r);
    if (r.aviso && !r.usoApi && estado.modo !== MODO_LOCAL) {
      toast(r.aviso, "info");
    }
    $("iaResultado").scrollIntoView({ block: "start", behavior: "smooth" });
    await cargarHistorial();
  } catch (error) {
  }
}

async function documentacion() {
  try {
    await aplicarConfig();
  } catch (error) {
    toast(error.message, "error");
    return;
  }
  const etapas = Object.values(NOMBRES).map((nombre, i) => ({ texto: nombre, prefijo: "Documentación " + (i + 1) + " de 10" }));
  try {
    const final = await ejecutarTarea({ ruta: "/api/ia/documentacion", cuerpo: {}, titulo: "Generando la documentación (10 funciones)", etapas });
    const r = final.datos;
    const caja = crear("div", "resultado-ia");
    const chips = crear("div", "resultado-ia-chips");
    chips.append(crear("span", "chip " + (r.conApi > 0 ? "chip-exito" : "chip-aviso"), r.conApi + " de " + r.total + " respondidas por la API"));
    const enlace = crear("button", "enlace");
    enlace.type = "button";
    enlace.append(icono("file-text"), crear("span", "", "Ver DOCUMENTACION.md"));
    enlace.addEventListener("click", () => verDocumentacion(r.archivo));
    chips.append(enlace);
    const lista = crear("div", "doc-lista");
    r.funciones.forEach((f) => {
      const fila = crear("div", "resultado-ia-chips");
      fila.append(crear("span", "chip " + (f.usoApi ? "chip-exito" : "chip-aviso"), (f.usoApi ? "IA" : "RESPALDO LOCAL")),
        crear("b", "", f.titulo));
      if (f.esDiagrama) {
        fila.append(crear("span", "chip " + (f.coherente ? "chip-exito" : "chip-aviso"), f.coherente ? "✓ coherente" : "revisar coherencia"));
      }
      const abrir = crear("button", "enlace", "Abrir");
      abrir.type = "button";
      abrir.addEventListener("click", () => abrirDelHistorial(f.historial));
      fila.append(abrir);
      lista.append(fila);
    });
    caja.append(chips, crear("p", "nota", "Se escribió diagramas/DOCUMENTACION.md y la página con zoom de cada diagrama."), lista);
    $("iaResultado").replaceChildren(caja);
    marcarFuncion(null);
    await cargarHistorial();
  } catch (error) {
  }
}

async function verDocumentacion(ruta) {
  try {
    const respuesta = await fetch(ruta + "?v=" + Date.now());
    abrirModal("diagramas/DOCUMENTACION.md", crear("pre", "texto-mono", await respuesta.text()));
  } catch (error) {
    toast("No se pudo abrir la documentación", "error");
  }
}

function pintarHistorial() {
  const caja = $("iaHistorial");
  if (estado.historial.length === 0) {
    caja.replaceChildren(crear("p", "nota", "Aún no hay resultados en esta sesión."));
    return;
  }
  caja.replaceChildren(...estado.historial.slice().reverse().map((item) => {
    const r = item.resultado;
    const boton = crear("button", "historial-item");
    boton.type = "button";
    boton.setAttribute("aria-current", String(item.id === estado.abierto));
    const hora = new Date(r.momento).toLocaleTimeString("es-EC", { hour: "2-digit", minute: "2-digit" });
    const detalle = crear("small");
    detalle.append(crear("i", r.usoApi ? "api" : ""), document.createTextNode((r.usoApi ? "IA" : "Respaldo local") + " · " + hora + (r.resumen ? " · con resumen" : "")));
    boton.append(crear("b", "", r.titulo), detalle);
    boton.addEventListener("click", () => abrirDelHistorial(item.id));
    return boton;
  }));
}

async function cargarHistorial() {
  try {
    estado.historial = await pedir("/api/ia/historial");
    pintarHistorial();
  } catch (error) {
  }
}

function abrirDelHistorial(id) {
  const item = estado.historial.find((h) => h.id === id);
  if (!item) {
    return;
  }
  estado.abierto = id;
  marcarFuncion(item.resultado.funcion);
  mostrarResultadoIa($("iaResultado"), item.resultado);
  pintarHistorial();
}

async function cargarApis() {
  try {
    const r = await pedir("/api/apis");
    estado.apis = r.apis;
    const select = $("iaGuardada");
    const elegida = select.value;
    select.replaceChildren(new Option("(elige una)", ""), ...r.apis.map((api) => new Option(api.nombre + " · " + api.proveedor, api.nombre)));
    select.value = r.apis.some((api) => api.nombre === elegida) ? elegida : "";
    return r;
  } catch (error) {
    toast(error.message, "error");
    return { apis: [], archivo: "", error: error.message };
  }
}

async function usarApi(nombre, avisar = true) {
  estado.config = await pedir("/api/apis/" + encodeURIComponent(nombre) + "/usar", { metodo: "POST", cuerpo: {} });
  pintarFormulario();
  $("iaGuardada").value = nombre;
  plegar(false);
  if (avisar) {
    toast("API «" + nombre + "» cargada en el servidor. Pulsa Validar API para comprobarla.", "exito");
  }
}

function campo(etiqueta, control, id, ayuda, envoltura) {
  const caja = crear("div", "campo");
  const rotulo = crear("label", "etiqueta", etiqueta);
  rotulo.htmlFor = id;
  control.id = id;
  caja.append(rotulo, envoltura || control);
  if (ayuda) {
    caja.append(ayuda);
  }
  caja.append(crear("small", "mensaje-campo"));
  return caja;
}

function entrada(tipo) {
  const control = crear("input");
  control.type = tipo;
  control.autocomplete = "off";
  control.spellcheck = false;
  return control;
}

function conOjo(control) {
  const caja = crear("div", "campo-clave");
  const ojo = crear("button", "boton-icono");
  ojo.type = "button";
  ojo.setAttribute("aria-label", "Mostrar la clave");
  ojo.setAttribute("aria-pressed", "false");
  ojo.append(icono("eye"));
  ojo.addEventListener("click", () => alternarOjo(control, ojo));
  caja.append(control, ojo);
  return caja;
}

function alternarOjo(control, ojo) {
  const mostrar = control.type === "password";
  control.type = mostrar ? "text" : "password";
  ojo.setAttribute("aria-pressed", String(mostrar));
  ojo.setAttribute("aria-label", mostrar ? "Ocultar la clave" : "Mostrar la clave");
  ojo.querySelector(".icono").style.setProperty("--icono", "url(/recursos/iconos/" + (mostrar ? "eye-off" : "eye") + ".svg)");
}

function marcarError(formulario, nombreCampo, mensaje) {
  formulario.querySelectorAll(".campo").forEach((c) => {
    c.classList.remove("invalido", "sacudir");
    c.querySelector(".mensaje-campo").textContent = "";
  });
  if (!nombreCampo && !mensaje) {
    return;
  }
  const caja = nombreCampo ? formulario.querySelector("#api-" + nombreCampo) : null;
  if (!caja) {
    toast(mensaje, "error");
    return;
  }
  const contenedor = caja.closest(".campo");
  contenedor.classList.add("invalido", "sacudir");
  contenedor.querySelector(".mensaje-campo").textContent = mensaje;
  setTimeout(() => contenedor.classList.remove("sacudir"), 400);
  caja.focus();
}

function gestionarApis() {
  const raiz = crear("div");
  const banner = crear("div", "apis-banner");
  banner.append(icono("triangle-alert"), crear("div", "", "Las claves se guardan en texto plano en tu computador. No las compartas ni las subas a GitHub. Nunca se envían al navegador: al editar verás •••••••• (guardada)."));
  const rejilla = crear("div", "apis-rejilla");
  const lista = crear("div", "apis-lista");
  autoAnimate(lista);
  const formulario = crear("form", "apis-formulario");
  formulario.noValidate = true;
  const tituloFormulario = crear("h3", "", "Nueva API");
  const nombre = entrada("text");
  nombre.placeholder = "ej. Gemini de clase";
  const proveedor = crear("select");
  estado.config.proveedores.forEach((p) => proveedor.append(new Option(p.nombre, p.nombre)));
  const modelo = entrada("text");
  const url = entrada("text");
  const clave = entrada("password");
  const ayudaModelo = crear("small", "ayuda-campo");
  const ayudaClave = crear("small", "ayuda-campo");
  let original = "";
  const ayudar = () => {
    const p = estado.config.proveedores.find((x) => x.nombre === proveedor.value);
    ayudaModelo.textContent = p.ayudaModelo;
    ayudaClave.textContent = p.ayudaClave;
    url.placeholder = p.url;
  };
  proveedor.addEventListener("change", () => {
    url.value = "";
    ayudar();
  });
  const limpiar = () => {
    original = "";
    tituloFormulario.textContent = "Nueva API";
    formulario.reset();
    clave.placeholder = "pega aquí la clave";
    marcarError(formulario, "", "");
    ayudar();
  };
  const guardar = async (yValidar) => {
    try {
      await pedir("/api/apis", { metodo: "POST", cuerpo: { nombre: nombre.value.trim(), proveedor: proveedor.value, modelo: modelo.value.trim(), url: url.value.trim(), clave: clave.value.trim(), original } });
      clave.value = "";
      const guardada = nombre.value.trim();
      toast("API «" + guardada + "» guardada", "exito");
      limpiar();
      await pintarApis();
      if (yValidar) {
        cerrar();
        await usarApi(guardada, false);
        await validar();
      }
    } catch (error) {
      marcarError(formulario, error.campo, error.message);
    }
  };
  const acciones = crear("div", "acciones");
  const botonGuardar = crear("button", "boton", "Guardar");
  botonGuardar.type = "button";
  botonGuardar.addEventListener("click", () => guardar(false));
  const botonValidar = crear("button", "boton boton-primario", "Guardar y validar");
  botonValidar.type = "submit";
  const botonLimpiar = crear("button", "boton boton-fantasma", "Limpiar");
  botonLimpiar.type = "button";
  botonLimpiar.addEventListener("click", limpiar);
  acciones.append(botonValidar, botonGuardar, botonLimpiar);
  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    guardar(true);
  });
  formulario.append(tituloFormulario, campo("Nombre", nombre, "api-nombre"), campo("Proveedor", proveedor, "api-proveedor"),
    campo("Modelo", modelo, "api-modelo", ayudaModelo), campo("URL (vacía = la del proveedor)", url, "api-url"));
  formulario.append(campo("Clave", clave, "api-clave", ayudaClave, conOjo(clave)), acciones);

  const editar = (api) => {
    original = api.nombre;
    tituloFormulario.textContent = "Editar «" + api.nombre + "»";
    nombre.value = api.nombre;
    proveedor.value = api.proveedor;
    modelo.value = api.modelo;
    url.value = api.url;
    clave.value = "";
    clave.placeholder = "•••••••• (guardada)";
    ayudar();
    nombre.focus();
  };
  const eliminar = (api) => {
    abrirModal("Eliminar API", crear("p", "", "¿Eliminar «" + api.nombre + "» (" + api.proveedor + ")? Se borra del archivo local y no se puede deshacer."), [
      { texto: "Cancelar" },
      { texto: "Eliminar", peligro: true, alPulsar: async () => {
        try {
          await pedir("/api/apis/" + encodeURIComponent(api.nombre), { metodo: "DELETE" });
          toast("API «" + api.nombre + "» eliminada", "exito");
          if (original === api.nombre) {
            limpiar();
          }
          await pintarApis();
        } catch (error) {
          toast(error.message, "error");
        }
      } },
    ]);
  };
  const pintarApis = async () => {
    const r = await cargarApis();
    if (r.error) {
      lista.replaceChildren(crear("p", "nota", r.error));
      return;
    }
    if (r.apis.length === 0) {
      const vacio = crear("div", "apis-vacio");
      const agregar = crear("button", "boton boton-primario");
      agregar.type = "button";
      agregar.append(icono("plus"), crear("span", "", "Agregar la primera"));
      agregar.addEventListener("click", () => {
        limpiar();
        nombre.focus();
      });
      vacio.append(crear("b", "", "Aún no tienes APIs guardadas"), crear("p", "", "Guárdalas aquí para no volver a pegar la clave en cada sesión."), agregar);
      lista.replaceChildren(vacio);
      return;
    }
    lista.replaceChildren(...r.apis.map((api) => {
      const tarjeta = crear("div", "api-tarjeta");
      const punto = crear("span", "punto-proveedor");
      punto.style.setProperty("--color", colorProveedor(api.proveedor));
      const botones = crear("div", "acciones");
      [["Usar", "check", async () => {
        try {
          cerrar();
          await usarApi(api.nombre);
        } catch (error) {
          toast(error.message, "error");
        }
      }], ["Editar", "pencil", () => editar(api)], ["Eliminar", "trash-2", () => eliminar(api)]].forEach(([texto, nombreIcono, accion]) => {
        const boton = crear("button", "boton" + (texto === "Eliminar" ? " boton-fantasma" : ""));
        boton.type = "button";
        boton.append(icono(nombreIcono), crear("span", "", texto));
        boton.addEventListener("click", accion);
        botones.append(boton);
      });
      tarjeta.append(punto, crear("b", "", api.nombre), crear("small", "", api.proveedor + " · " + api.modelo), botones);
      return tarjeta;
    }));
  };
  rejilla.append(lista, formulario);
  raiz.append(banner, rejilla, crear("p", "nota", "Archivo: ~/.smartetl/apis.properties"));
  const cerrar = abrirModal("Gestionar APIs", raiz);
  limpiar();
  pintarApis();
}

export function iniciarIa() {
  $("iaProveedor").addEventListener("change", alCambiarProveedor);
  ["iaModelo", "iaUrl", "iaClave"].forEach((id) => $(id).addEventListener("input", marcarCambio));
  $("iaOjo").addEventListener("click", () => alternarOjo($("iaClave"), $("iaOjo")));
  $("iaValidar").addEventListener("click", validar);
  $("iaPlegar").addEventListener("click", () => plegar(!$("iaFormulario").hidden));
  $("iaGestionar").addEventListener("click", () => estado.config && gestionarApis());
  $("iaGuardada").addEventListener("change", async () => {
    if ($("iaGuardada").value) {
      try {
        await usarApi($("iaGuardada").value);
      } catch (error) {
        toast(error.message, "error");
      }
    }
  });
  $("iaManoAlzada").checked = manoAlzadaActiva();
  $("iaManoAlzada").addEventListener("change", () => {
    usarManoAlzada($("iaManoAlzada").checked);
    const item = estado.historial.find((h) => h.id === estado.abierto);
    if (item) {
      mostrarResultadoIa($("iaResultado"), item.resultado);
    }
  });
  document.querySelectorAll("#iaFunciones [data-funcion]").forEach((boton) => boton.addEventListener("click", () => ejecutarFuncion(boton.dataset.funcion)));
  $("iaDocumentacion").addEventListener("click", documentacion);
  autoAnimate($("iaHistorial"));
  document.addEventListener("tema", () => {
    const item = estado.historial.find((h) => h.id === estado.abierto);
    if (item) {
      mostrarResultadoIa($("iaResultado"), item.resultado);
    }
  });
  document.addEventListener("vista", (evento) => {
    if (evento.detail === "ia") {
      cargarHistorial();
    }
  });
  cargarConfig();
  cargarApis();
  cargarHistorial();
}
