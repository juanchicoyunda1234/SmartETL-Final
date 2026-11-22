package app.web;

import app.Sesion;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class ServidorWeb {

    public static final int PUERTO_INICIAL = 7070;
    private static final int INTENTOS = 10;

    private final HttpServer servidor;
    private final int puerto;

    private ServidorWeb(HttpServer servidor, int puerto) {
        this.servidor = servidor;
        this.puerto = puerto;
    }

    public static ServidorWeb iniciar(Sesion sesion) throws IOException {
        InetAddress local = InetAddress.getByName("127.0.0.1");
        for (int puerto = PUERTO_INICIAL; puerto < PUERTO_INICIAL + INTENTOS; puerto++) {
            try {
                HttpServer servidor = HttpServer.create(new InetSocketAddress(local, puerto), 0);
                if (puerto != PUERTO_INICIAL) {
                    System.out.println("El puerto " + PUERTO_INICIAL + " está ocupado; se usa el " + puerto + ".");
                }
                configurar(servidor, sesion, puerto);
                return new ServidorWeb(servidor, puerto);
            } catch (BindException ocupado) {
            }
        }
        throw new IOException("No hay puertos libres entre " + PUERTO_INICIAL + " y " + (PUERTO_INICIAL + INTENTOS - 1));
    }

    private static void configurar(HttpServer servidor, Sesion sesion, int puerto) {
        ContextoWeb contexto = new ContextoWeb(sesion);
        contexto.setPuerto(puerto);
        Seguridad.setPuerto(puerto);
        ApiIa ia = new ApiIa(contexto);
        servidor.createContext("/api/", new Api(new ApiGeneral(contexto), new ApiDatos(contexto), new ApiEtl(contexto),
                new ApiEstructuras(contexto), new ApiGrafo(contexto), ia, new ApiApis(contexto, ia)));
        servidor.createContext("/", new Estaticos());
        servidor.setExecutor(Executors.newCachedThreadPool());
        servidor.start();
    }

    public String getDireccion() {
        return "http://127.0.0.1:" + puerto + "/";
    }

    public int getPuerto() {
        return puerto;
    }

    public void detener() {
        servidor.stop(0);
    }
}
