# SmartETL-DS: estado actual del proyecto (13 de noviembre de 2026)

Documento para planificar la migración de la interfaz Swing a web. Describe el código tal como está en `SmartETL_DS-main/` hoy. Los nombres de clases y métodos son los reales.

Resumen en una línea: aplicación de escritorio Java (Swing, solo JDK) que hace un ETL sobre los CSV de OpenFlights con estructuras de datos propias (listas, cola, pila, BST, grafo), y una capa de IA que genera flujogramas Mermaid y explicaciones a partir de eventos registrados durante la ejecución. 78 clases en 8 paquetes; la interfaz está toda en `src/app/` (12 clases).

Restricciones del proyecto (de `CLAUDE.md`, el documento de especificación del dueño): estructuras propias (prohibido `ArrayList`, `HashMap`, `Stack`, etc. en `estructuras`, `etl`, `algoritmos`, `grafo`, `ia`; solo se permiten en la capa de ventana si Swing los exige), solo JDK, sin comentarios en el código, compatible con JDK 11 (sin `record`, `var` ni bloques de texto). La única colección del JDK en `app/` es `java.util.List` en `Tarea.process` (exigida por `SwingWorker`) y en `JList.getSelectedValuesList()` de `PanelGrafo`.

---

## 1. Paquetes y clases

### `model/` (6): datos crudos

| Clase | Qué hace |
|---|---|
| `Registro` | Interfaz: `numeroCampos()`, `valor(int columna)`. La usan la tabla de la pestaña Datos y `Load` |
| `Aeropuerto` | 14 campos `String` de airports.csv; `desdeCampos(String[])`; sondas de búsqueda `conId(String)` y `conIata(String)` |
| `Ruta` | 9 campos de routes.csv; `desdeCampos` |
| `Aerolinea` | 8 campos de airlines.csv; `desdeCampos` |
| `ErrorETL` | fuente, línea, identificador, `TipoError`, motivo, valor original; `comoLinea()` |
| `TipoError` | Enum: `CAMPO_OBLIGATORIO`, `ID_INVALIDO`, `RANGO_INVALIDO`, `DUPLICADO`, `REFERENCIA_ROTA`, `ID_NULO_NO_RECUPERABLE` |

### `estructuras/` (10): estructuras propias

| Clase | Qué hace |
|---|---|
| `Lista<T>` | Interfaz: `insertarInicio`, `insertarFinal`, `obtener`, `tamanio`, `estaVacia`, `limpiar`, `buscar(Criterio)`, `filtrar`, `contar`, `descripcion` |
| `Nodo<T>` | Dato y siguiente |
| `ListaEnlazada<T>` | `head` y `tail`; además `primerNodo()`, `contiene(T)` (por identidad) y `setRegistro(RegistroEventos)` |
| `ListaSecuencial<T>` | Arreglo que duplica su capacidad; además `capacidad()`, `reemplazar(int, T)` y `copiaDe(Lista)` (estático) |
| `Cola<T>` | `encolar`, `desencolar`, `frente`, `estaVacia`, `tamanio`, `primeros(n)`, `mostrar`, `setRegistro`. Si está vacía lanza `IllegalStateException` |
| `Pila<T>` | `apilar`, `desapilar`, `cima`, `estaVacia`, `tamanio`, `primeros(n)`, `elementos()` (copia desde la cima), `mostrar`, `setRegistro` |
| `NodoArbol<T>` | Dato, izquierdo, derecho, `esHoja()` |
| `ArbolBST<T>` | Constructor con `Comparator<T>`. Iterativos: `insertar` (devuelve falso si ya existe), `buscar` (devuelve `ResultadoBusqueda` con índice = profundidad), `eliminar` (los 3 casos), `altura`, `contarNodos`, `promedioComparaciones`, `caminoHacia`, `primerosNiveles`. Recursivos con límite: `inOrden`, `preOrden`, `postOrden`, que lanzan `IllegalStateException` si la altura pasa de 2.000. También `setEtiquetador`, `setRegistro` |
| `Criterio<T>` | Interfaz funcional `cumple(T)` |
| `ResultadoBusqueda<T>` | Elemento, índice, comparaciones; `encontrado()` |

### `etl/` (12)

| Clase | Qué hace |
|---|---|
| `LectorCSV` | `separar(String)`: CSV con comillas y campos vacíos al final |
| `Extract` | `extraer(ruta, campos, mapeador, Lista, OpcionesCarga)` (Hito 1) y `extraerEnCola(…, Cola, …)`; no limpia nada |
| `OpcionesCarga`, `ModoCarga` | Modos TODOS, PRIMEROS, RANGO y ALEATORIO (con semilla) |
| `FuenteDatos` | Constantes `AEROPUERTOS`, `AEROLINEAS`, `RUTAS`: archivo por defecto, encabezados, mapeador |
| `Mapeador`, `ResultadoExtraccion` | Crear el objeto a partir de los campos; conteos de la extracción |
| `Transform` | `transformarAeropuertos(Cola)`, `transformarRutas(Cola)`, `transformarAerolineas(Cola)`; reglas de validación (sección 6); `getErrores()` devuelve la `Pila<ErrorETL>`; contadores por fuente y tipo; utilidades estáticas `esIataValido`, `esEntero`, `estaEnRango`, `limpio` |
| `DatosLimpios` | Listas de válidos, `ArbolBST<Aeropuerto>` por IATA, lista ordenada por ID; `buscarPorId` (búsqueda binaria), `buscarPorIata` (BST) |
| `Load` | `escribir(datos, transform, msExtract, msTransform, carpeta)` escribe 5 archivos; `textoReporte(…)` |
| `ProcesoETL` | Orquestador: `real()`, `demostracion()`, `ejecutar()`, `cargar(carpeta)`, `setAvisos(Consumer<String>)`, vistas de la cola y `reporte()`; tiene su propio `RegistroEventos` |
| `Proyecto` | Carpeta base del proyecto: `detectar()`, `usarCarpeta(Path)`, `ruta(String)`, `getBase()`, `tieneRecursos()` |

