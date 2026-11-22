# Migración a web: Fase 0 (seguridad y análisis)

Fecha: 13 de noviembre de 2026. Basado en `docs/ESTADO-ACTUAL.md`, `CLAUDE.md` y el código actual de `SmartETL_DS-main/`. **No se escribió código de la web ni se tocó el backend.**

---

## 1. Seguridad: respaldo sin Git (decidido)

**Decisión del dueño:** no se usa Git ni GitHub durante la migración. No se hacen `git init`, commits, tags ni push: los commits y la subida los hace el dueño.

En lugar de eso, el 13 de noviembre se crearon, **fuera** de `SmartETL_DS-main/` (en la carpeta `SmartETL/`), para que no entren en el repositorio:

| Archivo | Contenido |
|---|---|
| `respaldo-swing-aprobado-2026-11-13.zip` | Copia del proyecto completo tal como fue aprobado (380 archivos, 3,6 MB; sin `out/`) |
| `backend-swing-aprobado.sha256` | Huella SHA-256 de los 66 archivos de `src/model`, `estructuras`, `etl`, `algoritmos`, `grafo`, `ia` y `pruebas` |

Comprobar que el backend no cambió (reemplaza al `git diff swing-aprobado` de la Fase 4), desde `SmartETL_DS-main` en Git Bash:

```
sha256sum -c --quiet ../backend-swing-aprobado.sha256
```

Si no imprime nada, el backend está intacto. Un archivo nuevo en esas carpetas no lo detecta este comando; por eso en la Fase 4 también se compara la lista de archivos.

---

## 2. Confirmaciones del backend (sin modificarlo)

| Pregunta | Respuesta | Evidencia en el código |
|---|---|---|
| ¿`GeneradorPrompt.explicacion(tema, datos)` acepta texto libre como `datos`? | **Sí.** Solo concatena: `"…Datos reales:\n" + datos`. Puede recibir el código Mermaid | `ia/GeneradorPrompt.java:91-96` |
| ¿`Ordenamiento.burbuja` e `insercion` aceptan una `ListaSecuencial` con 12 a 20 aeropuertos? | **Sí.** Reciben `Lista<T>` y un `Comparator<T>`, copian la lista con `ListaSecuencial.copiaDe` y devuelven `ResultadoOrdenamiento` con comparaciones y movimientos | `algoritmos/Ordenamiento.java:13` y `:38` |
| ¿`Sesion` depende de Swing? | **No.** Solo importa `estructuras`, `etl`, `grafo`, `ia`, `model` y `java.io.IOException` | `app/Sesion.java:3-13` |

Detalles que afectan el diseño:

1. **Resumen bajo cada diagrama (8.5.12).** `AnalizadorIA.texto(funcion, prompt, textoLocal)` antepone `"Respaldo local (sin IA). Datos reales usados:"` al tercer parámetro cuando falla la API. Si se le pasa el Mermaid, el respaldo mostraría código crudo. Propuesta: la capa web pasa como `textoLocal` un texto corto armado a partir de las etiquetas del diagrama (por ejemplo, "El diagrama tiene 9 pasos: Inicio: BFS desde UIO → …"). Además, **cada resumen genera su propia evidencia** en `evidencias_ia/`: dos archivos por diagrama.
2. **El visualizador de ordenamiento puede coincidir exactamente con Java.** `Comparadores` usa `String.compareTo`, que compara por unidades UTF-16, igual que `<` y `>` entre strings en JavaScript. El JS debe contar igual que Java:
   - **burbuja:** una comparación por cada par visitado, corte cuando una pasada no hace intercambios, intercambio solo si `> 0`;
   - **inserción:** la comparación que detiene el desplazamiento también cuenta, y se detiene si `<= 0`.

   La muestra la elige el servidor (aleatoria con semilla, de los aeropuertos válidos), para que la animación y Java trabajen sobre la misma lista.
3. **Dibujo del BST (8.3.8).** `primerosNiveles(4, 8)` devuelve texto. Para dibujar el árbol, la capa web recorre `ArbolBST.getRaiz()` y los getters públicos de `NodoArbol` (no hay que cambiar el backend).
4. **Paginación de Datos (8.1.6).** Para no hacer `obtener(i)` por cada celda, la capa web recorre la `ListaEnlazada` con `primerNodo()`, que es público, una sola vez por página. La `ListaSecuencial` ya tiene acceso O(1).
5. **Tiempos por etapa del pipeline ETL (8.2.1).** `ProcesoETL` solo emite mensajes ("Extract: leyendo routes.csv…", "Transform de rutas: …"). La capa web mide el tiempo entre mensajes. El Load es una tarea aparte con su propio tiempo.
6. **Caso de eliminación del BST.** `ArbolBST.eliminar` no lo devuelve (solo `boolean`). Como pide el documento: solo antes y después.

