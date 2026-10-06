package com.carlos.system.util;

import cn.hutool.extra.spring.SpringUtil;
import com.carlos.core.base.Dict;
import com.carlos.core.base.RegionInfo;
import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.Result;
import com.carlos.system.api.ApiDict;
import com.carlos.system.api.ApiRegion;
import com.carlos.system.api.ApiSequence;
import com.carlos.system.api.ApiSysMenu;
import com.carlos.system.api.ApiSysNews;
import com.carlos.system.api.ApiSystemConfig;
import com.carlos.system.enums.SystemErrorCode;
import com.carlos.system.pojo.ao.DictItemAO;
import com.carlos.system.pojo.ao.SequenceBatchAO;
import com.carlos.system.pojo.ao.SysConfigAO;
import com.carlos.system.pojo.ao.SysMenuAO;
import com.carlos.system.pojo.ao.SysNewsAO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SystemUtil 单元测试
 *
 * <p>覆盖三类关注点：</p>
 * <ol>
 *   <li><b>接线</b>：每个方法对应的 Api 接口、方法名、透传参数是否正确 ——
 *       这类缺陷（错接 API / 错传参）编译期不报错，只会在联调时才暴露；</li>
 *   <li><b>拆包规则</b>：读接口「查不到」返回 null、序列号空值必须抛异常
 *       （这条是防「空单号静默写库」的核心不变量）；</li>
 *   <li><b>失败路径</b>：远端异常原样上抛不转换（通道 A 的真实形态）、
 *       {@code Result.success=false} 的防御分支、Bean 取不到与上下文未就绪两条排查路径。</li>
 * </ol>
 *
 * @author carlos
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SystemUtil 静态门面")
class SystemUtilTest {

    @Mock
    private ApiSequence apiSequence;

    @Mock
    private ApiDict apiDict;

    @Mock
    private ApiSystemConfig apiSystemConfig;

    @Mock
    private ApiSysMenu apiSysMenu;

    @Mock
    private ApiSysNews apiSysNews;

    @Mock
    private ApiRegion apiRegion;

    /**
     * 打桩 SpringUtil，使 api() 的上下文就绪判空通过
     */
    private MockedStatic<SpringUtil> stubContextReady() {
        MockedStatic<SpringUtil> mocked = mockStatic(SpringUtil.class);
        mocked.when(SpringUtil::getApplicationContext).thenReturn(mock(ApplicationContext.class));
        return mocked;
    }

    // ==================== 序列号：成功路径与空值不变量 ====================

    @Test
    @DisplayName("生成器返回正常编码时原样返回")
    void should_return_code_when_generator_succeeds() {
        try (MockedStatic<SpringUtil> mocked = stubContextReady()) {
            mocked.when(() -> SpringUtil.getBean(ApiSequence.class)).thenReturn(apiSequence);
            when(apiSequence.next("order_no")).thenReturn(Result.success("PO20260927-0001"));

            assertThat(SystemUtil.nextCode("order_no")).isEqualTo("PO20260927-0001");
        }
    }

