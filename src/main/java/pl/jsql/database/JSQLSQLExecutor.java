package pl.jsql.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import pl.jsql.database.executors.JSQLDeleteExecutor;
import pl.jsql.database.executors.JSQLInsertExecutor;
import pl.jsql.database.executors.JSQLSelectExecutor;
import pl.jsql.database.executors.JSQLUpdateExecutor;
import pl.jsql.dto.TransactionThread;
import pl.jsql.enums.JSQLQueryTypeEnum;
import pl.jsql.exceptions.JSQLException;
import pl.jsql.service.JSQLUtils;
import pl.jsql.service.SecurityService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

@Repository
public class JSQLSQLExecutor {

    @Autowired
    private JSQLConnectionProvider jsqlConnectionProvider;

    @Autowired
    private JSQLSelectExecutor selectExecutor;

    @Autowired
    private JSQLInsertExecutor insertExecutor;

    @Autowired
    private JSQLUpdateExecutor updateExecutor;

    @Autowired
    private JSQLDeleteExecutor deleteExecutor;

    @Autowired
    private SecurityService securityService;

    public void executeQuery(JSQLQueryTypeEnum queryTypeEnum, String sql, Map<String, Object> params, TransactionThread transactionThread) throws JSQLException {

        System.out.println("transactionThread.transactionId : "+transactionThread.transactionId);
        Connection connection = jsqlConnectionProvider.resolveConnection(transactionThread.transactionId, transactionThread.isTransactional);

        String finalSql = sql;
        Map<Integer, Object> psParams = new HashMap<>();

        if (!transactionThread.paramsAsArray) {
            finalSql = substituteParams(sql, params);
            psParams = getParamsMap(sql, params);
        } else {

            for (Map.Entry<String, Object> entry : params.entrySet()) {
                psParams.put(Integer.valueOf(entry.getKey()), entry.getValue());
            }

        }

        PreparedStatement ps;

        try {
            ps = connection.prepareStatement(finalSql);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new JSQLException("JSQL JSQLSQLExecutor.executeQuery: " + e.getMessage() + " " + e.getSQLState(), JSQLUtils.getSQLExceptionCause(e));
        }

        for (Map.Entry<Integer, Object> entry : psParams.entrySet()) {

            try {
                ps.setObject(entry.getKey(), entry.getValue());
            } catch (SQLException e) {
                e.printStackTrace();
                throw new JSQLException("JSQL JSQLSQLExecutor.executeQuery: " + e.getMessage() + " " + e.getSQLState(), JSQLUtils.getSQLExceptionCause(e));
            }

        }

        switch (queryTypeEnum) {
            case SELECT:
                transactionThread.response = selectExecutor.execute(ps);
                break;
            case INSERT:
                transactionThread.response = insertExecutor.execute(ps, finalSql, connection, psParams);
                break;
            case UPDATE:
                transactionThread.response = updateExecutor.execute(ps);
                break;
            case DELETE:
                transactionThread.response = deleteExecutor.execute(ps);
                break;
        }

        try {

            if (!ps.isClosed()) {
                ps.close();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new JSQLException("JSQL JSQLSQLExecutor.executeQuery: " + e.getMessage() + " " + e.getSQLState());
        }


        if (!transactionThread.isTransactional) {
            jsqlConnectionProvider.removeConnection(connection);
        }

    }

    public List<Map<String, Object>> commit(TransactionThread transactionThread) throws JSQLException {

        List<Map<String, Object>> response = new ArrayList<>();
        Map<String, Object> responseObject = new HashMap<>();

        Connection connection = jsqlConnectionProvider.resolveConnection(transactionThread.transactionId, transactionThread.isTransactional);

        try {
            connection.commit();
        } catch (SQLException e) {

            jsqlConnectionProvider.removeConnection(connection, transactionThread.transactionId);
            responseObject.put("status", "Error during commit transaction " + transactionThread.transactionId);
            return response;

        }

        jsqlConnectionProvider.removeConnection(connection, transactionThread.transactionId);


        responseObject.put("status", "OK");
        return response;

    }

    public List<Map<String, Object>> rollback(TransactionThread transactionThread) throws JSQLException {

        List<Map<String, Object>> response = new ArrayList<>();
        Map<String, Object> responseObject = new HashMap<>();

        Connection connection = jsqlConnectionProvider.resolveConnection(transactionThread.transactionId, transactionThread.isTransactional);

        System.out.println("Rollback transactionId: "+transactionThread.transactionId);

        try {
            connection.rollback();
            System.out.println("Rollback done transactionId: "+transactionThread.transactionId);
        } catch (SQLException e) {

            jsqlConnectionProvider.removeConnection(connection, transactionThread.transactionId);
            responseObject.put("status", "Error during rollback transaction " + transactionThread.transactionId);
            return response;

        }

        jsqlConnectionProvider.removeConnection(connection, transactionThread.transactionId);

        responseObject.put("status", "OK");
        return response;

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

    private String substituteParams(String sql, Map<String, Object> params) throws JSQLException {
        String finalSql = sql;
        for (Map.Entry<String, Object> map : params.entrySet()) {
            finalSql = finalSql.replace(":" + map.getKey(), "?");
        }

        String[] splittedSQL = finalSql.split("\\s+");

        Boolean error = false;
        StringBuilder errorMessage = new StringBuilder("You have to include these params in request: ");
        for (String s : splittedSQL) {
            if (s.contains(":") && !s.contains("'") && !s.contains("\"") && !s.contains("`")) {
                errorMessage.append(s).append("   ");
                error = true;

            }
        }

        if (error) {
            throw new JSQLException("JSQL JSQLSQLExecutor.substituteParams: " + errorMessage);
        }

        return finalSql;
    }

}
