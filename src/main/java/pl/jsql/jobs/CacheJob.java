package pl.jsql.jobs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.jsql.service.CacheService;

import java.util.HashMap;
import java.util.Set;

@Component
public class CacheJob {

    @Autowired
    private CacheService cacheService;

    private final static long DELAY = 60 * 1000L; //1 minute
    private final static int MINUTES = 30;
    private static HashMap<String, Integer> counters = new HashMap<>();

    @Scheduled(fixedDelay = DELAY)
    public void clearCache() {

        Set<String> keys = cacheService.getCache().keySet();

        for (String cacheKey : keys) {

            if (cacheKey.contains("OPTIONS_DEV")) {

                counters.putIfAbsent(cacheKey, 0);

                //dla developmentu - 1 minuta
                if (counters.get(cacheKey) >= 1) {
                    cacheService.cleanForKey(cacheKey);
                    counters.put(cacheKey, 0);
                }

                counters.put(cacheKey, counters.get(cacheKey) + 1);

            }

        }

    }

}
