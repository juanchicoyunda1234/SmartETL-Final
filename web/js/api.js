export async function pedir(ruta, opciones = {}) {
  const metodo = opciones.metodo || "GET";
  const configuracion = { method: metodo, headers: {} };
  if (metodo !== "GET") {
    configuracion.headers["X-SmartETL"] = "1";
  }
  if (opciones.cuerpo !== undefined) {
    configuracion.headers["Content-Type"] = "application/json";
    configuracion.body = JSON.stringify(opciones.cuerpo);
  }
  let respuesta;
  try {
    respuesta = await fetch(ruta, configuracion);
  } catch (falla) {
    throw crearError("No hay conexión con el servidor. ¿Sigue abierta la consola de SmartETL-DS?", 0);
  }
  const datos = await respuesta.json().catch(() => ({}));
  if (!respuesta.ok) {
    throw crearError(datos.error || "Error " + respuesta.status, respuesta.status, datos.campo);
  }
  return datos;
}

export function seguirTarea(id, alAviso) {
  return new Promise((resolver, rechazar) => {
    const fuente = new EventSource("/api/tareas/" + encodeURIComponent(id) + "/eventos");
    fuente.addEventListener("aviso", (evento) => alAviso(JSON.parse(evento.data)));
    fuente.addEventListener("resultado", (evento) => {
      fuente.close();
      resolver(JSON.parse(evento.data));
    });
    fuente.addEventListener("fallo", (evento) => {
      fuente.close();
      const datos = JSON.parse(evento.data);
      rechazar(crearError(datos.error, datos.codigo));
    });
    fuente.onerror = () => {
      fuente.close();
      esperarFin(id).then(resolver, rechazar);
    };
  });
}

async function esperarFin(id) {
  while (true) {
    const tarea = await pedir("/api/tareas/" + encodeURIComponent(id));
    if (tarea.terminada) {
      if (tarea.error) {
        throw crearError(tarea.error, 500);
      }
      return { ms: tarea.ms, datos: tarea.resultado };
    }
    await new Promise((listo) => setTimeout(listo, 700));
  }
}

function crearError(mensaje, codigo, campo) {
  const error = new Error(mensaje);
  error.codigo = codigo;
  error.campo = campo || "";
  return error;
}