---

## 3. Herramientas pedidas: disponibilidad real

| Herramienta | Estado en este entorno | Propuesta |
|---|---|---|
| Impeccable (`/impeccable`) | **No instalada** (no aparece entre los skills) | Opción A: la instalas y la uso. Opción B: escribo `PRODUCT.md` y `DESIGN.md` a mano con la misma estructura (dirección visual, paleta en claro y oscuro, tipografía, espaciado, movimiento) y hago la auditoría con lista propia |
| frontend-design (respaldo) | **No instalada** | Igual que la opción B |
| emil-design-eng | **No instalada** | Aplico sus reglas conocidas: easing `ease-out` para entradas, 150/300 ms, nada bloquea, `prefers-reduced-motion` |
| ui-ux-pro-max | **No instalada** | Criterios propios de accesibilidad: contraste AA, foco visible, roles ARIA en pestañas y modales |
| Playwright MCP | **No disponible** | Uso el **navegador integrado** de la app (`mcp__Claude_Browser__*`): navegar, hacer clic, escribir, capturas, leer la consola, viewport de escritorio y móvil. Cubre lo que pide la Fase 4 |
| `git`, Node (solo para descargar librerías de npm) | Disponibles | |

**Librerías que propongo descargar** (versión fija, archivos locales en `recursos/` y licencia en `recursos/licencias/`):

| Librería | Versión propuesta | Archivo |
|---|---|---|
| Anime.js | 4.x (la última estable de la rama 4 al descargar) | Bundle ESM |
| AutoAnimate (`@formkit/auto-animate`) | 0.8.x | Bundle ESM |
| Chart.js | 4.4.x | `chart.umd.min.js` |
| Lucide (íconos) | Versión fija; solo los SVG usados, copiados a `recursos/iconos/` | SVG |

La versión exacta se fija y se anota en la Fase 1, comprobando que el archivo descargado funcione sin CDN.

---

## 4. Tabla de mapeo Swing → web

Leyenda: **T** = tarea en segundo plano (POST devuelve un id; avance por SSE; una a la vez, 409 si ya hay otra); **S** = respuesta directa (rápida).

### 4.0 Arranque y elementos globales

| Swing | Backend | Endpoint | En la web |
|---|---|---|---|
| `Main.main` (detecta la carpeta) | `Proyecto.detectar`, `usarCarpeta` | — (antes de arrancar el servidor) | `app.Main` sin argumentos: detecta la carpeta → levanta el servidor en `127.0.0.1:7070` (o el siguiente puerto libre) → `Desktop.browse`. Con `--swing`, arranca la versión actual |
| Diálogo "Ubicar los datos" y `JFileChooser` de carpetas | `Proyecto.usarCarpeta` | — | Ver decisión D3 |
| Título con la carpeta | `Proyecto.getBase` | `GET /api/estado` | Ruta en pequeño en el encabezado |
| Barra de estado y `Animacion` "⏳ … N s" | Avisos `Consumer<String>` | SSE `GET /api/tareas/{id}/eventos` | Overlay de carga con etapas, ✓/○ y contador (7.1) |
| Diálogo de error "No se pudo completar" (`Tarea.mostrarError`) | `Main.explicacionCarpeta` | JSON `{error}` | Toast de error; si falta un archivo, toast largo con la explicación de la carpeta |
| Cursor de espera; botones siempre activos | — | — | Botones bloqueados mientras hay una tarea; el servidor responde 409 si llega otra |

### 4.1 Pestaña Datos (`PanelDatos`)

