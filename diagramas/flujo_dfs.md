# Flujograma dinámico: DFS desde UIO (detalle hasta nivel 1)

Generado por IA desde el registro de eventos

```mermaid
flowchart TD
    A(["Inicio: DFS desde UIO (solo rutas internas)"]) --> B["Apilar UIO<br/>estado: [UIO]"]
    B --> C["Paso 1: desapilar UIO<br/>vecinos apilados: XMS, TUA, MEC, LOH, ETR, ESM, CUE, OCC, GYE<br/>estado: [GYE, OCC, CUE, ESM, ETR, LOH, MEC, TUA, XMS]"]
    C --> D["Paso 2: desapilar GYE<br/>vecinos apilados: LTX, LOH, GPS, ESM, CUE, SCY<br/>estado: [SCY, CUE, ESM, GPS, LOH, LTX, OCC, CUE, ESM, ETR, LOH, MEC y 2 más]"]
    D --> E["Nivel 2: 7 aeropuertos nuevos en 1 país"]
    E --> F["Nivel 3: 1 aeropuertos nuevos en 1 país"]
    F --> G["Pasos 10 a 12: desapilar ETR, MEC, TUA<br/>ninguno aporta vecinos nuevos"]
    G --> H["Paso 13: desapilar XMS<br/>vecinos apilados: TNW<br/>estado: [TNW]"]
    H --> I{"¿Pila vacía?"}
    I -->|"Sí"| J(["Fin: 14 aeropuertos visitados"])
```

Para verlo con zoom abrir `ver_flujo_dfs.html`.
