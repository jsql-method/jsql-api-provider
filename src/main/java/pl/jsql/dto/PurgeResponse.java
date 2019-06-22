package pl.jsql.dto;

import java.util.List;

public class PurgeResponse {

    public List<String> options;
    public List<String> queries;

    @Override
    public String toString() {
        return "PurgeResponse{" +
                "options=" + options +
                ", queries=" + queries +
                '}';
    }
}