| Swing | Backend | Endpoint | En la web |
|---|---|---|---|
| `cmbFuente` (Aeropuertos, Aerolíneas, Rutas) | `FuenteDatos.todas`, `getArchivoPorDefecto`, `getEncabezados` | `GET /api/datos/fuentes` S | Selector segmentado con ícono |
| `txtArchivo` y botón "Examinar..." | — | `GET /api/datos/archivos` S | Selector con los CSV de `data/` y `data/demo/` (ver D2) |
| `cmbLista` (enlazada o secuencial) | `new ListaEnlazada` / `new ListaSecuencial` | (parámetro de cargar) | Interruptor de 2 opciones |
| `cmbModo` y los campos N, Desde, Hasta, Semilla | `OpcionesCarga.todos`, `primeros`, `rango`, `aleatorio` | (parámetro de cargar) | Los campos aparecen solo cuando el modo los usa; validación en el campo |
| Botón "Cargar" | `Extract.extraer(Proyecto.ruta(…), …)` | `POST /api/datos/cargar` S | Chips `Extract X ms · N cargadas · M no leídas · estructura` y la página 1 |
| `JTable` + `ModeloTablaRegistros` + `RenderizadorCeldas` | `Lista.obtener` (hoy, una vez por celda) | `GET /api/datos/pagina?n=` S | Tabla paginada de 50 filas, encabezado fijo, filas alternadas, `\N` como etiqueta roja y vacío como etiqueta amarilla, con leyenda |
| `cmbColumna`, `txtBusqueda`, botón "Buscar (primera coincidencia)" (y Enter) | `Lista.buscar(Criterio)` | `POST /api/datos/buscar` S | Salta a la página de la fila, la resalta y muestra `Posición · comparaciones · ms` |
| Botón "Filtrar (contiene)" | `Lista.filtrar` | `POST /api/datos/filtrar` S | Tabla filtrada con un chip "Filtro activo ✕" |
| Botón "Ver todos" | — | `POST /api/datos/todos` S | Quita el filtro |
| Diálogo "Opciones de carga no válidas" o "No se pudo leer el archivo" | `IllegalArgumentException`, `IOException` | 400 / 404 `{error}` | Error debajo del campo y toast |
| Etiquetas de estado, conteo, consulta y leyenda | `ResultadoExtraccion` | (dentro de las respuestas) | Chips y subtítulo "Exploración del archivo crudo (no alimenta el ETL)" |

### 4.2 Pestaña ETL (`PanelETL`)

| Swing | Backend | Endpoint | En la web |
|---|---|---|---|
| Botón "Ejecutar ETL (datos reales)" | `ProcesoETL.real`, `setAvisos`, `ejecutar`, `Sesion.registrarEtl(…, true)` | `POST /api/etl/ejecutar {modo:"real"}` T | Pipeline animado por SSE; chip "Fuente: archivos completos"; overlay solo sobre los resultados |
| Botón "Ejecutar demostración" | `ProcesoETL.demostracion`, `registrarEtl(…, false)` | `POST /api/etl/ejecutar {modo:"demo"}` T | Igual, con el chip `DEMO` |
| Botón "Cargar (Load a output/)" | `ProcesoETL.cargar("output")` → `Load.escribir` | `POST /api/etl/cargar` T | Deshabilitado hasta correr el ETL; luego lista de 5 archivos con tamaño y enlace |
| — | — | `GET /api/output/{archivo}` S | Ver un archivo de `output/` (solo esos 5 nombres; texto paginado si es grande) |
| `txtCola` (primeros 20 por fuente) | `getVistaCola(f)`, `getEncolados(f)` | `GET /api/etl/resultado` S | Cola dibujada como cajas en fila con `frente →`, pestañas por fuente |
| `txtValidos` (primeros 10) | `DatosLimpios.getAeropuertos`, `getRutas`, `getAerolineas` | `GET /api/etl/validos?fuente=&pagina=` S | Tabla paginada por fuente |
| `txtErrores` (primeros 60 desde la cima) | `getTransform().getErrores().elementos()` | `GET /api/etl/errores?tipo=&pagina=` S | Pila vertical con la cima arriba, tarjetas con color por tipo y filtro por tipo |
| `txtContadores` (reporte) | `ProcesoETL.reporte` → `Load.textoReporte`; `getProcesados`, `getValidos`, `getInvalidos`, `getErrores(f, tipo)` | (dentro de `/api/etl/resultado`) | KPIs, tabla por fuente, dona válidos/inválidos, barras por tipo, "Ver reporte completo" desplegable |
| (nuevo) "✨ Resumen con IA" / "✨ Flujograma con IA" | `AnalizadorIA.texto` / `diagrama` | El mismo `POST /api/ia/funcion` T | Resultado dentro de ETL con el chip IA/RESPALDO |

### 4.3 Pestaña Estructuras (`PanelEstructuras`)

