package etl;

import algoritmos.Busqueda;
import algoritmos.Comparadores;
import algoritmos.Ordenamiento;
import estructuras.ArbolBST;
import estructuras.ListaSecuencial;
import estructuras.ResultadoBusqueda;
import model.Aerolinea;
import model.Aeropuerto;
import model.Ruta;

public class DatosLimpios {

    private final ListaSecuencial<Aeropuerto> aeropuertos = new ListaSecuencial<>();
    private final ListaSecuencial<Ruta> rutas = new ListaSecuencial<>();
    private final ListaSecuencial<Aerolinea> aerolineas = new ListaSecuencial<>();
    private final ArbolBST<Aeropuerto> arbolIata = new ArbolBST<>(Comparadores.AEROPUERTO_POR_IATA);
    private ListaSecuencial<Aeropuerto> aeropuertosPorId = new ListaSecuencial<>();

    public DatosLimpios() {
        arbolIata.setEtiquetador(Aeropuerto::getIata);
    }

    public void agregarAeropuerto(Aeropuerto aeropuerto) {
        aeropuertos.insertarFinal(aeropuerto);
        if (tieneIata(aeropuerto)) {
            arbolIata.insertar(aeropuerto);
        }
    }

    public void agregarRuta(Ruta ruta) {
        rutas.insertarFinal(ruta);
    }

    public void agregarAerolinea(Aerolinea aerolinea) {
        aerolineas.insertarFinal(aerolinea);
    }

    public void indexarAeropuertosPorId() {
        aeropuertosPorId = Ordenamiento.insercion(aeropuertos, Comparadores.AEROPUERTO_POR_ID).getLista();
    }

    public Aeropuerto buscarPorId(String id) {
        if (!Transform.esEntero(id)) {
            return null;
        }
        ResultadoBusqueda<Aeropuerto> resultado = Busqueda.binaria(aeropuertosPorId, Aeropuerto.conId(id.trim()),
                Comparadores.AEROPUERTO_POR_ID);
        return resultado.getElemento();
    }

    public Aeropuerto buscarPorIata(String iata) {
        if (!Transform.esIataValido(iata)) {
            return null;
        }
        return arbolIata.buscar(Aeropuerto.conIata(iata)).getElemento();
    }

    public static boolean tieneIata(Aeropuerto aeropuerto) {
        return Transform.esIataValido(aeropuerto.getIata());
    }

    public ListaSecuencial<Aeropuerto> getAeropuertos() {
        return aeropuertos;
    }

    public ListaSecuencial<Aeropuerto> getAeropuertosPorId() {
        return aeropuertosPorId;
    }

    public ListaSecuencial<Ruta> getRutas() {
        return rutas;
    }

    public ListaSecuencial<Aerolinea> getAerolineas() {
        return aerolineas;
    }

    public ArbolBST<Aeropuerto> getArbolIata() {
        return arbolIata;
    }
}
