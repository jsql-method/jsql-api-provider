package pl.jsql.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.jsql.interceptors.ProviderSecurityInterceptor;

import javax.servlet.http.HttpServletRequest;

@Service
@Transactional
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SecurityService {

    @Autowired
    private HttpServletRequest request;

    public String getApiKey() {
        return request.getHeader(ProviderSecurityInterceptor.API_KEY_HEADER);
    }

    public String getDevKey() {
        return request.getHeader(ProviderSecurityInterceptor.DEV_KEY_HEADER);
    }

    public String getKey() {
        return this.getApiKey()+"-";
    }

}
