package pl.jsql.database.executors;

import org.springframework.stereotype.Repository;
import pl.jsql.exceptions.JSQLException;
import pl.jsql.service.JSQLUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class JSQLSelectExecutor {

    public List<Map<String, Object>> execute(PreparedStatement ps) throws JSQLException {

        List<Map<String, Object>> response = new ArrayList<>();

        ResultSet rs;

        try {

            rs = ps.executeQuery();
            ResultSetMetaData resultSetMetaData = rs.getMetaData();

            while (rs.next()) {

                Map<String, Object> map = new HashMap<>();

                for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                    map.put(JSQLUtils.toCamelCase(resultSetMetaData.getColumnName(i)), rs.getObject(i));
                }

                response.add(map);

            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new JSQLException("JSQL JSQLSelectExecutor.execute: " + e.getMessage() + " " + e.getSQLState(), JSQLUtils.getSQLExceptionCause(e));
        }

        return response;

    }

}
