# HoraFix — Compilador de Carga Académica

**Instituto Tecnológico de Tepic**
Departamento de Sistemas y Computación
Ingeniería en Sistemas Computacionales
Materia: Lenguajes y Autómatas I / II

**Equipo:**
- María Fernanda Martínez Tortolero — 21400724
- Germán Díaz Briseño — 21400669
- Kevin Iván Hernández Castro — 21400700
- Eder Enrique Sanchez Gomez

---

## Descripción

HoraFix es un compilador de dominio específico (DSL) para describir y validar la carga académica de un estudiante universitario: materias aprobadas, reprobadas, en curso, su horario y sus créditos. El compilador implementa las cuatro fases clásicas de construcción:

1. **Análisis léxico** — reconoce los tokens del lenguaje.
2. **Análisis sintáctico** — valida la estructura gramatical de cada sentencia.
3. **Análisis semántico** — valida reglas de negocio académico (choques de horario, créditos fuera de rango, estatus contradictorios, etc.).
4. **Generación de código intermedio** — traduce el programa validado a código de tres direcciones.

Los resultados de las cuatro fases se muestran en una interfaz gráfica (Java Swing), junto con un simulador visual del horario resultante.

---

## Estructura del proyecto

```
HoraFixCompilador/
├── src/horafix/
│   ├── Lexer.flex                   # Especificación JFlex del analizador léxico
│   ├── Lexer.java                   # Analizador léxico generado por JFlex
│   ├── Token.java                   # Representa un token (tipo, lexema, línea)
│   ├── Parser.java                  # Analizador sintáctico de descenso recursivo
│   ├── ErrorManager.java            # Catálogo y formato de todos los mensajes de error
│   ├── GeneradorCodigoIntermedio.java  # Generador de código de tres direcciones
│   ├── Interfaz.java                # Interfaz gráfica (Swing)
│   └── Main.java                    # Punto de entrada alternativo por consola
├── nbproject/                       # Configuración de NetBeans
├── build/                           # Clases compiladas
└── README.md
```

---

## Lenguaje HoraFix

Cada línea del programa es una sentencia independiente.

### Ejemplo de entrada válida

```
APROBADAS: matematicas1, programacion1
REPROBADAS: fisica1
CURSANDO: calculo2
MAX_CREDITOS: 30
MATERIA estructuras_datos 10-12 CREDITOS 4
MATERIA fisica1 8-10 CREDITOS 3
SERIACION calculo2 : calculo1
REGLA no_choques
REGLA respetar_seriacion
```

### Reglas activables (`REGLA`)

`REGLA` activa validaciones semánticas opcionales — si no se declara, el chequeo correspondiente **no se ejecuta**. Solo se reconocen tres nombres:

| Nombre de regla | Activa |
|---|---|
| `no_choques` | E-M01 (choque de horario entre materias) |
| `no_reinscribir_aprobadas` | E-M03 (materia aprobada re-inscrita) |
| `respetar_seriacion` | E-M14 (seriación incumplida, ver `SERIACION` abajo) |

Un nombre de regla que no está en esta tabla genera **E-M15**, con sugerencia por distancia de Levenshtein (ej. `REGLA no_choke` → "¿quiso escribir 'no_choques'?"). El resto de los errores semánticos (rangos, duplicados, estados contradictorios, etc.) son invariantes estructurales y siempre se validan, sin importar qué reglas se declaren.

### Seriación (`SERIACION`)

Declara los prerequisitos de una materia:

```
SERIACION calculo2 : calculo1
```

Si `calculo2` aparece en `CURSANDO` o tiene una declaración `MATERIA` sin que `calculo1` esté en `APROBADAS`, se reporta E-M14 — pero solo si el programa declaró `REGLA respetar_seriacion`.

Una declaración `MATERIA` no incluye el día de la semana: los créditos lo determinan automáticamente, siempre a partir del lunes. Cada crédito equivale a una hora de clase en un día distinto:

| Créditos | Días que ocupa |
|---|---|
| 3 | Lunes – Miércoles |
| 4 | Lunes – Jueves |
| 5 | Lunes – Viernes |
| 6 | Lunes – Viernes, con una hora extra el viernes |

### Gramática formal

