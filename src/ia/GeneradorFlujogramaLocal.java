package ia;

import estructuras.ListaEnlazada;
import estructuras.ListaSecuencial;
import estructuras.Nodo;
import grafo.Agrupacion;
import grafo.GrafoPais;
import grafo.Vertice;
import model.Aeropuerto;

public final class GeneradorFlujogramaLocal {

    public static final int LIMITE_VERTICES_COMPLETO = 60;

    private GeneradorFlujogramaLocal() {
    }

    public static String recorrido(RegistroEventos registro, int nivelDiagrama) {
        boolean esBfs = registro.contar("desencolar") > 0 || registro.contar("encolar") > 0;
        String estructura = esBfs ? "cola" : "pila";
        String verbo = esBfs ? "desencolar" : "desapilar";
        Mermaid m = new Mermaid();
        ListaSecuencial<Evento> pendientes = new ListaSecuencial<>();
        String total = "";
        boolean origenMostrado = false;
        for (Nodo<Evento> n = registro.getEventos().primerNodo(); n != null; n = n.getSiguiente()) {
            Evento e = n.getDato();
            if (e.getTipo().equals("inicio")) {
                m.inicio(e.getTexto().replace(" (solo rutas internas)", "").replace(" (cruzando fronteras)", ""));
            } else if (!origenMostrado && (e.getTipo().equals("encolar") || e.getTipo().equals("apilar"))) {
                origenMostrado = true;
                m.caja(e.getTexto() + "<br/>" + estructura + " = " + e.estadoComoTexto());
            } else if (e.getTipo().equals(verbo) && e.getNivel() <= nivelDiagrama) {
                if (e.getElementos().length == 0) {
                    pendientes.insertarFinal(e);
                } else {
                    volcar(m, pendientes, verbo, esBfs);
                    String rotulo = e.getElementos().length == 1 ? (esBfs ? "vecino nuevo" : "vecino apilado")
                            : (esBfs ? "vecinos nuevos" : "vecinos apilados");
                    m.caja(e.getTexto() + "<br/>" + rotulo + ": " + Evento.unir(e.getElementos(), e.getElementos().length)
                            + "<br/>" + estructura + " = " + e.estadoComoTexto());
                }
            } else if (e.getTipo().equals(verbo)) {
                volcar(m, pendientes, verbo, esBfs);
            } else if (e.getTipo().equals("fin")) {
                total = e.getSujeto();
            }
        }
        volcar(m, pendientes, verbo, esBfs);
        String[] resumen = resumenNivelesProfundos(registro, nivelDiagrama);
        for (String linea : resumen) {
            m.caja(linea);
        }
        m.decision(esBfs ? "¿Cola vacía?" : "¿Pila vacía?");
        m.fin("Fin: " + total + " aeropuertos visitados", "Sí");
        return m.toString();
    }

    public static String[] resumenNivelesProfundos(RegistroEventos registro, int nivelDiagrama) {
        boolean esBfs = registro.contar("desencolar") > 0;
        String verbo = esBfs ? "desencolar" : "desapilar";
        int nivelMaximo = 0;
        for (Nodo<Evento> n = registro.getEventos().primerNodo(); n != null; n = n.getSiguiente()) {
            if (n.getDato().getTipo().equals(verbo)) {
                nivelMaximo = Math.max(nivelMaximo, n.getDato().getNivel());
            }
        }
        Agrupacion[] porNivel = new Agrupacion[nivelMaximo + 1];
        for (Nodo<Evento> n = registro.getEventos().primerNodo(); n != null; n = n.getSiguiente()) {
            Evento e = n.getDato();
            if (e.getTipo().equals(verbo) && e.getNivel() > nivelDiagrama) {
                if (porNivel[e.getNivel()] == null) {
                    porNivel[e.getNivel()] = new Agrupacion();
                }
                porNivel[e.getNivel()].agregar(e.getGrupo(), e.getSujeto());
            }
        }
        ListaSecuencial<String> lineas = new ListaSecuencial<>();
        for (int nivel = 0; nivel < porNivel.length; nivel++) {
            if (porNivel[nivel] != null) {
                int cantidad = 0;
                for (int i = 0; i < porNivel[nivel].tamanio(); i++) {
                    cantidad += porNivel[nivel].conteo(i);
                }
                int paises = porNivel[nivel].tamanio();
                lineas.insertarFinal((esBfs ? "Nivel " : "Profundidad ") + nivel + ": " + cantidad
                        + " aeropuertos nuevos en " + paises + (paises == 1 ? " país" : " países"));
            }
        }
        String[] resultado = new String[lineas.tamanio()];
        for (int i = 0; i < resultado.length; i++) {
            resultado[i] = lineas.obtener(i);
        }
        return resultado;
    }

