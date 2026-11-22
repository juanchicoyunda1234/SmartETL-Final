# SmartETL-DS + IA: estado actual para la exposición

Documento generado leyendo el código de `SmartETL_DS-main/` el 15 de noviembre de 2026. Todas las rutas son relativas a esa carpeta. Donde algo no existe o está incompleto, se dice explícitamente.

---

## 1. Resumen

**Qué hace la aplicación**

1. Lee los CSV de **OpenFlights** (`data/airports.csv`, `data/routes.csv`, `data/airlines.csv`) y ejecuta un **ETL**: Extract los encola, Transform valida y limpia, y Load escribe archivos limpios y reportes en `output/`.
2. Sobre los datos limpios aplica estructuras propias: listas secuencial y enlazada, cola, pila y árbol binario de búsqueda (BST).
3. Compara ordenamientos (burbuja e inserción) y búsquedas (secuencial, binaria y por BST).
4. Arma **un grafo de rutas aéreas por país** y ofrece BFS, DFS, ruta más corta en vuelos y detección de ciclos.
5. Genera **flujogramas Mermaid y explicaciones con IA** (Gemini, Claude, OpenAI, GLM o GitHub Models) a partir de eventos reales de la ejecución, con un respaldo local sin internet. Todo se muestra en una **interfaz web local**, que incluye un reporte A4 para PDF.

**Dataset.** OpenFlights. Cifras que reproduce el ETL real, verificadas en esta sesión:

| Fuente | Procesados | Válidos | Inválidos |
|---|---|---|---|
| Aeropuertos | 7.698 | 7.698 | 0 |
| Rutas | 67.663 | 67.074 | 589 |
| Aerolíneas | 6.162 | 6.162 | 0 |

También hay un ETL de demostración con datos "sucios" en `data/demo/airports_sucio.csv` y `data/demo/routes_sucio.csv`.

**Cómo se ejecuta.** Clase principal `src/app/Main.java`. Desde la raíz del proyecto:

```
javac -encoding UTF-8 -d out src\model\*.java src\estructuras\*.java src\etl\*.java src\algoritmos\*.java src\grafo\*.java src\ia\*.java src\app\*.java src\app\swing\*.java src\app\web\*.java src\pruebas\*.java
java -cp out app.Main
```

- Levanta el servidor en `http://127.0.0.1:7070`. Si el puerto está ocupado, prueba del 7071 al 7079 (`ServidorWeb.PUERTO_INICIAL = 7070`, `INTENTOS = 10`) y abre el navegador.
- `java -cp out app.Main --swing` abre la versión Swing anterior.
- Requisito: JDK 11 o superior; no usa librerías externas de Java.

---

## 2. Árbol de carpetas

Sin `out/` (compilación), `.idea/` ni archivos generados.

```
SmartETL_DS-main/
├── src/
│   ├── model/          Entidades del dominio (aeropuerto, ruta, aerolínea) y errores del ETL
│   │   ├── Aerolinea.java, Aeropuerto.java, Ruta.java, Registro.java
│   │   └── ErrorETL.java, TipoError.java
│   ├── estructuras/    Estructuras de datos propias (listas, cola, pila, BST)
│   │   ├── Lista.java, Criterio.java, Nodo.java, ResultadoBusqueda.java
│   │   ├── ListaSecuencial.java, ListaEnlazada.java
│   │   ├── Cola.java, Pila.java
│   │   └── ArbolBST.java, NodoArbol.java
│   ├── etl/            Extract, Transform y Load; lectura de CSV; ubicación de la carpeta del proyecto
│   │   ├── LectorCSV.java, Extract.java, Mapeador.java, FuenteDatos.java
│   │   ├── OpcionesCarga.java, ModoCarga.java, ResultadoExtraccion.java
│   │   ├── Transform.java, DatosLimpios.java, Load.java
│   │   └── ProcesoETL.java, Proyecto.java
│   ├── algoritmos/     Ordenamiento, búsquedas y algoritmos de grafos (BFS, DFS, rutas, ciclos)
│   │   ├── Ordenamiento.java, ResultadoOrdenamiento.java, Comparadores.java, Busqueda.java
│   │   └── BFS.java, DFS.java, Rutas.java, Ciclos.java, Vecinos.java, ResultadoRecorrido.java
│   ├── grafo/          Grafo de rutas: vértices, un grafo por país y el BST de países
│   │   └── Vertice.java, Grafo.java, GrafoPais.java, MapaPaises.java, Agrupacion.java
│   ├── ia/             Capa de IA: clientes HTTP, prompts, respaldo local, Mermaid, evidencias, visores HTML
│   │   ├── ProveedorIA.java, ClienteCompatibleOpenAI.java, ClienteAnthropic.java, ConfiguracionIA.java
│   │   ├── AnalizadorIA.java, ResultadoIA.java, GeneradorPrompt.java, GeneradorFlujogramaLocal.java
│   │   ├── RegistroEventos.java, Evento.java, TrazasEstructuras.java, Mermaid.java, Json.java
│   │   └── VisorHtml.java, VisorGrafo.java, AlmacenApis.java, ApiGuardada.java
│   ├── app/            Lanzador (web o Swing) y estado compartido de la sesión
│   │   ├── Main.java, Sesion.java
│   │   ├── web/        Servidor HTTP local y endpoints JSON de la interfaz web
│   │   │   ├── ServidorWeb.java, Api.java, AreaApi.java, Estaticos.java, Seguridad.java
│   │   │   ├── Peticion.java, Respuesta.java, ErrorWeb.java
│   │   │   ├── ObjetoJson.java, ArregloJson.java, LectorJson.java
│   │   │   ├── GestorTareas.java, TareaWeb.java, TrabajoWeb.java, ContextoWeb.java, ReporteSesion.java
│   │   │   └── ApiGeneral.java, ApiDatos.java, ApiEtl.java, ApiEstructuras.java, ApiGrafo.java,
│   │   │       ApiIa.java, ApiApis.java, FuncionesIa.java
│   │   └── swing/      Versión de escritorio anterior (se conserva intacta)
│   │       ├── VentanaPrincipal.java, Tarea.java, Animacion.java, ModeloTablaRegistros.java
│   │       └── PanelDatos.java, PanelETL.java, PanelEstructuras.java, PanelGrafo.java,
│   │           PanelIA.java, PanelGuardarApis.java
│   ├── pruebas/        Pruebas sin JUnit (main con aserciones propias) por hito
│   │   └── PruebasHito1.java … PruebasHito4.java, PruebasIA.java
│   ├── diagramas/      (no es código) flujo_cola.md y ver_flujo_cola.html generados al ejecutar desde src/
│   └── evidencias_ia/  (no es código) 2 evidencias del 7 de noviembre generadas al ejecutar desde src/
├── web/                Interfaz web: HTML, CSS y JavaScript sin compilar
│   ├── index.html                Las 6 pestañas (Datos, ETL, Estructuras, Grafo, IA, Reporte)
│   ├── styles.css                Tokens de color, tema claro/oscuro, botones, tablas, overlay, toasts
│   ├── componentes.css           Pipeline ETL, cola, pila, gráficos, modal, resultado de IA
│   ├── estructuras.css, grafo.css, ia.css, reporte.css   Estilos de cada pestaña (reporte: A4 e impresión)
│   └── js/
│       ├── app.js         Pestañas, tema, estado general; arranca cada módulo
│       ├── api.js         fetch con cabecera de seguridad y seguimiento de tareas por SSE
│       ├── interfaz.js    Panel de carga, toasts, modal, KPIs, utilidades
│       ├── datos.js, etl.js, estructuras.js, grafo.js, ia.js, reporte.js   Una por pestaña
│       └── resultadoIa.js Dibuja Mermaid (zoom, colores, resaltado) y textos de la IA
├── recursos/           Librerías del frontend copiadas (sin CDN) e íconos
│   ├── anime.esm.min.js, auto-animate.mjs, chart.umd.min.js
│   ├── mermaid.min.js, svg-pan-zoom.min.js, vis-network.min.js
│   ├── iconos/ (SVG de Lucide)
│   └── licencias/ (8 archivos LICENSE)
├── data/               CSV de OpenFlights y data/demo/ con datos sucios
├── diagramas/          Diagramas .md y páginas .html generadas (versionadas)
├── evidencias_ia/      Prompt y respuesta de cada llamada a la IA (versionadas)
├── output/             Archivos limpios y reportes del Load (en .gitignore)
├── docs/               GUIA-INTERFAZ-WEB.md, GUIA-DEFENSA.md, GUION-DEMO.md, MIGRACION-FASE0.md, ESTADO-ACTUAL.md,
│                       RECORRIDO-PASO-A-PASO.md, GUIA-EXPOSICION.html
├── README.md, PRODUCT.md, DESIGN.md
```

