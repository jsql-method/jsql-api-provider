package pl.jsql.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.jsql.connector.dto.HashQueryPair;
import it.jsql.connector.exceptions.ErrorMessagesSingleton;
import it.jsql.connector.exceptions.JSQLException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Dawid on 2016-09-13.
 * Modified by Michael on 2018-09-10.
 */
class JSQLConnector {

    protected Boolean isMock = false;

    //protected String host = "http://localhost:9191/";
    protected String host = "http://softwarecartoon.com:9391/";
    protected String requestQueriesPath = "api/request/queries";

    private String apiKey = null;
    private String memberKey = null;

    public JSQLConnector() {
    }

    public JSQLConnector(String apiKey, String memberKey) {
        this.setApiKey(apiKey);
        this.setMemberKey(memberKey);
    }

    public String getRequestQueriesPath() {
        return requestQueriesPath;
    }

    public String getHost() {
        return host;
    }

    public String getApiKey() throws JSQLException {

        if (this.apiKey == null) {
            throw new JSQLException("No apiKey defined");
        }

        return apiKey;

    }

    public String getMemberKey() throws JSQLException {
        if (this.memberKey == null) {
            throw new JSQLException("No member key defined");
        }
        return memberKey;
    }

    public void setMemberKey(String memberKey) {
        this.memberKey = memberKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public List<HashQueryPair> requestQueries(List<String> hashesList) throws JSQLException {

        System.out.println("requestQueries apiKey " + this.getApiKey());

        if (isMock) {

            List<HashQueryPair> mockResponse = new ArrayList<>();

            for (String hash : hashesList) {
                mockResponse.add(new HashQueryPair(hash, "select * from users"));
            }

            return mockResponse;

        } else {

            System.out.println("size: "+hashesList.size());

            return this.call(this.buildJSONRequest(hashesList), hashesList.size() > 1);
        }

    }

    protected String buildJSONRequest(List<String> hashesList) {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("[");

        for (int i = 0; i < hashesList.size(); i++) {
            stringBuilder.append("\"" + hashesList.get(i) + "\"");
            if (i != hashesList.size() - 1) {
                stringBuilder.append(",");
            }
        }

        stringBuilder.append("]");

        return stringBuilder.toString();

    }


    protected List<HashQueryPair> call(String request, Boolean isGrouped) {
        String fullUrl = this.getHost() + this.getRequestQueriesPath();


        System.out.println("request : "+request);

        if (isGrouped)
            fullUrl += "/grouped";
        try {

            URL url = new URL(fullUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("ApiKey", this.getApiKey());
            conn.setRequestProperty("MemberKey", this.getMemberKey());

            OutputStream os = conn.getOutputStream();
            os.write(request.getBytes());
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
                ErrorMessagesSingleton.getInstance().setMessage(response);
                throw new JSQLException("HTTP error code : " + conn.getResponseCode() + "\nHTTP error message : " + response);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            StringBuilder builder = new StringBuilder();

            while (br.ready()) {
                builder.append(br.readLine());
            }

            conn.disconnect();

            String responseJSON = builder.toString();

            if (!responseJSON.isEmpty()) {
                List<HashQueryPair> response = Arrays.asList(new ObjectMapper().readValue(responseJSON, HashQueryPair[].class));
                return response;
            }


        } catch (MalformedURLException e) {
            e.printStackTrace();
            if (e.getMessage() != null)
                ErrorMessagesSingleton.getInstance().setMessage(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            if (e.getMessage() != null)
                ErrorMessagesSingleton.getInstance().setMessage(e.getMessage());
        } catch (JSQLException e) {
            e.printStackTrace();
            if (e.getMessage() != null)
                ErrorMessagesSingleton.getInstance().setMessage(e.getMessage());
        }

        return null;

    }

}
