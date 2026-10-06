package com.carlos.system.configration.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.system.configration.convert.SysConfigConvert;
import com.carlos.system.configration.manager.SysConfigManager;
import com.carlos.system.configration.pojo.dto.SysConfigDTO;
import com.carlos.system.configration.pojo.entity.SysConfig;
import com.carlos.system.configration.pojo.excel.SysConfigExcel;
import com.carlos.system.configration.pojo.param.SysConfigPageParam;
import com.carlos.system.configration.pojo.vo.SysConfigLoginPageVO;
import com.carlos.system.enums.ConfigValueType;
import com.carlos.system.enums.SystemErrorCode;
import com.carlos.util.easyexcel.ExcelUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Sets;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * <p>
 * 系统配置 业务接口实现类
 * </p>
 *
 * @author carlos
 * @date 2022-11-3 13:47:55
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigService {

    private final SysConfigManager configManager;

    /**
     * 新增系统配置
     *
     * @param dto 系统配置数据
     * @author carlos
     * @date 2022-11-3 13:47:55
     */

    public void addSysConfig(SysConfigDTO dto) {
        // 检查code是否重复
        List<SysConfigDTO> config = configManager.listByCodes(Sets.newHashSet(dto.getConfigCode()));
        if (CollUtil.isNotEmpty(config)) {
            throw SystemErrorCode.SYS_CONFIG_KEY_ALREADY_EXISTS.exception();
        }
        boolean success = configManager.add(dto);
        if (!success) {
            // 保存失败的应对措施
            return;
        }
        Serializable id = dto.getId();
        // 保存完成的后续业务
    }


    /**
     * 删除系统配置
     *
     * @param ids 系统配置id
     * @author carlos
     * @date 2022-11-3 13:47:55
     */

    public void deleteSysConfig(Set<Long> ids) {
        for (Serializable id : ids) {
            boolean success = configManager.delete(id);
            if (!success) {
                // 删除失败的措施
                continue;
            }
            // 删除成功的后续业务
        }
    }

    /**
     * 修改系统配置信息
     *
     * @param dto 对象信息
     * @author carlos
     * @date 2022-11-3 13:47:55
     */

    public void updateSysConfig(SysConfigDTO dto) {
        boolean success = configManager.modify(dto);
        if (!success) {
            // 修改失败操作
            return;
        }
        // 修改成功的后续操作
    }


    /**
     * 通过code获取配置项
     *
     * @param code 参数0
     * @return com.carlos.system.configration.pojo.dto.SysConfigDTO
     * @author Carlos
     * @date 2022/12/22 13:57
     */

    public SysConfigDTO getByCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        List<SysConfigDTO> dto = configManager.listByCodes(Sets.newHashSet(code));
        if (CollUtil.isEmpty(dto)) {
            return null;
        }
        SysConfigDTO sysConfigDTO = dto.getFirst();
        ConfigValueType type = sysConfigDTO.getValueType();
        if (type == ConfigValueType.FILE) {
            // TODO: Carlos 2022/12/22  如果是文件类型，将文件转换成地址
        }
        return sysConfigDTO;
    }

    private static final String LOGIN_TITLE = "login_page_title";
    private static final String LOGO = "home_logo_image";
    private static final String LOGIN_BACKGROUND_IMG = "login_backgroundImg";
    private static final String TITLE = "browser_title";
    private static final String MAIN_TITLE = "system_main_title";
    private static final String SUB_TITLE = "system_sub_title";
    private static final String VERSION = "system_version";
    private static final String SUPPLIER = "system_supplier";
    private static final String ALL_KEY = "config";
    private static final Set<String> HOME = Sets.newHashSet(LOGIN_TITLE, LOGO, LOGIN_BACKGROUND_IMG, TITLE, MAIN_TITLE, SUB_TITLE, VERSION, SUPPLIER);
    private static final Cache<String, SysConfigLoginPageVO> HOME_CONFIG = Caffeine.newBuilder().maximumSize(1000).expireAfterAccess(30L, TimeUnit.MINUTES).build();
    private static final Cache<String, List<SysConfigDTO>> ALL_CONFIG = Caffeine.newBuilder().maximumSize(10).expireAfterAccess(30L, TimeUnit.MINUTES).build();


    /**
     * 获取系统配置首页配置
     *
     * @return com.carlos.system.configration.pojo.vo.SysConfigLoginPageVO
     * @author carlos
     * @date 2022-11-3 13:47:55
     */

    public SysConfigLoginPageVO getHomePageConfig() {
        SysConfigLoginPageVO login = HOME_CONFIG.getIfPresent(LOGIN_TITLE);
        if (login != null) {
            return login;
        }
        List<SysConfigDTO> config = configManager.listByCodes(HOME);
        if (CollUtil.isEmpty(config)) {
            return null;
        }

        login = new SysConfigLoginPageVO();
        for (SysConfigDTO item : config) {
            switch (item.getConfigCode()) {
                case LOGIN_TITLE:
                    login.setLoginTitle(item.getConfigValue());
                    break;
                case LOGO:
                    login.setLogo(item.getConfigValue());
                    break;
                case LOGIN_BACKGROUND_IMG:
                    login.setBackgroundImg(item.getConfigValue());
                    break;
                case TITLE:
                    login.setTitle(item.getConfigValue());
                    break;
                case MAIN_TITLE:
                    login.setMainTitle(item.getConfigValue());
                    break;
                case SUB_TITLE:
                    login.setSubTitle(item.getConfigValue());
                    break;
                case VERSION:
                    login.setVersion(item.getConfigValue());
                    break;
                case SUPPLIER:
                    login.setSupplier(item.getConfigValue());
                    break;
                default:
                    break;
            }
        }
        HOME_CONFIG.put(LOGIN_TITLE, login);
        return login;
    }

    /**
     * 获取系统配置列表
     *
     * @return java.util.List<com.carlos.system.configration.pojo.dto.SysConfigDTO>
     * @throws
     * @author Carlos
     * @date 2025-12-24 12:57
     */

    public List<SysConfigDTO> listConfig() {
        List<SysConfigDTO> configs = ALL_CONFIG.getIfPresent(ALL_KEY);
        if (CollUtil.isNotEmpty(configs)) {
            return configs;
        }
        Set<String> sets = new HashSet<>(HOME);
        sets.remove(TITLE);
        configs = configManager.getAllConfig(sets);
        ALL_CONFIG.put(ALL_KEY, configs);
        return configs;
    }

    /**
     * 导出系统配置
     *
     * <p>
     * 按传入的查询条件导出。此前无论前端怎么筛选，导出走的都是
     * {@code configManager.list()}（整表），「先筛选再导出」是个空动作。
     * </p>
     *
     * @param param      查询条件（与列表页共用）
     * @param response   HttpServletResponse
     * @param isTemplate 是否为模板
     * @author carlos
     */

    public void export(SysConfigPageParam param, HttpServletResponse response, boolean isTemplate) {
        String name = "系统配置--" + DateUtil.format(new Date(), DatePattern.PURE_DATETIME_PATTERN);
        List<SysConfigExcel> data = CollUtil.newArrayList();
        if (!isTemplate) {
            // 与 getPage 共用同一份过滤条件，保证导出的就是用户看到的那批
            List<SysConfigDTO> dtoList = configManager.listByParam(param);
            data = SysConfigConvert.INSTANCE.dtoToExcel(dtoList);
        }
        try {
            ExcelUtil.download(response, name, SysConfigExcel.class, data);
        } catch (Exception e) {
            throw CommonErrorCode.BUSINESS_ERROR.exception("系统配置导出失败");
        }
    }

    /**
     * 导入系统配置
     *
     * @param configs 系统配置列表
     * @author carlos
     */

    @Transactional(rollbackFor = Exception.class)
    public void importConfigs(List<SysConfigDTO> configs) {
        if (CollUtil.isEmpty(configs)) {
            return;
        }
        for (SysConfigDTO dto : configs) {
            if (StrUtil.isBlank(dto.getConfigCode())) {
                continue;
            }
            List<SysConfigDTO> existList = configManager.listByCodes(Sets.newHashSet(dto.getConfigCode()));
            if (CollUtil.isNotEmpty(existList)) {
                // 已存在，更新
                SysConfigDTO exist = existList.getFirst();
                dto.setId(exist.getId());
                boolean success = configManager.modify(dto);
                if (!success) {
                    log.warn("Update config failed, code:{}", dto.getConfigCode());
                }
            } else {
                // 不存在，新增
                addSysConfig(dto);
            }
        }
        // 清空缓存
        HOME_CONFIG.invalidateAll();
        ALL_CONFIG.invalidateAll();
    }
}
