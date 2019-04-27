package pl.jsql.service;

public class JSQLUtils {

    public static String toCamelCase(String str) {
        String[] parts = str.split("_");
        String camelCaseString = "";
        for (int i = 0; i < parts.length; i++) {
            if (i != 0) {
                parts[i] = parts[i].substring(0, 1).toUpperCase() + parts[i].substring(1).toLowerCase();
            }
            camelCaseString += parts[i];
        }
        return camelCaseString;
    }

    public static String buildReturningId(String sql) {
        if (!sql.toLowerCase().contains("returning")) {
            if (sql.endsWith(";")) {
                sql = sql.substring(0, sql.length() - 1);
            }
            sql += " RETURNING id";
        }
        return sql;
    }


    public static String getSQLExceptionCause(Exception e) {
        Throwable cause = e;

        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }

        return cause.getMessage() != null ? cause.getMessage().split("\n")[0] : (e.getMessage() != null ? e.getMessage() : "No message available");

    }

}
