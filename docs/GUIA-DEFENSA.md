# Guía de defensa individual

Para cada estructura y algoritmo: **qué decir** (literal, con las palabras clave en negrita), **un ejemplo trazado a mano con datos reales** del proyecto y **la pregunta más probable** con su respuesta. Todas las cifras salen de ejecutar el programa sobre `data/`.

---

## 1. Lista enlazada (`ListaEnlazada`)

**Qué decir.** "Es una cadena de **nodos**; cada nodo guarda un dato y la referencia al **siguiente**. Guardamos **head** y **tail**, así insertar al final es **O(1)** sin recorrer. Buscar es **O(n)** porque hay que ir nodo por nodo desde head, y por eso devolvemos el número de comparaciones."

**Ejemplo a mano** (traza real de la pestaña IA, Flujograma Lista enlazada):

| Operación | Lista resultante |
|---|---|
| insertarFinal UIO | [UIO] (head = tail = UIO) |
| insertarFinal GYE | [UIO, GYE] (tail = GYE) |
| insertarFinal CUE | [UIO, GYE, CUE] |
| insertarInicio LIM | [LIM, UIO, GYE, CUE] (head = LIM) |
| buscar CUE | compara LIM, UIO, GYE, CUE: **4 comparaciones**, posición 4 |
| buscar BOG | recorre los 4 nodos y no lo encuentra |

Con datos reales: buscar `UIO` en los 7.698 aeropuertos cuesta **2.556 comparaciones** (está en la posición 2.556 del archivo).

**Pregunta probable:** "¿Por qué no hacen búsqueda binaria en la lista enlazada?"
**Respuesta:** "Porque la binaria necesita ir al elemento del medio en O(1). En la lista enlazada `obtener(i)` recorre desde head, que es O(n), así que cada salto costaría O(n) y se pierde la ventaja."

---

## 2. Lista secuencial (`ListaSecuencial`)

**Qué decir.** "Es un **arreglo** que crece: cuando se llena, crea uno del **doble de capacidad** y copia los datos. El acceso por índice es **O(1)**, por eso la usamos para la búsqueda binaria, para ordenar y para las listas de válidos."

**Ejemplo a mano.** Capacidad inicial 16. Al insertar el elemento 17 se crea un arreglo de 32 y se copian 16; con 7.698 aeropuertos la capacidad final es 8.192.

**Pregunta probable:** "¿Qué cuesta insertar al inicio?"
**Respuesta:** "O(n), porque hay que desplazar todos los elementos una posición a la derecha. Al final es O(1) salvo cuando hay que duplicar."

---

## 3. Cola (`Cola`)

**Qué decir.** "Es **FIFO**: el primero que entra es el primero que sale. La implementamos con nodos, un puntero al **frente** y otro al **final**, así encolar y desencolar son **O(1)**. La usamos en el **Extract**, donde se encolan los registros leídos, y en el **BFS**. Si se desencola una cola vacía lanzamos `IllegalStateException`."

**Ejemplo a mano** (traza real):

| Operación | Cola |
|---|---|
| encolar UIO | [UIO] |
| encolar GYE | [UIO, GYE] |
| encolar CUE | [UIO, GYE, CUE] |
| desencolar → UIO | [GYE, CUE] |
| encolar LIM | [GYE, CUE, LIM] |
| desencolar → GYE | [CUE, LIM] |

**Pregunta probable:** "¿Por qué el ETL usa una cola y no una lista?"
**Respuesta:** "Porque los registros se procesan en el mismo orden en que se leyeron, uno por uno, y cada registro procesado sale de la estructura. Eso es exactamente FIFO; además el número de línea del error coincide con el orden de llegada."

---

## 4. Pila (`Pila`)

**Qué decir.** "Es **LIFO**: el último que entra es el primero que sale. Solo tenemos una referencia a la **cima**; apilar pone un nodo nuevo encima y desapilar quita el de arriba, ambos **O(1)**. La usamos para la **pila de errores** del ETL, para el **DFS** iterativo y para **reconstruir caminos** al revés."

**Ejemplo a mano** (traza real):

| Operación | Pila (cima primero) |
|---|---|
| apilar REFERENCIA_ROTA | [REFERENCIA_ROTA] |
| apilar ID_NULO_NO_RECUPERABLE | [ID_NULO_NO_RECUPERABLE, REFERENCIA_ROTA] |
| apilar DUPLICADO | [DUPLICADO, ID_NULO..., REFERENCIA_ROTA] |
| desapilar → DUPLICADO | [ID_NULO_NO_RECUPERABLE, REFERENCIA_ROTA] |

