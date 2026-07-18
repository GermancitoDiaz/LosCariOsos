package horafix;

import java.util.ArrayList;
import java.util.List;

/**
 * FASE 4 — GENERADOR DE CÓDIGO INTERMEDIO — HoraFix
 */
public final class GeneradorCodigoIntermedio {

    private GeneradorCodigoIntermedio() {}

    public record Instruccion(String op, String arg1, String arg2, String resultado) {}

    public static List<Instruccion> generar(List<Token> tokens) {
        List<Instruccion> codigo = new ArrayList<>();
        int i = 0;
        while (i < tokens.size()) {
            Token t = tokens.get(i);

            if (t.esReservada("MATERIA") && i + 6 < tokens.size()) {
                // MATERIA nombre horaIni GUION horaFin CREDITOS n → 7 tokens
                String nombre   = tokens.get(i + 1).lexema;
                String horaIni  = tokens.get(i + 2).lexema;
                String horaFin  = tokens.get(i + 4).lexema;
                String creditos = tokens.get(i + 6).lexema;
                codigo.add(new Instruccion("PARAM", horaIni, null, null));
                codigo.add(new Instruccion("PARAM", horaFin, null, null));
                codigo.add(new Instruccion("PARAM", creditos, null, null));
                codigo.add(new Instruccion("CALL", "MATERIA", "3", nombre));
                i += 7;

            } else if (t.esReservada("APROBADAS")
                    || t.esReservada("REPROBADAS")
                    || t.esReservada("CURSANDO")) {
                String etiqueta = t.lexema;
                i += 2; // saltar RESERVADA y DOS_PUNTOS
                int n = 0;
                while (i < tokens.size()) {
                    Token tk = tokens.get(i);
                    if (tk.tipo.equals("IDENTIFICADOR")) {
                        codigo.add(new Instruccion("PARAM", tk.lexema, null, null));
                        n++;
                        i++;
                    } else if (tk.tipo.equals("COMA")) {
                        i++;
                    } else break;
                }
                codigo.add(new Instruccion("CALL", etiqueta, String.valueOf(n), etiqueta));

            } else if (t.esReservada("MAX_CREDITOS") && i + 2 < tokens.size()) {
                codigo.add(new Instruccion("=", tokens.get(i + 2).lexema, null, "MAX_CREDITOS"));
                i += 3;

            } else if (t.esReservada("REGLA") && i + 1 < tokens.size()) {
                String nombreRegla = tokens.get(i + 1).lexema;
                codigo.add(new Instruccion("PARAM", nombreRegla, null, null));
                codigo.add(new Instruccion("CALL", "REGLA", "1", "REGLA"));
                i += 2;

            } else {
                i++;
            }
        }
        return codigo;
    }


    public static String comoTextoTresDirecciones(List<Instruccion> codigo) {
        StringBuilder sb = new StringBuilder();
        for (Instruccion ins : codigo) {
            switch (ins.op()) {
                case "PARAM" -> sb.append("PARAM ").append(ins.arg1());
                case "CALL"  -> sb.append(ins.resultado()).append(" = CALL ")
                                  .append(ins.arg1()).append(", ").append(ins.arg2());
                case "="     -> sb.append(ins.resultado()).append(" = ").append(ins.arg1());
                default      -> sb.append(ins.resultado()).append(" = ")
                                  .append(ins.arg1()).append(" ").append(ins.op())
                                  .append(" ").append(ins.arg2());
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
