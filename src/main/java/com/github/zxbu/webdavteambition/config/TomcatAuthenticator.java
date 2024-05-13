package com.github.zxbu.webdavteambition.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.sf.webdav.util.EscaperUtils;
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.security.Principal;
import java.util.Calendar;
import java.util.Date;

@Slf4j
public class TomcatAuthenticator extends BasicAuthenticator {
    public static final String ACCESS_CONTROL_REQUEST_METHOD_HEADER = "Access-Control-Request-Method";
    private static final String DAV_PREFIX = "/dav";
    private final String mShareToken;

    public TomcatAuthenticator(String shareToken) {
        this.mShareToken = shareToken;
        setAllowCorsPreflight(AllowCorsPreflight.ALWAYS.name());
    }

    @Override
    public void invoke(Request req, Response response) throws IOException, ServletException {
        if (req instanceof HttpServletRequest) {
            if (isPreflightRequest(req)) {
                setRequestAsAnonymous(req);
            }
        }
        super.invoke(req, response);
    }

    @Override
    public boolean doAuthenticate(Request req, HttpServletResponse response) throws IOException {

        if (req instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest)req;
            if (isPreflightRequest(request)) {
                return true;
            }
            if ("/favicon.ico".equals(request.getRequestURI())) {
                return true;
            }
            return handlePublicLink(req, response);
        }
        return super.doAuthenticate(req, response);
    }



    private boolean isPreflightRequest(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"OPTIONS".equalsIgnoreCase(method))
            return false;
        if (request.getHeader(ACCESS_CONTROL_REQUEST_METHOD_HEADER) == null)
            return false;
        return true;
    }

    private boolean handlePublicLink(Request req, HttpServletResponse response) throws IOException {
        if (req instanceof HttpServletRequest) {
        } else {
            return super.doAuthenticate(req, response);
        }
        HttpServletRequest request = (HttpServletRequest) req;
        String path = request.getRequestURI();
        String method = request.getMethod();
        path = String.valueOf(path).replace(DAV_PREFIX, "");
        if (!"GET".equalsIgnoreCase(method)) {
            log.debug("handlePublicLink: " + path + " Expect http method: GET, got: " + method);
            return super.doAuthenticate(req, response);
        }
        String publicLinkHash = request.getParameter("p");
        if (StringUtils.isEmpty(publicLinkHash)) {
            log.debug("handlePublicLink: " + path + " Not public link");
            return super.doAuthenticate(req, response);
        }
        String expireStr = request.getParameter("expire");
        log.debug( "handlePublicLink: " + path + " expireStr: " + expireStr);
        long expireSec = Long.parseLong(expireStr);
        if (getCurrentDateGMT().getTime() / 1000 > expireSec) {
            log.debug("handlePublicLink:  " + path + " Link expired");
            try {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } catch (IOException e) {
            }
            return false;
        }

        String shareToken = mShareToken;
        if (publicLinkHash.equals(DigestUtils.sha1Hex(path + "_" + shareToken + "_" + expireSec))
                || publicLinkHash.equals(DigestUtils.sha1Hex(EscaperUtils.escapePath(path) + "_" + shareToken + "_" + expireSec))) {
            setRequestAsAnonymous(req);
            return true; // 允许匿名访问
        }
        log.debug("handlePublicLink:  " + path + " Hash not match");
        try {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (IOException e) {
        }
        return false;
    }

    private void setRequestAsAnonymous(Request req) {
        req.setUserPrincipal(new AnonymousPrincipal());
        req.setAuthType("NONE");
    }

    private Date getCurrentDateGMT() {
        Calendar time = Calendar.getInstance();
        time.add(Calendar.MILLISECOND, -time.getTimeZone().getOffset(time.getTimeInMillis()));
        return time.getTime();
    }

    private static class AnonymousPrincipal implements Principal {
        @Override
        public String getName() {
            return "anonymousUser";
        }
    }
}
