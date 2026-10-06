package com.carlos.cloud.feign;

import feign.FeignException;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Type;

@RequiredArgsConstructor
public class FeignDecoder implements Decoder {

    private final ObjectMapper mapper;

    /**
     * 这里统一处理，根据状态码判断返回正常还是异常的， 200返回正常的，其他状态码直接抛出异常
     */
    @Override
    public Object decode(final Response response, final Type type) throws IOException, DecodeException, FeignException {
        // if (response.status() >= 200 && response.status() <= 299) {
        //     String result = Util.toString(response.body().asReader());
        //     BaseResponse baseResponse = mapper.readValue(result, BaseResponse.class);
        //     int code = baseResponse.getCode();
        //     if (code == 200) {
        //         return baseResponse;
        //     }
        //     throw new MyException(EnumError.BAD_REQUEST);
        // }
        // throw new MyException(EnumError.OTHER_ERROR);
        return null;
    }
}
