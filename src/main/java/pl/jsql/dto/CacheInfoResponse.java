package pl.jsql.dto;

public class CacheInfoResponse {

    public String apiKey;
    public Boolean prodCache;

    public CacheInfoResponse(String apiKey, Boolean prodCache) {
        this.apiKey = apiKey;
        this.prodCache = prodCache;
    }

}
