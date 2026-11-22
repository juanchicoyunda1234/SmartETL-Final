# Evidencia de IA: Explicar grafo de Ecuador

- Fecha y hora: 2026-11-14 03:08:43
- Proveedor: Gemini
- Modelo: (no indicado)
- Se usó el respaldo local: sí
- Notas: La API no respondió bien (no hay clave de API). Se usó el respaldo local.

## Prompt enviado

````text
Eres profesor de Estructuras de Datos. Explica en español, en 2 a 4 párrafos cortos y claros para estudiantes de tercer semestre, cómo está formado el grafo de rutas aéreas de Ecuador, qué significan sus vértices, aristas internas y rutas externas.
Usa solo los datos reales que siguen; no inventes cifras ni aeropuertos. No uses tablas.

Datos reales:
Grafo dirigido de aeropuertos de Ecuador (vértices = aeropuertos con al menos una ruta válida, aristas = rutas únicas origen-destino).
Ecuador: 14 aeropuertos, 32 rutas internas, 26 salen al exterior, 27 llegan del exterior
Aeropuertos con más rutas internas de salida:
  UIO (Quito): 9 internas, 13 al exterior
  GYE (Guayaquil): 7 internas, 12 al exterior
  OCC (Coca): 2 internas, 0 al exterior
  ESM (Esmeraldas): 2 internas, 1 al exterior
  CUE (Cuenca): 2 internas, 0 al exterior
Rutas externas agrupadas por país destino:
  Colombia: 6 rutas
  United States: 5 rutas
  Netherlands: 1 rutas
  Peru: 2 rutas
  El Salvador: 2 rutas
  Panama: 2 rutas
  Spain: 2 rutas
  Venezuela: 1 rutas
  Argentina: 1 rutas
  Chile: 1 rutas
  Mexico: 1 rutas
  Brazil: 1 rutas
  y 1 países más
Ciclo encontrado con DFS: GYE -> UIO -> OCC -> LTX -> GYE

````

## Respuesta cruda

````text
(sin respuesta de la API)
````

## Resultado final

Respaldo local (sin IA). Datos reales usados:

Grafo dirigido de aeropuertos de Ecuador (vértices = aeropuertos con al menos una ruta válida, aristas = rutas únicas origen-destino).
Ecuador: 14 aeropuertos, 32 rutas internas, 26 salen al exterior, 27 llegan del exterior
Aeropuertos con más rutas internas de salida:
  UIO (Quito): 9 internas, 13 al exterior
  GYE (Guayaquil): 7 internas, 12 al exterior
  OCC (Coca): 2 internas, 0 al exterior
  ESM (Esmeraldas): 2 internas, 1 al exterior
  CUE (Cuenca): 2 internas, 0 al exterior
Rutas externas agrupadas por país destino:
  Colombia: 6 rutas
  United States: 5 rutas
  Netherlands: 1 rutas
  Peru: 2 rutas
  El Salvador: 2 rutas
  Panama: 2 rutas
  Spain: 2 rutas
  Venezuela: 1 rutas
  Argentina: 1 rutas
  Chile: 1 rutas
  Mexico: 1 rutas
  Brazil: 1 rutas
  y 1 países más
Ciclo encontrado con DFS: GYE -> UIO -> OCC -> LTX -> GYE

