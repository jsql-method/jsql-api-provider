package pl.jsql.database.executors;

import org.springframework.stereotype.Repository;
import pl.jsql.exceptions.JSQLException;
import pl.jsql.service.JSQLUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class JSQLUpdateExecutor {

    public List<Map<String, Object>> execute(PreparedStatement ps) throws JSQLException {

        List<Map<String, Object>> response = new ArrayList<>();

        try {
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new JSQLException("JSQL JSQLUpdateExecutor.execute: " + e.getMessage() + " " + e.getSQLState(), JSQLUtils.getSQLExceptionCause(e));
        }

        Map<String, Object> responseObject = new HashMap<>();
        responseObject.put("status", "OK");

        response.add(responseObject);

        return response;


    }

}
