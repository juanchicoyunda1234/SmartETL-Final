# Issues del proyecto SmartETL-DS + IA

Issues listos para copiar y pegar en GitHub a mano (**Issues › New issue**). Cada uno tiene: título, etiquetas, responsable y el cuerpo dentro de un bloque para copiarlo tal cual.

## Antes de crear los issues

1. **Crear las etiquetas** en **Issues › Labels › New label**:

   | Etiqueta | Color sugerido | Para qué |
   |---|---|---|
   | `estructura` | `#1d76db` | Estructuras de datos propias |
   | `datos` | `#0e8a16` | Dataset, CSV y modelo |
   | `etl` | `#fbca04` | Extract, Transform, Load |
   | `bst` | `#5319e7` | Árbol binario de búsqueda y búsquedas |
   | `grafo` | `#006b75` | Grafo, BFS, DFS, rutas y ciclos |
   | `web` | `#c5def5` | Servidor y frontend |
   | `ia` | `#d876e3` | Capa de IA |
   | `docs` | `#0075ca` | Documentación |
   | `seguridad` | `#b60205` | Llaves, `.gitignore`, seguridad |

2. **Crear los 14 issues en este orden, todos antes de abrir el primer Pull Request.** GitHub numera issues y Pull Requests con el mismo contador: si se abre un PR antes, los números se corren y los `Closes #N` dejan de coincidir.
3. Para asignar el responsable, el integrante debe estar invitado como colaborador del repositorio (**Settings › Collaborators**).

## Resumen

| # | Título | Responsable | Etiquetas | Depende de |
|---|---|---|---|---|
| 1 | Crear el esqueleto del repositorio | @juanchicoyunda1234 | `docs`, `seguridad` | — |
| 2 | Seguridad: `.gitignore` completo y configuración del IDE | @juanchicoyunda1234 | `seguridad` | #1 |
| 3 | Listas propias: secuencial y enlazada | @WinoSpop | `estructura`, `datos` | #2 |
| 4 | Modelo, lectura de CSV y dataset OpenFlights | @WinoSpop | `datos` | #3 |
| 5 | Cola de extracción, pila y Extract | @edithtuza15-collab | `etl`, `estructura` | #4 |
| 6 | Árbol binario de búsqueda propio | @llamucaandres161 | `bst`, `estructura` | #5 |
| 7 | Búsqueda secuencial, binaria y por BST, y pruebas del BST | @llamucaandres161 | `bst` | #6 (y #10 para las pruebas) |
| 8 | Ordenamiento burbuja e inserción | @wayusa25-cmyk | `bst`, `estructura` | #7 |
| 9 | Transform, pila de errores y Load | @edithtuza15-collab | `etl` | #8 |
| 10 | Grafo por país, BFS, DFS, ruta y ciclo | @juanchicoyunda1234 | `grafo` | #9 |
| 11 | IA: proveedores y API keys | @jullisaaltamirano2017-boop | `ia`, `seguridad` | #10 |
| 12 | IA: prompt con Mermaid, coherencia y respaldo local | @jullisaaltamirano2017-boop | `ia` | #11 |
| 13 | Capa web: servidor Java y frontend | @wayusa25-cmyk | `web` | #12 |
| 14 | README final, documentación e integración | @juanchicoyunda1234 | `docs` | #13 |

El orden sale de las dependencias reales del código. Se comprobó compilando por etapas: cada issue compila sobre los anteriores, y entre todos cubren los 101 archivos `.java` de `src/`. Hay dos casos especiales:

- La parte de **ETL** se divide en #5 y #9 porque `ArbolBST` usa la `Cola` y la `Pila` (#5), y `Transform` usa el `ArbolBST` para detectar IDs duplicados (#6).
- `ia/Evento.java` e `ia/RegistroEventos.java` van en #3 porque las estructuras registran sus operaciones en esa bitácora desde el principio.

**Cómo compilar en cualquier etapa** (Git Bash, que viene con Git para Windows). Compila solo los archivos que ya existen, así que funciona aunque falten paquetes:

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
```

---

## Issue #1: Crear el esqueleto del repositorio

- **Etiquetas:** `docs`, `seguridad`
- **Responsable:** Juan Carlos Chico (@juanchicoyunda1234)

```markdown
## Descripción
Crear la estructura base del repositorio en `main`: todas las carpetas del proyecto vacías, cada una con un `.gitkeep` para que Git las suba, más `.gitignore`, `.env.example` y un README inicial. Crear después la rama `develop`. Ningún archivo de código en este paso.

