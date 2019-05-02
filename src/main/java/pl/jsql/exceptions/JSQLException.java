package pl.jsql.exceptions;

public class JSQLException extends Exception {

    public JSQLException(){
        super();
    }

    public JSQLException(String message){
        super(message);
    }

    private String description;

    public JSQLException(String message, String description){
        this.description = description;
    }

    public String getDescription(){
        return this.description;
    }

}
