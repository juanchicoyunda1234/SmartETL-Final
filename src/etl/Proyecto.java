package etl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Proyecto {

    private static Path base = Paths.get("");

    private Proyecto() {
    }

    public static boolean detectar() {
        Path actual = Paths.get("").toAbsolutePath();
        for (Path carpeta = actual; carpeta != null; carpeta = carpeta.getParent()) {
            if (esRaiz(carpeta)) {
                base = carpeta;
                return true;
            }
        }
        File[] hijas = actual.toFile().listFiles(File::isDirectory);
        if (hijas != null) {
            for (File hija : hijas) {
                if (esRaiz(hija.toPath())) {
                    base = hija.toPath();
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean usarCarpeta(Path elegida) {
        if (esRaiz(elegida)) {
            base = elegida;
            return true;
        }
        if (Files.exists(elegida.resolve("airports.csv")) && elegida.getParent() != null) {
            base = elegida.getParent();
            return true;
        }
        return false;
    }

    public static boolean esRaiz(Path carpeta) {
        return Files.exists(carpeta.resolve("data").resolve("airports.csv"));
    }

    public static boolean tieneRecursos() {
        return Files.exists(base.resolve("recursos").resolve("mermaid.min.js"));
    }

    public static String ruta(String relativa) {
        return base.resolve(relativa).toString();
    }

    public static Path getBase() {
        return base.toAbsolutePath();
    }
}