    private static void volcar(Mermaid m, ListaSecuencial<Evento> pendientes, String verbo, boolean esBfs) {
        if (pendientes.estaVacia()) {
            return;
        }
        String[] nombres = new String[pendientes.tamanio()];
        for (int i = 0; i < nombres.length; i++) {
            nombres[i] = pendientes.obtener(i).getSujeto();
        }
        String ninguno = esBfs ? "vecinos nuevos" : "vecinos por apilar";
        if (nombres.length == 1) {
            m.caja(pendientes.obtener(0).getTexto() + "<br/>no aporta " + ninguno);
        } else {
            m.caja("Pasos " + numeroPaso(pendientes.obtener(0)) + " a " + numeroPaso(pendientes.obtener(nombres.length - 1))
                    + ": " + verbo + " " + Evento.unir(nombres, nombres.length) + "<br/>ninguno aporta " + ninguno);
        }
        pendientes.limpiar();
    }

    private static String numeroPaso(Evento evento) {
        String texto = evento.getTexto();
        int dosPuntos = texto.indexOf(':');
        return texto.startsWith("Paso ") && dosPuntos > 5 ? texto.substring(5, dosPuntos) : "?";
    }

    public static String etl(RegistroEventos registro) {
        Mermaid m = new Mermaid();
        ListaSecuencial<String> validos = new ListaSecuencial<>();
        ListaSecuencial<String> invalidos = new ListaSecuencial<>();
        ListaSecuencial<String> escritos = new ListaSecuencial<>();
        for (Nodo<Evento> n = registro.getEventos().primerNodo(); n != null; n = n.getSiguiente()) {
            Evento e = n.getDato();
            switch (e.getTipo()) {
                case "inicio":
                    m.inicio(e.getTexto());
                    break;
                case "leer":
                    m.caja("EXTRACT<br/>" + e.getTexto());
                    break;
                case "encolar":
                    m.caja("Cola de extracción<br/>" + e.getTexto());
                    break;
                case "valido":
                    validos.insertarFinal(e.getTexto());
                    break;
                case "invalido":
                    invalidos.insertarFinal(e.getSujeto());
                    break;
                case "totales":
                    ramaTransform(m, e, validos, invalidos);
                    break;
                case "indice":
                case "fin_transform":
                    m.caja(e.getTexto());
                    break;
                case "escribir":
                    escritos.insertarFinal(e.getTexto().replace("Load escribe ", ""));
                    break;
                default:
                    break;
            }
        }
        if (!escritos.estaVacia()) {
            String[] nombres = new String[escritos.tamanio()];
            for (int i = 0; i < nombres.length; i++) {
                nombres[i] = escritos.obtener(i);
            }
            m.caja("LOAD escribe en output<br/>" + Evento.unir(nombres, nombres.length));
            m.fin("Fin del ETL", null);
        } else {
            m.fin("Fin del Transform (falta pulsar Cargar para el Load)", null);
        }
        return m.toString();
    }

