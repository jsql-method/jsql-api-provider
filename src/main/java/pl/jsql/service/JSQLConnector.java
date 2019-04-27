package pl.jsql.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.jsql.dto.HashQueryPair;
import pl.jsql.dto.OptionsResponse;
import pl.jsql.enums.CacheType;
import pl.jsql.exceptions.JSQLException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class JSQLConnector {

    @Value("${jsql.origin}")
    private String origin;

    @Value("${jsql.api.path.request.options-all}")
    private String requestOptions;

    @Value("${jsql.api.path.request.queries}")
    private String requestQueries;

    @Value("${jsql.api.path.request.queries-grouped}")
    private String requestQueriesGrouped;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private CacheService cacheService;

    public List<HashQueryPair> requestQueries(List<String> hashesList)  {

        Boolean isGrouped = hashesList.size() > 1;
        String fullUrl = origin + requestQueries;

        if (isGrouped) {
            fullUrl = origin + requestQueriesGrouped;
        }


        String responseJSON = this.call(fullUrl, hashesList, "POST");

        if (!responseJSON.isEmpty()) {
            try {
                return Arrays.asList(new ObjectMapper().readValue(responseJSON, HashQueryPair[].class));
            } catch (IOException e) {
                e.printStackTrace();
                throw new JSQLException("JSQL JSQLConnector.requestQueries: "+e.getMessage());
            }
        }

        return new ArrayList<>();

    }

    public OptionsResponse requestOptions()  {

        if(cacheService.exists(CacheType.OPTIONS)){
            return (OptionsResponse) cacheService.get(CacheType.OPTIONS);
        }

        OptionsResponse optionsResponse = null;
        String responseJSON = this.call(origin + requestOptions, "GET");

        if (!responseJSON.isEmpty()) {
            try {
                optionsResponse = new ObjectMapper().readValue(responseJSON, OptionsResponse.class);
            } catch (IOException e) {
                e.printStackTrace();
                throw new JSQLException("JSQL JSQLConnector.requestOptions: "+e.getMessage());
            }
        }

        cacheService.cache(CacheType.OPTIONS, optionsResponse);

        return optionsResponse;

    }

    public String call(String fullUrl, String method)  {
        return this.call(fullUrl, null, method);
    }

    public String call(String fullUrl, Object request, String method)  {

        try {

            URL url = new URL(fullUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("ApiKey", securityService.getApiKey());
            conn.setRequestProperty("DevKey", securityService.getDevKey());

            OutputStream os = conn.getOutputStream();

            if (method.equals("POST")) {
                os.write(new Gson().toJson(request).getBytes());
            }

            os.flush();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader((conn.getErrorStream())));
                StringBuilder builder = new StringBuilder();
                while (br.ready()) {
                    builder.append(br.readLine());
                }
                conn.disconnect();

                String response = builder.toString();
                response = response.substring(response.lastIndexOf("</div><div>") + 11, response.lastIndexOf("</div></body></html>"));
                throw new JSQLException("HTTP error code : " + conn.getResponseCode() + "\nHTTP error message : " + response);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            StringBuilder builder = new StringBuilder();

            while (br.ready()) {
                builder.append(br.readLine());
            }

            conn.disconnect();

            return builder.toString();


        } catch (IOException e) {
            e.printStackTrace();
            throw new JSQLException("IOException JSQLConnector.call: " + e.getMessage());
        }

    }

}
