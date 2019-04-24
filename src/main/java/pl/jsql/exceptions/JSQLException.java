package pl.jsql.exceptions;

public class JSQLException extends RuntimeException {

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

}
