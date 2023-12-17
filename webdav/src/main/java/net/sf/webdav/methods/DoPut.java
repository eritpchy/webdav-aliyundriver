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
import net.sf.webdav.exceptions.AccessDeniedException;
import net.sf.webdav.exceptions.ChecksumNotMatchException;
import net.sf.webdav.exceptions.LockFailedException;
import net.sf.webdav.exceptions.WebdavException;
import net.sf.webdav.locking.IResourceLocks;
import net.sf.webdav.locking.LockedObject;
import net.sf.webdav.util.ClientIdentifyUtils;

import java.io.IOException;
import java.util.Hashtable;

public class DoPut extends AbstractMethod {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoPut.class);

    private IWebdavStore _store;
    private IResourceLocks _resourceLocks;
    private boolean _readOnly;
    private boolean _lazyFolderCreationOnPut;

    private String _userAgent;

    public DoPut(IWebdavStore store, IResourceLocks resLocks, boolean readOnly,
                 boolean lazyFolderCreationOnPut) {
        _store = store;
        _resourceLocks = resLocks;
        _readOnly = readOnly;
        _lazyFolderCreationOnPut = lazyFolderCreationOnPut;
    }

    public void execute(ITransaction transaction, JapHttpRequest req,
                        JapHttpResponse resp) throws IOException, LockFailedException {
        LOG.trace("-- " + this.getClass().getName());

        if (!_readOnly) {
            String path = getRelativePath(req);
            String name = req.getParameter("name");
            if (name != null && name.length() > 0) {
                path = path + "/" + name;
            }
            String parentPath = getParentPath(path);

            _userAgent = req.getHeader("User-Agent");

            if (ClientIdentifyUtils.isOSXFinder(_userAgent) && req.getContentLength() == 0) {
                // OS X Finder sends 2 PUTs; first has 0 content, second has content.
                // This is the first one, so we'll ignore it ...
                LOG.trace("-- First of multiple OS-X Finder PUT calls at {0}", path);
            }

            Hashtable<String, Integer> errorList = new Hashtable<>();
            if (ClientIdentifyUtils.isOSXFinder(_userAgent)) {
                // OS X Finder sends 2 PUTs; first has 0 content, second has content.
                // This is the second one that was preceded by a LOCK, so don't need to check the locks ...
            } else {
                if (!checkLocks(transaction, req, resp, _resourceLocks, parentPath)) {
                    resp.setStatus(WebdavStatus.SC_LOCKED);
                    return; // parent is locked
                }

                if (!checkLocks(transaction, req, resp, _resourceLocks, path)) {
                    resp.setStatus(WebdavStatus.SC_LOCKED);
                    return; // resource is locked
                }
            }

            String tempLockOwner = "doPut" + System.currentTimeMillis() + String.valueOf(req);
            if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0,
                    TEMP_TIMEOUT, TEMPORARY)) {
                StoredObject parentSo, so = null;
                try {
                    parentSo = _store.getStoredObject(transaction, parentPath);
                    if (parentPath != null && parentSo != null
                            && parentSo.isResource()) {
                        resp.sendError(WebdavStatus.SC_FORBIDDEN);
                        return;

                    } else if (parentPath != null && parentSo == null
                            && _lazyFolderCreationOnPut) {
                        _store.createFolder(transaction, parentPath);

                    } else if (parentPath != null && parentSo == null
                            && !_lazyFolderCreationOnPut) {
                        errorList.put(parentPath, WebdavStatus.SC_NOT_FOUND);
                        sendReport(req, resp, errorList);
                        return;
                    }

                    so = _store.getStoredObject(transaction, path);

                    if (so == null) {
                        _store.createResource(transaction, path);
                        // resp.setStatus(WebdavStatus.SC_CREATED);
                    } else {
                        // This has already been created, just update the data
                        if (so.isNullResource()) {

                            LockedObject nullResourceLo = _resourceLocks
                                    .getLockedObjectByPath(transaction, path);
                            if (nullResourceLo == null) {
                                resp
                                        .sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                                return;
                            }
                            String nullResourceLockToken = nullResourceLo
                                    .getID();
                            String[] lockTokens = getLockIdFromIfHeader(req);
                            String lockToken = null;
                            if (lockTokens != null) {
                                lockToken = lockTokens[0];
                            } else {
                                resp.sendError(WebdavStatus.SC_BAD_REQUEST);
                                return;
                            }
                            if (lockToken.equals(nullResourceLockToken)) {
                                so.setNullResource(false);
                                so.setFolder(false);

                                String[] nullResourceLockOwners = nullResourceLo
                                        .getOwner();
                                String owner = null;
                                if (nullResourceLockOwners != null)
                                    owner = nullResourceLockOwners[0];

                                if (!_resourceLocks.unlock(transaction,
                                        lockToken, owner)) {
                                    resp
                                            .sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                                }
                            } else {
                                errorList.put(path, WebdavStatus.SC_LOCKED);
                                sendReport(req, resp, errorList);
                            }
                        }
                    }
                    // User-Agent workarounds
                    doUserAgentWorkaround(resp);

                    // setting resourceContent
                    long resourceLength = _store
                            .setResourceContent(transaction, path, req
                                    .getInputStream(), null, null);

                    so = _store.getStoredObject(transaction, path);
                    if (so == null) {
                        resp.setStatus(WebdavStatus.SC_NOT_FOUND);
                    } else if (resourceLength != -1) {
                        so.setResourceLength(resourceLength);
                    }
                    // Now lets report back what was actually saved

                } catch (AccessDeniedException e) {
                    resp.sendError(WebdavStatus.SC_FORBIDDEN);
                } catch (ChecksumNotMatchException e) {
                    resp.sendError(WebdavStatus.SC_BAD_REQUEST, "The computed checksum does not match the one received from the client.");
                } catch (WebdavException e) {
                    LOG.error(e.getMessage());
                    resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                } finally {
                    //上传完主动释放这个文件的锁, finder这玩意有时候不会主动调用UNLOCK真是奇怪
                    String[] lockTokens = getLockIdFromIfHeader(req);
                    if (lockTokens != null) {
                        for (String lockToken : lockTokens) {
                            if (lockToken == null || lockToken.length() == 0) {
                                continue;
                            }
                            LockedObject lo = _resourceLocks.getLockedObjectByID(
                                    transaction, lockToken);
                            if (lo != null) {
                                lo.removeLockedObject();
                            }
                        }
                    }
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
     *
     */
    private void doUserAgentWorkaround(JapHttpResponse resp) {
        if (ClientIdentifyUtils.isOSXFinder(_userAgent)) {
            LOG.trace("DoPut.execute() : do workaround for user agent '"
                    + _userAgent + "'");
            resp.setStatus(WebdavStatus.SC_CREATED);
        } else if (_userAgent != null && _userAgent.contains("Transmit")) {
            // Transmit also uses WEBDAVFS 1.x.x but crashes
            // with SC_CREATED response
            LOG.trace("DoPut.execute() : do workaround for user agent '"
                    + _userAgent + "'");
            resp.setStatus(WebdavStatus.SC_NO_CONTENT);
        } else {
            resp.setStatus(WebdavStatus.SC_CREATED);
        }
    }


}
