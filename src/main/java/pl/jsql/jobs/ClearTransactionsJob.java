package pl.jsql.jobs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.jsql.database.JSQLConnectionProvider;
import pl.jsql.exceptions.JSQLException;

@Component
public class ClearTransactionsJob {

    private final static long DELAY = 10*1000L; //10 sec

    @Autowired
    private JSQLConnectionProvider jsqlConnectionProvider;

    @Scheduled(fixedDelay = DELAY)
    public void clearTransactions() throws JSQLException {
        jsqlConnectionProvider.clearUnusedConnections();
    }

}
