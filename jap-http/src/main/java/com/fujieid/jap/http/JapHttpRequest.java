package com.fujieid.jap.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * Define an interface to adapt to request objects of different frameworks,
 * such as: <code>javax.servlet.http.HttpServletRequest</code> or <code>com.blade.mvc.http.HttpRequest</code>
 *
 * @author yadong.zhang (yadong.zhang0415(a)gmail.com)
 * @version 1.0.0
 * @since 1.0.5
 */
public interface JapHttpRequest extends JapServletRequest {

    /**
     * Get the actual source object
     *
     * @return Object
     */
    Object getSource();

    /**
     * Returns the value of a request parameter as a <code>String</code>, or <code>null</code> if the parameter does not
     * exist.
     *
     * @param name a <code>String</code> specifying the name of the parameter
     * @return a <code>String</code> representing the single value of the parameter
     */
    String getParameter(String name);

    /**
     * Returns an array of <code>String</code> objects containing all of the values the given request parameter has, or
     * <code>null</code> if the parameter does not exist.
     *
     * @param name a <code>String</code> containing the name of the parameter whose value is requested
     * @return an array of <code>String</code> objects containing the parameter's values
     */
    String[] getParameterValues(String name);

    /**
     * an immutable java.util.Map containing parameter names as keys and parameter values as map values.
     *
     * @return Returns a java.util.Map of the parameters of this request.
     */
    Map<String, String[]> getParameterMap();

    /**
     * Returns the value of the specified request header as a <code>String</code>. If the request did not include a
     * header of the specified name, this method returns <code>null</code>.
     *
     * @param name a <code>String</code> specifying the header name
     * @return a <code>String</code> containing the value of the requested header, or <code>null</code> if the request
     * does not have a header of that name
     */
    String getHeader(String name);

    /**
     * Returns the part of this request's URL from the protocol name up to the query string in the first line of the
     * HTTP request.
     *
     * @return a <code>String</code> containing the part of the URL from the protocol name up to the query string
     */
    String getRequestUri();

    /**
     * Reconstructs the URL the client used to make the request. The returned URL contains a protocol, server name, port
     * number, and server path, but it does not include query string parameters.
     *
     * @return a <code>StringBuffer</code> object containing the reconstructed URL
     */
    StringBuffer getRequestUrl();

    /**
     * Returns the name of the HTTP method with which this request was made, for example, GET, POST, or PUT.
     *
     * @return a <code>String</code> specifying the name of the method with which this request was made
     */
    String getMethod();

    /**
     * Returns the query string that is contained in the request URL after the path. This method returns
     * <code>null</code> if the URL does not have a query string.
     *
     * @return a <code>String</code> containing the query string or <code>null</code> if the URL contains no query
     * string.
     */
    String getQueryString();

    /**
     * Returns an array containing all of the <code>JapHttpCookie</code> objects the client sent with this request. This method
     * returns <code>null</code> if no cookies were sent.
     *
     * @return an array of all the <code>JapHttpCookie</code> included with this request, or <code>null</code> if the request
     * has no cookies
     */
    JapHttpCookie[] getCookies();

    /**
     * Returns the Internet Protocol (IP) address of the client or last proxy that sent the request.
     *
     * @return a <code>String</code> containing the IP address of the client that sent the request
     */
    String getRemoteAddr();

    /**
     * Returns the part of this request's URL that calls the servlet. This path starts with a "/" character and includes
     * either the servlet name or a path to the servlet, but does not include any extra path information or a query
     * string.
     *
     * @return a <code>String</code> containing the name or path of the servlet being called, as specified in the
     * request URL, decoded, or an empty string if the servlet used to process the request is matched using the
     * "/*" pattern.
     */
    String getServletPath();

    /**
     * Returns the current <code>HttpSession</code> associated with this request
     *
     * @return the <code>HttpSession</code> associated with this request
     */
    JapHttpSession getSession();

    /**
     * Retrieves the body of the request as character data using a <code>BufferedReader</code>. The reader translates
     * the character data according to the character encoding used on the body.
     *
     * @return a <code>BufferedReader</code> containing the body of the request
     * @throws IOException if an input or output exception occurred
     */
    BufferedReader getReader() throws IOException;

    String getServerName();

    String getContextPath();

    String getPathInfo();

    Object getAttribute(String name);

    Object getServletContextAttribute(String name);

    String getRequestURI();

    Locale getLocale();

    InputStream getInputStream() throws IOException;

    long getContentLength() throws IOException;

    Principal getUserPrincipal();

    Enumeration<String> getHeaderNames();

    Enumeration<String> getAttributeNames();

    Enumeration<String> getParameterNames();


    /**
     * Gets all the {@link JapPart} components of this request, provided
     * that it is of type <tt>multipart/form-data</tt>.
     *
     * <p>If this request is of type <tt>multipart/form-data</tt>, but
     * does not contain any Part components, the returned
     * <tt>Collection</tt> will be empty.
     *
     * <p>Any changes to the returned <code>Collection</code> must not
     * affect this <code>HttpServletRequest</code>.
     *
     * @return a (possibly empty) <code>Collection</code> of the
     * Part components of this request
     *
     * @throws IOException if an I/O error occurred during the retrieval
     * of the {@link JapPart} components of this request
     *
     * @throws IllegalStateException if the request body is larger than
     * <tt>maxRequestSize</tt>, or any Part in the request is larger than
     * <tt>maxFileSize</tt>
     *
     * @see javax.servlet.annotation.MultipartConfig#maxFileSize
     * @see javax.servlet.annotation.MultipartConfig#maxRequestSize
     *
     * @since Servlet 3.0
     */
    public Collection<JapPart> getParts() throws IOException;


    /**
     * Gets the {@link JapPart} with the given name.
     *
     * @param name the name of the requested Part
     *
     * @return The Part with the given name, or <tt>null</tt> if this
     * request is of type <tt>multipart/form-data</tt>, but does not
     * contain the requested Part
     *
     * @throws IOException if an I/O error occurred during the retrieval
     * of the requested Part
     * @throws IllegalStateException if the request body is larger than
     * <tt>maxRequestSize</tt>, or any Part in the request is larger than
     * <tt>maxFileSize</tt>
     *
     * @see javax.servlet.annotation.MultipartConfig#maxFileSize
     * @see javax.servlet.annotation.MultipartConfig#maxRequestSize
     *
     * @since Servlet 3.0
     */
    public JapPart getPart(String name) throws IOException;
}
