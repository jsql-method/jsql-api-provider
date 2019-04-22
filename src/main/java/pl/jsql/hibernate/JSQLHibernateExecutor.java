package pl.jsql.hibernate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.math.BigInteger;
import java.sql.*;
import java.util.*;

@Repository
public class JSQLHibernateExecutor  {

    private static final long CONNECTION_TIMEOUT = 10000; //10s
    @Autowired
    ApplicationContext applicationContext;

    public static Map<String, Connection> connections = new HashMap<>();

    private volatile static JSQLHibernateExecutor instance;

    private boolean paramsError = false;

    public static JSQLHibernateExecutor getInstance(ApplicationContext applicationContext) {
        if (instance == null) {
            synchronized (JSQLHibernateExecutor.class) {
                if (instance == null) {
                    instance = new JSQLHibernateExecutor(applicationContext);
                }
            }
        }

        return instance;
    }

    public static JSQLHibernateExecutor getInstance() {
        if (instance == null) {
            synchronized (JSQLHibernateExecutor.class) {
                if (instance == null) {
                    instance = new JSQLHibernateExecutor();
                }
            }
        }

        return instance;
    }


    public JSQLHibernateExecutor() {
    }

    public JSQLHibernateExecutor(ApplicationContext applicationContext) {
        this.setApplicationContext(applicationContext);
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void executeSelect(String sql, Map<String, Object> params, TransactionThread transactionThread) {

        executeSQL(sql, params, transactionThread, JSQLQueryType.SELECT);

    }

    @Override
    public void executeDelete(String sql, Map<String, Object> params, TransactionThread transactionThread) {

        executeSQL(sql, params, transactionThread, JSQLQueryType.UPDATE_AND_DELETE);
    }

    @Override
    public void executeUpdate(String sql, Map<String, Object> params, TransactionThread transactionThread) {

        executeSQL(sql, params, transactionThread, JSQLQueryType.UPDATE_AND_DELETE);

    }

    @Override
    public void executeInsert(String sql, Map<String, Object> params, TransactionThread transactionThread) {
        executeSQL(sql, params, transactionThread, JSQLQueryType.INSERT);
    }

    private void executeSQL(String sql, Map<String, Object> params, TransactionThread transactionThread, JSQLQueryType queryType) {

        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> response = new HashMap<>();
        Connection connection = null;

        try {

            if (transactionThread.getTransactional()) {

                String transactionId = transactionThread.getTransactionId();

                if (transactionId == null || transactionId.isEmpty()) {

                    transactionId = UUID.randomUUID().toString();

                    transactionThread.setTransactionId(transactionId);

                }

                connection = getConnection(transactionId);

            } else {

                connection = getConnection();

            }
            String finalSql = sql;
            Map<Integer, Object> psParams = new HashMap<>();
            if (!transactionThread.getParamsAsArray()) {
                finalSql = substituteParams(sql, params);
                psParams = getParamsMap(sql, params);
            } else {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    System.out.println(entry.getKey());
                    System.out.println(entry.getValue());
                    psParams.put(Integer.valueOf(entry.getKey()), entry.getValue());
                }
            }


            PreparedStatement ps = connection.prepareStatement(finalSql);
            ResultSet rs;

            for (Map.Entry<Integer, Object> entry : psParams.entrySet()) {
                ps.setObject(entry.getKey(), entry.getValue());
            }

            switch (queryType) {
                case SELECT:
                    rs = ps.executeQuery();

                    ResultSetMetaData resultSetMetaData = rs.getMetaData();

                    while (rs.next()) {

                        Map<String, Object> map = new HashMap<>();

                        for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {

                            map.put(toCamelCase(resultSetMetaData.getColumnName(i)), rs.getObject(i));

                        }

                        list.add(map);

                    }

                    break;

                case INSERT:
                    Long lastId = -1l;

                    if (JSQLService.DATABASE_DIALECT.equals("POSTGRES")) {

                        finalSql = buildReturningId(finalSql);
                        ps = connection.prepareStatement(finalSql);
                        for (Map.Entry<Integer, Object> entry : psParams.entrySet()) {
                            ps.setObject(entry.getKey(), entry.getValue());
                        }
                        rs = ps.executeQuery();

                        while (rs.next()) {

                            lastId = rs.getLong(1);

                        }

                    } else {

                        ps.executeUpdate();
                        ps = connection.prepareStatement("SELECT LAST_INSERT_ID()");
                        rs = ps.executeQuery();

                        while (rs.next()) {

                            lastId = rs.getLong(1);

                        }

                    }
                    response.put("lastId", BigInteger.valueOf(lastId));
                    list.add(response);

                    break;

                case UPDATE_AND_DELETE:
                    ps.executeUpdate();
                    response.put("status", "OK");
                    list.add(response);

                    break;

                default:
                    response.put("status", "Unknown error");
                    list.add(response);

                    break;

            }
            if (!ps.isClosed()) ps.close();

        } catch (Exception e) {
            e.printStackTrace();
            getExceptionCause(list, response, e);
        } finally {
            try {
                if (!transactionThread.getTransactional() && connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        transactionThread.setResponse(list);
    }

    @Override
    public Map<String, String> commit(String txid) {
        Connection connection = connections.get(txid);

        if (connection != null) {

            try {

                connection.commit();

                if (!connection.isClosed()) connection.close();

            } catch (SQLException e) {

                try {

                    connection.rollback();
                    if (!connection.isClosed()) connection.close();

                } catch (SQLException e1) {
                    e1.printStackTrace();

                }

                e.printStackTrace();
                connections.remove(txid);
                return Collections.singletonMap("Status", "Error occurred");

            }

            connections.remove(txid);

        }

        return Collections.singletonMap("Status", "OK");
    }

    private void getExceptionCause(List<Map<String, Object>> list, Map<String, Object> response, Exception
            e) {
        Throwable cause = e;

        while (cause.getCause() != null && cause.getCause() != cause) {

            cause = cause.getCause();

        }

        response.put("code", 400);

        if (paramsError) {

            response.put("description", ErrorMessagesSingleton.getInstance().getMessage());
            paramsError = false;

        } else {

            response.put("description", cause.getMessage() != null ? cause.getMessage().split("\n")[0]
                    : e.getMessage() != null ? e.getMessage()
                    : ErrorMessagesSingleton.getInstance().getMessage() != null ? ErrorMessagesSingleton.getInstance().getMessage()
                    : "No message available");

        }

        list.add(response);

    }

    private Map<Integer, Object> getParamsMap(String sql, Map<String, Object> params) {
        Map<Integer, Object> paramsMapWithIndex = new HashMap<>();
        String[] splittedSQL = sql.split("\\s+");
        int index = 1;
        for (String param : splittedSQL) {
            if (param.contains(":") && !param.contains("'") && !param.contains("\"") && !param.contains("`")) {
                String cleanParam = param.substring(param.indexOf(':'));
                cleanParam = StringUtils.trimAllWhitespace(cleanParam.replaceAll("[=;,:()]", ""));
                paramsMapWithIndex.put(index, params.get(cleanParam));
                index++;
            }
        }
        return paramsMapWithIndex;
    }

    private String substituteParams(String sql, Map<String, Object> params) {
        String finalSql = sql;
        for (Map.Entry<String, Object> map : params.entrySet()) {
            finalSql = finalSql.replace(":" + map.getKey(), "?");
        }

        String[] splittedSQL = finalSql.split("\\s+");
        String response = "You have to include these params in request: ";
        for (String s : splittedSQL) {
            if (s.contains(":") && !s.contains("'") && !s.contains("\"") && !s.contains("`")) {
                response += s + "   ";
                paramsError = true;
            }
        }

        if (paramsError)
            ErrorMessagesSingleton.getInstance().setMessage(response);

        return finalSql;
    }

    private Connection getConnection() {

        return this.getConnection("", true);

    }

    private Connection getConnection(String txid) {

        return this.getConnection(txid, false);

    }

    private Connection getConnection(String txid, Boolean noTransaction) {

        try {

            DataSource ds = (DataSource) applicationContext.getBean("dataSource");
            Connection connection;

            if (noTransaction) {
                connection = ds.getConnection();
                return connection;
            }

            if (connections.get(txid) == null || connections.get(txid).isClosed()) {

                connection = ds.getConnection();

                new Thread(() -> {
                    try {

                        Thread.sleep(CONNECTION_TIMEOUT);

                        if (!connection.isClosed()) {

                            connection.rollback();
                            connection.close();
                        }

                        connections.remove(txid);

                    } catch (SQLException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();

                connection.setAutoCommit(false);
                connections.put(txid, connection);

                return connection;
            }

        } catch (SQLException e) {

            e.printStackTrace();

        }

        if (connections.get(txid) == null) {

            try {

                throw new JSQLException("Connection with given id does not exist.");

            } catch (JSQLException e) {

                e.printStackTrace();

            }
        }

        return connections.get(txid);

    }

}
