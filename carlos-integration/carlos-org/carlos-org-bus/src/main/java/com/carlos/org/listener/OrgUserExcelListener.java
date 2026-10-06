package com.carlos.org.listener;

import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import com.carlos.org.pojo.excel.OrgUserExcel;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 用户导入 Excel 监听器
 * </p>
 *
 * @author Carlos
 * @date 2026年5月19日
 */
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class OrgUserExcelListener extends AnalysisEventListener<OrgUserExcel> {

    private final List<OrgUserExcel> dataList = new ArrayList<>();

    public List<OrgUserExcel> getDataList() {
        return dataList;
    }

    @Override
    public void invoke(OrgUserExcel data, AnalysisContext context) {
        dataList.add(data);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("用户导入 Excel 解析完成，共 {} 条数据", dataList.size());
    }

}
