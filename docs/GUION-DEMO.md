# Guion de la demostración final (lunes 9 de noviembre de 2026)

Duración: 8 a 10 minutos. Todas las cifras se verificaron con los archivos de `data/` y las repiten las pruebas.

## Antes de entrar (10 minutos antes)

1. Abrir el proyecto en IntelliJ con la raíz del proyecto como carpeta de trabajo.
2. Ejecutar `pruebas.PruebasHito2`, `PruebasHito3`, `PruebasHito4` y `PruebasIA`: todas deben terminar con `fallaron: 0`. Dejar una consola abierta por si el docente pide verlas.
3. Ejecutar `app.Main` y maximizar la ventana.
4. Pestaña IA: elegir proveedor, escribir el modelo y comprobar que la clave aparece cargada (o escribirla). Pulsar **Validar API**: debe quedar en verde ("✔ API validada… respondió en N ms"). Si queda en rojo, el texto dice el motivo (sin clave, HTTP 401 clave inválida, modelo inexistente, sin internet). Si no hay internet, cambiar el modo a **Solo local**: todo el guion funciona igual.
5. Borrar los archivos viejos de `output/` para que se vea que Load los crea.

## Pasos

| # | Pestaña y acción | Qué debe verse | Qué decir |
|---|---|---|---|
| 1 | **ETL**: Ejecutar ETL (datos reales) | Cola con 7.698 aeropuertos, 67.663 rutas, 6.162 aerolíneas; pila con 589 errores; contadores | "Extract lee cada CSV y lo encola. Transform desencola registro por registro: si es válido va a la lista, si no, a la pila de errores con su tipo y motivo. De 67.663 rutas, 67.074 son válidas y 589 van a la pila: 473 referencias rotas y 116 IDs nulos que no se pudieron recuperar" |
| 2 | Señalar la cima de la pila | `cima -> rutas, línea 67569, REFERENCIA_ROTA` | "La pila es LIFO: arriba está el último error que se encontró, la línea 67.569" |
| 3 | Ejecutar demostración (archivos sucios) | Los 6 tipos de error en los contadores | "Como el dataset real no trae duplicados ni rangos malos, hicimos un archivo pequeño con un ID repetido, una latitud de 95, un nombre vacío, un ID 'A7', un país nulo y una longitud no numérica" |
| 4 | Ejecutar ETL (datos reales) otra vez y **Cargar** | `output/` con 5 archivos | "Load escribe los tres archivos limpios, el archivo de errores y el reporte con tiempos" (abrir `reporte_etl.txt`) |
| 5 | **Estructuras**: buscar `UIO` | Lista 2.556, binaria 13, BST 13 comparaciones | "En la lista enlazada recorremos 2.556 nodos: O(n). La binaria parte la lista ordenada a la mitad cada vez: 13 comparaciones, O(log n). El BST baja por un camino de 13 nodos. La binaria no se ofrece sobre la lista enlazada porque llegar al medio ya cuesta O(n)" |
| 6 | Ordenar con Burbuja por País y luego con Inserción por País | Comparaciones y milisegundos de cada uno | "Burbuja hace unas 29,6 millones de comparaciones. Inserción hace menos porque solo desplaza mientras el anterior es mayor. Se ordena una copia: la lista original no cambia" |
| 7 | Construir el BST, luego **Altura y nodos** | 6.071 nodos, altura 177, promedio 19,3 | "El árbol por IATA tiene 6.071 nodos y altura 177, lejos del ideal de 12, pero en promedio una búsqueda cuesta 19 comparaciones" |
| 8 | **Desbalance** | Altura 1.499 con 1.500 IDs | "Los IDs vienen ordenados, entonces cada uno se cuelga a la derecha del anterior: el árbol se vuelve una lista. Por eso insertar y buscar son iterativos, no recursivos" |
| 9 | **Grafo**: seleccionar Ecuador, Construir seleccionados | 14 aeropuertos, 32 internas, 26 salen, 27 llegan | "Cada país es un grafo dirigido. El BST de países guarda el grafo de cada país y lo construye la primera vez que se pide" |
| 10 | BFS desde UIO, nivel máximo vacío, nivel del diagrama 1, sin cruzar fronteras | 14 aeropuertos, niveles 0 a 2 | "BFS usa nuestra cola y avanza por capas: nivel 1 son los 9 vuelos directos desde Quito, nivel 2 los 4 que se alcanzan con una escala" |
| 11 | **IA**: Flujograma BFS | Mermaid, origen API o respaldo, coherencia | "Este diagrama no está escrito a mano: sale del registro de eventos del BFS que acabamos de correr. La IA recibe solo esos eventos. Luego el programa verifica que estén todos los aeropuertos del registro y que la IA no haya agregado ninguno" |
| 12 | **Ver diagrama** | HTML con zoom y el código debajo | "Se abre sin internet: Mermaid está en la carpeta recursos. El código Mermaid queda visible para explicarlo" |
| 13 | **Grafo**: seleccionar Perú, Construir; luego **Diagrama general con clic por país** | Página general; clic en Ecuador y luego en Perú | "El diagrama general tiene un nodo por país construido. Al hacer clic se abre el grafo del país, con las rutas externas agrupadas por país destino" |
| 14 | Buscar ruta UIO a CUZ | UIO -> LIM -> CUZ, 2 vuelos | "Es un BFS que guarda el padre de cada aeropuerto; al llegar al destino reconstruimos el camino con una pila" |
| 15 | Detectar ciclo en Ecuador | GYE -> UIO -> OCC -> LTX -> GYE | "DFS con estados blanco, gris y negro: si encontramos un vecino gris, cerramos un ciclo. Ignoramos los de ida y vuelta" |
| 16 | **IA**: Explicar árbol, Analizar errores ETL | Texto con las cifras reales | "La IA explica con los datos reales que le mandamos: altura, camino de UIO, conteos de errores. Nunca le mandamos registros individuales" |
| 17 | Generar documentación | `diagramas/DOCUMENTACION.md` y evidencias | "Se generan los 7 flujogramas mínimos y las explicaciones. Cada llamada deja su evidencia con prompt y respuesta en `evidencias_ia`, sin la clave" |
| 18 | **Grafo**: Abrir visor interactivo (origen UIO), niveles 2, desmarcar países | 548 aeropuertos a nivel 2; cambia al filtrar | "Es el mismo grafo exportado a una página. A nivel 1 son 23 aeropuertos, a nivel 2 son 548. Si pido más de 2.000 nodos el visor me pide reducir" |

## Si algo falla

| Problema | Qué hacer |
|---|---|
| La API no responde o no hay internet | El programa avisa y usa el respaldo local. Decir: "por eso existe el modo con respaldo: el diagrama sale igual del registro de eventos" |
| El navegador no se abre | Abrir a mano `diagramas/ver_flujo_bfs.html` |
| La ventana se ve lenta al construir todos | Es la primera vez que se construyen los 225 grafos; tarda menos de un segundo |
| Piden ver el código de una estructura | Ir directo a `src/estructuras/Cola.java`, `Pila.java`, `ArbolBST.java` o `src/algoritmos/BFS.java` |
