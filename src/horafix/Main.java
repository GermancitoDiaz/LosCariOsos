package horafix;

import java.io.StringReader;

/**
 * Punto de entrada alternativo: ejecuta el analizador léxico por consola,
 * sin levantar la interfaz gráfica. Útil para pruebas rápidas del Lexer.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String entrada = "MATERIA programacion 7-9 CREDITOS 4";

        Lexer lexer = new Lexer(new StringReader(entrada));

        String token;
        while ((token = lexer.yylex()) != null) {
            System.out.println(token);
        }
    }
}
