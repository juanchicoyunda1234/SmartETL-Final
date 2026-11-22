# Guía de la interfaz web de SmartETL-DS

Esta guía es para el grupo. Explica cómo funciona la interfaz web en lenguaje sencillo, para que cualquiera pueda contestar en la defensa.

## 1. La idea en una frase

El mismo programa Java de siempre levanta un **servidor local** en `http://127.0.0.1:7070` y abre el navegador. La página web solo **pide datos y dibuja**; todo el trabajo (ETL, estructuras, grafo, IA) lo sigue haciendo el **backend que ya teníamos**, sin cambios.

```
Navegador (web/)  ──fetch──▶  Servidor Java (src/app/web/)  ──llama──▶  Backend (model, estructuras, etl, algoritmos, grafo, ia)
        ▲                                │
        └──────────── JSON ◀─────────────┘
```

- No hay Spring, Node ni npm. El servidor es `com.sun.net.httpserver.HttpServer`, que viene en el JDK.
- La página es HTML, CSS y JavaScript sin compilar. Las librerías están copiadas en `recursos/`, así que funciona sin internet.
- `java -cp out app.Main` abre la web. `java -cp out app.Main --swing` abre la ventana Swing anterior, que sigue intacta en `src/app/swing/`.

## 2. Ejemplo real: buscar UIO en tres estructuras

En **Estructuras › Búsquedas** escribes `UIO` y pulsas el botón. Pasa esto:

1. **Navegador** (`web/js/estructuras.js`, función `buscar`):
   ```js
   pedir("/api/estructuras/buscar", { metodo: "POST", cuerpo: { iata: "UIO" } })
   ```
   `pedir` (en `web/js/api.js`) es un `fetch` que agrega la cabecera `X-SmartETL: 1` y convierte los errores en mensajes.
2. **Servidor** (`src/app/web/Api.java`) recibe `POST /api/estructuras/buscar` y se lo pasa a `ApiEstructuras`.
3. **Backend**: `ApiEstructuras.buscar` llama a lo que ya existía:
   - búsqueda secuencial sobre la `ListaEnlazada` de aeropuertos;
   - búsqueda binaria sobre la `ListaSecuencial` ordenada por IATA;
   - `ArbolBST.buscar` sobre el árbol por IATA, guardando el camino.
4. **JSON de vuelta** (recortado):
   ```json
   {
     "iata": "UIO", "encontrado": true, "elementos": 6071,
     "secuencial": { "comparaciones": 2556, "posicion": 2556 },
     "binaria": { "comparaciones": 13 },
     "bst": { "comparaciones": 13, "profundidad": 12 },
     "camino": [ { "codigo": "GKA", "lado": "derecha" }, "…", { "codigo": "UIO", "lado": "encontrado" } ]
   }
   ```
5. **Pantalla**: `pintarBusqueda` dibuja el gráfico de barras en escala logarítmica (Chart.js), las tres métricas y el camino en el BST, nodo por nodo.

Todas las pestañas funcionan igual: un botón → `fetch` → un endpoint → el backend → JSON → se dibuja.

## 3. Tareas largas: SSE y el panel de carga

Algunas operaciones tardan (ETL real, ordenar 7.698 aeropuertos, construir todos los grafos, llamar a la IA). Para no dejar la pantalla congelada:

1. El navegador hace `POST /api/etl/ejecutar`. El servidor **no espera**: crea una tarea en otro hilo y responde de inmediato `202 {"id": "t1"}`.
2. El navegador abre `GET /api/tareas/t1/eventos`. Es **SSE** (Server-Sent Events): una conexión que queda abierta y por la que el servidor va mandando mensajes:
   - `aviso`: "Extract: leyendo airports.csv", "Transform: validando rutas"… (son los avisos que el backend ya emitía);
   - `resultado`: el JSON final;
   - `fallo`: el error, si algo salió mal.
3. **El panel de carga** (`web/js/interfaz.js`, clase `Carga`):
   - aparece solo si la tarea dura más de **500 ms** (si es rápida, no parpadea);
   - después de **3 s** muestra un reloj;
   - cada aviso marca una etapa: ○ pendiente → ◌ en curso → ✓ hecha, con su tiempo en ms.
4. **Una tarea a la vez** (`GestorTareas`). Si llega otra mientras una corre, el servidor responde **409** y la página muestra un aviso. Además, los botones se desactivan mientras hay una tarea.

¿Por qué una a la vez? El backend guarda estado compartido (por ejemplo, las marcas de "visitado" de cada vértice del grafo). Dos recorridos al mismo tiempo se pisarían. Por eso también las operaciones rápidas del grafo esperan su turno con un candado (`ContextoWeb.getCandadoGrafo()`).

## 4. Seguridad (corta)

- El servidor escucha solo en `127.0.0.1`: nadie más en la red puede entrar.
- Rechaza peticiones con otra cabecera `Host` y exige `X-SmartETL: 1` en POST y DELETE, para que otra página web no pueda usar el servidor a escondidas.
- Los archivos estáticos solo se sirven desde `web/`, `recursos/` y `diagramas/` (no se puede pedir `../`).
- **Las claves de API nunca llegan al navegador.** Viajan una sola vez del formulario al servidor (POST) y quedan en su memoria. Ningún endpoint las devuelve. Si el campo queda vacío, el servidor usa la variable de entorno (`GEMINI_API_KEY`, etc.); el navegador solo ve el nombre de la variable.

## 5. Qué hace cada librería del frontend

