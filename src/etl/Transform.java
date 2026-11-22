package etl;

import estructuras.ArbolBST;
import estructuras.Cola;
import estructuras.Pila;
import ia.Evento;
import ia.RegistroEventos;
import model.Aerolinea;
import model.Aeropuerto;
import model.ErrorETL;
import model.Ruta;
import model.TipoError;

public class Transform {

    public static final String SIN_DATO = "Sin dato";
    public static final String NULO = "\\N";
    public static final int AEROPUERTOS = 0;
    public static final int RUTAS = 1;
    public static final int AEROLINEAS = 2;
    public static final String[] FUENTES = {"aeropuertos", "rutas", "aerolíneas"};

    private static final int DETALLE_VALIDOS = 3;
    private static final int DETALLE_ERRORES = 5;

    private final DatosLimpios datos = new DatosLimpios();
    private final Pila<ErrorETL> errores = new Pila<>();
    private final int[] procesados = new int[3];
    private final int[] validos = new int[3];
    private final int[][] erroresPorTipo = new int[3][TipoError.values().length];
    private final int[] detallesValidos = new int[3];
    private final int[] detallesErrores = new int[3];
    private final RegistroEventos registro;
    private double milisegundos;

    public Transform(RegistroEventos registro) {
        this.registro = registro;
    }

    public void transformarAeropuertos(Cola<Aeropuerto> cola) {
        long inicio = System.nanoTime();
        ArbolBST<Integer> ids = new ArbolBST<>(Integer::compare);
        int linea = 0;
        while (!cola.estaVacia()) {
            Aeropuerto crudo = cola.desencolar();
            linea++;
            procesados[AEROPUERTOS]++;
            ErrorETL error = validarAeropuerto(crudo, linea);
            if (error == null && !ids.insertar(Integer.parseInt(crudo.getId().trim()))) {
                error = new ErrorETL(FUENTES[AEROPUERTOS], linea, crudo.getId(), TipoError.DUPLICADO,
                        "El ID ya apareció antes (BST de IDs); se conserva el primero", crudo.getId());
            }
            if (error == null) {
                Aeropuerto limpio = limpiar(crudo);
                datos.agregarAeropuerto(limpio);
                registrarValido(AEROPUERTOS, linea, limpio.getId() + " " + limpio.getIata() + " " + limpio.getNombre());
            } else {
                registrarError(AEROPUERTOS, error);
            }
        }
        datos.indexarAeropuertosPorId();
        registrarTotales(AEROPUERTOS);
        milisegundos += (System.nanoTime() - inicio) / 1_000_000.0;
    }

    public void transformarRutas(Cola<Ruta> cola) {
        long inicio = System.nanoTime();
        int linea = 0;
        while (!cola.estaVacia()) {
            Ruta cruda = cola.desencolar();
            linea++;
            procesados[RUTAS]++;
            String identificador = cruda.getAerolinea() + " " + cruda.getOrigen() + "-" + cruda.getDestino();
            ErrorETL error = validarObligatoriosRuta(cruda, linea, identificador);
            String idOrigen = null;
            String idDestino = null;
            if (error == null) {
                idOrigen = resolverId(cruda.getOrigen(), cruda.getIdOrigen());
                error = errorDeExtremo("origen", cruda.getOrigen(), cruda.getIdOrigen(), idOrigen, linea, identificador);
            }
            if (error == null) {
                idDestino = resolverId(cruda.getDestino(), cruda.getIdDestino());
                error = errorDeExtremo("destino", cruda.getDestino(), cruda.getIdDestino(), idDestino, linea, identificador);
            }
            if (error == null) {
                Ruta limpia = new Ruta(cruda.getAerolinea().trim(), limpio(cruda.getIdAerolinea()), cruda.getOrigen().trim(),
                        idOrigen, cruda.getDestino().trim(), idDestino, cruda.getCodeshare().trim(),
                        limpio(cruda.getEscalas()), limpio(cruda.getEquipo()));
                datos.agregarRuta(limpia);
                registrarValido(RUTAS, linea, identificador + " (IDs " + idOrigen + " -> " + idDestino + ")");
            } else {
                registrarError(RUTAS, error);
            }
        }
        registrarTotales(RUTAS);
        milisegundos += (System.nanoTime() - inicio) / 1_000_000.0;
    }

