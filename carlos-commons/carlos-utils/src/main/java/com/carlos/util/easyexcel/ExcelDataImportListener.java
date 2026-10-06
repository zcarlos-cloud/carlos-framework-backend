package com.carlos.util.easyexcel;

import cn.hutool.json.JSONUtil;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.exception.ExcelDataConvertException;
import cn.idev.excel.metadata.CellExtra;
import cn.idev.excel.metadata.data.ReadCellData;
import cn.idev.excel.read.listener.ReadListener;
import cn.idev.excel.util.ConverterUtils;
import cn.idev.excel.util.ListUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;


/**
 * excel数据导入侦听器 使用时需保证线程安全
 *
 * @author carlos
 * @date 2023/08/31
 */
@Slf4j
public class ExcelDataImportListener implements ReadListener<ExcelData> {

    /**
     * 每隔5条存储数据库，实际使用中可以100条，然后清理list ，方便内存回收
     */
    private static final int BATCH_COUNT = 100;
    /**
     * 缓存的数据
     */
    private List<ExcelData> rightRows = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
    /**
     * 错误数据列表
     */
    private List<ExcelData> errorRows = ListUtils.newArrayList();
    /**
     * 数据导入执行器
     */
    private final ExcelImportExecutor executor;


    public ExcelDataImportListener(ExcelImportExecutor executor) {
        this.executor = executor;
    }

    /**
     * 这个每一条数据解析都会来调用
     *
     * @param row one row value. Is is same as {@link AnalysisContext#readRowHolder()}
     */
    @Override
    public void invoke(ExcelData row, AnalysisContext context) {
        // 获取到一条数据
        // 1. 检查数据内容
        this.executor.check(row, rightRows, errorRows);

        // 2. 达到批量阈值时保存数据，避免数据只校验不落地
        if (rightRows.size() >= BATCH_COUNT) {
            saveData();
        }
    }

    /**
     * 批量保存缓存的正确数据并清空列表
     */
    private void saveData() {
        if (rightRows.isEmpty()) {
            return;
        }
        try {
            boolean success = this.executor.saveBatch(rightRows);
            if (!success) {
                log.error("Excel 批量保存失败，将影响 {} 条数据", rightRows.size());
                errorRows.addAll(rightRows);
            }
        } catch (Exception e) {
            log.error("Excel 批量保存异常，将影响 {} 条数据", rightRows.size(), e);
            errorRows.addAll(rightRows);
        }
        rightRows = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        log.error("解析失败，但是继续解析下一行:{}", exception.getMessage());
        // 如果是某一个单元格的转换异常 能获取到具体行号
        // 如果要获取头的信息 配合invokeHeadMap使用
        if (exception instanceof ExcelDataConvertException) {
            ExcelDataConvertException excelDataConvertException = (ExcelDataConvertException) exception;
            log.error("第{}行，第{}列解析异常，数据为:{}", excelDataConvertException.getRowIndex(),
                excelDataConvertException.getColumnIndex(), excelDataConvertException.getCellData());
        }
    }

    @Override
    public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
        Map<Integer, String> headData = ConverterUtils.convertToStringMap(headMap, context);
        log.info("解析到一条头数据:{}", JSONUtil.toJsonPrettyStr(headMap));
        // 如果想转成成 Map<Integer,String>
        // 方案1： 不要implements ReadListener 而是 extends AnalysisEventListener
        // 方案2： 调用 ConverterUtils.convertToStringMap(headMap, context) 自动会转换
    }


    @Override
    public void extra(CellExtra extra, AnalysisContext context) {
        ReadListener.super.extra(extra, context);
    }

    /**
     * 所有数据解析完成了 都会来调用
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 保存最后遗留的数据，确保全部数据落库
        saveData();
        log.info("所有数据解析完成！");
        int rowIndex = context.readRowHolder().getRowIndex();
        int headLineNumber = context.readSheetHolder().getHeadRowNumber();
        this.executor.errorDataResponse(errorRows);

    }


    /**
     * 通过返回true  false 来决定是否有下一条数据
     *
     * @param context 参数0
     * @return boolean
     * @author Carlos
     * @date 2023/8/31 23:47
     */
    @Override
    public boolean hasNext(AnalysisContext context) {
        return ReadListener.super.hasNext(context);
    }

}