### `algoritmos/` (10)

| Clase | Qué hace |
|---|---|
| `Ordenamiento` | `burbuja(Lista, Comparator)` e `insercion(…)` sobre una copia; `estaOrdenada` |
| `ResultadoOrdenamiento` | Lista ordenada, comparaciones, movimientos, ms |
| `Comparadores` | `AEROPUERTO_POR_ID`, `_POR_IATA`, `_POR_NOMBRE`, `_POR_PAIS`; `porCampo(String)` |
| `Busqueda` | `secuencial(Lista, Criterio)`, `binaria(ListaSecuencial, T, Comparator)`, `porArbol(ArbolBST, T)` |
| `Vecinos` | `de(Vertice, MapaPaises)`: adyacentes, más externos si el mapa no es nulo; `etiquetas(…)` |
| `BFS` | `recorrer(origen, mapa o null, nivelMaximo, RegistroEventos)`, por capas, con `Cola` |
| `DFS` | `recorrer(origen, mapa o null, profundidadMaxima, RegistroEventos)`, iterativo, con `Pila` |
| `Rutas` | `buscar(origen, destino, mapa)`: BFS con padre; el camino se reconstruye con una `Pila` |
| `Ciclos` | `detectar(Grafo)`: DFS recursivo con estados gris y negro; ignora ciclos de longitud 2 |
| `ResultadoRecorrido` | Visitados, `nivelMaximo()`, `cantidadEnNivel(k)`, `paisesEnNivel(k)` |

### `grafo/` (5)

| Clase | Qué hace |
|---|---|
| `Vertice<T>` | dato, etiqueta, `adyacentes`, `externos`, `llegadasExternas` (todas `ListaEnlazada`); campos de trabajo `marca`, `nivel`, `padre`, `estado`; `nuevaCorrida()` (estático), `fueVisitado(corrida)`, `visitar(…)` |
| `Grafo<T>` | `agregarVertice`, `agregarArista` (sin duplicar), `buscarVertice(etiqueta)` (lineal), `numeroVertices`, `numeroAristas` |
| `GrafoPais` | Un `Grafo<Aeropuerto>` más nombre y contadores; `externosPorPais()`, `resumen()`; comparador `POR_NOMBRE` (sin distinguir mayúsculas) |
| `MapaPaises` | `ArbolBST<GrafoPais>` con 237 países; `obtener(pais)` construye el grafo si hace falta; `construirTodos()`; `vertice(Aeropuerto)` y `verticePorIata`, que usan el arreglo `verticePorId` (acceso O(1)); `construidos()` |
| `Agrupacion` | Conteo por clave con detalle (por ejemplo, rutas externas por país) |

### `ia/` (17)

| Clase | Qué hace |
|---|---|
| `Evento` | tipo, texto, sujeto, grupo, elementos, estado (primeros 12), tamaño del estado, nivel |
| `RegistroEventos` | `ListaEnlazada<Evento>` con un límite de 20.000 eventos; `agregar`, `registrar`, `contar(tipo)`, `ultimo(tipo)`, `comoTexto(limite)` |
| `TrazasEstructuras` | `cola()`, `pila()`, `lista()`, `bst()`: ejecutan una traza corta real y devuelven su registro |
| `GeneradorPrompt` | `flujogramaRecorrido`, `flujogramaEtl`, `flujogramaEstructura`, `explicacion(tema, datos)`, `datosArbol`, `datosGrafo`, `datosErrores` |
| `GeneradorFlujogramaLocal` | El respaldo sin IA: `recorrido`, `etl`, `estructura`, `generalPaises`, `grafoPais`, `resumenNivelesProfundos` |
| `Mermaid` | Constructor de diagramas `flowchart TD` (nodos, enlaces, `click`) |
| `ProveedorIA` | Interfaz `enviar(String prompt)` |
| `ClienteCompatibleOpenAI` | POST chat/completions con `Authorization: Bearer` |
| `ClienteAnthropic` | POST `/v1/messages` con `x-api-key`, `anthropic-version: 2026-10-21` y `max_tokens: 16000` |
| `ConfiguracionIA` | `PROVEEDORES`, proveedor, modelo, URL, clave (`char[]`), modo; `urlDe`, `variableDeEntorno`, `claveDeEntorno`, `ayudaModelo`, `ayudaClave`, `crearProveedor()`; espera de 30 s |
| `AnalizadorIA` | `diagrama(…)`, `texto(…)`, `validarConexion()`, `setAvisos`; estáticos `limpiarMermaid`, `coherencia`, `esperados*`, `conocidos`; guarda las evidencias |
| `ResultadoIA` | Contenido, si es diagrama, si usó la API, aviso, coherencia, ruta del HTML y de la evidencia |
| `Json` | `cadena(String)` (escapar) y `valorDeCadena(json, clave, desde)` (leer) |
| `VisorHtml` | `escribirDiagrama` (`.md` y `ver_*.html`), `escribirPagina`, `archivoPais`, `normalizar`, `abrir(Path)` con `Desktop.browse` |
| `VisorGrafo` | `escribir(MapaPaises, origen)`: `diagramas/visor_grafo.html` con los datos incrustados y vis-network |
| `ApiGuardada` | nombre, proveedor, modelo, url, clave; `descripcion()` sin la clave |
| `AlmacenApis` | `cargar`, `guardar` (actualiza si el nombre ya existe), `eliminar`, `buscar`, `escapar`, `separar`; archivo `~/.smartetl/apis.properties` |

### `app/` (12): interfaz Swing (sección 2)

`Main`, `VentanaPrincipal`, `Sesion`, `Tarea`, `Animacion`, `PanelDatos`, `ModeloTablaRegistros`, `PanelETL`, `PanelEstructuras`, `PanelGrafo`, `PanelIA`, `PanelGuardarApis`.

### `pruebas/` (5)

`PruebasHito1` (53 verificaciones), `PruebasHito2` (55), `PruebasHito3` (32), `PruebasHito4` (20), `PruebasIA` (60). Son programas con `main`, sin JUnit; terminan con `fallaron: 0`. No abren ventanas: prueban el backend directamente.

