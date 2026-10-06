package com.carlos.system.region.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;


/**
 * <p>
 * 文件上传参数
 * </p>
 *
 * @author Carlos
 * @date 2021-11-10 10:45:55
 */

@Data
public class SysRegionConvertParam {

    @NotNull(message = "上传文件不能为空")
    @Schema(description = "文件")
    private MultipartFile file;

    @NotBlank(message = "行政区域编码字段key不能为空")
    @Schema(description = "行政区域编码字段key")
    private String regionCode;
    @NotBlank(message = "行政区域名称字段key不能为空")
    @Schema(description = "行政区域名称字段key")
    private String regionName;
    @NotBlank(message = "上级id字段key不能为空")
    @Schema(description = "上级id字段")
    private String pid;

    /**
     * Excel 中「区域ID」那一列的**列名**
     *
     * 类型必须是 String（原来是 Long，是错的）。下游是把它当**列名**用的 ——
     * 见 `SysRegionService#importCustomRegions`：
     * <pre>
     * String id = row.get(param.getId()).toString();   // row 是 Map&lt;String, Object&gt; 的一行
     * </pre>
     * 传 Long 有两个后果：{@code row.get(Long)} 在 {@code Map<String,Object>} 里恒取不到值
     * → {@code .toString()} 抛 NPE；而且客户端必须传列名（如「区域ID」），
     * Spring 也无法把它转成 Long，直接 400。
     * 也就是说 {@code POST /sys/region/importV2} 此前根本调不通。
     */
    @NotBlank(message = "id字段key不能为空")
    @Schema(description = "id字段（Excel 列名）")
    private String id;
}
