package pl.jsql.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;
import pl.jsql.dto.TransactionThread;
import pl.jsql.enums.JSQLQueryTypeEnum;
import pl.jsql.exceptions.JSQLException;
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
    public List<Map<String, Object>> select(@RequestBody Map<String, Object> data, @RequestHeader(value = TRANSACTION_ID, required = false) String transactionId, HttpServletResponse response)  {

        TransactionThread transactionThread = new TransactionThread(data, transactionId);

        jsqlService.query(transactionThread, JSQLQueryTypeEnum.SELECT);

        if (transactionId != null) {
            response.addHeader(TRANSACTION_ID, transactionThread.transactionId);
        }

        return transactionThread.response;
    }

    @DeleteMapping("/delete")
    public List<Map<String, Object>> delete(@RequestBody Map<String, Object> data, @RequestHeader(value = TRANSACTION_ID, required = false) String transactionId, HttpServletResponse response)  {

        TransactionThread transactionThread = new TransactionThread(data, transactionId);

        jsqlService.query(transactionThread, JSQLQueryTypeEnum.DELETE);

        if (transactionId != null) {
            response.addHeader(TRANSACTION_ID, transactionThread.transactionId);
        }

        return transactionThread.response;
    }

    @PostMapping("/update")
    public List<Map<String, Object>> update(@RequestBody Map<String, Object> data, @RequestHeader(value = TRANSACTION_ID, required = false) String transactionId, HttpServletResponse response)  {

        TransactionThread transactionThread = new TransactionThread(data, transactionId);

        jsqlService.query(transactionThread, JSQLQueryTypeEnum.UPDATE);

        if (transactionId != null) {
            response.addHeader(TRANSACTION_ID, transactionThread.transactionId);
        }

        return transactionThread.response;
    }

    @PostMapping("/insert")
    public List<Map<String, Object>> insert(@RequestBody Map<String, Object> data, @RequestHeader(value = TRANSACTION_ID, required = false) String transactionId, HttpServletResponse response)  {

        TransactionThread transactionThread = new TransactionThread(data, transactionId);

        jsqlService.query(transactionThread, JSQLQueryTypeEnum.INSERT);

        if (transactionId != null) {
            response.addHeader(TRANSACTION_ID, transactionThread.transactionId);
        }

        return transactionThread.response;
    }

    @GetMapping("/commit")
    public void commit(@RequestHeader(TRANSACTION_ID) String transactionId) {

        TransactionThread transactionThread = new TransactionThread(transactionId);
        jsqlService.commitTransaction(transactionThread);
    }

}
