package pl.jsql.exceptions;

public class ErrorMessagesSingleton {

    private volatile static ErrorMessagesSingleton instance;
    private String message;
    private ErrorMessagesSingleton() {
    }

    public static ErrorMessagesSingleton getInstance() {
        if (instance == null) {
            synchronized (ErrorMessagesSingleton.class) {
                if (instance == null) {
                    instance = new ErrorMessagesSingleton();
                }
            }
        }

        return instance;
    }
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