---

## 2. Interfaz Swing (`app/`)

### Clases de soporte

| Clase | Papel |
|---|---|
| `Main.main` | `invokeLater`: look and feel del sistema → `Proyecto.detectar()`; si falla, `pedirCarpeta()` (diálogos `JOptionPane` y un `JFileChooser` de solo carpetas, en bucle hasta encontrar `airports.csv`) → `avisarSiFaltanRecursos()` → `new VentanaPrincipal()`, cuyo título incluye la carpeta. También tiene `explicacionCarpeta()` |
| `VentanaPrincipal` | `JFrame` "SmartETL-DS" de 1320×760 con un `JTabbedPane` de 6 pestañas: Datos, ETL, Estructuras, Grafo, IA, Guardar APIs. Crea una sola `Sesion` compartida |
| `Sesion` | Estado compartido, con métodos `synchronized`: `datosReales()` (ejecuta `ProcesoETL.real()` la primera vez), `ultimoEtl()`, `mapa()` (crea `MapaPaises` de forma perezosa), `listaEnlazada()` (copia de los aeropuertos en una `ListaEnlazada`), `registrarEtl`, `registrarBfs`, `registrarDfs`, `getUltimoBfs`, `getUltimoDfs`, `getNivelDiagrama*`, `getUltimoOrigen`, `get/setUltimoPais`, `getConfiguracion()` (un único `ConfiguracionIA`), `getApisGuardadas()` (`AlmacenApis`), `setAlCambiarApis` y `avisarCambioApis` (avisos entre pestañas) |
| `Tarea` | `ejecutar(panel, lblEstado, mensaje, Trabajo, AlTerminar)` y `ejecutarConAvisos(…, TrabajoConAvisos, …)`: un `SwingWorker` que publica mensajes de progreso, pone cursor de espera y, si hay error, muestra un diálogo "No se pudo completar" (con `NoSuchFileException` añade la explicación de la carpeta). Además tiene ayudantes de interfaz: `fila`, `areaTexto`, `conBorde`, `boton`, `entero` |
| `Animacion` | Un `javax.swing.Timer` de 400 ms que pinta "⏳ mensaje... N s" en una `JLabel` |

Todas las pestañas usan una `JLabel lblEstado` abajo como barra de estado. Los resultados se muestran casi siempre en una `JTextArea` monoespaciada de solo lectura; la única tabla de verdad es la de la pestaña Datos.

### Pestaña Datos: `PanelDatos` (el Hito 1, sin cambios de comportamiento)

| Control | Acción | Backend |
|---|---|---|
| `cmbFuente` (Aeropuertos, Aerolíneas, Rutas) | Cambia `txtArchivo` al archivo por defecto | `FuenteDatos.getArchivoPorDefecto()` |
| `txtArchivo` y botón "Examinar..." | `JFileChooser` que empieza en `data/` | |
| `cmbLista` ("Lista enlazada (head y tail)" o "Lista secuencial (arreglo)") | Elige la estructura de destino | `new ListaEnlazada` o `new ListaSecuencial` |
| `cmbModo` (`ModoCarga`) y los campos N, Desde, Hasta, Semilla | Habilita los campos según el modo | `OpcionesCarga.primeros`, `rango`, `aleatorio`, `todos` |
| Botón "Cargar" | Carga el CSV crudo en la lista y lo muestra en la tabla | `Extract.extraer(Proyecto.ruta(ruta), …)`. **Corre en el hilo de Swing (EDT)** |
| `cmbColumna`, `txtBusqueda` y botón "Buscar (primera coincidencia)" (también con Enter) | Selecciona la fila y muestra la posición, las comparaciones y los ms | `Lista.buscar(Criterio)`, en el EDT |
| Botón "Filtrar (contiene)" | Muestra las coincidencias | `Lista.filtrar`, en el EDT |
| Botón "Ver todos" | Vuelve a mostrar la lista cargada | |
| `JTable` con `ModeloTablaRegistros` (`AbstractTableModel` sobre `Lista<Registro>`) y `RenderizadorCeldas` | Pinta `\N` en rojo y los vacíos en amarillo | `getValueAt` llama a `lista.obtener(fila)`, que es O(n) por celda con `ListaEnlazada` |
| Diálogo de error | "Opciones de carga no válidas" o "No se pudo leer el archivo" | `JOptionPane.showMessageDialog` |

### Pestaña ETL: `PanelETL`

| Control | Acción | Backend |
|---|---|---|
| Botón "Ejecutar ETL (datos reales)" | Ejecuta el ETL; luego `mostrar(etl)` llena las 4 áreas | `ProcesoETL.real()`, `setAvisos`, `ejecutar()`, `sesion.registrarEtl(etl, true)`. En `SwingWorker` con avisos por etapa |
| Botón "Ejecutar demostración (archivos sucios de data/demo)" | Lo mismo con `data/demo/airports_sucio.csv` y `routes_sucio.csv` (aerolíneas reales) | `ProcesoETL.demostracion()`, `registrarEtl(etl, false)` |
| Botón "Cargar (Load a output/)" | Escribe los 5 archivos | `etl.cargar("output")` → `Load.escribir`, en `SwingWorker` |
| `txtCola` | Los primeros 20 registros encolados por fuente | `getVistaCola(f)`, `getEncolados(f)` |
| `txtValidos` | Los primeros 10 válidos de cada lista | `DatosLimpios.getAeropuertos()`, `getRutas()`, `getAerolineas()` |
| `txtErrores` | Los primeros 60 errores desde la cima | `getTransform().getErrores().elementos()` |
| `txtContadores` (dentro de un `JSplitPane`) | El reporte | `ProcesoETL.reporte()` → `Load.textoReporte` |

### Pestaña Estructuras: `PanelEstructuras` (todo en `SwingWorker` con `Tarea.ejecutar`)

