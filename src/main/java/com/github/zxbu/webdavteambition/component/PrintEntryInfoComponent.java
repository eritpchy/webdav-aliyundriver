package com.github.zxbu.webdavteambition.component;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@Component
public class PrintEntryInfoComponent implements ApplicationListener<WebServerInitializedEvent>  {

    @Autowired
    private TaskScheduler mTaskScheduler;

    @SneakyThrows
    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        WebServer server = event.getWebServer();
        WebServerApplicationContext context = event.getApplicationContext();
        Environment env = context.getEnvironment();

        int port = server.getPort();
        String contextPath = env.getProperty("server.servlet.context-path");
        if (contextPath == null) {
            contextPath = "";
        }
        StringBuilder sb = new StringBuilder();
        Set<String> ipSet = getIPs();
        sb.append("\n\n\tApplication is running! Access address:\n");
        sb.append("\n---------------------------------------------------------\n");
        sb.append("\nExplorer:\n");
        sb.append("\tLocal:\t\thttp://localhost:").append(port);
        for (String ip: ipSet) {
            sb.append("\n\tExternal:\thttp://").append(ip).append(":").append(port);
        }
        sb.append("\n");
        sb.append("\nWebdav:\n");
        sb.append("\tLocal:\t\thttp://localhost:").append(port).append("/dav");
        for (String ip: ipSet) {
            sb.append("\n\tExternal:\thttp://").append(ip).append(":").append(port).append("/dav");
        }
        sb.append("\n---------------------------------------------------------\n");

        mTaskScheduler.schedule(() -> log.info(sb.toString()), Instant.now().plusSeconds(5));
    }

    static Set<String> getIPs() throws SocketException {
        Set<String> ipSet = new LinkedHashSet<>();
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(nets)) {
            Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
            for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                if (inetAddress instanceof Inet6Address) {
                    continue;
                }
                if (inetAddress.isAnyLocalAddress()) {
                    continue;
                }
                if (inetAddress.isLoopbackAddress()) {
                    continue;
                }
                if (inetAddress.isLinkLocalAddress()) {
                    continue;
                }
                if (inetAddress.isMulticastAddress()) {
                    continue;
                }
                ipSet.add(inetAddress.getHostAddress());
            }
        }
        return ipSet;
    }
}
