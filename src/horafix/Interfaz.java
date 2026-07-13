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

public class Interfaz extends JFrame {

    // ── Componentes de la interfaz ────────────────────────────────────────────
    private JTextArea  txtEntrada;
    private JTable     tblTokens;
    // NUEVO: tablas de análisis adicionales (pestañas del panel derecho)
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

        // NUEVO: tabla catálogo de errores (pestaña 2)
        tblErrores = new JTable();
        tblErrores.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tblErrores.setRowHeight(22);
        tblErrores.setGridColor(new Color(210, 210, 210));
        tblErrores.setDefaultRenderer(Object.class, new ErrorCellRenderer());
        tblErrores.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tblErrores.getTableHeader().setBackground(new Color(240, 240, 240));
        tblErrores.setModel(crearCatalogoErrores());

        // NUEVO: tabla de símbolos (pestaña 3)
        tblSimbolos = new JTable();
        tblSimbolos.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tblSimbolos.setRowHeight(22);
        tblSimbolos.setGridColor(new Color(210, 210, 210));
        // NUEVO: mismos colores que la tabla de tokens; el tipo está en la columna 1
        tblSimbolos.setDefaultRenderer(Object.class, new TokenCellRenderer(1));
        tblSimbolos.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tblSimbolos.getTableHeader().setBackground(new Color(240, 240, 240));
        tblSimbolos.setModel(modeloSimbolosVacio());

        // NUEVO: pestaña "Tokens" agrupa la tabla original y su leyenda
        JPanel tabTokens = new JPanel(new BorderLayout(0, 0));
        tabTokens.add(new JScrollPane(tblTokens), BorderLayout.CENTER);
        tabTokens.add(crearLeyenda(), BorderLayout.SOUTH);

