package com.carlos.org.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.org.convert.OrgUserDepartmentConvert;
import com.carlos.org.manager.OrgUserDepartmentManager;
import com.carlos.org.mapper.OrgUserDepartmentMapper;
import com.carlos.org.pojo.dto.OrgUserDepartmentDTO;
import com.carlos.org.pojo.entity.OrgUserDepartment;
import com.carlos.org.pojo.param.OrgUserDepartmentPageParam;
import com.carlos.org.pojo.vo.OrgUserDepartmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 用户部门 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OrgUserDepartmentManagerImpl extends BaseServiceImpl<OrgUserDepartmentMapper, OrgUserDepartment> implements OrgUserDepartmentManager {

    @Override
    public boolean add(OrgUserDepartmentDTO dto) {
        OrgUserDepartment entity = OrgUserDepartmentConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'OrgUserDepartment' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'OrgUserDepartment' data: id:{}", entity.getId());
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
            log.warn("Remove 'OrgUserDepartment' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'OrgUserDepartment' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(OrgUserDepartmentDTO dto) {
        OrgUserDepartment entity = OrgUserDepartmentConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'OrgUserDepartment' data fail, entity:{}", entity);
            return false;
        }
        // 修改成功的后续操作
        log.debug("Update 'OrgUserDepartment' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public OrgUserDepartmentDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        OrgUserDepartment entity = getBaseMapper().selectById(id);
        return OrgUserDepartmentConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<OrgUserDepartmentVO> getPage(OrgUserDepartmentPageParam param) {
        LambdaQueryWrapper<OrgUserDepartment> wrapper = queryWrapper();
        wrapper.select(

            OrgUserDepartment::getId,
            OrgUserDepartment::getUserId,
            OrgUserDepartment::getDeptId,
            OrgUserDepartment::getMainDept,
            OrgUserDepartment::getCreateBy,
            OrgUserDepartment::getCreateTime
        );
        PageInfo<OrgUserDepartment> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, OrgUserDepartmentConvert.INSTANCE::toVO);
    }

    @Override
    public boolean removeByUserId(Serializable userId) {
        if (userId == null) {
            return false;
        }
        LambdaQueryWrapper<OrgUserDepartment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrgUserDepartment::getUserId, userId);
        return remove(wrapper);
    }

    @Override
    public List<OrgUserDepartmentDTO> listByUserId(Serializable userId) {
        if (userId == null) {
            return List.of();
        }
        LambdaQueryWrapper<OrgUserDepartment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrgUserDepartment::getUserId, userId);
        List<OrgUserDepartment> list = list(wrapper);
        return OrgUserDepartmentConvert.INSTANCE.toDTO(list);
    }

}
