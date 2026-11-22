# Plan de ramas y commits: SmartETL-DS + IA

Plan para subir el proyecto a un repositorio nuevo, desde cero, con commits de cada integrante desde su propia cuenta. Los comandos son para **Git Bash** (viene con Git para Windows).

Todos los comandos de este documento se probaron en una **simulación completa**: un repositorio temporal con un remoto local en lugar de GitHub. En ella se creó el esqueleto, se hicieron todos los commits con los archivos reales, se integró cada Pull Request en `develop` y se compiló después de cada commit. Al final se unió `develop` en `main` con la etiqueta `v1.0`, y `main` quedó con los mismos archivos que el proyecto.

Marcadores que hay que reemplazar: `<URL_DEL_REPOSITORIO>` (URL `https://github.com/...git`), `<NOMBRE_DEL_REPOSITORIO>` (nombre de la carpeta que crea `git clone`), `<tu-usuario>` y `<correo-de-tu-cuenta-de-GitHub>`.

## Contenido

1. [Reglas del equipo](#reglas-del-equipo)
2. [Orden de integración](#orden-de-integración)
3. [Paso 0: esqueleto del repositorio (líder)](#paso-0-esqueleto-del-repositorio-líder)
4. [Cómo integra el líder cada Pull Request](#cómo-integra-el-líder-cada-pull-request)
5. Una sección por integrante, lista para enviar por WhatsApp
6. [Si algo sale mal](#si-algo-sale-mal)

## Reglas del equipo

- **Ramas:** `main` solo tiene versiones estables. `develop` es la rama de integración. Cada integrante trabaja en su rama `feature/...`, y el líder además usa `chore/seguridad` y `docs/readme`.
- **Nadie hace commit directo en `main` ni en `develop`:** todo entra por Pull Request hacia `develop`, y solo el líder hace el merge.
- **Commits:** [Conventional Commits](https://www.conventionalcommits.org/es/v1.0.0/) en español: `tipo(ámbito): descripción en minúsculas`. Tipos: `feat`, `fix`, `docs`, `test`, `chore`, `refactor`.
- **Regla del `.gitkeep`:** el esqueleto tiene un `.gitkeep` en cada carpeta para que Git suba las carpetas vacías. **Cuando subes el primer archivo real a una carpeta, borras el `.gitkeep` de esa carpeta (y de las carpetas padre que dejan de estar vacías) en el mismo commit**, con `git rm`. Los comandos de cada commit ya lo incluyen. El único `.gitkeep` que se queda para siempre es `output/.gitkeep`.
- **Los archivos salen del proyecto final:** el líder comparte `SmartETL_DS-final.zip`. Cada integrante copia de ahí solo los archivos de su parte. Cada commit sale con el nombre y la cuenta de quien lo hace.
- **Nunca subir:** `out/`, `.env`, `apis.properties` ni archivos de otro integrante. Al usar la aplicación se regeneran archivos en `diagramas/`, `evidencias_ia/` y `output/`: no los agregues salvo que tu commit los pida.
- **Antes de cada PR:** compilar con `javac -encoding UTF-8 -d out $(find src -name "*.java")`. Este comando compila solo lo que ya existe, así que funciona aunque falten partes del proyecto.

## Orden de integración

El orden sale de las dependencias reales del código: cada PR compila sobre los anteriores. Por eso:

- El ETL va en dos partes: `ArbolBST` usa la `Cola` y la `Pila`, y `Transform` usa el `ArbolBST`.
- `PruebasHito3` va después del grafo, porque prueba también el BST de países.


| Orden | Rama | Responsable | Contenido | Cierra |
|---|---|---|---|---|
| 0 | `main` (directo) | Juan Carlos | Esqueleto: carpetas con `.gitkeep`, `.gitignore`, `.env.example`, README inicial; crea `develop` | #1 |
| 1 | `chore/seguridad` | Juan Carlos Chico | Seguridad: .gitignore completo y configuración del IDE | #2 |
| 2 | `feature/datos-jeremy` | Jeremy Torosina | Listas propias, modelo y dataset OpenFlights | #3, #4 |
| 3 | `feature/etl-noemi` | Noemi Tuza | Cola de extracción, pila y Extract | #5 |
| 4 | `feature/bst-andres` | Andres Llamuca | Árbol binario de búsqueda y búsquedas | #6, parte de #7 |
| 5 | `feature/web-joseph` | Joseph Romo | Ordenamiento burbuja e inserción | #8 |
| 6 | `feature/etl-noemi` | Noemi Tuza | Transform, pila de errores y Load | #9 |
| 7 | `feature/grafo-juan` | Juan Carlos Chico | Grafo por país, BFS, DFS, ruta y ciclo | #10 |
| 8 | `feature/bst-andres` | Andres Llamuca | Pruebas del BST | #7 |
| 9 | `feature/ia-jullisa` | Jullisa Altamirano | Capa de IA: proveedores, API keys, prompt con Mermaid y respaldo | #11, #12 |
| 10 | `feature/web-joseph` | Joseph Romo | Capa web: servidor Java y frontend | #13 |
| 11 | `docs/readme` | Juan Carlos Chico | README final y documentación | #14 |
| 12 | `develop` → `main` | Juan Carlos | Versión estable con etiqueta `v1.0` | — |


Resumido: esqueleto → seguridad → listas, modelo y datos → ETL (cola, pila, Extract) → BST y búsquedas → ordenamiento → ETL (Transform y Load) → grafo → pruebas del BST → IA → web → README final → `main` con `v1.0`.

## Paso 0: esqueleto del repositorio (líder)

**Antes de escribir cualquier código.** Juan Carlos crea en GitHub un repositorio **vacío** (sin README, sin `.gitignore` y sin licencia) e invita a los 5 integrantes como colaboradores (**Settings › Collaborators**). Después crea los 14 issues de `docs/ISSUES.md`, en orden.

Árbol exacto del primer commit (solo carpetas con `.gitkeep` y estos 3 archivos):

```text
SmartETL-DS/
├── src/
│   ├── model/
│   │   └── .gitkeep
│   ├── estructuras/
│   │   └── .gitkeep
│   ├── etl/
│   │   └── .gitkeep
│   ├── algoritmos/
│   │   └── .gitkeep
│   ├── grafo/
│   │   └── .gitkeep
│   ├── ia/
│   │   └── .gitkeep
│   ├── app/
│   │   ├── web/
│   │   │   └── .gitkeep
│   │   ├── swing/
│   │   │   └── .gitkeep
│   │   └── .gitkeep
│   ├── pruebas/
│   │   └── .gitkeep
│   └── .gitkeep
├── web/
│   ├── js/
│   │   └── .gitkeep
│   └── .gitkeep
├── recursos/
│   ├── iconos/
│   │   └── .gitkeep
│   ├── logos/
│   │   └── .gitkeep
│   ├── licencias/
│   │   └── .gitkeep
│   └── .gitkeep
├── data/
│   ├── demo/
│   │   └── .gitkeep
│   └── .gitkeep
├── diagramas/
│   └── .gitkeep
├── evidencias_ia/
│   └── .gitkeep
├── output/
│   └── .gitkeep
├── docs/
│   ├── img/
│   │   └── .gitkeep
│   └── .gitkeep
├── .gitignore
├── .env.example
└── README.md
```

Comandos (en Git Bash, dentro de la carpeta donde se va a crear el repositorio; `ORIGEN` es la carpeta del proyecto final):


```bash
ORIGEN="/c/Users/<tu-usuario>/<ruta-del-proyecto>/SmartETL_DS-main"
mkdir SmartETL-DS
cd SmartETL-DS
git init
mkdir -p src src/model src/estructuras src/etl src/algoritmos src/grafo src/ia src/app src/app/web src/app/swing src/pruebas web web/js recursos recursos/iconos recursos/logos recursos/licencias data data/demo diagramas evidencias_ia output docs docs/img
for d in src src/model src/estructuras src/etl src/algoritmos src/grafo src/ia src/app src/app/web src/app/swing src/pruebas web web/js recursos recursos/iconos recursos/logos recursos/licencias data data/demo diagramas evidencias_ia output docs docs/img; do touch "$d/.gitkeep"; done
cat > .gitignore <<'FIN'
out/
*.class
.env
.env.*
!.env.example
apis.properties
output/*
!output/.gitkeep
FIN
cat > README.md <<'FIN'
# SmartETL-DS + IA

Proyecto final de Estructura de Datos (UTA, FISEI, Software, Nivel III, paralelo B).

En construcción: el código se integra por ramas y Pull Requests hacia `develop`.
FIN
cp "$ORIGEN/.env.example" .
git add .
git commit -m "chore: crea el esqueleto del repositorio"
git branch -M main
git remote add origin <URL_DEL_REPOSITORIO>
git push -u origin main
git checkout -b develop
git push -u origin develop
```


Qué contiene cada archivo:

- `.gitignore` inicial: lo mínimo para no subir llaves ni compilados desde el primer commit. Se completa en el PR de seguridad.
- `.env.example`: los 5 nombres de variables de IA, vacíos.
- `README.md`: título y "en construcción". Se reemplaza por el completo en el último PR.

Después del paso 0, en GitHub deben verse las ramas `main` y `develop`, y todas las carpetas vacías.

**Antes de enviar los mensajes:** crea `SmartETL_DS-final.zip` con la carpeta del proyecto **sin** `out/` (por ejemplo, copia `SmartETL_DS-main`, borra `out/` de la copia y comprímela). La carpeta no tiene llaves (punto 1 de seguridad). Compártelo por el grupo.

## Cómo integra el líder cada Pull Request

1. Abre el PR en GitHub y revisa en **Files changed** que solo estén los archivos de esa parte, sin `out/`, `.env` ni llaves.
2. Comprueba que compila. En una copia local:
   ```bash
   git fetch origin
   ```
   ```bash
   git checkout <rama-del-pr>
   ```
   ```bash
   git pull origin <rama-del-pr>
   ```
   ```bash
   javac -encoding UTF-8 -d out $(find src -name "*.java")
   ```
3. Usa **Merge pull request › Create a merge commit**. No uses "Squash": juntaría los commits y se perdería quién hizo cada uno.
4. Avisa por el grupo: "integrado el PR N, sigue X".

**Al final**, cuando estén integrados los 11 PR:

```bash
git checkout develop
```
```bash
git pull origin develop
```
```bash
git checkout main
```
```bash
git pull origin main
```
```bash
git merge --no-ff develop -m "Merge develop: versión 1.0"
```
```bash
git tag -a v1.0 -m "SmartETL-DS + IA v1.0"
```
```bash
git push origin main
```
```bash
git push origin v1.0
```

También se puede hacer con un Pull Request de `develop` a `main` en GitHub, y después crear la etiqueta en **Releases › Draft a new release › Tag: v1.0**.

Opcional: en **Settings › Branches** agrega una regla para `main` y `develop` que exija Pull Request antes de hacer merge.


---

## Juan Carlos Chico (@juanchicoyunda1234)

> **Mensaje para enviar por WhatsApp.** Copia desde aquí hasta el final de esta sección.

Hola Juan 👋 Este es tu plan para subir tu parte de SmartETL-DS desde tu cuenta de GitHub (@juanchicoyunda1234). Tienes 3 Pull Requests en la rama `chore/seguridad` y `feature/grafo-juan` y `docs/readme`. Sigue los pasos en orden y **espera tu turno**: cada PR empieza cuando el anterior ya está integrado.

### PR 1 de Juan: Seguridad: .gitignore completo y configuración del IDE

**Rama:** `chore/seguridad` · **Empieza cuando** el líder haya integrado el esqueleto (#1) y la rama develop en `develop`.

**1. Ponerse al día y crear tu rama:**

```bash
git checkout develop
git pull origin develop
git checkout -b chore/seguridad
```

**2. Commits, uno por bloque, en este orden** (con `ORIGEN` ya definido):

**Commit 1:** `chore(seguridad): amplía .gitignore para llaves y archivos locales`  
Archivos: `.gitignore`

```bash
cp "$ORIGEN/.gitignore" .
git add .gitignore
git commit -m "chore(seguridad): amplía .gitignore para llaves y archivos locales"
```

**Commit 2:** `chore(ide): agrega la configuración compartida de IntelliJ`  
Archivos: `.idea/misc.xml`, `.idea/modules.xml`, `.idea/encodings.xml`, `.idea/vcs.xml`, `SmartETL_DS.iml`

```bash
mkdir -p .idea
cp "$ORIGEN/.idea/misc.xml" .idea/
cp "$ORIGEN/.idea/modules.xml" .idea/
cp "$ORIGEN/.idea/encodings.xml" .idea/
cp "$ORIGEN/.idea/vcs.xml" .idea/
cp "$ORIGEN/SmartETL_DS.iml" .
git add .idea/misc.xml .idea/modules.xml .idea/encodings.xml .idea/vcs.xml SmartETL_DS.iml
git commit -m "chore(ide): agrega la configuración compartida de IntelliJ"
```

**3. Verificar** que no se cuelan archivos locales (todavía no hay código para compilar):

```bash
echo "GEMINI_API_KEY=prueba" > .env
git status --short
rm .env
```

`git status` no debe mostrar `.env`: así se comprueba que el `.gitignore` lo ignora.

**4. Subir la rama:**

```bash
git push -u origin chore/seguridad
```

**5. Abrir el Pull Request en GitHub:** entra a <URL_DEL_REPOSITORIO>, pulsa **Compare & pull request** (o **Pull requests › New pull request**), elige **base: `develop`** y **compare: `chore/seguridad`**.

- Título: `Seguridad: .gitignore completo y configuración del IDE`
- Descripción:

```text
Closes #2
```

Avisa por el grupo cuando lo abras.

### PR 2 de Juan: Grafo por país, BFS, DFS, ruta y ciclo

**Rama:** `feature/grafo-juan` · **Empieza cuando** el líder haya integrado el segundo PR de Noemi (#9) en `develop`.

**1. Ponerse al día y crear tu rama:**

```bash
git checkout develop
git pull origin develop
git checkout -b feature/grafo-juan
```

**2. Commits, uno por bloque, en este orden** (con `ORIGEN` ya definido):

**Commit 1:** `feat(grafo): agrega Vertice y Grafo con lista de adyacencia`  
Archivos: `src/grafo/Vertice.java`, `src/grafo/Grafo.java`  
Borra: `src/grafo/.gitkeep`

```bash
cp "$ORIGEN/src/grafo/Vertice.java" src/grafo/
cp "$ORIGEN/src/grafo/Grafo.java" src/grafo/
git rm -q src/grafo/.gitkeep
git add src/grafo/Vertice.java src/grafo/Grafo.java
git commit -m "feat(grafo): agrega Vertice y Grafo con lista de adyacencia"
```

**Commit 2:** `feat(grafo): agrega el mapa de países con construcción bajo demanda`  
Archivos: `src/grafo/Agrupacion.java`, `src/grafo/GrafoPais.java`, `src/grafo/MapaPaises.java`

```bash
cp "$ORIGEN/src/grafo/Agrupacion.java" src/grafo/
cp "$ORIGEN/src/grafo/GrafoPais.java" src/grafo/
cp "$ORIGEN/src/grafo/MapaPaises.java" src/grafo/
git add src/grafo/Agrupacion.java src/grafo/GrafoPais.java src/grafo/MapaPaises.java
git commit -m "feat(grafo): agrega el mapa de países con construcción bajo demanda"
```

**Commit 3:** `feat(grafo): agrega BFS por niveles y DFS`  
Archivos: `src/algoritmos/Vecinos.java`, `src/algoritmos/ResultadoRecorrido.java`, `src/algoritmos/BFS.java`, `src/algoritmos/DFS.java`

```bash
cp "$ORIGEN/src/algoritmos/Vecinos.java" src/algoritmos/
cp "$ORIGEN/src/algoritmos/ResultadoRecorrido.java" src/algoritmos/
cp "$ORIGEN/src/algoritmos/BFS.java" src/algoritmos/
cp "$ORIGEN/src/algoritmos/DFS.java" src/algoritmos/
git add src/algoritmos/Vecinos.java src/algoritmos/ResultadoRecorrido.java src/algoritmos/BFS.java src/algoritmos/DFS.java
git commit -m "feat(grafo): agrega BFS por niveles y DFS"
```

**Commit 4:** `feat(grafo): agrega ruta más corta y detección de ciclos`  
Archivos: `src/algoritmos/Rutas.java`, `src/algoritmos/Ciclos.java`

```bash
cp "$ORIGEN/src/algoritmos/Rutas.java" src/algoritmos/
cp "$ORIGEN/src/algoritmos/Ciclos.java" src/algoritmos/
git add src/algoritmos/Rutas.java src/algoritmos/Ciclos.java
git commit -m "feat(grafo): agrega ruta más corta y detección de ciclos"
```

**Commit 5:** `test(grafo): agrega las pruebas del Hito 4`  
Archivos: `src/pruebas/PruebasHito4.java`

```bash
cp "$ORIGEN/src/pruebas/PruebasHito4.java" src/pruebas/
git add src/pruebas/PruebasHito4.java
git commit -m "test(grafo): agrega las pruebas del Hito 4"
```

**Commit 6:** `docs(grafo): agrega el diagrama general y las páginas por país`  
Archivos: `diagramas/general_paises.md`, `diagramas/ver_general_paises.html`, `diagramas/visor_grafo.html`, `diagramas/grafo_*.html`  
Borra: `diagramas/.gitkeep`

```bash
cp "$ORIGEN/diagramas/general_paises.md" diagramas/
cp "$ORIGEN/diagramas/ver_general_paises.html" diagramas/
cp "$ORIGEN/diagramas/visor_grafo.html" diagramas/
cp "$ORIGEN"/diagramas/grafo_*.html diagramas/
git rm -q diagramas/.gitkeep
git add diagramas/general_paises.md diagramas/ver_general_paises.html diagramas/visor_grafo.html diagramas/grafo_*.html
git commit -m "docs(grafo): agrega el diagrama general y las páginas por país"
```

**3. Verificar que compila** y que pasan las pruebas:

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out pruebas.PruebasHito4
```

Debe terminar con `Pruebas que pasaron: 20 | fallaron: 0` (PruebasHito4).

Revisa que `git status` no muestre `out/`, `.env` ni archivos que no son tuyos.

**4. Subir la rama:**

```bash
git push -u origin feature/grafo-juan
```

**5. Abrir el Pull Request en GitHub:** entra a <URL_DEL_REPOSITORIO>, pulsa **Compare & pull request** (o **Pull requests › New pull request**), elige **base: `develop`** y **compare: `feature/grafo-juan`**.

- Título: `Grafo por país, BFS, DFS, ruta y ciclo`
- Descripción:

```text
Closes #10
```

Avisa por el grupo cuando lo abras. No hagas merge tú: lo hace el líder.

### PR 3 de Juan: README final y documentación

**Rama:** `docs/readme` · **Empieza cuando** el líder haya integrado el segundo PR de Joseph (#13) en `develop`.

**1. Ponerse al día y crear tu rama:**

```bash
git checkout develop
git pull origin develop
git checkout -b docs/readme
```

**2. Commits, uno por bloque, en este orden** (con `ORIGEN` ya definido):

**Commit 1:** `docs: agrega las guías de la exposición y de la defensa`  
Archivos: `ESTADO_EXPOSICION.md`, `GUIA_CODIGO.md`, `docs/GUIA-DEFENSA.md`, `docs/GUION-DEMO.md`, `docs/RECORRIDO-PASO-A-PASO.md`, `docs/GUIA-EXPOSICION.html`, `docs/ESTADO-ACTUAL.md`

```bash
cp "$ORIGEN/ESTADO_EXPOSICION.md" .
cp "$ORIGEN/GUIA_CODIGO.md" .
cp "$ORIGEN/docs/GUIA-DEFENSA.md" docs/
cp "$ORIGEN/docs/GUION-DEMO.md" docs/
cp "$ORIGEN/docs/RECORRIDO-PASO-A-PASO.md" docs/
cp "$ORIGEN/docs/GUIA-EXPOSICION.html" docs/
cp "$ORIGEN/docs/ESTADO-ACTUAL.md" docs/
git add ESTADO_EXPOSICION.md GUIA_CODIGO.md docs/GUIA-DEFENSA.md docs/GUION-DEMO.md docs/RECORRIDO-PASO-A-PASO.md docs/GUIA-EXPOSICION.html docs/ESTADO-ACTUAL.md
git commit -m "docs: agrega las guías de la exposición y de la defensa"
```

**Commit 2:** `docs: agrega el diagrama de clases del Hito 1, los issues y el plan de commits`  
Archivos: `diagramas/Diagrama.md`, `docs/ISSUES.md`, `docs/PLAN_COMMITS.md`

```bash
cp "$ORIGEN/diagramas/Diagrama.md" diagramas/
cp "$ORIGEN/docs/ISSUES.md" docs/
cp "$ORIGEN/docs/PLAN_COMMITS.md" docs/
git add diagramas/Diagrama.md docs/ISSUES.md docs/PLAN_COMMITS.md
git commit -m "docs: agrega el diagrama de clases del Hito 1, los issues y el plan de commits"
```

**Commit 3:** `docs(readme): agrega el README completo`  
Archivos: `README.md`

```bash
cp "$ORIGEN/README.md" .
git add README.md
git commit -m "docs(readme): agrega el README completo"
```

**Commit 4:** `docs: agrega la guía de capturas de la interfaz`  
Archivos: `docs/img/LEEME.md`  
Borra: `docs/img/.gitkeep`

```bash
cp "$ORIGEN/docs/img/LEEME.md" docs/img/
git rm -q docs/img/.gitkeep
git add docs/img/LEEME.md
git commit -m "docs: agrega la guía de capturas de la interfaz"
```

**3. Verificar que compila**:

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
```

Revisa que `git status` no muestre `out/`, `.env` ni archivos que no son tuyos.

**4. Subir la rama:**

```bash
git push -u origin docs/readme
```

**5. Abrir el Pull Request en GitHub:** entra a <URL_DEL_REPOSITORIO>, pulsa **Compare & pull request** (o **Pull requests › New pull request**), elige **base: `develop`** y **compare: `docs/readme`**.

- Título: `README final y documentación`
- Descripción:

```text
Closes #14
```

Avisa por el grupo cuando lo abras. No hagas merge tú: lo hace el líder.

---

## Jeremy Torosina (@WinoSpop)

> **Mensaje para enviar por WhatsApp.** Copia desde aquí hasta el final de esta sección.

Hola Jeremy 👋 Este es tu plan para subir tu parte de SmartETL-DS desde tu cuenta de GitHub (@WinoSpop). Tienes 1 Pull Request en la rama `feature/datos-jeremy`. Sigue los pasos en orden y **espera tu turno**: cada PR empieza cuando el anterior ya está integrado.

**Preparación (una sola vez):**

1. Acepta la invitación al repositorio que te llega al correo de GitHub.
2. Descarga y descomprime `SmartETL_DS-final.zip` (lo comparte el líder).
3. Abre **Git Bash** en la carpeta donde quieras trabajar y ejecuta (cambia los datos entre `< >`):

```bash
git clone <URL_DEL_REPOSITORIO>
cd <NOMBRE_DEL_REPOSITORIO>
git config user.name "Jeremy Torosina"
git config user.email "<correo-de-tu-cuenta-de-GitHub>"
ORIGEN="/c/Users/<tu-usuario>/Downloads/SmartETL_DS-main"
```

`ORIGEN` es la carpeta descomprimida del zip (la que tiene `src/`, `web/` y `data/`). Si cierras Git Bash, vuelve a ejecutar la línea `ORIGEN=...` antes de seguir. El correo debe ser el de tu cuenta de GitHub para que los commits salgan con tu nombre.

### PR 1 de Jeremy: Listas propias, modelo y dataset OpenFlights

**Rama:** `feature/datos-jeremy` · **Empieza cuando** el líder haya integrado el PR de seguridad (#2) en `develop`.

**1. Ponerse al día y crear tu rama:**

```bash
git checkout develop
git pull origin develop
git checkout -b feature/datos-jeremy
```

**2. Commits, uno por bloque, en este orden** (con `ORIGEN` ya definido):

**Commit 1:** `feat(estructuras): agrega la interfaz Lista y el nodo simple`  
Archivos: `src/estructuras/Lista.java`, `src/estructuras/Criterio.java`, `src/estructuras/ResultadoBusqueda.java`, `src/estructuras/Nodo.java`  
Borra: `src/.gitkeep`, `src/estructuras/.gitkeep`

```bash
cp "$ORIGEN/src/estructuras/Lista.java" src/estructuras/
cp "$ORIGEN/src/estructuras/Criterio.java" src/estructuras/
cp "$ORIGEN/src/estructuras/ResultadoBusqueda.java" src/estructuras/
cp "$ORIGEN/src/estructuras/Nodo.java" src/estructuras/
git rm -q src/.gitkeep src/estructuras/.gitkeep
git add src/estructuras/Lista.java src/estructuras/Criterio.java src/estructuras/ResultadoBusqueda.java src/estructuras/Nodo.java
git commit -m "feat(estructuras): agrega la interfaz Lista y el nodo simple"
```

**Commit 2:** `feat(estructuras): agrega la lista enlazada y la bitácora de eventos`  
Archivos: `src/estructuras/ListaEnlazada.java`, `src/ia/Evento.java`, `src/ia/RegistroEventos.java`  
Borra: `src/ia/.gitkeep`

```bash
cp "$ORIGEN/src/estructuras/ListaEnlazada.java" src/estructuras/
cp "$ORIGEN/src/ia/Evento.java" src/ia/
cp "$ORIGEN/src/ia/RegistroEventos.java" src/ia/
git rm -q src/ia/.gitkeep
git add src/estructuras/ListaEnlazada.java src/ia/Evento.java src/ia/RegistroEventos.java
git commit -m "feat(estructuras): agrega la lista enlazada y la bitácora de eventos"
```

**Commit 3:** `feat(estructuras): agrega la lista secuencial que duplica su capacidad`  
Archivos: `src/estructuras/ListaSecuencial.java`

```bash
cp "$ORIGEN/src/estructuras/ListaSecuencial.java" src/estructuras/
git add src/estructuras/ListaSecuencial.java
git commit -m "feat(estructuras): agrega la lista secuencial que duplica su capacidad"
```

**Commit 4:** `feat(modelo): agrega Registro, Aeropuerto, Ruta y Aerolinea`  
Archivos: `src/model/Registro.java`, `src/model/Aeropuerto.java`, `src/model/Ruta.java`, `src/model/Aerolinea.java`  
Borra: `src/model/.gitkeep`

```bash
cp "$ORIGEN/src/model/Registro.java" src/model/
cp "$ORIGEN/src/model/Aeropuerto.java" src/model/
cp "$ORIGEN/src/model/Ruta.java" src/model/
cp "$ORIGEN/src/model/Aerolinea.java" src/model/
git rm -q src/model/.gitkeep
git add src/model/Registro.java src/model/Aeropuerto.java src/model/Ruta.java src/model/Aerolinea.java
git commit -m "feat(modelo): agrega Registro, Aeropuerto, Ruta y Aerolinea"
```

**Commit 5:** `feat(datos): agrega el lector CSV y las opciones de carga`  
Archivos: `src/etl/LectorCSV.java`, `src/etl/Mapeador.java`, `src/etl/ModoCarga.java`, `src/etl/OpcionesCarga.java`, `src/etl/ResultadoExtraccion.java`, `src/etl/FuenteDatos.java`, `src/etl/Proyecto.java`  
Borra: `src/etl/.gitkeep`

```bash
cp "$ORIGEN/src/etl/LectorCSV.java" src/etl/
cp "$ORIGEN/src/etl/Mapeador.java" src/etl/
cp "$ORIGEN/src/etl/ModoCarga.java" src/etl/
cp "$ORIGEN/src/etl/OpcionesCarga.java" src/etl/
cp "$ORIGEN/src/etl/ResultadoExtraccion.java" src/etl/
cp "$ORIGEN/src/etl/FuenteDatos.java" src/etl/
cp "$ORIGEN/src/etl/Proyecto.java" src/etl/
git rm -q src/etl/.gitkeep
git add src/etl/LectorCSV.java src/etl/Mapeador.java src/etl/ModoCarga.java src/etl/OpcionesCarga.java src/etl/ResultadoExtraccion.java src/etl/FuenteDatos.java src/etl/Proyecto.java
git commit -m "feat(datos): agrega el lector CSV y las opciones de carga"
```

**Commit 6:** `feat(datos): agrega el dataset OpenFlights`  
Archivos: `data/airports.csv`, `data/routes.csv`, `data/airlines.csv`, `data/FUENTE.txt`  
Borra: `data/.gitkeep`

```bash
cp "$ORIGEN/data/airports.csv" data/
cp "$ORIGEN/data/routes.csv" data/
cp "$ORIGEN/data/airlines.csv" data/
cp "$ORIGEN/data/FUENTE.txt" data/
git rm -q data/.gitkeep
git add data/airports.csv data/routes.csv data/airlines.csv data/FUENTE.txt
git commit -m "feat(datos): agrega el dataset OpenFlights"
```

**3. Verificar que compila**:

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
```

Revisa que `git status` no muestre `out/`, `.env` ni archivos que no son tuyos.

**4. Subir la rama:**

```bash
git push -u origin feature/datos-jeremy
```

**5. Abrir el Pull Request en GitHub:** entra a <URL_DEL_REPOSITORIO>, pulsa **Compare & pull request** (o **Pull requests › New pull request**), elige **base: `develop`** y **compare: `feature/datos-jeremy`**.

- Título: `Listas propias, modelo y dataset OpenFlights`
- Descripción:

```text
Closes #3
Closes #4
```

Avisa por el grupo cuando lo abras. No hagas merge tú: lo hace el líder.

---

## Noemi Tuza (@edithtuza15-collab)

> **Mensaje para enviar por WhatsApp.** Copia desde aquí hasta el final de esta sección.

Hola Noemi 👋 Este es tu plan para subir tu parte de SmartETL-DS desde tu cuenta de GitHub (@edithtuza15-collab). Tienes 2 Pull Requests en la rama `feature/etl-noemi`. Sigue los pasos en orden y **espera tu turno**: cada PR empieza cuando el anterior ya está integrado.

**Preparación (una sola vez):**

1. Acepta la invitación al repositorio que te llega al correo de GitHub.
2. Descarga y descomprime `SmartETL_DS-final.zip` (lo comparte el líder).
3. Abre **Git Bash** en la carpeta donde quieras trabajar y ejecuta (cambia los datos entre `< >`):

```bash
git clone <URL_DEL_REPOSITORIO>
cd <NOMBRE_DEL_REPOSITORIO>
git config user.name "Noemi Tuza"
git config user.email "<correo-de-tu-cuenta-de-GitHub>"
ORIGEN="/c/Users/<tu-usuario>/Downloads/SmartETL_DS-main"
```

`ORIGEN` es la carpeta descomprimida del zip (la que tiene `src/`, `web/` y `data/`). Si cierras Git Bash, vuelve a ejecutar la línea `ORIGEN=...` antes de seguir. El correo debe ser el de tu cuenta de GitHub para que los commits salgan con tu nombre.

### PR 1 de Noemi: Cola de extracción, pila y Extract

**Rama:** `feature/etl-noemi` · **Empieza cuando** el líder haya integrado el PR de Jeremy (#3 y #4) en `develop`.

**1. Ponerse al día y crear tu rama:**

```bash
git checkout develop
git pull origin develop
git checkout -b feature/etl-noemi
```

**2. Commits, uno por bloque, en este orden** (con `ORIGEN` ya definido):

**Commit 1:** `feat(etl): agrega la cola de extracción`  
Archivos: `src/estructuras/Cola.java`

```bash
cp "$ORIGEN/src/estructuras/Cola.java" src/estructuras/
git add src/estructuras/Cola.java
git commit -m "feat(etl): agrega la cola de extracción"
```

**Commit 2:** `feat(etl): agrega la pila de errores`  
Archivos: `src/estructuras/Pila.java`

```bash
cp "$ORIGEN/src/estructuras/Pila.java" src/estructuras/
git add src/estructuras/Pila.java
git commit -m "feat(etl): agrega la pila de errores"
```

**Commit 3:** `feat(etl): agrega Extract que encola los registros del CSV`  
Archivos: `src/etl/Extract.java`

```bash
cp "$ORIGEN/src/etl/Extract.java" src/etl/
git add src/etl/Extract.java
git commit -m "feat(etl): agrega Extract que encola los registros del CSV"
```

**Commit 4:** `test(datos): agrega las pruebas del Hito 1`  
Archivos: `src/pruebas/PruebasHito1.java`  
Borra: `src/pruebas/.gitkeep`

```bash
cp "$ORIGEN/src/pruebas/PruebasHito1.java" src/pruebas/
git rm -q src/pruebas/.gitkeep
git add src/pruebas/PruebasHito1.java
git commit -m "test(datos): agrega las pruebas del Hito 1"
```

**3. Verificar que compila** y que pasan las pruebas:

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out pruebas.PruebasHito1
```

Debe terminar con `Pruebas que pasaron: 53 | fallaron: 0` (PruebasHito1).

Revisa que `git status` no muestre `out/`, `.env` ni archivos que no son tuyos.

**4. Subir la rama:**

```bash
git push -u origin feature/etl-noemi
```

**5. Abrir el Pull Request en GitHub:** entra a <URL_DEL_REPOSITORIO>, pulsa **Compare & pull request** (o **Pull requests › New pull request**), elige **base: `develop`** y **compare: `feature/etl-noemi`**.

- Título: `Cola de extracción, pila y Extract`
- Descripción:

```text
Closes #5
```

Avisa por el grupo cuando lo abras. No hagas merge tú: lo hace el líder.

### PR 2 de Noemi: Transform, pila de errores y Load

**Rama:** `feature/etl-noemi` · **Empieza cuando** el líder haya integrado el primer PR de Joseph (#8) en `develop`.

**1. Ponerse al día y volver a tu rama:**

```bash
git checkout develop
git pull origin develop
git checkout feature/etl-noemi
git merge develop
```

`git merge develop` trae a tu rama lo que ya integraron los demás. Si abre un editor para el mensaje del merge, guarda y cierra.

**2. Commits, uno por bloque, en este orden** (con `ORIGEN` ya definido):

**Commit 1:** `feat(etl): agrega los tipos de error y ErrorETL`  
Archivos: `src/model/TipoError.java`, `src/model/ErrorETL.java`

```bash
cp "$ORIGEN/src/model/TipoError.java" src/model/
cp "$ORIGEN/src/model/ErrorETL.java" src/model/
git add src/model/TipoError.java src/model/ErrorETL.java
git commit -m "feat(etl): agrega los tipos de error y ErrorETL"
```

**Commit 2:** `feat(etl): agrega Transform con validación, limpieza de \N y resolución de IDs`  
Archivos: `src/etl/DatosLimpios.java`, `src/etl/Transform.java`

```bash
cp "$ORIGEN/src/etl/DatosLimpios.java" src/etl/
cp "$ORIGEN/src/etl/Transform.java" src/etl/
git add src/etl/DatosLimpios.java src/etl/Transform.java
git commit -m "feat(etl): agrega Transform con validación, limpieza de \\N y resolución de IDs"
```

**Commit 3:** `feat(etl): agrega Load y el proceso ETL completo`  
Archivos: `src/etl/Load.java`, `src/etl/ProcesoETL.java`

```bash
cp "$ORIGEN/src/etl/Load.java" src/etl/
cp "$ORIGEN/src/etl/ProcesoETL.java" src/etl/
git add src/etl/Load.java src/etl/ProcesoETL.java
git commit -m "feat(etl): agrega Load y el proceso ETL completo"
```

**Commit 4:** `feat(datos): agrega los datos sucios de demostración`  
Archivos: `data/demo/airports_sucio.csv`, `data/demo/routes_sucio.csv`, `data/demo/LEEME.txt`  
Borra: `data/demo/.gitkeep`

```bash
cp "$ORIGEN/data/demo/airports_sucio.csv" data/demo/
cp "$ORIGEN/data/demo/routes_sucio.csv" data/demo/
cp "$ORIGEN/data/demo/LEEME.txt" data/demo/
git rm -q data/demo/.gitkeep
git add data/demo/airports_sucio.csv data/demo/routes_sucio.csv data/demo/LEEME.txt
git commit -m "feat(datos): agrega los datos sucios de demostración"
```

**Commit 5:** `test(etl): agrega las pruebas del Hito 2`  
Archivos: `src/pruebas/PruebasHito2.java`

```bash
cp "$ORIGEN/src/pruebas/PruebasHito2.java" src/pruebas/
git add src/pruebas/PruebasHito2.java
git commit -m "test(etl): agrega las pruebas del Hito 2"
```

**3. Verificar que compila** y que pasan las pruebas:

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out pruebas.PruebasHito2
```

Debe terminar con `Pruebas que pasaron: 55 | fallaron: 0` (PruebasHito2).

Revisa que `git status` no muestre `out/`, `.env` ni archivos que no son tuyos.

**4. Subir la rama:**

```bash
git push -u origin feature/etl-noemi
```

**5. Abrir el Pull Request en GitHub:** entra a <URL_DEL_REPOSITORIO>, pulsa **Compare & pull request** (o **Pull requests › New pull request**), elige **base: `develop`** y **compare: `feature/etl-noemi`**.

- Título: `Transform, pila de errores y Load`
- Descripción:

```text
Closes #9
```

Avisa por el grupo cuando lo abras. No hagas merge tú: lo hace el líder.

---

## Andres Llamuca (@llamucaandres161)

> **Mensaje para enviar por WhatsApp.** Copia desde aquí hasta el final de esta sección.

Hola Andres 👋 Este es tu plan para subir tu parte de SmartETL-DS desde tu cuenta de GitHub (@llamucaandres161). Tienes 2 Pull Requests en la rama `feature/bst-andres`. Sigue los pasos en orden y **espera tu turno**: cada PR empieza cuando el anterior ya está integrado.

**Preparación (una sola vez):**

1. Acepta la invitación al repositorio que te llega al correo de GitHub.
2. Descarga y descomprime `SmartETL_DS-final.zip` (lo comparte el líder).
3. Abre **Git Bash** en la carpeta donde quieras trabajar y ejecuta (cambia los datos entre `< >`):

```bash
git clone <URL_DEL_REPOSITORIO>
cd <NOMBRE_DEL_REPOSITORIO>
git config user.name "Andres Llamuca"
git config user.email "<correo-de-tu-cuenta-de-GitHub>"
ORIGEN="/c/Users/<tu-usuario>/Downloads/SmartETL_DS-main"
```

`ORIGEN` es la carpeta descomprimida del zip (la que tiene `src/`, `web/` y `data/`). Si cierras Git Bash, vuelve a ejecutar la línea `ORIGEN=...` antes de seguir. El correo debe ser el de tu cuenta de GitHub para que los commits salgan con tu nombre.

### PR 1 de Andres: Árbol binario de búsqueda y búsquedas

**Rama:** `feature/bst-andres` · **Empieza cuando** el líder haya integrado el primer PR de Noemi (#5) en `develop`.

**1. Ponerse al día y crear tu rama:**

```bash
git checkout develop
git pull origin develop
git checkout -b feature/bst-andres
```

**2. Commits, uno por bloque, en este orden** (con `ORIGEN` ya definido):

**Commit 1:** `feat(bst): agrega el nodo del árbol`  
Archivos: `src/estructuras/NodoArbol.java`

```bash
cp "$ORIGEN/src/estructuras/NodoArbol.java" src/estructuras/
git add src/estructuras/NodoArbol.java
git commit -m "feat(bst): agrega el nodo del árbol"
```

**Commit 2:** `feat(bst): agrega ArbolBST con insertar, buscar y eliminar iterativos`  
Archivos: `src/estructuras/ArbolBST.java`

```bash
cp "$ORIGEN/src/estructuras/ArbolBST.java" src/estructuras/
git add src/estructuras/ArbolBST.java
git commit -m "feat(bst): agrega ArbolBST con insertar, buscar y eliminar iterativos"
```

**Commit 3:** `feat(bst): agrega búsqueda secuencial, binaria y por árbol`  
Archivos: `src/algoritmos/Busqueda.java`, `src/algoritmos/Comparadores.java`  
Borra: `src/algoritmos/.gitkeep`

```bash
cp "$ORIGEN/src/algoritmos/Busqueda.java" src/algoritmos/
cp "$ORIGEN/src/algoritmos/Comparadores.java" src/algoritmos/
git rm -q src/algoritmos/.gitkeep
git add src/algoritmos/Busqueda.java src/algoritmos/Comparadores.java
git commit -m "feat(bst): agrega búsqueda secuencial, binaria y por árbol"
```

**3. Verificar que compila**:

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
```

Revisa que `git status` no muestre `out/`, `.env` ni archivos que no son tuyos.

**4. Subir la rama:**

```bash
git push -u origin feature/bst-andres
```

**5. Abrir el Pull Request en GitHub:** entra a <URL_DEL_REPOSITORIO>, pulsa **Compare & pull request** (o **Pull requests › New pull request**), elige **base: `develop`** y **compare: `feature/bst-andres`**.

- Título: `Árbol binario de búsqueda y búsquedas`
- Descripción:

```text
Closes #6
Refs #7
```

Avisa por el grupo cuando lo abras. No hagas merge tú: lo hace el líder.

### PR 2 de Andres: Pruebas del BST

**Rama:** `feature/bst-andres` · **Empieza cuando** el líder haya integrado el PR del grafo (#10) en `develop`.

**1. Ponerse al día y volver a tu rama:**

```bash
git checkout develop
git pull origin develop
git checkout feature/bst-andres
git merge develop
```

`git merge develop` trae a tu rama lo que ya integraron los demás. Si abre un editor para el mensaje del merge, guarda y cierra.

**2. Commits, uno por bloque, en este orden** (con `ORIGEN` ya definido):

**Commit 1:** `test(bst): agrega las pruebas del Hito 3`  
Archivos: `src/pruebas/PruebasHito3.java`

```bash
cp "$ORIGEN/src/pruebas/PruebasHito3.java" src/pruebas/
git add src/pruebas/PruebasHito3.java
git commit -m "test(bst): agrega las pruebas del Hito 3"
```

**3. Verificar que compila** y que pasan las pruebas:

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out pruebas.PruebasHito3
```

Debe terminar con `Pruebas que pasaron: 32 | fallaron: 0` (PruebasHito3).

Revisa que `git status` no muestre `out/`, `.env` ni archivos que no son tuyos.

**4. Subir la rama:**

```bash
git push -u origin feature/bst-andres
```

**5. Abrir el Pull Request en GitHub:** entra a <URL_DEL_REPOSITORIO>, pulsa **Compare & pull request** (o **Pull requests › New pull request**), elige **base: `develop`** y **compare: `feature/bst-andres`**.

- Título: `Pruebas del BST`
- Descripción:

```text
Closes #7
```

Avisa por el grupo cuando lo abras. No hagas merge tú: lo hace el líder.

---

## Joseph Romo (@wayusa25-cmyk)

> **Mensaje para enviar por WhatsApp.** Copia desde aquí hasta el final de esta sección.

Hola Joseph 👋 Este es tu plan para subir tu parte de SmartETL-DS desde tu cuenta de GitHub (@wayusa25-cmyk). Tienes 2 Pull Requests en la rama `feature/web-joseph`. Sigue los pasos en orden y **espera tu turno**: cada PR empieza cuando el anterior ya está integrado.

**Preparación (una sola vez):**

1. Acepta la invitación al repositorio que te llega al correo de GitHub.
2. Descarga y descomprime `SmartETL_DS-final.zip` (lo comparte el líder).
3. Abre **Git Bash** en la carpeta donde quieras trabajar y ejecuta (cambia los datos entre `< >`):

```bash
git clone <URL_DEL_REPOSITORIO>
cd <NOMBRE_DEL_REPOSITORIO>
git config user.name "Joseph Romo"
git config user.email "<correo-de-tu-cuenta-de-GitHub>"
ORIGEN="/c/Users/<tu-usuario>/Downloads/SmartETL_DS-main"
```

`ORIGEN` es la carpeta descomprimida del zip (la que tiene `src/`, `web/` y `data/`). Si cierras Git Bash, vuelve a ejecutar la línea `ORIGEN=...` antes de seguir. El correo debe ser el de tu cuenta de GitHub para que los commits salgan con tu nombre.

### PR 1 de Joseph: Ordenamiento burbuja e inserción

**Rama:** `feature/web-joseph` · **Empieza cuando** el líder haya integrado el primer PR de Andres (#6) en `develop`.

**1. Ponerse al día y crear tu rama:**

```bash
git checkout develop
git pull origin develop
git checkout -b feature/web-joseph
```

**2. Commits, uno por bloque, en este orden** (con `ORIGEN` ya definido):

**Commit 1:** `feat(algoritmos): agrega ordenamiento burbuja e inserción`  
Archivos: `src/algoritmos/Ordenamiento.java`, `src/algoritmos/ResultadoOrdenamiento.java`

```bash
cp "$ORIGEN/src/algoritmos/Ordenamiento.java" src/algoritmos/
cp "$ORIGEN/src/algoritmos/ResultadoOrdenamiento.java" src/algoritmos/
git add src/algoritmos/Ordenamiento.java src/algoritmos/ResultadoOrdenamiento.java
git commit -m "feat(algoritmos): agrega ordenamiento burbuja e inserción"
```

**3. Verificar que compila**:

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
```

Revisa que `git status` no muestre `out/`, `.env` ni archivos que no son tuyos.

**4. Subir la rama:**

```bash
git push -u origin feature/web-joseph
```

**5. Abrir el Pull Request en GitHub:** entra a <URL_DEL_REPOSITORIO>, pulsa **Compare & pull request** (o **Pull requests › New pull request**), elige **base: `develop`** y **compare: `feature/web-joseph`**.

- Título: `Ordenamiento burbuja e inserción`
- Descripción:

```text
Closes #8
```

Avisa por el grupo cuando lo abras. No hagas merge tú: lo hace el líder.

### PR 2 de Joseph: Capa web: servidor Java y frontend

**Rama:** `feature/web-joseph` · **Empieza cuando** el líder haya integrado el PR de IA (#11 y #12) en `develop`.

**1. Ponerse al día y volver a tu rama:**

```bash
git checkout develop
git pull origin develop
git checkout feature/web-joseph
git merge develop
```

`git merge develop` trae a tu rama lo que ya integraron los demás. Si abre un editor para el mensaje del merge, guarda y cierra.

**2. Commits, uno por bloque, en este orden** (con `ORIGEN` ya definido):

**Commit 1:** `feat(web): agrega el servidor HTTP, la seguridad, los endpoints y el lanzador`  
Archivos: `src/app/Main.java`, `src/app/Sesion.java`, `src/app/web/*.java`, `src/app/swing/*.java`  
Borra: `src/app/.gitkeep`, `src/app/web/.gitkeep`, `src/app/swing/.gitkeep`

```bash
cp "$ORIGEN/src/app/Main.java" src/app/
cp "$ORIGEN/src/app/Sesion.java" src/app/
cp "$ORIGEN"/src/app/web/*.java src/app/web/
cp "$ORIGEN"/src/app/swing/*.java src/app/swing/
git rm -q src/app/.gitkeep src/app/web/.gitkeep src/app/swing/.gitkeep
git add src/app/Main.java src/app/Sesion.java src/app/web/*.java src/app/swing/*.java
git commit -m "feat(web): agrega el servidor HTTP, la seguridad, los endpoints y el lanzador"
```

**Commit 2:** `feat(web): agrega las librerías del frontend con sus licencias`  
Archivos: `recursos/*.js`, `recursos/*.mjs`, `recursos/licencias/*.txt`  
Borra: `recursos/.gitkeep`, `recursos/licencias/.gitkeep`

```bash
cp "$ORIGEN"/recursos/*.js recursos/
cp "$ORIGEN"/recursos/*.mjs recursos/
cp "$ORIGEN"/recursos/licencias/*.txt recursos/licencias/
git rm -q recursos/.gitkeep recursos/licencias/.gitkeep
git add recursos/*.js recursos/*.mjs recursos/licencias/*.txt
git commit -m "feat(web): agrega las librerías del frontend con sus licencias"
```

**Commit 3:** `feat(web): agrega los íconos y los logos de la UTA y la FISEI`  
Archivos: `recursos/iconos/*.svg`, `recursos/logos/*.png`  
Borra: `recursos/iconos/.gitkeep`, `recursos/logos/.gitkeep`

```bash
cp "$ORIGEN"/recursos/iconos/*.svg recursos/iconos/
cp "$ORIGEN"/recursos/logos/*.png recursos/logos/
git rm -q recursos/iconos/.gitkeep recursos/logos/.gitkeep
git add recursos/iconos/*.svg recursos/logos/*.png
git commit -m "feat(web): agrega los íconos y los logos de la UTA y la FISEI"
```

**Commit 4:** `feat(web): agrega la página principal y los estilos`  
Archivos: `web/index.html`, `web/*.css`  
Borra: `web/.gitkeep`

```bash
cp "$ORIGEN/web/index.html" web/
cp "$ORIGEN"/web/*.css web/
git rm -q web/.gitkeep
git add web/index.html web/*.css
git commit -m "feat(web): agrega la página principal y los estilos"
```

**Commit 5:** `feat(web): agrega la lógica del frontend por pestaña`  
Archivos: `web/js/*.js`  
Borra: `web/js/.gitkeep`

```bash
cp "$ORIGEN"/web/js/*.js web/js/
git rm -q web/js/.gitkeep
git add web/js/*.js
git commit -m "feat(web): agrega la lógica del frontend por pestaña"
```

**Commit 6:** `docs(web): agrega el diseño y la guía de la interfaz web`  
Archivos: `PRODUCT.md`, `DESIGN.md`, `docs/GUIA-INTERFAZ-WEB.md`, `docs/MIGRACION-FASE0.md`  
Borra: `docs/.gitkeep`

```bash
cp "$ORIGEN/PRODUCT.md" .
cp "$ORIGEN/DESIGN.md" .
cp "$ORIGEN/docs/GUIA-INTERFAZ-WEB.md" docs/
cp "$ORIGEN/docs/MIGRACION-FASE0.md" docs/
git rm -q docs/.gitkeep
git add PRODUCT.md DESIGN.md docs/GUIA-INTERFAZ-WEB.md docs/MIGRACION-FASE0.md
git commit -m "docs(web): agrega el diseño y la guía de la interfaz web"
```

**3. Verificar que compila**:

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
```

Revisa que `git status` no muestre `out/`, `.env` ni archivos que no son tuyos.

**4. Subir la rama:**

```bash
git push -u origin feature/web-joseph
```

**5. Abrir el Pull Request en GitHub:** entra a <URL_DEL_REPOSITORIO>, pulsa **Compare & pull request** (o **Pull requests › New pull request**), elige **base: `develop`** y **compare: `feature/web-joseph`**.

- Título: `Capa web: servidor Java y frontend`
- Descripción:

```text
Closes #13
```

Avisa por el grupo cuando lo abras. No hagas merge tú: lo hace el líder.

---

## Jullisa Altamirano (@jullisaaltamirano2017-boop)

> **Mensaje para enviar por WhatsApp.** Copia desde aquí hasta el final de esta sección.

Hola Jullisa 👋 Este es tu plan para subir tu parte de SmartETL-DS desde tu cuenta de GitHub (@jullisaaltamirano2017-boop). Tienes 1 Pull Request en la rama `feature/ia-jullisa`. Sigue los pasos en orden y **espera tu turno**: cada PR empieza cuando el anterior ya está integrado.

**Preparación (una sola vez):**

1. Acepta la invitación al repositorio que te llega al correo de GitHub.
2. Descarga y descomprime `SmartETL_DS-final.zip` (lo comparte el líder).
3. Abre **Git Bash** en la carpeta donde quieras trabajar y ejecuta (cambia los datos entre `< >`):

```bash
git clone <URL_DEL_REPOSITORIO>
cd <NOMBRE_DEL_REPOSITORIO>
git config user.name "Jullisa Altamirano"
git config user.email "<correo-de-tu-cuenta-de-GitHub>"
ORIGEN="/c/Users/<tu-usuario>/Downloads/SmartETL_DS-main"
```

`ORIGEN` es la carpeta descomprimida del zip (la que tiene `src/`, `web/` y `data/`). Si cierras Git Bash, vuelve a ejecutar la línea `ORIGEN=...` antes de seguir. El correo debe ser el de tu cuenta de GitHub para que los commits salgan con tu nombre.

### PR 1 de Jullisa: Capa de IA: proveedores, API keys, prompt con Mermaid y respaldo

**Rama:** `feature/ia-jullisa` · **Empieza cuando** el líder haya integrado el segundo PR de Andres (#7) en `develop`.

**1. Ponerse al día y crear tu rama:**

```bash
git checkout develop
git pull origin develop
git checkout -b feature/ia-jullisa
```

**2. Commits, uno por bloque, en este orden** (con `ORIGEN` ya definido):

**Commit 1:** `feat(ia): agrega el lector y escritor JSON`  
Archivos: `src/ia/Json.java`

```bash
cp "$ORIGEN/src/ia/Json.java" src/ia/
git add src/ia/Json.java
git commit -m "feat(ia): agrega el lector y escritor JSON"
```

**Commit 2:** `feat(ia): agrega los clientes de Gemini, OpenAI, GLM, GitHub Models y Claude`  
Archivos: `src/ia/ProveedorIA.java`, `src/ia/ClienteCompatibleOpenAI.java`, `src/ia/ClienteAnthropic.java`

```bash
cp "$ORIGEN/src/ia/ProveedorIA.java" src/ia/
cp "$ORIGEN/src/ia/ClienteCompatibleOpenAI.java" src/ia/
cp "$ORIGEN/src/ia/ClienteAnthropic.java" src/ia/
git add src/ia/ProveedorIA.java src/ia/ClienteCompatibleOpenAI.java src/ia/ClienteAnthropic.java
git commit -m "feat(ia): agrega los clientes de Gemini, OpenAI, GLM, GitHub Models y Claude"
```

**Commit 3:** `feat(ia): agrega la configuración de proveedores y las APIs guardadas`  
Archivos: `src/ia/ConfiguracionIA.java`, `src/ia/ApiGuardada.java`, `src/ia/AlmacenApis.java`

```bash
cp "$ORIGEN/src/ia/ConfiguracionIA.java" src/ia/
cp "$ORIGEN/src/ia/ApiGuardada.java" src/ia/
cp "$ORIGEN/src/ia/AlmacenApis.java" src/ia/
git add src/ia/ConfiguracionIA.java src/ia/ApiGuardada.java src/ia/AlmacenApis.java
git commit -m "feat(ia): agrega la configuración de proveedores y las APIs guardadas"
```

**Commit 4:** `feat(ia): agrega el constructor Mermaid y los visores HTML`  
Archivos: `src/ia/Mermaid.java`, `src/ia/ResultadoIA.java`, `src/ia/VisorHtml.java`, `src/ia/VisorGrafo.java`

```bash
cp "$ORIGEN/src/ia/Mermaid.java" src/ia/
cp "$ORIGEN/src/ia/ResultadoIA.java" src/ia/
cp "$ORIGEN/src/ia/VisorHtml.java" src/ia/
cp "$ORIGEN/src/ia/VisorGrafo.java" src/ia/
git add src/ia/Mermaid.java src/ia/ResultadoIA.java src/ia/VisorHtml.java src/ia/VisorGrafo.java
git commit -m "feat(ia): agrega el constructor Mermaid y los visores HTML"
```

**Commit 5:** `feat(ia): agrega el respaldo local y las trazas de estructuras`  
Archivos: `src/ia/GeneradorFlujogramaLocal.java`, `src/ia/TrazasEstructuras.java`

```bash
cp "$ORIGEN/src/ia/GeneradorFlujogramaLocal.java" src/ia/
cp "$ORIGEN/src/ia/TrazasEstructuras.java" src/ia/
git add src/ia/GeneradorFlujogramaLocal.java src/ia/TrazasEstructuras.java
git commit -m "feat(ia): agrega el respaldo local y las trazas de estructuras"
```

**Commit 6:** `feat(ia): agrega el prompt con reglas de Mermaid y el analizador con coherencia`  
Archivos: `src/ia/GeneradorPrompt.java`, `src/ia/AnalizadorIA.java`

```bash
cp "$ORIGEN/src/ia/GeneradorPrompt.java" src/ia/
cp "$ORIGEN/src/ia/AnalizadorIA.java" src/ia/
git add src/ia/GeneradorPrompt.java src/ia/AnalizadorIA.java
git commit -m "feat(ia): agrega el prompt con reglas de Mermaid y el analizador con coherencia"
```

**Commit 7:** `test(ia): agrega las pruebas de la capa de IA`  
Archivos: `src/pruebas/PruebasIA.java`

```bash
cp "$ORIGEN/src/pruebas/PruebasIA.java" src/pruebas/
git add src/pruebas/PruebasIA.java
git commit -m "test(ia): agrega las pruebas de la capa de IA"
```

**Commit 8:** `docs(ia): agrega los flujogramas generados y las evidencias`  
Archivos: `diagramas/flujo_*.md`, `diagramas/ver_flujo_*.html`, `diagramas/DOCUMENTACION.md`, `evidencias_ia/*.md`  
Borra: `evidencias_ia/.gitkeep`

```bash
cp "$ORIGEN"/diagramas/flujo_*.md diagramas/
cp "$ORIGEN"/diagramas/ver_flujo_*.html diagramas/
cp "$ORIGEN/diagramas/DOCUMENTACION.md" diagramas/
cp "$ORIGEN"/evidencias_ia/*.md evidencias_ia/
git rm -q evidencias_ia/.gitkeep
git add diagramas/flujo_*.md diagramas/ver_flujo_*.html diagramas/DOCUMENTACION.md evidencias_ia/*.md
git commit -m "docs(ia): agrega los flujogramas generados y las evidencias"
```

**3. Verificar que compila** y que pasan las pruebas:

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out pruebas.PruebasIA
```

Debe terminar con `Pruebas que pasaron: 60 | fallaron: 0` (PruebasIA).

Revisa que `git status` no muestre `out/`, `.env` ni archivos que no son tuyos.

**4. Subir la rama:**

```bash
git push -u origin feature/ia-jullisa
```

**5. Abrir el Pull Request en GitHub:** entra a <URL_DEL_REPOSITORIO>, pulsa **Compare & pull request** (o **Pull requests › New pull request**), elige **base: `develop`** y **compare: `feature/ia-jullisa`**.

- Título: `Capa de IA: proveedores, API keys, prompt con Mermaid y respaldo`
- Descripción:

```text
Closes #11
Closes #12
```

Avisa por el grupo cuando lo abras. No hagas merge tú: lo hace el líder.

---

## Si algo sale mal

| Problema | Solución |
|---|---|
| `git push` pide usuario y contraseña | GitHub ya no acepta la contraseña: inicia sesión en la ventana que abre Git para Windows o usa un token personal. |
| `git rm` dice que el `.gitkeep` no existe | Otro integrante ya lo borró. Quita ese `git rm` y sigue con el resto. |
| `cp: cannot stat` | `ORIGEN` no apunta a la carpeta del zip. Revisa la ruta con `ls "$ORIGEN"`: debe mostrar `src`, `web` y `data`. |
| Agregué un archivo que no era mío (antes de hacer push) | `git reset --soft HEAD~1` deshace el último commit sin perder los archivos; luego `git restore --staged <archivo>` y repite el commit. |
| `git merge develop` dice que hay conflicto | No debería pasar, porque cada uno toca archivos distintos. Si pasa, avisa al líder antes de seguir. |
| No compila | Revisa que copiaste todos los archivos de tu commit y que el PR anterior ya esté integrado en `develop` (`git pull origin develop`). |

## Archivos que no se suben

`out/` (clases compiladas), `output/*` menos `.gitkeep` (los genera el Load), `src/diagramas/` y `src/evidencias_ia/` (copias generadas por error al ejecutar desde `src/`), `.env` y `~/.smartetl/apis.properties` (llaves locales). Las capturas `docs/img/*.png` se agregan en el último PR cuando existan: `cp` y `git add` de cada imagen, en el mismo commit que `docs/img/LEEME.md`.
