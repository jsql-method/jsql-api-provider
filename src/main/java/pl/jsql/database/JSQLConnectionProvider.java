package pl.jsql.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.jsql.dto.OptionsResponse;
import pl.jsql.exceptions.JSQLException;
import pl.jsql.service.JSQLConnector;
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

    private static final Long MINUTE = 10*1000L;

    public void clearUnusedConnections() throws JSQLException {

        System.out.println("in: "+connections.size());
        System.out.println("in2: "+connectionsTime.size());

        Iterator it = connectionsTime.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();

            Long time = (Long) pair.getValue();
            Long now = new Date().getTime();

            if(now - time > MINUTE){
                removeConnection(connections.get(pair.getKey()), (String) pair.getKey());
                it.remove();
            }

        }

        System.out.println("out: "+connections.size());
        System.out.println("out2: "+connectionsTime.size());

    }

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

        Connection transactionalConnection = connections.get(securityService.getKey()+transactionId);


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

        transactionalConnection = this.getConnection();

        try {
            transactionalConnection.setAutoCommit(false);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new JSQLException("JSQL JSQLConnectionProvider.resolveConnection: " + e.getMessage() + " " + e.getSQLState());
        }


        connections.put(securityService.getKey()+transactionId, transactionalConnection);
        connectionsTime.put(securityService.getKey()+transactionId, new Date().getTime());

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

        String apiKey = transactionId.substring(0, transactionId.lastIndexOf("-"));

        OptionsResponse optionsResponse = jsqlConnector.requestOptions(apiKey);
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

                this.removeConnection(transactionId);

            } catch (SQLException | InterruptedException e) {
                e.printStackTrace();
            }

        }).start();

    }

    private void removeConnection(String transactionId) {
        connections.remove(transactionId);
        connectionsTime.remove(transactionId);
    }

}
