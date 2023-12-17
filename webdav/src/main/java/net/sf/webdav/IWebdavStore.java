/*
 * $Header: /Users/ak/temp/cvs2svn/webdav-servlet/src/main/java/net/sf/webdav/IWebdavStore.java,v 1.1 2008-08-05 07:38:42 bauhardt Exp $
 * $Revision: 1.1 $
 * $Date: 2008-08-05 07:38:42 $
 *
 * ====================================================================
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.sf.webdav;

import com.fujieid.jap.http.JapHttpRequest;
import com.fujieid.jap.http.JapHttpResponse;
import net.sf.webdav.exceptions.WebdavException;

import java.io.InputStream;
import java.security.Principal;

/**
 * Interface for simple implementation of any store for the WebdavServlet
 * <p>
 * based on the BasicWebdavStore from Oliver Zeigermann, that was part of the
 * Webdav Construcktion Kit from slide
 * 
 */
public interface IWebdavStore {

    /**
     * Life cycle method, called by WebdavServlet's destroy() method. Should be used to clean up resources.
     */
    void destroy();

    /**
     * Indicates that a new request or transaction with this store involved has
     * been started. The request will be terminated by either {@link #commit()}
     * or {@link #rollback()}. If only non-read methods have been called, the
     * request will be terminated by a {@link #commit()}. This method will be
     * called by (@link WebdavStoreAdapter} at the beginning of each request.
     *
     *
     * @param principal
     *      the principal that started this request or <code>null</code> if
     *      there is non available
     *
     */
    ITransaction begin(Principal principal, JapHttpRequest req, JapHttpResponse resp);

    /**
     * Checks if authentication information passed in is valid. If not throws an
     * exception.
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     */
    void checkAuthentication(ITransaction transaction);

    /**
     * Indicates that all changes done inside this request shall be made
     * permanent and any transactions, connections and other temporary resources
     * shall be terminated.
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * 
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    void commit(ITransaction transaction);

    /**
     * Indicates that all changes done inside this request shall be undone and
     * any transactions, connections and other temporary resources shall be
     * terminated.
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * 
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    void rollback(ITransaction transaction);

    /**
     * Creates a folder at the position specified by <code>folderUri</code>.
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param folderUri
     *      URI of the folder
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    void createFolder(ITransaction transaction, String folderUri);

    /**
     * Creates a content resource at the position specified by
     * <code>resourceUri</code>.
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param resourceUri
     *      URI of the content resource
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    void createResource(ITransaction transaction, String resourceUri);

    /**
     * Gets the content of the resource specified by <code>resourceUri</code>.
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param resourceUri
     *      URI of the content resource
     * @return input stream you can read the content of the resource from
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    InputStream getResourceContent(ITransaction transaction, String resourceUri);

    /**
     * Sets / stores the content of the resource specified by
     * <code>resourceUri</code>.
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param resourceUri
     *      URI of the resource where the content will be stored
     * @param content
     *      input stream from which the content will be read from
     * @param contentType
     *      content type of the resource or <code>null</code> if unknown
     * @param characterEncoding
     *      character encoding of the resource or <code>null</code> if unknown
     *      or not applicable
     * @return lenght of resource
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    long setResourceContent(ITransaction transaction, String resourceUri,
            InputStream content, String contentType, String characterEncoding);

    /**
     * Gets the names of the children of the folder specified by
     * <code>folderUri</code>.
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param folderUri
     *      URI of the folder
     * @return a (possibly empty) list of children, or <code>null</code> if the
     *  uri points to a file
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    String[] getChildrenNames(ITransaction transaction, String folderUri);

    /**
     * Gets the length of the content resource specified by
     * <code>resourceUri</code>.
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param resourceUri
     *      URI of the content resource
     * @return length of the resource in bytes, <code>-1</code> declares this
     *  value as invalid and asks the adapter to try to set it from the
     *  properties if possible
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    long getResourceLength(ITransaction transaction, String path);

    /**
     * Removes the object specified by <code>uri</code>.
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param uri
     *      URI of the object, i.e. content resource or folder
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    void removeObject(ITransaction transaction, String uri);

    boolean moveObject(ITransaction transaction, String destinationPath, String sourcePath);

    /**
     * Gets the storedObject specified by <code>uri</code>
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param uri
     *      URI
     * @return StoredObject
     */
    StoredObject getStoredObject(ITransaction transaction, String uri);

    long getQuotaAvailableBytes(ITransaction transaction);
    long getQuotaUsedBytes(ITransaction transaction);

    String getFooter(ITransaction transaction);

    String getResourceDownloadUrlForRedirection(ITransaction transaction, String path);

    String getPublicLink(ITransaction transaction, String path);

    boolean handleCustomGetRequest(ITransaction transaction, String path);
}