Con datos reales: la cima de la pila de errores es la ruta de la **línea 67.569** (`ZK SVC-PHX`, ID 6135 no existe) y el fondo es la de la **línea 171** (`2O ADQ-AOS`), el primer error encontrado.

**Pregunta probable:** "¿Por qué los errores van a una pila?"
**Respuesta:** "Porque el último error encontrado queda arriba y se revisa primero, y porque apilar es O(1). Además el documento del proyecto pide pila de errores. Para el archivo de errores recorremos la pila desde la cima."

---

## 5. ETL y reglas de Transform

**Qué decir.** "**Extract** solo lee y deja los datos crudos en una cola. **Transform** desencola, valida y limpia: si el registro pasa las reglas va a la lista de válidos; si no, crea un `ErrorETL` con fuente, línea, identificador, **tipo**, motivo y valor original, y lo apila. **Load** escribe los archivos limpios y los reportes. Primero procesamos aeropuertos porque generan los índices que necesitan las rutas."

| Regla | Tipo de error |
|---|---|
| ID no numérico | `ID_INVALIDO` |
| Falta nombre o país | `CAMPO_OBLIGATORIO` |
| Latitud fuera de -90..90 o longitud fuera de -180..180 | `RANGO_INVALIDO` |
| ID repetido (lo detecta el BST de IDs: `insertar` devuelve falso) | `DUPLICADO` |
| Ruta con ID de aeropuerto que no existe | `REFERENCIA_ROTA` |
| Ruta con ID `\N` cuyo código no está en el BST por IATA | `ID_NULO_NO_RECUPERABLE` |

**Ejemplo a mano.** Ruta `2O,…,ADQ,…,AOS,7167`: el origen ADQ se resuelve, pero el ID de destino 7167 no está en la lista de aeropuertos ordenada por ID (la búsqueda binaria devuelve -1) → `REFERENCIA_ROTA`. Una ruta con ID de origen `\N` y código `UIO` se recupera buscando `UIO` en el BST por IATA: se obtiene el ID 2688.

**Pregunta probable:** "¿Qué pasa con un aeropuerto sin código IATA?"
**Respuesta:** "Es válido: el IATA pasa a 'Sin dato'. Son 1.627 (1.626 con `\N` y uno con 'DU9'). No entra al BST por IATA, pero sí al grafo si tiene rutas, porque las rutas se unen por el ID resuelto."

---

## 6. Ordenamiento burbuja

**Qué decir.** "Recorre la lista comparando **pares vecinos** y los **intercambia** si están en desorden; tras cada pasada el mayor queda al final. Si en una pasada no hubo intercambios, la lista ya está ordenada y paramos. Es **O(n²)**. Ordenamos una **copia** para no alterar la lista original."

**Ejemplo a mano** con [5, 1, 4, 2, 8]:

| Pasada | Resultado | Intercambios |
|---|---|---|
| 1 | 5↔1, 5↔4, 5↔2 → [1, 4, 2, 5, 8] | 3 |
| 2 | 4↔2 → [1, 2, 4, 5, 8] | 1 |
| 3 | sin intercambios → termina | 0 |

Total: 4 intercambios. Con los 7.698 aeropuertos por país: **29.607.225 comparaciones** (unos 0,9 s).

**Pregunta probable:** "¿Cuál es el mejor caso?"
**Respuesta:** "Lista ya ordenada: una sola pasada sin intercambios, O(n), gracias a la bandera que corta el ciclo."

---

## 7. Ordenamiento por inserción

**Qué decir.** "Toma cada elemento y lo **inserta** en su lugar dentro de la parte ya ordenada, **desplazando** a la derecha los mayores. Es **O(n²)** en el peor caso pero **O(n)** si la lista ya está casi ordenada. Por eso lo usamos para la lista de aeropuertos por ID, que ya viene ordenada: hace solo 7.697 comparaciones."

**Ejemplo a mano** con [5, 1, 4, 2, 8]:

