package com.carlos.system.dict.service;

import com.carlos.core.exception.BusinessException;
import com.carlos.system.dict.manager.SysDictItemManager;
import com.carlos.system.dict.manager.SysDictManager;
import com.carlos.system.dict.pojo.dto.SysDictDTO;
import com.carlos.system.enums.SystemErrorCode;
import com.carlos.system.sequence.service.SysSequenceRuleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysDictService#initWithTxt 单元测试
 *
 * <p>盯的是「字典编码改为取自序列号生成器」这一处行为变更：原先它用写死的
 * {@code int dictCode = 25} 在内存里递增，与库中存量毫无关联。这里断言编码确实来自
 * {@code nextCode("dict_code")}，并且**规则缺失时整个导入直接失败**（而不是退回某个魔数继续跑）。</p>
 *
 * <p>测试数据用 {@link TempDir} 现写现读：该方法的入参是文件路径，不能假设运行环境里存在某个 txt。</p>
 *
 * @author carlos
 */
@ExtendWith(MockitoExtension.class)
class SysDictServiceInitWithTxtTest {

    /**
     * 与 {@code SysDictService.DICT_CODE_RULE} 对应
     */
    private static final String DICT_CODE_RULE = "dict_code";

    /**
     * 两段字典：每段一行单字段（字典名），空行触发落库
     */
    private static final String TWO_DICTS_TXT = "字典A\n\n字典B\n\n";

    @Mock
    private SysDictManager dictManager;

    @Mock
    private SysDictItemManager dictItemManager;

    @Mock
    private SysDictItemService itemService;

    @Mock
    private SysSequenceRuleService sequenceRuleService;

    @InjectMocks
    private SysDictService dictService;

    @TempDir
    private Path tempDir;

    @Test
    @DisplayName("字典编码取自序列号生成器，而非从魔数递增")
    void should_take_dict_code_from_sequence_generator() throws IOException {
        when(sequenceRuleService.nextCode(DICT_CODE_RULE)).thenReturn("025", "026");

        dictService.initWithTxt(writeTxt(TWO_DICTS_TXT));

        ArgumentCaptor<SysDictDTO> captor = ArgumentCaptor.forClass(SysDictDTO.class);
        verify(dictManager, times(2)).add(captor.capture());

        List<String> codes = captor.getAllValues().stream().map(SysDictDTO::getDictCode).toList();
        assertEquals(List.of("025", "026"), codes);
        verify(sequenceRuleService, times(2)).nextCode(DICT_CODE_RULE);
    }

    @Test
    @DisplayName("规则缺失时立即失败，不落任何字典")
    void should_fail_fast_when_rule_missing() throws IOException {
        when(sequenceRuleService.nextCode(DICT_CODE_RULE))
            .thenThrow(SystemErrorCode.SYS_SEQUENCE_RULE_NOT_FOUND.exception());

        String path = writeTxt(TWO_DICTS_TXT);

        assertThrows(BusinessException.class, () -> dictService.initWithTxt(path));

        // 关键：第一个字典就取不到编码 ⇒ 一条都不该写库，避免半截导入
        verify(dictManager, never()).add(any());
    }

    /**
     * 把内容写成临时 txt 并返回路径
     */
    private String writeTxt(String content) throws IOException {
        Path file = tempDir.resolve("dict.txt");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file.toString();
    }
}
