package app.swing;

import app.Sesion;

import algoritmos.Busqueda;
import algoritmos.Comparadores;
import algoritmos.Ordenamiento;
import algoritmos.ResultadoOrdenamiento;
import estructuras.ArbolBST;
import estructuras.Lista;
import estructuras.ListaEnlazada;
import estructuras.ListaSecuencial;
import estructuras.ResultadoBusqueda;
import etl.DatosLimpios;
import model.Aeropuerto;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;

public class PanelEstructuras extends JPanel {

    private final Sesion sesion;
    private final JComboBox<String> cmbAlgoritmo = new JComboBox<>(new String[]{"Burbuja", "Inserción"});
    private final JComboBox<String> cmbCampo = new JComboBox<>(new String[]{"IATA", "Nombre", "País"});
    private final JTextField txtBuscar = new JTextField("UIO", 6);
    private final JTextField txtClave = new JTextField("UIO", 6);
    private final JComboBox<String> cmbRecorrido = new JComboBox<>(new String[]{"Inorden", "Preorden", "Postorden"});
    private final JTextField txtLimite = new JTextField("20", 4);
    private final JTextArea txtSalida = Tarea.areaTexto();
    private final JLabel lblEstado = new JLabel("Los datos salen del ETL real (se ejecuta solo si hace falta).");
    private ListaSecuencial<Aeropuerto> ordenadaPorIata;
    private ArbolBST<Aeropuerto> arbolTrabajo;