## Tareas
- [ ] Crear el árbol de carpetas: `src/model`, `src/estructuras`, `src/etl`, `src/algoritmos`, `src/grafo`, `src/ia`, `src/app`, `src/app/web`, `src/app/swing`, `src/pruebas`, `web/js`, `recursos/iconos`, `recursos/logos`, `recursos/licencias`, `data/demo`, `diagramas`, `evidencias_ia`, `output`, `docs/img`
- [ ] Poner un `.gitkeep` vacío en cada carpeta hoja
- [ ] Agregar `.gitignore` inicial y `.env.example`
- [ ] Agregar un `README.md` inicial (título, integrantes, "en construcción")
- [ ] `git init`, primer commit en `main`, `git remote add origin <URL_DEL_REPOSITORIO>`, push
- [ ] Crear la rama `develop` desde `main` y subirla
- [ ] Invitar a los 5 integrantes como colaboradores

## Archivos involucrados
- `.gitkeep` en cada carpeta del árbol
- `.gitignore`, `.env.example`, `README.md`

## Criterio de aceptación
- En GitHub se ven todas las carpetas del árbol, aunque estén vacías.
- Existen las ramas `main` y `develop`.
- No hay ningún `.java`, `.csv` ni llave en el repositorio.

## Dependencias
Ninguna. Es el primer paso.
```

---

## Issue #2: Seguridad: `.gitignore` completo y configuración del IDE

- **Etiquetas:** `seguridad`
- **Responsable:** Juan Carlos Chico (@juanchicoyunda1234)

```markdown
## Descripción
Completar las reglas de `.gitignore` para que nunca se suban llaves, archivos locales ni compilados, y agregar la configuración compartida de IntelliJ. Se revisó todo el proyecto: no hay API keys en el código; la app las pide en la pestaña IA o las lee de variables de entorno.

## Tareas
- [ ] Ampliar `.gitignore`: `.env`, `.env.*` (excepto `.env.example`), `apis.properties`, `.smartetl/`, `*.keys`, `*.pem`, `out/`, `target/`, `bin/`, `*.class`, `.vscode/`, `.idea/*` (excepto 4 archivos), `output/*` (excepto `.gitkeep`)
- [ ] Agregar `.idea/misc.xml`, `.idea/modules.xml`, `.idea/encodings.xml`, `.idea/vcs.xml` y `SmartETL_DS.iml`
- [ ] Verificar con `git status` que no aparezcan `.env`, `out/` ni `apis.properties`
- [ ] Abrir PR hacia `develop` con `Closes #2`

## Archivos involucrados
- `.gitignore`
- `.idea/misc.xml`, `.idea/modules.xml`, `.idea/encodings.xml`, `.idea/vcs.xml`
- `SmartETL_DS.iml`

## Criterio de aceptación
- Al crear un `.env` de prueba con una llave falsa, `git status` no lo muestra.
- `.env.example` sí se sube y solo tiene los nombres de las variables, vacíos.

## Dependencias
#1
```

---

## Issue #3: Listas propias: secuencial y enlazada

- **Etiquetas:** `estructura`, `datos`
- **Responsable:** Jeremy Torosina (@WinoSpop)

```markdown
## Descripción
Subir las listas propias que usa todo el proyecto: la lista secuencial (arreglo que duplica su capacidad) y la lista enlazada (nodos con head y tail), con su interfaz común. Incluye `Evento` y `RegistroEventos` del paquete `ia`, porque las estructuras anotan ahí cada operación (es la bitácora que después usa la IA).

## Tareas
- [ ] Interfaz `Lista`, `Criterio`, `ResultadoBusqueda` y `Nodo`
- [ ] `ListaSecuencial` (asegurarCapacidad duplica el arreglo)
- [ ] `ListaEnlazada` (insertarFinal en O(1) con tail)
- [ ] `ia/Evento` e `ia/RegistroEventos` (bitácora de operaciones)
- [ ] Borrar el `.gitkeep` de `src`, `src/estructuras` y `src/ia` en el mismo commit
- [ ] Compilar con `javac -encoding UTF-8 -d out $(find src -name "*.java")`
- [ ] Abrir PR hacia `develop` con `Closes #3`

