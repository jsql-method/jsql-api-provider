package pl.jsql.enums;

public enum DatabaseDialectEnum {

    POSTGRES("org.postgresql.Driver"),
    MYSQL("com.mysql.jdbc.Driver");


    public String driverName;

    DatabaseDialectEnum(String driverName) {
        this.driverName = driverName;
    }

}
