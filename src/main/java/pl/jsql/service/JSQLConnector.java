package pl.jsql.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.jsql.dto.BasicResponseWithHashQueryPair;
import pl.jsql.dto.BasicResponseWithOptionsResponse;
import pl.jsql.dto.HashQueryPair;
import pl.jsql.dto.OptionsResponse;
import pl.jsql.enums.CacheType;
import pl.jsql.exceptions.JSQLException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
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

    public List<HashQueryPair> requestQueries(List<String> hashesList) throws JSQLException {

        Boolean isGrouped = hashesList.size() > 1;
        String fullUrl = origin + requestQueries;

        if (isGrouped) {
            fullUrl = origin + requestQueriesGrouped;
        }


        String responseJSON = this.call(fullUrl, hashesList, "POST");

        if (!responseJSON.isEmpty()) {
            try {

                BasicResponseWithHashQueryPair basicResponse = new ObjectMapper().readValue(responseJSON, BasicResponseWithHashQueryPair.class);
                return basicResponse.data;

            } catch (IOException e) {
                e.printStackTrace();
                throw new JSQLException("JSQL JSQLConnector.requestQueries: " + e.getMessage());
            }
        }

        return new ArrayList<>();

    }

    public OptionsResponse requestOptions(String apiKey) throws JSQLException {

        if (cacheService.exists(CacheType.OPTIONS, apiKey)) {
            return (OptionsResponse) cacheService.get(CacheType.OPTIONS, apiKey);
        }

        OptionsResponse optionsResponse = null;
        String responseJSON = this.call(origin + requestOptions, "GET");

        if (!responseJSON.isEmpty()) {
            try {
                optionsResponse = new ObjectMapper().readValue(responseJSON, BasicResponseWithOptionsResponse.class).data;
            } catch (IOException e) {
                e.printStackTrace();
                throw new JSQLException("JSQL JSQLConnector.requestOptions: " + e.getMessage());
            }
        }

        cacheService.cache(CacheType.OPTIONS, optionsResponse, apiKey);

        return optionsResponse;

    }

    public OptionsResponse requestOptions() throws JSQLException {
        return this.requestOptions(securityService.getApiKey());
    }

    public String call(String fullUrl, String method) throws JSQLException {
        return this.call(fullUrl, null, method);
    }

    public String call(String fullUrl, Object request, String method) throws JSQLException {

        HttpURLConnection conn = null;

        try {

            URL url = new URL(fullUrl);
            conn = (HttpURLConnection) url.openConnection();

            conn.setDoOutput(true);
            conn.setDoInput(true);

            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Api-Key", securityService.getApiKey());
            conn.setRequestProperty("Dev-Key", securityService.getDevKey());
            conn.setUseCaches(false);

            if (method.equals("POST")) {

                OutputStream os = conn.getOutputStream();

                os.write(new Gson().toJson(request).getBytes());
                os.flush();

            }

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {

                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {

                    String response = readInputStreamToString(conn, true);
                    conn.disconnect();

                    if (response.length() > 0 && response.contains("<div>")) {
                        response = response.substring(response.lastIndexOf("</div><div>") + 11, response.lastIndexOf("</div></body></html>"));
                    }

                    throw new JSQLException("HTTP error code : " + conn.getResponseCode() + "\nHTTP error message : " + response);
                }

            }

            String response = readInputStreamToString(conn, false);
            conn.disconnect();
            return response;


        } catch (Exception e) {
            e.printStackTrace();
            throw new JSQLException("IOException JSQLConnector.call: " + e.getMessage());
        } finally {

            if (conn != null) {
                conn.disconnect();
            }

        }

    }

    private static String readInputStreamToString(HttpURLConnection connection, boolean error) {
        String result = null;
        StringBuffer sb = new StringBuffer();
        InputStream is = null;

        try {
            is = new BufferedInputStream(error ? connection.getErrorStream() : connection.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String inputLine = "";
            while ((inputLine = br.readLine()) != null) {
                sb.append(inputLine);
            }
            result = sb.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
            result = null;
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

}