    public PanelEstructuras(Sesion sesion) {
        this.sesion = sesion;
        setLayout(new BorderLayout(4, 4));
        JPanel controles = new JPanel();
        controles.setLayout(new BoxLayout(controles, BoxLayout.Y_AXIS));

        JPanel ordenar = Tarea.fila("Ordenamiento (sobre una copia de los 7.698 aeropuertos)");
        ordenar.add(new JLabel("Algoritmo:"));
        ordenar.add(cmbAlgoritmo);
        ordenar.add(new JLabel("Campo:"));
        ordenar.add(cmbCampo);
        ordenar.add(Tarea.boton("Ordenar", this::ordenar));
        controles.add(ordenar);

        JPanel buscar = Tarea.fila("Comparar búsquedas con el mismo valor (IATA)");
        buscar.add(txtBuscar);
        buscar.add(Tarea.boton("Secuencial vs binaria vs BST", this::compararBusquedas));
        controles.add(buscar);

        JPanel bst = Tarea.fila("BST de aeropuertos (clave IATA, copia de trabajo)");
        bst.add(Tarea.boton("Construir", this::construirArbol));
        bst.add(new JLabel("IATA:"));
        bst.add(txtClave);
        bst.add(Tarea.boton("Insertar", () -> operarArbol("insertar")));
        bst.add(Tarea.boton("Buscar", () -> operarArbol("buscar")));
        bst.add(Tarea.boton("Eliminar", () -> operarArbol("eliminar")));
        controles.add(bst);

        JPanel recorridos = Tarea.fila("Recorridos y medidas del BST");
        recorridos.add(cmbRecorrido);
        recorridos.add(new JLabel("Límite:"));
        recorridos.add(txtLimite);
        recorridos.add(Tarea.boton("Recorrer", this::recorrer));
        recorridos.add(Tarea.boton("Altura y nodos", this::medidas));
        controles.add(recorridos);

        JPanel desbalance = Tarea.fila("Demostración de desbalance");
        desbalance.add(Tarea.boton("BST por ID ascendente vs BST por IATA", this::desbalance));
        controles.add(desbalance);
        controles.setPreferredSize(new Dimension(470, 10));

        add(controles, BorderLayout.WEST);
        txtSalida.setText(ayuda());
        JScrollPane salida = new JScrollPane(txtSalida);
        salida.setBorder(BorderFactory.createTitledBorder("Resultado"));
        add(salida, BorderLayout.CENTER);
        lblEstado.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));
        add(lblEstado, BorderLayout.SOUTH);
    }

    private String ayuda() {
        return "Búsqueda binaria: solo sobre ListaSecuencial ya ordenada, porque necesita llegar al elemento del medio\n"
                + "en O(1). En una ListaEnlazada, obtener(i) recorre desde head (O(n)), así que cada salto de la\n"
                + "binaria costaría O(n) y se perdería la ventaja. Por eso no se ofrece sobre la lista enlazada.\n";
    }

    private void ordenar() {
        String algoritmo = (String) cmbAlgoritmo.getSelectedItem();
        String campo = (String) cmbCampo.getSelectedItem();
        Tarea.ejecutar(this, lblEstado, "Ordenando con " + algoritmo + " por " + campo, () -> {
            DatosLimpios datos = sesion.datosReales();
            return algoritmo.equals("Burbuja")
                    ? Ordenamiento.burbuja(datos.getAeropuertos(), Comparadores.porCampo(campo))
                    : Ordenamiento.insercion(datos.getAeropuertos(), Comparadores.porCampo(campo));
        }, r -> {
            if (campo.equals("IATA")) {
                ordenadaPorIata = r.getLista();
            }
            StringBuilder texto = new StringBuilder();
            texto.append(String.format("%s por %s sobre %d aeropuertos%n%d comparaciones, %d %s, %.1f ms%n",
                    r.getAlgoritmo(), campo, r.getLista().tamanio(), r.getComparaciones(), r.getMovimientos(),
                    r.nombreMovimientos(), r.getMilisegundos()));
            texto.append("La lista original no cambia: se ordenó una copia (ListaSecuencial).\n\nPrimeros 15:\n");
            agregarAeropuertos(texto, r.getLista(), 15);
            txtSalida.setText(texto.toString());
            lblEstado.setText(String.format("%s: %d comparaciones en %.1f ms", r.getAlgoritmo(), r.getComparaciones(),
                    r.getMilisegundos()));
        });
    }

    private void agregarAeropuertos(StringBuilder texto, Lista<Aeropuerto> lista, int limite) {
        for (int i = 0; i < Math.min(limite, lista.tamanio()); i++) {
            Aeropuerto a = lista.obtener(i);
            texto.append(String.format("%4d. %-9s %-45s %s%n", i + 1, a.getIata(), recortar(a.getNombre(), 45), a.getPais()));
        }
    }

    private String recortar(String texto, int largo) {
        return texto.length() <= largo ? texto : texto.substring(0, largo - 1) + "…";
    }

    private void compararBusquedas() {
        String iata = txtBuscar.getText().trim().toUpperCase();
        Tarea.ejecutar(this, lblEstado, "Buscando " + iata + " de tres formas", () -> {
            DatosLimpios datos = sesion.datosReales();
            ListaEnlazada<Aeropuerto> lista = sesion.listaEnlazada();
            StringBuilder texto = new StringBuilder("Búsqueda de " + iata + " con el mismo valor en tres estructuras\n\n");
            long inicio = System.nanoTime();
            ResultadoBusqueda<Aeropuerto> secuencial = Busqueda.secuencial(lista, a -> a.getIata().equals(iata));
            double msSecuencial = (System.nanoTime() - inicio) / 1_000_000.0;
            String notaOrden = "";
            if (ordenadaPorIata == null) {
                ordenadaPorIata = Ordenamiento.insercion(datos.getAeropuertos(), Comparadores.AEROPUERTO_POR_IATA).getLista();
                notaOrden = " (se ordenó por IATA con inserción antes de buscar)";
            }
            inicio = System.nanoTime();
            ResultadoBusqueda<Aeropuerto> binaria = Busqueda.binaria(ordenadaPorIata, Aeropuerto.conIata(iata),
                    Comparadores.AEROPUERTO_POR_IATA);
            double msBinaria = (System.nanoTime() - inicio) / 1_000_000.0;
            inicio = System.nanoTime();
            ResultadoBusqueda<Aeropuerto> arbol = Busqueda.porArbol(datos.getArbolIata(), Aeropuerto.conIata(iata));
            double msArbol = (System.nanoTime() - inicio) / 1_000_000.0;
            texto.append(String.format("%-46s %14s %12s  %s%n", "Estructura y algoritmo", "Comparaciones", "Tiempo (ms)", "Resultado"));
            texto.append(fila("Secuencial en ListaEnlazada (orden del archivo)", secuencial, msSecuencial,
                    secuencial.encontrado() ? "posición " + (secuencial.getIndice() + 1) : "no existe"));
            texto.append(fila("Binaria en ListaSecuencial ordenada por IATA", binaria, msBinaria,
                    binaria.encontrado() ? "índice " + binaria.getIndice() : "no existe"));
            texto.append(fila("BST global por IATA", arbol, msArbol,
                    arbol.encontrado() ? "profundidad " + arbol.getIndice() : "no existe"));
            if (arbol.encontrado()) {
                texto.append("\nAeropuerto: ").append(arbol.getElemento()).append('\n');
                texto.append("Camino en el BST: ").append(String.join(" -> ",
                        datos.getArbolIata().caminoHacia(Aeropuerto.conIata(iata)))).append('\n');
            }
            texto.append(notaOrden.isEmpty() ? "" : "\nNota" + notaOrden + ".\n");
            texto.append('\n').append(ayuda());
            return texto.toString();
        }, texto -> {
            txtSalida.setText(texto);
            lblEstado.setText("Comparación lista: O(n), binaria: O(log n), BST: O(altura).");
        });
    }

    private String fila(String nombre, ResultadoBusqueda<Aeropuerto> r, double ms, String resultado) {
        return String.format("%-46s %14d %12.4f  %s%n", nombre, r.getComparaciones(), ms, resultado);
    }

    private void construirArbol() {
        Tarea.ejecutar(this, lblEstado, "Construyendo el BST por IATA", () -> {
            DatosLimpios datos = sesion.datosReales();
            long inicio = System.nanoTime();
            ArbolBST<Aeropuerto> arbol = new ArbolBST<>(Comparadores.AEROPUERTO_POR_IATA);
            arbol.setEtiquetador(Aeropuerto::getIata);
            for (int i = 0; i < datos.getAeropuertos().tamanio(); i++) {
                Aeropuerto a = datos.getAeropuertos().obtener(i);
                if (DatosLimpios.tieneIata(a)) {
                    arbol.insertar(a);
                }
            }
            arbolTrabajo = arbol;
            return (System.nanoTime() - inicio) / 1_000_000.0;
        }, ms -> {
            lblEstado.setText(String.format("BST construido en %.1f ms", ms));
            medidas();
        });
    }

    private ArbolBST<Aeropuerto> arbol() {
        if (arbolTrabajo == null) {
            throw new IllegalStateException("Primero pulsa Construir para crear el BST de trabajo");
        }
        return arbolTrabajo;
    }

    private void operarArbol(String operacion) {
        String iata = txtClave.getText().trim().toUpperCase();
        Tarea.ejecutar(this, lblEstado, "Operación " + operacion + " " + iata, () -> {
            ArbolBST<Aeropuerto> arbol = arbol();
            Aeropuerto clave = Aeropuerto.conIata(iata);
            StringBuilder texto = new StringBuilder();
            if (operacion.equals("insertar")) {
                Aeropuerto existente = sesion.datosReales().buscarPorIata(iata);
                boolean insertado = arbol.insertar(existente != null ? existente : clave);
                texto.append(insertado ? "Insertado " + iata + " como hoja." : iata + " ya existe: no se inserta (devuelve falso).")
                        .append('\n');
            } else if (operacion.equals("buscar")) {
                ResultadoBusqueda<Aeropuerto> r = arbol.buscar(clave);
                texto.append(r.encontrado() ? "Encontrado a profundidad " + r.getIndice() + ": " + r.getElemento() : "No existe")
                        .append("\nComparaciones: ").append(r.getComparaciones()).append('\n');
            } else {
                boolean eliminado = arbol.eliminar(clave);
                texto.append(eliminado ? "Eliminado " + iata + "." : iata + " no existe en el árbol.").append('\n');
            }
            texto.append("Camino: ").append(String.join(" -> ", arbol.caminoHacia(clave))).append('\n')
                    .append("Nodos: ").append(arbol.contarNodos()).append(", altura: ").append(arbol.altura()).append('\n');
            return texto.toString();
        }, texto -> {
            txtSalida.setText(texto);
            lblEstado.setText("Operación " + operacion + " terminada.");
        });
    }

    private void recorrer() {
        String tipo = (String) cmbRecorrido.getSelectedItem();
        int limite = Tarea.entero(txtLimite, "Límite", 20);
        Tarea.ejecutar(this, lblEstado, "Recorrido " + tipo, () -> {
            ArbolBST<Aeropuerto> arbol = arbol();
            Lista<Aeropuerto> resultado = tipo.equals("Inorden") ? arbol.inOrden(limite)
                    : tipo.equals("Preorden") ? arbol.preOrden(limite) : arbol.postOrden(limite);
            StringBuilder texto = new StringBuilder(tipo + " (primeros " + limite + " de " + arbol.contarNodos() + "):\n");
            for (int i = 0; i < resultado.tamanio(); i++) {
                texto.append(i == 0 ? "" : ", ").append(resultado.obtener(i).getIata());
            }
            texto.append("\n\nInorden da las claves ordenadas; preorden visita raíz-izquierda-derecha; postorden izquierda-derecha-raíz.\n");
            return texto.toString();
        }, txtSalida::setText);
    }

    private void medidas() {
        Tarea.ejecutar(this, lblEstado, "Calculando medidas del BST", () -> {
            ArbolBST<Aeropuerto> arbol = arbol();
            StringBuilder texto = new StringBuilder();
            texto.append("Nodos: ").append(arbol.contarNodos()).append('\n')
                    .append("Altura (raíz sola = 0): ").append(arbol.altura()).append('\n')
                    .append(String.format("Promedio de comparaciones de una búsqueda exitosa: %.2f%n", arbol.promedioComparaciones()))
                    .append("\nPrimeros niveles:\n");
            for (String linea : arbol.primerosNiveles(4, 8)) {
                texto.append("  ").append(linea).append('\n');
            }
            return texto.toString();
        }, txtSalida::setText);
    }

    private void desbalance() {
        Tarea.ejecutar(this, lblEstado, "Construyendo el BST por ID ascendente", () -> {
            DatosLimpios datos = sesion.datosReales();
            ArbolBST<Aeropuerto> porId = new ArbolBST<>(Comparadores.AEROPUERTO_POR_ID);
            long inicio = System.nanoTime();
            for (int i = 0; i < 1500; i++) {
                porId.insertar(datos.getAeropuertos().obtener(i));
            }
            double ms = (System.nanoTime() - inicio) / 1_000_000.0;
            ArbolBST<Aeropuerto> iata = datos.getArbolIata();
            ResultadoBusqueda<Aeropuerto> ultimo = porId.buscar(datos.getAeropuertos().obtener(1499));
            return String.format("BST por ID con los primeros 1.500 aeropuertos (IDs ya vienen ordenados):%n"
                            + "  altura %d, %d nodos, construido en %.1f ms%n"
                            + "  buscar el último cuesta %d comparaciones: el árbol degeneró en una lista.%n%n"
                            + "BST global por IATA (6.071 claves en orden del archivo, no ordenadas):%n"
                            + "  altura %d, promedio de comparaciones %.2f%n%n"
                            + "Conclusión: el orden de inserción decide la forma del BST. Con claves ya ordenadas cada nodo%n"
                            + "queda a la derecha del anterior y la búsqueda pasa de O(log n) a O(n). Por eso insertar y buscar%n"
                            + "son iterativos: con 7.698 IDs la altura sería 7.697 y la recursión podría desbordar la pila.%n",
                    porId.altura(), porId.contarNodos(), ms, ultimo.getComparaciones(), iata.altura(),
                    iata.promedioComparaciones());
        }, txtSalida::setText);
    }
}