```
programa          → sentencia*

sentencia         → sent_aprobadas
                  | sent_reprobadas
                  | sent_cursando
                  | sent_max_creditos
                  | sent_materia
                  | sent_regla
                  | sent_seriacion

sent_aprobadas    → APROBADAS    ':' lista_materias
sent_reprobadas   → REPROBADAS   ':' lista_materias
sent_cursando     → CURSANDO     ':' lista_materias
sent_max_creditos → MAX_CREDITOS ':' NUMERO
sent_materia      → MATERIA IDENTIFICADOR NUMERO '-' NUMERO CREDITOS NUMERO
sent_regla        → REGLA IDENTIFICADOR
sent_seriacion    → SERIACION IDENTIFICADOR ':' lista_materias

lista_materias    → IDENTIFICADOR (',' IDENTIFICADOR)*
```

---

## Fase 1 — Análisis léxico

El analizador léxico (`Lexer.flex`, generado a `Lexer.java` mediante JFlex) recorre el código fuente carácter por carácter y lo agrupa en tokens. Cada token se devuelve como una cadena con formato `"TIPO,lexema,linea"`, que `Interfaz.java` convierte en un objeto `Token`.

| Token | Patrón | Ejemplo |
|---|---|---|
| `RESERVADA` | `APROBADAS`, `REPROBADAS`, `CURSANDO`, `MAX_CREDITOS`, `MATERIA`, `REGLA`, `CREDITOS`, `SERIACION` | `MATERIA` |
| `IDENTIFICADOR` | `[a-z][a-zA-Z0-9_]*` | `fisica1`, `calculo` |
| `NUMERO` | `0` \| `[1-9][0-9]*` (sin ceros a la izquierda) | `8`, `30` |
| `DOS_PUNTOS` | `:` | `:` |
| `COMA` | `,` | `,` |
| `GUION` | `-` | `-` |
| `ERROR_CHAR` | carácter no reconocido, o número con ceros a la izquierda | `@`, `007` |

JFlex resuelve ambigüedades por coincidencia más larga y, en caso de empate, por orden de declaración; por eso las palabras reservadas se declaran antes que la regla genérica de identificadores.

Para regenerar `Lexer.java` después de modificar `Lexer.flex`:
```bash
jflex src/horafix/Lexer.flex
```

### Errores léxicos (E-L01 – E-L04)

| Código | Descripción |
|---|---|
| E-L01 | Carácter(es) no válido(s) en el código fuente |
| E-L02 | Número con ceros a la izquierda (`007`) |
| E-L03 | Palabra reservada mal escrita, con sugerencia por distancia de Levenshtein |
| E-L04 | Texto donde se esperaba un número entero |

---

## Fase 2 — Análisis sintáctico

`Parser.java` implementa un parser de **descenso recursivo predictivo**: cada regla de la gramática tiene su propio método (`analizarMateria()`, `analizarListaEstatus()`, `analizarMaxCreditos()`, `analizarRegla()`), y el método `consumir()` verifica y avanza sobre el token actual.

Ante un error, el parser entra en **modo pánico**: descarta tokens hasta encontrar la siguiente palabra reservada (inicio de una nueva sentencia) y continúa el análisis, para poder reportar todos los errores del programa en una sola pasada en lugar de detenerse en el primero.

### Errores sintácticos (E-S01 – E-S10)

| Código | Descripción |
|---|---|
| E-S01 | Instrucción desconocida al inicio de sentencia |
| E-S02 | Lista de materias vacía después de `:` |
| E-S03 | Coma final sin identificador |
| E-S04 | Sentencia incompleta al final del archivo |
| E-S05 | Token inesperado dentro de una sentencia |
| E-S06 | Palabra reservada en posición incorrecta |
| E-S07 | Se esperaba `:` después de una palabra reservada |
| E-S08 | Se esperaba un identificador de materia |
| E-S09 | Se esperaba un número de hora válido |
| E-S10 | Se esperaba `-` para separar hora de inicio y fin |

---

## Fase 3 — Análisis semántico

Verifica restricciones de negocio académico que la gramática, por sí sola, no puede capturar. Todos los mensajes incluyen el número de línea del problema, y esa línea se resalta en el editor.

### Errores semánticos (E-M01 – E-M13)

