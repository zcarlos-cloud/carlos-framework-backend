package com.carlos.system.menu.manager.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.system.enums.OperationType;
import com.carlos.system.menu.convert.SysOperationConvert;
import com.carlos.system.menu.manager.SysOperationManager;
import com.carlos.system.menu.mapper.SysOperationMapper;
import com.carlos.system.menu.pojo.dto.SysOperationDTO;
import com.carlos.system.menu.pojo.entity.SysOperation;
import com.carlos.system.menu.pojo.param.SysOperationPageParam;
import com.carlos.system.menu.pojo.vo.SysOperationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 统一操作表 查询封装实现类
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SysOperationManagerImpl extends BaseServiceImpl<SysOperationMapper, SysOperation> implements SysOperationManager {

    @Override
    public boolean add(SysOperationDTO dto) {
        SysOperation entity = SysOperationConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'SysOperation' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        log.debug("Insert 'SysOperation' data: id:{}", entity.getId());
        return true;
    }

    @Override
    public boolean delete(Serializable id) {
        if (id == null) {
            log.warn("id can't be null");
            return false;
        }
        boolean success = removeById(id);
        if (!success) {
            log.warn("Remove 'SysOperation' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'SysOperation' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(SysOperationDTO dto) {
        SysOperation entity = SysOperationConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'SysOperation' data fail, entity:{}", entity);
            return false;
        }
        log.debug("Update 'SysOperation' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public boolean move(Serializable id, Long menuId) {
        if (id == null) {
            log.warn("id can't be null");
            return false;
        }
        LambdaUpdateWrapper<SysOperation> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysOperation::getId, id)
            // 显式 set：menuId 为 null 也要更新（摘除菜单），updateById 的选择性更新无法置 null
            .set(SysOperation::getMenuId, menuId)
            // 显式 set：wrapper 更新且 entity 为 null 时不会触发 FieldFill.UPDATE
            .set(SysOperation::getUpdateTime, LocalDateTime.now());
        boolean success = update(wrapper);
        if (!success) {
            log.warn("Move 'SysOperation' data fail, id:{}, menuId:{}", id, menuId);
            return false;
        }
        log.debug("Move 'SysOperation' data by id:{} to menuId:{}", id, menuId);
        return true;
    }

    @Override
    public SysOperationDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        SysOperation entity = getBaseMapper().selectById(id);
        return SysOperationConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<SysOperationVO> getPage(SysOperationPageParam param) {
        LambdaQueryWrapper<SysOperation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(param.getOpType() != null, SysOperation::getOpType, param.getOpType())
            .eq(param.getMenuId() != null, SysOperation::getMenuId, param.getMenuId())
            .like(StrUtil.isNotBlank(param.getOpName()), SysOperation::getOpName, param.getOpName())
            .like(StrUtil.isNotBlank(param.getPermCode()), SysOperation::getPermCode, param.getPermCode())
            .like(StrUtil.isNotBlank(param.getApiPath()), SysOperation::getApiPath, param.getApiPath())
                .eq(param.getOpStatus() != null, SysOperation::getOpStatus, param.getOpStatus())
                .orderByAsc(SysOperation::getOrderNum)
            .orderByAsc(SysOperation::getId);

        PageInfo<SysOperation> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, SysOperationConvert.INSTANCE::toVO);
    }

    @Override
    public List<SysOperationDTO> listByMenuId(Long menuId) {
        if (menuId == null) {
            return List.of();
        }
        List<SysOperation> list = lambdaQuery()
            .eq(SysOperation::getMenuId, menuId)
                .orderByAsc(SysOperation::getOrderNum)
            .orderByAsc(SysOperation::getId)
            .list();
        return SysOperationConvert.INSTANCE.toDTO(list);
    }

    @Override
    public List<SysOperationDTO> listByMenuIds(Set<Long> menuIds) {
        if (CollectionUtil.isEmpty(menuIds)) {
            return List.of();
        }
        List<SysOperation> list = lambdaQuery()
            .in(SysOperation::getMenuId, menuIds)
                .orderByAsc(SysOperation::getOrderNum)
            .orderByAsc(SysOperation::getId)
            .list();
        return SysOperationConvert.INSTANCE.toDTO(list);
    }

    @Override
    public SysOperationDTO getByPermCode(String permCode) {
        if (StrUtil.isBlank(permCode)) {
            return null;
        }
        SysOperation entity = lambdaQuery()
            .eq(SysOperation::getPermCode, permCode)
            .one();
        return SysOperationConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public List<SysOperationDTO> listByPermCodes(Set<String> permCodes) {
        if (CollectionUtil.isEmpty(permCodes)) {
            return List.of();
        }
        List<SysOperation> list = lambdaQuery()
            .in(SysOperation::getPermCode, permCodes)
            .list();
        return SysOperationConvert.INSTANCE.toDTO(list);
    }

    @Override
    public List<SysOperationDTO> listAutoScanApis() {
        List<SysOperation> list = lambdaQuery()
                .eq(SysOperation::getAutoScan, true)
            .orderByAsc(SysOperation::getId)
            .list();
        return SysOperationConvert.INSTANCE.toDTO(list);
    }


    @Override
    public List<SysOperationDTO> listAll() {
        List<SysOperation> list = lambdaQuery().list();
        return SysOperationConvert.INSTANCE.toDTO(list);
    }

    @Override
    public boolean deleteByMenuIdAndOpType(Long menuId, OperationType opType) {
        if (menuId == null || opType == null) {
            return false;
        }
        LambdaQueryWrapper<SysOperation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOperation::getMenuId, menuId)
            .eq(SysOperation::getOpType, opType);
        boolean success = remove(wrapper);
        if (!success) {
            log.warn("Remove SysOperation data fail, menuId:{}, opType:{}", menuId, opType);
            return false;
        }
        log.debug("Remove SysOperation data by menuId:{}, opType:{}", menuId, opType);
        return true;
    }

    @Override
    public List<SysOperationDTO> listButtonsByMenuId(Long menuId) {
        if (menuId == null) {
            return List.of();
        }
        List<SysOperation> list = lambdaQuery()
            .eq(SysOperation::getMenuId, menuId)
                .eq(SysOperation::getOpType, OperationType.BUTTON)
                .orderByAsc(SysOperation::getOrderNum)
            .orderByAsc(SysOperation::getId)
            .list();
        return SysOperationConvert.INSTANCE.toDTO(list);
    }

}