---

## 3. Clases por paquete

En "Usa" se listan solo las clases del proyecto. La única API de colecciones de Java que aparece es `java.util.List`, en dos lugares de Swing: `PanelGrafo` (lo devuelve `JList`) y `Tarea` (lo exige `SwingWorker`). Del JDK, el backend solo usa `Comparator`, `Function`, `Consumer` y `Random`, que no son colecciones.

### `model`

| Clase | Qué hace | Métodos principales | Usa |
|---|---|---|---|
| `Registro` | Interfaz común de una fila de CSV | `numeroCampos`, `valor(i)` | — |
| `Aeropuerto` | Aeropuerto de OpenFlights (14 campos) | `desdeCampos`, `conId`, `conIata`, getters | `Registro` |
| `Ruta` | Ruta aerolínea-origen-destino (9 campos) | `desdeCampos`, `getIdOrigen`, `getIdDestino` | `Registro` |
| `Aerolinea` | Aerolínea (8 campos) | `desdeCampos`, getters | `Registro` |
| `ErrorETL` | Un registro rechazado: fuente, línea, tipo, motivo, valor original | `comoLinea`, getters | `TipoError` |
| `TipoError` | Enum: `CAMPO_OBLIGATORIO`, `ID_INVALIDO`, `RANGO_INVALIDO`, `DUPLICADO`, `REFERENCIA_ROTA`, `ID_NULO_NO_RECUPERABLE` | — | — |

### `estructuras`

| Clase | Qué hace | Métodos principales | Usa |
|---|---|---|---|
| `Lista<T>` | Interfaz común de las dos listas | `insertarInicio`, `insertarFinal`, `obtener`, `buscar`, `filtrar`, `contar` | `Criterio`, `ResultadoBusqueda` |
| `Criterio<T>` | Condición para buscar o filtrar | `cumple` | — |
| `Nodo<T>` | Nodo simple (dato + siguiente) | `getDato`, `getSiguiente`, `setSiguiente` | — |
| `ListaSecuencial<T>` | Lista sobre arreglo que duplica su capacidad | `insertarFinal`, `obtener`, `reemplazar`, `copiaDe`, `buscar`, `filtrar` | `ListaEnlazada` (en `copiaDe`) |
| `ListaEnlazada<T>` | Lista simplemente enlazada con `head` y `tail` | `insertarInicio`, `insertarFinal`, `obtener`, `buscar`, `contiene`, `primerNodo` | `Nodo`, `RegistroEventos` |
| `Cola<T>` | Cola FIFO con nodos (`frente` y `fin`) | `encolar`, `desencolar`, `frente`, `primeros` | `Nodo`, `RegistroEventos` |
| `Pila<T>` | Pila LIFO con nodos (`cima`) | `apilar`, `desapilar`, `cima`, `elementos` | `Nodo`, `ListaSecuencial` |
| `NodoArbol<T>` | Nodo con hijo izquierdo y derecho | `getIzquierdo`, `getDerecho`, `esHoja` | — |
| `ArbolBST<T>` | Árbol binario de búsqueda con comparador | `insertar`, `buscar`, `eliminar`, `caminoHacia`, `altura`, `contarNodos`, `promedioComparaciones`, `inOrden`, `preOrden`, `postOrden` | `Cola`, `Pila`, `ListaSecuencial`, `NodoArbol` |
| `ResultadoBusqueda<T>` | Resultado: elemento, índice o profundidad, comparaciones | `encontrado`, `getComparaciones` | — |

### `etl`

| Clase | Qué hace | Métodos principales | Usa |
|---|---|---|---|
| `LectorCSV` | Parte una línea CSV respetando comillas | `separar` | — |
| `Mapeador<T>` | Interfaz "campos → objeto" | `crear` | — |
| `FuenteDatos` | Describe cada fuente (archivo, columnas, mapeador) para la pestaña Datos | `todas`, `getMapeador`, `getEncabezados` | `Aeropuerto`, `Ruta`, `Aerolinea` |
| `ModoCarga`, `OpcionesCarga` | Cargar todos, los primeros N, un rango o N aleatorios con semilla | `todos`, `primeros`, `rango`, `aleatorio` | — |
| `Extract` | Lee el CSV y entrega cada registro a una `Lista` o a una `Cola` | `extraer`, `extraerEnCola` | `LectorCSV`, `Cola`, `Lista`, `OpcionesCarga` |
| `ResultadoExtraccion<T>` | Totales de la lectura (líneas, cargadas, no leídas, ms) | getters | `Lista` |
| `Transform` | Desencola, valida, limpia; los válidos van a `DatosLimpios` y los errores a una `Pila` | `transformarAeropuertos`, `transformarRutas`, `transformarAerolineas`, `limpio`, `esIataValido` | `Cola`, `Pila`, `ArbolBST`, `DatosLimpios`, `ErrorETL`, `RegistroEventos` |
| `DatosLimpios` | Listas de válidos e índices (BST por IATA, lista ordenada por ID) | `agregarAeropuerto`, `indexarAeropuertosPorId`, `buscarPorId`, `buscarPorIata` | `ListaSecuencial`, `ArbolBST`, `Ordenamiento`, `Busqueda`, `Comparadores` |
| `Load` | Escribe los CSV limpios, `errores_etl.txt` y `reporte_etl.txt` | `escribir`, `lineaCsv`, `textoReporte` | `DatosLimpios`, `Transform`, `Pila` |
| `ProcesoETL` | Orquesta Extract → Transform → (Load), real o demostración | `real`, `demostracion`, `ejecutar`, `cargar`, `getRegistro` | `Extract`, `Transform`, `Load`, `Cola`, `RegistroEventos` |
| `Proyecto` | Encuentra la carpeta raíz (la que tiene `data/airports.csv`) | `detectar`, `usarCarpeta`, `ruta`, `getBase` | — |

### `algoritmos`

| Clase | Qué hace | Métodos principales | Usa |
|---|---|---|---|
| `Ordenamiento` | Burbuja (con corte temprano) e inserción sobre una copia; cuenta comparaciones y movimientos | `burbuja`, `insercion`, `estaOrdenada` | `ListaSecuencial`, `ResultadoOrdenamiento` |
| `ResultadoOrdenamiento<T>` | Lista ordenada + comparaciones, movimientos, ms | getters, `nombreMovimientos` | `ListaSecuencial` |
| `Comparadores` | Comparadores de aeropuerto por ID, IATA, nombre o país | `porCampo` y constantes | `Aeropuerto` |
| `Busqueda` | Secuencial (delegada a la lista), binaria y por BST | `secuencial`, `binaria`, `porArbol` | `Lista`, `ListaSecuencial`, `ArbolBST` |
| `Vecinos` | Vecinos de un vértice: internos, y externos si se cruzan fronteras | `de`, `etiquetas` | `Vertice`, `MapaPaises` |
| `BFS` | Recorrido por niveles con la `Cola` propia | `recorrer` | `Cola`, `Vecinos`, `RegistroEventos`, `ResultadoRecorrido` |
| `DFS` | Recorrido en profundidad con la `Pila` propia (iterativo) | `recorrer` | `Pila`, `Vecinos`, `RegistroEventos`, `ResultadoRecorrido` |
| `Rutas` | Ruta más corta en número de vuelos (BFS con padre) y camino reconstruido con una `Pila` | `buscar` | `Cola`, `Pila`, `Vecinos` |
| `Ciclos` | Ciclo de longitud 3 o más (DFS recursivo con estados gris y negro) | `detectar` | `Grafo`, `Vertice`, `Pila` |
| `ResultadoRecorrido` | Visitados, niveles, países por nivel, ms | `cantidad`, `nivelMaximo`, `cantidadEnNivel`, `paisesEnNivel` | `Vertice`, `Agrupacion` |

