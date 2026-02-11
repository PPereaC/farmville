package com.ppereac.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogHelper {

    // Logger para mostrar cosas por consola (en lugar de System.out)
    private static final Logger consoleLogger = LoggerFactory.getLogger(LogHelper.class);

    // Formato para la fecha en los ficheros de log
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    /**
     * Escribe en el fichero de errores.
     * Con el parámetro 'append', que en FileWriter, si es true, acumula los
     * errores.
    */
    public static void logError(String rutaFichero, String mensaje) {
        try (FileWriter fw = new FileWriter(rutaFichero, true);
                PrintWriter pw = new PrintWriter(fw)) {

            pw.println("[" + LocalDateTime.now().format(formatter) + "] ERROR: " + mensaje);

        } catch (IOException e) {
            consoleLogger.error("Fallo crítico!!! No se pudo escribir en el log de errores.", e);
        }
    }

    /**
     * Escribe en el fichero de duplicados.
    */
    public static void logDuplicado(String rutaFichero, String mensaje) {
        try (FileWriter fw = new FileWriter(rutaFichero, true);
                PrintWriter pw = new PrintWriter(fw)) {

            pw.println("[" + LocalDateTime.now().format(formatter) + "] DUPLICADO: " + mensaje);

        } catch (IOException e) {
            consoleLogger.error("No se pudo escribir en el log de duplicados.", e);
        }
    }

    // Métodos auxiliares para no usar System.out en el resto del código
    public static void info(String msg) {
        consoleLogger.info(msg);
    }

    public static void error(String msg, Throwable t) {
        consoleLogger.error(msg, t);
    }
}
