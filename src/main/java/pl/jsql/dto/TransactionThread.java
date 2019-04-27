package pl.jsql.dto;

import pl.jsql.service.JSQLService;

import java.util.List;
import java.util.Map;

public class TransactionThread {

    public Map<String, Object> request;
    public List<Map<String, Object>> response;
    public String transactionId;
    public Boolean isTransactional;
    public Boolean paramsAsArray;

    public TransactionThread() {
    }

    public TransactionThread(Map<String, Object> request, String transactionId) {
        this.request = request;
        this.isTransactional = transactionId != null;
        this.transactionId = transactionId;
        this.paramsAsArray = request.get(JSQLService.PARAMS_NAME) instanceof List;
    }


    public TransactionThread(String transactionId) {
        this.request = null;
        this.isTransactional = transactionId != null;
        this.transactionId = transactionId;
        this.paramsAsArray = false;
    }

}