### `grafo`

| Clase | Qué hace | Métodos principales | Usa |
|---|---|---|---|
| `Vertice<T>` | Aeropuerto con sus adyacentes internos, destinos externos y llegadas externas; marcas para recorridos | `agregarAdyacente`, `agregarExterno`, `visitar`, `fueVisitado`, `nuevaCorrida` | `ListaEnlazada` |
| `Grafo<T>` | Lista de vértices y conteo de aristas (dirigido: origen → destino) | `agregarVertice`, `agregarArista`, `buscarVertice` | `ListaEnlazada`, `Vertice` |
| `GrafoPais` | El grafo de un país y sus totales | `externosPorPais`, `resumen`, `getVertices`, `getRutasInternas` | `Grafo`, `Agrupacion` |
| `MapaPaises` | BST de países; construye cada grafo bajo demanda a partir de las rutas limpias | `obtener`, `construirTodos`, `verticePorIata`, `construidos` | `ArbolBST`, `GrafoPais`, `DatosLimpios` |
| `Agrupacion` | Conteo por clave con 3 listas paralelas (por ejemplo, rutas externas por país) | `agregar`, `clave`, `conteo`, `detalle` | `ListaSecuencial` |

### `ia`

| Clase | Qué hace | Métodos principales | Usa |
|---|---|---|---|
| `ProveedorIA` | Interfaz de un cliente de IA | `enviar(prompt)` | — |
| `ClienteCompatibleOpenAI` | HTTP en formato chat/completions (Gemini, OpenAI, GLM, GitHub Models) | `enviar` | `Json` |
| `ClienteAnthropic` | HTTP con el formato de Claude (`x-api-key`, `anthropic-version`) | `enviar` | `Json` |
| `ConfiguracionIA` | Proveedor, modelo, URL, clave (en memoria) y modo; URLs y variables de entorno por proveedor | `urlDe`, `claveDeEntorno`, `crearProveedor`, `soloLocal` | Clientes |
| `AnalizadorIA` | Llama a la API o al respaldo; limpia el Mermaid; comprueba coherencia; escribe HTML y evidencia | `diagrama`, `texto`, `validarConexion`, `limpiarMermaid`, `coherencia` | `ConfiguracionIA`, `VisorHtml`, `RegistroEventos`, `ResultadoIA` |
| `ResultadoIA` | Contenido, si es diagrama, si usó la API, aviso, coherencia, rutas de HTML y evidencia | getters, `origen` | — |
| `GeneradorPrompt` | Arma los prompts con reglas, ejemplo y eventos reales | `flujogramaEtl`, `flujogramaEstructura`, `flujogramaRecorrido`, `explicacion`, `datosArbol`, `datosGrafo`, `datosErrores` | `RegistroEventos`, `ArbolBST`, `GrafoPais`, `Transform` |
| `GeneradorFlujogramaLocal` | Respaldo sin IA: arma el Mermaid desde el registro de eventos | `etl`, `estructura`, `recorrido`, `generalPaises`, `grafoPais` | `RegistroEventos`, `Mermaid`, `GrafoPais` |
| `RegistroEventos`, `Evento` | Bitácora de lo que hizo cada estructura (lista enlazada, máximo 20.000 eventos) | `agregar`, `registrar`, `comoTexto`, `contar` | `ListaEnlazada` |
| `TrazasEstructuras` | Ejecuta trazas cortas reales de cola, pila, lista y BST para sus flujogramas | `cola`, `pila`, `lista`, `bst` | `Cola`, `Pila`, `ListaEnlazada`, `ArbolBST` |
| `Mermaid` | Constructor de código Mermaid (nodos, cajas, decisiones, enlaces) | `inicio`, `caja`, `decision`, `enlazar` | — |
| `Json` | Escapa y lee valores JSON a mano | `cadena`, `valorDeCadena`, `posicion` | — |
| `VisorHtml` | Escribe `diagramas/*.md` y `ver_*.html` con Mermaid local y zoom | `escribirDiagrama`, `escribirPagina`, `archivoPais` | `Mermaid`, `Proyecto` |
| `VisorGrafo` | Escribe `diagramas/visor_grafo.html` con vis-network | `escribir` | `MapaPaises`, `Json` |
| `AlmacenApis`, `ApiGuardada` | APIs guardadas en `~/.smartetl/apis.properties` (texto plano) | `cargar`, `guardar`, `eliminar`, `buscar` | `ListaSecuencial` |

### `app`

| Clase | Qué hace | Métodos principales | Usa |
|---|---|---|---|
| `Main` | Si recibe `--swing` abre la ventana; si no, prepara la carpeta, inicia el servidor y abre el navegador | `main`, `explicacionCarpeta` | `Proyecto`, `ServidorWeb`, `Sesion`, `VentanaPrincipal` |
| `Sesion` | Estado compartido: último ETL, ETL real, mapa de países, configuración de IA, últimos BFS/DFS | `datosReales`, `ultimoEtl`, `mapa`, `listaEnlazada`, `registrarBfs`, `getConfiguracion` | `ProcesoETL`, `MapaPaises`, `ConfiguracionIA`, `AlmacenApis` |

### `app.web`

| Clase | Qué hace | Métodos principales | Usa |
|---|---|---|---|
| `ServidorWeb` | Crea el `HttpServer` en 127.0.0.1 (7070–7079) y registra `/api/` y `/` | `iniciar`, `getDireccion`, `detener` | `Api`, `Estaticos`, `ContextoWeb` y todas las `Api*` |
| `Api` | Recibe `/api/*`, aplica `Seguridad`, prueba cada `AreaApi` y traduce errores a JSON | `handle` | `AreaApi`, `Seguridad`, `Respuesta` |
| `AreaApi` | Interfaz de un grupo de endpoints | `atender(Peticion)` | `Peticion` |
| `Estaticos` | Sirve `web/`, `recursos/` y `diagramas/` sin salir de esas carpetas | `handle`, `resolver`, `tipo` | `Proyecto`, `Seguridad` |
| `Seguridad` | Verifica `Host` local y cabecera `X-SmartETL: 1` en POST y DELETE | `permitir` | `Respuesta` |
| `Peticion`, `Respuesta`, `ErrorWeb` | Leer ruta y JSON de entrada; responder JSON o error con código | `es`, `parte`, `parametro`, `json` / `json`, `error` | `LectorJson`, `ObjetoJson` |
| `ObjetoJson`, `ArregloJson`, `LectorJson` | Construir y leer JSON a mano | `texto`, `entero`, `crudo` / `textos` | `Json`, `ListaSecuencial` |
| `GestorTareas`, `TareaWeb`, `TrabajoWeb` | Una tarea larga a la vez en otro hilo (409 si ya hay una); avisos y resultado | `iniciar`, `exigirLibre` / `avisar`, `terminar`, `fallar` | `ListaEnlazada`, `ListaSecuencial` |
| `ContextoWeb` | Sesión, tareas, reporte, estado de validación de la IA, candado del grafo | `getSesion`, `getCandadoGrafo` | `Sesion`, `GestorTareas`, `ReporteSesion` |
| `ReporteSesion` | Guarda lo ejecutado en la sesión (para Reporte) y el historial de IA | `registrar`, `agregarIa`, `historialIa`, `reporte` | `ListaSecuencial`, `ListaEnlazada` |
| `ApiGeneral` | Estado general, reporte, consulta de tareas y SSE | `atender`, `tareaIniciada` | `ContextoWeb` |
| `ApiDatos` | Pestaña Datos (Hito 1) | `atender` | `Extract`, `FuenteDatos`, `ListaEnlazada`, `ListaSecuencial` |
| `ApiEtl` | Pestaña ETL | `atender`, `resumen` | `ProcesoETL`, `Transform`, `Load` |
| `ApiEstructuras` | Pestaña Estructuras | `atender` | `Ordenamiento`, `Busqueda`, `ArbolBST`, `Comparadores` |
| `ApiGrafo` | Pestaña Grafo | `atender` | `MapaPaises`, `BFS`, `DFS`, `Rutas`, `Ciclos`, `VisorHtml`, `VisorGrafo` |
| `ApiIa`, `FuncionesIa` | Configuración, validación y las 10 funciones de IA; documentación | `atender`, `configuracion`, `comoJson` / `ejecutar` | `AnalizadorIA`, `GeneradorPrompt`, `TrazasEstructuras` |
| `ApiApis` | APIs guardadas (nunca devuelve la clave) | `atender` | `AlmacenApis`, `ApiIa` |

