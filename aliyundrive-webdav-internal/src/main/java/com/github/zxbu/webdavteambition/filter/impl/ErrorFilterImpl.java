package com.github.zxbu.webdavteambition.filter.impl;

import com.fujieid.jap.http.JapException;
import com.fujieid.jap.http.JapHttpRequest;
import com.fujieid.jap.http.JapHttpResponse;
import com.github.zxbu.webdavteambition.filter.IErrorFilter;
import com.github.zxbu.webdavteambition.filter.IErrorFilterCall;
import com.github.zxbu.webdavteambition.filter.IErrorWrapperResponse;
import net.sf.webdav.WebdavStatus;

import java.io.IOException;

public class ErrorFilterImpl implements IErrorFilter {

    private String mErrorPage;

    private final IErrorFilterCall mErrorFilterCall;
    public ErrorFilterImpl(IErrorFilterCall iErrorFilterCall) {
        this.mErrorFilterCall = iErrorFilterCall;
    }

    @Override
    public void doFilterCall(JapHttpRequest req, JapHttpResponse res, IErrorWrapperResponse wrapperResponse) {
        try {
            if (wrapperResponse.hasErrorToSend()) {
                int status = wrapperResponse.getStatus();
                String message = wrapperResponse.getMessage();
                if (message == null) {
                    message = WebdavStatus.getStatusText(status);
                }
                res.sendError(status, message);
                String errorXml = readErrorPage().replace("{{code}}", status + "").replace("{{message}}", message);
                res.write(errorXml);
            }
            res.flushBuffer();
        } catch (Throwable t) {
            try {
                res.setStatus(500);
                res.write(t.getMessage());
                res.flushBuffer();
            } catch (IOException e) {
            }
        }
    }

    private String readErrorPage() {
        if (this.mErrorPage == null) {
            synchronized (ErrorFilterImpl.class) {
                if (this.mErrorPage == null) {
                    this.mErrorPage = this.mErrorFilterCall.readErrorPage();
                }
            }
        }
        return this.mErrorPage;
    }

    public interface Call {
        void doFilter(JapHttpRequest request, IErrorWrapperResponse errorWrapperResponse) throws JapException, IOException;
    }
}
