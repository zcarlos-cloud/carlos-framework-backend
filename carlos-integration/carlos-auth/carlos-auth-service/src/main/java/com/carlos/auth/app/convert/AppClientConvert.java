package com.carlos.auth.app.convert;

import com.carlos.auth.api.pojo.ao.AppClientAO;
import com.carlos.auth.app.pojo.dto.AppClientDTO;
import com.carlos.auth.app.pojo.entity.AppClient;
import com.carlos.auth.app.pojo.param.AppClientCreateParam;
import com.carlos.auth.app.pojo.param.AppClientResetSecretParam;
import com.carlos.auth.app.pojo.param.AppClientUpdateParam;
import com.carlos.auth.app.pojo.vo.AppClientListVO;
import com.carlos.auth.app.pojo.vo.AppClientPageVO;
import com.carlos.auth.app.pojo.vo.AppClientVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.util.List;

/**
 * <p>
 * 应用信息 转换器
 * </p>
 *
 * @author Carlos
 * @date 2025-3-12 14:00:14
 */
@Mapper(uses = {CommonConvert.class}, imports = {ClientSettings.class, TokenSettings.class})
public interface AppClientConvert {

    AppClientConvert INSTANCE = Mappers.getMapper(AppClientConvert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2025-3-12 14:00:14
     */
    AppClientDTO toDTO(AppClientCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2025-3-12 14:00:14
     */
    AppClientDTO toDTO(AppClientUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2025-3-12 14:00:14
     */
    List<AppClientDTO> toDTO(List<AppClient> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2025-3-12 14:00:14
     */
    @Mapping(source = "scopes", target = "scopes", qualifiedByName = "str2set")
    @Mapping(source = "redirectUris", target = "redirectUris", qualifiedByName = "str2set")
    @Mapping(source = "authorizationGrantTypes", target = "authorizationGrantTypes", qualifiedByName = "str2set")
    @Mapping(source = "authenticationMethods", target = "authenticationMethods", qualifiedByName = "str2set")
    @Mapping(target = "clientSettings", expression = "java(CommonConvert.str2clientsetting(entity.getClientSettings()))")
    @Mapping(target = "tokenSettings", expression = "java(CommonConvert.str2tokensetting(entity.getTokenSettings()))")
    AppClientDTO toDTO(AppClient entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author Carlos
     * @date 2025-3-12 14:00:14
     */
    @Mapping(source = "scopes", target = "scopes", qualifiedByName = "set2str")
    @Mapping(source = "redirectUris", target = "redirectUris", qualifiedByName = "set2str")
    @Mapping(source = "authorizationGrantTypes", target = "authorizationGrantTypes", qualifiedByName = "set2str")
    @Mapping(source = "authenticationMethods", target = "authenticationMethods", qualifiedByName = "set2str")
    @Mapping(source = "clientSettings", target = "clientSettings", qualifiedByName = "obj2str")
    @Mapping(source = "tokenSettings", target = "tokenSettings", qualifiedByName = "obj2str")
    AppClient toDO(AppClientDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2025-3-12 14:00:14
     */
    AppClientVO toVO(AppClientDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2025-3-12 14:00:14
     */
    List<AppClientPageVO> toVO(List<AppClient> dos);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param entity 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2025-3-12 14:00:14
     */
    @Mapping(source = "scopes", target = "scopes", qualifiedByName = "str2set")
    @Mapping(source = "redirectUris", target = "redirectUris", qualifiedByName = "str2set")
    @Mapping(source = "authorizationGrantTypes", target = "authorizationGrantTypes", qualifiedByName = "str2set")
    @Mapping(source = "authenticationMethods", target = "authenticationMethods", qualifiedByName = "str2set")
    @Mapping(target = "clientSettings", expression = "java(CommonConvert.str2clientsetting(entity.getClientSettings()))")
    @Mapping(target = "tokenSettings", expression = "java(CommonConvert.str2tokensetting(entity.getTokenSettings()))")
    AppClientVO toVO(AppClient entity);

    /**
     * 秘钥重置参数转换
     *
     * @param param 参数0
     * @return com.carlos.oauth.client.pojo.dto.AppClientDTO
     * @author Carlos
     * @date 2025-03-14 14:40
     */
    AppClientDTO toDTO(AppClientResetSecretParam param);

    List<AppClientListVO> toListVO(List<AppClientDTO> list);

    AppClientAO toAO(AppClientDTO client);
}