### `app.swing` (versión anterior, intacta)

| Clase | Qué hace | Usa |
|---|---|---|
| `VentanaPrincipal` | `JFrame` con las 6 pestañas Swing | Los `Panel*`, `Sesion` |
| `PanelDatos`, `PanelETL`, `PanelEstructuras`, `PanelGrafo`, `PanelIA`, `PanelGuardarApis` | Una pestaña cada uno | El backend correspondiente |
| `Tarea`, `Animacion`, `ModeloTablaRegistros` | `SwingWorker` con avisos, texto animado y modelo de `JTable` | `Main`, `Lista` |

### `pruebas`

| Clase | Qué prueba | Resultado en esta sesión |
|---|---|---|
| `PruebasHito1` | CSV, listas, modos de carga, búsquedas del Hito 1 | 53 pasan, 0 fallan |
| `PruebasHito2` | ETL, cola, pila, ordenamiento, búsquedas | 55 pasan, 0 fallan |
| `PruebasHito3` | BST, mapa de países | 32 pasan, 0 fallan |
| `PruebasHito4` | Grafo, BFS, DFS, rutas, ciclos | 20 pasan, 0 fallan |
| `PruebasIA` | Prompts, limpieza de Mermaid, coherencia, almacén de APIs | 60 pasan, 0 fallan |

No usan JUnit: cada clase tiene un `main` que imprime `OK` o `FALLA` y el total.

---

## 4. Estructuras de datos

Todas son **implementación propia** en `src/estructuras/` y `src/grafo/`. No se usa `ArrayList`, `HashMap`, `Stack` ni `LinkedList` de Java en el backend ni en la capa web. Además se usan arreglos nativos de Java (`Object[]` dentro de `ListaSecuencial`, `Vertice[] verticePorId` en `MapaPaises`, `int[]` de contadores en `Transform`).

### 4.1 Lista secuencial: `src/estructuras/ListaSecuencial.java`

- **Dónde se usa**
  - Listas de válidos del ETL: aeropuertos, rutas y aerolíneas en `DatosLimpios`.
  - La lista ordenada por ID para la búsqueda binaria.
  - Resultados de ordenamiento, visitados de BFS/DFS y la vista paginada de la web.
- **Por qué:** acceso por índice O(1), que necesitan la búsqueda binaria, el ordenamiento y la paginación (50 o 20 filas por página).

```java
private void asegurarCapacidad() {
    if (tope == datos.length) {
        Object[] ampliado = new Object[datos.length * 2];
        for (int i = 0; i < tope; i++) {
            ampliado[i] = datos[i];
        }
        datos = ampliado;
    }
}
```

| Operación | Complejidad |
|---|---|
| `obtener(i)`, `reemplazar(i)` | O(1) |
| `insertarFinal` | O(1) amortizado (duplica la capacidad, inicial 16) |
| `insertarInicio` | O(n) (desplaza todo) |
| `buscar`, `filtrar`, `contar` | O(n) |

### 4.2 Lista enlazada: `src/estructuras/ListaEnlazada.java` (con `Nodo.java`)

- **Dónde se usa**
  - Pestaña Datos (Hito 1), como estructura de carga elegible.
  - Lista para la búsqueda secuencial de Estructuras: `Sesion.listaEnlazada()` copia los aeropuertos.
  - Lista de vértices de `Grafo` y adyacencias, destinos externos y llegadas externas de cada `Vertice`.
  - Eventos de `RegistroEventos`.
- **Por qué:** insertar al final en O(1) gracias a `tail`, sin redimensionar. Sirve para listas que solo crecen y se recorren de principio a fin (adyacencias, bitácora).

```java
public void insertarFinal(T dato) {
    Nodo<T> nuevo = new Nodo<>(dato);
    if (head == null) {
        head = nuevo;
        tail = nuevo;
    } else {
        tail.setSiguiente(nuevo);
        tail = nuevo;
    }
    tamanio++;
```

| Operación | Complejidad |
|---|---|
| `insertarInicio`, `insertarFinal` | O(1) |
| `obtener(i)` | O(i) (recorre desde `head`) |
| `buscar`, `contiene`, `filtrar` | O(n) |

### 4.3 Cola: `src/estructuras/Cola.java`

- **Dónde se usa**
  - **Extract**: `Extract.extraerEnCola` encola cada registro leído y `Transform` los desencola.
  - BFS (`BFS.recorrer`) y ruta más corta (`Rutas.buscar`).
  - Dentro del BST: `altura`, `promedioComparaciones` y `primerosNiveles` recorren por niveles.
- **Por qué:** FIFO conserva el orden del archivo (la línea N del error es la línea N leída) y es justo lo que pide BFS (visitar por capas).

```java
public void encolar(T dato) {
    Nodo<T> nuevo = new Nodo<>(dato);
    if (fin == null) {
        frente = nuevo;
    } else {
        fin.setSiguiente(nuevo);
    }
    fin = nuevo;
    tamanio++;
```

| Operación | Complejidad |
|---|---|
| `encolar`, `desencolar`, `frente`, `estaVacia` | O(1) |
| `primeros(k)` (vista para la pantalla) | O(k) |

### 4.4 Pila: `src/estructuras/Pila.java`

- **Dónde se usa**
  - **Pila de errores del Transform** (`Transform.registrarError` → `errores.apilar`).
  - DFS (`DFS.recorrer`).
  - Reconstruir caminos al revés (`Rutas.reconstruir`, `Ciclos.construirCiclo`).
  - `ArbolBST.contarNodos`.
- **Por qué:** LIFO deja arriba el último error encontrado. En DFS profundiza en el último vecino apilado, y para reconstruir un camino desde el destino hasta el origen basta apilar e invertir.

| Operación | Complejidad |
|---|---|
| `apilar`, `desapilar`, `cima`, `estaVacia` | O(1) |
| `elementos()` (copia para Load y la web) | O(n) |

### 4.5 Árbol binario de búsqueda: `src/estructuras/ArbolBST.java` (con `NodoArbol.java`)

Es genérico y recibe un `Comparator`. Se usa en cuatro lugares:

| Instancia | Clase y método | Para qué |
|---|---|---|
| `ArbolBST<Integer>` de IDs | `Transform.transformarAeropuertos` y `transformarAerolineas` | Detectar IDs duplicados (`insertar` devuelve `false`) |
| `ArbolBST<Aeropuerto>` por IATA | `DatosLimpios.arbolIata` | Resolver rutas con ID `\N` por código IATA y la búsqueda por BST de la pestaña Estructuras (6.071 nodos, altura 177) |
| `ArbolBST<GrafoPais>` | `MapaPaises` | Buscar un país por nombre (237 países, altura 15) |
| Copia de trabajo | `ApiEstructuras` | Insertar, buscar y eliminar desde la web sin tocar el índice del ETL |

- **Por qué:** búsqueda en O(h) en lugar de O(n). Comparación real en esta sesión al buscar UIO:

  | Método | Comparaciones |
  |---|---|
  | Secuencial | 2.556 |
  | BST | 13 |

- `insertar`, `buscar` y `eliminar` son **iterativos**.
- Los recorridos `inOrden`, `preOrden` y `postOrden` son recursivos, pero se niegan si la altura supera 2.000 (`ALTURA_MAXIMA_RECURSIVA`) para no desbordar la pila de llamadas.

