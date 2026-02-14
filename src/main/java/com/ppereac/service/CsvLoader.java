package com.ppereac.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.ppereac.util.DatabaseConnection;
import com.ppereac.util.LogHelper;

import java.io.FileReader;
import java.io.Reader;
import java.sql.*;
import java.util.Properties;

public class CsvLoader {

    private final Properties props;
    private final Connection conn;

    public CsvLoader(Properties props) throws SQLException {
        this.props = props;
        this.conn = DatabaseConnection.getConnection(props);
    }

    public void procesar() {
        // Orden de archivos a procesar según el enunciado
        String[] ordenArchivos = {
                "granjeros.csv",
                "plantaciones.csv",
                "riegos.csv",
                "construcciones.csv",
                "tractores.csv",
                "granjero_granjero.csv"
        };

        for (String nombreArchivo : ordenArchivos) {
            procesarArchivo(nombreArchivo);
        }
    }

    private void procesarArchivo(String nombreArchivo) {
        String nombreSinExtension = nombreArchivo.replace(".csv", "");
        String rutaCompleta = props.getProperty("ruta.csv") + nombreSinExtension + "/" + nombreArchivo;
        Savepoint savepoint = null;

        LogHelper.info(">>> Iniciando carga de: " + nombreArchivo);

        try (Reader reader = new FileReader(rutaCompleta)) {

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader() // Detecta la cabecera automáticamente
                    .setSkipHeaderRecord(true) // Salta la primera línea (porque es la cabecera)
                    .setIgnoreHeaderCase(true) // Ignora mayúsculas/minúsculas en los nombres de las columnas
                    .setTrim(true) // Quita espacios en blanco sobrantes
                    .get();

            try (CSVParser csvParser = format.parse(reader)) {

                // Creación de un Savepoint, para poder hacer rollback en caso de error en este
                // archivo
                // sin afectar a los archivos anteriores que ya se han procesado correctamente.
                savepoint = conn.setSavepoint("Savepoint_" + nombreArchivo);

                for (CSVRecord record : csvParser) {
                    switch (nombreArchivo) {
                        case "granjeros.csv":
                            procesarGranjero(record);
                            break;
                        case "plantaciones.csv":
                            procesarPlantacion(record);
                            break;
                        case "riegos.csv":
                            procesarRiego(record);
                            break;
                        case "construcciones.csv":
                            procesarConstruccion(record);
                            break;
                        case "tractores.csv":
                            procesarTractor(record);
                            break;
                        case "granjero_granjero.csv":
                            procesarGranjeroGranjero(record);
                            break;
                    }
                }
            }

            // Commit si todo va bien
            conn.commit();
            LogHelper.info("✔ Fichero " + nombreArchivo + " cargado y confirmado (COMMIT).");

        } catch (Exception e) {
            // ERROR CRÍTICO
            LogHelper.error("✘ Error procesando " + nombreArchivo, e);

            try {
                // 3. Rollback parcial
                if (savepoint != null) {
                    conn.rollback(savepoint);
                    LogHelper.info("↺ Se ha realizado ROLLBACK de " + nombreArchivo);
                }

                // 4. Log con errores
                LogHelper.logError(props.getProperty("ruta.errores"),
                        "Fichero: " + nombreArchivo + " | Error: " + e.getMessage());

                // 5. Parar ejecución
                LogHelper.info("Deteniendo ejecución del programa debido a un error.");
                System.exit(1);

            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    // Método para procesar cada registro de granjero
    private void procesarGranjero(CSVRecord record) throws SQLException {

        // Convertir dato de String a int/double)
        int id = Integer.parseInt(record.get("id"));

        String nombre = record.get("nombre");
        String descripcion = record.get("descripcion");
        double dinero = Double.parseDouble(record.get("dinero"));
        int puntos = Integer.parseInt(record.get("puntos"));
        int nivel = Integer.parseInt(record.get("nivel"));

        String sqlCheck = "SELECT * FROM granjeros WHERE id = ?";
        try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
            psCheck.setInt(1, id);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                // Ya existe. Comprobamos si hay cambios
                boolean esDiferente = false;

                // Comparamos campos
                if (!rs.getString("nombre").equals(nombre))
                    esDiferente = true;
                if (!rs.getString("descripcion").equals(descripcion))
                    esDiferente = true;
                if (rs.getDouble("dinero") != dinero)
                    esDiferente = true;
                if (rs.getInt("puntos") != puntos)
                    esDiferente = true;
                if (rs.getInt("nivel") != nivel)
                    esDiferente = true;

                if (esDiferente) {
                    // Existe pero es diferente. Hacemos update
                    String sqlUpdate = "UPDATE granjeros SET nombre=?, descripcion=?, dinero=?, puntos=?, nivel=? WHERE id=?";
                    try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                        psUpdate.setString(1, nombre);
                        psUpdate.setString(2, descripcion);
                        psUpdate.setDouble(3, dinero);
                        psUpdate.setInt(4, puntos);
                        psUpdate.setInt(5, nivel);
                        psUpdate.setInt(6, id);
                        psUpdate.executeUpdate();
                    }
                } else {
                    // Duplicado exacto. Se registra en el log de duplicados
                    LogHelper.logDuplicado(props.getProperty("ruta.duplicados"),
                            "Granjero ID " + id + " (" + nombre + ") ya existe y es idéntico.");
                }

            } else {
                // No existe. Lo insertamos
                String sqlInsert = "INSERT INTO granjeros (id, nombre, descripcion, dinero, puntos, nivel) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement psInsert = conn.prepareStatement(sqlInsert)) {
                    psInsert.setInt(1, id);
                    psInsert.setString(2, nombre);
                    psInsert.setString(3, descripcion);
                    psInsert.setDouble(4, dinero);
                    psInsert.setInt(5, puntos);
                    psInsert.setInt(6, nivel);
                    psInsert.executeUpdate();
                }
            }
        }
    }

    private void procesarPlantacion(CSVRecord record) throws SQLException {
        int id = Integer.parseInt(record.get("id"));
        String nombre = record.get("nombre");
        double precioCompra = Double.parseDouble(record.get("precio_compra"));
        double precioVenta = Double.parseDouble(record.get("precio_venta"));
        Timestamp proximaCosecha = Timestamp.valueOf(record.get("proxima_cosecha"));
        int idGranjero = Integer.parseInt(record.get("id_granjero"));

        String sqlCheck = "SELECT * FROM plantaciones WHERE id = ?";
        try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
            psCheck.setInt(1, id);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {

                boolean esDiferente = false;

                // Comparamos campos
                if (!rs.getString("nombre").equals(nombre))
                    esDiferente = true;
                if (rs.getDouble("precio_compra") != precioCompra)
                    esDiferente = true;
                if (rs.getDouble("precio_venta") != precioVenta)
                    esDiferente = true;
                if (!rs.getTimestamp("proxima_cosecha").equals(proximaCosecha))
                    esDiferente = true;
                if (rs.getInt("id_granjero") != idGranjero)
                    esDiferente = true;

                if (esDiferente) {
                    // Existe pero es diferente. Hacemos update
                    String sqlUpdate = "UPDATE plantaciones SET nombre=?, precio_compra=?, precio_venta=?, proxima_cosecha=?, id_granjero=? WHERE id=?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                        ps.setString(1, nombre);
                        ps.setDouble(2, precioCompra);
                        ps.setDouble(3, precioVenta);
                        ps.setTimestamp(4, proximaCosecha);
                        ps.setInt(5, idGranjero);
                        ps.setInt(6, id);
                        ps.executeUpdate();
                    }
                } else {
                    // Duplicado exacto. Se registra en el log de duplicados
                    LogHelper.logDuplicado(props.getProperty("ruta.duplicados"),
                            "Plantación ID " + id + " (" + nombre + ") ya existe y es idéntica.");
                }

            } else {
                // No existe. Lo insertamos
                String sqlInsert = "INSERT INTO plantaciones (id, nombre, precio_compra, precio_venta, proxima_cosecha, id_granjero) VALUES (?,?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
                    ps.setInt(1, id);
                    ps.setString(2, nombre);
                    ps.setDouble(3, precioCompra);
                    ps.setDouble(4, precioVenta);
                    ps.setTimestamp(5, proximaCosecha);
                    ps.setInt(6, idGranjero);
                    ps.executeUpdate();
                }
            }
        }
    }

    private void procesarRiego(CSVRecord record) throws SQLException {
        int id = Integer.parseInt(record.get("id"));
        String tipo = record.get("tipo");
        double velocidad = Double.parseDouble(record.get("velocidad"));
        int idPlantacion = Integer.parseInt(record.get("id_plantacion"));

        String sqlCheck = "SELECT * FROM riegos WHERE id = ?";
        try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
            psCheck.setInt(1, id);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {

                boolean esDiferente = false;

                // Comparamos campos
                if (!rs.getString("tipo").equals(tipo))
                    esDiferente = true;
                if (rs.getDouble("velocidad") != velocidad)
                    esDiferente = true;
                if (rs.getInt("id_plantacion") != idPlantacion)
                    esDiferente = true;

                if (esDiferente) {
                    // Existe pero es diferente. Hacemos update
                    String sqlUpdate = "UPDATE riegos SET tipo=?, velocidad=?, id_plantacion=? WHERE id=?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                        ps.setString(1, tipo);
                        ps.setDouble(2, velocidad);
                        ps.setInt(3, idPlantacion);
                        ps.setInt(4, id);
                        ps.executeUpdate();
                    }
                } else {
                    // Duplicado exacto. Se registra en el log de duplicados
                    LogHelper.logDuplicado(props.getProperty("ruta.duplicados"),
                            "Riego ID " + id + " (" + tipo + ") ya existe y es idéntico.");
                }
            } else {
                // No existe. Lo insertamos
                String sqlInsert = "INSERT INTO riegos (id, tipo, velocidad, id_plantacion) VALUES (?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
                    ps.setInt(1, id);
                    ps.setString(2, tipo);
                    ps.setDouble(3, velocidad);
                    ps.setInt(4, idPlantacion);
                    ps.executeUpdate();
                }
            }
        }
    }

    private void procesarConstruccion(CSVRecord record) throws SQLException {
        int id = Integer.parseInt(record.get("id"));
        String nombre = record.get("nombre");
        double precio = Double.parseDouble(record.get("precio"));

        // Manejo de nulos en id_granjero (algunas construcciones pueden no tener dueño
        // aún)
        String idGranjeroStr = record.get("id_granjero");
        Integer idGranjero = (idGranjeroStr == null || idGranjeroStr.isEmpty()) ? null
                : Integer.parseInt(idGranjeroStr);

        String sqlCheck = "SELECT * FROM construcciones WHERE id = ?";
        try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
            psCheck.setInt(1, id);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {

                boolean esDiferente = false;

                // Comparamos campos
                if (!rs.getString("nombre").equals(nombre))
                    esDiferente = true;
                if (rs.getDouble("precio") != precio)
                    esDiferente = true;

                // Manejo de nulos en id_granjero
                int idGranjeroEnBDD = rs.getInt("id_granjero");
                if (rs.wasNull()) { // Si el valor en la BDD es NULL
                    if (idGranjero != null)
                        esDiferente = true;
                } else { // Si el valor en la BDD no es NULL
                    if (idGranjero == null || idGranjeroEnBDD != idGranjero)
                        esDiferente = true;
                }

                if (esDiferente) {
                    // Existe pero es diferente. Hacemos update
                    String sqlUpdate = "UPDATE construcciones SET nombre=?, precio=?, id_granjero=? WHERE id=?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                        ps.setString(1, nombre);
                        ps.setDouble(2, precio);
                        if (idGranjero == null)
                            ps.setNull(3, Types.INTEGER);
                        else
                            ps.setInt(3, idGranjero);
                        ps.setInt(4, id);
                        ps.executeUpdate();
                    }
                } else {
                    // Duplicado exacto. Se registra en el log de duplicados
                    LogHelper.logDuplicado(props.getProperty("ruta.duplicados"),
                            "Construcción ID " + id + " (" + nombre + ") ya existe y es idéntica.");
                }
            } else {
                // No existe. Lo insertamos
                String sqlInsert = "INSERT INTO construcciones (id, nombre, precio, id_granjero) VALUES (?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
                    ps.setInt(1, id);
                    ps.setString(2, nombre);
                    ps.setDouble(3, precio);
                    if (idGranjero == null)
                        ps.setNull(4, Types.INTEGER);
                    else
                        ps.setInt(4, idGranjero);
                    ps.executeUpdate();
                }
            }
        }
    }

    private void procesarTractor(CSVRecord record) throws SQLException {
        int id = Integer.parseInt(record.get("id"));
        String modelo = record.get("modelo");
        int velocidad = Integer.parseInt(record.get("velocidad"));
        double precioVenta = Double.parseDouble(record.get("precio_venta"));

        String idConstruccionStr = record.get("id_construccion");

        // Manejo de nulos en id_construccion (algunos tractores pueden no estar
        // asignados a una construcción aún)
        Integer idConstruccion = (idConstruccionStr == null || idConstruccionStr.isEmpty()) ? null
                : Integer.parseInt(idConstruccionStr);

        String sqlCheck = "SELECT * FROM tractores WHERE id = ?";
        try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
            psCheck.setInt(1, id);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {

                boolean esDiferente = false;

                // Comparamos campos
                if (!rs.getString("modelo").equals(modelo))
                    esDiferente = true;
                if (rs.getInt("velocidad") != velocidad)
                    esDiferente = true;
                if (rs.getDouble("precio_venta") != precioVenta)
                    esDiferente = true;

                // Manejo de nulos en id_construccion
                int idConstruccionEnBDD = rs.getInt("id_construccion");
                if (rs.wasNull()) { // Si el valor en la BDD es NULL
                    if (idConstruccion != null)
                        esDiferente = true;
                } else { // Si el valor en la BDD no es NULL
                    if (idConstruccion == null || idConstruccionEnBDD != idConstruccion)
                        esDiferente = true;
                }

                if (esDiferente) {
                    // Existe pero es diferente. Hacemos update

                    String sqlUpdate = "UPDATE tractores SET modelo=?, velocidad=?, precio_venta=?, id_construccion=? WHERE id=?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                        ps.setString(1, modelo);
                        ps.setInt(2, velocidad);
                        ps.setDouble(3, precioVenta);
                        if (idConstruccion == null)
                            ps.setNull(4, Types.INTEGER);
                        else
                            ps.setInt(4, idConstruccion);
                        ps.setInt(5, id);
                        ps.executeUpdate();
                    }
                } else {
                    // Duplicado exacto. Se registra en el log de duplicados
                    LogHelper.logDuplicado(props.getProperty("ruta.duplicados"),
                            "Tractor ID " + id + " (" + modelo + ") ya existe y es idéntico.");
                }
            } else {
                // No existe. Lo insertamos
                String sqlInsert = "INSERT INTO tractores (id, modelo, velocidad, precio_venta, id_construccion) VALUES (?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
                    ps.setInt(1, id);
                    ps.setString(2, modelo);
                    ps.setInt(3, velocidad);
                    ps.setDouble(4, precioVenta);
                    if (idConstruccion == null)
                        ps.setNull(5, Types.INTEGER);
                    else
                        ps.setInt(5, idConstruccion);
                    ps.executeUpdate();
                }
            }
        }
    }

    private void procesarGranjeroGranjero(CSVRecord record) throws SQLException {
        int idGranjero = Integer.parseInt(record.get("id_granjero"));
        int idVecino = Integer.parseInt(record.get("id_vecino"));
        int puntosCompartidos = Integer.parseInt(record.get("puntos_compartidos"));

        String sqlCheck = "SELECT * FROM granjero_granjero WHERE id_granjero = ? AND id_vecino = ?";
        try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
            psCheck.setInt(1, idGranjero);
            psCheck.setInt(2, idVecino);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {

                boolean esDiferente = false;

                // Comparamos campos
                if (rs.getInt("puntos_compartidos") != puntosCompartidos)
                    esDiferente = true;

                if (esDiferente) {
                    // Existe pero es diferente. Hacemos update
                    String sqlUpdate = "UPDATE granjero_granjero SET puntos_compartidos=? WHERE id_granjero=? AND id_vecino=?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                        ps.setInt(1, puntosCompartidos);
                        ps.setInt(2, idGranjero);
                        ps.setInt(3, idVecino);
                        ps.executeUpdate();
                    }
                } else {
                    // Duplicado exacto. Se registra en el log de duplicados
                    LogHelper.logDuplicado(props.getProperty("ruta.duplicados"),
                            "Relación Granjero ID " + idGranjero + " - Vecino ID " + idVecino
                                    + " ya existe y es idéntica.");
                }
            } else {
                // No existe. Lo insertamos
                String sqlInsert = "INSERT INTO granjero_granjero (id_granjero, id_vecino, puntos_compartidos) VALUES (?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
                    ps.setInt(1, idGranjero);
                    ps.setInt(2, idVecino);
                    ps.setInt(3, puntosCompartidos);
                    ps.executeUpdate();
                }
            }
        }
    }
}
