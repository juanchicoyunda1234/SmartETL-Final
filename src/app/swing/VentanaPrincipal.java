package app.swing;

import app.Sesion;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import java.awt.Dimension;

public class VentanaPrincipal extends JFrame {

    public VentanaPrincipal() {
        super("SmartETL-DS");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Sesion sesion = new Sesion();
        JTabbedPane pestanias = new JTabbedPane();
        pestanias.addTab("Datos", new PanelDatos());
        pestanias.addTab("ETL", new PanelETL(sesion));
        pestanias.addTab("Estructuras", new PanelEstructuras(sesion));
        pestanias.addTab("Grafo", new PanelGrafo(sesion));
        PanelIA panelIA = new PanelIA(sesion);
        pestanias.addTab("IA", panelIA);
        pestanias.addTab("Guardar APIs", new PanelGuardarApis(sesion, panelIA, () -> pestanias.setSelectedComponent(panelIA)));
        add(pestanias);
        setSize(1320, 760);
        setMinimumSize(new Dimension(1150, 600));
        setLocationRelativeTo(null);
    }
}