    public void transformarAerolineas(Cola<Aerolinea> cola) {
        long inicio = System.nanoTime();
        ArbolBST<Integer> ids = new ArbolBST<>(Integer::compare);
        int linea = 0;
        while (!cola.estaVacia()) {
            Aerolinea cruda = cola.desencolar();
            linea++;
            procesados[AEROLINEAS]++;
            ErrorETL error = null;
            if (!esEntero(cruda.getId())) {
                error = new ErrorETL(FUENTES[AEROLINEAS], linea, cruda.getId(), TipoError.ID_INVALIDO,
                        "El ID no es numérico", cruda.getId());
            } else if (esVacio(cruda.getNombre())) {
                error = new ErrorETL(FUENTES[AEROLINEAS], linea, cruda.getId(), TipoError.CAMPO_OBLIGATORIO,
                        "Falta el nombre", cruda.getNombre());
            } else if (!ids.insertar(Integer.parseInt(cruda.getId().trim()))) {
                error = new ErrorETL(FUENTES[AEROLINEAS], linea, cruda.getId(), TipoError.DUPLICADO,
                        "El ID ya apareció antes; se conserva el primero", cruda.getId());
            }
            if (error == null) {
                Aerolinea limpia = new Aerolinea(cruda.getId().trim(), cruda.getNombre().trim(), limpio(cruda.getAlias()),
                        limpio(cruda.getIata()), limpio(cruda.getIcao()), limpio(cruda.getCallsign()),
                        limpio(cruda.getPais()), limpio(cruda.getActiva()));
                datos.agregarAerolinea(limpia);
                registrarValido(AEROLINEAS, linea, limpia.getId() + " " + limpia.getNombre());
            } else {
                registrarError(AEROLINEAS, error);
            }
        }
        registrarTotales(AEROLINEAS);
        milisegundos += (System.nanoTime() - inicio) / 1_000_000.0;
    }

    private ErrorETL validarAeropuerto(Aeropuerto a, int linea) {
        String fuente = FUENTES[AEROPUERTOS];
        if (!esEntero(a.getId())) {
            return new ErrorETL(fuente, linea, a.getId(), TipoError.ID_INVALIDO, "El ID no es numérico", a.getId());
        }
        if (esVacio(a.getNombre())) {
            return new ErrorETL(fuente, linea, a.getId(), TipoError.CAMPO_OBLIGATORIO, "Falta el nombre", a.getNombre());
        }
        if (esVacio(a.getPais())) {
            return new ErrorETL(fuente, linea, a.getId(), TipoError.CAMPO_OBLIGATORIO, "Falta el país", a.getPais());
        }
        if (!estaEnRango(a.getLatitud(), 90)) {
            return new ErrorETL(fuente, linea, a.getId(), TipoError.RANGO_INVALIDO,
                    "La latitud debe estar entre -90 y 90", a.getLatitud());
        }
        if (!estaEnRango(a.getLongitud(), 180)) {
            return new ErrorETL(fuente, linea, a.getId(), TipoError.RANGO_INVALIDO,
                    "La longitud debe estar entre -180 y 180", a.getLongitud());
        }
        return null;
    }

    private Aeropuerto limpiar(Aeropuerto a) {
        String iata = esIataValido(a.getIata()) ? a.getIata().trim() : SIN_DATO;
        return new Aeropuerto(a.getId().trim(), a.getNombre().trim(), limpio(a.getCiudad()), a.getPais().trim(), iata,
                limpio(a.getIcao()), a.getLatitud().trim(), a.getLongitud().trim(), limpio(a.getAltitud()),
                limpio(a.getZonaUtc()), limpio(a.getDst()), limpio(a.getZonaHoraria()), limpio(a.getTipo()),
                limpio(a.getFuente()));
    }

    private ErrorETL validarObligatoriosRuta(Ruta r, int linea, String identificador) {
        String fuente = FUENTES[RUTAS];
        if (esVacio(r.getAerolinea())) {
            return new ErrorETL(fuente, linea, identificador, TipoError.CAMPO_OBLIGATORIO, "Falta la aerolínea", r.getAerolinea());
        }
        if (esVacio(r.getOrigen())) {
            return new ErrorETL(fuente, linea, identificador, TipoError.CAMPO_OBLIGATORIO, "Falta el origen", r.getOrigen());
        }
        if (esVacio(r.getDestino())) {
            return new ErrorETL(fuente, linea, identificador, TipoError.CAMPO_OBLIGATORIO, "Falta el destino", r.getDestino());
        }
        return null;
    }

    private String resolverId(String codigo, String id) {
        Aeropuerto encontrado = esNulo(id) ? datos.buscarPorIata(codigo.trim()) : datos.buscarPorId(id);
        return encontrado == null ? null : encontrado.getId();
    }

