# Evidencia de IA: Analizar errores ETL

- Fecha y hora: 2026-11-07 11:59:28
- Proveedor: Gemini
- Modelo: gemini-3.6-flash
- Se usó el respaldo local: no
- Notas: Respuesta de Gemini (gemini-3.6-flash).

## Prompt enviado

````text
Eres profesor de Estructuras de Datos. Explica en español, en 2 a 4 párrafos cortos y claros para estudiantes de tercer semestre, qué dicen estos errores del ETL sobre la calidad de los datos y por qué cada regla los detecta.
Usa solo los datos reales que siguen; no inventes cifras ni aeropuertos. No uses tablas.

Datos reales:
Conteos agregados del ETL sobre OpenFlights (sin registros individuales).
aeropuertos: 7698 procesados, 7698 válidos, 0 inválidos
rutas: 67663 procesados, 67074 válidos, 589 inválidos
aerolíneas: 6162 procesados, 6162 válidos, 0 inválidos
Errores por tipo:
  CAMPO_OBLIGATORIO: 0
  ID_INVALIDO: 0
  RANGO_INVALIDO: 0
  DUPLICADO: 0
  REFERENCIA_ROTA: 473
  ID_NULO_NO_RECUPERABLE: 116
Reglas: REFERENCIA_ROTA = el ID de aeropuerto de la ruta no existe en airports; ID_NULO_NO_RECUPERABLE = el ID era \N y el código del aeropuerto no está en el BST por IATA; los IDs nulos recuperables se resuelven buscando el código IATA en el BST.

````

## Respuesta cruda

````text
Estimados estudiantes, al analizar los resultados del proceso ETL sobre la base de datos de OpenFlights, vemos que la calidad de los datos es perfecta en los catálogos principales: de 7698 aeropuertos y 6162 aerolíneas procesados, todos resultaron válidos (0 inválidos). El problema de calidad se concentra únicamente en el conjunto de rutas, donde de 67663 procesadas, 67074 son válidas y 589 son inválidas. Noten también que no hay errores por campos obligatorios, IDs inválidos, rangos inválidos ni duplicados (todos marcan 0).

El error más frecuente es `REFERENCIA_ROTA`, con 473 casos. Esta regla se activa porque la ruta incluye un ID de aeropuerto que no existe en el catálogo de aeropuertos cargado. Desde el punto de vista de las estructuras de datos y la integridad referencial, esto equivale a intentar hacer una búsqueda o conectar un nodo hacia una referencia inexistente en memoria o en nuestra tabla hash/diccionario.

El segundo fallo es `ID_NULO_NO_RECUPERABLE`, con 116 casos. Esta regla detecta registros donde el ID del aeropuerto venía como `\N` (nulo) y el ETL intentó reparar el dato buscando el código IATA correspondiente dentro de un árbol de búsqueda binaria (BST). Como dicho código IATA tampoco se encontró en el BST, el algoritmo no pudo resolver el ID nulo. Esto nos demuestra cómo las estructuras de datos eficientes nos ayudan a recuperar información, pero si los atributos clave faltan en la fuente, el registro se clasifica como no recuperable.
````

## Resultado final

Estimados estudiantes, al analizar los resultados del proceso ETL sobre la base de datos de OpenFlights, vemos que la calidad de los datos es perfecta en los catálogos principales: de 7698 aeropuertos y 6162 aerolíneas procesados, todos resultaron válidos (0 inválidos). El problema de calidad se concentra únicamente en el conjunto de rutas, donde de 67663 procesadas, 67074 son válidas y 589 son inválidas. Noten también que no hay errores por campos obligatorios, IDs inválidos, rangos inválidos ni duplicados (todos marcan 0).

El error más frecuente es `REFERENCIA_ROTA`, con 473 casos. Esta regla se activa porque la ruta incluye un ID de aeropuerto que no existe en el catálogo de aeropuertos cargado. Desde el punto de vista de las estructuras de datos y la integridad referencial, esto equivale a intentar hacer una búsqueda o conectar un nodo hacia una referencia inexistente en memoria o en nuestra tabla hash/diccionario.

El segundo fallo es `ID_NULO_NO_RECUPERABLE`, con 116 casos. Esta regla detecta registros donde el ID del aeropuerto venía como `\N` (nulo) y el ETL intentó reparar el dato buscando el código IATA correspondiente dentro de un árbol de búsqueda binaria (BST). Como dicho código IATA tampoco se encontró en el BST, el algoritmo no pudo resolver el ID nulo. Esto nos demuestra cómo las estructuras de datos eficientes nos ayudan a recuperar información, pero si los atributos clave faltan en la fuente, el registro se clasifica como no recuperable.
