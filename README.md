# HoraFix — Compilador de Carga Académica

**Instituto Tecnológico de Tepic**  
Departamento de Sistemas y Computación  
Ingeniería en Sistemas Computacionales  
Materia: Lenguajes y Autómatas I

**Equipo:**
- María Fernanda Martínez Tortolero — 21400724
- Germán Díaz Briseño — 21400669
- Kevin Iván Hernández Castro — 21400700
- Eder Enrique Sanchez Gomez

---

## Descripción

HoraFix es un compilador de dominio específico (DSL) diseñado para optimizar la selección de carga académica de estudiantes universitarios. Procesa un lenguaje propio que describe el estado académico de un alumno (materias aprobadas, reprobadas, en curso, horarios disponibles y restricciones de créditos) y valida que la estructura sea léxica y sintácticamente correcta.

---

## Estructura del proyecto

```
HoraFixCompilador/
├── src/horafix/
│   ├── Lexer.flex        # Especificación JFlex del analizador léxico
│   ├── Lexer.java        # Analizador léxico generado por JFlex
│   ├── Token.java        # Clase que representa un token (tipo, lexema, línea)
│   ├── Parser.java       # Analizador sintáctico de descenso recursivo
│   ├── Interfaz.java     # Interfaz gráfica (Swing)
│   └── Main.java         # Punto de entrada CLI
├── nbproject/            # Configuración de NetBeans
├── build/                # Clases compiladas
└── README.md
```

---

## Lenguaje HoraFix

El lenguaje permite describir la situación académica de un estudiante mediante instrucciones simples. Cada línea representa una declaración.

### Ejemplo de entrada válida

```
APROBADAS: matematicas1, programacion1
REPROBADAS: fisica1
CURSANDO: calculo2
MAX_CREDITOS: 30
MATERIA estructuras_datos LUNES 10-12
MATERIA fisica1 MARTES 8-10
REGLA no_choques
REGLA respetar_seriacion
```

### Ejemplo de salida esperada (fase futura — semántica)

```
PLAN_OPTIMO
MATERIA estructuras_datos LUNES 10-12
MATERIA fisica1 MARTES 8-10
TOTAL_CREDITOS: 24
ESTADO: VALIDO
```

---

## Fase 1 — Análisis Léxico

### ¿Qué hace el analizador léxico?

El analizador léxico (Lexer) es la primera fase del compilador. Su trabajo es leer el texto de entrada carácter por carácter y agruparlos en unidades con significado llamadas **tokens**.

Por ejemplo, la línea:
```
MATERIA fisica1 LUNES 8-10
```
Se convierte en:
```
RESERVADA   → MATERIA
IDENTIFICADOR → fisica1
DIA         → LUNES
NUMERO      → 8
GUION       → -
NUMERO      → 10
```

### Herramienta utilizada: JFlex

El lexer se define en el archivo `Lexer.flex` usando expresiones regulares. JFlex lee ese archivo y genera automáticamente `Lexer.java`, que contiene un autómata finito determinista (AFD) capaz de reconocer todos los tokens.

Para regenerar el lexer tras modificar `Lexer.flex`:
```
jflex src/horafix/Lexer.flex
```

### Tabla de tokens

| Token | Patrón | Ejemplo |
|---|---|---|
| `RESERVADA` | `APROBADAS`, `REPROBADAS`, `CURSANDO`, `MATERIA`, `REGLA`, `MAX_CREDITOS` | `MATERIA` |
| `DIA` | `LUNES`, `MARTES`, `MIERCOLES`, `JUEVES`, `VIERNES` | `LUNES` |
| `IDENTIFICADOR` | `[a-zA-Z][a-zA-Z0-9_]*` | `fisica1`, `programacion1` |
| `NUMERO` | `[0-9]+` | `30`, `8`, `12` |
| `GUION` | `-` | `-` |
| `DOS_PUNTOS` | `:` | `:` |
| `COMA` | `,` | `,` |
| `ERROR` | cualquier carácter no reconocido | `@`, `#`, `ñ` |

### Cómo el lexer devuelve tokens

Cada token se devuelve como un String con el formato:
```
"TIPO,lexema,numeroDeLinea"
```

Ejemplo: `"RESERVADA,MATERIA,1"`, `"IDENTIFICADOR,fisica1,2"`, `"NUMERO,10,3"`

> **Nota:** Para el token COMA, el lexema es `,` lo que genera `"COMA,,,1"`. Por esto en la interfaz se usa `indexOf` y `lastIndexOf` en lugar de `split(",")` para separar correctamente los campos.

### Errores léxicos

Cuando el lexer encuentra un carácter que no coincide con ninguna regla, lo marca como `ERROR`:

```java
. {
    return "ERROR," + yytext() + "," + (yyline+1);
}
```

Ejemplo: escribir `física1` con tilde genera `ERROR,í,1` porque `í` no está en el patrón `[a-zA-Z]`.

---

## Fase 2 — Análisis Sintáctico

### ¿Qué hace el analizador sintáctico?