## Archivos involucrados
- `src/estructuras/Lista.java`, `Criterio.java`, `ResultadoBusqueda.java`, `Nodo.java`
- `src/estructuras/ListaSecuencial.java`, `ListaEnlazada.java`
- `src/ia/Evento.java`, `src/ia/RegistroEventos.java`

## Criterio de aceptación
- Compila sin errores.
- Se puede explicar por qué la secuencial tiene `obtener(i)` en O(1) y la enlazada `insertarFinal` en O(1).

## Dependencias
#2
```

---

## Issue #4: Modelo, lectura de CSV y dataset OpenFlights

- **Etiquetas:** `datos`
- **Responsable:** Jeremy Torosina (@WinoSpop)

```markdown
## Descripción
Subir el modelo de datos (aeropuerto, ruta, aerolínea), el lector de CSV que respeta comillas, las opciones de carga del Hito 1 y el dataset OpenFlights con su fuente.

## Tareas
- [ ] Modelo: `Registro`, `Aeropuerto`, `Ruta`, `Aerolinea`
- [ ] Lectura: `LectorCSV`, `Mapeador`, `FuenteDatos`, `ResultadoExtraccion`
- [ ] Opciones de carga: `ModoCarga`, `OpcionesCarga`
- [ ] `Proyecto` (busca la carpeta que tiene `data/airports.csv`)
- [ ] Dataset: `data/airports.csv`, `data/routes.csv`, `data/airlines.csv`, `data/FUENTE.txt`
- [ ] Borrar el `.gitkeep` de `src/model` y de `data` en el mismo commit
- [ ] Compilar y abrir PR hacia `develop` con `Closes #4`

## Archivos involucrados
- `src/model/Registro.java`, `Aeropuerto.java`, `Ruta.java`, `Aerolinea.java`
- `src/etl/LectorCSV.java`, `Mapeador.java`, `FuenteDatos.java`, `ResultadoExtraccion.java`, `ModoCarga.java`, `OpcionesCarga.java`, `Proyecto.java`
- `data/airports.csv`, `data/routes.csv`, `data/airlines.csv`, `data/FUENTE.txt`

## Criterio de aceptación
- Compila sin errores.
- Los CSV tienen 7.698, 67.663 y 6.162 filas.
- `data/FUENTE.txt` cita OpenFlights como fuente (la licencia ODbL está documentada en el README).

## Dependencias
#3
```

---

## Issue #5: Cola de extracción, pila y Extract

- **Etiquetas:** `etl`, `estructura`
- **Responsable:** Noemi Tuza (@edithtuza15-collab)

```markdown
## Descripción
Subir la cola (FIFO) y la pila (LIFO) propias, y el Extract, que lee cada CSV y encola los registros en el orden del archivo. Incluye las pruebas del Hito 1.

## Tareas
- [ ] `Cola` (encolar y desencolar en O(1) con frente y fin)
- [ ] `Pila` (apilar y desapilar en O(1) con cima)
- [ ] `Extract` (`extraer` a una lista y `extraerEnCola`)
- [ ] `PruebasHito1`
- [ ] Borrar el `.gitkeep` de `src/pruebas` en el mismo commit
- [ ] Compilar y ejecutar `java -cp out pruebas.PruebasHito1`
- [ ] Abrir PR hacia `develop` con `Closes #5`

## Archivos involucrados
- `src/estructuras/Cola.java`, `src/estructuras/Pila.java`
- `src/etl/Extract.java`
- `src/pruebas/PruebasHito1.java`

## Criterio de aceptación
- `PruebasHito1` termina con `Pruebas que pasaron: 53 | fallaron: 0`.

## Dependencias
#4
```

---

## Issue #6: Árbol binario de búsqueda propio

- **Etiquetas:** `bst`, `estructura`
- **Responsable:** Andres Llamuca (@llamucaandres161)

```markdown
## Descripción
Subir el BST genérico con comparador: insertar, buscar y eliminar iterativos (para no desbordar la pila de llamadas con árboles degenerados), altura por niveles con la cola, conteo con la pila y recorridos inorden, preorden y postorden.