| Swing | Backend | Endpoint | En la web |
|---|---|---|---|
| `cmbAlgoritmo`, `cmbCampo`, botón "Ordenar" | `Ordenamiento.burbuja`/`insercion(datos.getAeropuertos(), Comparadores.porCampo)` | `POST /api/estructuras/ordenar` T | KPIs y tabla de los primeros 15 |
| (nuevo) "Comparar burbuja vs inserción" | Las dos llamadas anteriores | `POST /api/estructuras/comparar-ordenamientos` T | Barras de comparaciones y ms |
| (nuevo) Visualizador de ordenamiento | `Ordenamiento` sobre una `ListaSecuencial` de 12 o 20 | `POST /api/estructuras/muestra {cantidad, campo, algoritmo}` S | Animación JS (reproducir, pausa, paso a paso, velocidad) y validación "Animación: N · Java: N ✓" |
| `txtBuscar`, botón "Secuencial vs binaria vs BST" | `Busqueda.secuencial(sesion.listaEnlazada())`, `binaria(ordenadaPorIata)`, `porArbol(datos.getArbolIata())`, `caminoHacia` | `POST /api/estructuras/buscar {iata}` S (T la primera vez si falta el ETL o la lista ordenada) | Barras de comparaciones con ms, camino del BST dibujado en vertical, tarjeta sobre la binaria |
| Botón "Construir" | `new ArbolBST(AEROPUERTO_POR_IATA)` + `insertar` | `POST /api/estructuras/bst/construir` S | Chip `Copia de trabajo: N nodos · altura H`; habilita las operaciones |
| `txtClave` y los botones Insertar, Buscar, Eliminar | `insertar`, `buscar`, `eliminar`, `caminoHacia`, `contarNodos`, `altura` | `POST /api/estructuras/bst/{operacion} {iata}` S | Camino dibujado y chip antes → después |
| `cmbRecorrido`, `txtLimite`, botón "Recorrer" | `inOrden`/`preOrden`/`postOrden(limite)` | `GET /api/estructuras/bst/recorrido?tipo=&limite=` S | Chips IATA que aparecen uno por uno |
| Botón "Altura y nodos" | `contarNodos`, `altura`, `promedioComparaciones`; `getRaiz()` para el dibujo | `GET /api/estructuras/bst/medidas` S | Tarjetas y los primeros 4 niveles dibujados como árbol |
| Botón "BST por ID ascendente vs BST por IATA" | `ArbolBST(AEROPUERTO_POR_ID)` con 1.500 aeropuertos; `datos.getArbolIata()` | `POST /api/estructuras/desbalance` S | Lado a lado: cadena contra árbol, barras de altura |
| Texto de ayuda sobre la binaria | — | — | Tarjeta informativa |

### 4.4 Pestaña Grafo (`PanelGrafo`)

| Swing | Backend | Endpoint | En la web |
|---|---|---|---|
| `lstPaises` (se carga al mostrar la pestaña) | `sesion.mapa()`, `Ordenamiento.insercion(mapa.getPaises(), GrafoPais.POR_NOMBRE)` | `GET /api/grafo/paises` S (T si falta el ETL) | Buscador, casillas, chip `N seleccionados`, ✓ en los construidos |
| Botón "Construir seleccionados" | `mapa.obtener(nombre)`, `GrafoPais.resumen` | `POST /api/grafo/construir {paises}` S | Tarjetas por país |
| Botón "Construir todos (con tiempo)" | `mapa.construirTodos`, `paisesConVertices` | `POST /api/grafo/construir-todos` T | Overlay y KPIs: países, vértices, aristas, ms |
| `txtAeropuerto`, botón "Ver conexiones" | `verticePorIata`, `getAdyacentes`, `GrafoPais.externosPorPais(v)`, `getLlegadasExternas` | `GET /api/grafo/conexiones?iata=` S | Grafo estrella en vis-network: internas alrededor y externas agrupadas por país |
| `txtOrigen`, `txtNivelMaximo`, `txtNivelDiagrama`, `chkFronteras`, botones BFS y DFS | `BFS.recorrer`/`DFS.recorrer`, `sesion.registrarBfs`/`registrarDfs` | `POST /api/grafo/recorrido {algoritmo, origen, nivelMaximo, nivelDiagrama, fronteras}` S | Grafo animado (BFS por capas con color por nivel; DFS numerado), barras por nivel, rótulo "Orden de visita real"; ⓘ con ayuda en los niveles |
| `txtRutaOrigen`, `txtRutaDestino`, "Buscar ruta" | `Rutas.buscar` | `GET /api/grafo/ruta?desde=&hasta=` S | Cadena `UIO ✈ LIM ✈ CUZ · 2 vuelos`, resaltada sobre un mini grafo |
| `txtPaisCiclo`, "Detectar ciclo" | `Ciclos.detectar(mapa.obtener(pais).getGrafo())` | `GET /api/grafo/ciclo?pais=` S | Anillo con flechas |
| "Diagrama general con clic por país" | `GeneradorFlujogramaLocal.grafoPais`, `generalPaises`, `VisorHtml.escribirPagina`, `escribirDiagrama` (sin `abrir`) | `POST /api/grafo/diagrama-general {nivelDiagrama}` T | Se muestra en un `iframe` de `/diagramas/ver_general_paises.html`; el clic en un país navega dentro del mismo `iframe` |
| "Abrir visor interactivo" | `mapa.construirTodos`, `VisorGrafo.escribir(mapa, origen)` (sin `abrir`) | `POST /api/grafo/visor {origen}` T | `iframe` de `/diagramas/visor_grafo.html` |
| `txtSalida` | — | — | Se reemplaza por las vistas gráficas de cada acción |

