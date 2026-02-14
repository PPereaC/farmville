package com.ppereac.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    
    private static Connection connection;

    public static Connection getConnection(Properties props) throws SQLException {
        // Si no hay conexión o se ha cerrado, se crea una nueva
        if (connection == null || connection.isClosed()) {
            try {
                // Se obtienen los datos del fichero properties
                String url = props.getProperty("db.url");
                String user = props.getProperty("db.user");
                String pass = props.getProperty("db.password");

                // Se crea la conexión
                connection = DriverManager.getConnection(url, user, pass);

                // VIP: Se desactiva el auto-guardado para manejar las transacciones manualmente
                connection.setAutoCommit(false);
                
                LogHelper.info("> Conexión a la BDD establecida correctamente.");

            } catch (SQLException e) {
                throw new SQLException("Error conectando a la BDD: " + e.getMessage(), e);
            }
        }
        return connection;
    }
}
