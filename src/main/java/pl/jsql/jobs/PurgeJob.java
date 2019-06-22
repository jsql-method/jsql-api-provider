package pl.jsql.jobs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import pl.jsql.dto.PurgeResponse;
import pl.jsql.exceptions.JSQLException;
import pl.jsql.service.CacheService;
import pl.jsql.service.JSQLConnector;

import java.util.Set;

@Component
public class PurgeJob {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private JSQLConnector jsqlConnector;

    private final static long DELAY = 10 * 1000L; //10 sec

    @Scheduled(fixedDelay = DELAY)
    public void clearCache() throws JSQLException {

        PurgeResponse purgeResponse = jsqlConnector.requestPurge();

        if (purgeResponse == null) {
            return;
        }

        Set<String> keys = cacheService.getCache().keySet();

        for (String cacheKey : keys) {

            if (cacheKey.contains("OPTIONS_PROD") || cacheKey.contains("OPTIONS_DEV")) {

                for (String apiKey : purgeResponse.options) {

                    String optionApiKey = cacheKey.substring(0, cacheKey.indexOf("-"));
                    if (DigestUtils.md5DigestAsHex(optionApiKey.getBytes()).equals(apiKey)) {
                        cacheService.cleanForKey(cacheKey);
                    }

                }

            }
//            else if (cacheKey.contains("QUERIES_PROD")) {
//
//                for (String apiKey : purgeResponse.queries) {
//
//                    String optionApiKey = cacheKey.substring(0, cacheKey.indexOf("-"));
//
//                    if (DigestUtils.md5DigestAsHex(optionApiKey.getBytes()).equals(apiKey)) {
//                        cacheService.cleanForKey(cacheKey);
//                    }
//
//                }
//
//            }

        }

//        List<CacheInfoResponse> cacheInfoResponseList = jsqlConnector.requestCacheInfo();
//
//        for(CacheInfoResponse cacheInfoResponse : cacheInfoResponseList){
//            cacheService.setIsProdCache(cacheInfoResponse.apiKey, cacheInfoResponse.prodCache);
//        }


    }

}
