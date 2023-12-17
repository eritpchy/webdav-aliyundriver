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

import com.fujieid.jap.http.JapHttpRequest;
import com.fujieid.jap.http.JapHttpResponse;
import net.sf.webdav.ITransaction;
import net.sf.webdav.IWebdavStore;
import net.sf.webdav.StoredObject;
import net.sf.webdav.WebdavStatus;
import net.sf.webdav.exceptions.*;
import net.sf.webdav.locking.ResourceLocks;

import java.io.IOException;
import java.util.Hashtable;

public class DoCopy extends AbstractMethod {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoCopy.class);

    private IWebdavStore _store;
    private ResourceLocks _resourceLocks;
    private DoDelete _doDelete;
    private boolean _readOnly;

    public DoCopy(IWebdavStore store, ResourceLocks resourceLocks,
            DoDelete doDelete, boolean readOnly) {
        _store = store;
        _resourceLocks = resourceLocks;
        _doDelete = doDelete;
        _readOnly = readOnly;
    }

    public void execute(ITransaction transaction, JapHttpRequest req,
            JapHttpResponse resp) throws IOException, LockFailedException {
        LOG.trace("-- " + this.getClass().getName());

        String path = getRelativePath(req);
        if (!_readOnly) {

            String tempLockOwner = "doCopy" + System.currentTimeMillis() + String.valueOf(req);
            if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0,
                    TEMP_TIMEOUT, TEMPORARY)) {
                try {
                    if (!copyResource(transaction, req, resp))
                        return;
                } catch (AccessDeniedException e) {
                    resp.sendError(WebdavStatus.SC_FORBIDDEN);
                } catch (ObjectAlreadyExistsException e) {
                    resp.sendError(WebdavStatus.SC_CONFLICT, req
                            .getRequestURI());
                } catch (ObjectNotFoundException e) {
                    resp.sendError(WebdavStatus.SC_NOT_FOUND, req
                            .getRequestURI());
                } catch (WebdavException e) {
                    LOG.error(e.getMessage());
                    resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                } finally {
                    _resourceLocks.unlockTemporaryLockedObjects(transaction,
                            path, tempLockOwner);
                }
            } else {
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }

        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        }

    }

    /**
     * Copy a resource.
     *
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param req
     *      Servlet request
     * @param resp
     *      Servlet response
     * @return true if the copy is successful
     * @throws WebdavException
     *      if an error in the underlying store occurs
     * @throws IOException
     *      when an error occurs while sending the response
     */
    public boolean copyResource(ITransaction transaction,
            JapHttpRequest req, JapHttpResponse resp)
            throws WebdavException, IOException, LockFailedException {

        // Parsing destination header
        String destinationPath = parseDestinationHeader(req, resp);

        if (destinationPath == null)
            return false;

        String path = getRelativePath(req);

        if (path.equals(destinationPath)) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
            return false;
        }

        Hashtable<String, Integer> errorList = new Hashtable<>();
        String parentDestinationPath = getParentPath(getCleanPath(destinationPath));

        if (!checkLocks(transaction, req, resp, _resourceLocks,
                parentDestinationPath)) {
            resp.setStatus(WebdavStatus.SC_LOCKED);
            return false; // parentDestination is locked
        }

        if (!checkLocks(transaction, req, resp, _resourceLocks, destinationPath)) {
            resp.setStatus(WebdavStatus.SC_LOCKED);
            return false; // destination is locked
        }

        // Parsing overwrite header

        boolean overwrite = true;
        String overwriteHeader = req.getHeader("Overwrite");

        if (overwriteHeader != null) {
            overwrite = overwriteHeader.equalsIgnoreCase("T");
        }

        // Overwriting the destination
        String lockOwner = "copyResource" + System.currentTimeMillis() + String.valueOf(req);

        if (_resourceLocks.lock(transaction, destinationPath, lockOwner, false,
                0, TEMP_TIMEOUT, TEMPORARY)) {
            StoredObject copySo, destinationSo = null;
            try {
                copySo = _store.getStoredObject(transaction, path);
                // Retrieve the resources
                if (copySo == null) {
                    resp.sendError(JapHttpResponse.SC_NOT_FOUND);
                    return false;
                }

                if (copySo.isNullResource()) {
                    String methodsAllowed = DeterminableMethod
                            .determineMethodsAllowed(copySo);
                    resp.addHeader("Allow", methodsAllowed);
                    resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
                    return false;
                }

                errorList = new Hashtable<>();

                destinationSo = _store.getStoredObject(transaction,
                        destinationPath);

                if (overwrite) {

                    // Delete destination resource, if it exists
                    if (destinationSo != null) {
                        _doDelete.deleteResource(transaction, destinationPath,
                                errorList, req, resp);

                    } else {
                        resp.setStatus(WebdavStatus.SC_CREATED);
                    }
                } else {

                    // If the destination exists, then it's a conflict
                    if (destinationSo != null) {
                        resp.sendError(WebdavStatus.SC_PRECONDITION_FAILED);
                        return false;
                    } else {
                        resp.setStatus(WebdavStatus.SC_CREATED);
                    }

                }
                copy(transaction, path, destinationPath, errorList, req, resp);

                if (!errorList.isEmpty()) {
                    sendReport(req, resp, errorList);
                }

            } finally {
                _resourceLocks.unlockTemporaryLockedObjects(transaction,
                        destinationPath, lockOwner);
            }
        } else {
            resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            return false;
        }
        return true;

    }

    /**
     * copies the specified resource(s) to the specified destination.
     * preconditions must be handled by the caller. Standard status codes must
     * be handled by the caller. a multi status report in case of errors is
     * created here.
     *
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param sourcePath
     *      path from where to read
     * @param destinationPath
     *      path where to write
     * @param req
     *      JapHttpRequest
     * @param resp
     *      JapHttpResponse
     * @throws WebdavException
     *      if an error in the underlying store occurs
     */
    private void copy(ITransaction transaction, String sourcePath,
            String destinationPath, Hashtable<String, Integer> errorList,
            JapHttpRequest req, JapHttpResponse resp)
            throws WebdavException, IOException {

        StoredObject sourceSo = _store.getStoredObject(transaction, sourcePath);
        if (sourceSo.isResource()) {
            _store.createResource(transaction, destinationPath);
            long resourceLength = _store.setResourceContent(transaction,
                    destinationPath, _store.getResourceContent(transaction,
                            sourcePath), null, null);

            if (resourceLength != -1) {
                StoredObject destinationSo = _store.getStoredObject(
                        transaction, destinationPath);
                destinationSo.setResourceLength(resourceLength);
            }

        } else {

            if (sourceSo.isFolder()) {
                copyFolder(transaction, sourcePath, destinationPath, errorList,
                        req, resp);
            } else {
                resp.sendError(WebdavStatus.SC_NOT_FOUND);
            }
        }
    }

    /**
     * helper method of copy() recursively copies the FOLDER at source path to
     * destination path
     *
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param sourcePath
     *      where to read
     * @param destinationPath
     *      where to write
     * @param errorList
     *      all errors that ocurred
     * @param req
     *      JapHttpRequest
     * @param resp
     *      JapHttpResponse
     * @throws WebdavException
     *      if an error in the underlying store occurs
     */
    private void copyFolder(ITransaction transaction, String sourcePath,
            String destinationPath, Hashtable<String, Integer> errorList,
            JapHttpRequest req, JapHttpResponse resp)
            throws WebdavException {

        _store.createFolder(transaction, destinationPath);
        boolean infiniteDepth = true;
        String depth = req.getHeader("Depth");
        if (depth != null) {
            if (depth.equals("0")) {
                infiniteDepth = false;
            }
        }
        if (infiniteDepth) {
            String[] children = _store
                    .getChildrenNames(transaction, sourcePath);
            children = children == null ? new String[] {} : children;

            StoredObject childSo;
            for (int i = children.length - 1; i >= 0; i--) {
                children[i] = "/" + children[i];
                try {
                    childSo = _store.getStoredObject(transaction,
                            (sourcePath + children[i]));
                    if (childSo.isResource()) {
                        _store.createResource(transaction, destinationPath
                                + children[i]);
                        long resourceLength = _store.setResourceContent(
                                transaction, destinationPath + children[i],
                                _store.getResourceContent(transaction,
                                        sourcePath + children[i]), null, null);

                        if (resourceLength != -1) {
                            StoredObject destinationSo = _store
                                    .getStoredObject(transaction,
                                            destinationPath + children[i]);
                            destinationSo.setResourceLength(resourceLength);
                        }

                    } else {
                        copyFolder(transaction, sourcePath + children[i],
                                destinationPath + children[i], errorList, req,
                                resp);
                    }
                } catch (AccessDeniedException e) {
                    errorList.put(destinationPath + children[i], Integer.valueOf(WebdavStatus.SC_FORBIDDEN));
                } catch (ObjectNotFoundException e) {
                    errorList.put(destinationPath + children[i], Integer.valueOf(WebdavStatus.SC_NOT_FOUND));
                } catch (ObjectAlreadyExistsException e) {
                    errorList.put(destinationPath + children[i], Integer.valueOf(WebdavStatus.SC_CONFLICT));
                } catch (WebdavException e) {
                    errorList.put(destinationPath + children[i], Integer.valueOf(WebdavStatus.SC_INTERNAL_SERVER_ERROR));
                }
            }
        }
    }


}
