package com.carlos.auth.app.listener;

import cn.hutool.core.util.StrUtil;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import cn.idev.excel.exception.ExcelDataConvertException;
import cn.idev.excel.metadata.data.CellData;
import com.carlos.auth.api.enums.AuthErrorCode;
import com.carlos.auth.app.pojo.dto.AppClientDTO;
import com.carlos.auth.app.pojo.excel.AppClientExcel;
import com.carlos.auth.app.service.AppClientService;
import com.carlos.boot.web.util.ResponseWriter;
import com.carlos.util.easyexcel.ExcelUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.List;

/**
 * <p>
 *   应用信息导入
 * </p>
 *
 * @author Carlos
 * @date 2025-03-27 13:58
 */
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
@RequiredArgsConstructor
public class AppClientExcelListener extends AnalysisEventListener<AppClientExcel> {

    private final AppClientService clientService;


    private List<AppClientExcel> errorList = Lists.newArrayList();

    @Override
    public void invoke(final AppClientExcel client, final AnalysisContext context) {
        // 这里可以进行单元格的校验  或者整体的校验
        AppClientDTO dto = new AppClientDTO();
        dto.setClientIssuedAt(client.getClientIssuedAt());
        dto.setClientSecretExpiresAt(client.getClientSecretExpiresAt());
        if (StrUtil.isBlank(client.getAuthenticationMethods())) {
            dto.setAuthenticationMethods(Sets.newHashSet(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue()));
        } else {
            dto.setAuthenticationMethods(Sets.newHashSet(StrUtil.split(client.getAuthenticationMethods(), StrUtil.COMMA)));
        }
        if (StrUtil.isBlank(client.getAuthorizationGrantTypes())) {
            dto.setAuthorizationGrantTypes(Sets.newHashSet(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue()));
        } else {
            dto.setAuthorizationGrantTypes(Sets.newHashSet(StrUtil.split(client.getAuthorizationGrantTypes(), StrUtil.COMMA)));
        }
        dto.setAppLogo(client.getAppLogo());

        String appName = client.getAppName();
        if (StrUtil.isBlank(appName)) {
            client.setError("应用名称不能为空");
            errorList.add(client);
            return;
        }
        dto.setAppName(appName);
        try {
            clientService.addAppClient(dto);
        } catch (Exception e) {
            Integer rowIndex = context.readRowHolder() != null ? context.readRowHolder().getRowIndex() : null;
            log.error("第{}行应用信息保存失败：{}", rowIndex, e.getMessage(), e);
            client.setError("第" + rowIndex + "行保存失败：" + e.getMessage());
            errorList.add(client);
        }
    }

    @Override
    public void doAfterAllAnalysed(final AnalysisContext context) {
        // 将处理写过写回
        HttpServletResponse response = ResponseWriter.getResponse();
        String name = "应用导入失败信息-" + System.currentTimeMillis();
        try {
            ExcelUtil.download(response, name, AppClientExcel.class, errorList);
        } catch (Exception e) {
            throw AuthErrorCode.AUTH_CLIENT_EXPORT_FAILED.exception();
        }
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
            throw AuthErrorCode.AUTH_CLIENT_IMPORT_FAILED.exception(
                "第%d行，第%d列解析异常，数据为:%s", rowIndex, columnIndex, cellData);
        }
    }

}
