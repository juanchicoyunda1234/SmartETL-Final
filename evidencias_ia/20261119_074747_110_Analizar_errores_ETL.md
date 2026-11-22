# Evidencia de IA: Analizar errores ETL

- Fecha y hora: 2026-11-19 07:47:47
- Proveedor: Gemini
- Modelo: gemini-3.5-flash-lite
- Se usó el respaldo local: no
- Notas: Respuesta de Gemini (gemini-3.5-flash-lite).

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
¡Hola, muchachos! Bienvenidos a su tercer semestre. Analicemos el reporte ETL de OpenFlights para evaluar la calidad de los datos reales que procesamos. Vemos que las entidades de aeropuertos y aerolíneas tienen una calidad impecable, con 7698 y 6162 registros válidos respectivamente, y cero errores. Esto significa que la estructura base está limpia y no presenta nulos obligatorios ni duplicados en sus identificadores principales.

Sin embargo, el problema crítico se concentra en la tabla de rutas, donde de 67663 elementos procesados, 589 resultaron inválidos. Al revisar los tipos de errores, notamos que no hay fallas de formato ni de rangos; todos los problemas provienen de la integridad referencial y de identificadores nulos. Específicamente, 473 errores corresponden a `REFERENCIA_ROTA` y 116 a `ID_NULO_NO_RECUPERABLE`. 

La regla `REFERENCIA_ROTA` detecta estos 473 casos porque el ID de aeropuerto asignado a una ruta simplemente no existe en el conjunto de aeropuertos, lo que destruye la relación relacional que estudiamos en clase. Por otro lado, la regla `ID_NULO_NO_RECUPERABLE` señala 116 registros donde el identificador original era "\N" y, al intentar buscar su código IATA en nuestro árbol binario de búsqueda (BST), este tampoco se encontró; a diferencia de los nulos recuperables, estos no pudieron ser rescatados automáticamente y debieron descartarse.
````

## Resultado final

¡Hola, muchachos! Bienvenidos a su tercer semestre. Analicemos el reporte ETL de OpenFlights para evaluar la calidad de los datos reales que procesamos. Vemos que las entidades de aeropuertos y aerolíneas tienen una calidad impecable, con 7698 y 6162 registros válidos respectivamente, y cero errores. Esto significa que la estructura base está limpia y no presenta nulos obligatorios ni duplicados en sus identificadores principales.

Sin embargo, el problema crítico se concentra en la tabla de rutas, donde de 67663 elementos procesados, 589 resultaron inválidos. Al revisar los tipos de errores, notamos que no hay fallas de formato ni de rangos; todos los problemas provienen de la integridad referencial y de identificadores nulos. Específicamente, 473 errores corresponden a `REFERENCIA_ROTA` y 116 a `ID_NULO_NO_RECUPERABLE`. 

La regla `REFERENCIA_ROTA` detecta estos 473 casos porque el ID de aeropuerto asignado a una ruta simplemente no existe en el conjunto de aeropuertos, lo que destruye la relación relacional que estudiamos en clase. Por otro lado, la regla `ID_NULO_NO_RECUPERABLE` señala 116 registros donde el identificador original era "\N" y, al intentar buscar su código IATA en nuestro árbol binario de búsqueda (BST), este tampoco se encontró; a diferencia de los nulos recuperables, estos no pudieron ser rescatados automáticamente y debieron descartarse.
