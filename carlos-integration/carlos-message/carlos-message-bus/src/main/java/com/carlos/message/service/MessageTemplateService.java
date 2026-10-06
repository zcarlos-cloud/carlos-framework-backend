package com.carlos.message.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.message.enums.MessageErrorCode;
import com.carlos.message.manager.MessageTemplateManager;
import com.carlos.message.pojo.dto.MessageTemplateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;


/**
 * <p>
 * 消息模板 业务
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageTemplateService {

    private final MessageTemplateManager templateManager;

    /**
     * 新增消息模板
     *
     * @param dto 消息模板数据
     * @author Carlos
     * @date 2026年3月12日 上午11:17:06
     */
    public void addMessageTemplate(MessageTemplateDTO dto) {
        validateTemplateCodeUnique(dto.getTemplateCode());
        boolean success = templateManager.add(dto);
        if (!success) {
            // 保存失败的应对措施
            return;
        }
        Serializable id = dto.getId();
        log.info("Insert 'MessageTemplate' data: id:{}", id);
        // 保存完成的后续业务
    }

    /**
     * 删除消息模板
     *
     * @param ids 消息模板id
     * @author Carlos
     * @date 2026年3月12日 上午11:17:06
     */
    public void deleteMessageTemplate(Set<Long> ids) {
        for (Serializable id : ids) {
            boolean success = templateManager.delete(id);
            if (!success) {
                // 删除失败的措施
                continue;
            }
            // 删除成功的后续业务
        }
    }

    /**
     * 修改消息模板信息
     *
     * @param dto 对象信息
     * @author Carlos
     * @date 2026年3月12日 上午11:17:06
     */
    public void updateMessageTemplate(MessageTemplateDTO dto) {
        boolean success = templateManager.modify(dto);
        if (!success) {
            // 修改失败操作
            return;
        }
        // 修改成功的后续操作
        log.info("Update 'MessageTemplate' data: id:{}", dto.getId());
    }

    /**
     * 根据模板编码查询消息模板
     *
     * @param templateCode 模板编码
     * @return 消息模板DTO
     * @author Carlos
     * @date 2026年3月12日
     */
    public MessageTemplateDTO getByTemplateCode(String templateCode) {
        if (StrUtil.isBlank(templateCode)) {
            throw MessageErrorCode.MSG_PARAM_TEMPLATE_CODE_EMPTY.exception();
        }
        MessageTemplateDTO dto = templateManager.getByTemplateCode(templateCode);
        if (dto == null) {
            throw MessageErrorCode.MSG_TEMPLATE_NOT_FOUND.exception("消息模板不存在: " + templateCode);
        }
        return dto;
    }

    /**
     * 发布消息模板（草稿->启用）
     *
     * @param id 主键ID
     * @author Carlos
     * @date 2026年3月12日
     */
    public void publishTemplate(Serializable id) {
        MessageTemplateDTO existing = templateManager.getDtoById(id);
        if (existing == null) {
            throw MessageErrorCode.MSG_TEMPLATE_NOT_FOUND.exception();
        }
        if (existing.getEnabled() != null && existing.getEnabled()) {
            throw MessageErrorCode.MSG_TEMPLATE_ALREADY_EXISTS.exception("消息模板已处于发布状态");
        }
        boolean success = templateManager.publish(id);
        if (!success) {
            throw CommonErrorCode.BUSINESS_ERROR.exception("发布消息模板失败");
        }
        log.info("Publish MessageTemplate: id:{}", id);
    }

    /**
     * 启用消息模板
     *
     * @param id 主键ID
     * @author Carlos
     * @date 2026年3月12日
     */
    public void enableTemplate(Serializable id) {
        MessageTemplateDTO existing = templateManager.getDtoById(id);
        if (existing == null) {
            throw MessageErrorCode.MSG_TEMPLATE_NOT_FOUND.exception();
        }
        if (existing.getEnabled() != null && existing.getEnabled()) {
            throw MessageErrorCode.MSG_TEMPLATE_ALREADY_EXISTS.exception("消息模板已处于启用状态");
        }
        boolean success = templateManager.updateStatus(id, Boolean.TRUE);
        if (!success) {
            throw CommonErrorCode.BUSINESS_ERROR.exception("启用消息模板失败");
        }
        log.info("Enable MessageTemplate: id:{}", id);
    }

    /**
     * 禁用消息模板
     *
     * @param id 主键ID
     * @author Carlos
     * @date 2026年3月12日
     */
    public void disableTemplate(Serializable id) {
        MessageTemplateDTO existing = templateManager.getDtoById(id);
        if (existing == null) {
            throw MessageErrorCode.MSG_TEMPLATE_NOT_FOUND.exception();
        }
        if (existing.getEnabled() != null && !existing.getEnabled()) {
            throw MessageErrorCode.MSG_TEMPLATE_DISABLED.exception("消息模板已处于禁用状态");
        }
        boolean success = templateManager.updateStatus(id, Boolean.FALSE);
        if (!success) {
            throw CommonErrorCode.BUSINESS_ERROR.exception("禁用消息模板失败");
        }
        log.info("Disable MessageTemplate: id:{}", id);
    }

    /**
     * 验证模板参数
     *
     * @param templateCode 模板编码
     * @param params 参数Map
     * @author Carlos
     * @date 2026年3月12日
     */
    public void validateTemplateParams(String templateCode, Map<String, Object> params) {
        if (StrUtil.isBlank(templateCode)) {
            throw MessageErrorCode.MSG_PARAM_TEMPLATE_CODE_EMPTY.exception();
        }
        MessageTemplateDTO template = getByTemplateCode(templateCode);
        String paramSchema = template.getParamSchema();
        if (StrUtil.isBlank(paramSchema)) {
            return;
        }
        JSONObject schemaJson;
        try {
            schemaJson = JSONUtil.parseObj(paramSchema);
        } catch (Exception e) {
            log.error("Parse param_schema fail, templateCode: {}, schema: {}", templateCode, paramSchema, e);
            throw MessageErrorCode.MSG_TEMPLATE_PARAM_INVALID.exception();
        }
        if (params == null) {
            throw MessageErrorCode.MSG_TEMPLATE_PARAM_INVALID.exception("模板参数不能为空");
        }
        for (String key : schemaJson.keySet()) {
            if (!params.containsKey(key)) {
                throw MessageErrorCode.MSG_TEMPLATE_PARAM_INVALID.exception("缺少模板参数: " + key);
            }
        }
    }

    /**
     * 新增时校验templateCode唯一性
     *
     * @param templateCode 模板编码
     * @author Carlos
     * @date 2026年3月12日
     */
    public void validateTemplateCodeUnique(String templateCode) {
        if (StrUtil.isBlank(templateCode)) {
            throw MessageErrorCode.MSG_PARAM_TEMPLATE_CODE_EMPTY.exception();
        }
        MessageTemplateDTO existing = templateManager.getByTemplateCode(templateCode);
        if (existing != null) {
            throw MessageErrorCode.MSG_TEMPLATE_ALREADY_EXISTS.exception("模板编码已存在: " + templateCode);
        }
    }
}
