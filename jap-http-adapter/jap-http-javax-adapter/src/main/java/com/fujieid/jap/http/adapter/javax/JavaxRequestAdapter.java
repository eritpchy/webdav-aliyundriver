package com.fujieid.jap.http.adapter.javax;

import com.fujieid.jap.http.JapHttpCookie;
import com.fujieid.jap.http.JapHttpRequest;
import com.fujieid.jap.http.JapHttpSession;
import com.fujieid.jap.http.JapPart;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.*;

/**
 * @author yadong.zhang (yadong.zhang0415(a)gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 */
public class JavaxRequestAdapter implements JapHttpRequest {

    private final HttpServletRequest request;

    public JavaxRequestAdapter(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Get the actual source object
     *
     * @return Object
     */
    @Override
    public Object getSource() {
        return request;
    }

    /**
     * Returns the value of a request parameter as a <code>String</code>, or <code>null</code> if the parameter does not
     * exist.
     *
     * @param name a <code>String</code> specifying the name of the parameter
     * @return a <code>String</code> representing the single value of the parameter
     */
    @Override
    public String getParameter(String name) {
        return this.request.getParameter(name);
    }

    /**
     * Returns an array of <code>String</code> objects containing all of the values the given request parameter has, or
     * <code>null</code> if the parameter does not exist.
     *
     * @param name a <code>String</code> containing the name of the parameter whose value is requested
     * @return an array of <code>String</code> objects containing the parameter's values
     */
    @Override
    public String[] getParameterValues(String name) {
        return this.request.getParameterValues(name);
    }

    /**
     * an immutable java.util.Map containing parameter names as keys and parameter values as map values.
     *
     * @return Returns a java.util.Map of the parameters of this request.
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        return this.request.getParameterMap();
    }

    /**
     * Returns the value of the specified request header as a <code>String</code>. If the request did not include a
     * header of the specified name, this method returns <code>null</code>.
     *
     * @param name a <code>String</code> specifying the header name
     * @return a <code>String</code> containing the value of the requested header, or <code>null</code> if the request
     * does not have a header of that name
     */
    @Override
    public String getHeader(String name) {
        return this.request.getHeader(name);
    }

    /**
     * Returns the part of this request's URL from the protocol name up to the query string in the first line of the
     * HTTP request.
     *
     * @return a <code>String</code> containing the part of the URL from the protocol name up to the query string
     */
    @Override
    public String getRequestUri() {
        return this.request.getRequestURI();
    }

    /**
     * Reconstructs the URL the client used to make the request. The returned URL contains a protocol, server name, port
     * number, and server path, but it does not include query string parameters.
     *
     * @return a <code>StringBuffer</code> object containing the reconstructed URL
     */
    @Override
    public StringBuffer getRequestUrl() {
        return this.request.getRequestURL();
    }

    /**
     * Returns the name of the HTTP method with which this request was made, for example, GET, POST, or PUT.
     *
     * @return a <code>String</code> specifying the name of the method with which this request was made
     */
    @Override
    public String getMethod() {
        return this.request.getMethod();
    }

    /**
     * Returns the query string that is contained in the request URL after the path. This method returns
     * <code>null</code> if the URL does not have a query string.
     *
     * @return a <code>String</code> containing the query string or <code>null</code> if the URL contains no query
     * string.
     */
    @Override
    public String getQueryString() {
        return this.request.getQueryString();
    }

    /**
     * Returns the Internet Protocol (IP) address of the client or last proxy that sent the request.
     *
     * @return a <code>String</code> containing the IP address of the client that sent the request
     */
    @Override
    public String getRemoteAddr() {
        return this.request.getRemoteAddr();
    }

    /**
     * Returns the part of this request's URL that calls the servlet. This path starts with a "/" character and includes
     * either the servlet name or a path to the servlet, but does not include any extra path information or a query
     * string.
     *
     * @return a <code>String</code> containing the name or path of the servlet being called, as specified in the
     * request URL, decoded, or an empty string if the servlet used to process the request is matched using the
     * "/*" pattern.
     */
    @Override
    public String getServletPath() {
        return this.request.getServletPath();
    }

    /**
     * Returns an array containing all of the <code>JapHttpCookie</code> objects the client sent with this request. This method
     * returns <code>null</code> if no cookies were sent.
     *
     * @return an array of all the <code>JapHttpCookie</code> included with this request, or <code>null</code> if the request
     * has no cookies
     */
    @Override
    public JapHttpCookie[] getCookies() {
        Cookie[] cookies = this.request.getCookies();
        if (null == cookies || cookies.length == 0) {
            return null;
        }
        int cookieLen = cookies.length;
        JapHttpCookie[] japHttpCookies = new JavaxCookieAdapter[cookieLen];
        for (int i = 0; i < cookieLen; i++) {
            japHttpCookies[i] = new JavaxCookieAdapter(cookies[i]);
        }
        return japHttpCookies;
    }

    /**
     * Returns the current <code>HttpSession</code> associated with this request
     *
     * @return the <code>HttpSession</code> associated with this request
     */
    @Override
    public JapHttpSession getSession() {
        return new JavaxSessionAdapter(this.request.getSession());
    }

    /**
     * Retrieves the body of the request as character data using a <code>BufferedReader</code>. The reader translates
     * the character data according to the character encoding used on the body.
     *
     * @return a <code>BufferedReader</code> containing the body of the request
     * @throws IOException if an input or output exception occurred
     */
    @Override
    public BufferedReader getReader() throws IOException {
        return this.request.getReader();
    }

    @Override
    public String getServerName() {
        return this.request.getServerName();
    }

    @Override
    public String getContextPath() {
        return this.request.getContextPath();
    }

    @Override
    public String getPathInfo() {
        return this.request.getPathInfo();
    }

    @Override
    public Object getAttribute(String name) {
        return this.request.getAttribute(name);
    }

    @Override
    public Object getServletContextAttribute(String name) {
        return this.request.getServletContext().getAttribute(name);
    }

    @Override
    public String getRequestURI() {
        return this.request.getRequestURI();
    }

    @Override
    public Locale getLocale() {
        return this.request.getLocale();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return this.request.getInputStream();
    }

    @Override
    public long getContentLength() {
        return this.request.getContentLength();
    }

    @Override
    public Principal getUserPrincipal() {
        return this.request.getUserPrincipal();
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return this.request.getHeaderNames();
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return this.request.getAttributeNames();
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return this.request.getParameterNames();
    }

    @Override
    public Collection<JapPart> getParts() throws IOException {
        try {
            List<JapPart> japParts = new ArrayList<>();
            Collection<Part> parts = this.request.getParts();
            for (final Part part : parts) {
                japParts.add(new JavaxJapPart(part));
            }
            return japParts;
        } catch (ServletException e) {
            throw new IOException(e);
        }
    }

    @Override
    public JapPart getPart(String name) throws IOException {
        try {
            Part part = this.request.getPart(name);
            if (part == null) {
                return null;
            }
            return new JavaxJapPart(part);
        } catch (ServletException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String getScheme() {
        return this.request.getScheme();
    }

    @Override
    public boolean isSecure() {
        return this.request.isSecure();
    }
}
