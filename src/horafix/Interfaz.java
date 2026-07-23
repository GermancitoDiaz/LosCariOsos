package horafix;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import javax.swing.tree.*;

/**
 * Interfaz gráfica de HoraFix: coordina las cuatro fases del compilador
 * (léxica, sintáctica, semántica y generación de código intermedio) y
 * expone sus resultados en la tabla de tokens, la tabla de símbolos, el
 * catálogo de errores, el árbol sintáctico, la pestaña de código intermedio
 * y el simulador de horario académico.
 */
public class Interfaz extends JFrame {

    // ── Componentes de la interfaz ────────────────────────────────────────────
    private JTextArea  txtEntrada;
    private JTable     tblTokens;
    // Tablas de análisis adicionales (pestañas del panel derecho)
    private JTable     tblErrores;   // catálogo de errores léxicos/sintácticos
    private JTable     tblSimbolos;  // tabla de símbolos reconocidos
    private JTextArea  txtResultados;
    private JButton    btnAnalizar;
    private JButton    btnBorrar;
    private JButton    btnSalir;
    private JButton    btnCargarArchivo;
    private JButton    btnGuardarArchivo;
    private JButton    btnSimulacion;
    private JButton    btnVerArbol;
    // Nodo raíz del último árbol sintáctico generado (null si no hay análisis válido)
    private DefaultMutableTreeNode ultimoArbol;

    // FASE 4 — pestaña de código intermedio (tres direcciones / triplos / cuádruplos)
    private JTextArea  txtTresDirecciones;
    private JTable     tblTriplos;
    private JTable     tblCuadruplos;
    private JButton    btnGuardarIntermedio;
    private List<GeneradorCodigoIntermedio.Instruccion> ultimoCodigoIntermedio = new ArrayList<>();

    // Tokens del último análisis válido (para la simulación)
    private List<Token> ultimosTokens = new ArrayList<>();

    public Interfaz() {
        initComponentes();
        setLocationRelativeTo(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Construcción de la interfaz
    // ─────────────────────────────────────────────────────────────────────────

    private void initComponentes() {
        setTitle("HoraFix — Compilador de Carga Académica");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(920, 660);
        setLayout(new BorderLayout(0, 0));

        add(crearHeader(), BorderLayout.NORTH);
        add(crearCentro(), BorderLayout.CENTER);
        add(crearSur(),    BorderLayout.SOUTH);
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private JPanel crearHeader() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(25, 70, 130));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 10, 14, 10));

