package com.example.habits.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * Spring Security 6.5's documented "SPA" CSRF request handler: tokens are
 * rendered (XOR-encoded) for the response, the deferred token is materialized
 * so the XSRF-TOKEN cookie is always written, and incoming values are resolved
 * as raw when they came from the X-XSRF-TOKEN header (cookie echo), or decoded
 * when they came from the _csrf request parameter (server-rendered forms).
 */
public final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       Supplier<CsrfToken> csrfToken) {
        // Always use the XOR handler to provide BREACH protection when the
        // token is rendered into the response.
        this.xor.handle(request, response, csrfToken);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String headerValue = request.getHeader(csrfToken.getHeaderName());
        // Header present (SPA echoing the cookie's raw value) -> plain resolver;
        // otherwise (_csrf parameter from a form) -> XOR-decoding resolver.
        return (StringUtils.hasText(headerValue) ? this.plain : this.xor)
                .resolveCsrfTokenValue(request, csrfToken);
    }
}
