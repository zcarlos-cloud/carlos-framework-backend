package com.carlos.system.dict.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.system.dict.convert.SysDictConvert;
import com.carlos.system.dict.convert.SysDictItemConvert;
import com.carlos.system.dict.manager.SysDictItemManager;
import com.carlos.system.dict.manager.SysDictManager;
import com.carlos.system.dict.pojo.dto.SysDictDTO;
import com.carlos.system.dict.pojo.dto.SysDictItemDTO;
import com.carlos.system.dict.pojo.param.SysDictCreateParam;
import com.carlos.system.dict.pojo.param.SysDictCreateParam.Item;
import com.carlos.system.dict.pojo.param.SysDictUpdateParam;
import com.carlos.system.enums.SystemErrorCode;
import com.carlos.system.sequence.service.SysSequenceRuleService;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;


/**
 * <p>
 * 系统字典 业务接口实现类
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictService {

    private final SysDictManager dictManager;

    private final SysDictItemManager dictItemManager;

    private final SysDictItemService itemService;

    private final SysSequenceRuleService sequenceRuleService;

    /**
     * txt 导入字典时使用的编码规则（对应 {@code sys_sequence_rule.rule_code}）
     *
     * <p>规则须先存在，建规则 SQL 见 {@code docs/sql/sys_sequence_rule_dict_code.sql}；
     * 规则缺失时 {@link SysSequenceRuleService#nextCode(String)} 会抛
     * {@code SYS_SEQUENCE_RULE_NOT_FOUND}，导入整体失败 —— 这是刻意的，
     * 比起退回「自己从某个魔数开始递增」，明确报错更容易被发现。</p>
     */
    private static final String DICT_CODE_RULE = "dict_code";

    /**
     * 新增字典
     *
     * @param dto 字典数据
     * @return boolean
     * @author carlos
     * @date 2021/11/30 10:28
     */

    @Transactional(rollbackFor = Exception.class)
    public boolean addDict(SysDictDTO dto, List<SysDictCreateParam.Item> items) {
        if (dictManager.count(dto.getDictCode(), null, null) > 0) {
            throw SystemErrorCode.SYS_DICT_ALREADY_EXISTS.exception("字典编码不可重复");
        }
        if (dictManager.count(null, dto.getDictName(), null) > 0) {
            throw SystemErrorCode.SYS_DICT_ALREADY_EXISTS.exception("字典名称不可重复");
        }
        boolean save = dictManager.add(dto);
        if (save) {
            // 保存字典选项
            if (CollectionUtil.isNotEmpty(items)) {
                itemService.addDictItem(dto.getId(), SysDictItemConvert.INSTANCE.create2dto(items));
            }
        }
        return true;
    }

    /**
     * 修改字典信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author carlos
     * @date 2021/11/30 16:59
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean update(SysDictDTO dto, List<SysDictUpdateParam.Item> items) {
        Serializable dictId = dto.getId();
        SysDictDTO dict = dictManager.getDictById(dictId);
        if (dict == null) {
            throw SystemErrorCode.SYS_DICT_NOT_FOUND.exception();
        }

        if (dictManager.count(null, dto.getDictName(), dto.getId()) > 0) {
            throw SystemErrorCode.SYS_DICT_ALREADY_EXISTS.exception("字典名称不可重复");
        }
        boolean success = dictManager.updateById(SysDictConvert.INSTANCE.toDO(dto));
        if (success) {
            // 保存字典选项
            if (CollectionUtil.isEmpty(items)) {
                return true;
            }
            dto.setDictCode(dict.getDictCode());
            List<SysDictItemDTO> dtos = SysDictItemConvert.INSTANCE.update2dto(items);
            itemService.saveOrUpdate(dto, dtos);

        }
        return true;
    }

    /**
     * 删除字典
     *
     * @param ids 字典id
     * @return boolean
     * @author carlos
     * @date 2021/11/30 11:03
     */

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDict(Set<Long> ids) {
        List<SysDictDTO> dicts = dictManager.listByDictIds(ids);
        if (dictManager.removeBatchByIds(ids)) {
            log.info("删除字典: {}", dicts);
        }
        for (SysDictDTO dict : dicts) {
            itemService.deleteByDict(dict);
        }
        return true;
    }

    /**
     * 根据名称获取下拉列表
     *
     * @param name 字典名称
     * @return com.carlos.dict.pojo.dto.DictDTO
     */
    public List<SysDictDTO> getListByName(String name) {
        List<SysDictDTO> list = this.dictManager.listDict(name);
        for (SysDictDTO dict : list) {
            List<SysDictItemDTO> items = itemService.getItemCache(dict.getDictCode());
            items.sort(Comparator.comparing(SysDictItemDTO::getOrderNum));
            dict.setItems(items);
        }
        return list;
    }

    /**
     * 获取字典详情
     *
     * @param id 参数0
     * @return com.carlos.system.dict.pojo.dto.DictDTO
     * @author Carlos
     * @date 2022/12/7 23:54
     */
    public SysDictDTO getDetail(Long id) {
        SysDictDTO dict = this.dictManager.getDictById(id);
        if (dict == null) {
            throw SystemErrorCode.SYS_DICT_NOT_FOUND.exception();
        }
        List<SysDictItemDTO> items = dictItemManager.listItems(Sets.newHashSet(dict.getId()), null, false);
        dict.setItems(items);
        return dict;
    }

    /**
     * 使用文本初始化字典
     *
     * <p>字典编码取自序列号生成器（规则 {@value #DICT_CODE_RULE}），不再从写死的魔数递增 ——
     * 原先的 {@code int dictCode = 25} 与库中存量毫无关联，换一台环境或漏改一次就是重号。
     * 改造后序号由 Redis 计数承载，生成器发出的编码不会与它自己发出的重复；但手工在字典页
     * 填写的编码仍可能落在后续生成值上，那种冲突出 {@link #addDict} 的两条 count 检查兜住，
     * 表现为报错而非静默重号。</p>
     *
     * <p>整个导入放在一个事务里：循环体逐条调用 {@link #addDict} 是**自调用**，
     * {@code addDict} 上的 {@code @Transactional} 不生效，没有这一层时第 N 个字典失败会留下
     * 前 N-1 个已落库的半截状态 —— 而重跑又会从第一个就撞唯一约束，半截状态会黏住。
     * 注意序列号已消耗的部分不随回滚归还（Redis 计数不在事务内），跳号是既定语义。</p>
     *
     * @param path 文本路径
     * @author Carlos
     * @date 2023/4/6 19:32
     */
    @Transactional(rollbackFor = Exception.class)
    public void initWithTxt(String path) {
        List<String> lines = FileUtil.readUtf8Lines(path);
        SysDictDTO dict = null;
        List<SysDictCreateParam.Item> items = null;
        int sort = 1;
        for (String line : lines) {
            if (StrUtil.isBlank(line)) {
                if (dict != null) {
                    addDict(dict, items);
                    log.info("dict: {}", dict);
                    log.info("items: {}", items);
                    dict = null;
                }
                continue;
            }
            List<String> words = StrUtil.split(line, StrUtil.DASHED);
            if (words.size() == 1) {
                dict = new SysDictDTO();
                items = new ArrayList<>();
                sort = 1;
                dict.setDictName(words.getFirst());
                dict.setDescription(words.getFirst());
                dict.setDictCode(this.sequenceRuleService.nextCode(DICT_CODE_RULE));
                continue;
            }
            Item item = new Item();
            item.setItemName(words.get(1));
            item.setItemCode(words.getFirst());
            item.setDescription(words.get(1));
            item.setOrderNum(sort);
            sort++;
            items.add(item);
        }
    }
}
