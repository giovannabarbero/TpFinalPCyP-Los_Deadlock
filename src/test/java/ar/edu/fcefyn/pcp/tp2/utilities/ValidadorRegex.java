package ar.edu.fcefyn.pcp.tp2.utilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidadorRegex {

    public static final int INVARIANTES_ESPERADOS = 200;

    // Única expresión regular con los 3 T-invariantes como alternativas
    private static final Pattern PATRON_INVARIANTE = Pattern.compile(
            "^(.*)\\bT0\\b(.*?)(?:" +
                    "\\bT1\\b(.*?)\\bT2\\b(.*?)\\bT3\\b|" +
                    "\\bT4\\b(.*?)\\bT5\\b|" +
                    "\\bT6\\b(.*?)\\bT7\\b(.*?)\\bT8\\b" +
                    ")(.*?)\\bT9\\b(.*)$"
    );

    public ResultadoValidacion validarArchivo(Path rutaLog) throws IOException {
        String contenidoCrudo = Files.readString(rutaLog);
        return validar(contenidoCrudo);
    }

    public ResultadoValidacion validar(String logCrudo) {
        if (logCrudo == null || logCrudo.isBlank()) {
            return new ResultadoValidacion(false, 0, 0, 0, 0, "", "El log está vacío.");
        }

        String cadenaTrabajo = extraerSecuenciaTransiciones(logCrudo);

        int c1 = 0; // Tarjeta: T0 -> T1 -> T2 -> T3 -> T9
        int c2 = 0; // Alto riesgo: T0 -> T4 -> T5 -> T9
        int c3 = 0; // Transferencia: T0 -> T6 -> T7 -> T8 -> T9
        boolean huboCoincidencia;

        do {
            huboCoincidencia = false;
            Matcher m = PATRON_INVARIANTE.matcher(cadenaTrabajo);

            if (m.matches()) {
                if (m.group(3) != null) {
                    c1++;
                } else if (m.group(5) != null) {
                    c2++;
                } else {
                    c3++;
                }

                cadenaTrabajo = reconstruirRemanente(m);
                huboCoincidencia = true;
            }
        } while (huboCoincidencia && !cadenaTrabajo.isEmpty());

        int total = c1 + c2 + c3;

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

    private String extraerSecuenciaTransiciones(String logCrudo) {
        Matcher extractor = Pattern.compile("\\bT[0-9]+\\b").matcher(logCrudo);
        List<String> tokens = new ArrayList<>();
        while (extractor.find()) {
            tokens.add(extractor.group());
        }
        return String.join(" ", tokens);
    }

    private String reconstruirRemanente(Matcher m) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= m.groupCount(); i++) {
            String grupo = m.group(i);
            if (grupo != null) {
                String limpio = grupo.trim();
                if (!limpio.isEmpty()) {
                    if (!sb.isEmpty()) {
                        sb.append(" ");
                    }
                    sb.append(limpio);
                }
            }
        }
        return sb.toString();
    }
}