package com.github.zxbu.webdavteambition.handler;

import java.util.HashMap;
import java.util.Map;

public class GetRequestHandlerHolder {
    public static Map<String, IGetRequestHandler> INSTANCE = new HashMap<String, IGetRequestHandler>() {{
        put("thumbnail", new ThumbnailRequestHandler());
        put("preview", new PreviewRequestHandler());
        put("proxy", new ProxyDownloaderRequestHandler());
    }};
}
