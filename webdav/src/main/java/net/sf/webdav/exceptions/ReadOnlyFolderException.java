package net.sf.webdav.exceptions;

public class ReadOnlyFolderException extends WebdavException {

    public ReadOnlyFolderException() {
        super("ReadOnlyFolderCode", "Folder is read only.");
    }
}