        JLabel lblTitulo = new JLabel("HoraFix");
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 26));
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Compilador de Carga Académica — ITT");
        lblSub.setForeground(new Color(170, 205, 255));
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(lblTitulo);
        panel.add(Box.createVerticalStrut(4));
        panel.add(lblSub);

        return panel;
    }

    // ── Centro: entrada + tabla de tokens ────────────────────────────────────

    private JSplitPane crearCentro() {
        JPanel pnlEntrada = new JPanel(new BorderLayout(4, 4));
        pnlEntrada.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                " Código fuente "));

        txtEntrada = new JTextArea();
        txtEntrada.setFont(new Font("Monospaced", Font.PLAIN, 13));
        txtEntrada.setTabSize(4);
        txtEntrada.setLineWrap(false);
        txtEntrada.setMargin(new Insets(6, 8, 6, 8));

        JScrollPane scrollEntrada = new JScrollPane(txtEntrada);
        scrollEntrada.setRowHeaderView(new LineNumberPane(txtEntrada));
        pnlEntrada.add(scrollEntrada, BorderLayout.CENTER);

        // ── Tabla de tokens (pestaña 1) ──────────────────────────────────────
        tblTokens = new JTable();
        tblTokens.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tblTokens.setRowHeight(22);
        tblTokens.setGridColor(new Color(210, 210, 210));
        tblTokens.setDefaultRenderer(Object.class, new TokenCellRenderer());
        tblTokens.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tblTokens.getTableHeader().setBackground(new Color(240, 240, 240));
        tblTokens.setModel(modeloVacio());

        // Tabla del catálogo de errores (pestaña 2)
        tblErrores = new JTable();
        tblErrores.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tblErrores.setRowHeight(22);
        tblErrores.setGridColor(new Color(210, 210, 210));
        tblErrores.setDefaultRenderer(Object.class, new ErrorCellRenderer());
        tblErrores.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tblErrores.getTableHeader().setBackground(new Color(240, 240, 240));
        tblErrores.setModel(crearCatalogoErrores());

        // Tabla de símbolos (pestaña 3)
        tblSimbolos = new JTable();
        tblSimbolos.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tblSimbolos.setRowHeight(22);
        tblSimbolos.setGridColor(new Color(210, 210, 210));
        // Mismos colores que la tabla de tokens; el tipo está en la columna 1
        tblSimbolos.setDefaultRenderer(Object.class, new TokenCellRenderer(1));
        tblSimbolos.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tblSimbolos.getTableHeader().setBackground(new Color(240, 240, 240));
        tblSimbolos.setModel(modeloSimbolosVacio());

        // Pestaña "Tokens": tabla original más su leyenda de colores
        JPanel tabTokens = new JPanel(new BorderLayout(0, 0));
        tabTokens.add(new JScrollPane(tblTokens), BorderLayout.CENTER);
        tabTokens.add(crearLeyenda(), BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("SansSerif", Font.BOLD, 12));
        // Pestaña "Tabla de símbolos" con la misma leyenda que "Tokens"
        JPanel tabSimbolos = new JPanel(new BorderLayout(0, 0));
        tabSimbolos.add(new JScrollPane(tblSimbolos), BorderLayout.CENTER);
        tabSimbolos.add(crearLeyenda(), BorderLayout.SOUTH);

        tabs.addTab("Tokens",              tabTokens);
        tabs.addTab("Tabla de símbolos",   tabSimbolos);
        tabs.addTab("Catálogo de errores", new JScrollPane(tblErrores));
        // FASE 4 — pestaña de código intermedio (tres direcciones / triplos / cuádruplos)
        tabs.addTab("Código Intermedio",   crearTabCodigoIntermedio());

        JPanel pnlDerecho = new JPanel(new BorderLayout(0, 0));
        pnlDerecho.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                " Análisis "));
        pnlDerecho.add(tabs, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pnlEntrada, pnlDerecho);
        split.setResizeWeight(0.45);
        split.setDividerLocation(400);
        split.setBorder(BorderFactory.createEmptyBorder(6, 8, 4, 8));

        return split;
    }

    // ── Pestaña de código intermedio (Fase 4) ─────────────────────────────────

    /**
     * Construye la pestaña "Código Intermedio": tres sub-pestañas que muestran
     * la MISMA lista de instrucciones ({@link GeneradorCodigoIntermedio}) en
     * tres formatos equivalentes — texto de tres direcciones, triplos y
     * cuádruplos — más un botón para exportarla a un archivo .txt.
     *
     * Se genera a partir del mismo código fuente que ya se analiza con el
     * botón "Analizar" (no hay un campo de entrada separado); se llena en
     * {@link #analizar()} solo cuando la sintaxis es válida.
     */
    private JPanel crearTabCodigoIntermedio() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));

        txtTresDirecciones = new JTextArea();
        txtTresDirecciones.setEditable(false);
        txtTresDirecciones.setFont(new Font("Monospaced", Font.PLAIN, 13));
        txtTresDirecciones.setMargin(new Insets(8, 10, 8, 10));
        txtTresDirecciones.setText("Analiza un código fuente válido para generar el código intermedio.");

        tblTriplos = new JTable();
        tblTriplos.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tblTriplos.setRowHeight(22);
        tblTriplos.setGridColor(new Color(210, 210, 210));
        tblTriplos.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tblTriplos.getTableHeader().setBackground(new Color(240, 240, 240));
        tblTriplos.setModel(modeloTriplosVacio());

        tblCuadruplos = new JTable();
        tblCuadruplos.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tblCuadruplos.setRowHeight(22);
        tblCuadruplos.setGridColor(new Color(210, 210, 210));
        tblCuadruplos.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tblCuadruplos.getTableHeader().setBackground(new Color(240, 240, 240));
        tblCuadruplos.setModel(modeloCuadruplosVacio());

        JTabbedPane subTabs = new JTabbedPane(JTabbedPane.TOP);
        subTabs.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subTabs.addTab("Tres direcciones", new JScrollPane(txtTresDirecciones));
        subTabs.addTab("Triplos",          new JScrollPane(tblTriplos));
        subTabs.addTab("Cuádruplos",       new JScrollPane(tblCuadruplos));
        panel.add(subTabs, BorderLayout.CENTER);

        JPanel pnlBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        btnGuardarIntermedio = new JButton("  Guardar como .txt  ");
        btnGuardarIntermedio.setBackground(new Color(60, 179, 113));
        btnGuardarIntermedio.setForeground(Color.WHITE);
        btnGuardarIntermedio.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnGuardarIntermedio.setFocusPainted(false);
        btnGuardarIntermedio.setEnabled(false);
        btnGuardarIntermedio.addActionListener(e -> guardarCodigoIntermedio());
        pnlBoton.add(btnGuardarIntermedio);
        panel.add(pnlBoton, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Genera el código intermedio del último análisis y llena las tres
     * vistas de la pestaña "Código Intermedio". Se invoca desde
     * {@link #analizar()} solo cuando la sintaxis es válida.
     */
    private void poblarCodigoIntermedio(List<Token> tokens) {
        ultimoCodigoIntermedio = GeneradorCodigoIntermedio.generar(tokens);

        txtTresDirecciones.setText(
            GeneradorCodigoIntermedio.comoTextoTresDirecciones(ultimoCodigoIntermedio));
        txtTresDirecciones.setCaretPosition(0);

        DefaultTableModel modeloTriplos = modeloTriplosVacio();
        DefaultTableModel modeloCuadruplos = modeloCuadruplosVacio();
        int n = 1;
        for (GeneradorCodigoIntermedio.Instruccion ins : ultimoCodigoIntermedio) {
            modeloTriplos.addRow(new Object[]{
                "T" + (n++), nvl(ins.resultado()), ins.op(), nvl(ins.arg1()), nvl(ins.arg2())});
            modeloCuadruplos.addRow(new Object[]{
                ins.op(), nvl(ins.arg1()), nvl(ins.arg2()), nvl(ins.resultado())});
        }
        tblTriplos.setModel(modeloTriplos);
        tblCuadruplos.setModel(modeloCuadruplos);

        btnGuardarIntermedio.setEnabled(!ultimoCodigoIntermedio.isEmpty());
    }

    /** Limpia la pestaña de código intermedio (sin análisis válido que mostrar). */
    private void limpiarCodigoIntermedio() {
        ultimoCodigoIntermedio = new ArrayList<>();
        txtTresDirecciones.setText("Analiza un código fuente válido para generar el código intermedio.");
        tblTriplos.setModel(modeloTriplosVacio());
        tblCuadruplos.setModel(modeloCuadruplosVacio());
        btnGuardarIntermedio.setEnabled(false);
    }

    /** {@code null} → cadena vacía, para no imprimir "null" en las tablas. */
    private String nvl(String s) { return s == null ? "" : s; }

    private DefaultTableModel modeloTriplosVacio() {
        return new DefaultTableModel(new String[]{"#", "Resultado", "Operador", "Arg1", "Arg2"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
    }

    private DefaultTableModel modeloCuadruplosVacio() {
        return new DefaultTableModel(new String[]{"Operador", "Arg1", "Arg2", "Resultado"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
    }

    /** Exporta el código de tres direcciones del último análisis a un archivo .txt. */
    private void guardarCodigoIntermedio() {
        if (ultimoCodigoIntermedio.isEmpty()) return;

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar código intermedio");
        fc.setFileFilter(new FileNameExtensionFilter("Archivos de texto (*.txt)", "txt"));
        fc.setSelectedFile(new File("codigo_intermedio.txt"));

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = fc.getSelectedFile();
            if (!archivo.getName().toLowerCase().endsWith(".txt"))
                archivo = new File(archivo.getAbsolutePath() + ".txt");

            if (archivo.exists()) {
                int ok = JOptionPane.showConfirmDialog(this,
                        "El archivo '" + archivo.getName() + "' ya existe.\n¿Deseas reemplazarlo?",
                        "Confirmar sobreescritura", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ok != JOptionPane.YES_OPTION) return;
            }
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8))) {
                writer.write(GeneradorCodigoIntermedio.comoTextoTresDirecciones(ultimoCodigoIntermedio));
                JOptionPane.showMessageDialog(this, "Código intermedio guardado en:\n" + archivo.getAbsolutePath(),
                        "Guardado exitoso", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "No se pudo guardar el archivo:\n" + e.getMessage(),
                        "Error de escritura", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── Sur: botones + área de resultados ────────────────────────────────────

    private JPanel crearSur() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 10, 8));

        JPanel pnlBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));

        btnAnalizar = new JButton("  Analizar  ");
        btnAnalizar.setBackground(new Color(25, 70, 130));
        btnAnalizar.setForeground(Color.WHITE);
        btnAnalizar.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnAnalizar.setFocusPainted(false);

        btnCargarArchivo = new JButton("  Cargar Archivo  ");
        btnCargarArchivo.setBackground(new Color(70, 130, 180));
        btnCargarArchivo.setForeground(Color.WHITE);
        btnCargarArchivo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btnCargarArchivo.setFocusPainted(false);

        btnGuardarArchivo = new JButton("  Guardar Archivo  ");
        btnGuardarArchivo.setBackground(new Color(60, 179, 113));
        btnGuardarArchivo.setForeground(Color.WHITE);
        btnGuardarArchivo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btnGuardarArchivo.setFocusPainted(false);

        btnSimulacion = new JButton("  Simulación  ");
        btnSimulacion.setBackground(new Color(100, 65, 165));
        btnSimulacion.setForeground(Color.WHITE);
        btnSimulacion.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnSimulacion.setFocusPainted(false);
        btnSimulacion.setEnabled(false);

        // Botón para ver el árbol sintáctico en ventana emergente
        btnVerArbol = new JButton("  Ver Árbol  ");
        btnVerArbol.setBackground(new Color(30, 140, 140));
        btnVerArbol.setForeground(Color.WHITE);
        btnVerArbol.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnVerArbol.setFocusPainted(false);
        btnVerArbol.setEnabled(false);

        btnBorrar = new JButton("  Borrar  ");
        btnBorrar.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btnBorrar.setFocusPainted(false);

        btnSalir = new JButton("  Salir  ");
        btnSalir.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btnSalir.setFocusPainted(false);

        pnlBotones.add(btnAnalizar);
        pnlBotones.add(btnCargarArchivo);
        pnlBotones.add(btnGuardarArchivo);
        pnlBotones.add(btnSimulacion);
        pnlBotones.add(btnVerArbol);
        pnlBotones.add(btnBorrar);
        pnlBotones.add(btnSalir);

        txtResultados = new JTextArea(5, 0);
        txtResultados.setEditable(false);
        txtResultados.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtResultados.setBackground(new Color(248, 248, 248));
        txtResultados.setMargin(new Insets(6, 8, 6, 8));

        JScrollPane scrollRes = new JScrollPane(txtResultados);
        scrollRes.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                " Resultados del análisis "));

        panel.add(pnlBotones, BorderLayout.NORTH);
        panel.add(scrollRes,  BorderLayout.CENTER);

        btnAnalizar.addActionListener(e -> analizar());
        btnBorrar.addActionListener(e -> borrar());
        btnSalir.addActionListener(e -> System.exit(0));
        btnCargarArchivo.addActionListener(e -> cargarArchivo());
        btnGuardarArchivo.addActionListener(e -> guardarArchivo());
        btnSimulacion.addActionListener(e -> mostrarSimulacion());
        btnVerArbol.addActionListener(e -> mostrarArbol());

        return panel;
    }

    // ── Leyenda de colores ────────────────────────────────────────────────────

    private JPanel crearLeyenda() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        panel.setBackground(Color.WHITE);

        agregarChip(panel, "RESERVADA",     new Color(198, 230, 255));
        agregarChip(panel, "IDENTIFICADOR", new Color(200, 255, 210));
        agregarChip(panel, "NUMERO",        new Color(255, 250, 200));
        agregarChip(panel, "SEPARADOR",     new Color(232, 232, 232));
        // Solo ERROR_CHAR: las keywords mal escritas se reportan como IDENTIFICADOR
        agregarChip(panel, "ERROR_CHAR", new Color(255, 180, 180)); // rojo — carácter inválido

        return panel;
    }

    private void agregarChip(JPanel panel, String texto, Color color) {
        JLabel chip = new JLabel(" " + texto + " ");
        chip.setOpaque(true);
        chip.setBackground(color);
        chip.setFont(new Font("SansSerif", Font.PLAIN, 10));
        chip.setBorder(BorderFactory.createLineBorder(color.darker(), 1));
        panel.add(chip);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lógica del análisis
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Punto de entrada del pipeline de compilación, invocado al presionar
     * "Analizar". Ejecuta en orden las cuatro fases del compilador —léxica,
     * sintáctica, semántica y generación de código intermedio— y actualiza
     * con sus resultados la tabla de tokens, la tabla de símbolos, el panel
     * de resultados, el resaltado de errores en el editor y la pestaña de
     * código intermedio.
     *
     * El análisis sintáctico se ejecuta siempre, incluso si hubo errores
     * léxicos: {@link Parser} salta los tokens ERROR_CHAR y continúa sobre
     * el resto. El código intermedio, en cambio, solo se genera si la
     * sintaxis resultó válida.
     */
    private void analizar() {
        try {
            String entrada = txtEntrada.getText();

            if (entrada.trim().isEmpty()) {
                txtResultados.setText("⚠  Escribe código fuente antes de analizar.");
                return;
            }

            // ── FASE 1: Análisis léxico ───────────────────────────────────────
            Lexer lexer = new Lexer(new java.io.StringReader(entrada));
            DefaultTableModel modelo = modeloVacio();

            List<Token> listaTokens = new ArrayList<>();
            String tokenStr;
            int erroresLexicos = 0;

            while ((tokenStr = lexer.yylex()) != null) {
                int primeraComa = tokenStr.indexOf(',');
                int ultimaComa  = tokenStr.lastIndexOf(',');
                String tipo     = tokenStr.substring(0, primeraComa);
                String lexema   = tokenStr.substring(primeraComa + 1, ultimaComa);
                String lineaStr = tokenStr.substring(ultimaComa + 1);

                modelo.addRow(new Object[]{tipo, lexema, lineaStr});
                listaTokens.add(new Token(tipo, lexema, Integer.parseInt(lineaStr)));

                // Cuenta los tokens de error para el resumen del análisis
                if (tipo.startsWith("ERROR")) erroresLexicos++;
            }

            tblTokens.setModel(modelo);

            // ── Contador de tokens por tipo ───────────────────────────
            // Cuenta cuántos tokens hay de cada categoría y los muestra en resultados
            java.util.Map<String, Integer> conteo = new java.util.LinkedHashMap<>();
            conteo.put("RESERVADA", 0);
            conteo.put("IDENTIFICADOR", 0);
            conteo.put("NUMERO", 0);
            conteo.put("SEPARADORES", 0);
            conteo.put("ERRORES", 0);

            for (Token t : listaTokens) {
                switch (t.tipo) {
                    case "GUION": case "DOS_PUNTOS": case "COMA":
                        conteo.merge("SEPARADORES", 1, Integer::sum); break;
                    case "ERROR_KEYWORD": case "ERROR_CHAR":
                        conteo.merge("ERRORES", 1, Integer::sum); break;
                    default:
                        conteo.merge(t.tipo, 1, Integer::sum); break;
                }
            }

            // ── FASE 2: ejecutar el parser SIEMPRE, incluso si hay errores léxicos.
            // El parser usa skipErrorTokens() para saltar ERROR_CHAR y seguir analizando.
            List<String> errLexParser = new ArrayList<>();
            List<String> errSintPuros = new ArrayList<>();
            boolean sintaxisOk = false;
            Parser parser = new Parser(listaTokens);
            List<String> erroresSint = parser.analizar();
            ultimoArbol = parser.getRaiz();
            for (String err : erroresSint) {
                if (err.startsWith("Error léxico")) errLexParser.add(err);
                else                               errSintPuros.add(err);
            }

            // ── Construir resultado con sección léxica unificada ─────────────
            StringBuilder resultado = new StringBuilder();
            resultado.append("Tokens encontrados : ").append(listaTokens.size()).append("\n");

            StringBuilder conteoStr = new StringBuilder("Tipos              : ");
            conteo.forEach((k, v) -> { if (v > 0) conteoStr.append(k).append("=").append(v).append("  "); });
            resultado.append(conteoStr).append("\n");

            // Conteo combinado: errores del lexer + errores léxicos detectados por el parser
            int totalLex = erroresLexicos + errLexParser.size();
            resultado.append("Errores léxicos    : ").append(totalLex).append("\n");

            // Mensajes de errores léxicos del lexer (ERROR_CHAR) — vía ErrorManager
            java.util.Set<Integer> lineasLexico = new java.util.HashSet<>();
            for (Token t : listaTokens) {
                if (t.tipo.equals("ERROR_CHAR")) {
                    lineasLexico.add(t.linea);
                    String msg = t.lexema.matches("0[0-9]+")
                        ? ErrorManager.ceroAlInicio(t.linea, t.lexema)
                        : ErrorManager.charInvalido(t.linea, t.lexema);
                    resultado.append("  • ").append(msg).append("\n");
                }
            }

            // Mensajes de errores léxicos detectados por el parser (números, keywords)
            for (String err : errLexParser) {
                resultado.append("  • ").append(err).append("\n");
                extraerLinea(err).ifPresent(lineasLexico::add);
            }

            resultado.append("─".repeat(50)).append("\n");

            // ── Resultado del análisis sintáctico ─────────────────────────────
            java.util.Set<Integer> lineasSintaxis = new java.util.HashSet<>();
            if (!errSintPuros.isEmpty()) {
                resultado.append("Errores sintácticos : ").append(errSintPuros.size()).append("\n");
                for (String err : errSintPuros) {
                    resultado.append("  • ").append(err).append("\n");
                    extraerLinea(err).ifPresent(lineasSintaxis::add);
                }
            }
            if (totalLex == 0 && errSintPuros.isEmpty()) {
                resultado.append("Análisis sintáctico: ✔  Sintaxis correcta");
                sintaxisOk = true;
            } else if (errSintPuros.isEmpty()) {
                resultado.append("Análisis sintáctico: ✔  Estructura sintáctica válida — corrija los errores léxicos");
            }

            // Habilitar botones que requieren sintaxis correcta
            ultimosTokens = listaTokens;
            btnSimulacion.setEnabled(sintaxisOk);
            btnVerArbol.setEnabled(ultimoArbol != null);

            // El catálogo de errores es estático; aquí solo se llena la tabla de símbolos
            poblarTablaSimbolos(listaTokens);

            // ── FASE 3: Análisis semántico ────────────────────────────────────
            // Se ejecuta siempre, independientemente de si hubo errores léxicos
            // o sintácticos, para reportar todos los problemas en una sola pasada.
            java.util.Set<Integer> lineasSemantico = new java.util.HashSet<>();
            List<String> erroresSemanticos = validarSemantica(listaTokens);
            if (!erroresSemanticos.isEmpty()) {
                resultado.append("─".repeat(50)).append("\n");
                resultado.append("Errores semánticos : ").append(erroresSemanticos.size()).append("\n");
                for (String err : erroresSemanticos) {
                    resultado.append("  • ").append(err).append("\n");
                    extraerLinea(err).ifPresent(lineasSemantico::add);
                }
            } else if (sintaxisOk) {
                resultado.append("─".repeat(50)).append("\n");
                resultado.append("Análisis semántico  : ✔  Sin inconsistencias detectadas");
            }

            // ── FASE 4: Código intermedio ─────────────────────────────────────
            // Requiere sintaxis válida (sin ella no hay sentencias completas de
            // las que derivar instrucciones de tres direcciones).
            if (sintaxisOk) {
                poblarCodigoIntermedio(listaTokens);
            } else {
                limpiarCodigoIntermedio();
            }

            txtResultados.setText(resultado.toString());
            // Prioridad de color cuando una línea tiene errores de varias fases:
            // léxico (más grave) > sintáctico > semántico (se pinta primero para
            // que los siguientes lo sobrescriban en caso de solapamiento).
            java.util.Map<Integer, Color> lineasConColor = new java.util.LinkedHashMap<>();
            lineasSemantico.forEach(l -> lineasConColor.put(l, new Color(200, 235, 205)));
            lineasSintaxis.forEach(l  -> lineasConColor.put(l, new Color(255, 224, 178)));
            lineasLexico.forEach(l    -> lineasConColor.put(l, new Color(255, 200, 200)));
            resaltarLineasError(lineasConColor);

        } catch (Exception e) {
            txtResultados.setText("Error inesperado: " + e.getMessage());
        }
    }

    /** Extrae el número de línea (base 1) de un mensaje "... en línea N ...", si lo tiene. */
    private java.util.Optional<Integer> extraerLinea(String mensaje) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("línea (\\d+)").matcher(mensaje);
        return m.find() ? java.util.Optional.of(Integer.parseInt(m.group(1))) : java.util.Optional.empty();
    }

    /**
     * Resalta las líneas del editor que contienen errores, con un color distinto
     * según la fase que los detectó (léxico, sintáctico o semántico) — el mismo
     * código de colores que usa la Tabla de Símbolos y el Catálogo de Errores.
     * Se invoca al final de {@link #analizar()}.
     * Se limpia automáticamente al borrar el código o al re-analizar sin errores.
     *
     * @param lineasConColor mapa línea (base 1) → color de resaltado
     */
    private void resaltarLineasError(java.util.Map<Integer, Color> lineasConColor) {
        txtEntrada.getHighlighter().removeAllHighlights();

        for (var entrada : lineasConColor.entrySet()) {
            javax.swing.text.Highlighter.HighlightPainter painter =
                new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(entrada.getValue());
            try {
                int ini = txtEntrada.getLineStartOffset(entrada.getKey() - 1);
                int fin = txtEntrada.getLineEndOffset(entrada.getKey() - 1);
                txtEntrada.getHighlighter().addHighlight(ini, fin, painter);
            } catch (Exception ignored) {}
        }
    }

    // Muestra el árbol sintáctico en una ventana emergente, con nodos coloreados por tipo
    private void mostrarArbol() {
        if (ultimoArbol == null) return;

        JDialog dialogo = new JDialog(this, "Árbol Sintáctico — HoraFix", true);
        dialogo.setSize(520, 580);
        dialogo.setLocationRelativeTo(this);
        dialogo.setLayout(new BorderLayout(0, 0));

        JTree arbol = new JTree(ultimoArbol);
        arbol.setFont(new Font("SansSerif", Font.PLAIN, 13));
        arbol.setRootVisible(true);
        arbol.setShowsRootHandles(true);
        arbol.setRowHeight(30);
        arbol.setBackground(new Color(245, 245, 245));
        // Renderer personalizado con chips de colores por tipo de nodo
        arbol.setCellRenderer(new ArbolCellRenderer());
        // Expandir todos los nodos al abrir
        for (int i = 0; i < arbol.getRowCount(); i++) arbol.expandRow(i);

        JScrollPane scroll = new JScrollPane(arbol);
        scroll.setBackground(new Color(245, 245, 245));
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                " Estructura sintáctica "));

        JButton btnCerrar = new JButton("  Cerrar  ");
        btnCerrar.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btnCerrar.addActionListener(e -> dialogo.dispose());
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        sur.add(btnCerrar);

        dialogo.add(scroll, BorderLayout.CENTER);
        dialogo.add(sur,    BorderLayout.SOUTH);
        dialogo.setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Renderer del árbol sintáctico — chip de color según tipo de nodo
    // ─────────────────────────────────────────────────────────────────────────

    private static class ArbolCellRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            // Quitar iconos de carpeta/archivo — usamos solo el texto coloreado
            setLeafIcon(null);
            setOpenIcon(null);
            setClosedIcon(null);

            String texto = value.toString();
            setOpaque(true);
            setForeground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));

            if (sel) {
                setBackground(new Color(220, 230, 245));
                setForeground(new Color(25, 70, 130));
                setFont(new Font("SansSerif", Font.BOLD, 12));
                return this;
            }

            // Fondo blanco para todos — el color va en el texto
            setBackground(new Color(250, 250, 250));

            if (texto.equals("programa")) {
                setForeground(new Color(25, 70, 130));
                setFont(new Font("SansSerif", Font.BOLD, 14));

            } else if (texto.startsWith("sent_")) {
                setForeground(new Color(30, 100, 190));
                setFont(new Font("SansSerif", Font.BOLD, 12));

            } else if (texto.equals("lista_materias")) {
                setForeground(new Color(110, 50, 160));
                setFont(new Font("SansSerif", Font.BOLD, 12));

            } else if (texto.contains("→")) {
                // Terminales con valor: ID →, HORARIO →, CREDITOS →, NUMERO →
                setForeground(new Color(30, 130, 60));
                setFont(new Font("Monospaced", Font.PLAIN, 12));

            } else if (texto.equals(texto.toUpperCase())) {
                // Palabras reservadas en mayúsculas
                setForeground(new Color(180, 80, 20));
                setFont(new Font("SansSerif", Font.BOLD, 12));

            } else {
                // Identificadores hoja (nombres de materias)
                setForeground(new Color(15, 120, 120));
                setFont(new Font("Monospaced", Font.PLAIN, 12));
            }

            return this;
        }
    }

    private void borrar() {
        txtEntrada.setText("");
        txtResultados.setText("");
        tblTokens.setModel(modeloVacio());
        // El catálogo de errores es estático y no se borra; la tabla de símbolos sí
        tblSimbolos.setModel(modeloSimbolosVacio());
        txtEntrada.getHighlighter().removeAllHighlights();
        ultimosTokens.clear();
        btnSimulacion.setEnabled(false);
        ultimoArbol = null;
        btnVerArbol.setEnabled(false);
        limpiarCodigoIntermedio();
        // Devolver el foco al área de código para poder escribir de inmediato
        txtEntrada.requestFocusInWindow();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cargar / Guardar archivo
    // ─────────────────────────────────────────────────────────────────────────

    private void cargarArchivo() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar archivo de código fuente");
        fc.setFileFilter(new FileNameExtensionFilter("Archivos de texto (*.txt)", "txt"));

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = fc.getSelectedFile();
            try {
                StringBuilder contenido = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {
                    String linea;
                    while ((linea = reader.readLine()) != null) contenido.append(linea).append("\n");
                }
                borrar();
                txtEntrada.setText(contenido.toString());
                txtEntrada.setCaretPosition(0);
                txtResultados.setText("✔  Archivo cargado: " + archivo.getName()
                        + "  (" + archivo.length() + " bytes)");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "No se pudo leer el archivo:\n" + e.getMessage(),
                        "Error de lectura", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void guardarArchivo() {
        if (txtEntrada.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay contenido para guardar.",
                    "Área vacía", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar código fuente");
        fc.setFileFilter(new FileNameExtensionFilter("Archivos de texto (*.txt)", "txt"));
        fc.setSelectedFile(new File("codigo_horafix.txt"));

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = fc.getSelectedFile();
            if (!archivo.getName().toLowerCase().endsWith(".txt"))
                archivo = new File(archivo.getAbsolutePath() + ".txt");

            if (archivo.exists()) {
                int ok = JOptionPane.showConfirmDialog(this,
                        "El archivo '" + archivo.getName() + "' ya existe.\n¿Deseas reemplazarlo?",
                        "Confirmar sobreescritura", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ok != JOptionPane.YES_OPTION) return;
            }
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8))) {
                writer.write(txtEntrada.getText());
                txtResultados.setText("✔  Archivo guardado:\n   " + archivo.getAbsolutePath());
                JOptionPane.showMessageDialog(this, "Archivo guardado:\n" + archivo.getAbsolutePath(),
                        "Guardado exitoso", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "No se pudo guardar el archivo:\n" + e.getMessage(),
                        "Error de escritura", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Simulación de horario
    // ─────────────────────────────────────────────────────────────────────────

    private void mostrarSimulacion() {
        // ── Extraer datos estructurados de los tokens analizados ──────────────
        List<String[]> materias   = new ArrayList<>();
        List<String>   aprobadas  = new ArrayList<>();
        List<String>   reprobadas = new ArrayList<>();
        List<String>   cursando   = new ArrayList<>();
        List<String>   reglas     = new ArrayList<>();
        int            maxCred    = -1;

        int i = 0;
        while (i < ultimosTokens.size()) {
            Token t = ultimosTokens.get(i);
            if (t.esReservada("MATERIA") && i + 6 < ultimosTokens.size()) {
                // MATERIA nombre horaIni GUION horaFin CREDITOS n → 7 tokens
                materias.add(new String[]{
                    ultimosTokens.get(i + 1).lexema,  // nombre
                    ultimosTokens.get(i + 2).lexema,  // hora inicio
                    ultimosTokens.get(i + 4).lexema,  // hora fin
                    ultimosTokens.get(i + 6).lexema   // créditos
                });
                i += 7;
            } else if (t.esReservada("APROBADAS") || t.esReservada("REPROBADAS") || t.esReservada("CURSANDO")) {
                List<String> target = t.esReservada("APROBADAS") ? aprobadas
                                    : t.esReservada("REPROBADAS") ? reprobadas : cursando;
                i += 2;
                while (i < ultimosTokens.size()) {
                    Token tk = ultimosTokens.get(i);
                    if (tk.tipo.equals("IDENTIFICADOR")) { target.add(tk.lexema); i++; }
                    else if (tk.tipo.equals("COMA"))     { i++; }
                    else break;
                }
            } else if (t.esReservada("MAX_CREDITOS") && i + 2 < ultimosTokens.size()) {
                try { maxCred = Integer.parseInt(ultimosTokens.get(i + 2).lexema); }
                catch (NumberFormatException ignored) {}
                i += 3;
            } else if (t.esReservada("REGLA") && i + 1 < ultimosTokens.size()) {
                reglas.add(ultimosTokens.get(i + 1).lexema);
                i += 2;
            } else {
                i++;
            }
        }

        List<String[]> recomendaciones = generarRecomendaciones(materias, aprobadas, reprobadas, cursando);

        // ── Construir el diálogo ──────────────────────────────────────────────
        JDialog dialog = new JDialog(this, "Simulación de Horario Académico", true);
        dialog.setLayout(new BorderLayout(0, 0));
        dialog.setSize(1010, 700);
        dialog.setLocationRelativeTo(this);

        // Panel encabezado
        JPanel infoPanel = new JPanel();
        infoPanel.setBackground(new Color(25, 70, 130));
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        JLabel lblTitulo = new JLabel("Horario Académico Simulado");
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(lblTitulo);

        StringBuilder sb = new StringBuilder();
        if (maxCred > 0)           sb.append("Máx. créditos: ").append(maxCred).append("  ·  ");
        if (!aprobadas.isEmpty())  sb.append("Aprobadas: ").append(aprobadas.size()).append("  ·  ");
        if (!reprobadas.isEmpty()) sb.append("Reprobadas: ").append(reprobadas.size()).append("  ·  ");
        if (!cursando.isEmpty())   sb.append("Cursando: ").append(cursando.size()).append("  ·  ");
        if (!reglas.isEmpty())     sb.append("Reglas: ").append(String.join(", ", reglas));
        String stats = sb.toString().replaceAll("\\s*·\\s*$", "").trim();
        if (!stats.isEmpty()) {
            JLabel lblStats = new JLabel(stats);
            lblStats.setForeground(new Color(170, 205, 255));
            lblStats.setFont(new Font("SansSerif", Font.PLAIN, 12));
            lblStats.setAlignmentX(Component.LEFT_ALIGNMENT);
            infoPanel.add(Box.createVerticalStrut(4));
            infoPanel.add(lblStats);
        }
        dialog.add(infoPanel, BorderLayout.NORTH);

        // Área central: grilla + recomendaciones
        JPanel areaHorario = new JPanel(new BorderLayout(0, 0));
        if (materias.isEmpty()) {
            areaHorario.add(construirEstadoVacio(), BorderLayout.CENTER);
        } else {
            int minH = Integer.MAX_VALUE, maxH = Integer.MIN_VALUE;
            for (String[] m : materias) {
                try {
                    minH = Math.min(minH, Integer.parseInt(m[1]));
                    maxH = Math.max(maxH, Integer.parseInt(m[2]));
                } catch (NumberFormatException ignored) {}
            }
            if (minH == Integer.MAX_VALUE) { minH = 7; maxH = 20; }

            JScrollPane scroll = new JScrollPane(
                construirGrilla(materias, aprobadas, reprobadas, cursando, minH, maxH));
            scroll.setBorder(null);
            scroll.getViewport().setBackground(Color.WHITE);
            areaHorario.add(scroll, BorderLayout.CENTER);
        }
        areaHorario.add(construirPanelRecomendaciones(recomendaciones), BorderLayout.SOUTH);
        dialog.add(areaHorario, BorderLayout.CENTER);

        // Botón cerrar
        JPanel surPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 8));
        surPanel.setBackground(new Color(245, 247, 250));
        JButton btnCerrar = new JButton("  Cerrar  ");
        btnCerrar.setBackground(new Color(25, 70, 130));
        btnCerrar.setForeground(Color.WHITE);
        btnCerrar.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnCerrar.setFocusPainted(false);
        btnCerrar.addActionListener(e -> dialog.dispose());
        surPanel.add(btnCerrar);
        dialog.add(surPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    // ── Generación de recomendaciones ────────────────────────────────────────

    private List<String[]> generarRecomendaciones(List<String[]> materias,
            List<String> aprobadas, List<String> reprobadas, List<String> cursando) {
        List<String[]> recs = new ArrayList<>();

        // 1. Choques de horario: días en común (según créditos) con horas superpuestas
        for (int a = 0; a < materias.size(); a++) {
            for (int b = a + 1; b < materias.size(); b++) {
                String[] ma = materias.get(a);
                String[] mb = materias.get(b);
                try {
                    int iniA = Integer.parseInt(ma[1]), finA = Integer.parseInt(ma[2]), credA = Integer.parseInt(ma[3]);
                    int iniB = Integer.parseInt(mb[1]), finB = Integer.parseInt(mb[2]), credB = Integer.parseInt(mb[3]);
                    List<String> diasB = diasParaCreditos(credB);
                    for (String dia : diasParaCreditos(credA)) {
                        if (!diasB.contains(dia)) continue;
                        int finEfA = finA + (tieneHoraExtra(credA, dia) ? 1 : 0);
                        int finEfB = finB + (tieneHoraExtra(credB, dia) ? 1 : 0);
                        int solapIni = Math.max(iniA, iniB);
                        int solapFin = Math.min(finEfA, finEfB);
                        if (solapIni < solapFin) {
                            recs.add(new String[]{"error",
                                "Choque de horario: \"" + ma[0].replace("_", " ") + "\" y \""
                                + mb[0].replace("_", " ") + "\" se solapan en " + dia
                                + " " + solapIni + ":00–" + solapFin + ":00. "
                                + "Cambia una de ellas a otro horario o ajusta los créditos."});
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // 2. Materia reprobada con horario asignado
        for (String[] m : materias) {
            if (reprobadas.contains(m[0])) {
                recs.add(new String[]{"warning",
                    "Prioriza inscribir \"" + m[0].replace("_", " ")
                    + "\" (materia reprobada) — es obligatoria para continuar en la carrera."});
            }
        }

        // 3. Materia reprobada sin horario declarado
        for (String rep : reprobadas) {
            boolean tieneHorario = false;
            for (String[] m : materias) { if (m[0].equals(rep)) { tieneHorario = true; break; } }
            if (!tieneHorario) {
                recs.add(new String[]{"warning",
                    "\"" + rep.replace("_", " ") + "\" está reprobada pero no tiene horario asignado — "
                    + "agrega una declaración MATERIA para programarla."});
            }
        }

        // 4. Materia aprobada con horario (posible inscripción innecesaria)
        for (String[] m : materias) {
            if (aprobadas.contains(m[0])) {
                recs.add(new String[]{"info",
                    "\"" + m[0].replace("_", " ") + "\" ya está en APROBADAS — "
                    + "verifica si realmente necesitas inscribirla de nuevo."});
            }
        }

        // 5. Materia en CURSANDO sin declaración MATERIA
        for (String cur : cursando) {
            boolean tieneHorario = false;
            for (String[] m : materias) { if (m[0].equals(cur)) { tieneHorario = true; break; } }
            if (!tieneHorario) {
                recs.add(new String[]{"info",
                    "\"" + cur.replace("_", " ") + "\" está en CURSANDO pero no tiene "
                    + "horario declarado con MATERIA."});
            }
        }

        if (recs.isEmpty()) {
            recs.add(new String[]{"ok",
                "Sin conflictos detectados. Tu carga académica parece estar en orden."});
        }
        return recs;
    }

    private javax.swing.JScrollPane construirPanelRecomendaciones(List<String[]> recs) {
        // Barra de título
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(235, 238, 248));
        titleBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(200, 212, 235)),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        JLabel lblTitulo = new JLabel("  Recomendaciones del Compilador");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblTitulo.setForeground(new Color(35, 48, 100));
        titleBar.add(lblTitulo, BorderLayout.WEST);
        JLabel lblCuenta = new JLabel(recs.size() + " elemento" + (recs.size() != 1 ? "s" : "") + "  ");
        lblCuenta.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblCuenta.setForeground(new Color(110, 120, 150));
        titleBar.add(lblCuenta, BorderLayout.EAST);

        // Filas de recomendaciones
        JPanel filas = new JPanel();
        filas.setBackground(new Color(252, 252, 255));
        filas.setLayout(new BoxLayout(filas, BoxLayout.Y_AXIS));
        filas.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        for (String[] rec : recs) {
            String tipo = rec[0];
            String msg  = rec[1];

            Color bg, fg, accentRec;
            String icon;
            switch (tipo) {
                case "error":
                    bg = new Color(255, 240, 240); fg = new Color(140, 20, 20);
                    accentRec = new Color(210, 60, 60);  icon = "⛔"; break;
                case "warning":
                    bg = new Color(255, 251, 228); fg = new Color(120, 78, 0);
                    accentRec = new Color(230, 165, 30); icon = "⚠"; break;
                case "info":
                    bg = new Color(234, 243, 255); fg = new Color(20, 58, 145);
                    accentRec = new Color(66, 133, 244); icon = "ℹ"; break;
                default:
                    bg = new Color(234, 252, 240); fg = new Color(20, 105, 45);
                    accentRec = new Color(52, 168, 83);  icon = "✔"; break;
            }

            JPanel fila = new JPanel(new BorderLayout(10, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(accentRec);
                    g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                    g2.fillRect(2, 0, 2, getHeight());
                    g2.dispose();
                }
                @Override public boolean isOpaque() { return false; }
            };
            fila.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 10));
            fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            fila.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel lblIcon = new JLabel(icon + " ");
            lblIcon.setFont(new Font("SansSerif", Font.PLAIN, 14));
            JLabel lblMsg = new JLabel("<html><body style='width:700px'>" + msg + "</body></html>");
            lblMsg.setFont(new Font("SansSerif", Font.PLAIN, 12));
            lblMsg.setForeground(fg);

            fila.add(lblIcon, BorderLayout.WEST);
            fila.add(lblMsg, BorderLayout.CENTER);
            filas.add(fila);
            filas.add(Box.createVerticalStrut(4));
        }

        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.add(titleBar, BorderLayout.NORTH);
        wrapper.add(filas, BorderLayout.CENTER);

        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(0, 170));
        scroll.setMinimumSize(new Dimension(0, 170));
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        return scroll;
    }

    // ── Grilla del horario ────────────────────────────────────────────────────

    private JPanel construirGrilla(List<String[]> materias, List<String> aprobadas,
                                   List<String> reprobadas, List<String> cursando,
                                   int minHour, int maxHour) {
        final int TIME_W = 88;
        final int COL_W  = 170;
        final int ROW_H  = 80;
        final int HEAD_H = 54;
        final int PAD    = 5;
        final String[] NOMBRES_DIAS = {"LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES"};

        int numHours = maxHour - minHour;
        int totalW   = TIME_W + 5 * COL_W;
        int totalH   = HEAD_H + numHours * ROW_H;

        JPanel panel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                // Fondo blanco
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Filas alternadas
                for (int h = 0; h < numHours; h++) {
                    int y = HEAD_H + h * ROW_H;
                    g2.setColor(h % 2 == 0 ? new Color(249, 250, 253) : new Color(243, 245, 251));
                    g2.fillRect(TIME_W, y, 5 * COL_W, ROW_H);
                }
                // Líneas de cuadrícula
                g2.setColor(new Color(218, 224, 238));
                for (int h = 0; h <= numHours; h++) {
                    int y = HEAD_H + h * ROW_H;
                    g2.drawLine(0, y, totalW, y);
                }
                for (int d = 0; d <= 5; d++) {
                    int x = TIME_W + d * COL_W;
                    g2.drawLine(x, HEAD_H, x, totalH);
                }
                // Línea azul bajo el header
                g2.setColor(new Color(25, 70, 130));
                g2.fillRect(0, HEAD_H, totalW, 2);
            }
        };
        panel.setPreferredSize(new Dimension(totalW, totalH));
        panel.setOpaque(false);

        // Paleta de colores
        Color[] paleta = {
            new Color(66, 133, 244),
            new Color(52, 168, 83),
            new Color(234, 67, 53),
            new Color(160, 90, 200),
            new Color(251, 140, 0),
            new Color(0, 188, 212),
            new Color(233, 30, 99),
            new Color(0, 150, 136),
        };
        java.util.Map<String, Color> coloresMaterias = new java.util.LinkedHashMap<>();
        int ci = 0;
        for (String[] m : materias) {
            if (!coloresMaterias.containsKey(m[0])) {
                coloresMaterias.put(m[0], paleta[ci % paleta.length]);
                ci++;
            }
        }

        // Esquina superior izquierda
        JLabel lblReloj = new JLabel("⏰", SwingConstants.CENTER);
        lblReloj.setFont(new Font("SansSerif", Font.PLAIN, 22));
        lblReloj.setBackground(new Color(20, 62, 118));
        lblReloj.setForeground(Color.WHITE);
        lblReloj.setOpaque(true);
        lblReloj.setBounds(0, 0, TIME_W, HEAD_H);
        panel.add(lblReloj);

        // Encabezados de días
        for (int d = 0; d < 5; d++) {
            JLabel lbl = new JLabel(NOMBRES_DIAS[d], SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
            lbl.setForeground(Color.WHITE);
            lbl.setBackground(new Color(25, 70, 130));
            lbl.setOpaque(true);
            lbl.setBounds(TIME_W + d * COL_W, 0, COL_W, HEAD_H);
            panel.add(lbl);
        }

        // Etiquetas de hora
        for (int h = 0; h < numHours; h++) {
            int hora = minHour + h;
            int y = HEAD_H + h * ROW_H;
            JLabel lbl = new JLabel(hora + ":00 — " + (hora + 1) + ":00", SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
            lbl.setForeground(new Color(70, 78, 105));
            lbl.setBackground(h % 2 == 0 ? new Color(238, 241, 250) : new Color(230, 234, 246));
            lbl.setOpaque(true);
            lbl.setBounds(0, y, TIME_W, ROW_H);
            panel.add(lbl);
        }

        // ── Tarjetas de materias — se pintan en varios días según los créditos ──
        // Siempre inician en Lunes: 3→Lun-Mié, 4→Lun-Jue, 5 y 6→Lun-Vie
        // (6 créditos agrega 1 hora extra el viernes).
        for (String[] m : materias) {
            String nombre = m[0];
            int inicio, fin, creditos;
            try {
                inicio   = Integer.parseInt(m[1]);
                fin      = Integer.parseInt(m[2]);
                creditos = Integer.parseInt(m[3]);
            } catch (NumberFormatException e) { continue; }
            if (inicio < minHour || fin > maxHour || inicio >= fin) continue;

            Color  acento = coloresMaterias.getOrDefault(nombre, paleta[0]);
            String estado = aprobadas.contains(nombre)  ? "✔ Aprobada"
                          : reprobadas.contains(nombre) ? "✘ Reprobada"
                          : cursando.contains(nombre)   ? "→ Cursando"
                          : null;

            List<String> dias = diasParaCreditos(creditos);
            for (int col = 0; col < dias.size(); col++) {
                String dia = dias.get(col);
                boolean horaExtra  = tieneHoraExtra(creditos, dia);
                int     finEfectivo = horaExtra ? fin + 1 : fin;
                boolean primerDia   = (col == 0);

                int x = TIME_W + col * COL_W + PAD;
                int y = HEAD_H + (inicio - minHour) * ROW_H + PAD;
                int w = COL_W - 2 * PAD;
                int h = (finEfectivo - inicio) * ROW_H - 2 * PAD;

                // Créditos y estado solo se muestran en el día de inicio: evita
                // repetir la misma info 3-5 veces y deja las celdas de 1 hora
                // con espacio suficiente para su contenido (ver crearTarjetaMateria).
                String etiquetaCreds  = primerDia ? creditos + " créditos" : null;
                String estadoMostrado = primerDia ? estado : null;
                JPanel card = crearTarjetaMateria(nombre, inicio, finEfectivo,
                                                  acento, estadoMostrado, etiquetaCreds, horaExtra);
                card.setBounds(x, y, w, h);
                panel.add(card);
            }
        }

        return panel;
    }

    /** Panel centrado que se muestra cuando no hay ninguna declaración MATERIA que dibujar. */
    private JPanel construirEstadoVacio() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setBackground(Color.WHITE);

        JPanel contenido = new JPanel();
        contenido.setBackground(Color.WHITE);
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));

        JLabel lblIcono = new JLabel("🗓");
        lblIcono.setFont(new Font("SansSerif", Font.PLAIN, 40));
        lblIcono.setAlignmentX(Component.CENTER_ALIGNMENT);
        contenido.add(lblIcono);

        contenido.add(Box.createVerticalStrut(10));
        JLabel lblTitulo = new JLabel("Todavía no hay horario que mostrar");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblTitulo.setForeground(new Color(70, 80, 105));
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        contenido.add(lblTitulo);

        contenido.add(Box.createVerticalStrut(4));
        // Mensaje corto (evita depender del wrap de HTML de Swing, que en
        // frases largas puede partir una palabra a la mitad en vez de cortar
        // en el espacio anterior) + un "chip" de ejemplo aparte, en fuente
        // monoespaciada, como si fuera código.
        JLabel lblSub = new JLabel("Agrega al menos una declaración MATERIA");
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSub.setForeground(new Color(150, 155, 170));
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        contenido.add(lblSub);

        contenido.add(Box.createVerticalStrut(12));
        JLabel lblEjemplo = new JLabel("MATERIA calculo 8-10 CREDITOS 4");
        lblEjemplo.setFont(new Font("Monospaced", Font.PLAIN, 12));
        lblEjemplo.setForeground(new Color(70, 80, 105));
        lblEjemplo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 236), 1, true),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        lblEjemplo.setOpaque(true);
        lblEjemplo.setBackground(new Color(248, 249, 252));
        lblEjemplo.setAlignmentX(Component.CENTER_ALIGNMENT);
        contenido.add(lblEjemplo);

        wrap.add(contenido);
        return wrap;
    }

    /** Color del texto de estado según su ícono (✔ aprobada, ✘ reprobada, → cursando). */
    private Color colorParaEstado(String estado) {
        if (estado.contains("✔")) return new Color(25, 120, 40);
        if (estado.contains("✘")) return new Color(170, 30, 30);
        return new Color(55, 90, 190);
    }

    /**
     * Construye la tarjeta visual de una materia dentro de un día de la grilla.
     *
     * El contenido se mantiene deliberadamente compacto: estado y créditos se
     * combinan en una sola línea (en vez de apilarse por separado) para que la
     * tarjeta quepa sin recortarse incluso en celdas de una sola hora.
     */
    private JPanel crearTarjetaMateria(String nombre, int inicio, int fin,
                                       Color acento, String estado,
                                       String etiquetaCreds, boolean horaExtra) {
        Color bgCard = new Color(
            Math.min(255, acento.getRed()   / 10 + 245),
            Math.min(255, acento.getGreen() / 10 + 245),
            Math.min(255, acento.getBlue()  / 10 + 245));
        Color borderColor = new Color(
            Math.min(255, acento.getRed()   / 3 + 168),
            Math.min(255, acento.getGreen() / 3 + 168),
            Math.min(255, acento.getBlue()  / 3 + 168));

        final int RADIO = 11;
        final int SOMBRA = 2;

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                // El área visible se encoge SOMBRA px para dejar sitio a una
                // sombra sutil dentro de los mismos bounds del componente —
                // así no invade la celda vecina ni el padding (PAD) de la grilla.
                int w = getWidth() - SOMBRA, h = getHeight() - SOMBRA;

                Graphics2D gs = (Graphics2D) g.create();
                gs.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gs.setColor(new Color(20, 30, 60, 28));
                gs.fillRoundRect(SOMBRA, SOMBRA, w, h, RADIO, RADIO);
                gs.dispose();

                // Contexto A: fondo + barra de acento.
                // g2.clip() INTERSECTA con el clip del JScrollPane, nunca lo elimina.
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgCard);
                g2.fillRoundRect(0, 0, w, h, RADIO, RADIO);
                g2.clip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, RADIO, RADIO));
                g2.setColor(acento);
                g2.fillRect(0, 0, 5, h);
                g2.dispose();
                // Contexto B: borde (sin modificar el clip)
                Graphics2D g3 = (Graphics2D) g.create();
                g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g3.setColor(borderColor);
                g3.setStroke(new java.awt.BasicStroke(1f));
                g3.drawRoundRect(0, 0, w - 1, h - 1, RADIO, RADIO);
                g3.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(7, 14, 8, 10));

        // Nombre — envuelto en HTML con ancho fijo para que los nombres largos
        // pasen a una segunda línea en vez de recortarse contra el borde.
        JLabel lblNombre = new JLabel(
            "<html><body style='width:120px'><b>" + nombre.replace("_", " ") + "</b></body></html>");
        lblNombre.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblNombre.setForeground(new Color(22, 30, 60));
        lblNombre.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblNombre);

        // Estado y créditos van en una sola línea de texto plano y abreviado:
        // en celdas de una hora no hay espacio vertical para dos líneas, y el
        // motor HTML de Swing no ajusta bien texto largo con emoji.
        if (estado != null || etiquetaCreds != null) {
            String creditosCortos = etiquetaCreds == null ? null
                : etiquetaCreds.replace(" créditos", " cr.");
            String texto = (estado != null && creditosCortos != null) ? estado + " · " + creditosCortos
                         : estado != null ? estado
                         : "📚 " + etiquetaCreds;
            JLabel lblInfo = new JLabel(texto);
            lblInfo.setFont(new Font("SansSerif", Font.BOLD, 10));
            lblInfo.setForeground(estado != null ? colorParaEstado(estado) : new Color(80, 60, 160));
            lblInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(Box.createVerticalStrut(3));
            card.add(lblInfo);
        }

        // Hora extra para 6 créditos — indicador visual en viernes (esa celda
        // siempre abarca 2 horas, así que hay espacio de sobra para esta línea).
        if (horaExtra) {
            JLabel lblExtra = new JLabel("⏱ +1h extra");
            lblExtra.setFont(new Font("SansSerif", Font.BOLD, 10));
            lblExtra.setForeground(new Color(180, 60, 20));
            lblExtra.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(Box.createVerticalStrut(2));
            card.add(lblExtra);
        }

        card.add(Box.createVerticalStrut(5));
        JLabel lblHora = new JLabel(inicio + ":00 – " + fin + ":00"
            + (horaExtra ? " (+1h)" : ""));
        lblHora.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblHora.setForeground(new Color(90, 100, 130));
        lblHora.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblHora);

        return card;
    }

    /** Días de la semana disponibles, en orden desde Lunes. */
    private static final String[] DIAS_SEMANA = {"LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES"};

    /**
     * Calcula los días de clase que ocupa una materia según sus créditos.
     * Cada crédito equivale a una hora de clase en un día distinto, siempre
     * empezando en Lunes: 3 créditos → Lun-Mié, 4 → Lun-Jue, 5 y 6 → Lun-Vie.
     *
     * @param creditos cantidad de créditos de la materia (3–6)
     * @return lista de días de clase, en orden desde Lunes
     */
    private List<String> diasParaCreditos(int creditos) {
        int numDias = Math.min(Math.max(creditos, 1), 5);
        return java.util.Arrays.asList(DIAS_SEMANA).subList(0, numDias);
    }

    /** Con 6 créditos el viernes tiene 1 hora extra de clase (el "crédito sobrante"). */
    private boolean tieneHoraExtra(int creditos, String dia) {
        return creditos == 6 && dia.equals("VIERNES");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilidades
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Construye el modelo de datos del <b>Catálogo de Errores</b> (pestaña estática).
     *
     * Cada fila describe un código de error del compilador con su categoría,
     * descripción y un ejemplo. Los códigos deben coincidir con los definidos
     * en {@link ErrorManager.CodigoLexico} y {@link ErrorManager.CodigoSintactico}.
     * Este catálogo no cambia entre análisis — se crea una sola vez en el constructor.
     *
     * @return modelo de tabla con columnas: Código | Categoría | Descripción | Ejemplo
     */
    private DefaultTableModel crearCatalogoErrores() {
        DefaultTableModel m = new DefaultTableModel(
                new String[]{"Código", "Categoría", "Descripción", "Ejemplo"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        // ── Errores léxicos ──────────────────────────────────────────────────
        m.addRow(new Object[]{"E-L01", "Léxico",
            "Carácter(es) no válido(s) en el código fuente",
            "@, #, $, %, &"});
        m.addRow(new Object[]{"E-L02", "Léxico",
            "Número con ceros al inicio (no permitido)",
            "007, 030, 001"});
        m.addRow(new Object[]{"E-L03", "Léxico",
            "Palabra reservada mal escrita (sugerencia disponible)",
            "APROBDA, MATIRIA, CURSAND"});
        m.addRow(new Object[]{"E-L04", "Léxico",
            "Valor no numérico donde se espera un número de hora o créditos",
            "ocho, diez, veinte"});
        // ── Errores sintácticos ──────────────────────────────────────────────
        m.addRow(new Object[]{"E-S01", "Sintáctico",
            "Instrucción desconocida al inicio de sentencia",
            "xyz : materia1"});
        m.addRow(new Object[]{"E-S02", "Sintáctico",
            "Lista de materias vacía después de ':'",
            "APROBADAS:"});
        m.addRow(new Object[]{"E-S03", "Sintáctico",
            "Coma final sin identificador de materia",
            "APROBADAS: mat1, mat2,"});
        m.addRow(new Object[]{"E-S04", "Sintáctico",
            "Sentencia incompleta al final del archivo",
            "MATERIA algebra"});
        m.addRow(new Object[]{"E-S05", "Sintáctico",
            "Token inesperado dentro de una sentencia",
            "MATERIA 123 8-10 CREDITOS 4"});
        m.addRow(new Object[]{"E-S06", "Sintáctico",
            "Palabra reservada en posición incorrecta",
            "APROBADAS CURSANDO: mat1"});
        m.addRow(new Object[]{"E-S07", "Sintáctico",
            "Se esperaba ':' después de palabra reservada",
            "APROBADAS mat1"});
        m.addRow(new Object[]{"E-S08", "Sintáctico",
            "Se esperaba un identificador de materia",
            "APROBADAS: 123"});
        m.addRow(new Object[]{"E-S09", "Sintáctico",
            "Se esperaba un número de hora válido",
            "MATERIA algebra ocho-10 CREDITOS 4"});
        m.addRow(new Object[]{"E-S10", "Sintáctico",
            "Se esperaba '-' para separar hora inicio y fin",
            "MATERIA algebra 8 10 CREDITOS 4"});
        // ── Errores semánticos ───────────────────────────────────────────────
        m.addRow(new Object[]{"E-M01", "Semántico",
            "Choque de horario entre dos materias (días en común según créditos) — requiere REGLA no_choques",
            "REGLA no_choques / MATERIA calculo 8-10 CREDITOS 5 / MATERIA fisica 9-11 CREDITOS 3"});
        m.addRow(new Object[]{"E-M02", "Semántico",
            "Materia reprobada sin horario asignado",
            "REPROBADAS: fisica — sin declaración MATERIA fisica"});
        m.addRow(new Object[]{"E-M03", "Semántico",
            "Materia aprobada re-inscrita innecesariamente — requiere REGLA no_reinscribir_aprobadas",
            "REGLA no_reinscribir_aprobadas / APROBADAS: calculo / MATERIA calculo 8-10 CREDITOS 4"});
        m.addRow(new Object[]{"E-M04", "Semántico",
            "Materia en CURSANDO sin horario declarado",
            "CURSANDO: algebra — sin declaración MATERIA algebra"});
        m.addRow(new Object[]{"E-M05", "Semántico",
            "Total de créditos supera MAX_CREDITOS",
            "MAX_CREDITOS: 10 con 4 materias de 6 créditos c/u"});
        m.addRow(new Object[]{"E-M06", "Semántico",
            "Horario fuera del rango académico válido (7:00–21:00)",
            "MATERIA calculo 3-5 CREDITOS 4"});
        m.addRow(new Object[]{"E-M07", "Semántico",
            "Hora de fin menor o igual a la hora de inicio",
            "MATERIA calculo 10-8 CREDITOS 4"});
        m.addRow(new Object[]{"E-M08", "Semántico",
            "Sin materias declaradas para generar el plan",
            "Solo APROBADAS y REPROBADAS sin ningún MATERIA"});
        m.addRow(new Object[]{"E-M09", "Semántico",
            "Créditos fuera del rango válido (3–6)",
            "MATERIA calculo 8-10 CREDITOS 7"});
        m.addRow(new Object[]{"E-M10", "Semántico",
            "Total de créditos insuficiente (menos del 50% de MAX_CREDITOS)",
            "MAX_CREDITOS: 30 con solo 1 materia de 3 créditos"});
        m.addRow(new Object[]{"E-M11", "Semántico",
            "MAX_CREDITOS fuera del rango institucional válido (20–35)",
            "MAX_CREDITOS: 40"});
        m.addRow(new Object[]{"E-M12", "Semántico",
            "Materia con estado contradictorio (aparece en más de una lista)",
            "APROBADAS: calculo / REPROBADAS: calculo"});
        m.addRow(new Object[]{"E-M13", "Semántico",
            "Materia declarada más de una vez con MATERIA",
            "MATERIA calculo 8-10 CREDITOS 4 / MATERIA calculo 14-16 CREDITOS 3"});
        m.addRow(new Object[]{"E-M14", "Semántico",
            "Seriación incumplida (SERIACION) — requiere REGLA respetar_seriacion",
            "REGLA respetar_seriacion / SERIACION calculo2 : calculo1 / CURSANDO: calculo2"});
        m.addRow(new Object[]{"E-M15", "Semántico",
            "REGLA con nombre desconocido (sugerencia disponible)",
            "REGLA no_choke"});
        return m;
    }

    /** Declaración MATERIA ya resuelta: nombre, horario, créditos y línea de origen. */
    private record MateriaInfo(String nombre, int horaInicio, int horaFin, int creditos, int linea) {}

    /** Un identificador dentro de APROBADAS/REPROBADAS/CURSANDO, con su línea de origen. */
    private record Ocurrencia(String nombre, int linea) {}

    /** Declaración SERIACION: materia dependiente y la lista de sus prerequisitos. */
    private record Seriacion(String materia, int linea, List<Ocurrencia> prerequisitos) {}

    /** Nombres de REGLA reconocidos por el análisis semántico y el efecto que activan. */
    private static final java.util.Set<String> REGLAS_CONOCIDAS = java.util.Set.of(
        "no_choques", "no_reinscribir_aprobadas", "respetar_seriacion");

    /**
     * Análisis semántico (fase 3). Recorre los tokens del último análisis
     * y verifica restricciones de negocio académico que la gramática no
     * puede capturar por sí sola. Todos los mensajes incluyen número de
     * línea para que la interfaz los resalte en el editor, igual que los
     * errores léxicos y sintácticos.
     *
     *   E-M01 Choques de horario entre materias con días en común (según créditos) — requiere REGLA no_choques
     *   E-M02 Materia reprobada sin horario declarado
     *   E-M03 Materia aprobada que se intenta re-inscribir — requiere REGLA no_reinscribir_aprobadas
     *   E-M04 Materia en cursando sin horario declarado
     *   E-M05 Total de créditos supera MAX_CREDITOS
     *   E-M06 Hora fuera del rango válido (7–21)
     *   E-M07 Hora de fin <= hora de inicio
     *   E-M08 No hay ninguna declaración MATERIA
     *   E-M09 Créditos fuera del rango válido (3–6)
     *   E-M10 Total de créditos insuficiente respecto a MAX_CREDITOS
     *   E-M11 MAX_CREDITOS fuera del rango institucional válido (20–35)
     *   E-M12 Materia con estado contradictorio (aprobada/reprobada/cursando a la vez)
     *   E-M13 Materia declarada más de una vez con MATERIA
     *   E-M14 Seriación incumplida (SERIACION) — requiere REGLA respetar_seriacion
     *   E-M15 REGLA con nombre desconocido, con sugerencia por distancia de Levenshtein
     *
     * E-M01, E-M03 y E-M14 son "reglas activables": solo se validan si el programa
     * declara la REGLA correspondiente ({@link #REGLAS_CONOCIDAS}). El resto son
     * invariantes estructurales de los datos y siempre se validan.
     *
     * @param tokens lista completa de tokens del último análisis léxico
     * @return lista de mensajes de error semántico; vacía si no hay errores
     */
    private List<String> validarSemantica(List<Token> tokens) {
        List<String> errores = new ArrayList<>();

        // ── Extraer datos estructurados de los tokens, con línea de origen ────
        List<MateriaInfo>   materias      = new ArrayList<>();
        List<Ocurrencia>    aprobadas     = new ArrayList<>();
        List<Ocurrencia>    reprobadas    = new ArrayList<>();
        List<Ocurrencia>    cursando      = new ArrayList<>();
        List<Ocurrencia>    reglas        = new ArrayList<>();
        List<Seriacion>     seriaciones   = new ArrayList<>();
        int  maxCred      = -1;
        int  maxCredLinea = -1;

        int i = 0;
        while (i < tokens.size()) {
            Token t = tokens.get(i);

            if (t.esReservada("MATERIA") && i + 6 < tokens.size()) {
                // MATERIA nombre numero GUION numero CREDITOS numero → 7 tokens
                try {
                    materias.add(new MateriaInfo(
                        tokens.get(i + 1).lexema,
                        Integer.parseInt(tokens.get(i + 2).lexema),
                        Integer.parseInt(tokens.get(i + 4).lexema),  // i+3 = GUION
                        Integer.parseInt(tokens.get(i + 6).lexema),  // i+5 = RESERVADA "CREDITOS"
                        t.linea));
                } catch (NumberFormatException ignored) {}
                i += 7;

            } else if (t.esReservada("APROBADAS")
                    || t.esReservada("REPROBADAS")
                    || t.esReservada("CURSANDO")) {
                List<Ocurrencia> target = t.esReservada("APROBADAS")  ? aprobadas
                                        : t.esReservada("REPROBADAS") ? reprobadas
                                        : cursando;
                i += 2; // saltar RESERVADA y DOS_PUNTOS
                while (i < tokens.size()) {
                    Token tk = tokens.get(i);
                    if (tk.tipo.equals("IDENTIFICADOR")) { target.add(new Ocurrencia(tk.lexema, tk.linea)); i++; }
                    else if (tk.tipo.equals("COMA"))     { i++; }
                    else break;
                }

            } else if (t.esReservada("MAX_CREDITOS") && i + 2 < tokens.size()) {
                try {
                    maxCred = Integer.parseInt(tokens.get(i + 2).lexema);
                    maxCredLinea = t.linea;
                } catch (NumberFormatException ignored) {}
                i += 3;

            } else if (t.esReservada("REGLA") && i + 1 < tokens.size()) {
                reglas.add(new Ocurrencia(tokens.get(i + 1).lexema, t.linea));
                i += 2;

            } else if (t.esReservada("SERIACION") && i + 2 < tokens.size()) {
                // SERIACION nombre DOS_PUNTOS prereq (',' prereq)* → nombre en i+1, prereqs desde i+3
                String materiaDep = tokens.get(i + 1).lexema;
                int lineaDep = t.linea;
                i += 3;
                List<Ocurrencia> prereqs = new ArrayList<>();
                while (i < tokens.size()) {
                    Token tk = tokens.get(i);
                    if (tk.tipo.equals("IDENTIFICADOR")) { prereqs.add(new Ocurrencia(tk.lexema, tk.linea)); i++; }
                    else if (tk.tipo.equals("COMA"))     { i++; }
                    else break;
                }
                seriaciones.add(new Seriacion(materiaDep, lineaDep, prereqs));

            } else {
                i++;
            }
        }

        // ── E-M08: sin materias declaradas ───────────────────────────────────
        if (materias.isEmpty()) {
            errores.add(ErrorManager.sinMaterias());
            return errores; // sin materias las demás validaciones no aplican
        }

        // ── E-M13: materia declarada más de una vez con MATERIA ──────────────
        java.util.Map<String, Integer> primeraDeclaracion = new java.util.LinkedHashMap<>();
        for (MateriaInfo m : materias) {
            Integer previa = primeraDeclaracion.putIfAbsent(m.nombre(), m.linea());
            if (previa != null)
                errores.add(ErrorManager.materiaDuplicada(m.nombre(), previa, m.linea()));
        }

        // ── E-M15: REGLA con nombre desconocido ──────────────────────────────
        java.util.Set<String> reglasActivas = new java.util.HashSet<>();
        for (Ocurrencia r : reglas) {
            reglasActivas.add(r.nombre());
            if (!REGLAS_CONOCIDAS.contains(r.nombre())) {
                errores.add(ErrorManager.reglaDesconocida(r.linea(), r.nombre(), sugerirRegla(r.nombre())));
            }
        }

        // ── E-M09: créditos fuera del rango válido (3–6) ─────────────────────
        for (MateriaInfo m : materias) {
            if (m.creditos() < 3 || m.creditos() > 6)
                errores.add(ErrorManager.creditosFueraDeRango(m.nombre(), m.creditos(), m.linea()));
        }

        // ── E-M06 y E-M07: rango horario inválido ────────────────────────────
        for (MateriaInfo m : materias) {
            if (m.horaInicio() < 7 || m.horaInicio() > 21)
                errores.add(ErrorManager.horarioFueraDeRango(m.nombre(), m.horaInicio(), m.linea()));
            else if (m.horaFin() < 7 || m.horaFin() > 21)
                errores.add(ErrorManager.horarioFueraDeRango(m.nombre(), m.horaFin(), m.linea()));
            if (m.horaFin() <= m.horaInicio())
                errores.add(ErrorManager.horaFinInvalida(m.nombre(), m.horaInicio(), m.horaFin(), m.linea()));
        }

        // ── E-M01: choques de horario (días en común según créditos) ──────────
        // Solo se valida si se declaró REGLA no_choques.
        if (reglasActivas.contains("no_choques")) {
            for (int a = 0; a < materias.size(); a++) {
                for (int b = a + 1; b < materias.size(); b++) {
                    MateriaInfo ma = materias.get(a);
                    MateriaInfo mb = materias.get(b);
                    List<String> diasB = diasParaCreditos(mb.creditos());
                    for (String dia : diasParaCreditos(ma.creditos())) {
                        if (!diasB.contains(dia)) continue;
                        int finEfA = ma.horaFin() + (tieneHoraExtra(ma.creditos(), dia) ? 1 : 0);
                        int finEfB = mb.horaFin() + (tieneHoraExtra(mb.creditos(), dia) ? 1 : 0);
                        int solapIni = Math.max(ma.horaInicio(), mb.horaInicio());
                        int solapFin = Math.min(finEfA, finEfB);
                        if (solapIni < solapFin)
                            errores.add(ErrorManager.choqueHorario(
                                ma.nombre(), ma.linea(), mb.nombre(), mb.linea(), dia, solapIni, solapFin));
                    }
                }
            }
        }

        // ── E-M02: reprobada sin horario ──────────────────────────────────────
        for (Ocurrencia rep : reprobadas) {
            boolean tieneHorario = materias.stream().anyMatch(m -> m.nombre().equals(rep.nombre()));
            if (!tieneHorario)
                errores.add(ErrorManager.reprobadaSinHorario(rep.nombre(), rep.linea()));
        }

        // ── E-M03: aprobada re-inscrita ───────────────────────────────────────
        // Solo se valida si se declaró REGLA no_reinscribir_aprobadas.
        java.util.Set<String> nombresAprobadas = new java.util.HashSet<>();
        for (Ocurrencia ap : aprobadas) nombresAprobadas.add(ap.nombre());
        if (reglasActivas.contains("no_reinscribir_aprobadas")) {
            for (MateriaInfo m : materias)
                if (nombresAprobadas.contains(m.nombre()))
                    errores.add(ErrorManager.aprobadaReInscrita(m.nombre(), m.linea()));
        }

        // ── E-M14: seriación incumplida ────────────────────────────────────────
        // Solo se valida si se declaró REGLA respetar_seriacion.
        if (reglasActivas.contains("respetar_seriacion")) {
            for (Seriacion s : seriaciones) {
                boolean seCursaAhora = cursando.stream().anyMatch(o -> o.nombre().equals(s.materia()))
                        || materias.stream().anyMatch(m -> m.nombre().equals(s.materia()));
                if (!seCursaAhora) continue;
                for (Ocurrencia prereq : s.prerequisitos()) {
                    if (!nombresAprobadas.contains(prereq.nombre()))
                        errores.add(ErrorManager.seriacionIncumplida(s.materia(), prereq.nombre(), prereq.linea()));
                }
            }
        }

        // ── E-M04: cursando sin horario ───────────────────────────────────────
        for (Ocurrencia cur : cursando) {
            boolean tieneHorario = materias.stream().anyMatch(m -> m.nombre().equals(cur.nombre()));
            if (!tieneHorario)
                errores.add(ErrorManager.cursandoSinHorario(cur.nombre(), cur.linea()));
        }

        // ── E-M12: materia con estado contradictorio (2+ listas a la vez) ─────
        java.util.Set<String> yaReportadas = new java.util.HashSet<>();
        for (Ocurrencia oc : aprobadas) {
            String nombre = oc.nombre();
            if (!yaReportadas.add(nombre)) continue;
            List<String> estados = new ArrayList<>();
            if (nombresAprobadas.contains(nombre))                                estados.add("APROBADAS");
            if (reprobadas.stream().anyMatch(o -> o.nombre().equals(nombre)))     estados.add("REPROBADAS");
            if (cursando.stream().anyMatch(o -> o.nombre().equals(nombre)))       estados.add("CURSANDO");
            if (estados.size() > 1)
                errores.add(ErrorManager.estadoContradictorio(nombre, String.join(", ", estados)));
        }
        for (Ocurrencia oc : reprobadas) {
            String nombre = oc.nombre();
            if (!yaReportadas.add(nombre)) continue;
            boolean enCursando = cursando.stream().anyMatch(o -> o.nombre().equals(nombre));
            if (enCursando)
                errores.add(ErrorManager.estadoContradictorio(nombre, "REPROBADAS, CURSANDO"));
        }

        // ── E-M11: MAX_CREDITOS fuera del rango institucional válido (20–35) ──────
        if (maxCred != -1 && (maxCred < 20 || maxCred > 35)) {
            errores.add(ErrorManager.maxCreditosFueraDeRango(maxCred, maxCredLinea));
        } else if (maxCred != -1) {
            // ── E-M05 / E-M10: total de créditos fuera de rango respecto a MAX_CREDITOS ──
            // Solo se compara contra MAX_CREDITOS si su valor declarado es válido.
            int totalCreditos = 0;
            for (MateriaInfo m : materias) totalCreditos += m.creditos();
            if (totalCreditos > maxCred) {
                errores.add(ErrorManager.excesoCreditos(totalCreditos, maxCred, maxCredLinea));
            } else if (totalCreditos * 2 < maxCred) {
                // Insuficiente si el total inscrito es menos del 50% de MAX_CREDITOS
                errores.add(ErrorManager.creditosInsuficientes(totalCreditos, maxCred, maxCredLinea));
            }
        }

        return errores;
    }

    /**
     * Busca el nombre de regla conocido más parecido a {@code nombre} usando
     * distancia de Levenshtein, para sugerirlo en el mensaje E-M15 (ej: "no_choke" → "no_choques").
     *
     * @param nombre nombre de regla tal como fue escrito por el estudiante
     * @return la regla conocida más cercana si la distancia es aceptable, {@code null} si no hay
     */
    private String sugerirRegla(String nombre) {
        String mejorSugerencia = null;
        int mejorDistancia = Integer.MAX_VALUE;
        for (String regla : REGLAS_CONOCIDAS) {
            int dist = levenshtein(nombre, regla);
            int umbral = Math.max(2, regla.length() / 3);
            if (dist <= umbral && dist < mejorDistancia) {
                mejorDistancia = dist;
                mejorSugerencia = regla;
            }
        }
        return mejorSugerencia;
    }

    /** Distancia de Levenshtein entre {@code a} y {@code b}, usada por {@link #sugerirRegla(String)}. */
    private static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        return dp[a.length()][b.length()];
    }

    /** Crea un modelo vacío con las columnas de la Tabla de Símbolos. */
    private DefaultTableModel modeloSimbolosVacio() {
        return new DefaultTableModel(new String[]{"Símbolo", "Tipo", "Ocurrencias"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
    }

    /**
     * Llena la <b>Tabla de Símbolos</b> con los tokens del último análisis.
     *
     * Agrupa por par (lexema, tipo) y cuenta cuántas veces aparece cada símbolo.
     * Los tokens de tipo ERROR_CHAR se excluyen — la tabla solo muestra símbolos válidos.
     * El orden de aparición en el código fuente se preserva (LinkedHashMap).
     *
     * @param tokens lista completa de tokens del último análisis léxico
     */
    private void poblarTablaSimbolos(List<Token> tokens) {
        // LinkedHashMap preserva el orden de primera aparición
        java.util.LinkedHashMap<String, int[]> simb = new java.util.LinkedHashMap<>();
        for (Token t : tokens) {
            if (t.tipo.startsWith("ERROR")) continue;
            String key = t.lexema + "\0" + t.tipo;
            simb.computeIfAbsent(key, k -> new int[]{0})[0]++;
        }
        DefaultTableModel m = modeloSimbolosVacio();
        for (java.util.Map.Entry<String, int[]> e : simb.entrySet()) {
            String[] partes = e.getKey().split("\0", 2);
            m.addRow(new Object[]{partes[0], partes[1], e.getValue()[0]});
        }
        tblSimbolos.setModel(m);
    }

    private DefaultTableModel modeloVacio() {
        return new DefaultTableModel(new String[]{"TOKEN", "LEXEMA", "LÍNEA"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Renderer: colorea cada fila según el tipo de token
    // ─────────────────────────────────────────────────────────────────────────

    private static class TokenCellRenderer extends DefaultTableCellRenderer {
        // Columna de donde se lee el tipo de token para aplicar el color:
        // la tabla de tokens usa la columna 0, la de símbolos la columna 1.
        private final int tipoCol;
        TokenCellRenderer()         { this(0); }
        TokenCellRenderer(int col)  { this.tipoCol = col; }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                String tipo = String.valueOf(table.getValueAt(row, tipoCol));
                switch (tipo) {
                    case "RESERVADA":     c.setBackground(new Color(198, 230, 255)); break;
                    case "IDENTIFICADOR": c.setBackground(new Color(200, 255, 210)); break;
                    case "NUMERO":        c.setBackground(new Color(255, 250, 200)); break;
                    case "GUION":
                    case "DOS_PUNTOS":
                    case "COMA":          c.setBackground(new Color(232, 232, 232)); break;
                    case "ERROR_CHAR":    c.setBackground(new Color(255, 180, 180)); break; // rojo
                    default:              c.setBackground(Color.WHITE);              break;
                }
            }
            return c;
        }
    }

    /** Colorea las filas del catálogo de errores según su categoría (léxico, sintáctico, semántico). */
    private static class ErrorCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                String cat = String.valueOf(table.getValueAt(row, 1));
                switch (cat) {
                    case "Léxico":     c.setBackground(new Color(255, 220, 220)); break; // rojo suave
                    case "Sintáctico": c.setBackground(new Color(255, 242, 200)); break; // naranja suave
                    case "Semántico":  c.setBackground(new Color(220, 242, 220)); break; // verde suave
                    default:           c.setBackground(Color.WHITE);              break;
                }
            }
            return c;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Panel de números de línea
    // ─────────────────────────────────────────────────────────────────────────

    private static class LineNumberPane extends JComponent {
        private final JTextArea textArea;

        LineNumberPane(JTextArea textArea) {
            this.textArea = textArea;
            setPreferredSize(new Dimension(36, 0));
            setBackground(new Color(240, 240, 240));
            setOpaque(true);
            textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e)  { repaint(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e)  { repaint(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(240, 240, 240));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setFont(textArea.getFont());
            g.setColor(new Color(120, 120, 120));

            // modelToView2D da la coordenada Y exacta de cada línea, respetando
            // los insets del JTextArea (margin top = 6 px). Usar lineHeight * i en
            // su lugar desalinea los números porque ignora el margen superior.
            FontMetrics fm = g.getFontMetrics();
            int lineCount  = textArea.getLineCount();
            Rectangle clip = g.getClipBounds();

            for (int i = 0; i < lineCount; i++) {
                try {
                    int offset = textArea.getLineStartOffset(i);
                    java.awt.geom.Rectangle2D rect = textArea.modelToView2D(offset);
                    if (rect == null) continue;
                    int y = (int) rect.getY() + fm.getAscent();
                    // Solo dibujar si está dentro del área de repintado visible
                    if (clip != null && (y < clip.y - fm.getHeight() || y > clip.y + clip.height)) continue;
                    String num = String.valueOf(i + 1);
                    int x = getWidth() - fm.stringWidth(num) - 4;
                    g.drawString(num, x, y);
                } catch (Exception ignored) {}
            }
        }

        @Override
        public Dimension getPreferredSize() {
            // La altura debe igualar la del área de texto para que el scroll del
            // encabezado de fila se mantenga sincronizado con el contenido
            return new Dimension(36, textArea.getPreferredSize().height);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Main
    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new Interfaz().setVisible(true));
    }
}