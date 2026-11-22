package grafo;

import estructuras.ArbolBST;
import estructuras.ListaSecuencial;
import etl.DatosLimpios;
import model.Aeropuerto;
import model.Ruta;

public class MapaPaises {

    private final DatosLimpios datos;
    private final ArbolBST<GrafoPais> arbol = new ArbolBST<>(GrafoPais.POR_NOMBRE);
    private final ListaSecuencial<GrafoPais> paises = new ListaSecuencial<>();
    private final Aeropuerto[] origenes;
    private final Aeropuerto[] destinos;
    private final Vertice<Aeropuerto>[] verticePorId;

    @SuppressWarnings("unchecked")
    public MapaPaises(DatosLimpios datos) {
        this.datos = datos;
        arbol.setEtiquetador(GrafoPais::getNombre);
        int idMaximo = 0;
        ListaSecuencial<Aeropuerto> aeropuertos = datos.getAeropuertos();
        for (int i = 0; i < aeropuertos.tamanio(); i++) {
            Aeropuerto aeropuerto = aeropuertos.obtener(i);
            idMaximo = Math.max(idMaximo, Integer.parseInt(aeropuerto.getId()));
            if (arbol.insertar(GrafoPais.clave(aeropuerto.getPais()))) {
                paises.insertarFinal(arbol.buscar(GrafoPais.clave(aeropuerto.getPais())).getElemento());
            }
        }
        verticePorId = (Vertice<Aeropuerto>[]) new Vertice[idMaximo + 1];
        ListaSecuencial<Ruta> rutas = datos.getRutas();
        origenes = new Aeropuerto[rutas.tamanio()];
        destinos = new Aeropuerto[rutas.tamanio()];
        for (int i = 0; i < rutas.tamanio(); i++) {
            origenes[i] = datos.buscarPorId(rutas.obtener(i).getIdOrigen());
            destinos[i] = datos.buscarPorId(rutas.obtener(i).getIdDestino());
        }
    }

    public GrafoPais obtener(String pais) {
        GrafoPais grafoPais = arbol.buscar(GrafoPais.clave(pais)).getElemento();
        if (grafoPais != null && !grafoPais.estaConstruido()) {
            construir(grafoPais);
        }
        return grafoPais;
    }

    public boolean existe(String pais) {
        return arbol.buscar(GrafoPais.clave(pais)).encontrado();
    }

    public double construirTodos() {
        long inicio = System.nanoTime();
        for (int i = 0; i < paises.tamanio(); i++) {
            obtener(paises.obtener(i).getNombre());
        }
        return (System.nanoTime() - inicio) / 1_000_000.0;
    }

    public int paisesConVertices() {
        return paises.contar(p -> p.estaConstruido() && p.getVertices() > 0);
    }

    private void construir(GrafoPais grafoPais) {
        long inicio = System.nanoTime();
        String nombre = grafoPais.getNombre();
        for (int i = 0; i < origenes.length; i++) {
            Aeropuerto origen = origenes[i];
            Aeropuerto destino = destinos[i];
            boolean origenDentro = origen.getPais().equalsIgnoreCase(nombre);
            boolean destinoDentro = destino.getPais().equalsIgnoreCase(nombre);
            if (origenDentro && destinoDentro) {
                grafoPais.getGrafo().agregarArista(asegurarVertice(grafoPais, origen), asegurarVertice(grafoPais, destino));
            } else if (origenDentro) {
                if (asegurarVertice(grafoPais, origen).agregarExterno(destino)) {
                    grafoPais.contarSalida();
                }
            } else if (destinoDentro) {
                if (asegurarVertice(grafoPais, destino).agregarLlegadaExterna(origen)) {
                    grafoPais.contarLlegada();
                }
            }
        }
        grafoPais.marcarConstruido((System.nanoTime() - inicio) / 1_000_000.0);
    }

    private Vertice<Aeropuerto> asegurarVertice(GrafoPais grafoPais, Aeropuerto aeropuerto) {
        int id = Integer.parseInt(aeropuerto.getId());
        if (verticePorId[id] == null) {
            verticePorId[id] = grafoPais.getGrafo().agregarVertice(aeropuerto, etiqueta(aeropuerto));
        }
        return verticePorId[id];
    }

    public static String etiqueta(Aeropuerto aeropuerto) {
        if (DatosLimpios.tieneIata(aeropuerto)) {
            return aeropuerto.getIata();
        }
        if (!aeropuerto.getIcao().equals("Sin dato")) {
            return aeropuerto.getIcao();
        }
        return "ID" + aeropuerto.getId();
    }

    public Vertice<Aeropuerto> vertice(Aeropuerto aeropuerto) {
        if (aeropuerto == null) {
            return null;
        }
        obtener(aeropuerto.getPais());
        return verticePorId[Integer.parseInt(aeropuerto.getId())];
    }

    public Vertice<Aeropuerto> verticePorIata(String iata) {
        return vertice(datos.buscarPorIata(iata.trim().toUpperCase()));
    }

    public ArbolBST<GrafoPais> getArbol() {
        return arbol;
    }

    public ListaSecuencial<GrafoPais> getPaises() {
        return paises;
    }

    public ListaSecuencial<GrafoPais> construidos() {
        ListaSecuencial<GrafoPais> lista = new ListaSecuencial<>();
        for (int i = 0; i < paises.tamanio(); i++) {
            if (paises.obtener(i).estaConstruido()) {
                lista.insertarFinal(paises.obtener(i));
            }
        }
        return lista;
    }

    public DatosLimpios getDatos() {
        return datos;
    }
}
