package com.carlos.util.function;

import org.springframework.lang.Nullable;

import java.io.Serializable;

/**
 * <p>
 *
 * </p>
 *
 * @author Carlos
 * @date 2022/11/14 1:00
 */
@FunctionalInterface
public interface CheckedConsumer<T> extends Serializable {

    /**
     * Run the Consumer
     *
     * @param var1 T
     * @throws Throwable UncheckedException
     */

    @Nullable
    void accept(@Nullable T var1) throws Throwable;

}
