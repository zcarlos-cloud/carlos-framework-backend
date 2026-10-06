package com.carlos.system.configration.listener;


import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.exception.ExcelDataConvertException;
import cn.idev.excel.metadata.data.CellData;
import cn.idev.excel.read.listener.ReadListener;
import cn.idev.excel.util.ListUtils;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.system.configration.convert.SysConfigConvert;
import com.carlos.system.configration.pojo.dto.SysConfigDTO;
import com.carlos.system.configration.pojo.excel.SysConfigExcel;
import com.carlos.system.configration.service.SysConfigService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * <p>
 * 系统配置 导入数据监听器
 * </p>
 *
 * @author carlos
 */
@Slf4j
@Data
public class SysConfigExcelListener implements ReadListener<SysConfigExcel> {

    private final SysConfigService configService;

    private List<SysConfigExcel> cachedDataList = ListUtils.newArrayList();

    public SysConfigExcelListener(SysConfigService configService) {
        this.configService = configService;
    }

    @Override
    public void invoke(SysConfigExcel data, AnalysisContext context) {
        cachedDataList.add(data);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        saveData();
    }

    /**
     * 在转换异常或其他异常下会调用本接口。抛出异常则停止读取。
     */
    @Override
    public void onException(Exception exception, AnalysisContext context) {
        if (exception instanceof ExcelDataConvertException) {
            ExcelDataConvertException excelDataConvertException = (ExcelDataConvertException) exception;
            Integer rowIndex = excelDataConvertException.getRowIndex();
            Integer columnIndex = excelDataConvertException.getColumnIndex();
            CellData<?> cellData = excelDataConvertException.getCellData();
            log.error("第{}行，第{}列解析异常，数据为:{}", rowIndex, columnIndex, cellData);
            throw CommonErrorCode.PARAM_FORMAT_ERROR.exception("第" + rowIndex + "行，第" + columnIndex + "列解析异常，数据为:" + cellData);
        }
    }

    /**
     * 存储数据库
     */
    private void saveData() {
        List<SysConfigDTO> dtoList = SysConfigConvert.INSTANCE.excelToDTO(cachedDataList);
        configService.importConfigs(dtoList);
    }

}