    @Test
    @DisplayName("生成器返回空白编码时抛异常，绝不返回 null")
    void should_throw_when_generator_returns_blank_code() {
        try (MockedStatic<SpringUtil> mocked = stubContextReady()) {
            mocked.when(() -> SpringUtil.getBean(ApiSequence.class)).thenReturn(apiSequence);
            when(apiSequence.next("order_no")).thenReturn(Result.success("   "));

            assertThatThrownBy(() -> SystemUtil.nextCode("order_no"))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo("50908");
                    assertThat(e.getMessage()).contains("空编码");
                });
        }
    }

    @Test
    @DisplayName("生成器返回 null 编码时抛异常")
    void should_throw_when_generator_returns_null_code() {
        try (MockedStatic<SpringUtil> mocked = stubContextReady()) {
            mocked.when(() -> SpringUtil.getBean(ApiSequence.class)).thenReturn(apiSequence);
            when(apiSequence.next("order_no")).thenReturn(Result.success(null));

            assertThatThrownBy(() -> SystemUtil.nextCode("order_no"))
                .isInstanceOfSatisfying(BusinessException.class,
                    e -> assertThat(e.getErrorCode()).isEqualTo("50908"));
        }
    }

    @Test
    @DisplayName("批量预取返回空号段时抛异常")
    void should_throw_when_batch_is_null() {
        try (MockedStatic<SpringUtil> mocked = stubContextReady()) {
            mocked.when(() -> SpringUtil.getBean(ApiSequence.class)).thenReturn(apiSequence);
            when(apiSequence.nextBatch("order_no", 10)).thenReturn(Result.success(null));

            assertThatThrownBy(() -> SystemUtil.nextCodeBatch("order_no", 10))
                .isInstanceOfSatisfying(BusinessException.class,
                    e -> assertThat(e.getErrorCode()).isEqualTo("50908"));
        }
    }

    @Test
    @DisplayName("批量预取段首/段尾为空时同样抛异常（与单号同一条不变量）")
    void should_throw_when_batch_boundary_is_blank() {
        try (MockedStatic<SpringUtil> mocked = stubContextReady()) {
            mocked.when(() -> SpringUtil.getBean(ApiSequence.class)).thenReturn(apiSequence);
            SequenceBatchAO batch = new SequenceBatchAO();
            batch.setStartCode("PO-0001");
            batch.setEndCode("  ");
            when(apiSequence.nextBatch("order_no", 10)).thenReturn(Result.success(batch));

            assertThatThrownBy(() -> SystemUtil.nextCodeBatch("order_no", 10))
                .isInstanceOfSatisfying(BusinessException.class,
                    e -> assertThat(e.getErrorCode()).isEqualTo("50908"));
        }
    }

    @Test
    @DisplayName("批量预取正常时原样返回号段")
    void should_return_batch_when_generator_succeeds() {
        SequenceBatchAO batch = new SequenceBatchAO();
        batch.setStartCode("PO-0001");
        batch.setEndCode("PO-0010");

        try (MockedStatic<SpringUtil> mocked = stubContextReady()) {
            mocked.when(() -> SpringUtil.getBean(ApiSequence.class)).thenReturn(apiSequence);
            when(apiSequence.nextBatch("order_no", 10)).thenReturn(Result.success(batch));

            assertThat(SystemUtil.nextCodeBatch("order_no", 10)).isSameAs(batch);
        }
    }

    // ==================== 读接口：接线 + 「查不到返回 null」 ====================

    @Test
    @DisplayName("getDictItemById 透传 id 并按当前语义返回")
    void should_delegate_get_dict_item_by_id() {
        Dict expected = mock(Dict.class);
        try (MockedStatic<SpringUtil> mocked = stubContextReady()) {
            mocked.when(() -> SpringUtil.getBean(ApiDict.class)).thenReturn(apiDict);
            when(apiDict.getById(7L)).thenReturn(Result.success(expected));

            assertThat(SystemUtil.getDictItemById(7L)).isSameAs(expected);
            verify(apiDict).getById(7L);
        }
    }

    @Test
    @DisplayName("读接口「查不到」返回 null —— success + null data 是正常语义")
    void should_return_null_when_dict_item_not_found() {
        try (MockedStatic<SpringUtil> mocked = stubContextReady()) {
            mocked.when(() -> SpringUtil.getBean(ApiDict.class)).thenReturn(apiDict);
            when(apiDict.getByCode("not_exist")).thenReturn(Result.success(null));

            assertThat(SystemUtil.getDictItemByCode("not_exist")).isNull();
        }
    }

    @Test
    @DisplayName("listDictItems 透传 dictCode")
    void should_delegate_list_dict_items() {
        List<DictItemAO> expected = List.of(new DictItemAO());
        try (MockedStatic<SpringUtil> mocked = stubContextReady()) {
            mocked.when(() -> SpringUtil.getBean(ApiDict.class)).thenReturn(apiDict);
            when(apiDict.list("user_status")).thenReturn(Result.success(expected));

            assertThat(SystemUtil.listDictItems("user_status")).isSameAs(expected);
            verify(apiDict).list("user_status");
        }
    }

    @Test
    @DisplayName("getConfigByCode 透传 configCode")
    void should_delegate_get_config_by_code() {
        SysConfigAO expected = new SysConfigAO();
        try (MockedStatic<SpringUtil> mocked = stubContextReady()) {
            mocked.when(() -> SpringUtil.getBean(ApiSystemConfig.class)).thenReturn(apiSystemConfig);
            when(apiSystemConfig.getByCode("sys.name")).thenReturn(Result.success(expected));

            assertThat(SystemUtil.getConfigByCode("sys.name")).isSameAs(expected);
            verify(apiSystemConfig).getByCode("sys.name");
        }
    }

    @Test
    @DisplayName("getMenuById 透传 menuId")
    void should_delegate_get_menu_by_id() {
        SysMenuAO expected = new SysMenuAO();
        try (MockedStatic<SpringUtil> mocked = stubContextReady()) {
            mocked.when(() -> SpringUtil.getBean(ApiSysMenu.class)).thenReturn(apiSysMenu);
            when(apiSysMenu.getMenuById(3L)).thenReturn(Result.success(expected));

            assertThat(SystemUtil.getMenuById(3L)).isSameAs(expected);
            verify(apiSysMenu).getMenuById(3L);
        }
    }

    @Test
    @DisplayName("getNewsById 透传 newsId")
    void should_delegate_get_news_by_id() {
        SysNewsAO expected = new SysNewsAO();
        try (MockedStatic<SpringUtil> mocked = stubContextReady()) {
            mocked.when(() -> SpringUtil.getBean(ApiSysNews.class)).thenReturn(apiSysNews);
            when(apiSysNews.getById(9L)).thenReturn(Result.success(expected));

            assertThat(SystemUtil.getNewsById(9L)).isSameAs(expected);
            verify(apiSysNews).getById(9L);
        }
    }

    @Test
    @DisplayName("getRegionInfo 透传 regionCode 与 limit")
    void should_delegate_get_region_info() {
        RegionInfo expected = mock(RegionInfo.class);
        try (MockedStatic<SpringUtil> mocked = stubContextReady()) {
            mocked.when(() -> SpringUtil.getBean(ApiRegion.class)).thenReturn(apiRegion);
            when(apiRegion.getRegionInfo("330000", 2)).thenReturn(Result.success(expected));

            assertThat(SystemUtil.getRegionInfo("330000", 2)).isSameAs(expected);
            verify(apiRegion).getRegionInfo("330000", 2);
        }
    }

    // ==================== 失败路径 ====================

    @Test
    @DisplayName("远端异常原样上抛、不被本类转换（通道 A 的真实形态）")
    void should_propagate_remote_exception_unchanged() {
        IllegalStateException remote = new IllegalStateException("openfeign调用异常");
        try (MockedStatic<SpringUtil> mocked = stubContextReady()) {
            mocked.when(() -> SpringUtil.getBean(ApiSequence.class)).thenReturn(apiSequence);
            when(apiSequence.next("order_no")).thenThrow(remote);

            assertThatThrownBy(() -> SystemUtil.nextCode("order_no")).isSameAs(remote);
        }
    }

    @Test
    @DisplayName("防御分支：Result.success=false 时抛异常并保留远端错误码")
    void should_throw_and_keep_remote_code_when_result_is_not_success() {
        try (MockedStatic<SpringUtil> mocked = stubContextReady()) {
            mocked.when(() -> SpringUtil.getBean(ApiSequence.class)).thenReturn(apiSequence);
            when(apiSequence.next("order_no"))
                .thenReturn(Result.error(SystemErrorCode.SYS_SEQUENCE_RULE_NOT_FOUND));

            assertThatThrownBy(() -> SystemUtil.nextCode("order_no"))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo("50911");
                    assertThat(e.getMessage()).contains("20981");
                });
        }
    }

    @Test
    @DisplayName("远端返回 null 响应（通道 B 的极端形态）时抛异常")
    void should_throw_when_result_is_null() {
        try (MockedStatic<SpringUtil> mocked = stubContextReady()) {
            mocked.when(() -> SpringUtil.getBean(ApiDict.class)).thenReturn(apiDict);
            when(apiDict.getByCode("user_status")).thenReturn(null);

            assertThatThrownBy(() -> SystemUtil.getDictItemByCode("user_status"))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo("50911");
                    assertThat(e.getMessage()).contains("空响应");
                });
        }
    }

    @Test
    @DisplayName("Feign 客户端未装配时抛异常并给出扫描范围排查指引")
    void should_hint_when_feign_client_missing() {
        try (MockedStatic<SpringUtil> mocked = stubContextReady()) {
            mocked.when(() -> SpringUtil.getBean(ApiDict.class))
                .thenThrow(new NoSuchBeanDefinitionException(ApiDict.class));

            assertThatThrownBy(() -> SystemUtil.getDictItemByCode("user_status"))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo("50910");
                    assertThat(e.getMessage()).contains("EnableFeignClients");
                });
        }
    }

    @Test
    @DisplayName("Spring 上下文未就绪时抛异常而不是 NPE")
    void should_throw_when_context_not_ready() {
        try (MockedStatic<SpringUtil> mocked = mockStatic(SpringUtil.class)) {
            mocked.when(SpringUtil::getApplicationContext).thenReturn(null);

            assertThatThrownBy(() -> SystemUtil.getDictItemByCode("user_status"))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo("50910");
                    assertThat(e.getMessage()).contains("上下文尚未就绪");
                });
        }
    }
}
