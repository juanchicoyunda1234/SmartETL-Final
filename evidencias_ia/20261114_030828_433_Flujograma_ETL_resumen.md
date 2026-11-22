# Evidencia de IA: Flujograma ETL · resumen

- Fecha y hora: 2026-11-14 03:08:28
- Proveedor: Gemini
- Modelo: (no indicado)
- Se usó el respaldo local: sí
- Notas: La API no respondió bien (no hay clave de API). Se usó el respaldo local.

## Prompt enviado

````text
Eres profesor de Estructuras de Datos. Explica en español, en 2 a 4 párrafos cortos y claros para estudiantes de tercer semestre, qué muestra este flujograma paso a paso, en lenguaje sencillo para un estudiante.
Usa solo los datos reales que siguen; no inventes cifras ni aeropuertos. No uses tablas.

Datos reales:
flowchart TD
    N0(["Inicio del ETL: Extract a una Cola, Transform a Lista de válidos y Pila de errores"])
    N1["EXTRACT<br/>Extract lee airports.csv: 7698 líneas, 0 no leídas"]
    N0 --> N1
    N2["Cola de extracción<br/>7698 registros de aeropuertos encolados en la cola de extracción"]
    N1 --> N2
    N3["TRANSFORM de aeropuertos<br/>desencolar cada registro y validar"]
    N2 --> N3
    N4{"¿Registro válido?"}
    N3 --> N4
    N5["Lista de válidos: 7698<br/>ej. 1 GKA Goroka Airport"]
    N6["Pila de errores: 0"]
    N4 -->|"Sí"| N5
    N4 -->|"No"| N6
    N7["Totales de aeropuertos: 7698 procesados, 7698 válidos, 0 inválidos"]
    N5 --> N7
    N6 --> N7
    N8["Índices: BST global por IATA con 6071 nodos y lista de aeropuertos ordenada por ID para búsqueda binaria"]
    N7 --> N8
    N9["EXTRACT<br/>Extract lee routes.csv: 67663 líneas, 0 no leídas"]
    N8 --> N9
    N10["Cola de extracción<br/>67663 registros de rutas encolados en la cola de extracción"]
    N9 --> N10
    N11["TRANSFORM de rutas<br/>desencolar cada registro y validar"]
    N10 --> N11
    N12{"¿Registro válido?"}
    N11 --> N12
    N13["Lista de válidos: 67074<br/>ej. 2B AER-KZN (IDs 2965 -> 2990)"]
    N14["Pila de errores: 589<br/>REFERENCIA_ROTA 473, ID_NULO_NO_RECUPERABLE 116"]
    N12 -->|"Sí"| N13
    N12 -->|"No"| N14
    N15["Totales de rutas: 67663 procesados, 67074 válidos, 589 inválidos"]
    N13 --> N15
    N14 --> N15
    N16["EXTRACT<br/>Extract lee airlines.csv: 6162 líneas, 0 no leídas"]
    N15 --> N16
    N17["Cola de extracción<br/>6162 registros de aerolíneas encolados en la cola de extracción"]
    N16 --> N17
    N18["TRANSFORM de aerolíneas<br/>desencolar cada registro y validar"]
    N17 --> N18
    N19{"¿Registro válido?"}
    N18 --> N19
    N20["Lista de válidos: 6162<br/>ej. -1 Unknown"]
    N21["Pila de errores: 0"]
    N19 -->|"Sí"| N20
    N19 -->|"No"| N21
    N22["Totales de aerolíneas: 6162 procesados, 6162 válidos, 0 inválidos"]
    N20 --> N22
    N21 --> N22
    N23["Fin del Transform: 80934 válidos en listas y 589 errores en la pila"]
    N22 --> N23
    N24(["Fin del Transform (falta pulsar Cargar para el Load)"])
    N23 --> N24

````

## Respuesta cruda

````text
(sin respuesta de la API)
````

## Resultado final

Respaldo local (sin IA). Datos reales usados:

El diagrama tiene 18 pasos y 27 flechas. Recorrido: Inicio del ETL: Extract a una Cola, Transform a… → EXTRACT → Cola de extracción → TRANSFORM de aeropuertos → ¿Registro válido? → Lista de válidos: 7698 → Pila de errores: 0 → Totales de aeropuertos: 7698 procesados, 7698 v… → ….
