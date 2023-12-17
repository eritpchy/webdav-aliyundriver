/*
 * Copyright 1999,2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.webdav.methods;

import com.fujieid.jap.http.JapException;
import com.fujieid.jap.http.JapHttpRequest;
import com.fujieid.jap.http.JapHttpResponse;
import net.sf.webdav.*;
import net.sf.webdav.exceptions.AccessDeniedException;
import net.sf.webdav.exceptions.LockFailedException;
import net.sf.webdav.exceptions.WebdavException;
import net.sf.webdav.fromcatalina.XMLHelper;
import net.sf.webdav.fromcatalina.XMLWriter;
import net.sf.webdav.locking.LockedObject;
import net.sf.webdav.locking.ResourceLocks;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

public class DoPropfind extends AbstractMethod {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoPropfind.class);

    /**
     * PROPFIND - Specify a property mask.
     */
    private static final int FIND_BY_PROPERTY = 0;

    /**
     * PROPFIND - Display all properties.
     */
    private static final int FIND_ALL_PROP = 1;

    /**
     * PROPFIND - Return property names.
     */
    private static final int FIND_PROPERTY_NAMES = 2;

    private IWebdavStore _store;
    private ResourceLocks _resourceLocks;
    private IMimeTyper _mimeTyper;

    private int _depth;

    private static final String NAMESPACE_APPLE_WEBDAVFS = "http://www.apple.com/webdav_fs/props/";
    private static final String NAMESPACE_NEXTCLOUD = "http://nextcloud.org/ns";
    private static final String NAMESPACE_OWNCLOUD = "http://owncloud.org/ns";

    public DoPropfind(IWebdavStore store, ResourceLocks resLocks,
                      IMimeTyper mimeTyper) {
        _store = store;
        _resourceLocks = resLocks;
        _mimeTyper = mimeTyper;
    }

    public void execute(ITransaction transaction, JapHttpRequest req,
                        JapHttpResponse resp) throws IOException, LockFailedException {
        LOG.trace("-- " + this.getClass().getName());

        // Retrieve the resources
        String path = getCleanPath(getRelativePath(req));
        String tempLockOwner = "doPropfind" + System.currentTimeMillis() + String.valueOf(req);
        _depth = getDepth(req);

        if (_resourceLocks.lock(transaction, path, tempLockOwner, false,
                _depth, TEMP_TIMEOUT, TEMPORARY)) {

            StoredObject so = null;
            try {
                so = _store.getStoredObject(transaction, path);
                if (so == null) {
                    resp.setContentType("text/xml; charset=UTF-8");
                    resp.sendError(JapHttpResponse.SC_NOT_FOUND, req
                            .getRequestURI());
                    return;
                }

                Vector<String> properties = null;
                path = getCleanPath(getRelativePath(req));

                int propertyFindType = FIND_ALL_PROP;
                Node propNode = null;

                if (req.getContentLength() > 0) {
                    DocumentBuilder documentBuilder = getDocumentBuilder();
                    try {
                        Document document = documentBuilder
                                .parse(new InputSource(req.getInputStream()));
                        // Get the root element of the document
                        Element rootElement = document.getDocumentElement();

                        propNode = XMLHelper
                                .findSubElement(rootElement, "prop");
                        if (propNode != null) {
                            propertyFindType = FIND_BY_PROPERTY;
                        } else if (XMLHelper.findSubElement(rootElement,
                                "propname") != null) {
                            propertyFindType = FIND_PROPERTY_NAMES;
                        } else if (XMLHelper.findSubElement(rootElement,
                                "allprop") != null) {
                            propertyFindType = FIND_ALL_PROP;
                        }
                    } catch (Exception e) {
                        resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                        return;
                    }
                } else {
                    // no content, which means it is a allprop request
                    propertyFindType = FIND_ALL_PROP;
                }

                HashMap<String, String> namespaces = new HashMap<>();
                namespaces.put(NAMESPACE_APPLE_WEBDAVFS, "S");
                namespaces.put(NAMESPACE_OWNCLOUD, "oc");
                namespaces.put(NAMESPACE_NEXTCLOUD, "ns");
                namespaces.put("DAV:", "D");

                if (propertyFindType == FIND_BY_PROPERTY) {
                    propertyFindType = 0;
                    properties = XMLHelper.getPropertiesFromXML(propNode);
                }

                resp.setStatus(WebdavStatus.SC_MULTI_STATUS);
                resp.setContentType("text/xml; charset=UTF-8");

                // Create multistatus object
                XMLWriter generatedXML = new XMLWriter(new OutputStreamWriter(resp.getOutputStream()),
                        namespaces);
                generatedXML.writeXMLHeader();
                generatedXML
                        .writeElement("DAV::multistatus", XMLWriter.OPENING);
                if (_depth == 0) {
                    parseProperties(transaction, req, generatedXML, path,
                            propertyFindType, properties, _mimeTyper
                                    .getMimeType(transaction, path));
                } else {
                    recursiveParseProperties(transaction, path, req,
                            generatedXML, propertyFindType, properties, _depth);
                }
                generatedXML
                        .writeElement("DAV::multistatus", XMLWriter.CLOSING);

                generatedXML.sendData();
            } catch (AccessDeniedException e) {
                resp.sendError(WebdavStatus.SC_FORBIDDEN);
            } catch (WebdavException e) {
                LOG.warn("Sending internal error!", e);
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (JapException e) {
                LOG.warn("DoPropfind JapException", e);
                // To change body of catch statement use
                // File | Settings | File Templates.
            } finally {
                _resourceLocks.unlockTemporaryLockedObjects(transaction, path,
                        tempLockOwner);
            }
        } else {
            Hashtable<String, Integer> errorList = new Hashtable<>();
            errorList.put(path, WebdavStatus.SC_LOCKED);
            sendReport(req, resp, errorList);
        }
    }

    /**
     * goes recursive through all folders. used by propfind
     *
     * @param currentPath the current path
     * @param req         JapHttpRequest
     * @param depth       depth of the propfind
     * @throws IOException if an error in the underlying store occurs
     */
    private void recursiveParseProperties(ITransaction transaction,
                                          String currentPath, JapHttpRequest req, XMLWriter generatedXML,
                                          int propertyFindType, Vector<String> properties, int depth) throws WebdavException {

        String mimeType = _mimeTyper.getMimeType(transaction, currentPath);
        parseProperties(transaction, req, generatedXML, currentPath,
                propertyFindType, properties, mimeType);

        if (depth > 0) {
            // no need to get name if depth is already zero
            String[] names = _store.getChildrenNames(transaction, currentPath);
            names = names == null ? new String[]{} : names;
            String newPath = null;

            for (String name : names) {
                newPath = currentPath;
                if (!(newPath.endsWith("/"))) {
                    newPath += "/";
                }
                newPath += name;
                recursiveParseProperties(transaction, newPath, req,
                        generatedXML, propertyFindType, properties, depth - 1);
            }
        }
    }

    /**
     * Propfind helper method.
     *
     * @param req              The servlet request
     * @param generatedXML     XML response to the Propfind request
     * @param path             Path of the current resource
     * @param type             Propfind type
     * @param propertiesVector If the propfind type is find properties by name, then this Vector
     *                         contains those properties
     */
    private void parseProperties(ITransaction transaction,
                                 JapHttpRequest req, XMLWriter generatedXML, String path,
                                 int type, Vector<String> propertiesVector, String mimeType)
            throws WebdavException {

        StoredObject so = _store.getStoredObject(transaction, path);
        if (so == null) return;
        boolean isFolder = so.isFolder();
        String userAgent = transaction.getRequest().getHeader("User-Agent");
        final String creationDateString = creationDateFormat(userAgent, so.getCreationDate());
        final String lastModifiedDateString = lastModifiedDateFormat(userAgent, so.getLastModified());
        String sha1Sum = so.getSha1sum();
        String resourceLength = String.valueOf(so.getResourceLength());

        // ResourceInfo resourceInfo = new ResourceInfo(path, resources);

        generatedXML.writeElement("DAV::response", XMLWriter.OPENING);
        String status = new String("HTTP/1.1 " + WebdavStatus.SC_OK + " "
                + WebdavStatus.getStatusText(WebdavStatus.SC_OK));

        // Generating href element
        generatedXML.writeElement("DAV::href", XMLWriter.OPENING);

        String href = req.getContextPath();
        String servletPath = req.getServletPath();
        if (servletPath != null) {
            if ((href.endsWith("/")) && (servletPath.startsWith("/")))
                href += servletPath.substring(1);
            else
                href += servletPath;
        }
        if ((href.endsWith("/")) && (path.startsWith("/")))
            href += path.substring(1);
        else
            href += path;
        if ((isFolder) && (!href.endsWith("/")))
            href += "/";

        generatedXML.writeText(rewriteUrl(href));

        generatedXML.writeElement("DAV::href", XMLWriter.CLOSING);

        String resourceName = path;
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash != -1)
            resourceName = resourceName.substring(lastSlash + 1);

        switch (type) {

            case FIND_ALL_PROP:

                generatedXML.writeElement("DAV::propstat", XMLWriter.OPENING);
                generatedXML.writeElement("DAV::prop", XMLWriter.OPENING);

                generatedXML.writeProperty("DAV::creationdate", creationDateString);
                generatedXML.writeElement("DAV::displayname", XMLWriter.OPENING);
                generatedXML.writeData(resourceName);
                generatedXML.writeElement("DAV::displayname", XMLWriter.CLOSING);
                if (!isFolder) {
                    generatedXML.writeProperty("DAV::getlastmodified", lastModifiedDateString);
                    generatedXML.writeProperty("DAV::getcontentlength", resourceLength);
                    String contentType = mimeType;
                    if (contentType != null && !contentType.isEmpty()) {
                        generatedXML.writeProperty("DAV::getcontenttype", contentType);
                    }
                    generatedXML.writeProperty("DAV::getetag", getETag(so));
                    generatedXML.writeElement("DAV::resourcetype", XMLWriter.NO_CONTENT);
                    generatedXML.writeProperty("DAV::publiclink", String.valueOf(_store.getPublicLink(transaction, path)));
                    generatedXML.writeProperty("DAV::thumbnail", URL_ENCODER.encode(String.valueOf(so.getThumbnailUrl())));
                } else {
                    generatedXML.writeElement("DAV::resourcetype", XMLWriter.OPENING);
                    generatedXML.writeElement("DAV::collection", XMLWriter.NO_CONTENT);
                    generatedXML.writeElement("DAV::resourcetype", XMLWriter.CLOSING);
                    generatedXML.writeProperty("DAV::getlastmodified", lastModifiedDateString);
                }

                writeSupportedLockElements(transaction, generatedXML, path);
                writeLockDiscoveryElements(transaction, generatedXML, path);

                generatedXML.writeProperty("DAV::source", "");
                generatedXML.writeProperty(NAMESPACE_APPLE_WEBDAVFS + ":appledoubleheader", "AAUWBwACAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAACAAAAJgAAACwAAAAJAAAAMgAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==");

                generatedXML.writeElement(NAMESPACE_OWNCLOUD + ":checksums", XMLWriter.OPENING);
                if (sha1Sum != null && sha1Sum.length() > 0) {
                    generatedXML.writeProperty(NAMESPACE_OWNCLOUD + ":checksum", "sha1:" + sha1Sum.toLowerCase());
                }
                generatedXML.writeElement(NAMESPACE_OWNCLOUD + ":checksums", XMLWriter.CLOSING);
                if (sha1Sum != null && sha1Sum.length() > 0) {
                    generatedXML.writeProperty("ME::sha1hex", sha1Sum.toLowerCase());
                }
                generatedXML.writeElement("DAV::prop", XMLWriter.CLOSING);
                generatedXML.writeElement("DAV::status", XMLWriter.OPENING);
                generatedXML.writeText(status);
                generatedXML.writeElement("DAV::status", XMLWriter.CLOSING);
                generatedXML.writeElement("DAV::propstat", XMLWriter.CLOSING);
                break;
            case FIND_PROPERTY_NAMES:

                generatedXML.writeElement("DAV::propstat", XMLWriter.OPENING);
                generatedXML.writeElement("DAV::prop", XMLWriter.OPENING);

                generatedXML.writeElement(NAMESPACE_APPLE_WEBDAVFS + ":appledoubleheader", XMLWriter.NO_CONTENT);
                generatedXML.writeElement(NAMESPACE_OWNCLOUD + ":checksums", XMLWriter.NO_CONTENT);
                generatedXML.writeElement(NAMESPACE_NEXTCLOUD + ":checksums", XMLWriter.NO_CONTENT);
                generatedXML.writeElement("ME:sha1hex", XMLWriter.NO_CONTENT);
                generatedXML.writeElement("DAV::quota", XMLWriter.NO_CONTENT);
                generatedXML.writeElement("DAV::quotaused", XMLWriter.NO_CONTENT);
                generatedXML.writeElement("DAV::quota-available-bytes", XMLWriter.NO_CONTENT);
                generatedXML.writeElement("DAV::quota-used-bytes", XMLWriter.NO_CONTENT);
                generatedXML.writeElement("DAV::quota-used-bytes", XMLWriter.NO_CONTENT);
                generatedXML.writeElement("DAV::creationdate", XMLWriter.NO_CONTENT);
                generatedXML.writeElement("DAV::displayname", XMLWriter.NO_CONTENT);
                if (!isFolder) {
                    generatedXML.writeElement("DAV::getcontentlanguage",
                            XMLWriter.NO_CONTENT);
                    generatedXML.writeElement("DAV::getcontentlength",
                            XMLWriter.NO_CONTENT);
                    generatedXML.writeElement("DAV::getcontenttype",
                            XMLWriter.NO_CONTENT);
                    generatedXML.writeElement("DAV::getetag", XMLWriter.NO_CONTENT);
                    generatedXML.writeElement("DAV::getlastmodified",
                            XMLWriter.NO_CONTENT);
                    generatedXML.writeElement("DAV::publiclink",
                            XMLWriter.NO_CONTENT);
                    generatedXML.writeElement("DAV::thumbnail",
                            XMLWriter.NO_CONTENT);

                }
                generatedXML.writeElement("DAV::resourcetype", XMLWriter.NO_CONTENT);
                generatedXML.writeElement("DAV::supportedlock", XMLWriter.NO_CONTENT);
                generatedXML.writeElement("DAV::source", XMLWriter.NO_CONTENT);

                generatedXML.writeElement("DAV::prop", XMLWriter.CLOSING);
                generatedXML.writeElement("DAV::status", XMLWriter.OPENING);
                generatedXML.writeText(status);
                generatedXML.writeElement("DAV::status", XMLWriter.CLOSING);
                generatedXML.writeElement("DAV::propstat", XMLWriter.CLOSING);

                break;
            case FIND_BY_PROPERTY:

                Vector<String> propertiesNotFound = new Vector<>();

                // Parse the list of properties

                generatedXML.writeElement("DAV::propstat", XMLWriter.OPENING);
                generatedXML.writeElement("DAV::prop", XMLWriter.OPENING);

                Enumeration<String> properties = propertiesVector.elements();

                while (properties.hasMoreElements()) {

                    String property = (String) properties.nextElement();
                    switch (property) {
                        case "DAV::creationdate":
                            generatedXML.writeProperty("DAV::creationdate", creationDateString);
                            break;
                        case "DAV::displayname":
                            generatedXML.writeElement("DAV::displayname", XMLWriter.OPENING);
                            generatedXML.writeData(resourceName);
                            generatedXML.writeElement("DAV::displayname", XMLWriter.CLOSING);
                            break;
                        case "DAV::getcontentlanguage":
                            if (isFolder) {
                                propertiesNotFound.addElement(property);
                            } else {
                                generatedXML.writeElement("DAV::getcontentlanguage", XMLWriter.NO_CONTENT);
                            }
                            break;
                        case "DAV::getcontentlength":
                            if (isFolder) {
                                propertiesNotFound.addElement(property);
                            } else {
                                generatedXML.writeProperty("DAV::getcontentlength",
                                        resourceLength);
                            }
                            break;
                        case "DAV::getcontenttype":
                            if (isFolder) {
                                propertiesNotFound.addElement(property);
                            } else {
                                generatedXML.writeProperty("DAV::getcontenttype",
                                        mimeType);
                            }
                            break;
                        case "DAV::getetag":
                            if (isFolder || so.isNullResource()) {
                                propertiesNotFound.addElement(property);
                            } else {
                                generatedXML.writeProperty("DAV::getetag", getETag(so));
                            }
                            break;
                        case "DAV::getlastmodified":
                            if (isFolder) {
                                propertiesNotFound.addElement(property);
                            } else {
                                generatedXML.writeProperty("DAV::getlastmodified", lastModifiedDateString);
                            }
                            break;
                        case "DAV::resourcetype":
                            if (isFolder) {
                                generatedXML.writeElement("DAV::resourcetype", XMLWriter.OPENING);
                                generatedXML.writeElement("DAV::collection", XMLWriter.NO_CONTENT);
                                generatedXML.writeElement("DAV::resourcetype", XMLWriter.CLOSING);
                            } else {
                                generatedXML.writeElement("DAV::resourcetype", XMLWriter.NO_CONTENT);
                            }
                            break;
                        case "DAV::source":
                            generatedXML.writeProperty("DAV::source", "");
                            break;
                        case "DAV::supportedlock":
                            writeSupportedLockElements(transaction, generatedXML, path);
                            break;
                        case "DAV::lockdiscovery":
                            writeLockDiscoveryElements(transaction, generatedXML, path);
                            break;
                        case "DAV::quota":
                        case "DAV::quota-available-bytes":
                            long availableBytes = _store.getQuotaAvailableBytes(transaction);
                            if (availableBytes >= 0) {
                                generatedXML.writeProperty(property, String.valueOf(availableBytes));
                            }
                            break;
                        case "DAV::quota-used-bytes":
                        case "DAV::quotaused":
                            long usedBytes = _store.getQuotaUsedBytes(transaction);
                            if (usedBytes >= 0) {
                                generatedXML.writeProperty(property, String.valueOf(usedBytes));
                            }
                            break;
                        case "DAV::publiclink":
                            generatedXML.writeProperty(property, String.valueOf(_store.getPublicLink(transaction, path)));
                            break;
                        case "DAV::thumbnail":
                            generatedXML.writeProperty(property, URL_ENCODER.encode(String.valueOf(so.getThumbnailUrl())));
                            break;
                        case NAMESPACE_APPLE_WEBDAVFS + ":appledoubleheader":
                            generatedXML.writeProperty(property, "AAUWBwACAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAACAAAAJgAAACwAAAAJAAAAMgAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==");
                            break;
                        case NAMESPACE_OWNCLOUD + ":checksums":
                        case NAMESPACE_NEXTCLOUD + ":checksums":
                            generatedXML.writeElement(NAMESPACE_OWNCLOUD + ":checksums", XMLWriter.OPENING);
                            if (sha1Sum != null && sha1Sum.length() > 0) {
                                generatedXML.writeProperty(NAMESPACE_OWNCLOUD + ":checksum", "sha1:" + sha1Sum.toLowerCase());
                            }
                            generatedXML.writeElement(NAMESPACE_OWNCLOUD + ":checksums", XMLWriter.CLOSING);
                            break;
                        default:
                            propertiesNotFound.addElement(property);
                            break;
                    }

                }

                generatedXML.writeElement("DAV::prop", XMLWriter.CLOSING);
                generatedXML.writeElement("DAV::status", XMLWriter.OPENING);
                generatedXML.writeText(status);
                generatedXML.writeElement("DAV::status", XMLWriter.CLOSING);
                generatedXML.writeElement("DAV::propstat", XMLWriter.CLOSING);

                Enumeration<String> propertiesNotFoundList = propertiesNotFound.elements();

                if (propertiesNotFoundList.hasMoreElements()) {
                    status = new String("HTTP/1.1 " + WebdavStatus.SC_NOT_FOUND
                            + " "
                            + WebdavStatus.getStatusText(WebdavStatus.SC_NOT_FOUND));

                    generatedXML.writeElement("DAV::propstat", XMLWriter.OPENING);
                    generatedXML.writeElement("DAV::prop", XMLWriter.OPENING);

                    while (propertiesNotFoundList.hasMoreElements()) {
                        generatedXML.writeElement((String) propertiesNotFoundList
                                .nextElement(), XMLWriter.NO_CONTENT);
                    }

                    generatedXML.writeElement("DAV::prop", XMLWriter.CLOSING);
                    generatedXML.writeElement("DAV::status", XMLWriter.OPENING);
                    generatedXML.writeText(status);
                    generatedXML.writeElement("DAV::status", XMLWriter.CLOSING);
                    generatedXML.writeElement("DAV::propstat", XMLWriter.CLOSING);
                }
                break;
        }
        generatedXML.writeElement("DAV::response", XMLWriter.CLOSING);
        so = null;
    }

    private void writeSupportedLockElements(ITransaction transaction,
                                            XMLWriter generatedXML, String path) {

        LockedObject lo = _resourceLocks.getLockedObjectByPath(transaction,
                path);

        generatedXML.writeElement("DAV::supportedlock", XMLWriter.OPENING);

        if (lo == null) {
            // both locks (shared/exclusive) can be granted
            generatedXML.writeElement("DAV::lockentry", XMLWriter.OPENING);

            generatedXML.writeElement("DAV::lockscope", XMLWriter.OPENING);
            generatedXML.writeElement("DAV::exclusive", XMLWriter.NO_CONTENT);
            generatedXML.writeElement("DAV::lockscope", XMLWriter.CLOSING);

            generatedXML.writeElement("DAV::locktype", XMLWriter.OPENING);
            generatedXML.writeElement("DAV::write", XMLWriter.NO_CONTENT);
            generatedXML.writeElement("DAV::locktype", XMLWriter.CLOSING);

            generatedXML.writeElement("DAV::lockentry", XMLWriter.CLOSING);

            generatedXML.writeElement("DAV::lockentry", XMLWriter.OPENING);

            generatedXML.writeElement("DAV::lockscope", XMLWriter.OPENING);
            generatedXML.writeElement("DAV::shared", XMLWriter.NO_CONTENT);
            generatedXML.writeElement("DAV::lockscope", XMLWriter.CLOSING);

            generatedXML.writeElement("DAV::locktype", XMLWriter.OPENING);
            generatedXML.writeElement("DAV::write", XMLWriter.NO_CONTENT);
            generatedXML.writeElement("DAV::locktype", XMLWriter.CLOSING);

            generatedXML.writeElement("DAV::lockentry", XMLWriter.CLOSING);

        } else {
            // LockObject exists, checking lock state
            // if an exclusive lock exists, no further lock is possible
            if (lo.isShared()) {

                generatedXML.writeElement("DAV::lockentry", XMLWriter.OPENING);

                generatedXML.writeElement("DAV::lockscope", XMLWriter.OPENING);
                generatedXML.writeElement("DAV::shared", XMLWriter.NO_CONTENT);
                generatedXML.writeElement("DAV::lockscope", XMLWriter.CLOSING);

                generatedXML.writeElement("DAV::locktype", XMLWriter.OPENING);
                generatedXML.writeElement("DAV::" + lo.getType(), XMLWriter.NO_CONTENT);
                generatedXML.writeElement("DAV::locktype", XMLWriter.CLOSING);

                generatedXML.writeElement("DAV::lockentry", XMLWriter.CLOSING);
            }
        }

        generatedXML.writeElement("DAV::supportedlock", XMLWriter.CLOSING);

        lo = null;
    }

    private void writeLockDiscoveryElements(ITransaction transaction,
                                            XMLWriter generatedXML, String path) {

        LockedObject lo = _resourceLocks.getLockedObjectByPath(transaction,
                path);

        if (lo != null && !lo.hasExpired()) {

            generatedXML.writeElement("DAV::lockdiscovery", XMLWriter.OPENING);
            generatedXML.writeElement("DAV::activelock", XMLWriter.OPENING);

            generatedXML.writeElement("DAV::locktype", XMLWriter.OPENING);
            generatedXML.writeProperty("DAV::" + lo.getType());
            generatedXML.writeElement("DAV::locktype", XMLWriter.CLOSING);

            generatedXML.writeElement("DAV::lockscope", XMLWriter.OPENING);
            if (lo.isExclusive()) {
                generatedXML.writeProperty("DAV::exclusive");
            } else {
                generatedXML.writeProperty("DAV::shared");
            }
            generatedXML.writeElement("DAV::lockscope", XMLWriter.CLOSING);

            generatedXML.writeElement("DAV::depth", XMLWriter.OPENING);
            if (_depth == INFINITY) {
                generatedXML.writeText("Infinity");
            } else {
                generatedXML.writeText(String.valueOf(_depth));
            }
            generatedXML.writeElement("DAV::depth", XMLWriter.CLOSING);

            String[] owners = lo.getOwner();
            if (owners != null) {
                for (int i = 0; i < owners.length; i++) {
                    generatedXML.writeElement("DAV::owner", XMLWriter.OPENING);
                    generatedXML.writeElement("DAV::href", XMLWriter.OPENING);
                    generatedXML.writeText(owners[i]);
                    generatedXML.writeElement("DAV::href", XMLWriter.CLOSING);
                    generatedXML.writeElement("DAV::owner", XMLWriter.CLOSING);
                }
            } else {
                generatedXML.writeElement("DAV::owner", XMLWriter.NO_CONTENT);
            }

            int timeout = (int) (lo.getTimeoutMillis() / 1000);
            String timeoutStr = Integer.toString(timeout);
            generatedXML.writeElement("DAV::timeout", XMLWriter.OPENING);
            generatedXML.writeText("Second-" + timeoutStr);
            generatedXML.writeElement("DAV::timeout", XMLWriter.CLOSING);

            String lockToken = lo.getID();

            generatedXML.writeElement("DAV::locktoken", XMLWriter.OPENING);
            generatedXML.writeElement("DAV::href", XMLWriter.OPENING);
            generatedXML.writeText("opaquelocktoken:" + lockToken);
            generatedXML.writeElement("DAV::href", XMLWriter.CLOSING);
            generatedXML.writeElement("DAV::locktoken", XMLWriter.CLOSING);

            generatedXML.writeElement("DAV::activelock", XMLWriter.CLOSING);
            generatedXML.writeElement("DAV::lockdiscovery", XMLWriter.CLOSING);

        } else {
            generatedXML.writeElement("DAV::lockdiscovery",
                    XMLWriter.NO_CONTENT);
        }

        lo = null;
    }

}