    private static void ramaTransform(Mermaid m, Evento totales, ListaSecuencial<String> validos,
                                      ListaSecuencial<String> invalidos) {
        String fuente = totales.getGrupo();
        String[] cifras = totales.getElementos();
        m.caja("TRANSFORM de " + fuente + "<br/>desencolar cada registro y validar");
        String pregunta = m.decision("¿Registro válido?");
        String lista = m.nodo("[", "Lista de válidos: " + cifras[1] + (validos.estaVacia() ? "" : "<br/>ej. "
                + ejemplo(validos.obtener(0))), "]");
        String pila = m.nodo("[", "Pila de errores: " + cifras[2] + (cifras[3].isEmpty() ? "" : "<br/>" + cifras[3]), "]");
        m.enlazar(pregunta, lista, "Sí");
        m.enlazar(pregunta, pila, "No");
        String resumen = m.nodo("[", "Totales de " + fuente + ": " + cifras[0] + " procesados, " + cifras[1]
                + " válidos, " + cifras[2] + " inválidos", "]");
        m.enlazar(lista, resumen, null);
        m.enlazar(pila, resumen, null);
        m.setUltimo(resumen);
        validos.limpiar();
        invalidos.limpiar();
    }

    private static String ejemplo(String texto) {
        int inicio = texto.indexOf("lista (");
        return inicio < 0 ? texto : texto.substring(inicio + 7, texto.length() - 1);
    }

    public static String estructura(RegistroEventos registro) {
        Mermaid m = new Mermaid();
        m.inicio("Inicio: traza real de " + registro.getTitulo());
        for (Nodo<Evento> n = registro.getEventos().primerNodo(); n != null; n = n.getSiguiente()) {
            Evento e = n.getDato();
            StringBuilder etiqueta = new StringBuilder(e.getTexto());
            if (e.getGrupo().equals("bst")) {
                if (e.getElementos().length > 0) {
                    etiqueta.append("<br/>camino: ").append(String.join(" → ", e.getElementos()));
                }
                etiqueta.append("<br/>nodos en el árbol: ").append(e.getTamanioEstado());
            } else if (!e.getGrupo().isEmpty()) {
                etiqueta.append("<br/>").append(e.getGrupo()).append(" = ").append(e.estadoComoTexto());
            }
            m.caja(etiqueta.toString());
        }
        m.fin("Fin de la traza", null);
        return m.toString();
    }

    public static String generalPaises(ListaSecuencial<GrafoPais> construidos, int limite) {
        Mermaid m = new Mermaid();
        m.inicio("Inicio: construir grafos por país");
        int mostrar = Math.min(limite, construidos.tamanio());
        for (int i = 0; i < mostrar; i++) {
            GrafoPais p = construidos.obtener(i);
            String id = m.caja("Construir grafo " + p.getNombre() + "<br/>" + p.getVertices() + " aeropuertos, "
                    + p.getRutasInternas() + " rutas internas<br/>" + p.getSalidasExterior() + " salen, "
                    + p.getLlegadasExterior() + " llegan del exterior");
            m.clic(id, VisorHtml.archivoPais(p.getNombre()));
        }
        if (construidos.tamanio() > mostrar) {
            m.caja("y " + (construidos.tamanio() - mostrar) + " países más");
        }
        m.fin("Fin: " + construidos.tamanio() + " países construidos", null);
        return m.toString();
    }

    public static ListaSecuencial<Vertice<Aeropuerto>> verticesParaDiagrama(GrafoPais pais, Vertice<Aeropuerto> origen,
                                                                           int nivelDiagrama) {
        ListaSecuencial<Vertice<Aeropuerto>> incluidos = new ListaSecuencial<>();
        ListaEnlazada<Vertice<Aeropuerto>> vertices = pais.getGrafo().getVertices();
        if (pais.getVertices() <= LIMITE_VERTICES_COMPLETO || origen == null) {
            for (Nodo<Vertice<Aeropuerto>> n = vertices.primerNodo(); n != null; n = n.getSiguiente()) {
                incluidos.insertarFinal(n.getDato());
                if (incluidos.tamanio() >= LIMITE_VERTICES_COMPLETO * 2) {
                    break;
                }
            }
            return incluidos;
        }
        return algoritmos.BFS.recorrer(origen, null, nivelDiagrama, null).getVisitados();
    }

