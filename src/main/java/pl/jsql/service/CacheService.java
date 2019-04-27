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

    public void cache(CacheType cacheType, Object object){

        if(object != null){
            this.cache.put(securityService.getApiKey()+cacheType.toString(), object);
        }

    }

    public Object get(CacheType cacheType){
        return this.cache.get(securityService.getApiKey()+cacheType.toString());
    }

    public boolean exists(CacheType cacheType){
        return this.cache.containsKey(securityService.getApiKey()+cacheType.toString());
    }

    public void cleanAll(){
        this.cache = new HashMap<>();
    }


}