### 4.5 Pestaña IA (`PanelIA`) y Gestionar APIs (`PanelGuardarApis`)

| Swing | Backend | Endpoint | En la web |
|---|---|---|---|
| `cmbProveedor`, `txtModelo`, `txtUrl`, `cmbModo`, `lblAyuda` | `ConfiguracionIA.PROVEEDORES`, `urlDe`, `ayudaModelo`, `ayudaClave`, `setProveedor`, `setModelo`, `setUrl`, `setModo` | `GET /api/ia/config` S y `POST /api/ia/config` S (409 si hay una tarea) | Tarjeta "Conexión" plegable; ayuda debajo de cada campo |
| `txtClave` (`JPasswordField`) y `cargarClaveDeEntorno` | `claveDeEntorno`, `variableDeEntorno`, `setClave` | La clave solo viaja en `POST /api/ia/config` y nunca vuelve | Campo con ojo. Ver D1 para la variable de entorno |
| `cmbGuardadas` ("API guardada") | `AlmacenApis.buscar` → `ConfiguracionIA` | `POST /api/apis/{nombre}/usar` S | Desplegable o el botón "Usar" de la tarjeta; la clave se carga solo en el servidor |
| Botón "Validar API" y `lblApi` | `AnalizadorIA.validarConexion` | `POST /api/ia/validar` T | Indicador gris → amarillo con spinner → ✓ verde o ✗ rojo con el error; también en el encabezado de la app |
| Flujograma ETL, Cola, Pila, Lista enlazada, BST, BFS, DFS | `diagramaEtl`, `diagramaEstructura`, `diagramaRecorrido` → `AnalizadorIA.diagrama` (con `GeneradorPrompt` y `GeneradorFlujogramaLocal`) | `POST /api/ia/funcion {funcion, incluirResumen}` T | Mermaid dentro de la página con zoom; pestañas Diagrama y Código; chips IA/RESPALDO y coherencia; "Ver evidencia"; resumen debajo |
| Explicar árbol, Explicar grafo, Analizar errores ETL | `textoArbol`, `textoGrafo`, `textoErrores` → `AnalizadorIA.texto` | `POST /api/ia/funcion` T | Tarjeta legible con botón copiar |
| "Generar documentación" | Las 10 funciones + `escribirDocumentacion` | `POST /api/ia/documentacion` T | Overlay con las 10 (✓ / ◌ / ○), `Documentación k de 10`, enlace a `DOCUMENTACION.md` |
| `txtResultado`, `lblOrigen`, `lblAviso`, `lblCoherencia`, `lblEstado` | `ResultadoIA` | (en el resultado de la tarea) | Chips, aviso como toast y vista del resultado |
| (nuevo) Historial | — | `GET /api/ia/historial` S | Lista lateral; reabre sin volver a llamar a la API |
| — | — | `GET /api/evidencias/{archivo}` S | Ver la evidencia (solo nombres dentro de `evidencias_ia/`) |
| `cmbVista`, "Ver diagrama", mermaid.ink, `JDialog` con zoom | `VisorHtml.abrir`, `pedirImagen` | — | **Se eliminan en la web** (8.5.6); siguen en `--swing` |
| `PanelGuardarApis`: `lstApis` | `AlmacenApis.getApis`, `ApiGuardada.descripcion` | `GET /api/apis` S (nombre, proveedor, modelo, url; **sin la clave**) | Tarjetas con punto de color, Usar · Editar · Eliminar; estado vacío |
| Botón Guardar (actualiza si el nombre ya existe) | `AlmacenApis.guardar` (si la clave llega vacía, se conserva la anterior con `buscar`) | `POST /api/apis` S | Formulario en un panel lateral; validación en el campo; "Guardar y validar" (+ `POST /api/ia/validar`) |
| Botón Eliminar + `showConfirmDialog` | `AlmacenApis.eliminar` | `DELETE /api/apis/{nombre}` S | Modal propio de confirmación |
| "Cargar en la pestaña IA" y doble clic | `PanelIA.cargarApi` | `POST /api/apis/{nombre}/usar` S | Botón "Usar" |
| `chkMostrar`, Limpiar, aviso amarillo | — | — | Ojo en el campo, botón Limpiar, banner ⚠ |

