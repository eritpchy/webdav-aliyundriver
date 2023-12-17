package com.github.zxbu.webdavteambition.filter.impl;

import com.fujieid.jap.http.JapHttpRequest;
import com.fujieid.jap.http.JapHttpResponse;
import com.github.zxbu.webdavteambition.config.AliyunDriveProperties;
import com.github.zxbu.webdavteambition.filter.IErrorWrapperResponse;
import com.github.zxbu.webdavteambition.filter.IFilter;
import com.github.zxbu.webdavteambition.filter.IFilterChainCall;
import com.github.zxbu.webdavteambition.manager.AliyunDriveSessionManager;
import com.github.zxbu.webdavteambition.servlet.impl.StartupServletImpl;
import com.github.zxbu.webdavteambition.store.AliyunDriveClientService;
import com.github.zxbu.webdavteambition.util.update.FrontendUpdateFactory;
import net.sf.webdav.WebdavStatus;
import net.xdow.aliyundrive.IAliyunDrive;
import net.xdow.aliyundrive.bean.AliyunDriveEnum;
import net.xdow.aliyundrive.bean.AliyunDriveRequest;
import net.xdow.aliyundrive.bean.AliyunDriveResponse;
import net.xdow.aliyundrive.exception.NotAuthenticatedException;
import net.xdow.aliyundrive.util.StringUtils;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class AliyunDriveLoginFilterImpl implements IFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AliyunDriveLoginFilterImpl.class);

    private static final long DEFAULT_FRONTEND_UPDATE_CHECKIN_TIME_S = 24 * 60 * 60; //24h
    private long mLastFrontendUpdateCheckedTimeS = 0L;

    @Override
    public void doHttpFilter(JapHttpRequest request, JapHttpResponse response, IFilterChainCall chain) throws IOException {
        String requestURI = request.getRequestURI();
        try {
            if ("GET".equalsIgnoreCase(request.getMethod())
                && ("/".equals(requestURI) || "/dav".equals(requestURI))) {
                String accessToken = request.getParameter("access_token");
                String refreshToken = request.getParameter("refresh_token");
                String tokenType = request.getParameter("token_type");
                String expiresIn = request.getParameter("expires_in");
                String redirectUri = request.getParameter("redirect_uri");
                if (!StringUtils.isEmpty(accessToken) && !StringUtils.isEmpty(refreshToken)
                        && !StringUtils.isEmpty(tokenType) && !StringUtils.isEmpty(expiresIn)) {
                    AliyunDriveResponse.AccessTokenInfo accessTokenInfo = new AliyunDriveResponse.AccessTokenInfo();
                    accessTokenInfo.setAccessToken(accessToken);
                    accessTokenInfo.setRefreshToken(refreshToken);
                    accessTokenInfo.setTokenType(tokenType);
                    accessTokenInfo.setExpiresIn(expiresIn);
                    AliyunDriveClientService service = (AliyunDriveClientService) request.getServletContextAttribute(AliyunDriveClientService.class.getName());
                    service.getAliyunDrive().setAccessTokenInfo(accessTokenInfo);
                    service.getProperties().save(accessTokenInfo);
                    LOGGER.info("登录成功");
                    service.onAccountChanged();
                    sendHtml5Redirect(response, !StringUtils.isEmpty(redirectUri) ? redirectUri : "/", 0, "");
                    return;
                } else if (!StringUtils.isEmpty(refreshToken)) {
                    AliyunDriveClientService service = (AliyunDriveClientService) request.getServletContextAttribute(AliyunDriveClientService.class.getName());
                    IAliyunDrive aliyunDrive = service.getAliyunDrive();
                    AliyunDriveRequest.AccessTokenInfo query = new AliyunDriveRequest.AccessTokenInfo();
                    query.setGrantType(AliyunDriveEnum.GrantType.RefreshToken);
                    query.setRefreshToken(refreshToken);
                    AliyunDriveResponse.AccessTokenInfo accessTokenInfo = aliyunDrive.getAccessToken(query)
                            .disableAuthorizeCheck().execute();
                    if (accessTokenInfo.isError()) {
                        response.sendError(JapHttpResponse.SC_UNAUTHORIZED,
                                accessTokenInfo.getMessage() + "(" + accessTokenInfo.getCode() + ")");
                        return;
                    }
                    aliyunDrive.setAccessTokenInfo(accessTokenInfo);
                    service.getProperties().save(accessTokenInfo);
                    StartupServletImpl startupServletImpl = (StartupServletImpl) request.getServletContextAttribute(StartupServletImpl.class.getName());
                    if (startupServletImpl == null) {
                        LOGGER.error("Unexpected Error: startupService is null, maybe server is shutting down.");
                        return;
                    }
                    AliyunDriveSessionManager sessionManager = startupServletImpl.getTaskByClass(AliyunDriveSessionManager.class);
                    if (sessionManager != null) {
                        sessionManager.updateSession();
                    }
                    LOGGER.info("登录成功");
                    service.onAccountChanged();
                    sendHtml5Redirect(response, !StringUtils.isEmpty(redirectUri) ? redirectUri : "/", 0, "");
                    return;
                }
            }
        } finally {
            downloadFrontendUpdateIfNeeded(request);
            chain.doFilter(request, response);
        }
        if (response.getSource() instanceof IErrorWrapperResponse) {
            IErrorWrapperResponse resp = (IErrorWrapperResponse) response.getSource();
            if (resp.getStatus() == WebdavStatus.SC_INTERNAL_SERVER_ERROR) {
                if (String.valueOf(resp.getMessage()).contains(NotAuthenticatedException.class.getName())) {
                    if (!"GET".equalsIgnoreCase(request.getMethod())) {
                        return;
                    }
                    //开始登录
                    String rootUrl = request.getRequestUrl().toString();
                    String redirectUri = request.getParameter("redirect_uri");
                    rootUrl = HttpUrl.parse(rootUrl).newBuilder().addQueryParameter("redirect_uri", redirectUri).build().toString();
                    AliyunDriveClientService service = (AliyunDriveClientService) request.getServletContextAttribute(AliyunDriveClientService.class.getName());
                    AliyunDriveProperties properties = service.getProperties();
                    if (properties.getDriver() == AliyunDriveProperties.Driver.OpenApi) {
                        String loginUrl = String.format(Locale.getDefault(), properties.getAliyunAuthorizeUrl(), rootUrl);
                        resp.sendRedirect(loginUrl);
//                    resp.sendError(WebdavStatus.SC_UNAUTHORIZED, "AliyunDriveAccessTokenInvalid");
                        resp.sendError(WebdavStatus.SC_UNAUTHORIZED,
                                "授权已失效, 请<a href=\"" + loginUrl + "\">点击链接</a>前往获取授权. ");
                    }
                }
            }
        }
    }

    private void downloadFrontendUpdateIfNeeded(JapHttpRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return;
        }
        long currentTime = System.currentTimeMillis() / 1000;
        if (Math.abs(mLastFrontendUpdateCheckedTimeS - currentTime) < DEFAULT_FRONTEND_UPDATE_CHECKIN_TIME_S) {
            return;
        }
        synchronized (AliyunDriveLoginFilterImpl.class) {
            AliyunDriveClientService service = (AliyunDriveClientService) request.getServletContextAttribute(AliyunDriveClientService.class.getName());
            AliyunDriveProperties properties = service.getProperties();
            File frontendDir = properties.getFrontendDir();
            FrontendUpdateFactory factory = new FrontendUpdateFactory(frontendDir);
            factory.doUpdateCheck(true, null);
            mLastFrontendUpdateCheckedTimeS = currentTime;
        }
    }

    private void sendHtml5Redirect(JapHttpResponse response, String url, int delayTimeSec, String content) throws IOException {
        response.setStatus(JapHttpResponse.SC_OK);
        response.write("<!DOCTYPE html>\n" +
                "<html>\n" +
                "  <head>\n" +
                "    <meta http-equiv=\"refresh\" content=\""+ delayTimeSec +"; url='" + url + "'\" />\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <p>"+ content +"</p>\n" +
                "  </body>\n" +
                "</html>");
    }
}
