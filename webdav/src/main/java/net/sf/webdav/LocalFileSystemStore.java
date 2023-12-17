/*
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
 *
 */
package net.sf.webdav;

import com.fujieid.jap.http.JapHttpRequest;
import com.fujieid.jap.http.JapHttpResponse;
import net.sf.webdav.exceptions.WebdavException;

import java.io.*;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Reference Implementation of WebdavStore
 * 
 * @author joa
 * @author re
 */
public class LocalFileSystemStore implements IWebdavStore {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(LocalFileSystemStore.class);

    private static int BUF_SIZE = 65536;

    private File _root = null;

    public LocalFileSystemStore(File root) {
        _root = root;
    }

    public void destroy() {
        ;
    }

    public ITransaction begin(Principal principal, JapHttpRequest req, JapHttpResponse resp) throws WebdavException {
        LOG.info("LocalFileSystemStore.begin()");
        if (!_root.exists()) {
            if (!_root.mkdirs()) {
                throw new WebdavException("root path: "
                        + _root.getAbsolutePath()
                        + " does not exist and could not be created");
            }
        }
//        if (principal == null) {
//            throw new UnauthenticatedException(WebdavStatus.SC_UNAUTHORIZED);
//        }
        return null;
    }

    public void checkAuthentication(ITransaction transaction)
            throws SecurityException {
        LOG.info("LocalFileSystemStore.checkAuthentication()");
        // do nothing
//        throw new UnauthenticatedException(WebdavStatus.SC_FORBIDDEN);
    }

    public void commit(ITransaction transaction) throws WebdavException {
        // do nothing
        LOG.info("LocalFileSystemStore.commit()");
    }

    public void rollback(ITransaction transaction) throws WebdavException {
        // do nothing
        LOG.info("LocalFileSystemStore.rollback()");

    }

    public void createFolder(ITransaction transaction, String uri)
            throws WebdavException {
        LOG.info("LocalFileSystemStore.createFolder(" + uri + ")");
        File file = new File(_root, uri);
        if (!file.mkdir())
            throw new WebdavException("cannot create folder: " + uri);
    }

    public void createResource(ITransaction transaction, String uri)
            throws WebdavException {
        LOG.info("LocalFileSystemStore.createResource(" + uri + ")");
        File file = new File(_root, uri);
        try {
            if (!file.createNewFile())
                throw new WebdavException("cannot create file: " + uri);
        } catch (IOException e) {
            LOG
                    .error("LocalFileSystemStore.createResource(" + uri
                            + ") failed");
            throw new WebdavException(e);
        }
    }

    public long setResourceContent(ITransaction transaction, String uri,
            InputStream is, String contentType, String characterEncoding)
            throws WebdavException {

        LOG.info("LocalFileSystemStore.setResourceContent(" + uri + ")");
        File file = new File(_root, uri);
        try {
            OutputStream os = new BufferedOutputStream(new FileOutputStream(
                    file), BUF_SIZE);
            try {
                int read;
                byte[] copyBuffer = new byte[BUF_SIZE];

                while ((read = is.read(copyBuffer, 0, copyBuffer.length)) != -1) {
                    os.write(copyBuffer, 0, read);
                }
            } finally {
                try {
                    is.close();
                } finally {
                    os.close();
                }
            }
        } catch (IOException e) {
            LOG.error("LocalFileSystemStore.setResourceContent(" + uri
                    + ") failed");
            throw new WebdavException(e);
        }
        long length = -1;

        try {
            length = file.length();
        } catch (SecurityException e) {
            LOG.error("LocalFileSystemStore.setResourceContent(" + uri
                    + ") failed" + "\nCan't get file.length");
        }

        return length;
    }

    public String[] getChildrenNames(ITransaction transaction, String uri)
            throws WebdavException {
        LOG.info("LocalFileSystemStore.getChildrenNames(" + uri + ")");
        File file = new File(_root, uri);
        String[] childrenNames = null;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            List<String> childList = new ArrayList<>();
            String name = null;
            for (int i = 0; i < children.length; i++) {
                name = children[i].getName();
                childList.add(name);
                LOG.info("Child " + i + ": " + name);
            }
            childrenNames = new String[childList.size()];
            childrenNames = (String[]) childList.toArray(childrenNames);
        }
        return childrenNames;
    }

    public void removeObject(ITransaction transaction, String uri)
            throws WebdavException {
        File file = new File(_root, uri);
        boolean success = file.delete();
        LOG.info("LocalFileSystemStore.removeObject(" + uri + ")=" + success);
        if (!success) {
            throw new WebdavException("cannot delete object: " + uri);
        }

    }

    @Override
    public boolean moveObject(ITransaction transaction, String destinationPath, String path) {
        return false;
    }

    public InputStream getResourceContent(ITransaction transaction, String uri)
            throws WebdavException {
        LOG.info("LocalFileSystemStore.getResourceContent(" + uri + ")");
        File file = new File(_root, uri);

        InputStream in;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
        } catch (IOException e) {
            LOG.error("LocalFileSystemStore.getResourceContent(" + uri
                    + ") failed");
            throw new WebdavException(e);
        }
        return in;
    }

    public long getResourceLength(ITransaction transaction, String uri)
            throws WebdavException {
        LOG.info("LocalFileSystemStore.getResourceLength(" + uri + ")");
        File file = new File(_root, uri);
        return file.length();
    }

    public StoredObject getStoredObject(ITransaction transaction, String uri) {

        StoredObject so = null;

        File file = new File(_root, uri);
        if (file.exists()) {
            so = new StoredObject();
            so.setFolder(file.isDirectory());
            so.setLastModified(new Date(file.lastModified()));
            so.setCreationDate(new Date(file.lastModified()));
            so.setResourceLength(getResourceLength(transaction, uri));
        }

        return so;
    }

    @Override
    public long getQuotaAvailableBytes(ITransaction transaction) {
        return _root.getUsableSpace();
    }

    @Override
    public long getQuotaUsedBytes(ITransaction transaction) {
        return _root.getTotalSpace() - _root.getUsableSpace();
    }

    @Override
    public String getFooter(ITransaction transaction) {
        return "";
    }

    @Override
    public String getResourceDownloadUrlForRedirection(ITransaction transaction, String path) {
        return null;
    }
    @Override
    public String getPublicLink(ITransaction transaction, String path) {
        return null;
    }

    @Override
    public boolean handleCustomGetRequest(ITransaction transaction, String path) {
        return false;
    }
}
