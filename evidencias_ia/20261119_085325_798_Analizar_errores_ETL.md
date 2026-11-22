# Evidencia de IA: Analizar errores ETL

- Fecha y hora: 2026-11-19 08:53:25
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
Hola, muchachos. Analizando los conteos agregados del proceso ETL sobre OpenFlights para nuestras estructuras de datos, vemos que los bloques de aeropuertos y aerolíneas tienen una calidad impecable, con 7698 y 6162 registros válidos respectivamente y cero errores. Esto significa que la carga base de entidades maestras está completa y sin duplicados ni campos obligatorios faltantes.

Sin embargo, el bloque de rutas presenta problemas en la integración. De 67663 rutas procesadas, 589 resultaron inválidas, y todos estos errores se concentran en dos categorías: 473 casos de `REFERENCIA_ROTA` y 116 casos de `ID_NULO_NO_RECUPERABLE`. No hubo fallas de formato, rangos ni duplicados; el problema radica exclusivamente en cómo se relacionan las rutas con los aeropuertos.

La regla `REFERENCIA_ROTA` detecta esos 473 errores porque el identificador del aeropuerto asociado a la ruta simplemente no existe en la tabla de aeropuertos, lo que rompe la integridad referencial del grafo de vuelos. Por su parte, la regla `ID_NULO_NO_RECUPERABLE` identifica los 116 casos donde el ID venía como nulo (`\N`) y el algoritmo falló al buscar el código IATA correspondiente dentro del BST (Árbol Binario de Búsqueda). Esto último nos enseña que, aunque algunos IDs nulos sí pudieron rescatarse consultando el BST, estos 116 registros no tenían salvación porque su código tampoco habitaba en nuestra estructura en memoria.
````

## Resultado final

Hola, muchachos. Analizando los conteos agregados del proceso ETL sobre OpenFlights para nuestras estructuras de datos, vemos que los bloques de aeropuertos y aerolíneas tienen una calidad impecable, con 7698 y 6162 registros válidos respectivamente y cero errores. Esto significa que la carga base de entidades maestras está completa y sin duplicados ni campos obligatorios faltantes.

Sin embargo, el bloque de rutas presenta problemas en la integración. De 67663 rutas procesadas, 589 resultaron inválidas, y todos estos errores se concentran en dos categorías: 473 casos de `REFERENCIA_ROTA` y 116 casos de `ID_NULO_NO_RECUPERABLE`. No hubo fallas de formato, rangos ni duplicados; el problema radica exclusivamente en cómo se relacionan las rutas con los aeropuertos.

La regla `REFERENCIA_ROTA` detecta esos 473 errores porque el identificador del aeropuerto asociado a la ruta simplemente no existe en la tabla de aeropuertos, lo que rompe la integridad referencial del grafo de vuelos. Por su parte, la regla `ID_NULO_NO_RECUPERABLE` identifica los 116 casos donde el ID venía como nulo (`\N`) y el algoritmo falló al buscar el código IATA correspondiente dentro del BST (Árbol Binario de Búsqueda). Esto último nos enseña que, aunque algunos IDs nulos sí pudieron rescatarse consultando el BST, estos 116 registros no tenían salvación porque su código tampoco habitaba en nuestra estructura en memoria.
