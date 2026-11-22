package algoritmos;

import model.Aeropuerto;

import java.util.Comparator;

public final class Comparadores {

    public static final Comparator<Aeropuerto> AEROPUERTO_POR_ID =
            (a, b) -> Integer.compare(Integer.parseInt(a.getId()), Integer.parseInt(b.getId()));

    public static final Comparator<Aeropuerto> AEROPUERTO_POR_IATA = (a, b) -> a.getIata().compareTo(b.getIata());

    public static final Comparator<Aeropuerto> AEROPUERTO_POR_NOMBRE = (a, b) -> a.getNombre().compareTo(b.getNombre());

    public static final Comparator<Aeropuerto> AEROPUERTO_POR_PAIS = (a, b) -> a.getPais().compareTo(b.getPais());

    private Comparadores() {
    }

    public static Comparator<Aeropuerto> porCampo(String campo) {
        switch (campo) {
            case "Nombre":
                return AEROPUERTO_POR_NOMBRE;
            case "País":
                return AEROPUERTO_POR_PAIS;
            default:
                return AEROPUERTO_POR_IATA;
        }
    }
}
