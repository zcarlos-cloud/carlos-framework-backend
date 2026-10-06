package com.carlos.system.region.listener;


import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.exception.ExcelDataConvertException;
import cn.idev.excel.metadata.data.CellData;
import cn.idev.excel.read.listener.ReadListener;
import cn.idev.excel.util.ListUtils;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.system.region.convert.SysRegionConvert;
import com.carlos.system.region.pojo.dto.SysRegionDTO;
import com.carlos.system.region.pojo.excel.RegionExcel;
import com.carlos.system.region.service.SysRegionService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @Description: 区域导入数据--监听器
 * @Date: 2023/2/21 19:18
 */
@Slf4j
@Data
public class RegionExcelListener implements ReadListener<RegionExcel> {

    private final SysRegionService regionService;


    private List<RegionExcel> cachedDataList = ListUtils.newArrayList();

    public RegionExcelListener(final SysRegionService regionService) {
        this.regionService = regionService;
    }

    @Override
    public void invoke(final RegionExcel user, final AnalysisContext context) {
        cachedDataList.add(user);
    }

    @Override
    public void doAfterAllAnalysed(final AnalysisContext context) {
        saveData();
    }

    /**
     * 在转换异常 获取其他异常下会调用本接口。抛出异常则停止读取。如果这里不抛出异常则 继续读取下一行。
     */
    @Override
    public void onException(final Exception exception, final AnalysisContext context) {
        // 如果是某一个单元格的转换异常 能获取到具体行号
        // 如果要获取头的信息 配合invokeHeadMap使用
        if (exception instanceof ExcelDataConvertException) {
            final ExcelDataConvertException excelDataConvertException = (ExcelDataConvertException) exception;
            final Integer rowIndex = excelDataConvertException.getRowIndex();
            final Integer columnIndex = excelDataConvertException.getColumnIndex();
            final CellData<?> cellData = excelDataConvertException.getCellData();
            log.error("第{}行，第{}列解析异常，数据为:{}", excelDataConvertException.getRowIndex(),
                excelDataConvertException.getColumnIndex(), excelDataConvertException.getCellData());
            throw CommonErrorCode.PARAM_FORMAT_ERROR.exception("第" + rowIndex + "行，第" + columnIndex + "列解析异常，数据为:" + cellData);
        }
    }

    /**
     * 存储数据库
     */
    private void saveData() {
        final List<SysRegionDTO> regions = SysRegionConvert.INSTANCE.excelToDTOS(cachedDataList);
        regionService.importRegions(regions);
    }
}
