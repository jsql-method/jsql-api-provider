package pl.jsql.service;

import it.jsql.connector.dto.HashQueryPair;
import it.jsql.connector.dto.TransactionThread;
import it.jsql.connector.exceptions.ErrorMessagesSingleton;
import it.jsql.connector.hibernate.IJSQLHibernateExecutor;
import it.jsql.connector.hibernate.JSQLHibernateExecutor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import pl.jsql.dto.TransactionThread;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Dawid on 2016-09-13.
 * Modified by Michael on 2018-09-10.
 */
@Service
public class JSQLService implements IJSQLService {

    String LAST_ID_NAME = "lastId";
    String PARAMS_NAME = "params";
    String HASH_NAME = "token";

    public static String DATABASE_DIALECT;
    private JSQLConnector jsqlConnector = null;
    private IJSQLHibernateExecutor hibernateExecutor = null;

    static String _apiKeyTemp = null;
    static String _memberKeyTemp = null;


    public JSQLService() {
        assignDatabaseDialect();
    }

    public JSQLService(IJSQLHibernateExecutor hibernateExecutor) {

        this.setHibernateExecutor(hibernateExecutor);
        assignDatabaseDialect();
    }

    public JSQLService(ApplicationContext applicationContext, String apiKey, String memberKey) {

        this.setHibernateExecutor(JSQLHibernateExecutor.getInstance(applicationContext));
        this.setJsqlConnector(new JSQLConnector(apiKey, memberKey));
        assignDatabaseDialect();
    }

    public void construct() {
        if (JSQLService._apiKeyTemp != null && JSQLService._memberKeyTemp != null) {
            this.setJsqlConnector(new JSQLConnector(JSQLService._apiKeyTemp, JSQLService._memberKeyTemp));
        }
    }

    public void setApiKey(String apiKey) {
        JSQLService._apiKeyTemp = apiKey;
        this.construct();
    }

    public void setMemberKey(String memberKey) {
        JSQLService._memberKeyTemp = memberKey;
        this.construct();
    }


    @Override
    public void select(TransactionThread transactionThread) {

        Map<String, Object> request = transactionThread.getRequest();
        String sql;
        transactionThread.setParamsAsArray(request.get(PARAMS_NAME) instanceof List);
        try {

            sql = this.getSQLQuery(request);

        } catch (Exception e) {
            e.printStackTrace();
            transactionThread.setResponse(getErrorResponse());

            return;
        }

        if (sql != null && !sql.trim().toLowerCase().startsWith("select")) {

            ErrorMessagesSingleton.getInstance().setMessage("Only `SELECT` queries allowed in this method!");
            transactionThread.setResponse(getErrorResponse());

            return;
        }

        Map<String, Object> paramsMap = this.getParamsMap(request);

        paramsMap = paramsMap == null ? new HashMap<>() : paramsMap;

        this.getHibernateExecutor().executeSelect(sql, paramsMap, transactionThread);
    }

    @Override
    public void delete(TransactionThread transactionThread) {

        Map<String, Object> request = transactionThread.getRequest();
        String sql;
        transactionThread.setParamsAsArray(request.get(PARAMS_NAME) instanceof List);
        try {

            sql = this.getSQLQuery(request);

        } catch (Exception e) {
            e.printStackTrace();
            transactionThread.setResponse(getErrorResponse());

            return;
        }

        if (sql != null && !sql.trim().toLowerCase().startsWith("delete")) {

            ErrorMessagesSingleton.getInstance().setMessage("Only `DELETE` queries allowed in this method!");
            transactionThread.setResponse(getErrorResponse());

            return;
        }

        Map<String, Object> paramsMap = this.getParamsMap(request);

        paramsMap = paramsMap == null ? new HashMap<>() : paramsMap;

        this.getHibernateExecutor().executeDelete(sql, paramsMap, transactionThread);
    }