| Paso | Elemento | Resultado | Desplazamientos |
|---|---|---|---|
| 1 | 1 | [1, 5, 4, 2, 8] | 1 |
| 2 | 4 | [1, 4, 5, 2, 8] | 1 |
| 3 | 2 | [1, 2, 4, 5, 8] | 2 |
| 4 | 8 | sin cambios | 0 |

Total: 4 desplazamientos.

**Pregunta probable:** "¿Por qué inserción es más rápida que burbuja si ambas son O(n²)?"
**Respuesta:** "Porque deja de comparar apenas encuentra un elemento menor o igual, y mueve elementos en vez de intercambiar pares. En promedio hace la mitad de comparaciones."

---

## 8. Búsqueda binaria

**Qué decir.** "Funciona sobre una **lista ordenada con acceso por índice**. Compara con el **elemento del medio**: si es igual terminó; si la clave es menor, descarta la mitad derecha; si es mayor, la izquierda. Cada comparación **parte el problema a la mitad**, por eso es **O(log n)**."

**Ejemplo a mano real** (buscar `UIO` en los 7.698 aeropuertos ordenados por IATA):

| # | izq | der | medio | IATA en el medio | Decisión |
|---|---|---|---|---|---|
| 1 | 0 | 7697 | 3848 | PBG | derecha |
| 2 | 3849 | 7697 | 5773 | Sin dato | derecha |
| 3 | 5774 | 7697 | 6735 | TTB | derecha |
| 4 | 6736 | 7697 | 7216 | XMW | izquierda |
| 5 | 6736 | 7215 | 6975 | VHZ | izquierda |
| 6 | 6736 | 6974 | 6855 | UME | izquierda |
| 7 | 6736 | 6854 | 6795 | UAI | derecha |
| 8 | 6796 | 6854 | 6825 | UIH | derecha |
| 9 | 6826 | 6854 | 6840 | ULA | izquierda |
| 10 | 6826 | 6839 | 6832 | UKA | izquierda |
| 11 | 6826 | 6831 | 6828 | UIN | derecha |
| 12 | 6829 | 6831 | 6830 | UIP | izquierda |
| 13 | 6829 | 6829 | 6829 | UIO | **encontrado** |

**13 comparaciones** frente a 2.556 de la lista. ("Sin dato" es el IATA limpio de los aeropuertos sin código; en orden de texto queda entre los que empiezan con S.)

**Pregunta probable:** "¿Cuántas comparaciones hace como máximo?"
**Respuesta:** "Alrededor de log₂(n) + 1. Con 7.698 elementos, 13 o 14."

---

## 9. Árbol binario de búsqueda (`ArbolBST`)

**Qué decir.** "Cada nodo tiene un hijo **izquierdo** con claves **menores** y uno **derecho** con claves **mayores**. Insertar y buscar bajan desde la raíz comparando; cuestan **O(altura)**. Insertar es **iterativo** y devuelve falso si la clave existe. Eliminar tiene **tres casos**: hoja, un hijo, o dos hijos, donde lo reemplazamos por el **sucesor inorden**."

**Ejemplo a mano** (traza real, insertar MEX, GYE, UIO, BOG, LIM, SCL, CUE):

```
            MEX
          /     \
       GYE       UIO
      /   \      /
    BOG   LIM  SCL
      \
      CUE
```

- Buscar LIM: MEX (izquierda) → GYE (derecha) → LIM: **3 comparaciones**.
- Eliminar GYE (dos hijos): su sucesor inorden es LIM (el menor del subárbol derecho). LIM sube al lugar de GYE.
- Buscar CUE después: MEX → LIM → BOG → CUE: 4 comparaciones.
- Recorridos: inorden BOG, CUE, GYE, LIM, MEX, SCL, UIO (ordenado); preorden MEX, GYE, BOG, CUE, LIM, UIO, SCL; postorden CUE, BOG, LIM, GYE, SCL, UIO, MEX.

**Con datos reales.** BST global por IATA: raíz GKA (el primer aeropuerto del archivo), 6.071 nodos, **altura 177**, promedio **19,3** comparaciones. Camino de UIO: GKA → MAG → POM → WWK → UAK → VEY → URD → UME → ULD → UGO → UIP → UHE → UIO: **13 comparaciones**.

**Pregunta probable:** "¿Por qué la altura es 177 y no 12?"
**Respuesta:** "Porque la forma depende del orden de inserción. Insertamos en el orden del archivo, que no es aleatorio: hay rachas de códigos parecidos. Un árbol balanceado tendría altura 12, pero no hacemos AVL; igual el promedio de 19 comparaciones es mucho mejor que 2.556."

