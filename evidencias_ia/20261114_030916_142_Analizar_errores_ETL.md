# Evidencia de IA: Analizar errores ETL

- Fecha y hora: 2026-11-14 03:09:16
- Proveedor: Gemini
- Modelo: (no indicado)
- Se usó el respaldo local: sí
- Notas: La API no respondió bien (no hay clave de API). Se usó el respaldo local.

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
(sin respuesta de la API)
````

## Resultado final

Respaldo local (sin IA). Datos reales usados:

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

