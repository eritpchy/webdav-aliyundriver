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

public class DoDelete extends AbstractMethod {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoDelete.class);

    private IWebdavStore _store;
    private ResourceLocks _resourceLocks;
    private boolean _readOnly;

    private static final boolean RECURSE_DELETE = false;

    public DoDelete(IWebdavStore store, ResourceLocks resourceLocks,
            boolean readOnly) {
        _store = store;
        _resourceLocks = resourceLocks;
        _readOnly = readOnly;
    }

    public void execute(ITransaction transaction, JapHttpRequest req,
            JapHttpResponse resp) throws IOException, LockFailedException {
        LOG.trace("-- " + this.getClass().getName());

        if (!_readOnly) {
            String path = getRelativePath(req);
            String parentPath = getParentPath(getCleanPath(path));

            Hashtable<String, Integer> errorList = new Hashtable<>();

            if (!checkLocks(transaction, req, resp, _resourceLocks, parentPath)) {
                resp.setStatus(WebdavStatus.SC_LOCKED);
                return; // parent is locked
            }

            if (!checkLocks(transaction, req, resp, _resourceLocks, path)) {
                resp.setStatus(WebdavStatus.SC_LOCKED);
                return; // resource is locked
            }

            String tempLockOwner = "doDelete" + System.currentTimeMillis() + String.valueOf(req);
            if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0,
                    TEMP_TIMEOUT, TEMPORARY)) {
                try {
                    errorList = new Hashtable<>();
                    deleteResource(transaction, path, errorList, req, resp);
                    if (!errorList.isEmpty()) {
                        sendReport(req, resp, errorList);
                    }
                } catch (AccessDeniedException e) {
                    resp.sendError(WebdavStatus.SC_FORBIDDEN);
                } catch (ObjectAlreadyExistsException e) {
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
     * deletes the recources at "path"
     *
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param path
     *      the folder to be deleted
     * @param errorList
     *      all errors that ocurred
     * @param req
     *      JapHttpRequest
     * @param resp
     *      JapHttpResponse
     * @throws WebdavException
     *      if an error in the underlying store occurs
     * @throws IOException
     *      when an error occurs while sending the response
     */
    public void deleteResource(ITransaction transaction, String path,
            Hashtable<String, Integer> errorList, JapHttpRequest req,
            JapHttpResponse resp) throws IOException, WebdavException {

        resp.setStatus(WebdavStatus.SC_NO_CONTENT);

        if (_readOnly) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
            return;
        }
        StoredObject so = _store.getStoredObject(transaction, path);
        if (so == null) {
            //已删除
            return;
        }
        if (!RECURSE_DELETE) {
            _store.removeObject(transaction, path);
        } else {
            if (so.isResource()) {
                _store.removeObject(transaction, path);
            } else {
                if (so.isFolder()) {
                    deleteFolder(transaction, path, errorList, req, resp);
                    _store.removeObject(transaction, path);
                } else {
                    resp.sendError(WebdavStatus.SC_NOT_FOUND);
                }
            }
        }
    }

    /**
     *
     * helper method of deleteResource() deletes the folder and all of its
     * contents
     *
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param path
     *      the folder to be deleted
     * @param errorList
     *      all errors that ocurred
     * @param req
     *      JapHttpRequest
     * @param resp
     *      JapHttpResponse
     * @throws WebdavException
     *      if an error in the underlying store occurs
     */
    private void deleteFolder(ITransaction transaction, String path,
            Hashtable<String, Integer> errorList, JapHttpRequest req,
            JapHttpResponse resp) throws WebdavException {

        String[] children = _store.getChildrenNames(transaction, path);
        children = children == null ? new String[] {} : children;
        StoredObject so = null;
        for (int i = children.length - 1; i >= 0; i--) {
            children[i] = "/" + children[i];
            try {
                so = _store.getStoredObject(transaction, path + children[i]);
                if (so.isResource()) {
                    _store.removeObject(transaction, path + children[i]);

                } else {
                    deleteFolder(transaction, path + children[i], errorList,
                            req, resp);

                    _store.removeObject(transaction, path + children[i]);

                }
            } catch (AccessDeniedException e) {
                // errorList.put(path + children[i], new Integer(WebdavStatus.SC_FORBIDDEN));
                errorList.put(path + children[i], Integer.valueOf(WebdavStatus.SC_FORBIDDEN));
            } catch (ObjectNotFoundException e) {
                // errorList.put(path + children[i], new Integer(WebdavStatus.SC_NOT_FOUND));
                errorList.put(path + children[i], Integer.valueOf(WebdavStatus.SC_NOT_FOUND));
            } catch (WebdavException e) {
                // errorList.put(path + children[i], new Integer(WebdavStatus.SC_INTERNAL_SERVER_ERROR));
                errorList.put(path + children[i], Integer.valueOf(WebdavStatus.SC_INTERNAL_SERVER_ERROR));
            }
        }
        so = null;
    }
}