| Control | Acción | Backend |
|---|---|---|
| `cmbAlgoritmo` (Burbuja o Inserción), `cmbCampo` (IATA, Nombre, País) y botón "Ordenar" | Muestra comparaciones, movimientos, ms y los primeros 15 | `Ordenamiento.burbuja` o `insercion(datos.getAeropuertos(), Comparadores.porCampo(campo))`. Si se ordenó por IATA, guarda `ordenadaPorIata` |
| `txtBuscar` y botón "Secuencial vs binaria vs BST" | Tabla de texto con comparaciones y ms de las tres búsquedas, más el camino en el BST | `Busqueda.secuencial(sesion.listaEnlazada(), …)`, `Busqueda.binaria(ordenadaPorIata, …)` (si no hay lista ordenada, ordena antes con inserción), `Busqueda.porArbol(datos.getArbolIata(), …)` |
| Botón "Construir" | Crea `arbolTrabajo`, una copia del BST por IATA | Inserta los aeropuertos con IATA uno por uno en un `ArbolBST` |
| `txtClave` y los botones "Insertar", "Buscar", "Eliminar" | Opera sobre `arbolTrabajo` y muestra el camino, los nodos y la altura | `ArbolBST.insertar`, `buscar`, `eliminar`, `caminoHacia` |
| `cmbRecorrido` (Inorden, Preorden, Postorden), `txtLimite` y botón "Recorrer" | Lista de códigos IATA | `inOrden`, `preOrden` o `postOrden(limite)` |
| Botón "Altura y nodos" | Medidas y primeros niveles | `contarNodos`, `altura`, `promedioComparaciones`, `primerosNiveles(4, 8)` |
| Botón "BST por ID ascendente vs BST por IATA" | Demostración del desbalance | `ArbolBST` con `AEROPUERTO_POR_ID` y los primeros 1.500 aeropuertos |
| `txtSalida` | Área de resultado | |

### Pestaña Grafo: `PanelGrafo` (todo en `SwingWorker`)

| Control | Acción | Backend |
|---|---|---|
| `lstPaises` (`JList` de selección múltiple) | Se llena al mostrar la pestaña por primera vez (`AncestorListener`), en orden alfabético | `sesion.mapa()` (puede disparar el ETL) y `Ordenamiento.insercion(mapa.getPaises(), GrafoPais.POR_NOMBRE)` |
| Botón "Construir seleccionados" | Muestra el resumen de cada país | `mapa.obtener(nombre)` |
| Botón "Construir todos (con tiempo)" | Muestra los países con vértices y los ms | `mapa.construirTodos()`, `paisesConVertices()` |
| Botón "Diagrama general con clic por país" | Escribe `grafo_<País>.html` por cada país construido y `ver_general_paises.html`, y abre esta última en el navegador | `GeneradorFlujogramaLocal.grafoPais` y `generalPaises`, `VisorHtml.escribirPagina`, `escribirDiagrama` y `abrir` |
| Botón "Abrir visor interactivo" | Construye todos los países y abre `diagramas/visor_grafo.html` | `mapa.construirTodos()`, `VisorGrafo.escribir(mapa, origen)`, `VisorHtml.abrir` |
| `txtAeropuerto` y botón "Ver conexiones" | Rutas internas, externas agrupadas por país y llegadas | `mapa.verticePorIata`, `GrafoPais.externosPorPais(v)` |
| `txtOrigen`, `txtNivelMaximo` (vacío = sin límite), `txtNivelDiagrama`, `chkFronteras` y los botones "BFS" y "DFS" | Aeropuertos por nivel o en orden de visita; guarda el registro para la pestaña IA | `BFS.recorrer` o `DFS.recorrer(origen, chkFronteras ? mapa : null, …)`, `sesion.registrarBfs` o `registrarDfs` |
| `txtRutaOrigen`, `txtRutaDestino` y botón "Buscar ruta" | El camino con la cantidad de vuelos | `Rutas.buscar` |
| `txtPaisCiclo` y botón "Detectar ciclo" | El ciclo encontrado | `Ciclos.detectar(mapa.obtener(pais).getGrafo())` |
| `txtSalida` | Área de resultado | |

### Pestaña IA: `PanelIA`

| Control | Acción | Backend |
|---|---|---|
| `cmbProveedor` (Gemini, Claude, OpenAI, GLM, GitHub Models (Copilot)) | Recarga la clave desde la variable de entorno (**sobrescribe el campo de clave, aunque quede vacío**), rellena la URL por defecto y el texto de ayuda | `ConfiguracionIA.claveDeEntorno`, `urlDe`, `ayudaModelo`, `ayudaClave` |
| `txtModelo` | Texto libre, sin catálogo | |
| `txtClave` (`JPasswordField`) | | |
| `cmbModo` ("API en vivo + respaldo local", "Solo local") | | |
| `cmbGuardadas` ("API guardada") | Llena proveedor, modelo, URL y clave | `sesion.getApisGuardadas().buscar(nombre)` → `cargarApi(ApiGuardada)` |
| `txtUrl` | Editable | `ConfiguracionIA.setUrl` |
| `lblAyuda` | Formato del modelo y tipo de clave | |
| Botón "Validar API" y `lblApi` (gris, amarillo, verde o rojo) | Envía el prompt "Responde únicamente con la palabra OK." | `AnalizadorIA.validarConexion()` en un `SwingWorker` propio. Cualquier cambio en modelo, clave o URL devuelve el indicador a "sin validar" (`DocumentListener`) |
| `cmbVista` ("HTML local con zoom (sin internet)" o "Imagen dentro de la ventana (mermaid.ink, requiere internet)") y botón "Ver diagrama" | Abre el último HTML en el navegador, o pide un PNG a mermaid.ink y lo muestra en un `JDialog` con "Acercar", "Alejar" y "100 %". Si falla, un `showConfirmDialog` ofrece abrir el HTML | `VisorHtml.abrir`, o `pedirImagen` (`HttpClient` GET `https://mermaid.ink/img/<base64url>?type=png`) |
| Botones "Flujograma ETL", "Flujograma Cola", "Pila", "Lista enlazada", "BST", "Flujograma BFS", "Flujograma DFS" | Diagrama Mermaid | `diagramaEtl`, `diagramaEstructura`, `diagramaRecorrido` → `AnalizadorIA.diagrama(…)` |
| Botones "Explicar árbol", "Explicar grafo", "Analizar errores ETL" | Texto | `textoArbol`, `textoGrafo`, `textoErrores` → `AnalizadorIA.texto(…)` |
| Botón "Generar documentación" | Las 10 funciones en secuencia (avisa "Documentación k de 10") y escribe `diagramas/DOCUMENTACION.md` | `documentacion()`, `escribirDocumentacion` |
| `txtResultado` | Código Mermaid o texto | |
| `lblOrigen`, `lblAviso`, `lblCoherencia`, `lblEstado` | Si respondió la API o el respaldo, el aviso, la coherencia y las rutas del HTML y la evidencia | `ResultadoIA` |