    public static String grafoPais(GrafoPais pais, Vertice<Aeropuerto> origen, int nivelDiagrama,
                                   ListaSecuencial<GrafoPais> construidos) {
        ListaSecuencial<Vertice<Aeropuerto>> incluidos = verticesParaDiagrama(pais, origen, nivelDiagrama);
        boolean soloArbol = pais.getVertices() > LIMITE_VERTICES_COMPLETO && origen != null;
        StringBuilder m = new StringBuilder("flowchart LR\n");
        m.append("    subgraph PAIS[\"").append(Mermaid.limpiar(pais.getNombre())).append(": ")
                .append(incluidos.tamanio()).append(" de ").append(pais.getVertices()).append(" aeropuertos")
                .append(soloArbol ? " (árbol BFS desde " + origen.getEtiqueta() + " hasta nivel " + nivelDiagrama + ")" : "")
                .append("\"]\n");
        for (int i = 0; i < incluidos.tamanio(); i++) {
            Vertice<Aeropuerto> v = incluidos.obtener(i);
            m.append("        ").append(id(v)).append("[\"").append(Mermaid.limpiar(v.getEtiqueta() + "<br/>"
                    + v.getDato().getCiudad())).append("\"]\n");
        }
        for (int i = 0; soloArbol && i < incluidos.tamanio(); i++) {
            Vertice<Aeropuerto> v = incluidos.obtener(i);
            if (v.getPadre() != null) {
                m.append("        ").append(id(v.getPadre())).append(" --> ").append(id(v)).append('\n');
            }
        }
        for (int i = 0; !soloArbol && i < incluidos.tamanio(); i++) {
            Vertice<Aeropuerto> v = incluidos.obtener(i);
            for (Nodo<Vertice<Aeropuerto>> n = v.getAdyacentes().primerNodo(); n != null; n = n.getSiguiente()) {
                Vertice<Aeropuerto> w = n.getDato();
                if (!contiene(incluidos, w)) {
                    continue;
                }
                boolean ida = w.getAdyacentes().contiene(v);
                if (ida && indice(incluidos, w) < i) {
                    continue;
                }
                m.append("        ").append(id(v)).append(ida ? " <--> " : " --> ").append(id(w)).append('\n');
            }
        }
        m.append("    end\n");
        Agrupacion externos = pais.externosPorPais();
        for (int i = 0; i < externos.tamanio(); i++) {
            String idPais = "EXT" + i;
            m.append("    ").append(idPais).append("[\"").append(Mermaid.limpiar(externos.clave(i))).append("\"]\n");
            m.append("    PAIS -->|\"").append(externos.conteo(i)).append(externos.conteo(i) == 1 ? " ruta" : " rutas")
                    .append("\"| ").append(idPais).append('\n');
            if (estaConstruido(construidos, externos.clave(i))) {
                m.append("    click ").append(idPais).append(" \"").append(VisorHtml.archivoPais(externos.clave(i)))
                        .append("\" \"Abrir país\"\n");
            }
        }
        return m.toString();
    }

    private static boolean estaConstruido(ListaSecuencial<GrafoPais> construidos, String pais) {
        return construidos.contar(p -> p.getNombre().equalsIgnoreCase(pais)) > 0;
    }

    private static boolean contiene(ListaSecuencial<Vertice<Aeropuerto>> lista, Vertice<Aeropuerto> v) {
        return indice(lista, v) >= 0;
    }

    private static int indice(ListaSecuencial<Vertice<Aeropuerto>> lista, Vertice<Aeropuerto> v) {
        for (int i = 0; i < lista.tamanio(); i++) {
            if (lista.obtener(i) == v) {
                return i;
            }
        }
        return -1;
    }

    private static String id(Vertice<Aeropuerto> v) {
        return "V_" + v.getEtiqueta().replaceAll("[^A-Za-z0-9]", "_");
    }
}
