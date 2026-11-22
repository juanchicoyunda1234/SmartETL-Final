# Guion del "vistazo al código": SmartETL-DS + IA

Para abrir en Visual Studio Code después de las diapositivas. Cada integrante muestra **solo lo fundamental** de su capa. Los números de línea se leyeron de los archivos actuales de `src/` el 15 de noviembre de 2026; si alguien edita un archivo antes de la exposición, hay que volver a revisarlos.

**Atajo útil en VS Code:** `Ctrl+P`, escribir el nombre del archivo, y después `Ctrl+G` y el número de línea para saltar directo.

| # | Integrante | Tema | Archivos | Duración estimada |
|---|---|---|---|---|
| 1 | Joseph | Arquitectura por capas | Árbol de `src/` | 1 min |
| 2 | Jeremy | Modelo y listas | 4 | 1,5 min |
| 3 | Noemi | ETL con cola y pila | 4 | 2 min |
| 4 | Andres | Ordenamiento y búsqueda | 2 | 1,5 min |
| 5 | Joseph | Árbol binario de búsqueda | 1 | 1,5 min |
| 6 | Juan Carlos | Grafo y recorridos | 6 | 2 min |
| 7 | Jullisa | IA | 3 | 1,5 min |
| 8 | Joseph | Web y cierre | 5 | 2 min |

**Nombres que difieren de lo pedido** (se usa el nombre real):

- **Limpieza de `\N`** (Noemi): no hay un método llamado así. La hacen `esNulo` (detecta `\N`) y `limpio` (lo cambia por `"Sin dato"`) en `Transform.java`; el método privado `limpiar` (línea 154) aplica `limpio` a cada campo del aeropuerto.
- **Construcción bajo demanda** (Juan Carlos): no hay un método con ese nombre. Es `MapaPaises.obtener`, que llama al método privado `construir` solo si el país aún no está construido.
- **Estados en `Ciclos`** (Juan Carlos): solo existen las constantes `GRIS` y `NEGRO`, en `Vertice.java`. El "blanco" no es una constante: significa "no visitado en esta corrida" (`fueVisitado` devuelve `false`).

---

## 1. JOSEPH: apertura, arquitectura por capas (1 minuto)

**Archivo:** el panel del explorador de VS Code con `src/` desplegado. No hay fragmento de código.

```
src/
├── model/        Las clases de los datos: Aeropuerto, Ruta, Aerolinea y ErrorETL.
├── estructuras/  Nuestras estructuras: ListaSecuencial, ListaEnlazada, Cola, Pila y ArbolBST.
├── etl/          El proceso ETL: lee los CSV, valida y limpia, y escribe los archivos limpios.
├── algoritmos/   Ordenamiento, búsquedas y los recorridos del grafo (BFS, DFS, rutas, ciclos).
├── grafo/        El grafo de rutas aéreas: un grafo por país.
├── ia/           La capa de IA: arma los prompts, llama a la API y tiene el respaldo local.
├── app/          Main (arranca el programa) y Sesion (lo que se recuerda durante la ejecución).
│   ├── web/      El servidor web local y los endpoints que usa la página.
│   └── swing/    La interfaz de escritorio anterior, que sigue funcionando.
└── pruebas/      Las pruebas de cada hito.
```

**Qué decir:**
> "Aquí podemos ver la arquitectura por capas. Abajo están `model`, con los datos, y `estructuras`, con nuestras listas, cola, pila y árbol, sin usar las colecciones de Java. Encima está `etl`, que usa esas estructuras para limpiar los datos; después `algoritmos` y `grafo`, que trabajan sobre los datos ya limpios, y `ia`, que explica lo que pasó. Arriba de todo está `app`: `Main` arranca el servidor de `app/web`, la página le pide datos y el servidor llama a las capas de abajo en ese orden: `etl`, luego `algoritmos` y `grafo`, y al final `ia`."

**Palabras clave:** capas, estructuras propias, ETL, servidor web, orden de llamadas.

**Si preguntan:** *"¿Una capa de abajo llama a una de arriba?"*
> "Casi nunca. La única excepción es que `estructuras` puede anotar cada operación en `ia.RegistroEventos`; lo hicimos para que la IA dibuje flujogramas con lo que realmente pasó."

**Traspaso:**
> "Empecemos por abajo: Jeremy nos muestra cómo se representa un aeropuerto y cómo se guardan en las listas."

---

## 2. JEREMY: modelo y listas (1,5 minutos)

### 2.1 `src/model/Aeropuerto.java`, líneas 41 a 54