Cada función crea un `AnalizadorIA` nuevo en `analizador()`, que copia los campos de la pantalla al `ConfiguracionIA` compartido de la `Sesion`. Todas corren en `Tarea.ejecutarConAvisos`.

### Pestaña Guardar APIs: `PanelGuardarApis`

| Control | Acción | Backend |
|---|---|---|
| Aviso amarillo | "Las claves se guardan en texto plano en tu computador. No las compartas ni las subas a GitHub." | |
| `txtNombre`, `cmbProveedor` (al elegir, rellena la URL y la ayuda), `txtModelo`, `txtUrl`, `txtClave` y `chkMostrar` (muestra u oculta la clave) | Formulario | |
| Botón "Guardar" | Guarda, o actualiza si el nombre ya existe (sin distinguir mayúsculas). Si falta el nombre, avisa con un diálogo | `AlmacenApis.guardar`, `refrescarLista()` → `sesion.avisarCambioApis()` (refresca `cmbGuardadas` en la pestaña IA) |
| Botón "Eliminar" | Pide confirmación (`showConfirmDialog`) y borra | `AlmacenApis.eliminar` |
| Botón "Cargar en la pestaña IA" (también doble clic en la lista) | Llena la pestaña IA y la selecciona | `panelIA.cargarApi(api)` y el callback que cambia de pestaña |
| Botón "Limpiar" | Vacía el formulario | |
| `lstApis` | Muestra "nombre \| proveedor \| modelo", nunca la clave; al seleccionar, llena el formulario (incluida la clave) | `ApiGuardada.descripcion()` |

Todo esto ocurre en el hilo de Swing; son escrituras pequeñas a disco.

---

## 3. Flujo completo del usuario

1. **Arranque.** `Main` detecta la carpeta del proyecto (`Proyecto.detectar()` sube por las carpetas superiores y mira las subcarpetas del directorio actual buscando `data/airports.csv`). Si no la encuentra, pide elegir `data` o la raíz. Luego `Sesion()` lee `~/.smartetl/apis.properties`, creándolo vacío si no existe, y se abre la ventana en la pestaña Datos.
2. **(Opcional) Datos.** Carga cruda de un CSV a una lista, con búsqueda y filtro. No afecta al resto.
3. **ETL.** "Ejecutar ETL (datos reales)":
   - Extract encola airports.csv, Transform valida;
   - después routes.csv, cuyos IDs se resuelven con la lista por ID y el BST por IATA;
   - después airlines.csv.

   Unos 2,3 s. Se guarda en `Sesion` como ETL real y último ETL. "Cargar" escribe `output/`.
4. **Si el usuario va directo a otra pestaña sin pasar por el ETL**, `Sesion.datosReales()` lo ejecuta en segundo plano la primera vez que hace falta.
5. **Estructuras.** Usa `datosReales()`: ordena una copia, compara búsquedas y opera una copia del BST.
6. **Grafo.**
   - Al entrar se crea `MapaPaises`: se insertan los 237 países en el BST y se resuelve el origen y destino de las 67.074 rutas.
   - El usuario construye países, corre BFS o DFS (el registro de eventos queda en `Sesion`), busca rutas y ciclos, y abre el drill-down o el visor en el navegador.
7. **IA.**
   - Elige proveedor, modelo, URL y clave, o una API guardada. Opcionalmente pulsa Validar API.
   - Pulsa una función. `AnalizadorIA` arma el prompt con `GeneradorPrompt`.
   - Si el modo es API: llama a `crearProveedor().enviar(prompt)`, limpia la respuesta con `limpiarMermaid` y, si no sirve, usa `GeneradorFlujogramaLocal`.
   - Calcula la coherencia, escribe `diagramas/<nombre>.md` y `ver_<nombre>.html`, y guarda la evidencia en `evidencias_ia/`.
   - Muestra el resultado. "Ver diagrama" lo abre en el navegador.
   - Si no hubo BFS o DFS antes, `diagramaRecorrido` corre uno desde UIO dentro de Ecuador.
8. **Guardar APIs.** Guarda configuraciones y las carga en la pestaña IA.

---

## 4. Capa de IA

### Proveedores (`ConfiguracionIA.PROVEEDORES`)

| Proveedor | Cliente | URL por defecto (editable en la pantalla) | Variable de entorno |
|---|---|---|---|
| Gemini | `ClienteCompatibleOpenAI` | `https://generativelanguage.googleapis.com/v1beta/openai/chat/completions` | `GEMINI_API_KEY` |
| Claude | `ClienteAnthropic` | `https://api.anthropic.com/v1/messages` | `ANTHROPIC_API_KEY` |
| OpenAI | `ClienteCompatibleOpenAI` | `https://api.openai.com/v1/chat/completions` | `OPENAI_API_KEY` |
| GLM | `ClienteCompatibleOpenAI` | `https://api.z.ai/api/paas/v4/chat/completions` (para China: `https://open.bigmodel.cn/api/paas/v4/chat/completions`) | `GLM_API_KEY` |
| GitHub Models (Copilot) | `ClienteCompatibleOpenAI` | `https://models.github.ai/inference/chat/completions` (token con permiso `models:read`; modelo como `openai/gpt-4.1-mini`) | `GITHUB_TOKEN` |