```java
NodoArbol<T> actual = raiz;
while (true) {
    int comparacion = comparador.compare(dato, actual.getDato());
    if (comparacion == 0) {
        return false;
    }
    if (comparacion < 0) {
        if (actual.getIzquierdo() == null) {
            actual.setIzquierdo(nuevo);
            break;
        }
        actual = actual.getIzquierdo();
```

(Fragmento recortado de `insertar`.)

| Operación | Complejidad |
|---|---|
| `insertar`, `buscar`, `eliminar`, `caminoHacia` | O(h): O(log n) si está balanceado, O(n) en el peor caso |
| `altura` (con `Cola`), `contarNodos` (con `Pila`), `promedioComparaciones`, recorridos | O(n) |

El peor caso se demuestra en la web, en Estructuras › Desbalance: con 1.500 IDs insertados en orden, la altura llega a 1.499.

### 4.6 Grafo: `src/grafo/Vertice.java`, `Grafo.java`, `GrafoPais.java`, `MapaPaises.java`

- **Modelo**
  - Un **grafo dirigido por país**, con lista de adyacencia: cada `Vertice` tiene una `ListaEnlazada` de adyacentes internos, otra de destinos en otros países y otra de llegadas desde otros países.
  - `MapaPaises` construye cada `GrafoPais` **bajo demanda** recorriendo las rutas limpias.
  - El arreglo `verticePorId` (indexado por ID de aeropuerto) evita crear un mismo vértice dos veces.
- **Dónde se usa:** después del ETL, en la pestaña Grafo (BFS, DFS, ruta, ciclo, visor) y en los flujogramas BFS/DFS de la IA.
- **Por qué lista de adyacencia:** el grafo es disperso (225 países con vértices, 3.264 vértices y 17.587 aristas internas). Una matriz desperdiciaría memoria.
- Las marcas de visita no usan un conjunto: cada vértice guarda el número de "corrida" del último recorrido que lo visitó (`Vertice.nuevaCorrida`, `fueVisitado`). Por eso dos recorridos no pueden correr a la vez; en la web lo impide `ContextoWeb.getCandadoGrafo()`.

```java
public void visitar(int corrida, int nivel, Vertice<T> padre) {
    this.marca = corrida;
    this.nivel = nivel;
    this.padre = padre;
    this.estado = GRIS;
}
```

| Operación | Complejidad |
|---|---|
| `Grafo.agregarVertice` | O(1) |
| `Grafo.agregarArista` | O(grado del origen) (`contiene` evita duplicados) |
| `Grafo.buscarVertice` | O(V) |
| Construir un país | O(R), con R = rutas limpias (67.074) |
| BFS, DFS, ruta más corta, detectar ciclo | O(V + E) |

### 4.7 Otras estructuras auxiliares

| Estructura | Clase | Uso |
|---|---|---|
| Agrupación (3 `ListaSecuencial` paralelas) | `src/grafo/Agrupacion.java` | Contar rutas externas por país y aeropuertos por país en cada nivel del BFS. `agregar` es O(k), con k = claves distintas |
| Bitácora de eventos | `src/ia/RegistroEventos.java` | `ListaEnlazada<Evento>` con tope de 20.000; cuenta los descartados. Es la entrada de los flujogramas |
| Historial y reporte | `src/app/web/ReporteSesion.java` | `ListaEnlazada` para el historial de IA y dos `ListaSecuencial` paralelas (sección → JSON) |

---

## 5. Flujo ETL paso a paso

Ejemplo: la línea del aeropuerto de Quito (ID 2688, IATA UIO) y una ruta que lo usa.

| # | Paso | Clase y método | Qué pasa con el dato |
|---|---|---|---|
| 1 | Inicio | Web: `POST /api/etl/ejecutar {modo:"real"}` → `ApiEtl.atender` crea una tarea → `ProcesoETL.real().ejecutar()` | Se crea un `Transform` con un `RegistroEventos` |
| 2 | Extracción | `ProcesoETL.extraer` → `Extract.extraerEnCola(ruta, 14, Aeropuerto::desdeCampos, cola, OpcionesCarga.todos())` | Lee `data/airports.csv` en UTF-8, quita el BOM y salta líneas vacías |
| 3 | Separar campos | `LectorCSV.separar(linea)` | Respeta comillas (`"Mariscal Sucre International Airport"`); si no hay 14 campos, la línea cuenta como "no leída" |
| 4 | Crear objeto y encolar | `Aeropuerto.desdeCampos(campos)` → `Cola.encolar` | El aeropuerto crudo entra a la cola de extracción |
| 5 | Validación | `Transform.transformarAeropuertos` → `Cola.desencolar` → `validarAeropuerto` | ID numérico, nombre y país presentes, latitud en [-90, 90], longitud en [-180, 180] |
| 6 | Duplicados | `ArbolBST<Integer>.insertar(id)` | Si el ID ya estaba, el error es `DUPLICADO` |
| 7 | Limpieza y normalización | `Transform.limpiar` y `Transform.limpio` | `trim` en todo; `\N` o vacío pasa a `"Sin dato"`; un IATA que no sea 3 letras A–Z pasa a `"Sin dato"` |
| 8 | Válido o error | `DatosLimpios.agregarAeropuerto` o `Transform.registrarError` → `Pila.apilar(ErrorETL)` | Válido: va a la `ListaSecuencial` y, si tiene IATA, al BST por IATA. Error: va a la pila con su `TipoError` |
| 9 | Índice por ID | `DatosLimpios.indexarAeropuertosPorId` → `Ordenamiento.insercion(..., AEROPUERTO_POR_ID)` | Lista ordenada por ID para la búsqueda binaria |
| 10 | Transformación de rutas | `Transform.transformarRutas` → `validarObligatoriosRuta` → `resolverId` | ID numérico: `Busqueda.binaria` sobre la lista por ID. ID `\N`: `ArbolBST.buscar` por IATA. Si no se resuelve: `REFERENCIA_ROTA` (473) o `ID_NULO_NO_RECUPERABLE` (116), y va a la pila |
| 11 | Aerolíneas | `Transform.transformarAerolineas` | ID numérico, nombre presente, sin duplicados (otro `ArbolBST<Integer>`) |
| 12 | Carga | Web: `POST /api/etl/cargar` → `ProcesoETL.cargar("output")` → `Load.escribir` | Escribe 5 archivos en `output/` (detallados abajo) |
| 13 | Consultas y reportes | `GET /api/etl/resultado`, `/api/etl/validos?fuente&pagina`, `/api/etl/errores?tipo&pagina`, `GET /api/output/{archivo}` | La pestaña ETL muestra KPIs, cola, pila de errores por tipo y válidos paginados |
| 14 | En pantalla | `web/js/etl.js` | Pipeline animado con los avisos que llegan por SSE, tarjetas KPI, gráficos Chart.js, tablas paginadas |
| 15 | Después del ETL | `Sesion.datosReales()` | Lo usan Estructuras (búsquedas, BST), Grafo (`MapaPaises`) e IA (flujograma ETL, analizar errores). Aparece en el Reporte |

Archivos que escribe el paso 12: `airports_limpios.csv`, `routes_limpias.csv`, `airlines_limpias.csv`, `errores_etl.txt` (sale de `Pila.elementos()`) y `reporte_etl.txt`.

La pestaña **Datos** usa otra vía, la del Hito 1, sin validar: `POST /api/datos/cargar` → `Extract.extraer` → `ListaEnlazada` o `ListaSecuencial`, con los modos todos, primeros, rango o aleatorio.

---

## 6. Capa de IA

**Proveedores** (`src/ia/ConfiguracionIA.java`, arreglo `PROVEEDORES`):

| Proveedor | URL por defecto | Variable de entorno | Cliente |
|---|---|---|---|
| Gemini | `generativelanguage.googleapis.com/v1beta/openai/chat/completions` | `GEMINI_API_KEY` | `ClienteCompatibleOpenAI` |
| Claude | `api.anthropic.com/v1/messages` | `ANTHROPIC_API_KEY` | `ClienteAnthropic` |
| OpenAI | `api.openai.com/v1/chat/completions` | `OPENAI_API_KEY` | `ClienteCompatibleOpenAI` |
| GLM | `api.z.ai/api/paas/v4/chat/completions` | `GLM_API_KEY` | `ClienteCompatibleOpenAI` |
| GitHub Models (Copilot) | `models.github.ai/inference/chat/completions` | `GITHUB_TOKEN` | `ClienteCompatibleOpenAI` |

