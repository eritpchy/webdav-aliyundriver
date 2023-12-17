package com.github.zxbu.webdavteambition.store;

import com.fujieid.jap.http.JapHttpRequest;
import com.fujieid.jap.http.JapHttpResponse;
import com.github.zxbu.webdavteambition.bean.AFileReqInfo;
import com.github.zxbu.webdavteambition.bean.PathInfo;
import com.github.zxbu.webdavteambition.config.AliyunDriveProperties;
import com.github.zxbu.webdavteambition.manager.AliyunDriveSessionManager;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.sf.webdav.exceptions.ChecksumNotMatchException;
import net.sf.webdav.exceptions.ReadOnlyFolderException;
import net.sf.webdav.exceptions.WebdavException;
import net.sf.webdav.util.DateTimeUtils;
import net.xdow.aliyundrive.AliyunDrive;
import net.xdow.aliyundrive.AliyunDriveConstant;
import net.xdow.aliyundrive.IAliyunDrive;
import net.xdow.aliyundrive.IAliyunDriveAuthorizer;
import net.xdow.aliyundrive.bean.*;
import net.xdow.aliyundrive.exception.NotAuthenticatedException;
import net.xdow.aliyundrive.net.AliyunDriveCall;
import net.xdow.aliyundrive.util.JsonUtils;
import net.xdow.aliyundrive.webapi.AliyunDriveWebConstant;
import net.xdow.aliyundrive.webapi.impl.AliyunDriveWebApiImplV1;
import okhttp3.HttpUrl;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AliyunDriveClientService<T extends IAliyunDrive> implements IAliyunDriveAuthorizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AliyunDriveClientService.class);
    private static String rootPath = "/";
    private static int chunkSize = 10485760; // 10MB
    private AliyunDriveResponse.UserDriveInfo mUserDriveInfo;

    private AliyunDriveProperties mAliyunDriveProperties;
    // / 字符占位符
    private static String FILE_PATH_PLACE_HOLDER = "[@-@]";
    private ScheduledExecutorService mTaskPool = Executors.newScheduledThreadPool(1);
    private Runnable mOnAccountChangedListener;

    private LoadingCache<AFileReqInfo, Set<AliyunDriveFileInfo>> tFileListCache = CacheBuilder.newBuilder()
            .initialCapacity(128)
            .maximumSize(10240)
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build(new CacheLoader<AFileReqInfo, Set<AliyunDriveFileInfo>>() {
                @Override
                public Set<AliyunDriveFileInfo> load(AFileReqInfo info) throws Exception {
                    return AliyunDriveClientService.this.getTFileListInternal(info);
                }
            });

    //异步刷新器, 10秒间隔刷新一次
    private LoadingCache<AFileReqInfo, Object> tFileListAsyncRefresherCache = CacheBuilder.newBuilder()
            .initialCapacity(128)
            .maximumSize(10240)
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .build(new CacheLoader<AFileReqInfo, Object>() {
                @Override
                public Object load(final AFileReqInfo info) throws Exception {
                    mTaskPool.schedule(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                tFileListCache.refresh(info);
                            } catch (Exception e) {
                                LOGGER.error("", e);
                            }
                        }
                    }, 1, TimeUnit.SECONDS);
                    return new Object();
                }
            });

    private LoadingCache<AFileReqInfo, AliyunDriveResponse.FileGetDownloadUrlInfo> tFileDownloadUrlCache = CacheBuilder.newBuilder()
            .initialCapacity(128)
            .maximumSize(10240)
            .expireAfterWrite(AliyunDriveConstant.MAX_DOWNLOAD_URL_EXPIRE_TIME_SEC, TimeUnit.SECONDS)
            .build(new CacheLoader<AFileReqInfo, AliyunDriveResponse.FileGetDownloadUrlInfo>() {
                @Override
                public AliyunDriveResponse.FileGetDownloadUrlInfo load(AFileReqInfo info) throws Exception {
                    return fileGetDownloadUrlInternal(info);
                }
            });
    private final T mAliyunDrive;

    public AliyunDriveClientService(Class<? extends IAliyunDrive> aliyunDriveCls, AliyunDriveProperties aliyunDriveProperties) {
        System.setProperty("dns.server", "223.5.5.5,114.114.114.114");
        this.mAliyunDriveProperties = aliyunDriveProperties;
        this.mAliyunDrive = (T) AliyunDrive.newAliyunDrive(aliyunDriveCls);
        this.mAliyunDrive.setAuthorizer(this);
        if (!login(aliyunDriveProperties.getRefreshToken())) {
            login(aliyunDriveProperties.getRefreshTokenNext());
        }
    }

    public AliyunDriveProperties getProperties() {
        return this.mAliyunDriveProperties;
    }

    public T getAliyunDrive() {
        return this.mAliyunDrive;
    }

    public Set<AliyunDriveFileInfo> getTFileListCached(final AFileReqInfo info) {
        try {
            Set<AliyunDriveFileInfo> tFiles = tFileListCache.get(info);
            Set<AliyunDriveFileInfo> all = new LinkedHashSet<>(tFiles);
            // 获取上传中的文件列表
            Collection<AliyunDriveFileInfo> virtualTFiles = VirtualTFileService.getInstance().list(info);
            Map<String, AliyunDriveFileInfo> virtualTFileIdMap = new HashMap<>();
            for (AliyunDriveFileInfo vFileInfo : virtualTFiles) {
                virtualTFileIdMap.put(vFileInfo.getFileId(), vFileInfo);
            }
            //如果文件真实存在以实际为准, 删除虚拟文件
            for (AliyunDriveFileInfo fileInfo : tFiles) {
                AliyunDriveFileInfo vFileInfo = virtualTFileIdMap.get(fileInfo.getFileId());
                if (vFileInfo != null) {
                    virtualTFiles.remove(vFileInfo);
                }
            }
            all.addAll(virtualTFiles);
            return all;
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<AliyunDriveFileInfo> getTFileListInternal(AFileReqInfo info) {
        if (AliyunDriveVirtualRootFileInfo.isRoot(info.getDriveId())) {
            return getVirtualRootChildrenSets();
        }
        List<AliyunDriveFileInfo> tFileList = fileListFromApi(info, null, new ArrayList<AliyunDriveFileInfo>());
        Collections.sort(tFileList, new Comparator<AliyunDriveFileInfo>() {
            @Override
            public int compare(AliyunDriveFileInfo o1, AliyunDriveFileInfo o2) {
                return o2.getUpdatedAt().compareTo(o1.getUpdatedAt());
            }
        });
        Set<AliyunDriveFileInfo> tFileSets = new LinkedHashSet<>();
        for (AliyunDriveFileInfo tFile : tFileList) {
            String fileName = tFile.getName();
            if (StringUtils.isNotEmpty(fileName) && fileName.contains("/")) {
                tFile.setName(fileName.replace("/", FILE_PATH_PLACE_HOLDER));
            }
            if (!tFileSets.add(tFile)) {
                LOGGER.info("当前目录 driveId: {} fileId:{} 存在同名文件：{}，文件大小：{}",
                        info.getDriveId(),info.getFileId(), tFile.getName(), tFile.getSize());
            }
        }
        // 对文件名进行去重，只保留最新的一个
        return tFileSets;
    }

    private Set<AliyunDriveFileInfo> getVirtualRootChildrenSets() {
        Set<AliyunDriveFileInfo> set = new LinkedHashSet<>();
        AliyunDriveResponse.UserDriveInfo driveInfo = getUserDriveInfo();
        if (driveInfo == null) {
            return set;
        }
        String backupDriveId = driveInfo.getBackupDriveId();
        if (StringUtils.isNotEmpty(backupDriveId)) {
            set.add(new AliyunDriveBackupRootFileInfo(backupDriveId));
        }
        String resourceDriveId = driveInfo.getResourceDriveId();
        if (StringUtils.isNotEmpty(resourceDriveId)) {
            set.add(new AliyunDriveResourceRootFileInfo(resourceDriveId));
        }
        if (set.size() == 0) {
            set.add(new AliyunDriveBackupRootFileInfo(driveInfo.getDefaultDriveId()));
        }
        return set;
    }

    private List<AliyunDriveFileInfo> fileListFromApi(AFileReqInfo info, String marker, List<AliyunDriveFileInfo> all) {
        try {
            AliyunDriveRequest.FileListInfo query = new AliyunDriveRequest.FileListInfo(
                    info.getDriveId(), info.getFileId()
            );
            query.setMarker(marker);
            query.setLimit(200);
            query.setOrderBy(AliyunDriveEnum.OrderBy.UpdatedAt);
//            造孽传local_modified_at拿不到
//            query.setFields("drive_id,file_id,parent_file_id,name,size,type,created_at,updated_at,content_hash,local_modified_at,local_created_at");
            query.setOrderDirection(AliyunDriveEnum.OrderDirection.Desc);
            AliyunDriveResponse.FileListInfo res = this.mAliyunDrive.fileList(query).execute();
            if (res.isError()) {
                if ("TooManyRequests".equals(res.getCode())) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(300);
                    } catch (InterruptedException e) {
                    }
                    res = this.mAliyunDrive.fileList(query).execute();
                }
            }
            if (res.isError()) {
                if ("TooManyRequests".equals(res.getCode())) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(300);
                    } catch (InterruptedException e) {
                    }
                    res = this.mAliyunDrive.fileList(query).execute();
                }
            }
            all.addAll(res.getItems());
            String nextMarker = res.getNextMarker();
            if (StringUtils.isEmpty(nextMarker)) {
                return all;
            }
            return fileListFromApi(info, nextMarker, all);
        } catch (NotAuthenticatedException e) {
            //WebApi需要这个来引导用户输入token
            if (this.mAliyunDrive instanceof AliyunDriveWebApiImplV1) {
                return new ArrayList<>();
            }
            throw e;
        }
    }

    @Nullable
    private AliyunDriveResponse.UserDriveInfo getUserDriveInfo() {
        if (mUserDriveInfo == null) {
            synchronized (AliyunDriveClientService.class) {
                if (mUserDriveInfo == null) {
                    AliyunDriveResponse.UserDriveInfo res = mAliyunDrive.getUserDriveInfo().execute();
                    if (!res.isError()) {
                        mUserDriveInfo = res;
                    }
                }
            }
        }
        return mUserDriveInfo;
    }

    public void uploadPre(String path, long size, InputStream inputStream, @Nullable String sha1Sum, long modifyTimeSec, JapHttpResponse response) {
        boolean uploadSuccess = false;
        final VirtualTFileService virtualTFileService = VirtualTFileService.getInstance();
        path = normalizingPath(path);
        PathInfo pathInfo = getPathInfo(path);
        final AliyunDriveFileInfo parent = getTFileByPath(pathInfo.getParentPath());
        if (parent == null) {
            return;
        }
        if (parent instanceof AliyunDriveVirtualRootFileInfo) {
            throw new ReadOnlyFolderException();
        }
        final AFileReqInfo parentInfo = AFileReqInfo.from(parent);
        // 如果已存在，先删除
        AliyunDriveFileInfo tfile = getTFileByPath(path);
        if (tfile != null) {
            //先校验hash
            if (StringUtils.isNotEmpty(sha1Sum) && StringUtils.isNotEmpty(tfile.getContentHash())) {
                if (String.valueOf(tfile.getContentHash()).equalsIgnoreCase(sha1Sum)) {
                    return;
                }
            } else {
                //如果文件大小一样，则不再上传
                if (size > 0 && tfile.getSize() == size) {
                    return;
                }
            }
            removeByPath(path);
        }
        try {
            AliyunDriveResponse.FileCreateInfo fileCreateInfo = uploadCreateFile(parentInfo, pathInfo.getName(), size, modifyTimeSec);
            AFileReqInfo fileInfo = AFileReqInfo.from(fileCreateInfo);
            final String fileId = fileCreateInfo.getFileId();
            String uploadId = fileCreateInfo.getUploadId();
            List<AliyunDriveFilePartInfo> partInfoList = fileCreateInfo.getPartInfoList();
            try {
                long totalUploadedSize = 0;
                if (partInfoList != null) {
                    virtualTFileService.createVirtualFile(parentInfo, fileCreateInfo, modifyTimeSec);
                    LOGGER.info("文件预处理成功，开始上传。文件名：{}，上传URL数量：{}", path, partInfoList.size());

                    byte[] buffer = new byte[chunkSize];
                    for (int i = 0; i < partInfoList.size(); i++) {
                        AliyunDriveFilePartInfo partInfo = partInfoList.get(i);

                        long expires = Long.parseLong(Objects.requireNonNull(Objects.requireNonNull(HttpUrl.parse(partInfo.getUploadUrl())).queryParameter("x-oss-expires")));
                        if (System.currentTimeMillis() / 1000 + 10 >= expires) {
                            // 已过期，重新置换UploadUrl
                            refreshUploadUrl(fileInfo, uploadId, partInfoList);
                        }

                        try {
                            int read = IOUtils.read(inputStream, buffer, 0, buffer.length);
                            if (read == -1) {
                                LOGGER.info("文件上传结束。文件名：{}，当前进度：{}/{}", path, (i + 1), partInfoList.size());
                                return;
                            } else if (read == 0) {
                                continue;
                            }
                            this.mAliyunDrive.upload(partInfo.getUploadUrl(), buffer, 0, read).execute();
                            virtualTFileService.updateLength(parentInfo, fileId, read);
                            LOGGER.info("文件正在上传。文件名：{}，当前进度：{}/{}", path, (i + 1), partInfoList.size());
//                            Rclone <= 1.62.2 版本, Vendor Nextcloud 不能使用flushBuffer, 否则会导致提前返回, 导致后续操作异常,
//                            比如: 获取到未完成传输的文件大小。1.63.0版本同Vendor未发现问题
//                            response.flushBuffer();
                            totalUploadedSize += read;
                        } catch (IOException e) {
                            throw new WebdavException(e);
                        }
                    }
                }
                if (size == 0) {
                    uploadSuccess = true;
                    AliyunDriveFileInfo vTFile = virtualTFileService.get(parentInfo, fileId);
                    vTFile.setContentHash(null);
                    vTFile.setSize(0L);
                    LOGGER.info("文件上传成功。文件名：{} 文件大小: {} 已上传: {} 虚拟文件: {}", path, size, totalUploadedSize, vTFile);
                } if (totalUploadedSize == size) {
                    AliyunDriveResponse.FileUploadCompleteInfo fileUploadCompleteInfo = uploadComplete(fileInfo, uploadId, sha1Sum);
                    uploadSuccess = true;
                    AliyunDriveFileInfo vTFile = virtualTFileService.get(parentInfo, fileId);
                    vTFile.setContentHash(fileUploadCompleteInfo.getContentHash());
                    vTFile.setSize(fileUploadCompleteInfo.getSize());
                    vTFile.setThumbnail(fileUploadCompleteInfo.getThumbnail());
                    LOGGER.info("文件上传成功。文件名：{} 文件大小: {} 已上传: {} 虚拟文件: {}", path, size, totalUploadedSize, vTFile);
                } else {
                    LOGGER.info("文件上传失败。文件名：{} 文件大小: {} 已上传: {}", path, size, totalUploadedSize);
                }
            } finally {
                if (!uploadSuccess) {
                    virtualTFileService.remove(parentInfo, fileId);
                } else {
                    //延迟删除虚拟文件,防止刚上传拿不到最新的文件
                    mTaskPool.schedule(new Runnable() {
                        @Override
                        public void run() {
                            virtualTFileService.remove(parentInfo, fileId);
                        }
                    }, 120, TimeUnit.SECONDS);
                }
                clearCache(fileInfo);
            }
        } finally {
            clearCacheAsync(parentInfo);
        }
    }

    private AliyunDriveResponse.FileUploadCompleteInfo uploadComplete(AFileReqInfo info, String uploadId, @Nullable String sha1Sum) {
        AliyunDriveRequest.FileUploadCompleteInfo query = new AliyunDriveRequest.FileUploadCompleteInfo(
                info.getDriveId(), info.getFileId(), uploadId
        );
        AliyunDriveResponse.FileUploadCompleteInfo res = this.mAliyunDrive.fileUploadComplete(query).execute();
        if (!StringUtils.isEmpty(res.getCode())) {
            throw new WebdavException(new WebdavException(res.getCode(), res.getMessage()));
        }
        if (StringUtils.isNotEmpty(sha1Sum)) {
            if (!String.valueOf(res.getContentHash()).equalsIgnoreCase(sha1Sum)) {
                throw new ChecksumNotMatchException();
            }
        }
        return res;
    }

    private void refreshUploadUrl(AFileReqInfo info, String uploadId, List<AliyunDriveFilePartInfo> partInfoList) {
        AliyunDriveRequest.FileGetUploadUrlInfo query = new AliyunDriveRequest.FileGetUploadUrlInfo(
                info.getDriveId(), info.getFileId(), uploadId, partInfoList
        );
        AliyunDriveResponse.FileGetUploadUrlInfo res = this.mAliyunDrive.fileGetUploadUrl(query).execute();
        List<AliyunDriveFilePartInfo> newPartInfoList = res.getPartInfoList();
        Map<Long, AliyunDriveFilePartInfo> newPartInfoMap = new HashMap<>();
        for (AliyunDriveFilePartInfo partInfo : newPartInfoList) {
            newPartInfoMap.put(partInfo.getPartNumber(), partInfo);
        }

        for (int j = 0; j < partInfoList.size(); j++) {
            AliyunDriveFilePartInfo oldInfo = partInfoList.get(j);
            AliyunDriveFilePartInfo newInfo = newPartInfoMap.get(oldInfo.getPartNumber());
            if (newInfo == null) {
                throw new NullPointerException("newInfo is null");
            }
            oldInfo.setUploadUrl(newInfo.getUploadUrl());
        }
    }

    private AliyunDriveResponse.FileCreateInfo uploadCreateFile(AFileReqInfo parentInfo, String name, long size, long modifyTimeSec) {
        AliyunDriveRequest.FileCreateInfo query = new AliyunDriveRequest.FileCreateInfo(
                parentInfo.getDriveId(), parentInfo.getFileId(), name, AliyunDriveEnum.Type.File,
                AliyunDriveEnum.CheckNameMode.Refuse
        );
        if (modifyTimeSec != -1) {
            query.setLocalModifiedAt(DateTimeUtils.convertLocalDateToGMT(modifyTimeSec * 1000));
        }
        query.setSize(size);
        int chunkCount = (int) Math.ceil(((double) size) / chunkSize); // 进1法
        List<AliyunDriveFilePartInfo> partInfoList = new ArrayList<>();
        for (int i = 0; i < chunkCount; i++) {
            AliyunDriveFilePartInfo partInfo = new AliyunDriveFilePartInfo();
            partInfo.setPartNumber(i + 1);
            partInfoList.add(partInfo);
        }
        query.setPartInfoList(partInfoList);
        LOGGER.info("开始上传文件，文件名：{}，总大小：{}, 文件块数量：{}", name, size, chunkCount);
        AliyunDriveResponse.FileCreateInfo res = this.mAliyunDrive.fileCreate(query).execute();
        if (!StringUtils.isEmpty(res.getCode())) {
            throw new WebdavException(new WebdavException(res.getCode(), res.getMessage()));
        }
        return res;
    }


    public void rename(String sourcePath, String newName) {
        sourcePath = normalizingPath(sourcePath);
        AliyunDriveFileInfo tFile = getTFileByPath(sourcePath);
        if (tFile instanceof AliyunDriveVirtualRootFileInfo
            || tFile instanceof AliyunDriveResourceRootFileInfo
            || tFile instanceof AliyunDriveBackupRootFileInfo) {
            throw new ReadOnlyFolderException();
        }
        AFileReqInfo fileInfo = AFileReqInfo.from(tFile);
        AFileReqInfo parentInfo = AFileReqInfo.fromParent(tFile);
        try {
            AliyunDriveRequest.FileRenameInfo query = new AliyunDriveRequest.FileRenameInfo(
                    tFile.getDriveId(), tFile.getFileId(), newName, tFile.getParentFileId()
            );

            AliyunDriveResponse.FileRenameInfo res = this.mAliyunDrive.fileRename(query).execute();
            if (res.isError()) {
                if ("AlreadyExist.File".equals(res.getCode())) {
                    removeByPath(getNodeIdByParentId(parentInfo, newName));
                    res = this.mAliyunDrive.fileRename(query).execute();
                }
            }
            try {
                if (!StringUtils.isEmpty(res.getCode())) {
                    throw new WebdavException(new WebdavException(res.getCode(), res.getMessage()));
                }
            } finally {
                if (!res.isError()) {
                    clearCache(new AFileReqInfo(res.getDriveId(), res.getFileId()));
                }
            }
        } finally {
            clearCache(fileInfo);
            clearCache(parentInfo);
        }
    }

    public void move(String sourcePath, String targetPath) {
        sourcePath = normalizingPath(sourcePath);
        targetPath = normalizingPath(targetPath);

        AliyunDriveFileInfo sourceTFile = getTFileByPath(sourcePath);
        AliyunDriveFileInfo targetTFile = getTFileByPath(targetPath);
        AFileReqInfo sourceFileInfo = AFileReqInfo.from(sourceTFile);
        AFileReqInfo targetFileInfo = AFileReqInfo.from(targetTFile);
        AFileReqInfo sourceParentInfo = AFileReqInfo.fromParent(sourceTFile);
        AFileReqInfo targetParentInfo = AFileReqInfo.fromParent(targetTFile);
        if (targetTFile instanceof AliyunDriveVirtualRootFileInfo
                || sourceTFile instanceof AliyunDriveVirtualRootFileInfo
                || sourceTFile instanceof AliyunDriveResourceRootFileInfo
                || sourceTFile instanceof AliyunDriveBackupRootFileInfo) {
            throw new ReadOnlyFolderException();
        }
        try {
            if (!sourceTFile.getDriveId().equals(targetTFile.getDriveId())) {
                throw new WebdavException("CrossDriveCode", "Cross drive operation not supported yet.");
//                TODO Cross drive operation not supported yet.
//                moveFileCrossDrive(sourceTFile, targetTFile);
//                return;
            }
            AliyunDriveRequest.FileMoveInfo query = new AliyunDriveRequest.FileMoveInfo(
                    sourceFileInfo.getDriveId(), targetFileInfo.getDriveId(),
                    sourceTFile.getFileId(), targetTFile.getFileId()
            );
            AliyunDriveResponse.FileMoveInfo res = this.mAliyunDrive.fileMove(query).execute();
            if (res.isError()) {
                throw new WebdavException(res.getCode(), res.getMessage());
            }
        } finally {
            clearCache(sourceFileInfo);
            clearCache(sourceParentInfo);
            clearCache(targetFileInfo);
            clearCache(targetParentInfo);
        }
    }

    private void moveFileCrossDrive(AliyunDriveFileInfo sourceTFile, AliyunDriveFileInfo targetTFile) {
        copyFile(sourceTFile, targetTFile);
        removeByPath(sourceTFile);
    }

    private void copyFile(AliyunDriveFileInfo sourceTFile, AliyunDriveFileInfo targetTFile) {
        AliyunDriveRequest.FileCopyInfo query = new AliyunDriveRequest.FileCopyInfo(
                sourceTFile.getDriveId(), sourceTFile.getFileId(),
                targetTFile.getDriveId(), targetTFile.getParentFileId()
        );
        AliyunDriveResponse.FileCopyInfo res = this.mAliyunDrive.fileCopy(query).execute();
        if (res.isError()) {
            throw new WebdavException(res.getCode(), res.getMessage());
        }
    }

    public void removeByPath(@Nullable AliyunDriveFileInfo tFile) {
        if (tFile == null) {
            return;
        }
        if (tFile instanceof AliyunDriveVirtualRootFileInfo
            || tFile instanceof AliyunDriveResourceRootFileInfo
            || tFile instanceof AliyunDriveBackupRootFileInfo) {
            throw new ReadOnlyFolderException();
        }
        try {
            removeById(AFileReqInfo.from(tFile));
        } finally {
            clearCache(AFileReqInfo.fromParent(tFile));
        }
    }

    public void removeById(AFileReqInfo info) {
        if (!info.isValid()) {
            return;
        }
        try {
            AliyunDriveRequest.FileMoveToTrashInfo query = new AliyunDriveRequest.FileMoveToTrashInfo(
                    info.getDriveId(), info.getFileId()
            );
            AliyunDriveResponse.FileMoveToTrashInfo res = this.mAliyunDrive.fileMoveToTrash(query).execute();
            if (!StringUtils.isEmpty(res.getCode())) {
                throw new WebdavException(new WebdavException(res.getCode(), res.getMessage()));
            }
        } finally {
            clearCache(info);
        }
    }

    public void removeByPath(String path) {
        path = normalizingPath(path);
        AliyunDriveFileInfo tFile = getTFileByPath(path);
        AFileReqInfo parentInfo = AFileReqInfo.fromParent(tFile);
        VirtualTFileService.getInstance().remove(parentInfo, tFile.getFileId());
        removeByPath(tFile);
    }

    public void createFolder(String path) {
        path = normalizingPath(path);
        PathInfo pathInfo = getPathInfo(path);
        AliyunDriveFileInfo parent = getTFileByPath(pathInfo.getParentPath());
        if (parent instanceof AliyunDriveVirtualRootFileInfo) {
            throw new ReadOnlyFolderException();
        }
        if (parent == null) {
            LOGGER.warn("创建目录失败，未发现父级目录：{}", pathInfo.getParentPath());
            return;
        }
        AFileReqInfo parentInfo = AFileReqInfo.from(parent);
        try {
            AliyunDriveRequest.FileCreateInfo query = new AliyunDriveRequest.FileCreateInfo(
                    parentInfo.getDriveId(), parentInfo.getFileId(), pathInfo.getName(),
                    AliyunDriveEnum.Type.Folder, AliyunDriveEnum.CheckNameMode.Refuse
            );
            query.setLocalCreatedAt(DateTimeUtils.getCurrentDateGMT());
            AliyunDriveResponse.FileCreateInfo res = this.mAliyunDrive.fileCreate(query).execute();
            try {
                if (!StringUtils.isEmpty(res.getCode())) {
                    throw new WebdavException(new WebdavException(res.getCode(), res.getMessage()));
                }
            } finally {
                if (!res.isError()) {
                    clearCache(AFileReqInfo.from(res));
                }
            }
        } finally {
            clearCache(parentInfo);
        }
    }

    public AliyunDriveFileInfo getTFileByPath(String path) {
        path = normalizingPath(path);
        return getNodeIdByPath2(path);
    }

    public Response download(String path, JapHttpRequest request, long size) {
        if (size == 0) {
            return new Response.Builder()
                    .request(new Request.Builder().url(request.getRequestUrl().toString()).build())
                    .code(204)
                    .protocol(Protocol.HTTP_1_1)
                    .message("")
                    .build();
        }
        AliyunDriveFileInfo file = getTFileByPath(path);
        AFileReqInfo fileInfo = AFileReqInfo.from(file);
        String range = extractRangeHeader(request, size);
        String ifRange = extractIfRangeHeader(request);
        AliyunDriveResponse.FileGetDownloadUrlInfo res = fileGetDownloadUrlInfo(fileInfo);
        if (res.isError()) {
            throw new WebdavException(new WebdavException(res.getCode(), res.getMessage()));
        }
        String url = res.getUrl().replaceAll("^https://", "http://");
        try {
            return this.mAliyunDrive.download(url, range, ifRange).execute();
        } catch (Throwable t) {
            throw new WebdavException(t);
        }
    }

   @Nullable
   public String getDownloadUrlByPath(String path) {
        AliyunDriveFileInfo file = getTFileByPath(path);
        if (file == null) {
            return null;
        }
       AFileReqInfo fileInfo = AFileReqInfo.from(file);
       AliyunDriveResponse.FileGetDownloadUrlInfo res = fileGetDownloadUrlInfo(fileInfo);
        if (res.isError()) {
            throw new WebdavException(new WebdavException(res.getCode(), res.getMessage()));
        }
        return res.getUrl();
    }

    private synchronized AliyunDriveResponse.FileGetDownloadUrlInfo fileGetDownloadUrlInfo(AFileReqInfo info) {
        AliyunDriveResponse.FileGetDownloadUrlInfo res;
        try {
            res = tFileDownloadUrlCache.get(info);
            Date expirationDate = res.getExpiration();
            if (expirationDate != null && DateTimeUtils.getCurrentDateGMT().after(expirationDate)) {
                tFileDownloadUrlCache.invalidate(info);
                res = tFileDownloadUrlCache.get(info);
            }
            return res;
        } catch (ExecutionException e) {
            res = new AliyunDriveResponse.FileGetDownloadUrlInfo();
            res.setCode(e.getMessage());
            res.setMessage(e.toString());
            return res;
        }
    }

    private synchronized AliyunDriveResponse.FileGetDownloadUrlInfo fileGetDownloadUrlInternal(AFileReqInfo info) {
        AliyunDriveRequest.FileGetDownloadUrlInfo query = new AliyunDriveRequest.FileGetDownloadUrlInfo(
                info.getDriveId(), info.getFileId()
        );
        query.setExpireSec(AliyunDriveConstant.MAX_DOWNLOAD_URL_EXPIRE_TIME_SEC);
        AliyunDriveResponse.FileGetDownloadUrlInfo res = this.mAliyunDrive.fileGetDownloadUrl(query).execute();
        if (res.isError()) {
            if ("TooManyRequests".equals(res.getCode())) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                }
                res = this.mAliyunDrive.fileGetDownloadUrl(query).execute();
            }
        }
        if (res.isError()) {
            if ("TooManyRequests".equals(res.getCode())) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                }
                res = this.mAliyunDrive.fileGetDownloadUrl(query).execute();
            }
        }
        if (res.isError()) {
            LOGGER.error("fileGetDownloadUrlInternal code: " + res.getCode() + " message: " + res.getMessage());
            res.setUrl("");
            return res;
        }
        return res;
    }

    private String extractIfRangeHeader(JapHttpRequest request) {
        String ifRange = request.getHeader("if-range");
        if (ifRange == null) {
            return null;
        }
        return ifRange;
    }

    private String extractRangeHeader(JapHttpRequest request, long size) {
        String range = request.getHeader("range");
        if (range == null) {
            return null;
        }
        // 如果range最后 >= size， 则去掉
        String[] split = range.split("-");
        if (split.length == 2) {
            String end = split[1];
            if (Long.parseLong(end) >= size) {
                range = range.substring(0, range.lastIndexOf('-') + 1);
            }
        }
        return range;
    }

    private AliyunDriveFileInfo getNodeIdByPath2(String path) {
        if (StringUtils.isEmpty(path)) {
            return AliyunDriveVirtualRootFileInfo.INSTANCE;
        }

        PathInfo pathInfo = getPathInfo(path);
        AliyunDriveFileInfo tFile = getTFileByPath(pathInfo.getParentPath());
        if (tFile == null) {
            return null;
        }
        String name = pathInfo.getName();
        if (tFile instanceof AliyunDriveVirtualRootFileInfo) {
            Set<AliyunDriveFileInfo> sets = getVirtualRootChildrenSets();
            for (AliyunDriveFileInfo info: sets) {
                if (info.getName().equals(name)) {
                    return info;
                }
            }
        }
        return getNodeIdByParentId(AFileReqInfo.from(tFile), name);
    }

    public PathInfo getPathInfo(String path) {
        path = normalizingPath(path);
        if (path.equals(rootPath)) {
            PathInfo pathInfo = new PathInfo();
            pathInfo.setPath(path);
            pathInfo.setName(path);
            return pathInfo;
        }
        File file = new File(path);
        PathInfo pathInfo = new PathInfo();
        pathInfo.setPath(path);
        String parentPath = file.getParent();
        if (parentPath == null) {
            pathInfo.setParentPath("/");
        } else {
            pathInfo.setParentPath(parentPath.replace("\\", "/"));
        }
        pathInfo.setName(file.getName());
        return pathInfo;
    }

    public AliyunDriveFileInfo getNodeIdByParentId(AFileReqInfo info, String name) {
        Set<AliyunDriveFileInfo> tFiles = getTFileListCached(info);
        for (AliyunDriveFileInfo tFile : tFiles) {
            if (tFile.getName().equals(name)) {
                return tFile;
            }
        }
        return null;
    }

    private String normalizingPath(String path) {
        path = path.replace("\\", "/");
        path = path.replaceAll("//", "/");
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public void clearCache(AFileReqInfo info) {
        if (!info.isValid()) {
            return;
        }
        LOGGER.info("clearCache! {}", info);
        tFileListCache.invalidate(info);
    }

    public void clearCacheAsync(AFileReqInfo info) {
        if (StringUtils.isEmpty(info.getDriveId())) {
            return;
        }
        if (StringUtils.isEmpty(info.getFileId())) {
            return;
        }
        LOGGER.info("clearCacheAsync! driveId: {} fileId: {}",
                info.getDriveId(), info.getFileId());
        try {
            tFileListAsyncRefresherCache.get(info);
        } catch (ExecutionException e) {
            LOGGER.error("", e);
        }
    }

    public void clearCacheAll() {
        tFileListCache.invalidateAll();
    }

    private boolean login(String refreshToken) {
        if (StringUtils.isEmpty(refreshToken)) {
            return false;
        }
        AliyunDriveCall<AliyunDriveResponse.AccessTokenInfo> call;
        String accessToken = "";
        if (this.mAliyunDrive instanceof AliyunDriveWebApiImplV1) {
            AliyunDriveRequest.AccessTokenInfo query = new AliyunDriveRequest.AccessTokenInfo();
            query.setGrantType(AliyunDriveEnum.GrantType.RefreshToken);
            query.setRefreshToken(refreshToken);
            call = this.mAliyunDrive.getAccessToken(query);
        } else {
            String url = String.format(Locale.getDefault(), this.mAliyunDriveProperties.getAliyunAccessTokenUrl(), accessToken, refreshToken);
            call = this.mAliyunDrive.getAccessToken(url);
        }
        try {
            AliyunDriveResponse.AccessTokenInfo res = call.execute();
            if (res.isError()) {
                return false;
            }
            AliyunDriveClientService.this.mAliyunDrive.setAccessTokenInfo(res);
            AliyunDriveClientService.this.mAliyunDriveProperties.save(res);
            LOGGER.info("登录成功! {}", JsonUtils.toJson(res));
            return true;
        } catch (Exception e) {
            LOGGER.error("login", e);
        }
        return false;
    }

    @Override
    public AliyunDriveResponse.AccessTokenInfo acquireNewAccessToken(AliyunDriveResponse.AccessTokenInfo oldAccessTokenInfo) {
        if (this.mAliyunDrive instanceof AliyunDriveWebApiImplV1) {
            return acquireNewAccessTokenWebApi();
        }
        String refreshToken = this.mAliyunDriveProperties.getRefreshToken();
        String refreshTokenNext = this.mAliyunDriveProperties.getRefreshTokenNext();
        String accessToken = "";
        String url = "";
        AliyunDriveResponse.AccessTokenInfo res = null;
        if (StringUtils.isNotEmpty(refreshToken)) {
            url = String.format(Locale.getDefault(), this.mAliyunDriveProperties.getAliyunAccessTokenUrl(), accessToken, refreshToken);
            res = this.mAliyunDrive.getAccessToken(url).execute();
            if (!res.isError()) {
                AliyunDriveClientService.this.mAliyunDriveProperties.save(res);
                return res;
            }
        }
        if (StringUtils.isNotEmpty(refreshTokenNext)) {
            url = String.format(Locale.getDefault(), this.mAliyunDriveProperties.getAliyunAccessTokenUrl(), accessToken, refreshTokenNext);
            res = this.mAliyunDrive.getAccessToken(url).execute();
            if (!res.isError()) {
                AliyunDriveClientService.this.mAliyunDriveProperties.save(res);
                return res;
            }
        }
        return null;
    }

    private AliyunDriveResponse.AccessTokenInfo acquireNewAccessTokenWebApi() {

        if (this.mAliyunDrive instanceof AliyunDriveWebApiImplV1) {
        } else {
            throw new RuntimeException("Error: AliyunDrive class cast Error, expected: "
                    + AliyunDriveWebApiImplV1.class + " got: " + this.mAliyunDrive.getClass());
        }
        mAliyunDriveProperties.setAuthorization(null);
        AliyunDriveResponse.AccessTokenInfo res;
        AliyunDriveRequest.AccessTokenInfo query = new AliyunDriveRequest.AccessTokenInfo();
        query.setGrantType(AliyunDriveEnum.GrantType.RefreshToken);
        query.setRefreshToken(mAliyunDriveProperties.getRefreshToken());
        res = this.mAliyunDrive.getAccessToken(query).execute();
        if (res.isError()) {
            query.setRefreshToken(mAliyunDriveProperties.getRefreshTokenNext());
            res = this.mAliyunDrive.getAccessToken(query).execute();
        }
        if (res.isError()) {
            throw new IllegalStateException(res.getMessage() + "(" + res.getCode() + ")");
        }

        String accessToken = res.getAccessToken();
        String refreshToken = res.getRefreshToken();
        String userId = res.getUserId();
        if (StringUtils.isEmpty(res.getAccessToken()))
            throw new IllegalArgumentException("获取accessToken失败");
        if (StringUtils.isEmpty(res.getRefreshToken()))
            throw new IllegalArgumentException("获取refreshToken失败");
        if (StringUtils.isEmpty(userId))
            throw new IllegalArgumentException("获取userId失败");
        mAliyunDriveProperties.setUserId(userId);
        mAliyunDriveProperties.setAuthorization(accessToken);
        mAliyunDriveProperties.setRefreshToken(refreshToken);
        mAliyunDriveProperties.save();
        return res;
    }

    @Override
    public <T> T onAuthorizerEvent(String eventId, Object data, Class<T> resultCls) {
        if (StringUtils.isEmpty(eventId)) {
            return null;
        }
        switch (eventId) {
            case AliyunDriveWebConstant.Event.DEVICE_SESSION_SIGNATURE_INVALID: {
                if (this.mAliyunDrive instanceof AliyunDriveWebApiImplV1) {
                    AliyunDriveSessionManager mgr = new AliyunDriveSessionManager((AliyunDriveWebApiImplV1) this.mAliyunDrive, this.mAliyunDriveProperties);
                    mgr.updateSession();
                } else {
                    throw new RuntimeException("Error: AliyunDrive class cast Error, expected: "
                            + AliyunDriveWebApiImplV1.class + " got: " + this.mAliyunDrive.getClass());
                }
            }
            break;
            case AliyunDriveWebConstant.Event.USER_DEVICE_OFFLINE: {
                this.mAliyunDriveProperties.getSession().setNonce(0);
                this.mAliyunDriveProperties.getSession().setExpireTimeSec(0);
                this.mAliyunDriveProperties.save();
            }
            break;
            case AliyunDriveWebConstant.Event.ACQUIRE_DEVICE_ID: {
                return (T) this.mAliyunDriveProperties.getDeviceId();
            }
            case AliyunDriveWebConstant.Event.ACQUIRE_SESSION_SIGNATURE: {
                return (T) this.mAliyunDriveProperties.getSession().getSignature();
            }
        }
        return null;
    }

    public void setAccessTokenInvalidListener(Runnable listener) {
        this.mAliyunDrive.setAccessTokenInvalidListener(listener);
    }

    public void setOnAccountChangedListener(Runnable listener) {
        this.mOnAccountChangedListener = listener;
    }

    private LoadingCache<String, AliyunDriveResponse.UserSpaceInfo> tSpaceInfoCache = CacheBuilder.newBuilder()
            .initialCapacity(128)
            .maximumSize(10240)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(new CacheLoader<String, AliyunDriveResponse.UserSpaceInfo>() {
                @Override
                public AliyunDriveResponse.UserSpaceInfo load(String key) throws Exception {
                    return AliyunDriveClientService.this.mAliyunDrive.getUserSpaceInfo().execute();
                }
            });

    public long getQuotaAvailableBytes() {
        try {
            AliyunDriveResponse.UserSpaceInfo res = tSpaceInfoCache.get("");
            if (res.isError()) {
                tSpaceInfoCache.invalidate("");
            }
            return res.getTotalSize() - res.getUsedSize();
        } catch (ExecutionException e) {
            LOGGER.error("getQuotaAvailableBytes", e);
        }
        return -1;
    }

    public long getQuotaUsedBytes() {
        try {
            AliyunDriveResponse.UserSpaceInfo res = tSpaceInfoCache.get("");
            if (res.isError()) {
                tSpaceInfoCache.invalidate("");
            }
            return res.getUsedSize();
        } catch (ExecutionException e) {
            LOGGER.error("getQuotaAvailableBytes", e);
        }
        return -1;
    }

    public void onAccountChanged() {
        this.clearCacheAll();
        this.mUserDriveInfo = null;
        this.tSpaceInfoCache.invalidateAll();
        Runnable onAccountChangedListener = mOnAccountChangedListener;
        if (onAccountChangedListener != null) {
            onAccountChangedListener.run();
        }
    }
}
