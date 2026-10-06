package com.carlos.util.easyexcel;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.idev.excel.ExcelReader;
import cn.idev.excel.FastExcel;
import cn.idev.excel.event.AnalysisEventListener;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;

/**
 * excel相关工具
 *
 * @author Carlos
 * @date 2022/11/28 16:48
 */
@Slf4j
public class ExcelUtil extends cn.hutool.poi.excel.ExcelUtil {

    /**
     * 表格下载 无具体对象
     *
     * @param response 请求响应对象
     * @param name     文件名称，不带扩展名
     * @param head     表头信息
     * @param data     数据内容
     * @author Carlos
     * @date 2022/11/28 16:24
     */
    public static void download(
        final HttpServletResponse response, final String name, final List<List<String>> head, final Collection<?> data) {

        // 这里注意 有同学反应使用swagger 会导致各种问题，请直接用浏览器或者用postman
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
            final String fileName = URLEncoder.encode(name, "UTF-8").replaceAll("\\+", "%20");
            response.setHeader(
                "Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            // 这里需要设置不关闭流
            FastExcel.write(response.getOutputStream())
                .autoCloseStream(Boolean.FALSE)
                .head(head)
                .sheet("sheet1")
                .doWrite(data);
        } catch (final Exception e) {
            throw new IllegalStateException("数据导出失败", e);
        }
    }

    /**
     * 表格下载 有具体对象
     *
     * @param response 请求响应对象
     * @param name     文件名称，不带扩展名
     * @param head     表头信息
     * @param data     数据内容
     * @author Carlos
     * @date 2022/11/28 16:24
     */
    public static void download(
        final HttpServletResponse response, final String name, final Class head, final Collection<?> data) {

        // 这里注意 有同学反应使用swagger 会导致各种问题，请直接用浏览器或者用postman
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
            final String fileName = URLEncoder.encode(name, "UTF-8").replaceAll("\\+", "%20");
            response.setHeader(
                "Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            // 这里需要设置不关闭流
            FastExcel.write(response.getOutputStream(), head)
                .autoCloseStream(Boolean.TRUE)
                .sheet("sheet1")
                .doWrite(data);
        } catch (final Exception e) {
            throw new IllegalStateException("数据导出失败", e);
        }
    }

    public static void checkExcel(final String name) {
        final String extName = FileNameUtil.extName(name);
        if (!CharSequenceUtil.equalsAny(extName, "xls", "xlsx")) {
            throw new IllegalStateException("不支持的excel文件类型");
        }

    }


    /**
     * 返回 ExcelReader
     *
     * @param excel         需要解析的 Excel 文件
     * @param excelListener new ExcelListener()
     */
    private static ExcelReader getReader(MultipartFile excel, Class head, AnalysisEventListener excelListener) {
        String filename = FileNameUtil.getName(excel.getOriginalFilename());

        if (filename == null || (!filename.toLowerCase().endsWith(".xls") && !filename.toLowerCase().endsWith(".xlsx"))) {
            return null;
        }
        InputStream inputStream;

        try {
            inputStream = new BufferedInputStream(excel.getInputStream());
            ExcelReader excelReader = FastExcel.read(inputStream, head, excelListener).build();

            return excelReader;
        } catch (IOException e) {
            log.error("读取Excel文件失败: {}", FileNameUtil.getName(excel.getOriginalFilename()), e);
        }

        return null;
    }

    /**
     * 判断你一个类是否存在某个属性（字段）
     *
     * @param fieldName 字段
     * @param obj       类对象
     * @return true:存在，false:不存在, null:参数不合法
     */
    public static Boolean isExistField(String fieldName, Object obj) {
        if (obj == null || !StringUtils.hasText(fieldName)) {
            return null;
        }
        // 获取这个类的所有属性
        Field[] fields = obj.getClass().getDeclaredFields();
        boolean flag = false;
        // 循环遍历所有的fields
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getName().equals(fieldName)) {
                flag = true;
                break;
            }
        }
        return flag;
    }


    /**
     * 大数据量流式导出（数十万行数据场景）
     * <p>
     * FastExcel 内部已使用 SXSSF 流式写入，内存占用极低。
     * 本方法是 {@link #download(HttpServletResponse, String, Class, Collection)} 的别名，
     * 显式标识大数据量导出场景，便于代码审查和性能监控。
     * </p>
     * <p>
     * 性能说明：
     * <ul>
     *   <li>FastExcel.write() 内部自动使用 SXSSFWorkbook（POI 流式写入）</li>
     *   <li>默认 windowSize=100，即内存中仅保留最后 100 行</li>
     *   <li>支持百万行数据导出，内存占用恒定（仅受 windowSize 影响）</li>
     * </ul>
     * </p>
     *
     * @param response HTTP 响应
     * @param name     文件名称（不含扩展名）
     * @param head     数据类（使用 &#064;ExcelProperty 标注字段）
     * @param data     数据集合（支持任意大小）
     */
    public static void downloadLarge(
        final HttpServletResponse response, final String name, final Class<?> head, final Collection<?> data) {
        download(response, name, head, data);
        log.debug("Large Excel download completed: name={}, size={}", name, data.size());
    }

    private static ExcelReader getReaderNoHead(MultipartFile excel, AnalysisEventListener excelListener) {
        String filename = excel.getOriginalFilename();

        if (filename == null || (!filename.toLowerCase().endsWith(".xls") && !filename.toLowerCase().endsWith(".xlsx"))) {
            return null;
        }
        InputStream inputStream;

        try {
            inputStream = new BufferedInputStream(excel.getInputStream());
            ExcelReader excelReader = FastExcel.read(inputStream, excelListener).build();

            return excelReader;
        } catch (IOException e) {
            log.error("读取Excel文件失败(无头模式): {}", excel.getOriginalFilename(), e);
        }

        return null;
    }


}
