package ar.edu.fcefyn.pcp.tp2.utilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidadorRegex {
    // Cantidad de T-invariantes
    public static final int INVARIANTES_ESPERADOS = 200;

    // Patrones de los 3 T-invariantes de la red
    private static final Pattern PATRON_CAMINO_1 = Pattern.compile(
            "^(.*)\\bT0\\b(.*?)\\bT1\\b(.*?)\\bT2\\b(.*?)\\bT3\\b(.*?)\\bT9\\b(.*)$"
    );
    private static final Pattern PATRON_CAMINO_2 = Pattern.compile(
            "^(.*)\\bT0\\b(.*?)\\bT4\\b(.*?)\\bT5\\b(.*?)\\bT9\\b(.*)$"
    );
    private static final Pattern PATRON_CAMINO_3 = Pattern.compile(
            "^(.*)\\bT0\\b(.*?)\\bT6\\b(.*?)\\bT7\\b(.*?)\\bT8\\b(.*?)\\bT9\\b(.*)$"
    );

   //lee el archivo log y valida
    public ResultadoValidacion validarArchivo(Path rutaLog) throws IOException {
        String contenidoCrudo = Files.readString(rutaLog);
        return validar(contenidoCrudo);
    }

   //valida el log de a una cadena
    public ResultadoValidacion validar(String logCrudo) {
        if (logCrudo == null || logCrudo.isBlank()) {
            return new ResultadoValidacion(false, 0, 0, 0, 0, "", "El log está vacío.");
        }

        //placeholder hasta tener formato de salida
        String cadenaTrabajo = extraerSecuenciaTransiciones(logCrudo);

        int c1 = 0;
        int c2 = 0;
        int c3 = 0;
        boolean huboCoincidencia;

        // iteracion expresiones regulares
        do {
            huboCoincidencia = false;

            Matcher m1 = PATRON_CAMINO_1.matcher(cadenaTrabajo);
            if (m1.matches()) {
                c1++;
                cadenaTrabajo = reconstruirRemanente(m1, 6);
                huboCoincidencia = true;
                continue;
            }

            Matcher m2 = PATRON_CAMINO_2.matcher(cadenaTrabajo);
            if (m2.matches()) {
                c2++;
                cadenaTrabajo = reconstruirRemanente(m2, 5);
                huboCoincidencia = true;
                continue;
            }

            Matcher m3 = PATRON_CAMINO_3.matcher(cadenaTrabajo);
            if (m3.matches()) {
                c3++;
                cadenaTrabajo = reconstruirRemanente(m3, 6);
                huboCoincidencia = true;
            }

        } while (huboCoincidencia && !cadenaTrabajo.isEmpty());

        int total = c1 + c2 + c3;

        // verificacion de log y totalidad de transiciones
        if (!cadenaTrabajo.isEmpty()) {
            return new ResultadoValidacion(
                    false, total, c1, c2, c3, cadenaTrabajo,
                    "Quedaron transiciones residuales sin completar un T-invariante: " + cadenaTrabajo
            );
        }

        if (total != INVARIANTES_ESPERADOS) {
            return new ResultadoValidacion(
                    false, total, c1, c2, c3, "",
                    String.format("Cantidad incorrecta de invariantes: se esperaban %d y se encontraron %d.",
                            INVARIANTES_ESPERADOS, total)
            );
        }

        return new ResultadoValidacion(
                true, total, c1, c2, c3, "",
                String.format("Validación exitosa: %d invariantes (C1=%d, C2=%d, C3=%d).", total, c1, c2, c3)
        );
    }

   //corregir cuando se tenga el logger
    private String extraerSecuenciaTransiciones(String logCrudo) {
        Matcher extractor = Pattern.compile("\\bT[0-9]+\\b").matcher(logCrudo);
        List<String> tokens = new ArrayList<>();
        while (extractor.find()) {
            tokens.add(extractor.group());
        }
        return String.join(" ", tokens);
    }

    private String reconstruirRemanente(Matcher m, int cantidadGrupos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= cantidadGrupos; i++) {
            String grupo = m.group(i).trim();
            if (!grupo.isEmpty()) {
                if (!sb.isEmpty()) {
                    sb.append(" ");
                }
                sb.append(grupo);
            }
        }
        return sb.toString();
    }
}