### 4.6 Reporte (nuevo)

| Contenido | Fuente | Endpoint | En la web |
|---|---|---|---|
| Portada, ETL, estructuras, grafo, IA | Resultados guardados por la capa web durante la sesión | `GET /api/reporte` S | Página A4 con `@media print`; "Descargar PDF" con `window.print()`; "No ejecutado en esta sesión" donde falte |

---

## 5. Endpoints que voy a crear

Comunes:
- `GET /` → `web/index.html`; estáticos `GET /web/*`, `/recursos/*`, `/diagramas/*`. `../recursos/*.js` sigue funcionando desde `/diagramas/*`.
- `GET /api/estado`: carpeta, puerto, si hay una tarea en curso, si hay ETL, estado de la IA (sin la clave).
- `GET /api/tareas/{id}/eventos` (SSE): eventos `aviso`, `resultado` y `error`.
- Errores: `{ "error": "…" }` con 400, 404, 409 o 500.

Datos: `GET /api/datos/fuentes`, `GET /api/datos/archivos`, `POST /api/datos/cargar`, `GET /api/datos/pagina`, `POST /api/datos/buscar`, `POST /api/datos/filtrar`, `POST /api/datos/todos`.

ETL: `POST /api/etl/ejecutar`, `POST /api/etl/cargar`, `GET /api/etl/resultado`, `GET /api/etl/validos`, `GET /api/etl/errores`, `GET /api/output/{archivo}`.

Estructuras: `POST /api/estructuras/ordenar`, `POST /api/estructuras/comparar-ordenamientos`, `POST /api/estructuras/muestra`, `POST /api/estructuras/buscar`, `POST /api/estructuras/bst/construir`, `POST /api/estructuras/bst/insertar`, `POST /api/estructuras/bst/buscar`, `POST /api/estructuras/bst/eliminar`, `GET /api/estructuras/bst/recorrido`, `GET /api/estructuras/bst/medidas`, `POST /api/estructuras/desbalance`.

Grafo: `GET /api/grafo/paises`, `POST /api/grafo/construir`, `POST /api/grafo/construir-todos`, `GET /api/grafo/conexiones`, `POST /api/grafo/recorrido`, `GET /api/grafo/ruta`, `GET /api/grafo/ciclo`, `POST /api/grafo/diagrama-general`, `POST /api/grafo/visor`.

IA: `GET /api/ia/config`, `POST /api/ia/config`, `POST /api/ia/validar`, `POST /api/ia/funcion`, `POST /api/ia/documentacion`, `GET /api/ia/historial`, `GET /api/evidencias/{archivo}`.

APIs: `GET /api/apis`, `POST /api/apis`, `DELETE /api/apis/{nombre}`, `POST /api/apis/{nombre}/usar`.

Reporte: `GET /api/reporte`.

**Protecciones de la capa web** (agregadas por mí; no cambian ninguna funcionalidad):
- Escucha solo en `127.0.0.1`.
- Rechaza peticiones cuyo `Host` no sea `127.0.0.1:<puerto>` o `localhost:<puerto>`, para evitar que otra página lo alcance renombrando su dominio.
- Exige la cabecera `X-SmartETL: 1` en todo POST y DELETE, para que otra pestaña del navegador no pueda disparar acciones (por ejemplo, gastar créditos de la IA o borrar APIs).
- Los archivos estáticos y los visores de `output/` y `evidencias_ia/` solo sirven nombres dentro de su carpeta (nada de `../`).

