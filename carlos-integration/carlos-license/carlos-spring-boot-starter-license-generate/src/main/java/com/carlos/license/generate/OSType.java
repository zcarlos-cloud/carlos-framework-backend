package com.carlos.license.generate;


import com.carlos.core.exception.ComponentException;
import com.carlos.license.service.AbstractSystemInfoDao;
import com.carlos.license.service.LinuxSystemInfoDao;
import com.carlos.license.service.WindowsSystemInfoDao;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 *   系统类型
 * </p>
 *
 * @author Carlos
 * @date 2025-04-10 13:46 
 */
@AllArgsConstructor
@Getter
public enum OSType {
    WINDOWS("windows", new WindowsSystemInfoDao()),
    LINUX("linux", new LinuxSystemInfoDao()),
    MAC_OS("mac", new LinuxSystemInfoDao());

    private final String tag;

    private final AbstractSystemInfoDao serverInfos;

    public static OSType tagOf(String osName) {
        for (OSType value : OSType.values()) {
            if (osName.startsWith(value.getTag())) {
                return value;
            }
        }
        throw new ComponentException("不支持的操作系统类型:" + osName);
    }
}
