package pl.jsql.dto;

import pl.jsql.enums.DatabaseDialectEnum;
import pl.jsql.enums.EncodingEnum;

public class OptionsResponse {

    public String apiKey;

    public Boolean isProductionDeveloper;

    public EncodingEnum encodingAlgorithm;

    public Boolean isSalt;

    public String salt;

    public Boolean saltBefore;

    public Boolean saltAfter;

    public Boolean saltRandomize;

    public Boolean hashLengthLikeQuery;

    public Integer hashMinLength;

    public Integer hashMaxLength;

    public Boolean removeQueriesAfterBuild;

    public DatabaseDialectEnum databaseDialect;

    public Boolean allowedPlainQueries;

    public Boolean prodCache;

    public String randomSaltBefore;

    public String randomSaltAfter;

    public DatabaseConnectionResponse productionDatabaseOptions;

    public DatabaseConnectionResponse developerDatabaseOptions;

}
