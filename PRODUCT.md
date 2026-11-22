# SmartETL-DS: producto

## Qué es

Aplicación de escritorio académica para la materia Estructura de Datos (UTA, FISEI). Toma los datos reales y sucios de OpenFlights (7.698 aeropuertos, 67.663 rutas, 6.162 aerolíneas), los limpia con un ETL y los organiza con estructuras propias: listas, cola, pila, árbol BST y grafo. Una capa de IA genera flujogramas y explicaciones a partir de lo que el programa hizo de verdad.

La interfaz es una página web servida por el mismo programa Java (`127.0.0.1`, puerto 7070), que se abre sola en el navegador. La versión Swing anterior sigue disponible con `--swing`.

## Para quién

- **Primarios:** los 6 integrantes del grupo, que la usan para demostrar y defender el proyecto frente al docente.
- **Secundario:** el docente, que la ve proyectada y puede pedir que se ejecute, se explique o se modifique algo en vivo.

## Qué tiene que lograr

1. **Funcionar al primer intento**, sin internet (salvo las llamadas a la IA).
2. **Enseñar las estructuras:** cada resultado deja ver qué hizo la estructura (comparaciones, camino en el árbol, orden de visita), no solo la respuesta.
3. **Entenderse sin explicación:** íconos, estados vacíos con una acción clara y mensajes que dicen qué está pasando.
4. **Ser explicable:** cada animación y cada función JS se puede contar en una frase durante la defensa.
5. **Ser honesta con la IA:** siempre se ve si respondió la API o el respaldo local, si el diagrama es coherente con la ejecución, y el código Mermaid detrás de cada diagrama.

## Tono

Claro, directo, en español de Ecuador. Frases cortas y verbos de acción ("Ejecuta el ETL", "Construye el árbol"). Los números con separador de miles con punto y decimales con coma (7.698; 1,54 s).

## Lo que no es

- No es una herramienta de producción ni multiusuario: atiende a una persona en su propio computador.
- No reemplaza el backend: la web solo llama a las mismas clases que usaba Swing.
- No envía datos personales a la IA: solo datos públicos de OpenFlights o conteos.

## Restricciones

- Backend intacto (`model`, `estructuras`, `etl`, `algoritmos`, `grafo`, `ia`, `pruebas`).
- Solo el JDK (servidor `com.sun.net.httpserver`), compatible con JDK 11, sin Maven.
- Frontend en HTML, CSS y JavaScript sin compilación; librerías locales en `recursos/` con su licencia.
- Sin comentarios en el código.
- Las claves de API nunca llegan al navegador.