- Los clientes usan `java.net.http.HttpClient` con una espera de 30 s.
- El JSON se arma y se lee a mano (`src/ia/Json.java`).
- La URL es editable y el nombre del modelo lo escribe el usuario.

**Cómo se ingresa la clave**

1. En la pestaña IA (campo de contraseña con botón de ojo). Viaja una sola vez por `POST /api/ia/config` y queda en la memoria del servidor (`ConfiguracionIA.setClave`). Ningún endpoint la devuelve.
2. Si el campo queda vacío, el servidor usa la variable de entorno del proveedor (`ApiIa.aplicar` → `ConfiguracionIA.claveDeEntorno`). El navegador solo ve el nombre de la variable.
3. Con "Gestionar APIs" se guarda con nombre en `~/.smartetl/apis.properties`, en texto plano (`AlmacenApis`), y "Usar" la carga en el servidor (`POST /api/apis/{nombre}/usar`).

**Modos.** "API en vivo + respaldo local" o "Solo local". "Validar API" (`POST /api/ia/validar` → `AnalizadorIA.validarConexion`) envía `"Responde únicamente con la palabra OK."`.

**Las 10 funciones** (`src/app/web/FuncionesIa.java`):

| Función | Origen de los datos |
|---|---|
| Flujograma ETL | `RegistroEventos` del último ETL |
| Flujograma Cola, Pila, Lista enlazada, BST | Traza real corta ejecutada al momento (`TrazasEstructuras`) |
| Flujograma BFS y DFS | Registro del último recorrido de la pestaña Grafo; si no hay, ejecuta uno desde UIO |
| Explicar árbol | `GeneradorPrompt.datosArbol`: nodos, altura, promedio, primeros niveles, búsqueda de UIO y su camino |
| Explicar grafo | `GeneradorPrompt.datosGrafo` del último país usado (Ecuador por defecto) |
| Analizar errores ETL | `GeneradorPrompt.datosErrores`: solo conteos agregados por tipo |

**Qué se le envía al modelo.** Solo texto armado por `GeneradorPrompt` con eventos de la ejecución y datos públicos de OpenFlights; nada personal. Para diagramas incluye reglas fijas:

```java
"- Devuelve SOLO un bloque de código Mermaid (```mermaid ... ```), sin texto antes ni después.\n"
"- La primera línea del diagrama debe ser: flowchart TD\n"
"- No inventes datos: usa solo los aeropuertos, números y pasos que aparecen en la entrada.\n"
"- Cada nodo debe corresponder a un paso real del registro de eventos.\n"
```

Se envían como máximo 250 eventos (`LIMITE_EVENTOS`), más un ejemplo del formato esperado.

**Qué hace la app con la respuesta** (`AnalizadorIA.diagrama` y `texto`):

