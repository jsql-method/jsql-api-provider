package pl.jsql.exceptions;

public class JSQLException extends Exception {

    public JSQLException(){
        super();
    }

    public JSQLException(String message){
        super("jSQL: "+message);
    }

}
