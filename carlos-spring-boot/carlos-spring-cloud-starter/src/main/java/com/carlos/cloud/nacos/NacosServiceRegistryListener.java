package com.carlos.cloud.nacos;

import com.alibaba.cloud.nacos.registry.NacosRegistration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Nacos 服务注册监听器
 * </p>
 *
 * <p>
 * 在服务注册时自动添加元数据信息，如版本号、启动时间、主机信息等
 * </p>
 *
 * @author carlos
 * @date 2024/01/15
 */
@Slf4j
@RequiredArgsConstructor
public class NacosServiceRegistryListener implements ApplicationListener<WebServerInitializedEvent> {

    private final NacosRegistration nacosRegistration;

    private final Environment environment;

    @Override
    public void onApplicationEvent(@NonNull WebServerInitializedEvent event) {
        try {
            Map<String, String> metadata = new HashMap<>();

            // 添加应用版本号
            String version = environment.getProperty("carlos.application.version", "1.0.0");
            metadata.put("version", version);

            // 添加启动时间
            metadata.put("startup.time", String.valueOf(System.currentTimeMillis()));

            // 添加服务端口
            metadata.put("server.port", String.valueOf(event.getWebServer().getPort()));

            // 添加主机信息
            String hostAddress = getHostAddress();
            metadata.put("host.address", hostAddress);

            // 添加 Spring Profiles
            String[] profiles = environment.getActiveProfiles();
            if (profiles.length > 0) {
                metadata.put("spring.profiles", String.join(",", profiles));
            }

            // 添加服务描述
            String description = environment.getProperty("spring.application.description", "");
            if (!description.isEmpty()) {
                metadata.put("description", description);
            }

            // 添加到 Nacos 注册信息
            if (nacosRegistration != null) {
                Map<String, String> existingMetadata = nacosRegistration.getMetadata();
                if (existingMetadata != null) {
                    existingMetadata.putAll(metadata);
                }
                log.info("Nacos 服务元数据已更新: {}", metadata);
            }
        } catch (Exception e) {
            log.error("Nacos 服务注册监听处理失败", e);
        }
    }

    /**
     * 获取本机 IP 地址
     */
    private String getHostAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address.isLoopbackAddress()) {
                        continue;
                    }
                    String hostAddress = address.getHostAddress();
                    // 优先返回 IPv4 地址
                    if (hostAddress.indexOf(':') == -1) {
                        return hostAddress;
                    }
                }
            }
        } catch (SocketException e) {
            log.error("获取本机 IP 地址失败", e);
        }
        return "unknown";
    }
}