```java
    public static Aeropuerto desdeCampos(String[] c) {
        if (c.length != CAMPOS) {
            throw new IllegalArgumentException("Un aeropuerto requiere " + CAMPOS + " campos y llegaron " + c.length);
        }
        return new Aeropuerto(c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7], c[8], c[9], c[10], c[11], c[12], c[13]);
    }

    public static Aeropuerto conId(String id) {
        return new Aeropuerto(id, "", "", "", "", "", "", "", "", "", "", "", "", "");
    }

    public static Aeropuerto conIata(String iata) {
        return new Aeropuerto("", "", "", "", iata, "", "", "", "", "", "", "", "", "");
    }
```

**Qué decir:**
> "Cada aeropuerto del archivo tiene 14 campos y aquí lo convertimos en un objeto. Si la línea no tiene 14 campos, no se crea. `conId` y `conIata` crean un aeropuerto 'de búsqueda' que solo tiene la clave, para poder buscarlo en las listas o en el árbol."

**Palabras clave:** 14 campos, `desdeCampos`, objeto de búsqueda, `Registro`.

**Si preguntan:** *"¿Por qué todos los campos son `String`?"*
> "Porque así llegan del CSV, incluso con `\N`. Validarlos y convertirlos es trabajo del Transform, no del modelo."

### 2.2 `src/etl/LectorCSV.java`, líneas 15 a 29

```java
            if (enComillas) {
                if (c == '"') {
                    if (i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                        actual.append('"');
                        i++;
                    } else {
                        enComillas = false;
                    }
                } else {
                    actual.append(c);
                }
            } else if (c == '"') {
                enComillas = true;
            } else if (c == ',') {
                campos[indice] = actual.toString();
```

**Qué decir:**
> "No usamos `split(\",\")` porque hay nombres con comas, por ejemplo entre comillas. Recorremos la línea carácter por carácter y una coma solo separa campos cuando estamos fuera de comillas."

**Palabras clave:** carácter por carácter, comillas, separador.

**Si preguntan:** *"¿Qué pasa con unas comillas dobles `\"\"` dentro de un texto?"*
> "Se toman como una comilla literal: es la línea 17, que mira el siguiente carácter."

### 2.3 `src/estructuras/ListaSecuencial.java`, líneas 15 a 23

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

**Qué decir:**
> "La lista secuencial guarda los datos en un arreglo que empieza con 16 espacios. Cuando se llena, creamos uno del doble y copiamos todo. Así insertar al final es casi siempre O(1) y `obtener(i)` es O(1)."

**Palabras clave:** arreglo, duplicar capacidad, O(1) amortizado, acceso por índice.

**Si preguntan:** *"¿Por qué el doble y no uno más?"*
> "Si creciera de uno en uno, copiaríamos todo en cada inserción. Duplicando, las copias son pocas."

### 2.4 `src/estructuras/ListaEnlazada.java`, líneas 32 a 43

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
        registrar("insertar", "Insertar al final " + dato + " (nuevo tail, O(1))", String.valueOf(dato));
    }
```

**Qué decir:**
> "En la lista enlazada cada dato va en un nodo que apunta al siguiente. Como guardamos `tail`, insertar al final no recorre la lista: es O(1). La usamos en el grafo para las adyacencias, que solo crecen y se recorren de inicio a fin."

**Palabras clave:** nodo, `head`, `tail`, O(1), sin redimensionar.

**Si preguntan:** *"¿Entonces por qué no usan siempre la enlazada?"*
> "Porque `obtener(i)` en la enlazada es O(i). Para la búsqueda binaria y la paginación necesitamos acceso directo, y eso lo da la secuencial."

**Traspaso:**
> "Ya tenemos dónde guardar los datos; ahora Noemi nos muestra cómo los limpiamos en el ETL usando una cola y una pila."

---

## 3. NOEMI: ETL con cola y pila (2 minutos)

### 3.1 `src/estructuras/Cola.java`, líneas 18 a 26 (encolar) y 33 a 38 (desencolar)

```java
        Nodo<T> nuevo = new Nodo<>(dato);
        if (fin == null) {
            frente = nuevo;
        } else {
            fin.setSiguiente(nuevo);
        }
        fin = nuevo;
        tamanio++;
        registrar("encolar", "Encolar " + dato, dato);
```
```java
        T dato = frente.getDato();
        frente = frente.getSiguiente();
        if (frente == null) {
            fin = null;
        }
        tamanio--;