1. `limpiarMermaid` quita las cercas ```` ``` ```` y rechaza lo que no empiece con `flowchart` o `graph`.
2. Si la API falla (sin clave, HTTP 4xx o 5xx, sin internet o formato inválido), usa el **respaldo local** `GeneradorFlujogramaLocal` y lo indica en un aviso.
3. `coherencia` compara el diagrama con el registro: "faltan" (pasos esperados ausentes) y "sobran" (códigos de 3 letras que no estaban en la ejecución).
4. `VisorHtml.escribirDiagrama` escribe `diagramas/<nombre>.md` y `ver_<nombre>.html`.
5. `guardarEvidencia` escribe en `evidencias_ia/` el prompt, la respuesta cruda y el diagrama final. No se escribe en modo solo local, y nunca incluye la clave.
6. En la web (`ApiIa`), si "Incluir resumen" está activo, se hace una segunda llamada `AnalizadorIA.texto` que explica el diagrama. El resultado se guarda en el historial de la sesión y se dibuja con Mermaid en la página, con colores por tipo de paso (solo visual).
7. "Generar documentación" (`POST /api/ia/documentacion`) ejecuta las 10 funciones y escribe `diagramas/DOCUMENTACION.md`.

**No verificado en esta sesión:** respuestas reales de un proveedor con una clave válida. Se probó con una clave falsa (Gemini respondió `HTTP 400: Please pass a valid API key` y la app cayó al respaldo local) y en modo solo local.

---

## 7. Interfaz web

### 7.1 Cómo está montado el servidor

- `src/app/web/ServidorWeb.java` usa `com.sun.net.httpserver.HttpServer`, que viene en el JDK. Escucha en `127.0.0.1` y en el primer puerto libre entre 7070 y 7079, con un pool de hilos (`Executors.newCachedThreadPool`).
- Rutas registradas:
  - `/api/` → `Api`, que pasa por `Seguridad` y recorre las `AreaApi`: `ApiGeneral`, `ApiDatos`, `ApiEtl`, `ApiEstructuras`, `ApiGrafo`, `ApiIa`, `ApiApis`.
  - `/` → `Estaticos`: sirve `web/`, `recursos/` y `diagramas/`; la raíz devuelve `web/index.html`.
- **Seguridad** (`Seguridad.java`):
  - acepta solo `Host` igual a `127.0.0.1:puerto` o `localhost:puerto`;
  - exige la cabecera `X-SmartETL: 1` en POST y DELETE;
  - impide salir de las carpetas permitidas.
- **Tareas largas** (`GestorTareas`, `TareaWeb`):
  - el `POST` responde `202 {id, nombre, eventos}` y la tarea corre en otro hilo;
  - el progreso llega por **SSE** en `/api/tareas/{id}/eventos`, con los eventos `aviso`, `resultado` y `fallo`;
  - solo corre una tarea a la vez; si llega otra, la respuesta es `409`.

### 7.2 Endpoints

Fuente: los `p.es(...)` de cada clase en `src/app/web/`. Las filas marcadas "tarea" responden 202 y el resultado llega por SSE.

| Método | Ruta | Qué devuelve |
|---|---|---|
| GET | `/api/estado` | Carpeta, puerto, si hay datos, ETL ejecutado, tarea en curso, estado de la IA (sin la clave) |
| GET | `/api/reporte` | Todo lo registrado en la sesión para el Reporte |
| GET | `/api/tareas/{id}` | Estado de una tarea (terminada, ms, avisos, resultado o error) |
| GET | `/api/tareas/{id}/eventos` | Flujo SSE de la tarea |
| GET | `/api/datos/fuentes` | Las 3 fuentes con encabezados y columna de búsqueda |
| GET | `/api/datos/archivos` | CSV disponibles en `data/` y `data/demo/` |
| POST | `/api/datos/cargar` | Resumen de la carga cruda (Hito 1) con `ListaEnlazada` o `ListaSecuencial` |
| GET | `/api/datos/pagina?n=` | Página de 50 filas de la lista cargada |
| POST | `/api/datos/buscar` | Búsqueda secuencial en la lista cargada (comparaciones, ms, posición) |
| POST | `/api/datos/filtrar` | Filtra la lista por columna y valor |
| POST | `/api/datos/todos` | Quita el filtro |
| POST | `/api/etl/ejecutar` | Tarea: ETL real o demostración → resumen (procesados, válidos, errores, por fuente, por tipo) |
| POST | `/api/etl/cargar` | Tarea: Load → archivos escritos y tamaños |
| GET | `/api/etl/resultado` | Detalle del último ETL (KPIs, cola por fuente, errores por tipo, tiempos) |
| GET | `/api/etl/validos?fuente&pagina` | Válidos paginados por fuente |
| GET | `/api/etl/errores?tipo&pagina` | Errores de la pila paginados (20 por página) |
| GET | `/api/output/{archivo}` | Primeras 300 líneas de un archivo de `output/` (solo 5 nombres permitidos) |
| GET | `/api/estructuras/estado` | Si hay ETL real y el tamaño del BST de trabajo |
| POST | `/api/estructuras/ordenar` | Tarea: burbuja o inserción por IATA, nombre o país (comparaciones, movimientos, ms, primeros 15) |
| POST | `/api/estructuras/comparar-ordenamientos` | Tarea: burbuja frente a inserción con el mismo campo |
| POST | `/api/estructuras/muestra` | Muestra de 12 o 20 aeropuertos y los conteos de Java para validar el visualizador |
| POST | `/api/estructuras/buscar` | Secuencial, binaria y BST con el mismo IATA, y el camino en el BST |
| POST | `/api/estructuras/bst/construir` | Construye la copia de trabajo del BST |
| POST | `/api/estructuras/bst/{insertar\|buscar\|eliminar}` | Resultado de la operación, camino y tamaño antes y después |
| GET | `/api/estructuras/bst/recorrido?tipo&limite` | Primeros N códigos en inorden, preorden o postorden |
| GET | `/api/estructuras/bst/medidas` | Nodos, altura, altura ideal, promedio y los primeros 4 niveles |
| POST | `/api/estructuras/desbalance` | BST por ID ascendente (1.500) frente al BST global por IATA |
| GET | `/api/grafo/paises` | Los 237 países ordenados, cuáles están construidos y cuántos vértices tienen |
| POST | `/api/grafo/construir` | Tarea: construye los países elegidos |
| POST | `/api/grafo/construir-todos` | Tarea: construye todos (países, vértices, aristas, ms) |
| GET | `/api/grafo/conexiones?iata=` | Vecinos internos, rutas externas agrupadas por país y llegadas externas |
| POST | `/api/grafo/recorrido` | Tarea: BFS o DFS (origen, nivel máximo, fronteras) → niveles y primeros 160 visitados |
| POST | `/api/grafo/ruta` | Ruta más corta en vuelos, con los vecinos de cada escala |
| POST | `/api/grafo/ciclo` | Un ciclo de longitud 3 o más en el país |
| POST | `/api/grafo/diagrama-general` | Tarea: escribe `diagramas/ver_general_paises.html` y las páginas por país |
| POST | `/api/grafo/visor` | Tarea: escribe `diagramas/visor_grafo.html` |
| GET | `/api/ia/config` | Proveedores, modos y configuración actual (sin la clave; solo si existe y de dónde viene) |
| POST | `/api/ia/config` | Aplica proveedor, modelo, URL, modo y clave |
| POST | `/api/ia/validar` | Tarea: valida la conexión |
| POST | `/api/ia/funcion` | Tarea: una de las 10 funciones (más un resumen opcional) |
| POST | `/api/ia/documentacion` | Tarea: las 10 funciones y `DOCUMENTACION.md` |
| GET | `/api/ia/historial` | Resultados de IA de la sesión |
| GET | `/api/evidencias/{archivo}.md` | Contenido de una evidencia de `evidencias_ia/` |
| GET | `/api/apis` | APIs guardadas (nombre, proveedor, modelo, URL; **sin la clave**) |
| POST | `/api/apis` | Guarda o edita una API (si la clave viene vacía, conserva la anterior) |
| DELETE | `/api/apis/{nombre}` | Elimina una API guardada |
| POST | `/api/apis/{nombre}/usar` | Carga esa API en el servidor |

### 7.3 Pantallas (`web/index.html` y `web/js/`)

| Pestaña | Archivo | Qué hace | Endpoints |
|---|---|---|---|
| Datos | `datos.js` | Carga cruda (Hito 1) con lista enlazada o secuencial, modos de carga, tabla paginada, buscar y filtrar | `/api/datos/*` |
| ETL | `etl.js` | Pipeline en vivo (Extract → Cola → Transform → Válidos / Pila → Load), KPIs, gráficos, cola, pila por tipo, válidos, Load y visor de archivos; botones de IA para resumen y flujograma | `/api/etl/*`, `/api/output/*`, `/api/ia/funcion` |
| Estructuras | `estructuras.js` | 5 subpestañas (detalle abajo) | `/api/estructuras/*`, `/api/etl/ejecutar` |
| Grafo | `grafo.js` | Buscador de países con casillas; construir; grafo estrella; BFS/DFS animado; gráfico por nivel; ruta como cadena de vuelos; ciclo como anillo; diagrama general y visor dentro de la página | `/api/grafo/*`, `/api/etl/ejecutar` |
| IA | `ia.js`, `resultadoIa.js` | Tarjeta de conexión con indicador; 10 funciones agrupadas; diagrama Mermaid con zoom, pestañas Diagrama y Código y resumen debajo; historial; Gestionar APIs; documentación | `/api/ia/*`, `/api/apis*`, `/api/evidencias/*` |
| Reporte | `reporte.js` | Hoja A4 (portada, ETL, estructuras, grafo, IA). "No ejecutado en esta sesión" donde falta algo. "Descargar PDF" usa `window.print()` | `/api/reporte`, `/api/estado` |

Subpestañas de **Estructuras**:

- **Ordenamiento:** ordenar y comparar, más un visualizador paso a paso que compara sus conteos con los de Java.
- **Búsquedas:** secuencial, binaria y BST, con gráfico logarítmico y el camino en el árbol.
- **BST:** operaciones y árbol dibujado.
- **Recorridos.**
- **Desbalance.**

Comunes: `app.js` (pestañas, tema, `/api/estado`), `api.js` (`pedir` y `seguirTarea` con SSE), `interfaz.js` (panel de carga a partir de 500 ms con etapas ✓, toasts, modal).

Librerías en `recursos/`: Anime.js 4.5.0, AutoAnimate 0.10.0, Chart.js 4.6.1, Lucide 1.61.0, Mermaid 11.4.1, svg-pan-zoom 3.6.2 y vis-network 9.1.9. Sus licencias están en `recursos/licencias/`.

---

## 8. Qué cambió respecto a Swing

**Se migró** la interfaz: las 6 pestañas de Swing pasaron a la web en `web/` y `src/app/web/`. "Guardar APIs" dejó de ser una pestaña y quedó dentro de IA como "Gestionar APIs". Además se agregó:

- el pipeline ETL animado;
- el visualizador de ordenamiento validado contra Java;
- el árbol, el camino y el desbalance dibujados;
- el grafo estrella y el BFS/DFS animado con vis-network;
- los diagramas dentro de la página (antes se abría un HTML aparte o una imagen de mermaid.ink);
- el resumen bajo cada diagrama;
- el historial de IA;
- la pestaña Reporte para PDF.

**Quedó intacto:** todo el backend (`src/model`, `src/estructuras`, `src/etl`, `src/algoritmos`, `src/grafo`, `src/ia`) y `src/pruebas`.

- Se verificó con el archivo de sumas `../backend-swing-aprobado.sha256`, creado antes de la migración. El comando `sha256sum -c --quiet ../backend-swing-aprobado.sha256` no reporta diferencias, y las 220 pruebas pasan.
- Existe además un respaldo completo: `../respaldo-swing-aprobado-2026-11-13.zip`.

**`app/swing` sigue existiendo** en `src/app/swing/`. Solo cambió el `package` a `app.swing` y se agregaron los imports de `app.Sesion` y `app.Main`. Se abre con `java -cp out app.Main --swing`.

En `src/app/`:

- `Main.java` ahora decide entre web y Swing.
- `Sesion.java` ganó `tieneEtl`, `ultimoEtlEsReal` y `tieneEtlReal`; sigue sin depender de Swing.

**Observaciones**

- Las carpetas `src/diagramas/` y `src/evidencias_ia/` no son código: se generaron el 7 de noviembre al ejecutar el programa desde `src/`. Están en el respaldo; se pueden borrar si el grupo lo decide.
- La migración no está commiteada en Git: el usuario hace los commits.

---

## 9. Demo sugerida

Preparación:

- Compilar y ejecutar `java -cp out app.Main`.
- Si se quiere IA en vivo, tener la clave en la variable de entorno o en "Gestionar APIs". Si no, usar el modo "Solo local", que funciona sin internet.

1. **Datos (lista enlazada y secuencial).**
   - Cargar `airports.csv` con **ListaEnlazada** y modo "primeros 100".
   - Luego con **ListaSecuencial** y "todos" (7.698).
   - Buscar por IATA `UIO` y mostrar las comparaciones de la búsqueda secuencial.
   - Filtrar por país `Ecuador`.
2. **ETL (cola, pila, lista secuencial, BST de IDs).**
   - Ejecutar el ETL de **Demostración** para ver errores de todos los tipos.
   - Luego el ETL **real**: pipeline en vivo, 81.523 procesados, 589 errores en la pila (473 `REFERENCIA_ROTA` y 116 `ID_NULO_NO_RECUPERABLE`).
   - Pulsar **Cargar** y abrir `errores_etl.txt`.
3. **Estructuras › Ordenamiento.**
   - Comparar burbuja frente a inserción por IATA: inserción hace un 53,6 % menos de comparaciones.
   - Reproducir el visualizador con 12 aeropuertos y mostrar el chip "Java: N comparaciones ✓".
4. **Estructuras › Búsquedas y BST.**
   - Buscar `UIO`: 2.556 / 13 / 13 comparaciones y el camino en el BST.
   - Construir el BST, insertar y eliminar un código, ver altura 177 y los primeros niveles.
   - En **Desbalance**, mostrar la altura de 1.499 con claves ordenadas.
5. **Grafo (lista de adyacencia, BFS con cola, DFS con pila).**
   - Construir todos (225 países, menos de 1 s).
   - Ver conexiones de UIO (grafo estrella).
   - BFS desde UIO cruzando fronteras: 3.217 aeropuertos en 8 niveles, con el gráfico por nivel.
   - DFS sin cruzar: orden numerado.
6. **Grafo › Ruta y ciclo.**
   - UIO → CUZ: `UIO ✈ LIM ✈ CUZ · 2 vuelos`.
   - Detectar el ciclo en Ecuador: anillo GYE → UIO → OCC → LTX.
   - Abrir el diagrama general y hacer clic en un país.
7. **IA.**
   - Validar la API (indicador verde) o mostrar el modo solo local.
   - Generar el **Flujograma ETL** y el **Flujograma BFS**: diagrama con colores, chips IA o RESPALDO LOCAL y "✓ coherente", "Ver evidencia" y el resumen debajo.
   - **Explicar árbol**.
   - Reabrir un resultado desde el historial.
8. **Reporte.** Mostrar la hoja A4 con todo lo ejecutado y "Descargar PDF".

---

## 10. Propuesta de reparto en 6 partes

Los bloques tienen un peso parecido: cada uno tiene backend, la parte web de su pestaña y una o dos estructuras para explicar. Entre paréntesis, la responsabilidad que cada integrante tuvo en el Hito 1 según `README.md` §8, como sugerencia de asignación.

### Bloque 1: Modelo, lectura de CSV y listas (Hito 1)

Sugerido: Torosina Armendariz Jeremy (modelo) y apoyo de Llamuca Abrajan Andres y Romo Nunez Joseph (listas).

- `src/model/Registro.java`, `Aeropuerto.java`, `Ruta.java`, `Aerolinea.java`
- `src/etl/LectorCSV.java`, `Extract.java`, `Mapeador.java`, `FuenteDatos.java`, `OpcionesCarga.java`, `ModoCarga.java`, `ResultadoExtraccion.java`
- `src/estructuras/Lista.java`, `Criterio.java`, `Nodo.java`, `ListaSecuencial.java`, `ListaEnlazada.java`, `ResultadoBusqueda.java`
- Web: `src/app/web/ApiDatos.java`, `web/js/datos.js`
- Explicar: arreglo que duplica su capacidad frente a nodos con `head` y `tail`, modos de carga y búsqueda secuencial.

### Bloque 2: ETL con cola y pila

Sugerido: Tuza Quinatoa Noemi (Extract).

- `src/etl/ProcesoETL.java`, `Transform.java`, `DatosLimpios.java`, `Load.java`, `Proyecto.java`
- `src/estructuras/Cola.java`, `Pila.java`
- `src/model/ErrorETL.java`, `TipoError.java`
- Web: `src/app/web/ApiEtl.java`, `web/js/etl.js`
- Explicar:
  - por qué la cola (FIFO, orden del archivo) y la pila (errores);
  - las reglas de validación y limpieza;
  - cómo una ruta resuelve su ID (binaria o BST por IATA);
  - los 589 errores.

### Bloque 3: Ordenamiento y búsquedas

Sugerido: Llamuca Abrajan Andres.

- `src/algoritmos/Ordenamiento.java`, `ResultadoOrdenamiento.java`, `Comparadores.java`, `Busqueda.java`
- Web: `src/app/web/ApiEstructuras.java` (métodos `ordenar`, `muestra`, `buscar`) y, en `web/js/estructuras.js`, las secciones de ordenamiento, visualizador (`generarPasos`, `validarContraJava`) y búsquedas
- Explicar:
  - burbuja con corte temprano frente a inserción, contando comparaciones y movimientos;
  - binaria O(log n) frente a secuencial O(n);
  - cómo se valida la animación contra Java.

### Bloque 4: Árbol binario de búsqueda

Sugerido: Romo Nunez Joseph.

- `src/estructuras/ArbolBST.java`, `NodoArbol.java`
- Su uso en `src/etl/Transform.java` (IDs duplicados), `src/etl/DatosLimpios.java` (índice IATA) y `src/grafo/MapaPaises.java` (BST de países)
- Web: `src/app/web/ApiEstructuras.java` (métodos `construir`, `operar`, `recorrido`, `medidas`, `desbalance`) y, en `web/js/estructuras.js`, las secciones BST, Recorridos y Desbalance
- Explicar:
  - insertar, buscar y eliminar iterativos;
  - altura 177 frente a 1.499;
  - recorridos con límite de recursión;
  - promedio de comparaciones.

### Bloque 5: Grafo y recorridos

Sugerido: Chico Yunda Juan Carlos (líder).

- `src/grafo/Vertice.java`, `Grafo.java`, `GrafoPais.java`, `MapaPaises.java`, `Agrupacion.java`
- `src/algoritmos/BFS.java`, `DFS.java`, `Rutas.java`, `Ciclos.java`, `Vecinos.java`, `ResultadoRecorrido.java`
- Web: `src/app/web/ApiGrafo.java`, `web/js/grafo.js`
- Explicar:
  - la lista de adyacencia;
  - el grafo por país construido bajo demanda;
  - BFS con cola, DFS con pila;
  - la ruta con padre y pila;
  - el ciclo con estados gris y negro;
  - por qué el candado del grafo.

### Bloque 6: IA e interfaz web

Sugerido: Altamirano Segovia Jullisa (documentación y diagramas).

- `src/ia/ConfiguracionIA.java`, `ProveedorIA.java`, `ClienteCompatibleOpenAI.java`, `ClienteAnthropic.java`
- `src/ia/AnalizadorIA.java`, `GeneradorPrompt.java`, `GeneradorFlujogramaLocal.java`, `RegistroEventos.java`, `Evento.java`, `TrazasEstructuras.java`, `VisorHtml.java`, `AlmacenApis.java`
- `src/app/web/ApiIa.java`, `ApiApis.java`, `FuncionesIa.java`, `web/js/ia.js`, `web/js/resultadoIa.js`
- Servidor común (para la pregunta "¿cómo se comunica la web con Java?"): `src/app/web/ServidorWeb.java`, `Api.java`, `Seguridad.java`, `GestorTareas.java`, `TareaWeb.java`, `web/js/api.js`, `web/js/interfaz.js`, más el Reporte (`ReporteSesion.java`, `web/js/reporte.js`)
- Apoyo: `docs/GUIA-INTERFAZ-WEB.md`
- Explicar:
  - qué se envía a la IA y por qué no inventa (reglas más coherencia);
  - el respaldo local;
  - la seguridad de la clave;
  - el flujo navegador → `fetch` → endpoint → backend → JSON;
  - el SSE.

Si el bloque 6 resulta pesado, la parte del servidor común y el Reporte puede pasar al bloque 5, ya que el líder tuvo la arquitectura en el Hito 1.

Todos deberían poder contestar dos preguntas transversales:

- "¿Dónde se reutiliza cada estructura?" (sección 4).
- "¿Se modificó el backend para la web?" (sección 8).
