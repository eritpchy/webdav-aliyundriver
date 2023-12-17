package com.fujieid.jap.http;

public interface JapServletRequest {

    /**
     * Returns the name of the scheme used to make this request, for example,
     * <code>http</code>, <code>https</code>, or <code>ftp</code>. Different
     * schemes have different rules for constructing URLs, as noted in RFC 1738.
     *
     * @return a <code>String</code> containing the name of the scheme used to
     *         make this request
     */
    public String getScheme();

    /**
     * Returns a boolean indicating whether this request was made using a secure
     * channel, such as HTTPS.
     *
     * @return a boolean indicating if the request was made using a secure
     *         channel
     */
    public boolean isSecure();
}
