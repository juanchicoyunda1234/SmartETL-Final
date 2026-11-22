package ia;

import etl.Proyecto;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;

public final class VisorHtml {

    public static final String CARPETA = "diagramas";

    private VisorHtml() {
    }

    public static String normalizar(String nombre) {
        String sinTildes = Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    public static String archivoPais(String pais) {
        return "grafo_" + normalizar(pais) + ".html";
    }

    public static Path escribirDiagrama(String nombre, String titulo, String mermaid, String nota) throws IOException {
        Path carpeta = Paths.get(Proyecto.ruta(CARPETA));
        Files.createDirectories(carpeta);
        String markdown = "# " + titulo + "\n\n" + (nota.isEmpty() ? "" : nota + "\n\n") + "```mermaid\n" + mermaid.trim()
                + "\n```\n\nPara verlo con zoom abrir `ver_" + nombre + ".html`.\n";
        Files.write(carpeta.resolve(nombre + ".md"), markdown.getBytes(StandardCharsets.UTF_8));
        return escribirPagina("ver_" + nombre + ".html", titulo, mermaid, nota);
    }

    public static Path escribirPagina(String archivo, String titulo, String mermaid, String nota) throws IOException {
        Path carpeta = Paths.get(Proyecto.ruta(CARPETA));
        Files.createDirectories(carpeta);
        Path html = carpeta.resolve(archivo);
        Files.write(html, html(titulo, mermaid, nota).getBytes(StandardCharsets.UTF_8));
        return html;
    }

    public static String escaparHtml(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static String html(String titulo, String mermaid, String nota) {
        return "<!DOCTYPE html>\n<html lang=\"es\">\n<head>\n<meta charset=\"UTF-8\">\n<title>" + escaparHtml(titulo)
                + "</title>\n<style>\n"
                + "body{font-family:Segoe UI,Arial,sans-serif;margin:0;background:#f4f6f8;color:#1d2733}\n"
                + "header{padding:8px 16px;background:#1f3b5a;color:#fff;display:flex;gap:12px;align-items:center;flex-wrap:wrap}\n"
                + "button{padding:4px 12px;border:0;border-radius:4px;background:#e8eef5;cursor:pointer}\n"
                + "#lienzo{height:72vh;background:#fff;border-bottom:1px solid #ccd}\n"
                + "#lienzo svg{width:100%;height:100%}\n"
                + "textarea{box-sizing:border-box;width:calc(100% - 32px);height:20vh;margin:8px 16px;font-family:Consolas,monospace;font-size:12px}\n"
                + "</style>\n<script src=\"../recursos/mermaid.min.js\"></script>\n"
                + "<script src=\"../recursos/svg-pan-zoom.min.js\"></script>\n</head>\n<body>\n<header><strong>"
                + escaparHtml(titulo) + "</strong><button id=\"ajustar\">Ajustar</button>"
                + "<button id=\"copiar\">Copiar código Mermaid</button><span>" + escaparHtml(nota) + "</span></header>\n"
                + "<div id=\"lienzo\"></div>\n<textarea id=\"codigo\" readonly>" + escaparHtml(mermaid) + "</textarea>\n"
                + "<script>\n"
                + "mermaid.initialize({startOnLoad:false,securityLevel:'loose',maxTextSize:5000000,maxEdges:50000,"
                + "flowchart:{useMaxWidth:false,htmlLabels:true}});\n"
                + "var zoom=null;\n"
                + "mermaid.render('diagrama',document.getElementById('codigo').value).then(function(r){\n"
                + "  var lienzo=document.getElementById('lienzo');lienzo.innerHTML=r.svg;\n"
                + "  if(r.bindFunctions){r.bindFunctions(lienzo);}\n"
                + "  var svg=lienzo.querySelector('svg');svg.removeAttribute('style');\n"
                + "  svg.setAttribute('width','100%');svg.setAttribute('height','100%');\n"
                + "  zoom=svgPanZoom(svg,{zoomEnabled:true,controlIconsEnabled:true,fit:true,center:true,minZoom:0.05,maxZoom:50});\n"
                + "}).catch(function(e){document.getElementById('lienzo').innerHTML="
                + "'<pre style=\"color:#a00;padding:16px\">Error al dibujar: '+e+'</pre>';});\n"
                + "document.getElementById('ajustar').onclick=function(){if(zoom){zoom.resize();zoom.fit();zoom.center();}};\n"
                + "document.getElementById('copiar').onclick=function(){var t=document.getElementById('codigo');"
                + "t.select();document.execCommand('copy');};\n"
                + "</script>\n</body>\n</html>\n";
    }

    public static void abrir(Path html) throws IOException {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw new IOException("El sistema no permite abrir el navegador. Abre a mano: " + html.toAbsolutePath());
        }
        Desktop.getDesktop().browse(html.toAbsolutePath().toUri());
    }
}
