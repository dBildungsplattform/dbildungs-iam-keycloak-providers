package com.spsh.oidc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import com.jayway.jsonpath.JsonPath;

public class SpshApiOidcMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    private static final Logger LOGGER = Logger.getLogger(SpshApiOidcMapper.class);
    public static final String PROVIDER_ID = "spsh-custom-oidc-api-mapper";
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    public static final String FETCH_URL = "fetchUrl";
    public static final String EXTRACT_JSON_PATH = "extractJsonPath";

    static {
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, SpshApiOidcMapper.class);

        ProviderConfigProperty fetchUrlProperty = new ProviderConfigProperty();
        fetchUrlProperty.setName(FETCH_URL);
        fetchUrlProperty.setLabel("SPSH Fetch Url");
        fetchUrlProperty.setType(ProviderConfigProperty.STRING_TYPE);
        fetchUrlProperty.setHelpText("The URL to fetch data from the SPSH Backend.");
        configProperties.add(fetchUrlProperty);

        ProviderConfigProperty extractPathProperty = new ProviderConfigProperty();
        extractPathProperty.setName(EXTRACT_JSON_PATH);
        extractPathProperty.setLabel("SPSH Extract Json Path");
        extractPathProperty.setType(ProviderConfigProperty.STRING_TYPE);
        extractPathProperty.setHelpText("The JSON path to extract data from the API response.");
        configProperties.add(extractPathProperty);
    }

    @Override
    public String getDisplayCategory() {
        return "Token Mapper";
    }

    @Override
    public String getDisplayType() {
        return "SPSH Custom OIDC Api Mapper";
    }

    @Override
    public String getHelpText() {
        return "The mapper calls the provided SPSH fetch url, extracts the provided JsonPath from the api response and maps the result if not null to the claim";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel,
      UserSessionModel userSession, KeycloakSession keycloakSession,
      ClientSessionContext clientSessionCtx) {
        String fetchUrl = mappingModel.getConfig().get(FETCH_URL);
        String extractJsonPath = mappingModel.getConfig().get(EXTRACT_JSON_PATH);
        String userJwtToken = userSession.getNote("access_token");

        if (fetchUrl == null) {
            LOGGER.warn("SpshApiOidcMapper: fetchUrl is null. No data will be fetched, extracted and mapped.");
            return;
        }
        if (extractJsonPath == null) {
            LOGGER.warn("SpshApiOidcMapper: extractJsonPath is null. No data will be fetched, extracted and mapped.");
            return;
        }
        if (userJwtToken == null) {
            LOGGER.warn("SpshApiOidcMapper: userJwtToken is null. No data will be fetched, extracted and mapped.");
            return;
        }

        try {
            String responseData = fetchApiData(fetchUrl, userJwtToken);
            String extractedValue = extractFromJson(responseData, extractJsonPath);
            if (extractedValue != null) {
                OIDCAttributeMapperHelper.mapClaim(token, mappingModel, extractedValue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String fetchApiData(String url, String userJwtToken) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            request.addHeader("Authorization", "Bearer " + userJwtToken);

            return httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                if (statusCode >= 200 && statusCode < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new IOException("Unexpected response status: " + statusCode);
                }
            });
        }
    }

    private String extractFromJson(String jsonData, String jsonPath) {
            return JsonPath.read(jsonData, jsonPath).toString();
    }
}