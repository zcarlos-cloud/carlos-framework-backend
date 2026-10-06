package com.carlos.util.easyexcel;

import java.util.List;


public interface ExcelImportExecutor<T extends ExcelData> {


    /**
     * 检查
     *
     * @param t             t
     * @param cacheDataList 缓存数据列表
     * @param errorDataList 错误数据列表
     */
    void check(T t, List<T> cacheDataList, List<T> errorDataList);


    /**
     * 批量保存
     *
     * @param cacheDataList 缓存数据列表
     * @return boolean
     */
    boolean saveBatch(List<T> cacheDataList);

    /**
     * 错误数据响应
     *
     * @param errorDataList 错误数据列表
     * @return boolean
     */
    void errorDataResponse(List<T> errorDataList);

}