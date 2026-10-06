package com.carlos.datascope;

import java.util.Objects;


/**
 * <p>
 * 数据权限处理
 * </p>
 *
 * @author Carlos
 * @date 2022/11/22 10:46
 */
@FunctionalInterface
public interface DataScopeConsumer<T, U> {


    void accept(T t, U u);


    default DataScopeConsumer<T, U> andThen(DataScopeConsumer<? super T, ? super U> after) {
        Objects.requireNonNull(after);

        return (l, r) -> {
            accept(l, r);
            after.accept(l, r);
        };
    }
}