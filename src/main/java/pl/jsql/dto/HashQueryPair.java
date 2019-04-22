package pl.jsql.dto;

public class HashQueryPair {

    public String token;
    public String query;

    public HashQueryPair(){
    }

    public HashQueryPair(String token, String query) {
        this.token = token;
        this.query = query;
    }

}
