package com.carlos.fx.gitlab.entity;


import lombok.Data;
import org.gitlab4j.api.models.Group;

import java.util.List;

/**
 * <p>
 * gitlab group
 * </p>
 *
 * @author Carlos
 * @date 2024/4/29 16:57
 */
@Data
public class GitlabGroup {

    /**
     * 组信息
     */
    private Group group;

    /**
     * 子组
     */
    private List<GitlabGroup> subGroups;

}