| Librería | Dónde se nota | Para qué |
|---|---|---|
| **Anime.js** | Pipeline ETL, cola que entra, camino del BST, recorridos, números de los KPI, ciclo | Animaciones que explican algo; todas se apagan si el sistema pide "reducir movimiento" |
| **AutoAnimate** | Pila de errores, historial de IA, APIs guardadas | Las listas se reacomodan suavemente al cambiar |
| **Chart.js** | Gráficos de ETL, ordenamiento, búsquedas, BFS y reporte | Barras y dona |
| **Lucide** | Todos los íconos | Íconos SVG (solo los que se usan) |
| **Mermaid + svg-pan-zoom** | Pestaña IA, reporte | Dibujan los flujogramas con zoom y arrastre |
| **vis-network** | Pestaña Grafo | Grafo estrella, BFS/DFS animado, ruta y visor interactivo |

## 6. Mapa de archivos

| Pestaña | Página (`web/js/`) | Servidor (`src/app/web/`) |
|---|---|---|
| Datos | `datos.js` | `ApiDatos` |
| ETL | `etl.js` | `ApiEtl` |
| Estructuras | `estructuras.js` | `ApiEstructuras` |
| Grafo | `grafo.js` | `ApiGrafo` |
| IA y Gestionar APIs | `ia.js`, `resultadoIa.js` | `ApiIa`, `ApiApis`, `FuncionesIa` |
| Reporte | `reporte.js` | `ApiGeneral` (`/api/reporte`), `ReporteSesion` |
| Común | `app.js` (pestañas, tema), `api.js` (fetch y SSE), `interfaz.js` (panel de carga, toasts, modal) | `ServidorWeb`, `Api`, `Seguridad`, `Estaticos`, `GestorTareas`, `TareaWeb` |

## 7. Preguntas probables de la defensa

**¿La animación del ordenamiento es su código?**
Sí. `estructuras.js` (función `generarPasos`) repite burbuja e inserción paso a paso en una muestra de 12 o 20 aeropuertos. Al terminar, compara sus conteos con los que calculó **Java** (`Ordenamiento`) sobre la misma muestra, y muestra `Animación: N comparaciones · Java: N comparaciones ✓`. Si no coincidieran, saldría ✗.

**¿El BFS/DFS animado es el recorrido real?**
Sí. El orden lo calcula `BFS.recorrer` / `DFS.recorrer` en Java con nuestra Cola y nuestra Pila. El navegador solo dibuja los primeros 160 aeropuertos en ese orden. No mostramos la cola o la pila por dentro porque el backend no las expone; por eso el rótulo dice "Orden de visita real del BFS/DFS".

**¿Cambiaron el backend para hacer la web?**
No. `model`, `estructuras`, `etl`, `algoritmos`, `grafo`, `ia` y `pruebas` están igual: lo comprobamos con un archivo de sumas SHA-256 del backend aprobado, y las pruebas siguen en `fallaron: 0`. La web vive en `src/app/web/` y `web/`.

**¿Por qué no usaron Spring, Node o React?**
La regla del proyecto es solo JDK, sin librerías de Java. `HttpServer` viene en el JDK. En el frontend no hay paso de compilación: HTML, CSS y JavaScript que se abren tal cual.

**¿Usan ArrayList o HashMap en la web?**
No. La capa web usa nuestras estructuras (`ListaSecuencial`, `ListaEnlazada`) y arma el JSON a mano (`ObjetoJson`, `ArregloJson`, `LectorJson`).

**¿Qué pasa si no hay internet o la clave está mal?**
Todo funciona igual: la IA cae al **respaldo local** (`GeneradorFlujogramaLocal`), aparece el chip `RESPALDO LOCAL` y un aviso con el motivo (por ejemplo `HTTP 400: Please pass a valid API key`). Las librerías del frontend están en `recursos/`, no en internet.

**¿Dónde queda la clave de API?**
En la memoria del servidor (`ConfiguracionIA` de la `Sesion`). Si la guardas en "Gestionar APIs", va a `~/.smartetl/apis.properties` en tu computador (fuera del repositorio). Nunca va a `evidencias_ia/`, al HTML ni al navegador.

**¿Por qué el diagrama de la página se ve con colores y el `.md` no?**
El "vestido" (colores por tipo, emojis, flechas animadas) se aplica solo al dibujar en la página. El `.md` de `diagramas/`, la evidencia y la comprobación de coherencia usan el código Mermaid original de la IA o del respaldo.

**¿Cómo se genera el PDF?**
La pestaña Reporte arma una hoja A4 con lo que se ejecutó en la sesión. "Descargar PDF" llama a `window.print()` y el navegador lo guarda como PDF. Sin librerías. Lo que no se ejecutó dice "No ejecutado en esta sesión".

**¿Qué es SSE y por qué no WebSocket?**
SSE es una conexión HTTP normal que el servidor mantiene abierta para mandar mensajes en un solo sentido. Nos basta, porque solo el servidor avisa el progreso. WebSocket necesitaría más código y no viene listo en `HttpServer`.

**¿Qué pasa si cierro el navegador a mitad del ETL?**
La tarea sigue en el servidor. Si vuelves a abrir la página, ves que hay una tarea en curso y los botones quedan desactivados hasta que termine. Para detener todo, se cierra la consola (Ctrl+C).

**¿Qué pasa si el puerto 7070 está ocupado?**
El servidor prueba 7071, 7072… hasta 7079 y abre el navegador en el que consiguió.
