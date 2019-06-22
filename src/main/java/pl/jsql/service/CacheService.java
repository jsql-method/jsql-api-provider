package pl.jsql.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import pl.jsql.enums.CacheType;

import java.util.HashMap;

@Service
public class CacheService {

    @Autowired
    private SecurityService securityService;

    private HashMap<String, Object> cache = new HashMap<>();
   // private HashMap<String, Boolean> isProdCache = new HashMap<>();

//    public boolean isProdCache(String apiKey){
//
//        apiKey = DigestUtils.md5DigestAsHex(apiKey.getBytes());
//
//        if(!this.isProdCache.containsKey(apiKey)){
//            return false;
//        }
//
//        return this.isProdCache.get(apiKey);
//    }
//    public void setIsProdCache(String apiKeyMd5, Boolean isProdCache){
//        this.isProdCache.put(apiKeyMd5, isProdCache);
//    }

    public void cache(CacheType cacheType, Object object, String apiKey, String devKey){

        if(object != null){
            this.cache.put(apiKey+"-"+devKey+"-"+cacheType.toString(), object);
        }

    }

    public Object get(CacheType cacheType, String apiKey, String devKey){
        return this.cache.get(apiKey+"-"+devKey+"-"+cacheType.toString());
    }

    public boolean exists(CacheType cacheType, String apiKey, String devKey){
        return this.cache.containsKey(apiKey+"-"+devKey+"-"+cacheType.toString());
    }

    public void cleanForKey(String key){
        this.cache.remove(key);
    }

    public void cleanAll(){
        this.cache = new HashMap<>();
    }

    public HashMap<String, Object> getCache() {
        return cache;
    }


}
