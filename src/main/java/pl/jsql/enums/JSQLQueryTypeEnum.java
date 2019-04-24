package pl.jsql.enums;

public enum JSQLQueryTypeEnum {

    ANY,
    SELECT,
    INSERT,
    DELETE,
    UPDATE,
    UPDATE_AND_DELETE;

    public String toLower(){
        return this.toString().toLowerCase();
    }
}