**Segunda pregunta probable:** "¿Qué pasa si se insertan claves ordenadas?"
**Respuesta:** "El árbol se degenera en una lista. Con los primeros 1.500 aeropuertos por ID la altura es 1.499. Por eso insertar, buscar, eliminar y altura son iterativos: con 7.698 IDs la recursión llegaría a 7.697 llamadas y podría desbordar la pila de Java."

---

## 10. Grafo por país (`Vertice`, `Grafo`, `GrafoPais`, `MapaPaises`)

**Qué decir.** "Es un **grafo dirigido**: cada aeropuerto es un **vértice** y cada ruta única origen-destino es una **arista**. Usamos **listas de adyacencia** con nuestra lista enlazada. Hay un grafo por país: las rutas dentro del país son **adyacentes** y las que salen a otro país van en **externos**. Los grafos se guardan en un **BST de países** y se construyen la primera vez que se piden."

**Ejemplo real: Ecuador.** 14 aeropuertos, 32 rutas internas, 26 salen al exterior, 27 llegan. UIO tiene 9 vecinos internos (GYE, OCC, CUE, ESM, ETR, LOH, MEC, TUA, XMS) y 13 rutas externas. Externas agrupadas: Colombia 6, Estados Unidos 5, Perú 2, Panamá 2, España 2, El Salvador 2…

**Pregunta probable:** "¿Cómo encuentran rápido el vértice de un aeropuerto de otro país?"
**Respuesta:** "El ID del aeropuerto es un entero pequeño (máximo 14.110), así que usamos un arreglo indexado por ID: `verticePorId[id]` da el vértice en O(1). Si ese país todavía no se construyó, `obtener(país)` lo construye antes."

**Otra:** "¿Cómo marcan visitados sin un HashSet?"
**Respuesta:** "Cada vértice guarda el número de la corrida en que fue visitado. Cada recorrido pide un número nuevo, entonces 'visitado' es `marca == corrida`, O(1) y sin limpiar nada."

---

## 11. BFS (búsqueda en anchura)

**Qué decir.** "BFS usa nuestra **cola**. Encola el origen; mientras la cola no esté vacía, **desencola** un aeropuerto y **encola sus vecinos no visitados**. Procesamos **por capas**: al inicio de cada nivel tomamos `tamanio()` de la cola, así sabemos cuántos aeropuertos son de ese nivel. El nivel es el número de vuelos desde el origen."

**Ejemplo a mano real** (BFS desde UIO dentro de Ecuador):

| Paso | Desencola | Vecinos nuevos | Cola después |
|---|---|---|---|
| — | encolar UIO | | [UIO] |
| 1 | UIO (nivel 0) | GYE, OCC, CUE, ESM, ETR, LOH, MEC, TUA, XMS | [GYE … XMS] |
| 2 | GYE (nivel 1) | SCY, GPS, LTX | [OCC … XMS, SCY, GPS, LTX] |
| 3 a 9 | OCC, CUE, ESM, ETR, LOH, MEC, TUA | ninguno | [XMS, SCY, GPS, LTX] |
| 10 | XMS | TNW | [SCY, GPS, LTX, TNW] |
| 11 a 14 | SCY, GPS, LTX, TNW (nivel 2) | ninguno | [] |

Resultado: **14 aeropuertos, niveles 0 a 2**. Cruzando fronteras desde UIO: 3.217 aeropuertos en 8 niveles (nivel 1: 22, nivel 2: 525).

**Pregunta probable:** "¿Por qué BFS da la ruta con menos vuelos?"
**Respuesta:** "Porque visita todos los aeropuertos a 1 vuelo antes que los de 2, y así sucesivamente. La primera vez que llega a un aeropuerto es por el camino más corto en número de aristas."

---

## 12. DFS (búsqueda en profundidad)

**Qué decir.** "DFS usa nuestra **pila**, de forma **iterativa**. Apila el origen; mientras la pila no esté vacía, **desapila**: si ya fue visitado lo ignora; si no, lo marca y **apila sus vecinos**. Así se va lo más profundo posible antes de retroceder. Aceptamos una profundidad máxima."

