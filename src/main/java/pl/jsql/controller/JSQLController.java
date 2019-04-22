package pl.jsql.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;
import pl.jsql.dto.TransactionThread;
import pl.jsql.service.JSQLService;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;


@CrossOrigin
@RestController
public class JSQLController {

    private final String TRANSACTION_ID = "TXID";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private JSQLService jsqlService;

    @PostMapping("/select")
    public List<Map<String, Object>> select(@RequestBody Map<String, Object> data, @RequestHeader(value = TRANSACTION_ID, required = false) String transactionId, HttpServletResponse response) {

        TransactionThread transactionThread = new TransactionThread(data, transactionId);

        jsqlService.select(transactionThread);

        if (transactionId != null) {
            response.addHeader(TRANSACTION_ID, transactionThread.getTransactionId());
        }

        return transactionThread.getResponse();
    }

    @DeleteMapping("/delete")
    public List<Map<String, Object>> delete(@RequestBody Map<String, Object> data, @RequestHeader(value = TRANSACTION_ID, required = false) String transactionId, HttpServletResponse response) {

        TransactionThread transactionThread = new TransactionThread(data, transactionId);

        jsqlService.delete(transactionThread);

        if (transactionId != null) {
            response.addHeader(TRANSACTION_ID, transactionThread.getTransactionId());
        }

        return transactionThread.getResponse();
    }

    @PostMapping("/update")
    public List<Map<String, Object>> update(@RequestBody Map<String, Object> data, @RequestHeader(value = TRANSACTION_ID, required = false) String transactionId, HttpServletResponse response) {

        TransactionThread transactionThread = new TransactionThread(data, transactionId);

        jsqlService.update(transactionThread);

        if (transactionId != null) {
            response.addHeader(TRANSACTION_ID, transactionThread.getTransactionId());
        }

        return transactionThread.getResponse();
    }

    @PostMapping("/insert")
    public List<Map<String, Object>> insert(@RequestBody Map<String, Object> data, @RequestHeader(value = TRANSACTION_ID, required = false) String transactionId, HttpServletResponse response) {

        TransactionThread transactionThread = new TransactionThread(data, transactionId);

        jsqlService.insert(transactionThread);

        if (transactionId != null) {
            response.addHeader(TRANSACTION_ID, transactionThread.getTransactionId());
        }

        return transactionThread.getResponse();
    }

    @GetMapping("/commit")
    public Map<String, String> commit(@RequestHeader(TRANSACTION_ID) String transactionId) {
        return jsqlService.commitTransaction(transactionId);
    }

}
