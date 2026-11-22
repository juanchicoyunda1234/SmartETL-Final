package ia;

import estructuras.ListaSecuencial;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;

public class AlmacenApis {

    public static final int CAMPOS = 5;

    private final Path archivo;
    private ListaSecuencial<ApiGuardada> apis = new ListaSecuencial<>();

    public AlmacenApis(Path archivo) throws IOException {
        this.archivo = archivo;
        cargar();
    }

    public static Path archivoPorDefecto() {
        return Paths.get(System.getProperty("user.home"), ".smartetl", "apis.properties");
    }

    public void cargar() throws IOException {
        if (!Files.exists(archivo)) {
            Files.createDirectories(archivo.getParent());
            Files.write(archivo, new byte[0]);
            protegerArchivo();
        }
        ListaSecuencial<ApiGuardada> leidas = new ListaSecuencial<>();
        for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
            String[] campos = separar(linea);
            if (campos.length == CAMPOS && !campos[0].trim().isEmpty()) {
                leidas.insertarFinal(new ApiGuardada(campos[0], campos[1], campos[2], campos[3], campos[4]));
            }
        }
        apis = leidas;
    }

    public void guardar(ApiGuardada api) throws IOException {
        if (api.getNombre().isEmpty()) {
            throw new IllegalArgumentException("Escribe un nombre para la API");
        }
        int posicion = indice(api.getNombre());
        if (posicion >= 0) {
            apis.reemplazar(posicion, api);
        } else {
            apis.insertarFinal(api);
        }
        escribir();
    }

    public boolean eliminar(String nombre) throws IOException {
        int posicion = indice(nombre);
        if (posicion < 0) {
            return false;
        }
        ListaSecuencial<ApiGuardada> restantes = new ListaSecuencial<>();
        for (int i = 0; i < apis.tamanio(); i++) {
            if (i != posicion) {
                restantes.insertarFinal(apis.obtener(i));
            }
        }
        apis = restantes;
        escribir();
        return true;
    }

    public ApiGuardada buscar(String nombre) {
        int posicion = indice(nombre);
        return posicion < 0 ? null : apis.obtener(posicion);
    }

    public ListaSecuencial<ApiGuardada> getApis() {
        return apis;
    }

    public Path getArchivo() {
        return archivo;
    }

    private int indice(String nombre) {
        String buscado = nombre == null ? "" : nombre.trim();
        for (int i = 0; i < apis.tamanio(); i++) {
            if (apis.obtener(i).getNombre().equalsIgnoreCase(buscado)) {
                return i;
            }
        }
        return -1;
    }

    private void escribir() throws IOException {
        Files.createDirectories(archivo.getParent());
        try (BufferedWriter escritor = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
            for (int i = 0; i < apis.tamanio(); i++) {
                ApiGuardada api = apis.obtener(i);
                escritor.write(escapar(api.getNombre()) + "|" + escapar(api.getProveedor()) + "|" + escapar(api.getModelo())
                        + "|" + escapar(api.getUrl()) + "|" + escapar(api.getClave()));
                escritor.newLine();
            }
        }
        protegerArchivo();
    }

    private void protegerArchivo() {
        try {
            Files.setPosixFilePermissions(archivo, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException | IOException ignorada) {
        }
    }

    public static String escapar(String valor) {
        return valor.replace("\\", "\\\\").replace("|", "\\|").replace("\n", "\\n").replace("\r", "");
    }

    public static String[] separar(String linea) {
        ListaSecuencial<String> campos = new ListaSecuencial<>();
        StringBuilder actual = new StringBuilder();
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '\\' && i + 1 < linea.length()) {
                char siguiente = linea.charAt(i + 1);
                actual.append(siguiente == 'n' ? '\n' : siguiente);
                i++;
            } else if (c == '|') {
                campos.insertarFinal(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        campos.insertarFinal(actual.toString());
        String[] resultado = new String[campos.tamanio()];
        for (int i = 0; i < resultado.length; i++) {
            resultado[i] = campos.obtener(i);
        }
        return resultado;
    }
}
