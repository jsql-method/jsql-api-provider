package pl.jsql.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.jsql.enums.CacheType;

import java.util.HashMap;

@Service
public class CacheService {

    @Autowired
    private SecurityService securityService;

    private HashMap<String, Object> cache = new HashMap<>();

    public void cache(CacheType cacheType, Object object, String apiKey){

        if(object != null){
            this.cache.put(apiKey+cacheType.toString(), object);
        }

    }

    public Object get(CacheType cacheType, String apiKey){
        return this.cache.get(apiKey+cacheType.toString());
    }

    public boolean exists(CacheType cacheType, String apiKey){
        return this.cache.containsKey(apiKey+cacheType.toString());
    }

    public void cleanAll(){
        this.cache = new HashMap<>();
    }

    public HashMap<String, Object> getCache() {
        return cache;
    }


    public Object getByKey(String cacheKey) {
        return this.cache.get(cacheKey);
    }
}
