package com.carlos.auth.app.service;

import com.carlos.auth.api.enums.AuthErrorCode;
import com.carlos.auth.app.manager.AppCategoryManager;
import com.carlos.auth.app.pojo.dto.AppCategoryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

/**
 * 应用分类 业务逻辑服务实现
 *
 * @author Carlos
 * @date 2026-07-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppCategoryServiceImpl implements AppCategoryService {

    private final AppCategoryManager appCategoryManager;

    @Override
    public boolean add(AppCategoryDTO dto) {
        boolean success = appCategoryManager.add(dto);
        if (!success) {
            throw AuthErrorCode.AUTH_DATABASE_ERROR.exception("新增应用分类失败");
        }
        return true;
    }

    @Override
    public boolean delete(Serializable id) {
        if (id == null) {
            throw AuthErrorCode.AUTH_PARAM_ID_MISSING.exception();
        }
        boolean success = appCategoryManager.delete(id);
        if (!success) {
            throw AuthErrorCode.AUTH_DATABASE_ERROR.exception("删除应用分类失败");
        }
        return true;
    }

    @Override
    public boolean modify(AppCategoryDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw AuthErrorCode.AUTH_PARAM_ID_MISSING.exception();
        }
        boolean success = appCategoryManager.modify(dto);
        if (!success) {
            throw AuthErrorCode.AUTH_DATABASE_ERROR.exception("更新应用分类失败");
        }
        return true;
    }

    @Override
    public List<AppCategoryDTO> listAll() {
        return appCategoryManager.listAll();
    }
}
