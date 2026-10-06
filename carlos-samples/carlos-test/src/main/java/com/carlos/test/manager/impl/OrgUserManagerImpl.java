package com.carlos.test.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carlos.core.pagination.Paging;
import com.carlos.test.convert.OrgUserConvert;
import com.carlos.test.manager.OrgUserManager;
import com.carlos.test.mapper.OrgUserMapper;
import com.carlos.test.pojo.dto.OrgUserDTO;
import com.carlos.test.pojo.entity.OrgUser;
import com.carlos.test.pojo.param.OrgUserPageParam;
import com.carlos.test.pojo.vo.OrgUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * <p>
 * 系统用户 查询封装实现
 * </p>
 *
 * @author Carlos
 * @date 2023-8-12 11:16:18
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OrgUserManagerImpl extends ServiceImpl<OrgUserMapper, OrgUser> implements OrgUserManager {

    @Override
    public boolean add(OrgUserDTO dto) {
        OrgUser entity = OrgUserConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'OrgUser' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后
        log.debug("Insert 'OrgUser' data: id:{}", entity.getId());
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
            log.warn("Remove 'OrgUser' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'OrgUser' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(OrgUserDTO dto) {
        OrgUser entity = OrgUserConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'OrgUser' data fail, entity:{}", entity);
            return false;
        }
        // 修改成功的后续操
        log.debug("Update 'OrgUser' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public OrgUserDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        OrgUser entity = getBaseMapper().selectById(id);
        return OrgUserConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<OrgUserVO> getPage(OrgUserPageParam param) {
        // LambdaQueryWrapper<OrgUser> wrapper = queryWrapper();
        // wrapper.select(
        //         OrgUser::getId,
        //         OrgUser::getAccount,
        //         OrgUser::getRealname,
        //         OrgUser::getPwd,
        //         OrgUser::getIdentify,
        //         OrgUser::getPhone,
        //         OrgUser::getAddress,
        //         OrgUser::getGender,
        //         OrgUser::getHead,
        //         OrgUser::getSort,
        //         OrgUser::getDescription,
        //         OrgUser::getDingding,
        //         OrgUser::getPoliticalOutlook,
        //         OrgUser::getEducationBackground,
        //         OrgUser::getLastLogin,
        //         OrgUser::getLoginCount,
        //         OrgUser::getCreateBy,
        //         OrgUser::getCreateTime,
        //         OrgUser::getUpdateBy,
        //         OrgUser::getUpdateTime
        // );
        // PageInfo<OrgUser> page = page(pageInfo(param), wrapper);
        // return MybatisPage.convert(page, OrgUserConvert.INSTANCE::toVO);
        return null;
    }

}