- **Petición chat/completions:** `{"model":…, "messages":[{"role":"user","content": prompt}]}`. Se lee `choices[0].message.content` con `Json.valorDeCadena`.
- **Petición de Anthropic:** se agrega `max_tokens: 16000`, y se lee el primer bloque `"type":"text"`, saltando los bloques de tipo thinking.
- **Peticiones sin streaming**, sin `temperature`, con 30 s de espera.
- **Errores:** si la respuesta no es HTTP 200, se lanza `IOException("HTTP <código>: <message o cuerpo>")`.

### Claves

- **Entrada:** el `JPasswordField` de la pestaña IA, precargado desde la variable de entorno del proveedor. `ConfiguracionIA` guarda la clave como `char[]` en memoria.
- **Persistencia opcional:** `~/.smartetl/apis.properties` (`AlmacenApis.archivoPorDefecto()`), en texto plano, una línea por API con el formato `nombre|proveedor|modelo|url|clave`. `|` se escapa como `\|`, `\` como `\\` y el salto de línea como `\n`. En sistemas POSIX se intenta poner permisos `rw-------`; en Windows no. `.gitignore` incluye `apis.properties` y `*.keys`.
- **Dónde nunca se escribe:** evidencias, HTML, `.md` ni mensajes de error. `PruebasIA.pruebasClaveNoSeFiltra` lo verifica.

### Qué se envía a la IA

| Función | Contenido del prompt |
|---|---|
| Flujograma ETL | Reglas más `RegistroEventos` del último ETL: lecturas, conteos encolados, hasta 3 válidos y 5 inválidos de ejemplo por fuente, totales y archivos escritos |
| Flujograma Cola, Pila, Lista, BST | Reglas más los eventos de una traza fija (códigos IATA o nombres de tipos de error) |
| Flujograma BFS y DFS | Reglas, el ejemplo de formato y los eventos con nivel ≤ `nivelDiagrama` (hasta 250), más el resumen por nivel de los niveles más profundos |
| Explicar árbol | Nodos, altura, promedio, primeros 4 niveles, el camino de UIO y las comparaciones en la lista |
| Explicar grafo | Resumen del país, los 5 aeropuertos con más salidas, las rutas externas por país y un ciclo |
| Analizar errores ETL | Solo conteos por fuente y por tipo |

Las reglas del prompt piden: responder en español, solo un bloque Mermaid que empiece con `flowchart TD`, etiquetas entre comillas dobles, saltos de línea con `<br/>` y no inventar datos. Solo se envían datos públicos de OpenFlights o conteos.

### Qué vuelve y qué se muestra

- **Para diagramas:** se aplica `limpiarMermaid` (quita las cercas y exige que empiece con `flowchart` o `graph`). Luego `coherencia(…)` compara los esperados del registro con los códigos de tres letras del diagrama y devuelve "faltan: … | sobran: …". Se escriben `diagramas/<nombre>.md` y `diagramas/ver_<nombre>.html` (Mermaid 11.4.1 y svg-pan-zoom locales, con `securityLevel: 'loose'` y el código en un `textarea`).
- **Para textos:** el texto de la IA o, si falla, "Respaldo local (sin IA). Datos reales usados: …".
- **Evidencia** (solo si el modo no es "Solo local"): `evidencias_ia/AAAAMMDD_HHMMSS_mmm_<función>.md`, con fecha, proveedor, modelo, si se usó el respaldo, notas, prompt, respuesta cruda y resultado.
- **Respaldo automático** cuando no hay clave, no hay modelo, hay un error HTTP o de red, se agota el tiempo o la respuesta no es un diagrama válido.

**Uso real registrado:** hay 20 evidencias de los días 7 y 12 de noviembre, todas con Gemini (`gemini-3.6-flash`). 18 respondieron por API; 2 cayeron al respaldo por un `HTTP 503` ("high demand") de Gemini.

---

## 5. Estructuras de datos: dónde y para qué

| Estructura | Dónde | Operaciones |
|---|---|---|
| `ListaSecuencial` | Listas de válidos en `DatosLimpios` (aeropuertos 7.698, rutas 67.074, aerolíneas 6.162) y la lista de aeropuertos por ID | `insertarFinal`; `obtener(i)` O(1); búsqueda binaria sobre la lista por ID (`buscarPorId`); recorridos completos en `MapaPaises` |
| `ListaSecuencial` | `Ordenamiento` (copia con `copiaDe`), `ResultadoOrdenamiento`, `PanelEstructuras.ordenadaPorIata` | `reemplazar` para burbuja e inserción; búsqueda binaria |
| `ListaSecuencial` | Resultados de BFS, DFS, `Rutas`, `Ciclos`, `Vecinos`, `ArbolBST` (recorridos, caminos, niveles), `Pila.elementos()`, `Agrupacion`, `AlmacenApis.apis`, `MapaPaises.paises` | `insertarFinal`, `obtener`, `contar`, `filtrar`. Eliminar no existe en `Lista`: `AlmacenApis.eliminar` reconstruye la lista sin el elemento |
| `ListaEnlazada` | `Vertice.adyacentes`, `externos`, `llegadasExternas`; `Grafo.vertices` | `insertarFinal`; `contiene` (por identidad) para no duplicar aristas; recorrido con `primerNodo()` |
| `ListaEnlazada` | `RegistroEventos.eventos` | `insertarFinal`, recorrido, `contar` |
| `ListaEnlazada` | `Sesion.listaEnlazada()` (copia de los aeropuertos para la búsqueda secuencial de la pestaña Estructuras) y `TrazasEstructuras.lista()` | `buscar` con conteo de comparaciones; `insertarInicio` e `insertarFinal` en la traza |
| `ListaEnlazada` o `ListaSecuencial` | `PanelDatos` (a elección del usuario) y `ModeloTablaRegistros` | `insertarFinal` (Extract), `buscar`, `filtrar`, `obtener(fila)` para la tabla |
| `Cola` | `ProcesoETL` (una por fuente), `BFS`, `Rutas`, `ArbolBST.altura` y `promedioComparaciones`, `TrazasEstructuras.cola()` | `encolar`, `desencolar`, `tamanio`, `primeros` |
| `Pila` | `Transform.errores` (589 en los datos reales), `DFS`, `Rutas.reconstruir`, `Ciclos.construirCiclo`, `ArbolBST.contarNodos` | `apilar`, `desapilar`, `elementos` |
| `ArbolBST` | `DatosLimpios.arbolIata` (6.071 nodos, altura 177), el BST de IDs en `Transform` (para detectar duplicados), `MapaPaises.arbol` (237 países, altura 15) y la copia de trabajo de `PanelEstructuras` | `insertar`, `buscar`, `eliminar`, recorridos, `altura` |
| Arreglos simples | `MapaPaises.verticePorId`, `origenes`, `destinos`; los contadores de `Transform` (indexados por `TipoError.ordinal()`) | Acceso O(1) |

Ordenar solo se aplica a copias de `datos.getAeropuertos()` (pestaña Estructuras), a la lista por ID (inserción, sobre una lista que ya viene casi ordenada), a la lista de países (por nombre) y a los vértices en `GeneradorPrompt.datosGrafo`.

---

## 6. Datos

**Entrada** (CSV sin encabezado, nulos como `\N`, codificación UTF-8 y `LectorCSV` propio):

| Archivo | Filas | Campos |
|---|---|---|
| `data/airports.csv` | 7.698 | 14 |
| `data/routes.csv` | 67.663 | 9 |
| `data/airlines.csv` | 6.162 | 8 |
| `data/demo/airports_sucio.csv` | 15 | Demostración de errores |
| `data/demo/routes_sucio.csv` | 7 | Demostración de errores |

**Limpieza (`Transform`):**

- **Aeropuertos:**
  - `id` entero (si no, `ID_INVALIDO`).
  - `nombre` y `pais` obligatorios (`CAMPO_OBLIGATORIO`).
  - Latitud en [-90, 90] y longitud en [-180, 180] (`RANGO_INVALIDO`).
  - `id` repetido (`DUPLICADO`, detectado con el BST de IDs).
  - IATA válido solo si son 3 letras A-Z; si no, "Sin dato".
  - `\N` y los campos vacíos pasan a "Sin dato"; se recorta el espacio sobrante.
- **Rutas:**
  - aerolínea, origen y destino obligatorios.
  - Si un ID es `\N`, se busca el código en el BST por IATA (`ID_NULO_NO_RECUPERABLE` si no está).
  - Si el ID no existe en airports, `REFERENCIA_ROTA` (se comprueba con búsqueda binaria en la lista por ID).
  - Las rutas válidas guardan los IDs ya resueltos.
- **Aerolíneas:** `id` entero, nombre obligatorio, `DUPLICADO`.

Resultado con los datos reales: 0 errores en aeropuertos y aerolíneas; 589 en rutas (473 `REFERENCIA_ROTA` y 116 `ID_NULO_NO_RECUPERABLE`).

**Salida:**

| Destino | Contenido |
|---|---|
| `output/` (en `.gitignore`), con "Cargar" | `airports_limpios.csv`, `routes_limpias.csv`, `airlines_limpias.csv` (todos los campos entre comillas), `errores_etl.txt` (desde la cima de la pila), `reporte_etl.txt` |
| `diagramas/` (versionado) | `flujo_*.md`, `ver_flujo_*.html`, `general_paises.md`, `ver_general_paises.html`, `grafo_<País>.html` (hoy hay 225), `visor_grafo.html` (unos 600 KB con los datos incrustados), `DOCUMENTACION.md`; `Diagrama.md` es del Hito 1. En total, unos 2,6 MB |
| `evidencias_ia/` (versionado) | Un `.md` por llamada a la IA |
| `~/.smartetl/apis.properties` | APIs guardadas |

Todas las rutas se resuelven con `Proyecto.ruta(…)` contra la carpeta detectada. Las páginas HTML cargan `../recursos/*.js` con rutas relativas, así que `diagramas/` y `recursos/` deben ser carpetas hermanas.

---

## 7. Operaciones lentas y cómo se ejecutan hoy

| Operación | Duración aproximada (medida) | Cómo se ejecuta |
|---|---|---|
| ETL completo (`ProcesoETL.ejecutar`) | ~2,3 s (Extract ~0,8 s, Transform ~1,5 s; el BST de IDs degenerado aporta ~30 M comparaciones) | `SwingWorker` (`Tarea.ejecutarConAvisos`) con avisos por etapa; también puede dispararse implícitamente desde `Sesion.datosReales()` dentro de otro worker |
| Load | < 1 s | `SwingWorker` |
| Carga cruda en la pestaña Datos | ~0,1-0,25 s (airports) | **Hilo de Swing** (código del Hito 1) |
| Burbuja sobre 7.698 | ~0,9 s (29,6 M comparaciones) | `SwingWorker` |
| Crear `MapaPaises` y construir los 225 países | < 1 s | `SwingWorker` |
| BFS mundial | ~0,1 s | `SwingWorker` |
| Generar el drill-down (225 páginas) o el visor | 1 a 3 s | `SwingWorker` + `Desktop.browse` |
| Llamada a la IA | Hasta 30 s por llamada; "Generar documentación" hace hasta 10 en serie | `SwingWorker` con avisos; el indicador animado lo pinta un `javax.swing.Timer` |
| Validar API | Hasta 30 s | `SwingWorker` propio en `PanelIA.validarApi` |
| Imagen de mermaid.ink | Depende de la red | `SwingWorker` |

**Concurrencia:**

- Los métodos de `Sesion` son `synchronized`.
- Los botones **no se deshabilitan** mientras una tarea corre, así que se pueden lanzar tareas simultáneas.
- `ConfiguracionIA` es un único objeto compartido en `Sesion` que `PanelIA.analizador()` modifica desde el hilo de Swing, mientras un worker anterior puede estar leyéndolo.
- No hay cancelación de tareas.

---

## 8. Dependencias, configuración y ejecución

- **Lenguaje:** Java. El código es compatible con JDK 11 (compila con `--release 11`). `.idea/misc.xml` declara `languageLevel JDK_17` y el SDK "17"; durante el desarrollo se compiló y probó con Temurin 21.
- **Dependencias:** ninguna fuera del JDK: sin jars, sin Maven ni Gradle. Es un proyecto simple de IntelliJ (`SmartETL_DS.iml`, carpeta de fuentes `src`, salida `out`).
- **Módulos del JDK usados:** `java.desktop` (Swing, AWT, `ImageIO`, `Desktop`), `java.net.http` y `java.base`.
- **Recursos web locales** (`recursos/`, versiones fijas descargadas de npm vía jsDelivr, con sus licencias en `recursos/licencias/`): `mermaid.min.js` 11.4.1, `svg-pan-zoom.min.js` 3.6.2 y `vis-network.min.js` 9.1.9 (`standalone/umd`).
- **Ejecución** (desde `SmartETL_DS-main` en `cmd`; la ruta tiene espacios, por eso se compila con comodines por paquete y no con `@fuentes.txt`):

  ```
  javac -encoding UTF-8 -d out src\model\*.java src\estructuras\*.java src\etl\*.java src\algoritmos\*.java src\grafo\*.java src\ia\*.java src\app\*.java src\pruebas\*.java
  java -cp out app.Main
  java -cp out pruebas.PruebasHito2
  ```

  En IntelliJ, se ejecuta `src/app/Main.java`; si la carpeta de trabajo no es la raíz, `Proyecto.detectar()` la encuentra.
- **Internet:** solo hace falta para las llamadas a la IA y para mermaid.ink. Todo lo demás funciona sin red.
- **Git:** la copia de trabajo actual **no es un repositorio Git** (se descomprimió de un zip de GitHub). El remoto original es `https://github.com/juanchicoyunda1234/SmartETL_DS`. No se ha hecho ningún commit de los hitos 2 a 5.

---

## 9. Problemas conocidos y cosas a medio terminar

1. **La pestaña Datos bloquea la ventana:** carga, busca y filtra en el hilo de Swing. Además `ModeloTablaRegistros.getValueAt` llama a `lista.obtener(fila)`, que con `ListaEnlazada` es O(n) por celda, así que desplazarse con 67.663 rutas en lista enlazada es lento.
2. **No hay bloqueo de botones ni cancelación** de tareas en segundo plano. El `ConfiguracionIA` compartido puede cambiar mientras corre una llamada.
3. **Cambiar de proveedor en la pestaña IA reemplaza la clave escrita** por la de la variable de entorno, aunque esta esté vacía.
4. **GitHub Models no está verificado contra el servicio real:** desde el entorno de desarrollo, `models.github.ai` respondía `200 OK` en texto plano (la red interceptaba ese dominio). El manejo de 401, 403 y 429 es genérico y se probó con Anthropic (401) y Gemini (400 y 503).
5. **La visualización "imagen dentro de la ventana"** (mermaid.ink) está implementada pero sin probar con la red real. El "dibujo interno con Java2D" de la especificación (opcional) **no se implementó**.
6. **Gemini devolvió `HTTP 503` por alta demanda** en 2 de 20 llamadas; se usó el respaldo local. No hay reintentos.
7. **Carpetas sobrantes:** `src/diagramas/` y `src/evidencias_ia/` tienen archivos de ejecuciones del 7 de noviembre, anteriores a la clase `Proyecto`, cuando la app se lanzó con la carpeta de trabajo en `src`. Se pueden borrar.
8. **`diagramas/` tiene 225 páginas `grafo_*.html`** generadas por "Diagrama general con clic por país" después de construir todos los países. Están versionadas por diseño y engordan la carpeta.
9. **Rutas relativas en los HTML:** `ver_*.html` y `visor_grafo.html` cargan `../recursos/*.js`, así que dependen de que `diagramas/` y `recursos/` sean hermanas.
10. **Límites de diseño:**
    - `RegistroEventos` guarda como máximo 20.000 eventos y cuenta los que descarta.
    - Los prompts de BFS y DFS se cortan en 250 eventos.
    - El drill-down de países con más de 60 vértices se dibuja como árbol BFS hasta `nivelDiagrama`.
    - El visor se niega a mostrar más de 2.000 nodos.
11. **Desviación de la especificación (decidida):** el grafo incluye como vértices los aeropuertos válidos **sin** IATA que tienen rutas, etiquetados con su ICAO. Sin ellos no cuadran las cifras de referencia (37.110 pares, 550 vértices en Estados Unidos).
12. **Acoplamiento con Swing fuera de `app/`:** `ia/VisorHtml.abrir` usa `java.awt.Desktop`. `AnalizadorIA` y `ProcesoETL` avisan el progreso con `Consumer<String>` y no dependen de Swing, así que ya sirven como backend de un servicio web.

### Notas útiles para la migración a web

- Toda la lógica está detrás de métodos que no dependen de Swing:
  - `ProcesoETL`, `DatosLimpios`, `Ordenamiento`, `Busqueda`, `ArbolBST`, `MapaPaises`, `BFS`, `DFS`, `Rutas`, `Ciclos`;
  - `AnalizadorIA.diagrama` y `texto`, `GeneradorPrompt`, `GeneradorFlujogramaLocal`, `AlmacenApis`.

  La pestaña IA y la de Grafo solo arman parámetros y muestran texto.
- `Sesion` hoy es estado por proceso (un usuario). En web, ese estado tendría que ser por sesión o compartido, decidiendo qué se recalcula: el ETL tarda ~2,3 s y el `MapaPaises` menos de 1 s.
- El progreso ya es un flujo de mensajes (`Consumer<String>`), lo que facilita pasarlo por SSE o WebSocket.
- Los diagramas ya son HTML con Mermaid local; en web podrían servirse directamente desde `diagramas/` y `recursos/`.
- Las claves en `~/.smartetl/apis.properties` y en variables de entorno son locales de cada usuario; en web habría que redefinir dónde viven.
