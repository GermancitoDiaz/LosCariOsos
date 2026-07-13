package horafix;

import java.io.StringReader;

/**
 * PUNTO DE ENTRADA ALTERNATIVO — HoraFix
 */
public class Main {

    /**
     * Ejecuta el Lexer sobre una cadena de prueba e imprime los tokens en consola.
     * Cada línea de salida tiene el formato: {@code TIPO,lexema,linea}
     *
     * @param args argumentos de línea de comandos (no utilizados)
     * @throws Exception si ocurre un error de I/O al leer la entrada
     */
    public static void main(String[] args) throws Exception {

        String entrada = "MATERIA programacion 7-9 CREDITOS 4";

        Lexer lexer = new Lexer(new StringReader(entrada));

        String token;
        while ((token = lexer.yylex()) != null) {
            System.out.println(token);
        }
    }
}
