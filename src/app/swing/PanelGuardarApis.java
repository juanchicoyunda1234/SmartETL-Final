package app.swing;

import app.Sesion;

import ia.AlmacenApis;
import ia.ApiGuardada;
import ia.ConfiguracionIA;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class PanelGuardarApis extends JPanel {

    private final Sesion sesion;
    private final PanelIA panelIA;
    private final Runnable irAPestaniaIA;
    private final JTextField txtNombre = new JTextField(24);
    private final JComboBox<String> cmbProveedor = new JComboBox<>(ConfiguracionIA.PROVEEDORES);
    private final JTextField txtModelo = new JTextField(24);
    private final JTextField txtUrl = new JTextField(40);
    private final JPasswordField txtClave = new JPasswordField(30);
    private final JCheckBox chkMostrar = new JCheckBox("mostrar");
    private final JLabel lblAyuda = new JLabel(" ");
    private final DefaultListModel<String> modeloLista = new DefaultListModel<>();
    private final JList<String> lstApis = new JList<>(modeloLista);
    private final JLabel lblEstado = new JLabel(" ");
    private final char ocultar;

    public PanelGuardarApis(Sesion sesion, PanelIA panelIA, Runnable irAPestaniaIA) {
        this.sesion = sesion;
        this.panelIA = panelIA;
        this.irAPestaniaIA = irAPestaniaIA;
        this.ocultar = txtClave.getEchoChar();
        setLayout(new BorderLayout(6, 6));

        JLabel aviso = new JLabel("⚠ Las claves se guardan en texto plano en tu computador. No las compartas ni las subas a GitHub.");
        aviso.setOpaque(true);
        aviso.setBackground(new Color(255, 246, 204));
        aviso.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        add(aviso, BorderLayout.NORTH);

        add(construirFormulario(), BorderLayout.WEST);
        lstApis.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstApis.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarSeleccionada();
            }
        });
        lstApis.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    cargar();
                }
            }
        });
        add(Tarea.conBorde(lstApis, "APIs guardadas (nombre | proveedor | modelo). Doble clic para cargar en la pestaña IA"),
                BorderLayout.CENTER);
        lblEstado.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));
        add(lblEstado, BorderLayout.SOUTH);

        cmbProveedor.addActionListener(e -> proveedorElegido());
        chkMostrar.addActionListener(e -> txtClave.setEchoChar(chkMostrar.isSelected() ? (char) 0 : ocultar));
        proveedorElegido();
        refrescarLista();
        AlmacenApis almacen = sesion.getApisGuardadas();
        lblEstado.setText(almacen == null ? sesion.getErrorApis() : "Archivo: " + almacen.getArchivo());
    }

    private JPanel construirFormulario() {
        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createTitledBorder("Configuración"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
        agregarFila(formulario, c, 0, "Nombre:", txtNombre);
        agregarFila(formulario, c, 1, "Proveedor:", cmbProveedor);
        agregarFila(formulario, c, 2, "Modelo:", txtModelo);
        agregarFila(formulario, c, 3, "URL:", txtUrl);
        JPanel clave = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        clave.add(txtClave);
        clave.add(chkMostrar);
        agregarFila(formulario, c, 4, "Clave:", clave);
        c.gridx = 1;
        c.gridy = 5;
        lblAyuda.setForeground(new Color(90, 90, 90));
        formulario.add(lblAyuda, c);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        botones.add(Tarea.boton("Guardar", this::guardar));
        botones.add(Tarea.boton("Eliminar", this::eliminar));
        botones.add(Tarea.boton("Cargar en la pestaña IA", this::cargar));
        botones.add(Tarea.boton("Limpiar", this::limpiar));
        c.gridy = 6;
        formulario.add(botones, c);
        c.gridy = 7;
        c.weighty = 1;
        formulario.add(new JLabel(" "), c);
        return formulario;
    }

    private void agregarFila(JPanel formulario, GridBagConstraints c, int fila, String etiqueta, java.awt.Component campo) {
        c.gridy = fila;
        c.gridx = 0;
        c.weighty = 0;
        formulario.add(new JLabel(etiqueta), c);
        c.gridx = 1;
        formulario.add(campo, c);
    }

    private void proveedorElegido() {
        String proveedor = (String) cmbProveedor.getSelectedItem();
        txtUrl.setText(ConfiguracionIA.urlDe(proveedor));
        txtUrl.setCaretPosition(0);
        lblAyuda.setText("<html>Modelo: " + ConfiguracionIA.ayudaModelo(proveedor) + "<br/>Clave: "
                + ConfiguracionIA.ayudaClave(proveedor) + "</html>");
    }

    private void refrescarLista() {
        modeloLista.clear();
        AlmacenApis almacen = sesion.getApisGuardadas();
        if (almacen == null) {
            return;
        }
        for (int i = 0; i < almacen.getApis().tamanio(); i++) {
            modeloLista.addElement(almacen.getApis().obtener(i).descripcion());
        }
        sesion.avisarCambioApis();
    }

    private ApiGuardada seleccionada() {
        int indice = lstApis.getSelectedIndex();
        AlmacenApis almacen = sesion.getApisGuardadas();
        if (indice < 0 || almacen == null || indice >= almacen.getApis().tamanio()) {
            return null;
        }
        return almacen.getApis().obtener(indice);
    }

    private void mostrarSeleccionada() {
        ApiGuardada api = seleccionada();
        if (api == null) {
            return;
        }
        txtNombre.setText(api.getNombre());
        cmbProveedor.setSelectedItem(api.getProveedor());
        txtModelo.setText(api.getModelo());
        txtUrl.setText(api.getUrl());
        txtUrl.setCaretPosition(0);
        txtClave.setText(api.getClave());
    }

    private void guardar() {
        AlmacenApis almacen = sesion.getApisGuardadas();
        if (almacen == null) {
            lblEstado.setText(sesion.getErrorApis());
            return;
        }
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escribe un nombre, por ejemplo \"api de prueba 1\".", "Falta el nombre",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean existia = almacen.buscar(nombre) != null;
        try {
            almacen.guardar(new ApiGuardada(nombre, (String) cmbProveedor.getSelectedItem(), txtModelo.getText(),
                    txtUrl.getText(), new String(txtClave.getPassword())));
            refrescarLista();
            seleccionar(nombre);
            lblEstado.setText((existia ? "Actualizada" : "Guardada") + " \"" + nombre + "\" en " + almacen.getArchivo());
        } catch (IOException ex) {
            lblEstado.setText("No se pudo guardar: " + ex.getMessage());
        }
    }

    private void eliminar() {
        ApiGuardada api = seleccionada();
        AlmacenApis almacen = sesion.getApisGuardadas();
        if (api == null || almacen == null) {
            lblEstado.setText("Selecciona una API de la lista para eliminarla.");
            return;
        }
        int respuesta = JOptionPane.showConfirmDialog(this, "¿Eliminar \"" + api.getNombre() + "\"?", "Eliminar API",
                JOptionPane.YES_NO_OPTION);
        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            almacen.eliminar(api.getNombre());
            refrescarLista();
            limpiar();
            lblEstado.setText("Eliminada \"" + api.getNombre() + "\".");
        } catch (IOException ex) {
            lblEstado.setText("No se pudo eliminar: " + ex.getMessage());
        }
    }

    private void cargar() {
        ApiGuardada api = seleccionada();
        if (api == null) {
            lblEstado.setText("Selecciona una API de la lista para cargarla.");
            return;
        }
        panelIA.cargarApi(api);
        irAPestaniaIA.run();
    }

    private void limpiar() {
        lstApis.clearSelection();
        txtNombre.setText("");
        txtModelo.setText("");
        txtClave.setText("");
        proveedorElegido();
    }

    private void seleccionar(String nombre) {
        AlmacenApis almacen = sesion.getApisGuardadas();
        for (int i = 0; i < almacen.getApis().tamanio(); i++) {
            if (almacen.getApis().obtener(i).getNombre().equalsIgnoreCase(nombre)) {
                lstApis.setSelectedIndex(i);
                return;
            }
        }
    }
}