**Cómo encaja con las reglas:**
- Registro de tareas, cola de eventos SSE y listas de resultados con `ListaEnlazada`, `Cola` y arreglos propios, sincronizados con `synchronized`/`wait`/`notify`.
- De `HttpServer` solo se usan los `Headers` que exige su API.
- Una sola tarea a la vez, protegida en un solo lugar: eso también resuelve que `ConfiguracionIA` cambie mientras corre una llamada.
- JSON con `ia.Json.cadena` para escapar, más un ayudante `app.web.RespuestaJson` para armar objetos y listas. Leer el cuerpo de las peticiones también hace falta: los cuerpos de los POST son pequeños y planos, y `Json.valorDeCadena` solo lee strings, así que el ayudante web agrega la lectura de números, booleanos y listas de strings.

---

## 6. Decisiones que necesito de ti

| # | Tema | Contradicción o hueco | Propuesta |
|---|---|---|---|
| D1 | Clave desde la variable de entorno (8.5.2) frente a "la clave nunca llega al navegador" (sección 9) | Rellenar el campo del navegador con la variable enviaría la clave | El servidor usa la variable si el campo llega vacío y la variable existe; el navegador solo muestra el chip "Usando GEMINI_API_KEY del sistema", nunca el valor |
| D2 | "Examinar..." de la pestaña Datos | El navegador no puede explorar las carpetas del servidor | Selector con los CSV de `data/` y `data/demo/`, más un campo opcional para escribir una ruta |
| D3 | Carpeta sin `data/airports.csv` al arrancar | Hoy lo resuelve un `JFileChooser` de Swing | Mantener ese mismo diálogo nativo antes de levantar el servidor (ya existe y funciona en Windows). Alternativa: una página web "Ubicar los datos" con un campo de ruta |
| D4 | Herramientas | Impeccable, emil-design-eng, ui-ux-pro-max y Playwright no están instaladas | A: las instalas y las uso. B: sigo con `PRODUCT.md` y `DESIGN.md` escritos a mano y el navegador integrado para las capturas y la consola |
| D5 | Git | **Decidido:** sin Git ni GitHub; el dueño hace los commits | Respaldo zip y manifiesto SHA-256 fuera del proyecto (sección 1) |
| D6 | Mover Swing a `app.swing` | Cambia el `package` e imports de 10 clases (`VentanaPrincipal`, `Tarea`, `Animacion`, `ModeloTablaRegistros` y los 6 paneles). `Main` queda en `app` como lanzador (`--swing` delega en `app.swing`) y `Sesion` queda en `app` (no depende de Swing). `Tarea` seguirá llamando a `app.Main.explicacionCarpeta` | Confirmar que está bien que `Main` y `Sesion` se queden en `app` |

---

## 7. Qué se toca y qué no

- **No se toca:** `src/model`, `src/estructuras`, `src/etl`, `src/algoritmos`, `src/grafo`, `src/ia`, `src/pruebas`. Al final, `sha256sum -c ../backend-swing-aprobado.sha256` debe salir limpio y no debe haber archivos nuevos en esas carpetas.
- **Cambia:**
  - `src/app/Main.java` (lanzador);
  - las clases Swing pasan a `src/app/swing/` (solo `package` e imports);
  - nuevos `src/app/web/*`, `web/*`, `recursos/*` (nuevas librerías y licencias);
  - `README.md`, `PRODUCT.md`, `DESIGN.md`, `docs/GUIA-INTERFAZ-WEB.md`.
- **Riesgo a vigilar:** `Sesion` usa métodos `synchronized`. Una tarea larga (el ETL implícito de `datosReales()`) bloquea a otras peticiones que llamen a `Sesion`. Con "una tarea a la vez" no hay conflicto, pero las consultas rápidas que usan `Sesion` mientras corre una tarea esperarán. Se puede mostrar el overlay también en esos casos.

---

## 8. Siguiente paso

Con tu aprobación de esta Fase 0 (decisiones D1 a D4 y D6; D5 ya está resuelta), empiezo la **Fase 1**: mover Swing a `app.swing`, crear el servidor con estáticos, SSE y tareas, y el esqueleto visual con las 5 pestañas y el Reporte.