El parser toma la lista de tokens producida por el lexer y verifica que su **orden** sea correcto según la gramática del lenguaje. No le importa el significado, solo la estructura.

Ejemplo:
```
MATERIA fisica1 LUNES 8-10   ✔  estructura correcta
MATERIA LUNES fisica1 8-10   ✘  el día va antes que el nombre
```

Ambas líneas tienen tokens válidos léxicamente, pero la segunda viola la gramática.

### Gramática formal de HoraFix

```
programa          → sentencia*

sentencia         → sent_aprobadas
                  | sent_reprobadas
                  | sent_cursando
                  | sent_max_creditos
                  | sent_materia
                  | sent_regla

sent_aprobadas    → APROBADAS   ':' lista_materias
sent_reprobadas   → REPROBADAS  ':' lista_materias
sent_cursando     → CURSANDO    ':' lista_materias
sent_max_creditos → MAX_CREDITOS ':' NUMERO
sent_materia      → MATERIA IDENTIFICADOR DIA NUMERO '-' NUMERO
sent_regla        → REGLA IDENTIFICADOR

lista_materias    → IDENTIFICADOR (',' IDENTIFICADOR)*
```

### Tipo de parser: Descenso Recursivo

Se implementó un parser de **descenso recursivo** en `Parser.java`. Cada regla de la gramática se convierte en un método Java:

```
analizarPrograma()
  └── analizarSentencia()        ← mira la palabra clave para decidir qué regla aplicar
        ├── analizarMateria()    → MATERIA + IDENTIFICADOR + DIA + NUMERO + - + NUMERO
        ├── analizarMaxCreditos()→ MAX_CREDITOS + : + NUMERO
        ├── analizarListaEstatus()→ KEYWORD + : + lista_materias
        └── analizarRegla()      → REGLA + IDENTIFICADOR
```

### El método `consumir()`

Es la pieza central del parser. Verifica que el token actual sea el esperado y avanza el cursor:

```java
private void consumir(String tipoEsperado, String lexemaEsperado) throws ParseException {
    Token actual = tokens.get(pos);

    if (!actual.tipo.equals(tipoEsperado)) {
        throw new ParseException(
            "Error sintáctico en línea " + actual.linea +
            ": se esperaba " + descripcionToken(tipoEsperado, lexemaEsperado) +
            " pero se encontró '" + actual.lexema + "'"
        );
    }
    pos++; // token válido — avanzar al siguiente
}
```

### Recuperación de errores

Cuando se detecta un error en una sentencia, el parser no se detiene. Avanza tokens hasta encontrar el inicio de la siguiente sentencia (una palabra reservada) y continúa el análisis. Esto permite reportar **todos los errores** del programa en una sola pasada.

```java
private void recuperar() {
    while (pos < tokens.size()) {
        if (tokens.get(pos).tipo.equals("RESERVADA")) break;
        pos++;
    }
}
```

### Mensajes de error

Los errores incluyen el número de línea y una descripción legible:

```
Error sintáctico en línea 3: se esperaba un día (LUNES, MARTES, ...) pero se encontró '10'
Error sintáctico en línea 5: se esperaba ':' pero se encontró 'LUNES'
```

---

## Interfaz gráfica

La interfaz está construida con Java Swing y tiene tres secciones:

| Sección | Descripción |
|---|---|
| **Código fuente** | Área de texto con números de línea donde se escribe el programa |
| **Tokens reconocidos** | Tabla con colores por tipo de token |
| **Resultados del análisis** | Muestra errores léxicos y resultado del análisis sintáctico |

### Colores de la tabla de tokens

| Color | Tipo de token |
|---|---|
| Azul claro | `RESERVADA` |
| Verde claro | `IDENTIFICADOR` |
| Morado claro | `DIA` |
| Amarillo claro | `NUMERO` |
| Gris | `GUION`, `DOS_PUNTOS`, `COMA` |
| Rojo | `ERROR` |

---

## Cómo ejecutar el proyecto

### Desde NetBeans
1. Abrir el proyecto en NetBeans
2. Presionar **F6** (Run Project)
3. La clase principal es `horafix.Interfaz`

### Desde la terminal
```bash
# Compilar
javac -encoding UTF-8 -d build/classes src/horafix/Token.java src/horafix/Parser.java src/horafix/Lexer.java src/horafix/Interfaz.java src/horafix/Main.java

# Ejecutar
java -cp build/classes horafix.Interfaz
```

### Regenerar el lexer (si se modifica Lexer.flex)
```bash
jflex src/horafix/Lexer.flex
```

---

## Herramientas utilizadas

| Herramienta | Uso |
|---|---|
| **Java 17** | Lenguaje de programación principal |
| **NetBeans** | IDE de desarrollo |
| **JFlex 1.9.1** | Generador del analizador léxico |
| **Java Swing** | Interfaz gráfica |

---

## Estado del proyecto

| Fase | Estado |
|---|---|
| Análisis léxico | ✔ Completo |
| Análisis sintáctico | ✔ Completo |
| Análisis semántico | Pendiente |
| Generación de salida (`PLAN_OPTIMO`) | Pendiente |
