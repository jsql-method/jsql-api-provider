package pl.jsql.database.executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import pl.jsql.dto.OptionsResponse;
import pl.jsql.exceptions.JSQLException;
import pl.jsql.service.JSQLConnector;
import pl.jsql.service.JSQLUtils;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class JSQLInsertExecutor {

    @Autowired
    private JSQLConnector jsqlConnector;

    public List<Map<String, Object>> execute(PreparedStatement ps, String finalSql, Connection connection, Map<Integer, Object> psParams) throws JSQLException {

        OptionsResponse optionsResponse = jsqlConnector.requestOptions();

        List<Map<String, Object>> response = new ArrayList<>();

        Long lastId = -1L;

        try {

            ResultSet rs;

            switch (optionsResponse.databaseDialect) {
                case POSTGRES:

                    finalSql = JSQLUtils.buildReturningId(finalSql);
                    ps = connection.prepareStatement(finalSql);

                    for (Map.Entry<Integer, Object> entry : psParams.entrySet()) {
                        ps.setObject(entry.getKey(), entry.getValue());
                    }

                    rs = ps.executeQuery();

                    while (rs.next()) {
                        lastId = rs.getLong(1);
                    }

                    break;

                default:

                    ps.executeUpdate();
                    ps = connection.prepareStatement("SELECT LAST_INSERT_ID()");
                    rs = ps.executeQuery();

                    while (rs.next()) {

                        lastId = rs.getLong(1);

                    }

                    break;

            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new JSQLException("JSQL JSQLInsertExecutor.execute: " + e.getMessage() + " " + e.getSQLState(), JSQLUtils.getSQLExceptionCause(e));
        }


        Map<String, Object> responseObject = new HashMap<>();
        responseObject.put("lastId", BigInteger.valueOf(lastId));

        response.add(responseObject);

        return response;

    }

}