| Código | Descripción |
|---|---|
| E-M01 | Choque de horario entre dos materias con días en común (según sus créditos) |
| E-M02 | Materia reprobada sin horario asignado |
| E-M03 | Materia aprobada re-inscrita innecesariamente |
| E-M04 | Materia en CURSANDO sin horario declarado |
| E-M05 | Total de créditos supera `MAX_CREDITOS` |
| E-M06 | Horario fuera del rango académico válido (7:00–21:00) |
| E-M07 | Hora de fin menor o igual a la hora de inicio |
| E-M08 | Sin materias declaradas para generar el plan |
| E-M09 | Créditos de una materia fuera del rango válido (3–6) |
| E-M10 | Total de créditos insuficiente frente a `MAX_CREDITOS` (menos del 50%) |
| E-M11 | `MAX_CREDITOS` fuera del rango institucional válido (20–35) |
| E-M12 | Una materia aparece en más de una lista de estatus a la vez |
| E-M13 | Materia declarada más de una vez con `MATERIA` |
| E-M14 | Seriación incumplida — requiere `REGLA respetar_seriacion` |
| E-M15 | `REGLA` con nombre desconocido, con sugerencia por distancia de Levenshtein |

E-M01 y E-M03 también son "reglas activables": solo se validan si el programa declara `REGLA no_choques` / `REGLA no_reinscribir_aprobadas` respectivamente. Ver [Reglas activables](#reglas-activables-regla) arriba. El resto de los códigos son invariantes estructurales de los datos y siempre se validan.

---

## Fase 4 — Código intermedio

HoraFix es un lenguaje declarativo, sin expresiones aritméticas ni asignaciones, así que el código intermedio se genera tratando cada sentencia como una invocación con argumentos — usando las instrucciones estándar de código de tres direcciones `PARAM` y `CALL`:

```
MATERIA calculo 8-10 CREDITOS 5
    →  PARAM 8
       PARAM 10
       PARAM 5
       calculo = CALL MATERIA, 3
```

La misma lista de instrucciones (`GeneradorCodigoIntermedio.java`) se muestra en tres formatos equivalentes dentro de la pestaña **Código Intermedio**:

- **Tres direcciones** — texto plano, una instrucción por línea.
- **Triplos** — tabla con columnas `# | Resultado | Operador | Arg1 | Arg2`.
- **Cuádruplos** — tabla con columnas `Operador | Arg1 | Arg2 | Resultado`.

El código intermedio se genera únicamente cuando la sintaxis del programa es válida, y puede exportarse a un archivo `.txt` desde la propia pestaña.

---

## Interfaz gráfica

La ventana principal se divide en un área de código fuente (con numeración de línea) y un panel de análisis con cuatro pestañas:

| Pestaña | Contenido |
|---|---|
| **Tokens** | Tabla de tokens del último análisis, coloreada por tipo |
| **Tabla de símbolos** | Frecuencia de cada símbolo reconocido |
| **Catálogo de errores** | Referencia estática de todos los códigos E-L, E-S y E-M |
| **Código Intermedio** | Tres direcciones, triplos y cuádruplos del último análisis |

Al presionar **Analizar** se ejecutan las cuatro fases en orden y se actualizan todos los paneles. Los botones **Simulación** y **Ver Árbol** se habilitan solo si la sintaxis es válida:

- **Simulación** abre un horario semanal donde cada materia se pinta en sus días y horas calculados, con las recomendaciones del compilador (choques, materias sin horario, etc.).
- **Ver Árbol** muestra el árbol sintáctico del programa analizado.

### Colores de la tabla de tokens

| Color | Tipo de token |
|---|---|
| Azul claro | `RESERVADA` |
| Verde claro | `IDENTIFICADOR` |
| Amarillo claro | `NUMERO` |
| Gris | `GUION`, `DOS_PUNTOS`, `COMA` |
| Rojo | `ERROR_CHAR` |

En el editor, las líneas con errores se resaltan por fase: rojo (léxico), naranja (sintáctico) y verde (semántico); si una línea tiene errores de varias fases, se muestra el color de la más grave.

---

## Cómo ejecutar el proyecto

### Desde NetBeans
1. Abrir el proyecto en NetBeans.
2. Presionar **F6** (Run Project). La clase principal es `horafix.Interfaz`.

### Desde la terminal
```bash
# Compilar
javac -encoding UTF-8 -d build/classes src/horafix/*.java

# Ejecutar
java -cp build/classes horafix.Interfaz
```

### Regenerar el lexer (si se modifica `Lexer.flex`)
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
| Análisis semántico | ✔ Completo |
| Generación de código intermedio | ✔ Completo |
