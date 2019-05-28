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

    private Connection getConnection() throws JSQLException {

        OptionsResponse optionsResponse = jsqlConnector.requestOptions();

        try {

            Class.forName(optionsResponse.databaseDialect.driverName);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new JSQLException("JSQL JSQLConnectionProvider.getConnection: " + e.getMessage());
        }

        Properties properties = new Properties();

        String username;
        String password;
        String connectionUrl;

        if(optionsResponse.prod){
            username = optionsResponse.productionDatabaseOptions.databaseConnectionUsername;
            password = optionsResponse.productionDatabaseOptions.databaseConnectionPassword;
            connectionUrl = optionsResponse.productionDatabaseOptions.databaseConnectionUrl;
        }else{
            username = optionsResponse.developerDatabaseOptions.databaseConnectionUsername;
            password = optionsResponse.developerDatabaseOptions.databaseConnectionPassword;
            connectionUrl = optionsResponse.developerDatabaseOptions.databaseConnectionUrl;
        }

        if(username != null && password != null){
            properties.put("user", username);
            properties.put("password", password);
        }

        switch (optionsResponse.databaseDialect){
            case MYSQL:
                connectionUrl = "jdbc:mysql://" + connectionUrl;
                break;
            case POSTGRES:
                connectionUrl = "jdbc:postgresql://" + connectionUrl;
                break;
        }

        Connection connection;

        try {
            connection = DriverManager.getConnection(connectionUrl, properties);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new JSQLException("JSQL JSQLConnectionProvider.getConnection: " + e.getMessage() + " " + e.getSQLState());
        }

        return connection;

    }

    public Connection resolveConnection(String transactionId, Boolean isTransaction) throws JSQLException {

        if (!isTransaction) {
            return this.getConnection();
        }

        Connection transactionalConnection = connections.get(transactionId);

        System.out.println("transactionId : "+transactionId);
        System.out.println("transactionalConnection : "+transactionalConnection);


        try {

            if (transactionalConnection != null && !transactionalConnection.isClosed()) {
                return transactionalConnection;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new JSQLException("JSQL JSQLConnectionProvider.resolveConnection: " + e.getMessage() + " " + e.getSQLState());
        }

//        if (transactionalConnection == null) {
//            removeConnection(transactionId);
//            throw new JSQLException("JSQL JSQLConnectionProvider.resolveConnection: Connection for given transaction does not exist");
//        }

        try {

            if (transactionalConnection != null && transactionalConnection.isClosed()) {
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

    public void removeConnection(Connection connection) throws JSQLException {

        try {

            if(!connection.isClosed()){
                connection.close();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new JSQLException("JSQL JSQLConnectionProvider.removeConnection: " + e.getMessage() + " " + e.getSQLState());
        }

    }

    public void removeConnection(Connection connection, String transactionId) throws JSQLException {

        OptionsResponse optionsResponse = jsqlConnector.requestOptions();
        Integer connectionTimeout;

        if(optionsResponse.prod){
            connectionTimeout = optionsResponse.productionDatabaseOptions.databaseConnectionTimeout;
        }else{
            connectionTimeout = optionsResponse.developerDatabaseOptions.databaseConnectionTimeout;
        }

        Integer finalConnectionTimeout = connectionTimeout;

        new Thread(() -> {

            try {


                Thread.sleep(finalConnectionTimeout *1000);

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
