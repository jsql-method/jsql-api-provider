package pl.jsql.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.jsql.database.JSQLSQLExecutor;
import pl.jsql.dto.HashQueryPair;
import pl.jsql.dto.TransactionThread;
import pl.jsql.enums.JSQLQueryTypeEnum;
import pl.jsql.exceptions.JSQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JSQLService {

    public static final String PARAMS_NAME = "params";
    private static final String HASH_NAME = "token";

    @Autowired
    private JSQLConnector jsqlConnector;

    @Autowired
    private JSQLSQLExecutor hibernateExecutor;

    /**
     * Wykonuje zapytanie o podanym typie na podstawie podanych danych wejściowych
     * Zwraca wynik pod postacią TransactionThread
     *
     * @param transactionThread
     * @param queryTypeEnum
     * @
     */
    public void query(TransactionThread transactionThread, JSQLQueryTypeEnum queryTypeEnum) throws JSQLException {

        String sql = this.getSQLQuery(transactionThread.request);

        String queryType = queryTypeEnum.toLower();
        if (sql != null && !sql.trim().toLowerCase().startsWith(queryType)) {
            throw new JSQLException("JSQL JSQLService." + queryType + ": This method accepts only " + queryType + " statements");
        }

        Map<String, Object> params = this.getParamsMap(transactionThread.request);

        hibernateExecutor.executeQuery(queryTypeEnum, sql, params, transactionThread);

    }

    public List<Map<String, Object>> commitTransaction(TransactionThread transactionThread) throws JSQLException {
        return hibernateExecutor.commit(transactionThread);
    }


    public List<Map<String, Object>> rollbackTransaction(TransactionThread transactionThread) throws JSQLException {
        return hibernateExecutor.rollback(transactionThread);
    }

    /**
     * Pobiera hash dla którego będzie wyszukiwane query SQL
     * Hash może być listą lub stringiem
     *
     * @param data
     * @return
     */
    private List<String> getHash(Map<String, Object> data) {

        if (data.get(HASH_NAME) instanceof String) {

            List<String> list = new ArrayList<>();
            list.add((String) data.get(HASH_NAME));

            return list;
        }

        return (List<String>) data.get(HASH_NAME);
    }

    /**
     * Pobiera mapę parametrów do zapytania SQL
     * Parametry mogą być listą lub mapą, stąd potrzeba zmapowania listy do mapy
     * do późniejszego bindowania
     *
     * @param data
     * @return
     */
    private Map<String, Object> getParamsMap(Map<String, Object> data) {

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

        return new HashMap<>();
    }

    /**
     * Pobiera zapytanie SQL na podstawie hasha zapytania
     * Strzela do JSQL API po query SQL
     *
     * @param data
     * @return
     * @
     */
    private String getSQLQuery(Map<String, Object> data) throws JSQLException {

        List<HashQueryPair> queries;

        try {

            queries = jsqlConnector.requestQueries(this.getHash(data));
            return queries.get(0).query;

        } catch (Exception e) {
            e.printStackTrace();
            throw new JSQLException("JSQL JSQLService.getSQLQuery " + e.getMessage());
        }

    }

}
