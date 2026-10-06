package com.carlos.test.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.carlos.core.pagination.Paging;
import com.carlos.test.pojo.dto.OrgUserDTO;
import com.carlos.test.pojo.entity.OrgUser;
import com.carlos.test.pojo.param.OrgUserPageParam;
import com.carlos.test.pojo.vo.OrgUserVO;

import java.io.Serializable;

/**
 * <p>
 * 系统用户 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2023-8-12 11:16:18
 */
public interface OrgUserManager extends IService<OrgUser> {

    /**
     * 新增系统用户
     *
     * @param dto 系统用户数据
     * @return boolean
     * @author Carlos
     * @date 2023-8-12 11:16:18
     */
    boolean add(OrgUserDTO dto);

    /**
     * 删除系统用户
     *
     * @param id 系统用户id
     * @return boolean
     * @author Carlos
     * @date 2023-8-12 11:16:18
     */
    boolean delete(Serializable id);

    /**
     * 修改系统用户信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author Carlos
     * @date 2023-8-12 11:16:18
     */
    boolean modify(OrgUserDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.test.pojo.dto.OrgUserDTO
     * @author Carlos
     * @date 2023-8-12 11:16:18
     */
    OrgUserDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param param 分页参数
     * @author Carlos
     * @date 2023-8-12 11:16:18
     */
    Paging<OrgUserVO> getPage(OrgUserPageParam param);
}