```

**Qué decir:**
> "La cola entra por `fin` y sale por `frente`, así que el primero que entra es el primero que sale. En el ETL, Extract encola cada registro leído del CSV y Transform los desencola en el mismo orden del archivo. Ambas operaciones son O(1)."

**Palabras clave:** FIFO, `frente`, `fin`, orden del archivo, O(1).

**Si preguntan:** *"¿Para qué una cola si podían validar mientras leen?"*
> "Para separar las etapas: Extract solo lee y Transform solo valida. Además, la cola conserva el orden, y así el número de línea de cada error es el real."

### 3.2 `src/estructuras/Pila.java`, líneas 16 a 22

```java
    public void apilar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        nuevo.setSiguiente(cima);
        cima = nuevo;
        tamanio++;
        registrar("apilar", "Apilar " + dato, dato);
    }
```

**Qué decir:**
> "La pila agrega siempre arriba, en la `cima`: el último que entra es el primero que sale. Ahí van los registros con error del Transform; con los datos reales quedan 589 errores en la pila."

**Palabras clave:** LIFO, `cima`, pila de errores, 589.

**Si preguntan:** *"¿Por qué una pila y no otra lista para los errores?"*
> "Porque nos interesa ver primero el último error encontrado, que es lo que da LIFO, y apilar es O(1)."

### 3.3 `src/etl/Transform.java`: líneas 134 a 139, 242 a 244, 176 a 179 y 204 a 205

```java
        if (!esEntero(a.getId())) {
            return new ErrorETL(fuente, linea, a.getId(), TipoError.ID_INVALIDO, "El ID no es numérico", a.getId());
        }
        if (esVacio(a.getNombre())) {
            return new ErrorETL(fuente, linea, a.getId(), TipoError.CAMPO_OBLIGATORIO, "Falta el nombre", a.getNombre());
        }
```
```java
    public static String limpio(String valor) {
        return esVacio(valor) ? SIN_DATO : valor.trim();
    }
```
```java
    private String resolverId(String codigo, String id) {
        Aeropuerto encontrado = esNulo(id) ? datos.buscarPorIata(codigo.trim()) : datos.buscarPorId(id);
        return encontrado == null ? null : encontrado.getId();
    }
