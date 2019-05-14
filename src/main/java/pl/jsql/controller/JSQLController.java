package pl.jsql.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.jsql.annotations.ProviderSecurity;
import pl.jsql.dto.TransactionThread;
import pl.jsql.enums.JSQLQueryTypeEnum;
import pl.jsql.exceptions.JSQLException;
import pl.jsql.service.JSQLService;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/jsql")
public class JSQLController extends ValidateController {

    private final String TRANSACTION_ID = "txid";

    @Autowired
    private JSQLService jsqlService;

    @ProviderSecurity
    @PostMapping("/select")
    public ResponseEntity select(@RequestBody Map<String, Object> data, @RequestHeader(value = TRANSACTION_ID, required = false) String transactionId, HttpServletResponse response) throws JSQLException {

        TransactionThread transactionThread = new TransactionThread(data, transactionId);

        jsqlService.query(transactionThread, JSQLQueryTypeEnum.SELECT);

        if (transactionId != null) {
            response.addHeader(TRANSACTION_ID, transactionThread.transactionId);
        }

        return new ResponseEntity<>(transactionThread.response, HttpStatus.OK);
    }

    @ProviderSecurity
    @PostMapping("/delete")
    public ResponseEntity delete(@RequestBody Map<String, Object> data, @RequestHeader(value = TRANSACTION_ID, required = false) String transactionId, HttpServletResponse response) throws JSQLException {

        TransactionThread transactionThread = new TransactionThread(data, transactionId);

        jsqlService.query(transactionThread, JSQLQueryTypeEnum.DELETE);

        if (transactionId != null) {
            response.addHeader(TRANSACTION_ID, transactionThread.transactionId);
        }

        return new ResponseEntity<>(transactionThread.response, HttpStatus.OK);
    }

    @ProviderSecurity
    @PostMapping("/update")
    public ResponseEntity update(@RequestBody Map<String, Object> data, @RequestHeader(value = TRANSACTION_ID, required = false) String transactionId, HttpServletResponse response) throws JSQLException {

        TransactionThread transactionThread = new TransactionThread(data, transactionId);

        jsqlService.query(transactionThread, JSQLQueryTypeEnum.UPDATE);

        if (transactionId != null) {
            response.addHeader(TRANSACTION_ID, transactionThread.transactionId);
        }

        return new ResponseEntity<>(transactionThread.response, HttpStatus.OK);
    }

    @ProviderSecurity
    @PostMapping("/insert")
    public ResponseEntity insert(@RequestBody Map<String, Object> data, @RequestHeader(value = TRANSACTION_ID, required = false) String transactionId, HttpServletResponse response) throws JSQLException {

        TransactionThread transactionThread = new TransactionThread(data, transactionId);

        jsqlService.query(transactionThread, JSQLQueryTypeEnum.INSERT);

        if (transactionId != null) {
            response.addHeader(TRANSACTION_ID, transactionThread.transactionId);
        }

        return new ResponseEntity<>(transactionThread.response, HttpStatus.OK);
    }

    @ProviderSecurity
    @PostMapping("/rollback")
    public ResponseEntity rollback(@RequestHeader(TRANSACTION_ID) String transactionId) throws JSQLException {

        TransactionThread transactionThread = new TransactionThread(transactionId);
        transactionThread.response = jsqlService.rollbackTransaction(transactionThread);

        return new ResponseEntity<>(transactionThread.response, HttpStatus.OK);

    }

    @ProviderSecurity
    @PostMapping("/commit")
    public ResponseEntity commit(@RequestHeader(TRANSACTION_ID) String transactionId) throws JSQLException {

        TransactionThread transactionThread = new TransactionThread(transactionId);
        transactionThread.response = jsqlService.commitTransaction(transactionThread);

        return new ResponseEntity<>(transactionThread.response, HttpStatus.OK);

    }

}
