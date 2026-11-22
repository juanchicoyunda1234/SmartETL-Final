package app;

import estructuras.ListaEnlazada;
import etl.DatosLimpios;
import etl.ProcesoETL;
import grafo.MapaPaises;
import grafo.Vertice;
import ia.AlmacenApis;
import ia.ConfiguracionIA;
import ia.RegistroEventos;
import model.Aeropuerto;

import java.io.IOException;

public class Sesion {

    private final ConfiguracionIA configuracion = new ConfiguracionIA();
    private AlmacenApis apisGuardadas;
    private String errorApis = "";
    private Runnable alCambiarApis = () -> {
    };
    private ProcesoETL etlReal;
    private ProcesoETL ultimoEtl;
    private MapaPaises mapa;
    private ListaEnlazada<Aeropuerto> listaEnlazada;
    private RegistroEventos ultimoBfs;
    private RegistroEventos ultimoDfs;
    private int nivelDiagramaBfs = 1;
    private int nivelDiagramaDfs = 1;
    private Vertice<Aeropuerto> ultimoOrigen;
    private String ultimoPais = "Ecuador";

    public Sesion() {
        try {
            apisGuardadas = new AlmacenApis(AlmacenApis.archivoPorDefecto());
        } catch (IOException ex) {
            errorApis = "No se pudo leer " + AlmacenApis.archivoPorDefecto() + ": " + ex.getMessage();
        }
    }

    public AlmacenApis getApisGuardadas() {
        return apisGuardadas;
    }

    public String getErrorApis() {
        return errorApis;
    }

    public void setAlCambiarApis(Runnable alCambiarApis) {
        this.alCambiarApis = alCambiarApis;
    }

    public void avisarCambioApis() {
        alCambiarApis.run();
    }

    public synchronized void registrarEtl(ProcesoETL etl, boolean esReal) {
        ultimoEtl = etl;
        if (esReal) {
            etlReal = etl;
            mapa = null;
            listaEnlazada = null;
        }
    }

    public synchronized DatosLimpios datosReales() throws IOException {
        if (etlReal == null) {
            ProcesoETL etl = ProcesoETL.real();
            etl.ejecutar();
            registrarEtl(etl, true);
        }
        return etlReal.getDatos();
    }

    public synchronized boolean tieneEtlReal() {
        return etlReal != null;
    }

    public synchronized boolean tieneEtl() {
        return ultimoEtl != null;
    }

    public synchronized boolean ultimoEtlEsReal() {
        return ultimoEtl != null && ultimoEtl == etlReal;
    }

    public synchronized ProcesoETL ultimoEtl() throws IOException {
        if (ultimoEtl == null) {
            datosReales();
        }
        return ultimoEtl;
    }

    public synchronized MapaPaises mapa() throws IOException {
        if (mapa == null) {
            mapa = new MapaPaises(datosReales());
        }
        return mapa;
    }

    public synchronized ListaEnlazada<Aeropuerto> listaEnlazada() throws IOException {
        if (listaEnlazada == null) {
            listaEnlazada = new ListaEnlazada<>();
            DatosLimpios datos = datosReales();
            for (int i = 0; i < datos.getAeropuertos().tamanio(); i++) {
                listaEnlazada.insertarFinal(datos.getAeropuertos().obtener(i));
            }
        }
        return listaEnlazada;
    }

    public synchronized void registrarBfs(RegistroEventos registro, int nivelDiagrama, Vertice<Aeropuerto> origen) {
        ultimoBfs = registro;
        nivelDiagramaBfs = nivelDiagrama;
        ultimoOrigen = origen;
        ultimoPais = origen.getDato().getPais();
    }

    public synchronized void registrarDfs(RegistroEventos registro, int nivelDiagrama, Vertice<Aeropuerto> origen) {
        ultimoDfs = registro;
        nivelDiagramaDfs = nivelDiagrama;
        ultimoOrigen = origen;
        ultimoPais = origen.getDato().getPais();
    }

    public synchronized void setUltimoPais(String pais) {
        ultimoPais = pais;
    }

    public ConfiguracionIA getConfiguracion() {
        return configuracion;
    }

    public synchronized RegistroEventos getUltimoBfs() {
        return ultimoBfs;
    }

    public synchronized RegistroEventos getUltimoDfs() {
        return ultimoDfs;
    }

    public synchronized int getNivelDiagramaBfs() {
        return nivelDiagramaBfs;
    }

    public synchronized int getNivelDiagramaDfs() {
        return nivelDiagramaDfs;
    }

    public synchronized Vertice<Aeropuerto> getUltimoOrigen() {
        return ultimoOrigen;
    }

    public synchronized String getUltimoPais() {
        return ultimoPais;
    }
}