    @Override
    public void update(TransactionThread transactionThread) {

        Map<String, Object> request = transactionThread.getRequest();
        String sql;
        transactionThread.setParamsAsArray(request.get(PARAMS_NAME) instanceof List);

        try {

            sql = this.getSQLQuery(request);

        } catch (Exception e) {
            e.printStackTrace();
            transactionThread.setResponse(getErrorResponse());

            return;
        }

        if (sql != null && !sql.trim().toLowerCase().startsWith("update")) {

            ErrorMessagesSingleton.getInstance().setMessage("Only `UPDATE` queries allowed in this method!");
            transactionThread.setResponse(getErrorResponse());

            return;
        }

        Map<String, Object> paramsMap = this.getParamsMap(request);

        paramsMap = paramsMap == null ? new HashMap<>() : paramsMap;

        this.getHibernateExecutor().executeUpdate(sql, paramsMap, transactionThread);
    }

    @Override
    public void insert(TransactionThread transactionThread) {

        Map<String, Object> request = transactionThread.getRequest();
        String sql;
        transactionThread.setParamsAsArray(request.get(PARAMS_NAME) instanceof List);
        try {

            sql = this.getSQLQuery(request);

        } catch (Exception e) {
            e.printStackTrace();
            transactionThread.setResponse(getErrorResponse());

            return;
        }

        if (sql != null && !sql.trim().toLowerCase().startsWith("insert")) {

            ErrorMessagesSingleton.getInstance().setMessage("Only `INSERT` queries allowed in this method!");
            transactionThread.setResponse(getErrorResponse());

            return;
        }


        Map<String, Object> paramsMap = this.getParamsMap(request);

        paramsMap = paramsMap == null ? new HashMap<>() : paramsMap;

        this.getHibernateExecutor().executeInsert(sql, paramsMap, transactionThread);

    }

    @Override
    public Map<String, String> commitTransaction(String txid) {
        return this.getHibernateExecutor().commit(txid);
    }

    @Override
    public List<String> getHash(Map<String, Object> data) {

        if (data.get(HASH_NAME) instanceof String) {

            List<String> list = new ArrayList<>();
            list.add((String) data.get(HASH_NAME));

            return list;
        }

        return (List<String>) data.get(HASH_NAME);
    }

    @Override
    public Map<String, Object> getParamsMap(Map<String, Object> data) {

        Object params = data.get(PARAMS_NAME);

        if (params instanceof Map) {

            return (Map<String, Object>) params;

        } else if (params instanceof List) {
            Map<String, Object> map = new HashMap<>();
            int i = 1;
            for (Object o : (List) params) {
                map.put(String.valueOf(i), o);
                i++;
            }
            return map;

        }

        return null;
    }

    @Override
    public String getSQLQuery(Map<String, Object> data) {

        List<HashQueryPair> queries = null;

        try {

            queries = this.getJsqlConnector().requestQueries(this.getHash(data));

            return queries.get(0).getQuery();

        } catch (Exception e) {

            e.printStackTrace();

        }

        return null;
    }

    public IJSQLHibernateExecutor getHibernateExecutor() {
        return hibernateExecutor;
    }

    public void setHibernateExecutor(IJSQLHibernateExecutor hibernateExecutor) {
        this.hibernateExecutor = hibernateExecutor;
    }

    protected JSQLConnector getJsqlConnector() {
        return jsqlConnector;
    }

    protected void setJsqlConnector(JSQLConnector jsqlConnector) {
        this.jsqlConnector = jsqlConnector;
    }

    private List<Map<String, Object>> getErrorResponse() {

        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> response = new HashMap<>();

        response.put("code", 400);
        response.put("description", ErrorMessagesSingleton.getInstance().getMessage());

        list.add(response);

        return list;
    }

    private void assignDatabaseDialect() {

        if (DATABASE_DIALECT == null) {

            try {

                this.getDatabaseDialect();

            } catch (Exception e) {

                e.printStackTrace();

            }
        }
    }

    private void getDatabaseDialect() throws Exception {

        StringBuilder result = new StringBuilder();
        URL url = new URL("http://softwarecartoon.com:9391/api/request/options/all");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("ApiKey", this.getJsqlConnector().getApiKey());
        conn.setRequestProperty("MemberKey", this.getJsqlConnector().getMemberKey());

        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;

        while ((line = rd.readLine()) != null) {

            result.append(line);

        }

        rd.close();

        String[] words = result.toString().split(",");

        for (int i = 0; i < words.length; i++) {

            if (words[i].contains("databaseDialect")) {

                String[] dialect = words[i].split(":");

                DATABASE_DIALECT = dialect[1].substring(1, dialect[1].length() - 1);

            }
        }
    }

}
