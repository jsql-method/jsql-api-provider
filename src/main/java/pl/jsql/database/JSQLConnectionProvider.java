package pl.jsql.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.jsql.dto.OptionsResponse;
import pl.jsql.exceptions.JSQLException;
import pl.jsql.service.JSQLConnector;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Service
public class JSQLConnectionProvider {

    @Autowired
    private JSQLConnector jsqlConnector;

    private static Map<String, Connection> connections = new HashMap<>();

    private Connection getConnection() {

        OptionsResponse optionsResponse = jsqlConnector.requestOptions();

        try {

            Class.forName(optionsResponse.databaseDialect.driverName);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new JSQLException("JSQL JSQLConnectionProvider.getConnection: " + e.getMessage());
        }

        Properties properties = new Properties();
        properties.put("user", optionsResponse.databaseConnectionUsername);
        properties.put("password", optionsResponse.databaseConnectionPassword);

        Connection connection;

        try {

            connection = DriverManager.getConnection(optionsResponse.databaseConnectionUrl, properties);

        } catch (SQLException e) {
            e.printStackTrace();
            throw new JSQLException("JSQL JSQLConnectionProvider.getConnection: " + e.getMessage() + " " + e.getSQLState());
        }

        return connection;

    }

    public Connection resolveConnection(String transactionId, Boolean isTransaction) {

        if (!isTransaction) {
            return this.getConnection();
        }

        Connection transactionalConnection = connections.get(transactionId);

        try {

            if (transactionalConnection != null && !transactionalConnection.isClosed()) {
                return transactionalConnection;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new JSQLException("JSQL JSQLConnectionProvider.resolveConnection: " + e.getMessage() + " " + e.getSQLState());
        }

        if (transactionalConnection == null) {
            removeConnection(transactionId);
            throw new JSQLException("JSQL JSQLConnectionProvider.resolveConnection: Connection for given transaction does not exist");
        }

        try {

            if (transactionalConnection.isClosed()) {
                removeConnection(transactionalConnection, transactionId);
                throw new JSQLException("JSQL JSQLConnectionProvider.resolveConnection: Connection for given transaction is closed");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new JSQLException("JSQL JSQLConnectionProvider.resolveConnection: " + e.getMessage() + " " + e.getSQLState());
        }

        transactionalConnection = this.getConnection();

        try {

            transactionalConnection.setAutoCommit(false);

        } catch (SQLException e) {
            e.printStackTrace();
            throw new JSQLException("JSQL JSQLConnectionProvider.resolveConnection: " + e.getMessage() + " " + e.getSQLState());
        }


        connections.put(transactionId, transactionalConnection);

        return transactionalConnection;

    }

    public void removeConnection(Connection connection) {

        try {

            if(!connection.isClosed()){
                connection.close();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new JSQLException("JSQL JSQLConnectionProvider.removeConnection: " + e.getMessage() + " " + e.getSQLState());
        }

    }

    public void removeConnection(Connection connection, String transactionId) {

        OptionsResponse optionsResponse = jsqlConnector.requestOptions();

        new Thread(() -> {

            try {


                Thread.sleep(optionsResponse.databaseConnectionTimeout);

                if (!connection.isClosed()) {

                    connection.rollback();
                    connection.close();
                }

                connections.remove(transactionId);

            } catch (SQLException | InterruptedException e) {
                e.printStackTrace();
            }

        }).start();

    }

    private void removeConnection(String transactionId) {
        connections.remove(transactionId);
    }

}
