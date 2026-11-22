# SmartETL-DS: sistema de diseño

Fuente única de las decisiones visuales de la interfaz web. Los valores viven como variables CSS en `web/styles.css`.

## Dirección

**Tablero de datos técnico y calmado**, inspirado en la aviación: fondo azul noche, acento color cielo y colores que significan algo (azul = extraer, morado = transformar, verde = válido, rojo = error, ámbar = cargar o advertir). El color nunca es adorno: si algo es rojo, es un error.

**Tema oscuro por defecto**, con tema claro opcional (botón sol/luna, recordado en el navegador).

## Color

| Variable | Oscuro | Claro | Uso |
|---|---|---|---|
| `--fondo` | `#0b1016` | `#f3f6f9` | Fondo de la página |
| `--superficie` | `#121a23` | `#ffffff` | Tarjetas, controles |
| `--superficie-2` / `-3` | `#182330` / `#1f2d3c` | `#f7f9fb` / `#eef2f6` | Filas alternadas, chips neutros |
| `--borde` / `--borde-fuerte` | `#253445` / `#34485e` | `#dbe3eb` / `#c3cfdb` | Bordes y hover |
| `--texto` / `--texto-suave` / `--texto-tenue` | `#e6edf4` / `#93a4b6` / `#66788b` | `#142030` / `#536476` / `#7c8b9b` | Jerarquía de texto |
| `--acento` | `#4cc3ff` | `#0a84c6` | Acción principal, pestaña activa, foco |
| `--exito` | `#34d399` | `#0f9f6e` | Válidos, coherente, validada |
| `--error` | `#f87171` | `#d64545` | Errores, pila de errores, fallo |
| `--aviso` | `#fbbf24` | `#b7791f` | Load, advertencias, validando |
| `--morado` | `#a78bfa` | `#7c5cd6` | Transform |
| `--azul` | `#60a5fa` | `#2f6fd6` | Extract, Cola |

Cada color semántico tiene su versión `-suave` (fondo translúcido) para chips y etiquetas. Contraste objetivo: texto normal AA (4,5:1) sobre `--superficie` en los dos temas.

**Colores de las etapas del ETL** (también en los diagramas Mermaid "vestidos"):

| Etapa | Color |
|---|---|
| Extract, Cola | `--azul` |
| Transform | `--morado` |
| Válidos | `--exito` |
| Error, Pila | `--error` |
| Load | `--aviso` |

## Tipografía

- Texto: la fuente del sistema (`Segoe UI Variable` en Windows), 15 px, interlineado 1,55. No se descargan fuentes: funciona sin internet.
- Números: `font-variant-numeric: tabular-nums` en toda la app, para que las cifras no bailen al contar.
- Código, rutas y tiempos: `Cascadia Mono` / `Consolas`.
- Escala: título de vista 26 px/700 · título de sección 19 px/700 · KPI 30 px/700 · texto 15 px · secundario 13–13,5 px · chips 12,5 px/600.

## Espaciado y forma

- Base de 4 px: 4, 6, 8, 10, 12, 14, 16, 20, 24, 28.
- Radios: tarjetas 12 px, controles 8 px, chips y píldoras 999 px.
- Ancho máximo del contenido 1.320 px; márgenes laterales de 24 px (16 px en móvil).
- Sombras solo para elevar al pasar el mouse y para overlays y toasts.

## Componentes

| Componente | Regla |
|---|---|
| Encabezado | Fijo: marca, carpeta del proyecto en mono pequeño, tarea en curso, píldora de estado de la IA, botón de tema |
| Pestañas | Ícono + texto; la activa en color acento con una línea inferior que crece; flechas ← → para moverse con el teclado |
| Botón primario | Fondo acento; uno por zona. Secundario: superficie con borde |
| Botones con `data-tarea` | Se bloquean mientras corre una tarea; el servidor también responde 409 |
| Tarjeta KPI | Ícono + etiqueta, número grande que cuenta hacia arriba, detalle en texto tenue |
| Chip | Estado o dato corto: neutro, acento, éxito, error, aviso |
| Estado vacío | Ícono en un cuadro suave, título, una línea y una acción |
| Overlay de carga | Tarjeta con spinner, mensaje en vivo (SSE), etapas ○ → ✓ con su tiempo; contador desde 3 s; no aparece si la tarea dura menos de 0,5 s; en ETL cubre solo la zona de resultados |
| Toast | Abajo a la derecha; éxito 4 s, error 7 s; ícono por tipo |

Íconos: Lucide 1.61.0 (ISC), copiados como SVG en `recursos/iconos/` y pintados con `mask` para heredar el color del texto.

## Movimiento

Reglas: nunca hacer esperar; tres velocidades; animación solo si explica algo; respetar `prefers-reduced-motion` (todas las duraciones bajan a 1 ms).

| Velocidad | Duración | Uso |
|---|---|---|
| Rápida | 150 ms | Hover, botón presionado, cambio de borde |
| Media | 300 ms | Cambio de pestaña (fundido + 10 px hacia arriba), entrada de overlay y toasts, tema |
| Didáctica | La controla el usuario | Visualizador de ordenamiento (reproducir, pausa, paso a paso, velocidad) |
| Recorrido | Unos 4 s en total | BFS/DFS sobre el grafo: los aeropuertos aparecen en el orden real de visita; botón "Repetir animación" |

Curva por defecto `cubic-bezier(0.22, 1, 0.36, 1)` (sale rápido, frena suave) para todo lo que entra. Los números KPI cuentan en 900 ms con `outExpo` (Anime.js). Las etapas completadas hacen un "pop" de 260 ms.

## Librerías del frontend

| Librería | Versión | Licencia | Uso |
|---|---|---|---|
| Anime.js | 4.5.0 (ESM) | MIT | Animaciones que enseñan y contadores KPI |
| AutoAnimate | 0.10.0 (ESM) | MIT | Listas que cambian (pila, historial, APIs) |
| Chart.js | 4.6.1 (UMD) | MIT | Barras y donas |
| Lucide (íconos) | 1.61.0 | ISC | Íconos SVG |
| Mermaid | 11.4.1 | MIT | Diagramas de la IA |
| svg-pan-zoom | 3.6.2 | BSD-2 | Zoom y arrastre de diagramas |
| vis-network | 9.1.9 | MIT / Apache-2.0 | Grafo y visor |
