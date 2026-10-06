package com.carlos.redis.serialize;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Kryo 二进制序列化实现
 * <p>
 * 特点：
 * - 性能极高（比 JDK 序列化快 10-100 倍）
 * - 空间占用小（比 JDK 序列化小 3-5 倍）
 * - 二进制格式，Redis 中不可读
 * - 不适合长期存储（版本兼容性较差）
 * </p>
 *
 * @author carlos
 * @date 2026-03-14
 */
@Slf4j
public class KryoSerializer implements RedisSerializerStrategy {

    /**
     * Kryo 对象池 - 线程安全
     */
    private final Pool<Kryo> kryoPool;

    /**
     * 输出缓冲区大小
     */
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    public KryoSerializer() {
        // 创建 Kryo 对象池
        this.kryoPool = new Pool<Kryo>(true, false, 16) {
            @Override
            protected Kryo create() {
                Kryo kryo = new Kryo();
                // 配置 Kryo
                kryo.setRegistrationRequired(false); // 允许未注册类
                kryo.setReferences(true); // 支持循环引用
                kryo.setOptimizedGenerics(true); // 优化泛型
                return kryo;
            }
        };
    }

    @Override
    public byte[] serialize(Object obj) {
        if (obj == null) {
            return new byte[0];
        }

        Kryo kryo = kryoPool.obtain();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
             Output output = new Output(baos)) {

            kryo.writeClassAndObject(output, obj);
            output.flush();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Kryo serialize error", e);
            throw new SerializationException("Kryo serialize error: " + e.getMessage(), e);
        } finally {
            kryoPool.free(kryo);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        Kryo kryo = kryoPool.obtain();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             Input input = new Input(bais)) {

            Object obj = kryo.readClassAndObject(input);
            if (obj == null) {
                return null;
            }
            // 类型检查
            if (!clazz.isInstance(obj)) {
                throw new ClassCastException("Cannot cast " + obj.getClass() + " to " + clazz);
            }
            return (T) obj;

        } catch (Exception e) {
            log.error("Kryo deserialize error", e);
            throw new SerializationException("Kryo deserialize error: " + e.getMessage(), e);
        } finally {
            kryoPool.free(kryo);
        }
    }

    @Override
    public String getType() {
        return "kryo";
    }

    @Override
    public boolean isBinary() {
        return true;
    }

    /**
     * 序列化异常
     */
    public static class SerializationException extends com.carlos.core.exception.BusinessException {
        public SerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