**Ejemplo a mano real** (DFS desde UIO en Ecuador): se desapila UIO y se apilan sus 9 vecinos en orden inverso, para que GYE quede en la cima. Se desapila GYE y se apilan LTX, LOH, GPS, ESM, CUE, SCY; se desapila SCY (profundidad 2), luego CUE, ESM, GPS… Algunos aeropuertos quedan **dos veces en la pila** (CUE y ESM, apilados desde UIO y desde GYE); la segunda vez que salen ya están visitados y se ignoran. Visita los mismos 14 aeropuertos que BFS, en otro orden.

**Pregunta probable:** "¿Por qué no lo hicieron recursivo?"
**Respuesta:** "Para usar la pila propia y ver su estado en cada paso (eso alimenta el flujograma), y porque cruzando fronteras el camino puede ser muy largo y la recursión podría desbordar la pila de Java."

---

## 13. Ruta más corta (`Rutas.buscar`)

**Qué decir.** "Es un BFS que guarda el **padre** de cada aeropuerto descubierto. Cuando desencola el destino, sigue los padres hacia atrás hasta el origen, los mete en una **pila** y al desapilarlos queda el camino en orden."

**Ejemplo real.** UIO → LIM → CUZ: **2 vuelos**. Padre de CUZ = LIM, padre de LIM = UIO; pila [UIO, LIM, CUZ] desde la cima.

**Pregunta probable:** "¿Es la única ruta de 2 vuelos?"
**Respuesta:** "No necesariamente; BFS devuelve la primera que encuentra con la menor cantidad de vuelos."

---

## 14. Detección de ciclos (`Ciclos`)

**Qué decir.** "Es un DFS con **tres estados**: blanco (no visitado), **gris** (en el camino actual) y negro (terminado). Si desde un vértice llegamos a un vecino **gris**, hay un ciclo, y lo reconstruimos siguiendo los padres. Ignoramos los ciclos de longitud 2 (ir y volver A-B-A), porque casi todas las rutas tienen vuelta."

**Ejemplo real.** Ecuador: **GYE → UIO → OCC → LTX → GYE** (longitud 4).

**Pregunta probable:** "¿Por qué gris y no solo visitado?"
**Respuesta:** "Porque un vecino visitado en otra rama no forma ciclo; solo cierra ciclo si todavía está en el camino actual, que es lo que marca el gris."

---

## 15. Capa de IA y flujogramas dinámicos

**Qué decir.** "Mientras el programa trabaja, las estructuras y algoritmos escriben en un **registro de eventos** (que es una lista enlazada de `Evento`). La IA **no sustituye** a las estructuras: solo recibe ese registro y devuelve el **código Mermaid**. Si no hay clave o internet, un **generador local** produce el diagrama con las mismas reglas. Después **verificamos la coherencia**: que estén todos los pasos del registro y que no aparezcan aeropuertos inventados. Cada llamada queda como **evidencia** con su prompt y respuesta."

**Ejemplo.** El BFS de Ecuador genera 20 eventos (inicio, encolar, 14 desencolar, 3 fin de nivel, fin). Con nivel de diagrama 1, el flujograma detalla los pasos 1 a 10 y resume el nivel 2 como "Nivel 2: 4 aeropuertos nuevos en 1 país".

**Pregunta probable:** "¿Cómo sabe que la IA no inventó nada?"
**Respuesta:** "El programa compara: los aeropuertos esperados salen del registro de eventos, y cualquier código de tres letras que aparezca en el diagrama y no esté en el registro se reporta como 'sobra'. Además, el código Mermaid siempre está visible para revisarlo."

**Otra:** "¿Qué datos se envían a la IA?"
**Respuesta:** "Solo datos públicos de OpenFlights y conteos agregados. La clave nunca se guarda: está solo en memoria y no aparece en las evidencias."

---

## 16. Complejidades (resumen para memorizar)

| Operación | Complejidad |
|---|---|
| Lista enlazada: insertar al final / buscar | O(1) / O(n) |
| Lista secuencial: obtener(i) / insertar al inicio | O(1) / O(n) |
| Cola: encolar, desencolar / Pila: apilar, desapilar | O(1) |
| Burbuja / Inserción | O(n²), mejor caso O(n) |
| Búsqueda secuencial / binaria | O(n) / O(log n) |
| BST: insertar, buscar, eliminar | O(h): O(log n) balanceado, O(n) degenerado |
| BFS / DFS | O(V + E) |
