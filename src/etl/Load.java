package etl;

import estructuras.ListaSecuencial;
import model.ErrorETL;
import model.Registro;
import model.TipoError;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Load {

    private Load() {
    }

    public static String[] escribir(DatosLimpios datos, Transform transform, double msExtract, double msTransform,
                                    String carpeta) throws IOException {
        long inicio = System.nanoTime();
        Path destino = Paths.get(carpeta);
        Files.createDirectories(destino);
        String[] archivos = {
                escribirRegistros(destino.resolve("airports_limpios.csv"), datos.getAeropuertos()),
                escribirRegistros(destino.resolve("routes_limpias.csv"), datos.getRutas()),
                escribirRegistros(destino.resolve("airlines_limpias.csv"), datos.getAerolineas()),
                escribirErrores(destino.resolve("errores_etl.txt"), transform.getErrores().elementos()),
                null
        };
        double msLoad = (System.nanoTime() - inicio) / 1_000_000.0;
        archivos[4] = escribirReporte(destino.resolve("reporte_etl.txt"), transform, msExtract, msTransform, msLoad);
        return archivos;
    }

    private static <T extends Registro> String escribirRegistros(Path archivo, ListaSecuencial<T> registros)
            throws IOException {
        try (BufferedWriter escritor = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
            for (int i = 0; i < registros.tamanio(); i++) {
                escritor.write(lineaCsv(registros.obtener(i)));
                escritor.newLine();
            }
        }
        return archivo.toString();
    }

    public static String lineaCsv(Registro registro) {
        StringBuilder linea = new StringBuilder();
        for (int c = 0; c < registro.numeroCampos(); c++) {
            if (c > 0) {
                linea.append(',');
            }
            linea.append('"').append(registro.valor(c).replace("\"", "\"\"")).append('"');
        }
        return linea.toString();
    }

    private static String escribirErrores(Path archivo, ListaSecuencial<ErrorETL> errores) throws IOException {
        try (BufferedWriter escritor = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
            escritor.write("Pila de errores del ETL, de la cima (más reciente) al fondo. Total: " + errores.tamanio());
            escritor.newLine();
            for (int i = 0; i < errores.tamanio(); i++) {
                escritor.write(errores.obtener(i).comoLinea());
                escritor.newLine();
            }
        }
        return archivo.toString();
    }

    private static String escribirReporte(Path archivo, Transform transform, double msExtract, double msTransform,
                                          double msLoad) throws IOException {
        try (BufferedWriter escritor = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
            escritor.write(textoReporte(transform, msExtract, msTransform, msLoad));
        }
        return archivo.toString();
    }

    public static String textoReporte(Transform transform, double msExtract, double msTransform, double msLoad) {
        StringBuilder texto = new StringBuilder("REPORTE DEL ETL - SmartETL-DS\n\n");
        texto.append(String.format("%-12s %10s %10s %10s%n", "Fuente", "Procesados", "Válidos", "Inválidos"));
        for (int f = 0; f < Transform.FUENTES.length; f++) {
            texto.append(String.format("%-12s %10d %10d %10d%n", Transform.FUENTES[f], transform.getProcesados(f),
                    transform.getValidos(f), transform.getInvalidos(f)));
        }
        texto.append("\nErrores por tipo y fuente\n");
        for (TipoError tipo : TipoError.values()) {
            texto.append(String.format("%-24s", tipo));
            for (int f = 0; f < Transform.FUENTES.length; f++) {
                texto.append(String.format(" %s=%d", Transform.FUENTES[f], transform.getErrores(f, tipo)));
            }
            texto.append(String.format("  total=%d%n", transform.getErroresDeTipo(tipo)));
        }
        texto.append("\nErrores en la pila: ").append(transform.getErrores().tamanio()).append('\n');
        texto.append(String.format("%nTiempos: Extract %.1f ms, Transform %.1f ms, Load %.1f ms%n", msExtract,
                msTransform, msLoad));
        return texto.toString();
    }
}