## Tareas
- [ ] `NodoArbol`
- [ ] `ArbolBST`: insertar (devuelve false si la clave ya existe), buscar, eliminar (hoja, un hijo, dos hijos), caminoHacia, altura, contarNodos, promedioComparaciones, recorridos
- [ ] Compilar y abrir PR hacia `develop` con `Closes #6`

## Archivos involucrados
- `src/estructuras/NodoArbol.java`
- `src/estructuras/ArbolBST.java`

## Criterio de aceptación
- Compila sin errores.
- Se puede explicar por qué `insertar` devuelve `false` con un duplicado y por qué no se usa recursión para insertar, buscar y eliminar.

## Dependencias
#5 (el BST usa la Cola y la Pila)
```

---

## Issue #7: Búsqueda secuencial, binaria y por BST, y pruebas del BST

- **Etiquetas:** `bst`
- **Responsable:** Andres Llamuca (@llamucaandres161)

```markdown
## Descripción
Subir las tres búsquedas que se comparan en el proyecto (secuencial, binaria y por BST) y los comparadores de aeropuertos por ID, IATA, nombre y país. Las pruebas del BST (`PruebasHito3`) se suben en un segundo PR, cuando el grafo (#10) ya esté en `develop`, porque también prueban el BST de países.

## Tareas
- [ ] `Busqueda` (secuencial, binaria, porArbol)
- [ ] `Comparadores`
- [ ] Borrar el `.gitkeep` de `src/algoritmos` en el mismo commit
- [ ] Compilar y abrir PR hacia `develop` con `Refs #7`
- [ ] Cuando #10 esté integrado: subir `PruebasHito3`, ejecutarlo y abrir un segundo PR con `Closes #7`

## Archivos involucrados
- `src/algoritmos/Busqueda.java`, `src/algoritmos/Comparadores.java`
- `src/pruebas/PruebasHito3.java` (segundo PR)

## Criterio de aceptación
- Compila sin errores.
- `PruebasHito3` termina con `Pruebas que pasaron: 32 | fallaron: 0`.

## Dependencias
#6 para las búsquedas; #10 para `PruebasHito3`
```

---

## Issue #8: Ordenamiento burbuja e inserción

- **Etiquetas:** `bst`, `estructura`
- **Responsable:** Joseph Romo (@wayusa25-cmyk)

```markdown
## Descripción
Subir los ordenamientos burbuja (con corte temprano) e inserción, que trabajan sobre una copia y cuentan comparaciones y movimientos. La inserción deja ordenada la lista de aeropuertos por ID que usa la búsqueda binaria del ETL.

## Tareas
- [ ] `Ordenamiento` (burbuja, insercion, estaOrdenada)
- [ ] `ResultadoOrdenamiento`
- [ ] Compilar y abrir PR hacia `develop` con `Closes #8`

## Archivos involucrados
- `src/algoritmos/Ordenamiento.java`
- `src/algoritmos/ResultadoOrdenamiento.java`

## Criterio de aceptación
- Compila sin errores.
- Se puede señalar en qué línea se cuentan las comparaciones y dónde está el corte temprano de burbuja.

## Dependencias
#7
```

---

## Issue #9: Transform, pila de errores y Load

- **Etiquetas:** `etl`
- **Responsable:** Noemi Tuza (@edithtuza15-collab)

```markdown
## Descripción
Subir el corazón del ETL: Transform desencola, valida y limpia (`\N` y vacíos pasan a "Sin dato"), manda los válidos a las listas y los errores a la pila. Las rutas resuelven su aeropuerto por ID (búsqueda binaria) o, si el ID es `\N`, por IATA (BST). Si no se encuentra: `REFERENCIA_ROTA` (ID que no existe) o `ID_NULO_NO_RECUPERABLE` (`\N` cuyo IATA tampoco está). Load escribe los archivos limpios.

## Tareas
- [ ] `TipoError` y `ErrorETL`
- [ ] `DatosLimpios` (listas de válidos, BST por IATA, lista ordenada por ID)
- [ ] `Transform` (validarAeropuerto, limpio, resolverId, errorDeExtremo, registrarError)
- [ ] `Load` y `ProcesoETL` (Extract, Transform, Load)
- [ ] Datos sucios de demostración en `data/demo/`
- [ ] `PruebasHito2`
- [ ] Compilar y ejecutar `java -cp out pruebas.PruebasHito2`
- [ ] Abrir PR hacia `develop` con `Closes #9`

## Archivos involucrados
- `src/model/TipoError.java`, `src/model/ErrorETL.java`
- `src/etl/DatosLimpios.java`, `Transform.java`, `Load.java`, `ProcesoETL.java`
- `data/demo/airports_sucio.csv`, `data/demo/routes_sucio.csv`, `data/demo/LEEME.txt`
- `src/pruebas/PruebasHito2.java`

## Criterio de aceptación
- `PruebasHito2` termina con `Pruebas que pasaron: 55 | fallaron: 0`.
- El ETL real da 589 errores: 473 `REFERENCIA_ROTA` y 116 `ID_NULO_NO_RECUPERABLE`.

## Dependencias
#8 (DatosLimpios usa el BST, la búsqueda binaria y la inserción)
```

---

## Issue #10: Grafo por país, BFS, DFS, ruta y ciclo

- **Etiquetas:** `grafo`
- **Responsable:** Juan Carlos Chico (@juanchicoyunda1234)

```markdown
## Descripción
Subir el grafo dirigido por país con lista de adyacencia (construido bajo demanda desde el BST de países), BFS por niveles con la cola, DFS con la pila, ruta más corta en vuelos (BFS con padre y pila) y detección de ciclos (DFS con estados gris y negro). Incluye las páginas del diagrama general y el visor por país.

## Tareas
- [ ] `Vertice`, `Grafo`, `Agrupacion`, `GrafoPais`, `MapaPaises`
- [ ] `Vecinos`, `ResultadoRecorrido`, `BFS`, `DFS`, `Rutas`, `Ciclos`
- [ ] `PruebasHito4`
- [ ] Diagramas del grafo: `diagramas/general_paises.md`, `ver_general_paises.html`, `visor_grafo.html` y las 225 páginas `grafo_<Pais>.html`
- [ ] Borrar el `.gitkeep` de `src/grafo` y de `diagramas` en el mismo commit
- [ ] Compilar y ejecutar `java -cp out pruebas.PruebasHito4`
- [ ] Abrir PR hacia `develop` con `Closes #10`

## Archivos involucrados
- `src/grafo/Vertice.java`, `Grafo.java`, `Agrupacion.java`, `GrafoPais.java`, `MapaPaises.java`
- `src/algoritmos/Vecinos.java`, `ResultadoRecorrido.java`, `BFS.java`, `DFS.java`, `Rutas.java`, `Ciclos.java`
- `src/pruebas/PruebasHito4.java`
- `diagramas/general_paises.md`, `diagramas/ver_general_paises.html`, `diagramas/visor_grafo.html`, `diagramas/grafo_*.html`

## Criterio de aceptación
- `PruebasHito4` termina con `Pruebas que pasaron: 20 | fallaron: 0`.
- 225 países con aeropuertos; UIO → CUZ en 2 vuelos.

## Dependencias
#9 (MapaPaises arma los grafos desde los datos limpios)
```

---

## Issue #11: IA: proveedores y API keys

- **Etiquetas:** `ia`, `seguridad`
- **Responsable:** Jullisa Altamirano (@jullisaaltamirano2017-boop)

```markdown
## Descripción
Subir los clientes HTTP de IA y la configuración: Gemini, OpenAI, GLM y GitHub Models usan el formato compatible con OpenAI; Claude tiene su propio cliente. La llave vive solo en memoria, se lee de variables de entorno o se guarda con nombre en `~/.smartetl/apis.properties`, fuera del repositorio.

## Tareas
- [ ] `Json` (escapar y leer JSON a mano)
- [ ] `ProveedorIA`, `ClienteCompatibleOpenAI`, `ClienteAnthropic`
- [ ] `ConfiguracionIA` (URL y variable de entorno por proveedor)
- [ ] `ApiGuardada`, `AlmacenApis`
- [ ] Confirmar que ninguna llave real queda en el código ni en los commits
- [ ] Compilar y abrir PR hacia `develop` con `Closes #11`

## Archivos involucrados
- `src/ia/Json.java`, `ProveedorIA.java`, `ClienteCompatibleOpenAI.java`, `ClienteAnthropic.java`
- `src/ia/ConfiguracionIA.java`, `ApiGuardada.java`, `AlmacenApis.java`

## Criterio de aceptación
- Compila sin errores.
- Una búsqueda de `AIza`, `sk-` y `sk-ant-` en el repositorio no encuentra nada.

## Dependencias
#10
```

---

## Issue #12: IA: prompt con Mermaid, coherencia y respaldo local

- **Etiquetas:** `ia`
- **Responsable:** Jullisa Altamirano (@jullisaaltamirano2017-boop)

```markdown
## Descripción
Subir la parte que arma los prompts con reglas fijas y eventos reales, limpia la respuesta Mermaid, comprueba la coherencia (faltan y sobran) y, si la API falla, usa el respaldo local. Incluye las trazas de las estructuras, los visores HTML, los flujogramas generados y las evidencias.

## Tareas
- [ ] `Mermaid`, `ResultadoIA`, `VisorHtml`, `VisorGrafo`
- [ ] `GeneradorPrompt` (reglas fijas del prompt)
- [ ] `GeneradorFlujogramaLocal` y `TrazasEstructuras` (respaldo local)
- [ ] `AnalizadorIA` (limpiarMermaid, coherencia, respaldo)
- [ ] `PruebasIA`
- [ ] Flujogramas: `diagramas/flujo_*.md`, `diagramas/ver_flujo_*.html`, `diagramas/DOCUMENTACION.md`
- [ ] Evidencias: `evidencias_ia/*.md` (revisar que ninguna tenga una llave)
- [ ] Borrar el `.gitkeep` de `evidencias_ia` en el mismo commit
- [ ] Compilar y ejecutar `java -cp out pruebas.PruebasIA`
- [ ] Abrir PR hacia `develop` con `Closes #12`

## Archivos involucrados
- `src/ia/Mermaid.java`, `ResultadoIA.java`, `VisorHtml.java`, `VisorGrafo.java`
- `src/ia/GeneradorPrompt.java`, `GeneradorFlujogramaLocal.java`, `TrazasEstructuras.java`, `AnalizadorIA.java`
- `src/pruebas/PruebasIA.java`
- `diagramas/flujo_*.md` (7), `diagramas/ver_flujo_*.html` (7), `diagramas/DOCUMENTACION.md`
- `evidencias_ia/*.md`

## Criterio de aceptación
- `PruebasIA` termina con `Pruebas que pasaron: 60 | fallaron: 0`.
- En modo "Solo local" se generan los 7 flujogramas sin internet.

## Dependencias
#11
```

---

## Issue #13: Capa web: servidor Java y frontend

- **Etiquetas:** `web`
- **Responsable:** Joseph Romo (@wayusa25-cmyk)

```markdown
## Descripción
Subir la interfaz: el servidor local con `HttpServer` del JDK (127.0.0.1:7070, seguridad por Host y cabecera `X-SmartETL`, tareas con avisos por SSE), los endpoints de cada pestaña, el lanzador `Main` (web o `--swing`), la sesión, la versión Swing anterior y el frontend en HTML, CSS y JavaScript con sus librerías locales.

## Tareas
- [ ] `src/app/Main.java` y `src/app/Sesion.java`
- [ ] Servidor: `ServidorWeb`, `Api`, `AreaApi`, `Seguridad`, `Estaticos`, `Peticion`, `Respuesta`, `ErrorWeb`
- [ ] JSON y tareas: `ObjetoJson`, `ArregloJson`, `LectorJson`, `GestorTareas`, `TareaWeb`, `TrabajoWeb`, `ContextoWeb`, `ReporteSesion`
- [ ] Endpoints: `ApiGeneral`, `ApiDatos`, `ApiEtl`, `ApiEstructuras`, `ApiGrafo`, `ApiIa`, `ApiApis`, `FuncionesIa`
- [ ] Versión Swing: los 10 archivos de `src/app/swing/`
- [ ] Frontend: `web/index.html`, las 6 hojas de estilo y los 10 archivos de `web/js/`
- [ ] Recursos: librerías de `recursos/`, `recursos/iconos/`, `recursos/logos/` y `recursos/licencias/`
- [ ] Documentos de diseño: `PRODUCT.md`, `DESIGN.md`, `docs/GUIA-INTERFAZ-WEB.md`, `docs/MIGRACION-FASE0.md`
- [ ] Borrar los `.gitkeep` de `src/app`, `src/app/web`, `src/app/swing`, `web`, `web/js`, `recursos`, `recursos/*` y `docs` en los commits donde esas carpetas reciben su primer archivo
- [ ] Compilar, ejecutar `java -cp out app.Main` y probar las 6 pestañas
- [ ] Abrir PR hacia `develop` con `Closes #13`

## Archivos involucrados
- `src/app/Main.java`, `src/app/Sesion.java`
- `src/app/web/*.java` (24 archivos)
- `src/app/swing/*.java` (10 archivos)
- `web/index.html`, `web/*.css`, `web/js/*.js`
- `recursos/*.js`, `recursos/*.mjs`, `recursos/iconos/*.svg`, `recursos/logos/*.png`, `recursos/licencias/*.txt`
- `PRODUCT.md`, `DESIGN.md`, `docs/GUIA-INTERFAZ-WEB.md`, `docs/MIGRACION-FASE0.md`

## Criterio de aceptación
- El proyecto completo compila y `java -cp out app.Main` abre la web en http://127.0.0.1:7070.
- Las 6 pestañas funcionan sin errores en la consola del navegador.
- `java -cp out app.Main --swing` abre la versión de escritorio.

## Dependencias
#12 (la web usa todas las capas anteriores)
```

---

## Issue #14: README final, documentación e integración

- **Etiquetas:** `docs`
- **Responsable:** Juan Carlos Chico (@juanchicoyunda1234)

```markdown
## Descripción
Cerrar el proyecto: README final, guías de la exposición y la defensa, capturas, y la integración de `develop` en `main` con la etiqueta `v1.0`.

## Tareas
- [ ] `README.md` final (reemplazar `<URL_DEL_REPOSITORIO>` y `<URL_DIAPOSITIVAS>`)
- [ ] Guías: `ESTADO_EXPOSICION.md`, `GUIA_CODIGO.md`, `docs/GUIA-DEFENSA.md`, `docs/GUION-DEMO.md`, `docs/RECORRIDO-PASO-A-PASO.md`, `docs/GUIA-EXPOSICION.html`, `docs/ESTADO-ACTUAL.md`
- [ ] `diagramas/Diagrama.md`, `docs/ISSUES.md`, `docs/PLAN_COMMITS.md`
- [ ] Capturas en `docs/img/` según `docs/img/LEEME.md`
- [ ] Compilar todo y ejecutar las 5 pruebas (todas con `fallaron: 0`)
- [ ] PR de `develop` a `main`, merge y etiqueta `v1.0`

## Archivos involucrados
- `README.md`, `ESTADO_EXPOSICION.md`, `GUIA_CODIGO.md`
- `docs/GUIA-DEFENSA.md`, `docs/GUION-DEMO.md`, `docs/RECORRIDO-PASO-A-PASO.md`, `docs/GUIA-EXPOSICION.html`, `docs/ESTADO-ACTUAL.md`
- `docs/ISSUES.md`, `docs/PLAN_COMMITS.md`, `docs/img/LEEME.md`, `docs/img/*.png`
- `diagramas/Diagrama.md`

## Criterio de aceptación
- `main` tiene la versión completa con la etiqueta `v1.0`.
- Las 5 pruebas terminan con `fallaron: 0`.
- Todos los enlaces del README funcionan.

## Dependencias
#13 y el segundo PR de #7
```

---

## Archivos que no se suben

| Ruta | Por qué |
|---|---|
| `out/` | Clases compiladas (se generan con `javac`) |
| `output/*` (menos `.gitkeep`) | Los genera el Load al ejecutar |
| `src/diagramas/`, `src/evidencias_ia/` | Copias generadas por error al ejecutar desde `src/`; ya existen en `diagramas/` y `evidencias_ia/` |
| `~/.smartetl/apis.properties`, `.env` | Llaves locales de cada integrante |

Al usar la aplicación se regeneran archivos en `diagramas/`, `evidencias_ia/` y `output/`. **No los incluyas en tus commits** salvo que tu issue los pida.
