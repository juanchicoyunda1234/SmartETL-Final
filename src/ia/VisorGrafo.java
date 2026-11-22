package ia;

import etl.Proyecto;
import estructuras.ListaSecuencial;
import estructuras.Nodo;
import grafo.GrafoPais;
import grafo.MapaPaises;
import grafo.Vertice;
import model.Aeropuerto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class VisorGrafo {

    public static final String ARCHIVO = "visor_grafo.html";

    private VisorGrafo() {
    }

    public static Path escribir(MapaPaises mapa, String origen) throws IOException {
        int[] indicePorId = new int[idMaximo(mapa) + 1];
        StringBuilder nodos = new StringBuilder("[");
        int total = 0;
        ListaSecuencial<GrafoPais> paises = mapa.construidos();
        for (int p = 0; p < paises.tamanio(); p++) {
            for (Nodo<Vertice<Aeropuerto>> n = paises.obtener(p).getGrafo().getVertices().primerNodo(); n != null; n = n.getSiguiente()) {
                Aeropuerto a = n.getDato().getDato();
                indicePorId[Integer.parseInt(a.getId())] = total + 1;
                nodos.append(total == 0 ? "" : ",\n").append('[').append(Json.cadena(n.getDato().getEtiqueta())).append(',')
                        .append(Json.cadena(a.getNombre())).append(',').append(Json.cadena(a.getCiudad())).append(',')
                        .append(Json.cadena(a.getPais())).append(']');
                total++;
            }
        }
        nodos.append(']');
        StringBuilder aristas = new StringBuilder("[");
        int cantidad = 0;
        for (int p = 0; p < paises.tamanio(); p++) {
            for (Nodo<Vertice<Aeropuerto>> n = paises.obtener(p).getGrafo().getVertices().primerNodo(); n != null; n = n.getSiguiente()) {
                int desde = indicePorId[Integer.parseInt(n.getDato().getDato().getId())] - 1;
                for (Nodo<Vertice<Aeropuerto>> w = n.getDato().getAdyacentes().primerNodo(); w != null; w = w.getSiguiente()) {
                    cantidad = agregarArista(aristas, cantidad, desde, indicePorId[Integer.parseInt(w.getDato().getDato().getId())] - 1);
                }
                for (Nodo<Aeropuerto> e = n.getDato().getExternos().primerNodo(); e != null; e = e.getSiguiente()) {
                    cantidad = agregarArista(aristas, cantidad, desde, indicePorId[Integer.parseInt(e.getDato().getId())] - 1);
                }
            }
        }
        aristas.append(']');
        Path carpeta = Paths.get(Proyecto.ruta(VisorHtml.CARPETA));
        Files.createDirectories(carpeta);
        Path html = carpeta.resolve(ARCHIVO);
        Files.write(html, html(nodos.toString(), aristas.toString(), origen, total, cantidad).getBytes(StandardCharsets.UTF_8));
        return html;
    }

    private static int agregarArista(StringBuilder aristas, int cantidad, int desde, int hasta) {
        if (desde < 0 || hasta < 0) {
            return cantidad;
        }
        aristas.append(cantidad == 0 ? "" : ",").append(cantidad % 20 == 19 ? "\n" : "").append('[').append(desde)
                .append(',').append(hasta).append(']');
        return cantidad + 1;
    }

    private static int idMaximo(MapaPaises mapa) {
        int maximo = 0;
        ListaSecuencial<Aeropuerto> aeropuertos = mapa.getDatos().getAeropuertos();
        for (int i = 0; i < aeropuertos.tamanio(); i++) {
            maximo = Math.max(maximo, Integer.parseInt(aeropuertos.obtener(i).getId()));
        }
        return maximo;
    }

    private static String html(String nodos, String aristas, String origen, int totalNodos, int totalAristas) {
        return "<!DOCTYPE html>\n<html lang=\"es\">\n<head>\n<meta charset=\"UTF-8\">\n"
                + "<title>Visor del grafo de aeropuertos</title>\n"
                + "<script src=\"../recursos/vis-network.min.js\"></script>\n<style>\n"
                + "body{margin:0;font-family:Segoe UI,Arial,sans-serif;display:flex;height:100vh;color:#1d2733}\n"
                + "#lado{width:310px;padding:10px;overflow:auto;background:#f4f6f8;border-right:1px solid #ccd;box-sizing:border-box}\n"
                + "#red{flex:1}\n.fila{margin:8px 0}\n"
                + "#paises{max-height:32vh;overflow:auto;border:1px solid #ccd;background:#fff;padding:4px;font-size:13px}\n"
                + "#info{background:#fff;border:1px solid #ccd;padding:6px;min-height:90px;font-size:13px}\n"
                + "#aviso{color:#a00;font-weight:bold}\n.muestra{display:inline-block;width:10px;height:10px;border-radius:5px;margin-right:4px}\n"
                + "</style>\n</head>\n<body>\n<div id=\"lado\">\n<h3 style=\"margin:4px 0\">Grafo de aeropuertos</h3>\n"
                + "<div style=\"font-size:12px\">" + totalNodos + " aeropuertos y " + totalAristas
                + " rutas únicas (internas y externas) del ETL real.</div>\n"
                + "<div class=\"fila\">Origen IATA <input id=\"origen\" size=\"5\" value=\"" + VisorHtml.escaparHtml(origen)
                + "\"> <button id=\"ir\">Mostrar</button></div>\n"
                + "<div class=\"fila\">Niveles <input type=\"range\" id=\"niveles\" min=\"1\" max=\"8\" value=\"2\"> <b id=\"vn\">2</b></div>\n"
                + "<div class=\"fila\">Buscar IATA <input id=\"buscar\" size=\"5\"> <button id=\"btnBuscar\">Buscar</button></div>\n"
                + "<div class=\"fila\"><b>Países</b> <button id=\"todos\">Todos</button> <button id=\"ninguno\">Ninguno</button>"
                + "<div id=\"paises\"></div></div>\n<div id=\"aviso\"></div><div id=\"resumen\" style=\"font-size:13px\"></div>\n"
                + "<h4 style=\"margin:10px 0 4px\">Aeropuerto</h4><div id=\"info\">Haz clic en un aeropuerto para ver sus datos y "
                + "expandir sus vecinos.</div>\n</div>\n<div id=\"red\"></div>\n<script>\n"
                + "var N=" + nodos + ";\nvar E=" + aristas + ";\n" + SCRIPT + "</script>\n</body>\n</html>\n";
    }

    private static final String SCRIPT = ""
            + "var LIMITE=2000;\n"
            + "var salida=N.map(function(){return [];}),entrada=N.map(function(){return 0;});\n"
            + "E.forEach(function(e){salida[e[0]].push(e[1]);entrada[e[1]]++;});\n"
            + "var indice={};N.forEach(function(n,i){if(indice[n[0]]===undefined){indice[n[0]]=i;}});\n"
            + "var origen=-1,nivelDe={},extra={},ocultos={},red=null;\n"
            + "function $(id){return document.getElementById(id);}\n"
            + "function color(pais){var h=0;for(var i=0;i<pais.length;i++){h=(h*31+pais.charCodeAt(i))%360;}return 'hsl('+h+',65%,52%)';}\n"
            + "function bfs(o,max){var niv={};niv[o]=0;var cola=[o],k=0;while(k<cola.length){var v=cola[k++];"
            + "if(niv[v]>=max){continue;}salida[v].forEach(function(w){if(niv[w]===undefined){niv[w]=niv[v]+1;cola.push(w);}});}return niv;}\n"
            + "function calcular(){var codigo=$('origen').value.trim().toUpperCase();if(indice[codigo]===undefined){"
            + "$('aviso').textContent='No existe el aeropuerto '+codigo+' en el grafo.';return;}"
            + "origen=indice[codigo];extra={};ocultos={};nivelDe=bfs(origen,+$('niveles').value);listarPaises();dibujar();}\n"
            + "function listarPaises(){var cuenta={};Object.keys(nivelDe).forEach(function(k){var p=N[k][3];cuenta[p]=(cuenta[p]||0)+1;});"
            + "var caja=$('paises');caja.innerHTML='';Object.keys(cuenta).sort().forEach(function(p){var l=document.createElement('label');"
            + "l.style.display='block';var c=document.createElement('input');c.type='checkbox';c.checked=!ocultos[p];"
            + "c.onchange=function(){ocultos[p]=!c.checked;dibujar();};var m=document.createElement('span');m.className='muestra';"
            + "m.style.background=color(p);l.appendChild(c);l.appendChild(m);l.appendChild(document.createTextNode(p+' ('+cuenta[p]+')'));"
            + "caja.appendChild(l);});}\n"
            + "function visibles(){var ids={};Object.keys(nivelDe).forEach(function(k){k=+k;if(k===origen||!ocultos[N[k][3]]){ids[k]=true;}});"
            + "Object.keys(extra).forEach(function(k){ids[k]=true;});return Object.keys(ids).map(Number);}\n"
            + "function dibujar(){var ids=visibles();$('aviso').textContent='';"
            + "if(ids.length>LIMITE){$('aviso').textContent='Hay '+ids.length+' aeropuertos y el límite es '+LIMITE"
            + "+'. Reduce los niveles o desmarca países.';if(red){red.destroy();red=null;}$('resumen').textContent='';return;}\n"
            + "var dentro={};ids.forEach(function(i){dentro[i]=true;});\n"
            + "var nodos=ids.map(function(i){var n=N[i];return {id:i,label:n[0],title:n[1]+' ('+n[2]+', '+n[3]+')',"
            + "color:color(n[3]),shape:i===origen?'star':'dot',size:i===origen?24:9};});\n"
            + "var aristas=[];ids.forEach(function(i){salida[i].forEach(function(j){if(dentro[j]){aristas.push({from:i,to:j});}});});\n"
            + "if(red){red.destroy();}\n"
            + "red=new vis.Network($('red'),{nodes:new vis.DataSet(nodos),edges:new vis.DataSet(aristas)},{"
            + "nodes:{font:{size:11}},edges:{arrows:{to:{enabled:true,scaleFactor:0.4}},color:{color:'#b8c0cc'},smooth:false,width:0.6},"
            + "physics:{solver:'barnesHut',stabilization:{iterations:ids.length>600?80:200}},interaction:{hover:true}});\n"
            + "red.once('stabilizationIterationsDone',function(){red.setOptions({physics:false});});\n"
            + "red.on('click',function(p){if(p.nodes.length){var i=p.nodes[0];mostrar(i);salida[i].forEach(function(j){extra[j]=true;});"
            + "extra[i]=true;dibujar();red.selectNodes([i]);}});\n"
            + "$('resumen').textContent=ids.length+' aeropuertos y '+aristas.length+' rutas en pantalla (nivel '+$('niveles').value+' desde '+N[origen][0]+').';}\n"
            + "function mostrar(i){var n=N[i],info=$('info');info.innerHTML='';[['Código',n[0]],['Nombre',n[1]],['Ciudad',n[2]],"
            + "['País',n[3]],['Rutas de salida',salida[i].length],['Rutas de llegada',entrada[i]]].forEach(function(f){"
            + "var d=document.createElement('div');var b=document.createElement('b');b.textContent=f[0]+': ';d.appendChild(b);"
            + "d.appendChild(document.createTextNode(f[1]));info.appendChild(d);});}\n"
            + "$('niveles').oninput=function(){$('vn').textContent=this.value;};\n"
            + "$('niveles').onchange=function(){calcular();};\n"
            + "$('ir').onclick=calcular;\n"
            + "$('origen').onkeydown=function(e){if(e.key==='Enter'){calcular();}};\n"
            + "$('todos').onclick=function(){ocultos={};listarPaises();dibujar();};\n"
            + "$('ninguno').onclick=function(){Object.keys(nivelDe).forEach(function(k){ocultos[N[k][3]]=true;});listarPaises();dibujar();};\n"
            + "$('btnBuscar').onclick=function(){var c=$('buscar').value.trim().toUpperCase();var i=indice[c];"
            + "if(i===undefined){$('aviso').textContent='No existe '+c+'.';return;}mostrar(i);"
            + "if(red&&visibles().indexOf(i)>=0){red.selectNodes([i]);red.focus(i,{scale:1.4,animation:true});$('aviso').textContent='';}"
            + "else{$('aviso').textContent=c+' no está en la vista: aumenta los niveles o úsalo como origen.';}};\n"
            + "calcular();\n";
}
