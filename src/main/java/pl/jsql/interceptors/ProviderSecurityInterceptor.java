package pl.jsql.interceptors;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import pl.jsql.exceptions.JSQLException;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class ProviderSecurityInterceptor {

    public static final String API_KEY_HEADER = "ApiKey";
    public static final String DEV_KEY_HEADER = "DevKey";

    @Autowired
    private HttpServletRequest request;

    @Pointcut("@annotation(pl.jsql.annotations.ProviderSecurity)")
    private void providerSecurityAnnotation() {
    }

    @Around("pl.jsql.interceptors.ProviderSecurityInterceptor.providerSecurityAnnotation()")
    Object doSomething(ProceedingJoinPoint pjp) throws Throwable {

        String apiKey = request.getHeader(API_KEY_HEADER);
        String devKey = request.getHeader(DEV_KEY_HEADER);

        if (apiKey == null || devKey == null) {
            return new ResponseEntity<>(new JSQLException("Unauthorized"), HttpStatus.UNAUTHORIZED);
        }

        return pjp.proceed();

    }

}
