package net.sf.webdav.servlet.impl;

import com.fujieid.jap.http.JapException;
import com.fujieid.jap.http.JapHttpRequest;
import com.fujieid.jap.http.JapHttpResponse;
import net.sf.webdav.*;
import net.sf.webdav.exceptions.UnauthenticatedException;
import net.sf.webdav.exceptions.WebdavException;
import net.sf.webdav.fromcatalina.MD5Encoder;
import net.sf.webdav.locking.ResourceLocks;
import net.sf.webdav.methods.*;
import net.sf.webdav.servlet.IWebdavServletBean;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;

public class WebdavServletBeanImpl implements IWebdavServletBean {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(WebdavServletBeanImpl.class);

    /**
     * MD5 message digest provider.
     */
    protected static MessageDigest MD5_HELPER;

    /**
     * The MD5 helper object for this class.
     */
    protected static final MD5Encoder MD5_ENCODER = new MD5Encoder();

    private static final boolean READ_ONLY = false;
    protected ResourceLocks _resLocks;
    protected IWebdavStore _store;
    private HashMap<String, IMethodExecutor> _methodMap = new HashMap<>();
    private IWebdavServletBeanServletCall mWebdavServletBeanServletCall;

    @Override
    public void init(IWebdavServletBeanServletCall iWebdavServletBeanServletCall) {
        this.mWebdavServletBeanServletCall = iWebdavServletBeanServletCall;
        _resLocks = new ResourceLocks();
        try {
            MD5_HELPER = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void init(IWebdavStore store, String dftIndexFile, String insteadOf404,
                     int nocontentLenghHeaders, boolean lazyFolderCreationOnPut) {

        _store = store;

        IMimeTyper mimeTyper = new IMimeTyper() {
            @Override
            public String getMimeType(ITransaction transaction, String path) {
                StoredObject so = _store.getStoredObject(transaction, path);
                if (so == null) {
                    return null;
                }
                String retVal = so.getMimeType();
                if (retVal == null) {
                    retVal = WebdavServletBeanImpl.this.mWebdavServletBeanServletCall.getMimeType(path);
                }
                return retVal;
            }
        };

        register("GET", new DoGet(store, dftIndexFile, insteadOf404, _resLocks,
                mimeTyper, nocontentLenghHeaders));
        register("HEAD", new DoHead(store, dftIndexFile, insteadOf404,
                _resLocks, mimeTyper, nocontentLenghHeaders));
        DoDelete doDelete = (DoDelete) register("DELETE", new DoDelete(store,
                _resLocks, READ_ONLY));
        DoCopy doCopy = (DoCopy) register("COPY", new DoCopy(store, _resLocks,
                doDelete, READ_ONLY));
        register("LOCK", new DoLock(store, _resLocks, READ_ONLY));
        register("UNLOCK", new DoUnlock(store, _resLocks, READ_ONLY));
        register("MOVE", new DoMove(store, _resLocks, doDelete, doCopy, READ_ONLY));
        register("MKCOL", new DoMkcol(store, _resLocks, READ_ONLY));
        register("OPTIONS", new DoOptions(store, _resLocks));
        register("PUT", new DoPut(store, _resLocks, READ_ONLY,
                lazyFolderCreationOnPut));
        register("PROPFIND", new DoPropfind(store, _resLocks, mimeTyper));
        register("PROPPATCH", new DoProppatch(store, _resLocks, READ_ONLY));
        register("*NO*IMPL*", new DoNotImplemented(READ_ONLY));
    }

    @Override
    public void destroy() {
        if (_store != null)
            _store.destroy();
    }

    @Override
    public void service(JapHttpRequest req, JapHttpResponse resp) throws Exception {

        String methodName = req.getMethod();
        ITransaction transaction = null;
        boolean needRollback = false;

        if (LOG.isTraceEnabled())
            debugRequest(methodName, req);

        if (returnError(req, resp)) {
            return;
        }

        try {
            Principal userPrincipal = getUserPrincipal(req);
            transaction = _store.begin(userPrincipal, req, resp);
            needRollback = true;
            _store.checkAuthentication(transaction);
            resp.setStatus(WebdavStatus.SC_OK);

            try {
                IMethodExecutor methodExecutor = (IMethodExecutor) _methodMap
                        .get(methodName);
                if (methodExecutor == null) {
                    methodExecutor = (IMethodExecutor) _methodMap
                            .get("*NO*IMPL*");
                }

                methodExecutor.execute(transaction, req, resp);

                _store.commit(transaction);
                /** Clear not consumed data
                 *
                 * Clear input stream if available otherwise later access
                 * include current input.  These cases occure if the client
                 * sends a request with body to an not existing resource.
                 */
                if (req.getContentLength() != 0 && req.getInputStream().available() > 0) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Clear not consumed data!");
                    }
                    while (req.getInputStream().available() > 0) {
                        req.getInputStream().read();
                    }
                }
                needRollback = false;
            } catch (IOException e) {
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                e.printStackTrace(pw);
                LOG.error("IOException: " + sw.toString());
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                _store.rollback(transaction);
                throw new JapException(e);
            }

        } catch (UnauthenticatedException e) {
            resp.sendError(e.getCode());
        } catch (WebdavException e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            LOG.error("WebdavException: " + sw.toString());
            throw new JapException(e);
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            LOG.error("Exception: " + sw.toString());
            resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR, sw.toString());
        } finally {
            if (needRollback)
                _store.rollback(transaction);
        }
    }

    protected IMethodExecutor register(String methodName, IMethodExecutor method) {
        _methodMap.put(methodName, method);
        return method;
    }


    /**
     * Method that permit to customize the way
     * user information are extracted from the request, default use JAAS
     */
    protected Principal getUserPrincipal(JapHttpRequest req) {
        return req.getUserPrincipal();
    }

    private boolean returnError(JapHttpRequest req, JapHttpResponse resp) throws IOException {
        if (req.getRequestURI().equals("/error")) {
            Object codeObject = req.getAttribute("jakarta.servlet.error.status_code");
            if (codeObject != null) {
                int code = Integer.parseInt(codeObject.toString());
                if (code > 400) {
                    resp.setStatus(code);
                    resp.flushBuffer();
                    return true;
                }
            }
        }
        return false;
    }

    private void debugRequest(String methodName, JapHttpRequest req) {
        LOG.trace("-----------");
        LOG.trace("WebdavServlet\n request: methodName = " + methodName);
        LOG.trace("time: " + System.currentTimeMillis());
        LOG.trace("path: " + req.getRequestURI());
        LOG.trace("-----------");
        Enumeration<?> e = req.getHeaderNames();
        while (e.hasMoreElements()) {
            String s = (String) e.nextElement();
            LOG.trace("header: " + s + " " + req.getHeader(s));
        }
        e = req.getAttributeNames();
        while (e.hasMoreElements()) {
            String s = (String) e.nextElement();
            LOG.trace("attribute: " + s + " " + req.getAttribute(s));
        }
        e = req.getParameterNames();
        while (e.hasMoreElements()) {
            String s = (String) e.nextElement();
            LOG.trace("parameter: " + s + " " + req.getParameter(s));
        }
    }
}
