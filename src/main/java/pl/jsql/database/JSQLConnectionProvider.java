package pl.jsql.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.jsql.ApiProviderApplication;
import pl.jsql.dto.Keys;
import pl.jsql.dto.OptionsResponse;
import pl.jsql.exceptions.JSQLException;
import pl.jsql.service.JSQLConnector;
import pl.jsql.service.JSQLUtils;
import pl.jsql.service.SecurityService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

@Service
public class JSQLConnectionProvider {

    @Autowired
    private JSQLConnector jsqlConnector;

    @Autowired
    private SecurityService securityService;

    private static Map<String, Connection> connections = new HashMap<>();
    private static Map<String, Long> connectionsTime = new HashMap<>();
    private static Map<String, Keys> connectionKeys = new HashMap<>();

    private static final Long DELAY = 10 * 1000L; //10 sec

    public void clearUnusedConnections() throws JSQLException {

        Iterator it = connectionsTime.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();

            Long time = (Long) pair.getValue();
            Long now = new Date().getTime();

            if (now - time > DELAY) {
                removeConnection(connections.get(pair.getKey()), (String) pair.getKey());
                it.remove();
            }

        }

    }

    private Connection getConnection(boolean saveInMap) throws JSQLException {

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

        if (optionsResponse.isProductionDeveloper && !ApiProviderApplication.isLocalVersion) {
            username = optionsResponse.productionDatabaseOptions.databaseConnectionUsername;
            password = optionsResponse.productionDatabaseOptions.databaseConnectionPassword;
            connectionUrl = optionsResponse.productionDatabaseOptions.databaseConnectionUrl;
        } else {
            username = optionsResponse.developerDatabaseOptions.databaseConnectionUsername;
            password = optionsResponse.developerDatabaseOptions.databaseConnectionPassword;
            connectionUrl = optionsResponse.developerDatabaseOptions.databaseConnectionUrl;
        }

        if (username != null && password != null) {
            properties.put("user", username);
            properties.put("password", password);
        }

        switch (optionsResponse.databaseDialect) {
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

        if(saveInMap){
            String connectionUuid = JSQLUtils.uuid();
            connectionsTime.put(connectionUuid, new Date().getTime());
            connections.put(connectionUuid, connection);
            connectionKeys.put(connectionUuid, new Keys(securityService.getApiKey(), securityService.getDevKey()));
        }

        return connection;

    }

    public Connection resolveConnection(String transactionId, Boolean isTransaction) throws JSQLException {

        if (!isTransaction) {
            return this.getConnection(true);
        }

        Connection transactionalConnection = connections.get(transactionId);

        try {

            if (transactionalConnection != null && !transactionalConnection.isClosed()) {
                connectionsTime.put(transactionId, new Date().getTime());
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

        transactionalConnection = this.getConnection(false);

        try {
            transactionalConnection.setAutoCommit(false);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new JSQLException("JSQL JSQLConnectionProvider.resolveConnection: " + e.getMessage() + " " + e.getSQLState());
        }


        connections.put(transactionId, transactionalConnection);
        connectionsTime.put(transactionId, new Date().getTime());
        connectionKeys.put(transactionId, new Keys(securityService.getApiKey(), securityService.getDevKey()));

        return transactionalConnection;

    }

    public void removeConnection(Connection connection) throws JSQLException {

        try {

            if (!connection.isClosed()) {
                connection.close();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new JSQLException("JSQL JSQLConnectionProvider.removeConnection: " + e.getMessage() + " " + e.getSQLState());
        }finally {

            try {
                connection.close();
            } catch (Exception ex) {
                System.out.println("Could not remove connection");
                ex.printStackTrace();
            }

        }

    }

    public void removeConnection(Connection connection, String transactionId) throws JSQLException {

        Keys keys = connectionKeys.get(transactionId);

        OptionsResponse optionsResponse = jsqlConnector.requestOptions(keys.apiKey, keys.devKey);
        Integer connectionTimeout;

        if (optionsResponse.isProductionDeveloper) {
            connectionTimeout = optionsResponse.productionDatabaseOptions.databaseConnectionTimeout;
        } else {
            connectionTimeout = optionsResponse.developerDatabaseOptions.databaseConnectionTimeout;
        }

        Integer finalConnectionTimeout = connectionTimeout;

        new Thread(() -> {

            try {

                Thread.sleep(finalConnectionTimeout * 1000);

                if (!connection.isClosed()) {

                    if(!connection.getAutoCommit()){
                        connection.rollback();
                    }

                    connection.close();
                }

                this.removeConnection(transactionId);

            } catch (SQLException | InterruptedException e) {
                e.printStackTrace();
            } finally {

                try {
                    connection.close();
                } catch (Exception ex) {
                    System.out.println("Could not remove connection");
                    ex.printStackTrace();
                }

            }

        }).start();

    }

    private void removeConnection(String transactionId) {
        connections.remove(transactionId);
        connectionsTime.remove(transactionId);
    }

}
