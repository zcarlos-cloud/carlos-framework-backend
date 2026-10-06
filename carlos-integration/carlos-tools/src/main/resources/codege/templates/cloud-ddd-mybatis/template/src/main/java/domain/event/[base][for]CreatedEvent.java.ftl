package ${project.packageName}.domain.event;

import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * ${table.comment} 已创建领域事件
 * </p>
 * <p>当${table.comment}被成功创建后发布，供其他限界上下文订阅处理</p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Getter
public class ${table.classPrefix}CreatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ${table.comment}ID
     */
    private final Serializable id;

    /**
     * 发生时间
     */
    private final LocalDateTime occurredOn;

    public ${table.classPrefix}CreatedEvent(Serializable id) {
        this.id = id;
        this.occurredOn = LocalDateTime.now();
    }
}