    private ErrorETL errorDeExtremo(String extremo, String codigo, String idOriginal, String idResuelto, int linea,
                                    String identificador) {
        if (idResuelto != null) {
            return null;
        }
        if (esNulo(idOriginal)) {
            return new ErrorETL(FUENTES[RUTAS], linea, identificador, TipoError.ID_NULO_NO_RECUPERABLE,
                    "El ID de " + extremo + " es \\N y el código " + codigo + " no está en el BST por IATA", idOriginal);
        }
        return new ErrorETL(FUENTES[RUTAS], linea, identificador, TipoError.REFERENCIA_ROTA,
                "El ID de " + extremo + " " + idOriginal + " no existe en airports", idOriginal);
    }

    private void registrarValido(int fuente, int linea, String descripcion) {
        validos[fuente]++;
        if (registro != null && detallesValidos[fuente] < DETALLE_VALIDOS) {
            detallesValidos[fuente]++;
            registro.agregar(new Evento("valido", "Desencolar línea " + linea + " de " + FUENTES[fuente]
                    + ": válido, pasa a la lista (" + descripcion + ")", String.valueOf(linea), FUENTES[fuente],
                    new String[0], new String[0], 0, fuente));
        }
    }

    private void registrarError(int fuente, ErrorETL error) {
        errores.apilar(error);
        erroresPorTipo[fuente][error.getTipo().ordinal()]++;
        if (registro != null && detallesErrores[fuente] < DETALLE_ERRORES) {
            detallesErrores[fuente]++;
            registro.agregar(new Evento("invalido", "Desencolar línea " + error.getLinea() + " de " + FUENTES[fuente]
                    + ": inválido, " + error.getTipo() + " (" + error.getMotivo() + "), se apila en la pila de errores",
                    error.getTipo().name(), FUENTES[fuente], new String[0], new String[0], errores.tamanio(), fuente));
        }
    }

    private void registrarTotales(int fuente) {
        if (registro == null) {
            return;
        }
        StringBuilder tipos = new StringBuilder();
        for (TipoError tipo : TipoError.values()) {
            int cantidad = erroresPorTipo[fuente][tipo.ordinal()];
            if (cantidad > 0) {
                tipos.append(tipos.length() == 0 ? "" : ", ").append(tipo).append(" ").append(cantidad);
            }
        }
        registro.agregar(new Evento("totales", "Transform de " + FUENTES[fuente] + ": " + procesados[fuente]
                + " procesados, " + validos[fuente] + " válidos, " + getInvalidos(fuente) + " inválidos"
                + (tipos.length() == 0 ? "" : " (" + tipos + ")"), FUENTES[fuente], FUENTES[fuente],
                new String[]{String.valueOf(procesados[fuente]), String.valueOf(validos[fuente]),
                        String.valueOf(getInvalidos(fuente)), tipos.toString()},
                new String[0], errores.tamanio(), fuente));
    }

    public static boolean esNulo(String valor) {
        return valor == null || valor.trim().equals(NULO);
    }

    public static boolean esVacio(String valor) {
        return esNulo(valor) || valor.trim().isEmpty();
    }

    public static String limpio(String valor) {
        return esVacio(valor) ? SIN_DATO : valor.trim();
    }

    public static boolean esEntero(String valor) {
        if (valor == null) {
            return false;
        }
        String texto = valor.trim();
        if (texto.isEmpty() || texto.length() > 9) {
            return false;
        }
        int inicio = texto.charAt(0) == '-' ? 1 : 0;
        if (inicio == texto.length()) {
            return false;
        }
        for (int i = inicio; i < texto.length(); i++) {
            if (!Character.isDigit(texto.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean esIataValido(String valor) {
        if (valor == null || valor.length() != 3) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            char c = valor.charAt(i);
            if (c < 'A' || c > 'Z') {
                return false;
            }
        }
        return true;
    }

    public static boolean estaEnRango(String valor, double limite) {
        try {
            double numero = Double.parseDouble(valor.trim());
            return numero >= -limite && numero <= limite;
        } catch (NumberFormatException | NullPointerException ex) {
            return false;
        }
    }

    public DatosLimpios getDatos() {
        return datos;
    }

    public Pila<ErrorETL> getErrores() {
        return errores;
    }

    public int getProcesados(int fuente) {
        return procesados[fuente];
    }

    public int getValidos(int fuente) {
        return validos[fuente];
    }

    public int getInvalidos(int fuente) {
        return procesados[fuente] - validos[fuente];
    }

    public int getErrores(int fuente, TipoError tipo) {
        return erroresPorTipo[fuente][tipo.ordinal()];
    }

    public int getErroresDeTipo(TipoError tipo) {
        int total = 0;
        for (int fuente = 0; fuente < FUENTES.length; fuente++) {
            total += erroresPorTipo[fuente][tipo.ordinal()];
        }
        return total;
    }

    public double getMilisegundos() {
        return milisegundos;
    }
}