```
```java
    private void registrarError(int fuente, ErrorETL error) {
        errores.apilar(error);
```

**Qué decir:**
> "`validarAeropuerto` revisa en orden: ID numérico, nombre, país, latitud y longitud. Al primer problema devuelve un `ErrorETL` con su tipo. `limpio` cambia los `\N` y los vacíos por 'Sin dato'. En las rutas, `resolverId` busca el aeropuerto por ID con búsqueda binaria, o por código IATA en el árbol si el ID es `\N`; si no aparece, el error va a la pila con `registrarError`."

**Palabras clave:** `validarAeropuerto`, `\N` → "Sin dato", `resolverId`, `REFERENCIA_ROTA`, `apilar`.

**Si preguntan:** *"¿Qué diferencia hay entre `REFERENCIA_ROTA` e `ID_NULO_NO_RECUPERABLE`?"*
> "`REFERENCIA_ROTA` es un ID que no existe en `airports` (473 rutas). `ID_NULO_NO_RECUPERABLE` es un `\N` cuyo código IATA tampoco está en el árbol (116 rutas); lo decide `errorDeExtremo`, en las líneas 181 a 192."

### 3.4 `src/etl/ProcesoETL.java`, líneas 49 a 52 y 89 a 91

```java
        Cola<Aeropuerto> colaAeropuertos = new Cola<>();
        extraer(Transform.AEROPUERTOS, Aeropuerto.CAMPOS, colaAeropuertos, Aeropuerto::desdeCampos);
        avisar.accept("Transform de aeropuertos: validando " + colaAeropuertos.tamanio() + " registros de la cola");
        transform.transformarAeropuertos(colaAeropuertos);
```
```java
    public String[] cargar(String carpeta) throws IOException {
        avisar.accept("Load: escribiendo los archivos limpios y los reportes en " + carpeta);
        archivosEscritos = Load.escribir(datos, transform, msExtract, transform.getMilisegundos(), Proyecto.ruta(carpeta));
```

**Qué decir:**
> "`ProcesoETL` pone el orden: para cada fuente primero hace el Extract a una cola y luego el Transform de esa cola. Lo hace con aeropuertos, después rutas y después aerolíneas. Los aeropuertos van primero porque las rutas necesitan buscarlos. El Load es aparte, en `cargar`, y escribe los archivos limpios en `output/`."

**Palabras clave:** Extract → Transform → Load, aeropuertos primero, `output/`.

**Si preguntan:** *"¿Por qué el Load es un botón aparte?"*
> "Porque escribir archivos es opcional: se puede revisar el resultado del Transform en pantalla antes de guardarlo."

**Traspaso:**
> "Con los datos ya limpios en listas, Andrés nos muestra cómo los ordenamos y cómo buscamos en ellos."

---

## 4. ANDRES: ordenamiento y búsqueda (1,5 minutos)

### 4.1 `src/algoritmos/Ordenamiento.java`: líneas 20 a 23 y 31 a 33 (burbuja), 46 a 50 (inserción)

```java
            boolean huboIntercambio = false;
            for (int j = 0; j < n - 1 - i; j++) {
                comparaciones++;
                if (comparador.compare(lista.obtener(j), lista.obtener(j + 1)) > 0) {
```
```java
            if (!huboIntercambio) {
                break;
            }
```
```java
            while (j >= 0) {
                comparaciones++;
                if (comparador.compare(lista.obtener(j), actual) <= 0) {
                    break;
                }
```

**Qué decir:**
> "En burbuja contamos una comparación cada vez que comparamos dos vecinos, en la línea 22. El corte temprano está en las líneas 31 a 33: si en una pasada no hubo ningún intercambio, la lista ya está ordenada y salimos. En inserción contamos en la línea 47, y paramos apenas el elemento de la izquierda es menor o igual. Por eso inserción hace un 53,6 % menos de comparaciones que burbuja con los 7.698 aeropuertos por IATA."

**Palabras clave:** `comparaciones++`, corte temprano, `huboIntercambio`, desplazamientos, O(n²).

**Si preguntan:** *"¿Ordenan la lista original?"*
> "No: las dos empiezan con `ListaSecuencial.copiaDe(original)` (líneas 15 y 40), así la lista del ETL no se altera."

### 4.2 `src/algoritmos/Busqueda.java`, líneas 24 a 37

```java
        while (izquierda <= derecha) {
            int medio = (izquierda + derecha) / 2;
            T elemento = ordenada.obtener(medio);
            comparaciones++;
            int comparacion = comparador.compare(clave, elemento);
            if (comparacion == 0) {
                return new ResultadoBusqueda<>(elemento, medio, comparaciones);
            }
            if (comparacion < 0) {
                derecha = medio - 1;
            } else {
                izquierda = medio + 1;
            }
        }
```

**Qué decir:**
> "La búsqueda binaria mira el elemento del medio y descarta la mitad donde no puede estar. Por eso necesita una lista ordenada y una `ListaSecuencial`, que nos da `obtener(medio)` en O(1). Para encontrar UIO hace 13 comparaciones, contra 2.556 de la búsqueda secuencial."

**Palabras clave:** mitad, `medio`, lista ordenada, O(log n), 13 frente a 2.556.

**Si preguntan:** *"¿Por qué no hacen binaria sobre la lista enlazada?"*
> "Porque llegar al medio en una enlazada cuesta recorrer los nodos, O(n), y se pierde toda la ventaja."

**Traspaso:**
> "Otra forma de buscar rápido es el árbol binario de búsqueda; Joseph nos lo muestra."

---

## 5. JOSEPH: árbol binario de búsqueda (1,5 minutos)

### `src/estructuras/ArbolBST.java`: líneas 57 a 60 (insertar), 88 a 95 (buscar) y 173 a 175 (altura)

```java
            int comparacion = comparador.compare(dato, actual.getDato());
            if (comparacion == 0) {
                registrar("insertar", "Insertar " + etiqueta(dato) + ": ya existe, no se inserta", dato, caminoHacia(dato));
                return false;
```
```java
        while (actual != null) {
            comparaciones++;
            int comparacion = comparador.compare(clave, actual.getDato());
            if (comparacion == 0) {
                registrarBusqueda(clave, true, comparaciones);
                return new ResultadoBusqueda<>(actual.getDato(), profundidad, comparaciones);
            }
            actual = comparacion < 0 ? actual.getIzquierdo() : actual.getDerecho();
```
```java
        while (!cola.estaVacia()) {
            int enNivel = cola.tamanio();
            altura++;
```

**Qué decir:**
> "`insertar` no usa recursión: baja con un `while (true)` (línea 56), a la izquierda si es menor y a la derecha si es mayor, hasta encontrar un hueco. Si la clave ya existe, devuelve `false` y no la inserta. Eso lo aprovechamos en el ETL para detectar IDs duplicados. `buscar` hace el mismo camino contando comparaciones, y `altura` recorre el árbol por niveles con nuestra cola: cada vuelta del `while` es un nivel."

**Palabras clave:** iterativo, `return false` = duplicado, izquierda menor / derecha mayor, altura por niveles, O(h).

**Si preguntan:** *"¿Por qué iterativo y no recursivo?"*
> "Porque si las claves llegan ordenadas el árbol se vuelve una línea: con 1.500 IDs en orden la altura es 1.499. Con recursión serían miles de llamadas y podría desbordarse la pila de llamadas. El árbol por IATA, en cambio, tiene 6.071 nodos y altura 177."

**Traspaso:**
> "El árbol también lo usamos para encontrar países, y con eso Juan Carlos arma el grafo de rutas."

---

## 6. JUAN CARLOS: grafo y recorridos (2 minutos)

### 6.1 `src/grafo/Vertice.java`: líneas 14 a 16 y 36 a 41

```java
    private final ListaEnlazada<Vertice<T>> adyacentes = new ListaEnlazada<>();
    private final ListaEnlazada<T> externos = new ListaEnlazada<>();
    private final ListaEnlazada<T> llegadasExternas = new ListaEnlazada<>();
```
```java
    public void visitar(int corrida, int nivel, Vertice<T> padre) {
        this.marca = corrida;
        this.nivel = nivel;
        this.padre = padre;
        this.estado = GRIS;
    }
```

**Qué decir:**
> "Cada vértice es un aeropuerto con tres listas enlazadas: sus vecinos dentro del país, los aeropuertos de otros países a los que vuela y los que llegan desde afuera. `visitar` guarda en el vértice el número de recorrido, el nivel y de quién viene, el padre, que después sirve para reconstruir caminos."

**Palabras clave:** lista de adyacencia, internos y externos, `padre`, `marca` de corrida.

**Si preguntan:** *"¿Por qué no un conjunto de visitados?"*
> "Cada recorrido tiene un número nuevo (`nuevaCorrida`), y 'visitado' significa que la marca del vértice es igual a ese número. Así no hay que limpiar nada entre recorridos."

### 6.2 `src/grafo/MapaPaises.java`: líneas 41 a 47 y 73 a 74

```java
    public GrafoPais obtener(String pais) {
        GrafoPais grafoPais = arbol.buscar(GrafoPais.clave(pais)).getElemento();
        if (grafoPais != null && !grafoPais.estaConstruido()) {
            construir(grafoPais);
        }
        return grafoPais;
    }
```
```java
            if (origenDentro && destinoDentro) {
                grafoPais.getGrafo().agregarArista(asegurarVertice(grafoPais, origen), asegurarVertice(grafoPais, destino));
```

**Qué decir:**
> "Los países están en un árbol binario de búsqueda. La construcción es bajo demanda: `obtener` busca el país en el árbol y solo arma su grafo la primera vez que se pide. Si origen y destino están en el país, es una arista interna; si no, va a las listas de externos."

**Palabras clave:** bajo demanda, BST de países, arista interna, `obtener`.

**Si preguntan:** *"¿Cuánto tarda construir todos?"*
> "Los 225 países con aeropuertos se construyen en menos de un segundo (3.264 vértices y 17.587 aristas)."

### 6.3 `src/algoritmos/BFS.java`: líneas 34 a 37 y 44 a 47

```java
        while (!cola.estaVacia()) {
            int enCapa = cola.tamanio();
            for (int k = 0; k < enCapa; k++) {
                Vertice<Aeropuerto> actual = cola.desencolar();
```
```java
                        if (!vecino.fueVisitado(corrida)) {
                            vecino.visitar(corrida, nivel + 1, actual);
                            visitados.insertarFinal(vecino);
                            cola.encolar(vecino);
```

**Qué decir:**
> "BFS usa nuestra cola: desencola un aeropuerto y encola a sus vecinos no visitados. El `for` de la línea 36 procesa una capa completa, así sabemos en qué nivel está cada aeropuerto. Desde UIO cruzando fronteras se visitan 3.217 aeropuertos en 8 niveles."

**Palabras clave:** cola, capas, nivel, se marca al encolar.

**Si preguntan:** *"¿Por qué se marca como visitado al encolar y no al desencolar?"*
> "Para que un aeropuerto no entre dos veces a la cola si dos vecinos lo descubren en la misma capa."

### 6.4 `src/algoritmos/DFS.java`: líneas 34 a 38 y 44 a 49

```java
            Vertice<Aeropuerto> actual = pila.desapilar();
            if (actual.fueVisitado(corrida)) {
                continue;
            }
            actual.visitar(corrida, actual.getNivel(), actual.getPadre());
```
```java
                for (int i = vecinos.tamanio() - 1; i >= 0; i--) {
                    Vertice<Aeropuerto> vecino = vecinos.obtener(i);
                    if (!vecino.fueVisitado(corrida)) {
                        vecino.setNivel(actual.getNivel() + 1);
                        vecino.setPadre(actual);
                        pila.apilar(vecino);
```

**Orden en que DFS visita los vecinos (verificado en el código):**

- `Vecinos.de` (`src/algoritmos/Vecinos.java`, líneas 14 a 28) devuelve los vecinos en el orden de la lista de adyacencia. Ese orden es el de las rutas limpias, es decir, el orden en que aparecen en `routes.csv`. Si se cruzan fronteras, los externos van al final.
- El `for` de la línea 44 los apila **del último al primero**. Así el **primer vecino queda en la cima** y es el primero que se visita.
- Resultado: DFS profundiza siguiendo siempre el primer vecino de la lista, en el mismo orden de la adyacencia.
- **Ejemplo real:** los primeros vecinos de UIO son `GYE, OCC, CUE, …`. Se apila `…, CUE, OCC, GYE` y GYE queda arriba. El DFS visita `1·UIO`, `2·GYE` y después baja por el primer vecino de GYE (`3·SCY`) antes de volver a OCC.
- Un vértice puede quedar apilado dos veces. Por eso la línea 35 lo salta si ya fue visitado cuando sale de la pila.

**Qué decir:**
> "DFS es igual pero con nuestra pila, por eso va al fondo antes de volver. Apilamos los vecinos al revés, del último al primero, para que el primero quede arriba y se visite primero. Así el orden respeta la lista de adyacencia, igual que en BFS."

**Palabras clave:** pila, profundidad, apilar al revés, primer vecino primero, `continue` si ya fue visitado.

**Si preguntan:** *"¿Por qué apilan al revés?"*
> "Porque la pila saca primero lo último que entró. Si apiláramos en orden, se visitaría primero el último vecino."

### 6.5 `src/algoritmos/Rutas.java`: líneas 29 a 31 y 39 a 42

```java
                if (!vecino.fueVisitado(corrida)) {
                    vecino.visitar(corrida, actual.getNivel() + 1, actual);
                    cola.encolar(vecino);
```
```java
        Pila<Aeropuerto> pila = new Pila<>();
        for (Vertice<Aeropuerto> v = destino; v != null; v = v.getPadre()) {
            pila.apilar(v.getDato());
        }
```

**Qué decir:**
> "La ruta más corta en vuelos es un BFS que guarda el padre de cada aeropuerto. Al llegar al destino subimos de padre en padre hasta el origen y lo apilamos; al desapilar sale en el orden correcto. Por ejemplo, de UIO a CUZ: UIO, LIM, CUZ, 2 vuelos."

**Palabras clave:** BFS con padre, reconstruir, pila invierte el camino.

**Si preguntan:** *"¿Es la única ruta posible?"*
> "Es la de menos vuelos que BFS encuentra primero; puede haber otras con la misma cantidad."

### 6.6 `src/algoritmos/Ciclos.java`, líneas 32 a 42

```java
            if (!vecino.fueVisitado(corrida)) {
                vecino.visitar(corrida, actual.getNivel() + 1, actual);
                ListaSecuencial<Aeropuerto> ciclo = visitar(vecino, corrida);
                if (!ciclo.estaVacia()) {
                    return ciclo;
                }
            } else if (vecino.getEstado() == Vertice.GRIS && vecino != actual && vecino != actual.getPadre()) {
                return construirCiclo(actual, vecino);
            }
        }
        actual.setEstado(Vertice.NEGRO);
```

**Qué decir:**
> "Para los ciclos usamos un DFS con estados. No visitado es como blanco; `GRIS` es 'estoy dentro de este camino'; `NEGRO` es 'ya terminé con él'. Si llegamos a un vecino gris que no es el padre, cerramos un ciclo; por ejemplo, en Ecuador: GYE → UIO → OCC → LTX → GYE."

**Palabras clave:** blanco / gris / negro, ciclo, `vecino != padre`.

**Si preguntan:** *"¿Por qué ignoran al padre?"*
> "Porque ir de A a B y volver a A no es un ciclo real; pedimos longitud 3 o más."

**Traspaso:**
> "Todo lo que hicieron las estructuras queda anotado en eventos; Jullisa nos muestra cómo la IA convierte eso en flujogramas."

---

## 7. JULLISA: IA (1,5 minutos)

### 7.1 `src/ia/GeneradorPrompt.java`, líneas 19 a 26

````java
    private static final String REGLAS_DIAGRAMA = "Reglas obligatorias:\n"
            + "- Responde en español.\n"
            + "- Devuelve SOLO un bloque de código Mermaid (```mermaid ... ```), sin texto antes ni después.\n"
            + "- La primera línea del diagrama debe ser: flowchart TD\n"
            + "- Pon cada etiqueta de nodo entre comillas dobles, por ejemplo A[\"texto\"].\n"
            + "- Para saltos de línea dentro de una etiqueta usa <br/>.\n"
            + "- No inventes datos: usa solo los aeropuertos, números y pasos que aparecen en la entrada.\n"
            + "- Cada nodo debe corresponder a un paso real del registro de eventos.\n";
````

**Qué decir:**
> "Al modelo no le pedimos 'haz un diagrama' sin más: le mandamos estas reglas fijas junto con los eventos reales de la ejecución. La más importante es no inventar datos y que cada nodo sea un paso real."

**Palabras clave:** reglas fijas, eventos reales, Mermaid, no inventar.

**Si preguntan:** *"¿Le mandan datos personales a la IA?"*
> "No: solo eventos del programa con datos públicos de OpenFlights, o conteos agregados de errores."

### 7.2 `src/ia/AnalizadorIA.java`: líneas 149 a 151, 157 a 161 y 61 a 63

```java
        if (resultado.isEmpty() || !(resultado.startsWith("flowchart") || resultado.startsWith("graph"))) {
            return null;
        }
```
```java
        ListaSecuencial<String> faltan = new ListaSecuencial<>();
        for (String esperado : esperados) {
            if (!esperado.isEmpty() && !contienePalabra(normal, esperado) && faltan.contar(esperado::equals) == 0) {
                faltan.insertarFinal(esperado);
            }
```
```java
            } catch (Exception ex) {
                avisar.accept("La API falló (" + ex.getMessage() + "): usando el respaldo local");
                aviso = "La API no respondió bien (" + ex.getMessage() + "). Se usó el respaldo local.";
```

**Qué decir:**
> "No confiamos ciegamente en la respuesta. `limpiarMermaid` quita las marcas de código y rechaza lo que no empiece con `flowchart`. `coherencia` revisa qué pasos del registro faltan y qué aeropuertos sobran. Si la API falla, el `catch` usa el respaldo local: el diagrama que el programa arma solo, que ya estaba listo desde la línea 42 (`mermaid = mermaidLocal`)."

**Palabras clave:** `limpiarMermaid`, coherencia (faltan y sobran), respaldo local, sin internet.

**Si preguntan:** *"¿Y si no hay internet el día de la exposición?"*
> "Funciona igual: el respaldo local genera los mismos flujogramas, solo que aparece el chip 'RESPALDO LOCAL'."

### 7.3 `src/ia/ConfiguracionIA.java`: línea 8 y líneas 74 a 77

```java
    public static final String[] PROVEEDORES = {"Gemini", "Claude", "OpenAI", "GLM", GITHUB};
```
```java
        if (proveedor.equals("Claude")) {
            return new ClienteAnthropic(destino, modelo, texto, ESPERA);
        }
        return new ClienteCompatibleOpenAI(destino, modelo, texto, ESPERA);
```

**Qué decir:**
> "Soportamos cinco proveedores con solo dos clases cliente. Gemini, OpenAI, GLM y GitHub Models hablan el mismo formato, así que usan `ClienteCompatibleOpenAI`; Claude tiene el suyo. La clave vive solo en memoria o en una variable de entorno."

**Palabras clave:** 5 proveedores, 2 clientes, formato compatible, clave en memoria.

**Si preguntan:** *"¿Dónde está la clave? ¿Se ve en la página?"*
> "No. Viaja una sola vez al servidor local y queda en su memoria; ningún endpoint la devuelve."

**Traspaso:**
> "¿Y cómo llega todo esto al navegador? Joseph cierra mostrando la capa web."

---

## 8. JOSEPH: web y cierre (2 minutos)

### 8.1 `src/app/Main.java`, líneas 18 a 23

```java
        if (pideSwing(args)) {
            SwingUtilities.invokeLater(Main::iniciarSwing);
            return;
        }
        SwingUtilities.invokeAndWait(Main::prepararCarpeta);
        ServidorWeb servidor = ServidorWeb.iniciar(new Sesion());
```

**Qué decir:**
> "Este es el punto de entrada. Con `--swing` abre la ventana anterior; si no, busca la carpeta del proyecto e inicia el servidor web."

**Palabras clave:** `main`, `--swing`, servidor.

**Si preguntan:** *"¿La versión Swing todavía funciona?"*
> "Sí, está intacta en `app/swing` y usa el mismo backend."

### 8.2 `src/app/web/ServidorWeb.java`, líneas 45 a 50

```java
        ApiIa ia = new ApiIa(contexto);
        servidor.createContext("/api/", new Api(new ApiGeneral(contexto), new ApiDatos(contexto), new ApiEtl(contexto),
                new ApiEstructuras(contexto), new ApiGrafo(contexto), ia, new ApiApis(contexto, ia)));
        servidor.createContext("/", new Estaticos());
        servidor.setExecutor(Executors.newCachedThreadPool());
        servidor.start();
```

**Qué decir:**
> "Usamos el `HttpServer` que ya viene en el JDK, sin librerías. Todo lo que empieza con `/api/` lo atiende `Api`, que tiene un grupo de endpoints por pestaña; lo demás son los archivos de la página."

**Palabras clave:** `HttpServer` del JDK, `/api/`, un grupo por pestaña, `127.0.0.1`.

**Si preguntan:** *"¿Otra computadora puede entrar?"*
> "No: el servidor escucha solo en `127.0.0.1` (línea 26)."

### 8.3 `src/app/web/Api.java`, líneas 21 a 29

```java
            if (!Seguridad.permitir(intercambio)) {
                return;
            }
            Peticion peticion = new Peticion(intercambio);
            for (AreaApi area : areas) {
                if (area.atender(peticion)) {
                    return;
                }
            }
```

**Qué decir:**
> "Primero pasa por `Seguridad`. Después preguntamos a cada grupo si esa ruta es suya y el primero que la reconoce responde; si nadie la reconoce, devolvemos 404."

**Palabras clave:** `Seguridad` primero, recorrer las áreas, 404.

**Si preguntan:** *"¿Qué pasa si hay un error en el backend?"*
> "Los `catch` de las líneas 31 a 39 lo convierten en una respuesta JSON con su código (400, 404, 409 o 500) y la página lo muestra como aviso."

### 8.4 `src/app/web/Seguridad.java`, líneas 19 a 27

```java
        String anfitrion = intercambio.getRequestHeaders().getFirst("Host");
        if (anfitrion == null || !(anfitrion.equals("127.0.0.1:" + puerto) || anfitrion.equals("localhost:" + puerto))) {
            Respuesta.error(intercambio, 403, "Solo se aceptan peticiones dirigidas a 127.0.0.1 o localhost");
            return false;
        }
        String metodo = intercambio.getRequestMethod().toUpperCase();
        boolean modifica = !(metodo.equals("GET") || metodo.equals("HEAD"));
        if (modifica && !"1".equals(intercambio.getRequestHeaders().getFirst("X-SmartETL"))) {
            Respuesta.error(intercambio, 403, "Petición rechazada: falta la cabecera X-SmartETL");
```

**Qué decir:**
> "Dos reglas simples: solo aceptamos peticiones dirigidas a 127.0.0.1 o localhost, y las que modifican algo deben traer la cabecera `X-SmartETL`. Así otra página abierta en el navegador no puede usar nuestro servidor a escondidas."

**Palabras clave:** `Host`, cabecera `X-SmartETL`, 403.

**Si preguntan:** *"¿No basta con que escuche en 127.0.0.1?"*
> "Ayuda, pero una página maliciosa en el mismo navegador podría llamarlo. La cabecera personalizada lo impide."

### 8.5 `src/app/web/ApiEtl.java`: líneas 36 a 41 y 76 a 79 (endpoint completo `POST /api/etl/ejecutar`)

```java
        if (p.es("POST", "etl", "ejecutar")) {
            boolean esReal = !p.json().texto("modo", "real").equals("demo");
            TareaWeb tarea = contexto.getTareas().iniciar(esReal ? "ETL con datos reales" : "ETL de demostración",
                    avisar -> ejecutar(esReal, avisar));
            ApiGeneral.tareaIniciada(p.getIntercambio(), tarea);
            return true;
```
```java
        ProcesoETL etl = esReal ? ProcesoETL.real() : ProcesoETL.demostracion();
        etl.setAvisos(avisar);
        long inicio = System.nanoTime();
        etl.ejecutar();
```

**Qué decir:**
> "Este es un endpoint completo. La página manda `POST /api/etl/ejecutar` (en `web/js/etl.js`, línea 369). Aquí iniciamos el ETL como tarea en otro hilo y respondemos de inmediato con su número. La tarea llama al mismo `ProcesoETL` que mostró Noemi, y sus avisos llegan en vivo a la página, que va pintando el pipeline. Al terminar, el resumen viaja como JSON y se dibujan los KPIs."

**Palabras clave:** endpoint, tarea en otro hilo, avisos en vivo (SSE), mismo backend, JSON.

**Si preguntan:** *"¿Qué pasa si pulso el botón dos veces?"*
> "La segunda vez el gestor de tareas responde 409, 'ya hay una tarea en curso', y la página muestra el aviso; además, los botones se desactivan mientras corre."

**Cierre:**
> "En resumen: la página solo pide y dibuja; todo el trabajo lo hacen nuestras estructuras y algoritmos en Java, sin colecciones ni librerías externas. Gracias; ahora pasamos a la demostración en vivo."