        // NUEVO: JTabbedPane con las tres vistas de análisis
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("SansSerif", Font.BOLD, 12));
        // NUEVO: pestaña "Tabla de símbolos" con leyenda idéntica a la de Tokens
        JPanel tabSimbolos = new JPanel(new BorderLayout(0, 0));
        tabSimbolos.add(new JScrollPane(tblSimbolos), BorderLayout.CENTER);
        tabSimbolos.add(crearLeyenda(), BorderLayout.SOUTH);

        tabs.addTab("Tokens",              tabTokens);
        tabs.addTab("Tabla de símbolos",   tabSimbolos);
        tabs.addTab("Catálogo de errores", new JScrollPane(tblErrores));

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

        // NUEVO: botón para ver el árbol sintáctico en ventana emergente
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
        // CAMBIO: solo ERROR_CHAR — las keywords mal escritas ya no generan ERROR_KEYWORD
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
     * PIPELINE PRINCIPAL DEL COMPILADOR — invocado al presionar "Analizar".
     *
     * Ejecuta las dos fases del compilador en secuencia y actualiza todos los
     * paneles de la interfaz con los resultados:
     *
     * <pre>
     * FASE 1 — Análisis léxico (Lexer)
     *   • Instancia el Lexer sobre el texto del editor.
     *   • Llama yylex() en bucle hasta obtener null (fin de entrada).
     *   • Convierte cada String "TIPO,lexema,linea" en un objeto Token.
     *   • Llena la tabla de Tokens y cuenta los ERROR_CHAR.
     *
     * FASE 2 — Análisis sintáctico (Parser)
     *   • Siempre se ejecuta, incluso si hay ERROR_CHAR en el stream.
     *     El Parser salta los ERROR_CHAR con skipErrorTokens() y sigue.
     *   • Separa los errores del parser en dos listas:
     *       errLexParser  — mensajes que empiezan "Error léxico"  (E-L03/E-L04)
     *       errSintPuros  — mensajes que empiezan "Error sintáctico"
     *
     * POST-PROCESO
     *   • Construye el texto de resultados con conteos y listas de errores.
     *   • Resalta en rojo las líneas del editor que tienen errores.
     *   • Habilita/deshabilita los botones "Simulación" y "Ver Árbol".
     *   • Puebla la Tabla de Símbolos con los tokens no-error del análisis.
     * </pre>
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

                // CAMBIO: contar ambos tipos de error (ERROR_KEYWORD y ERROR_CHAR)
                if (tipo.startsWith("ERROR")) erroresLexicos++;
            }

            tblTokens.setModel(modelo);

            // ── NUEVO: Contador de tokens por tipo ───────────────────────────
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
            java.util.Set<Integer> lineasError = new java.util.HashSet<>();
            for (Token t : listaTokens) {
                if (t.tipo.equals("ERROR_CHAR")) {
                    lineasError.add(t.linea);
                    String msg = t.lexema.matches("0[0-9]+")
                        ? ErrorManager.ceroAlInicio(t.linea, t.lexema)
                        : ErrorManager.charInvalido(t.linea, t.lexema);
                    resultado.append("  • ").append(msg).append("\n");
                }
            }

            // Mensajes de errores léxicos detectados por el parser (días, números, keywords)
            for (String err : errLexParser) {
                resultado.append("  • ").append(err).append("\n");
                java.util.regex.Matcher m =
                    java.util.regex.Pattern.compile("línea (\\d+)").matcher(err);
                if (m.find()) lineasError.add(Integer.parseInt(m.group(1)));
            }

            resultado.append("─".repeat(50)).append("\n");

            // ── Resultado del análisis sintáctico ─────────────────────────────
            if (!errSintPuros.isEmpty()) {
                resultado.append("Errores sintácticos : ").append(errSintPuros.size()).append("\n");
                for (String err : errSintPuros) {
                    resultado.append("  • ").append(err).append("\n");
                    java.util.regex.Matcher m =
                        java.util.regex.Pattern.compile("línea (\\d+)").matcher(err);
                    if (m.find()) lineasError.add(Integer.parseInt(m.group(1)));
                }
            }
            if (totalLex == 0 && errSintPuros.isEmpty()) {
                resultado.append("Análisis sintáctico: ✔  Sintaxis correcta");
                sintaxisOk = true;
            } else if (errSintPuros.isEmpty()) {
                resultado.append("Análisis sintáctico: ✔  Estructura sintáctica válida — corrija los errores léxicos");
            }

            txtResultados.setText(resultado.toString());
            resaltarLineasError(lineasError);

            // Habilitar botones que requieren sintaxis correcta
            ultimosTokens = listaTokens;
            btnSimulacion.setEnabled(sintaxisOk);
            btnVerArbol.setEnabled(ultimoArbol != null);

            // NUEVO: poblar tabla de símbolos (el catálogo de errores es estático)
            poblarTablaSimbolos(listaTokens);

            // ── FASE 3: Análisis semántico ────────────────────────────────────
            // Se ejecuta siempre, independientemente de si hubo errores léxicos
            // o sintácticos, para reportar todos los problemas en una sola pasada.
            List<String> erroresSemanticos = validarSemantica(listaTokens);
            if (!erroresSemanticos.isEmpty()) {
                StringBuilder semBuilder = new StringBuilder();
                semBuilder.append("─".repeat(50)).append("\n");
                semBuilder.append("Errores semánticos : ")
                          .append(erroresSemanticos.size()).append("\n");
                for (String err : erroresSemanticos)
                    semBuilder.append("  • ").append(err).append("\n");
                txtResultados.append(semBuilder.toString());
            }

        } catch (Exception e) {
            txtResultados.setText("Error inesperado: " + e.getMessage());
        }
    }

    /**
     * Resalta en rojo claro las líneas del editor que contienen errores léxicos
     * o sintácticos. Se invoca al final de {@link #analizar()} con el conjunto
     * de números de línea extraídos de todos los mensajes de error.
     * Se limpia automáticamente al borrar el código o al re-analizar sin errores.
     *
     * @param lineas conjunto de números de línea a resaltar (base 1)
     */
    private void resaltarLineasError(java.util.Set<Integer> lineas) {
        txtEntrada.getHighlighter().removeAllHighlights();
        if (lineas.isEmpty()) return;

        javax.swing.text.Highlighter.HighlightPainter painter =
            new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(
                new Color(255, 200, 200));

        for (int linea : lineas) {
            try {
                int ini = txtEntrada.getLineStartOffset(linea - 1);
                int fin = txtEntrada.getLineEndOffset(linea - 1);
                txtEntrada.getHighlighter().addHighlight(ini, fin, painter);
            } catch (Exception ignored) {}
        }
    }

    // NUEVO: abre ventana emergente con el árbol sintáctico en chips de colores
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
        // NUEVO: limpiar tabla de símbolos; el catálogo de errores es estático y no se borra
        tblSimbolos.setModel(modeloSimbolosVacio());
        txtEntrada.getHighlighter().removeAllHighlights();
        ultimosTokens.clear();
        btnSimulacion.setEnabled(false);
        ultimoArbol = null;
        btnVerArbol.setEnabled(false);
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
            JLabel lblVacio = new JLabel(
                "No se encontraron declaraciones MATERIA — agrega al menos una para ver el horario.",
                SwingConstants.CENTER);
            lblVacio.setFont(new Font("SansSerif", Font.ITALIC, 14));
            lblVacio.setForeground(new Color(120, 120, 120));
            areaHorario.add(lblVacio, BorderLayout.CENTER);
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

                int x = TIME_W + col * COL_W + PAD;
                int y = HEAD_H + (inicio - minHour) * ROW_H + PAD;
                int w = COL_W - 2 * PAD;
                int h = (finEfectivo - inicio) * ROW_H - 2 * PAD;

                // Etiqueta del día en la tarjeta (solo mostrar créditos en el día inicio)
                String etiquetaCreds = (col == 0) ? creditos + " créditos" : null;
                JPanel card = crearTarjetaMateria(nombre, inicio, finEfectivo,
                                                  acento, estado, etiquetaCreds, horaExtra);
                card.setBounds(x, y, w, h);
                panel.add(card);
            }
        }

        return panel;
    }

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

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                int w = getWidth(), h = getHeight();
                // Contexto A: fondo + barra de acento.
                // g2.clip() INTERSECTA con el clip del JScrollPane, nunca lo elimina.
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgCard);
                g2.fillRoundRect(0, 0, w, h, 10, 10);
                g2.clip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, 10, 10));
                g2.setColor(acento);
                g2.fillRect(0, 0, 5, h);
                g2.dispose();
                // Contexto B: borde (sin modificar el clip)
                Graphics2D g3 = (Graphics2D) g.create();
                g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g3.setColor(borderColor);
                g3.setStroke(new java.awt.BasicStroke(1f));
                g3.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
                g3.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(8, 14, 7, 8));

        JLabel lblNombre = new JLabel("<html><b>" + nombre.replace("_", " ") + "</b></html>");
        lblNombre.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblNombre.setForeground(new Color(22, 30, 60));
        lblNombre.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblNombre);

        if (estado != null) {
            Color colorEstado = estado.contains("✔") ? new Color(25, 120, 40)
                              : estado.contains("✘") ? new Color(170, 30, 30)
                              : new Color(55, 90, 190);
            JLabel lblEstado = new JLabel(estado);
            lblEstado.setFont(new Font("SansSerif", Font.BOLD, 10));
            lblEstado.setForeground(colorEstado);
            lblEstado.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(Box.createVerticalStrut(3));
            card.add(lblEstado);
        }

        // Etiqueta de créditos — solo en el día de inicio
        if (etiquetaCreds != null) {
            JLabel lblCreds = new JLabel("📚 " + etiquetaCreds);
            lblCreds.setFont(new Font("SansSerif", Font.BOLD, 10));
            lblCreds.setForeground(new Color(80, 60, 160));
            lblCreds.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(Box.createVerticalStrut(2));
            card.add(lblCreds);
        }

        // Hora extra para 6 créditos — indicador visual en viernes
        if (horaExtra) {
            JLabel lblExtra = new JLabel("⏱ +1h extra");
            lblExtra.setFont(new Font("SansSerif", Font.BOLD, 10));
            lblExtra.setForeground(new Color(180, 60, 20));
            lblExtra.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(Box.createVerticalStrut(2));
            card.add(lblExtra);
        }

        card.add(Box.createVerticalStrut(6));
        JLabel lblHora = new JLabel(inicio + ":00 — " + fin + ":00"
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
            "Choque de horario entre dos materias (días en común según créditos)",
            "MATERIA calculo 8-10 CREDITOS 5 / MATERIA fisica 9-11 CREDITOS 3"});
        m.addRow(new Object[]{"E-M02", "Semántico",
            "Materia reprobada sin horario asignado",
            "REPROBADAS: fisica — sin declaración MATERIA fisica"});
        m.addRow(new Object[]{"E-M03", "Semántico",
            "Materia aprobada re-inscrita innecesariamente",
            "APROBADAS: calculo / MATERIA calculo 8-10 CREDITOS 4"});
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
        return m;
    }

    /**
     * FASE 3 — Análisis semántico.
     *
     * Recorre los tokens del último análisis y verifica restricciones de
     * negocio académico que la gramática no puede capturar:
     *   E-M01 Choques de horario entre materias con días en común (según créditos)
     *   E-M02 Materia reprobada sin horario declarado
     *   E-M03 Materia aprobada que se intenta re-inscribir
     *   E-M04 Materia en cursando sin horario declarado
     *   E-M05 Total de créditos supera MAX_CREDITOS
     *   E-M06 Hora fuera del rango válido (7–21)
     *   E-M07 Hora de fin <= hora de inicio
     *   E-M08 No hay ninguna declaración MATERIA
     *   E-M09 Créditos fuera del rango válido (3–6)
     *   E-M10 Total de créditos insuficiente respecto a MAX_CREDITOS
     *   E-M11 MAX_CREDITOS fuera del rango institucional válido (20–35)
     *   E-M10 Total de créditos insuficiente respecto a MAX_CREDITOS
     *
     * @param tokens lista completa de tokens del último análisis léxico
     * @return lista de mensajes de error semántico; vacía si no hay errores
     */
    private List<String> validarSemantica(List<Token> tokens) {
        List<String> errores = new ArrayList<>();

        // ── Extraer datos estructurados de los tokens ─────────────────────────
        // Cada materia guarda: [nombre, horaInicio, horaFin, creditos]
        List<String[]> materias   = new ArrayList<>();
        List<String>   aprobadas  = new ArrayList<>();
        List<String>   reprobadas = new ArrayList<>();
        List<String>   cursando   = new ArrayList<>();
        int            maxCred    = -1;

        int i = 0;
        while (i < tokens.size()) {
            Token t = tokens.get(i);

            if (t.esReservada("MATERIA") && i + 6 < tokens.size()) {
                // MATERIA nombre numero GUION numero CREDITOS numero → 7 tokens
                materias.add(new String[]{
                    tokens.get(i + 1).lexema,  // nombre
                    tokens.get(i + 2).lexema,  // hora inicio
                    tokens.get(i + 4).lexema,  // hora fin  (i+3 = GUION)
                    tokens.get(i + 6).lexema   // créditos  (i+5 = RESERVADA "CREDITOS")
                });
                i += 7;

            } else if (t.esReservada("APROBADAS")
                    || t.esReservada("REPROBADAS")
                    || t.esReservada("CURSANDO")) {
                List<String> target = t.esReservada("APROBADAS")  ? aprobadas
                                    : t.esReservada("REPROBADAS") ? reprobadas
                                    : cursando;
                i += 2; // saltar RESERVADA y DOS_PUNTOS
                while (i < tokens.size()) {
                    Token tk = tokens.get(i);
                    if (tk.tipo.equals("IDENTIFICADOR")) { target.add(tk.lexema); i++; }
                    else if (tk.tipo.equals("COMA"))     { i++; }
                    else break;
                }

            } else if (t.esReservada("MAX_CREDITOS") && i + 2 < tokens.size()) {
                try { maxCred = Integer.parseInt(tokens.get(i + 2).lexema); }
                catch (NumberFormatException ignored) {}
                i += 3;

            } else {
                i++;
            }
        }

        // ── E-M08: sin materias declaradas ───────────────────────────────────
        if (materias.isEmpty()) {
            errores.add(ErrorManager.sinMaterias());
            return errores; // sin materias las demás validaciones no aplican
        }

        // ── E-M09: créditos fuera del rango válido (3–6) ─────────────────────
        for (String[] m : materias) {
            try {
                int creds = Integer.parseInt(m[3]);
                if (creds < 3 || creds > 6)
                    errores.add(ErrorManager.errorSemantico(
                        ErrorManager.CodigoSemantico.E_M09,
                        "\"" + m[0] + "\" tiene " + creds +
                        " créditos — el valor debe estar entre 3 y 6."));
            } catch (NumberFormatException ignored) {}
        }

        // ── E-M06 y E-M07: rango horario inválido ────────────────────────────
        for (String[] m : materias) {
            try {
                int ini = Integer.parseInt(m[1]);
                int fin = Integer.parseInt(m[2]);
                if (ini < 7 || ini > 21)
                    errores.add(ErrorManager.horarioFueraDeRango(m[0], ini));
                else if (fin < 7 || fin > 21)
                    errores.add(ErrorManager.horarioFueraDeRango(m[0], fin));
                if (fin <= ini)
                    errores.add(ErrorManager.horaFinInvalida(m[0], ini, fin));
            } catch (NumberFormatException ignored) {}
        }

        // ── E-M01: choques de horario (días en común según créditos) ──────────
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
                        if (solapIni < solapFin)
                            errores.add(ErrorManager.choqueHorario(
                                ma[0], mb[0], dia, solapIni, solapFin));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // ── E-M02: reprobada sin horario ──────────────────────────────────────
        for (String rep : reprobadas) {
            boolean tieneHorario = false;
            for (String[] m : materias)
                if (m[0].equals(rep)) { tieneHorario = true; break; }
            if (!tieneHorario)
                errores.add(ErrorManager.reprobadaSinHorario(rep));
        }

        // ── E-M03: aprobada re-inscrita ───────────────────────────────────────
        for (String[] m : materias)
            if (aprobadas.contains(m[0]))
                errores.add(ErrorManager.aprobadaReInscrita(m[0]));

        // ── E-M04: cursando sin horario ───────────────────────────────────────
        for (String cur : cursando) {
            boolean tieneHorario = false;
            for (String[] m : materias)
                if (m[0].equals(cur)) { tieneHorario = true; break; }
            if (!tieneHorario)
                errores.add(ErrorManager.cursandoSinHorario(cur));
        }

        // ── E-M11: MAX_CREDITOS fuera del rango institucional válido (20–35) ──────
        if (maxCred != -1 && (maxCred < 20 || maxCred > 35)) {
            errores.add(ErrorManager.maxCreditosFueraDeRango(maxCred));
        } else if (maxCred != -1) {
            // ── E-M05 / E-M10: total de créditos fuera de rango respecto a MAX_CREDITOS ──
            // Solo se compara contra MAX_CREDITOS si su valor declarado es válido.
            int totalCreditos = 0;
            for (String[] m : materias) {
                try { totalCreditos += Integer.parseInt(m[3]); }
                catch (NumberFormatException ignored) {}
            }
            if (totalCreditos > maxCred) {
                errores.add(ErrorManager.excesoCreditos(totalCreditos, maxCred));
            } else if (totalCreditos * 2 < maxCred) {
                // Insuficiente si el total inscrito es menos del 50% de MAX_CREDITOS
                errores.add(ErrorManager.creditosInsuficientes(totalCreditos, maxCred));
            }
        }

        return errores;
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
        // CAMBIO: columna de donde se lee el tipo de token para aplicar el color.
        // La tabla de tokens usa columna 0; la tabla de símbolos usa columna 1.
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
                    // CAMBIO: solo ERROR_CHAR — keywords mal escritas son IDENTIFICADOR
                    case "ERROR_CHAR":    c.setBackground(new Color(255, 180, 180)); break; // rojo
                    default:              c.setBackground(Color.WHITE);              break;
                }
            }
            return c;
        }
    }

    // NUEVO: renderer para tblErrores — colorea filas por categoría de error
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

            // CAMBIO: modelToView2D obtiene la coordenada Y exacta de cada línea,
            // respetando los insets del JTextArea (margin top = 6 px) y el espaciado
            // real entre renglones. Antes se usaba lineHeight * i, lo que desviaba
            // los números hacia arriba porque no consideraba el margen superior.
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
            // CAMBIO: la altura coincide con la del área de texto para que el
            // scroll del encabezado de fila sincronice correctamente con el contenido
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