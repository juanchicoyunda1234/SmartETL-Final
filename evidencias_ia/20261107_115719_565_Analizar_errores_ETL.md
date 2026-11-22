# Evidencia de IA: Analizar errores ETL

- Fecha y hora: 2026-11-07 11:57:19
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
Estimados estudiantes, al analizar el reporte del ETL observamos que la calidad de los datos es perfecta en las entidades de aeropuertos (7698 procesados y válidos) y aerolíneas (6162 procesados y válidos). La degradación de la calidad se concentra únicamente en las rutas, donde de 67663 procesadas, 67074 resultaron válidas y 589 fueron clasificadas como inválidas. Al tener 0 errores en tipos como CAMPO_OBLIGATORIO, ID_INVALIDO, RANGO_INVALIDO y DUPLICADO, la calidad de los datos indica que las fallas no son de formato o duplicidad, sino de vinculación e identificación.

El primer tipo de fallo es REFERENCIA_ROTA, con 473 casos. Esta regla detecta cuándo un registro de ruta contiene un ID de aeropuerto que no existe en el conjunto de datos de aeropuertos. En términos de calidad, esto demuestra que hay rutas registradas con referencias a orígenes o destinos faltantes en el catálogo principal, lo que quiebra la relación directa entre ambas entidades.

El segundo fallo es ID_NULO_NO_RECUPERABLE, con 116 casos. La regla se activa cuando el ID del aeropuerto en la ruta tiene el valor nulo \N y, además, el código del aeropuerto no se encuentra cargado en la estructura de árbol binario de búsqueda (BST) por IATA. Mientras que los IDs nulos recuperables logran salvarse buscando el código IATA en el BST, estos 116 registros no pudieron corregirse debido a que su código carecía de coincidencia en dicha estructura de datos, impidiendo rescatar la identidad del aeropuerto.
````

## Resultado final

Estimados estudiantes, al analizar el reporte del ETL observamos que la calidad de los datos es perfecta en las entidades de aeropuertos (7698 procesados y válidos) y aerolíneas (6162 procesados y válidos). La degradación de la calidad se concentra únicamente en las rutas, donde de 67663 procesadas, 67074 resultaron válidas y 589 fueron clasificadas como inválidas. Al tener 0 errores en tipos como CAMPO_OBLIGATORIO, ID_INVALIDO, RANGO_INVALIDO y DUPLICADO, la calidad de los datos indica que las fallas no son de formato o duplicidad, sino de vinculación e identificación.

El primer tipo de fallo es REFERENCIA_ROTA, con 473 casos. Esta regla detecta cuándo un registro de ruta contiene un ID de aeropuerto que no existe en el conjunto de datos de aeropuertos. En términos de calidad, esto demuestra que hay rutas registradas con referencias a orígenes o destinos faltantes en el catálogo principal, lo que quiebra la relación directa entre ambas entidades.

El segundo fallo es ID_NULO_NO_RECUPERABLE, con 116 casos. La regla se activa cuando el ID del aeropuerto en la ruta tiene el valor nulo \N y, además, el código del aeropuerto no se encuentra cargado en la estructura de árbol binario de búsqueda (BST) por IATA. Mientras que los IDs nulos recuperables logran salvarse buscando el código IATA en el BST, estos 116 registros no pudieron corregirse debido a que su código carecía de coincidencia en dicha estructura de datos, impidiendo rescatar la identidad del aeropuerto.
