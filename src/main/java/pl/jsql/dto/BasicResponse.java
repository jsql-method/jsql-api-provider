package pl.jsql.dto;

public class BasicResponse<T> {

    public Integer status;
    public T data;

    public BasicResponse(Integer status) {
        this.status = status;
        this.data = null;
    }

    public BasicResponse(Integer status, T data) {
        this.status = status;
        this.data = data;
    }
}