package com.ppereac;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import com.ppereac.service.CsvLoader;
import com.ppereac.util.LogHelper;

public class Main {
    public static void main(String[] args) {

        // Se valida que nos pasen el fichero de propiedades
        if (args.length == 0) {
            LogHelper.info("❌ ERROR: Debes proporcionar la ruta del fichero de propiedades como argumento.");
            LogHelper.info("Ejemplo: java -jar programa.jar config.properties");
            return;
        }

        String propertiesPath = args[0];
        Properties props = new Properties();

        // Carga de la configuración
        try (FileInputStream fis = new FileInputStream(propertiesPath)) {
            props.load(fis);
            LogHelper.info("⚙️ Configuración cargada correctamente desde: " + propertiesPath);
            
            // Inicio del proceso
            LogHelper.info("🚀 Iniciando proceso de carga...");
            
            CsvLoader loader = new CsvLoader(props);
            loader.procesar();
            
            LogHelper.info("🏁 Proceso finalizado.");
            
        } catch (FileNotFoundException e) {
            LogHelper.info("❌ No se encuentra el fichero de propiedades: " + propertiesPath);
        } catch (IOException e) {
            LogHelper.error("❌ Error leyendo el fichero de propiedades", e);
        } catch (Exception e) {
            LogHelper.error("❌ Error en la aplicación", e);
        }
    }
}