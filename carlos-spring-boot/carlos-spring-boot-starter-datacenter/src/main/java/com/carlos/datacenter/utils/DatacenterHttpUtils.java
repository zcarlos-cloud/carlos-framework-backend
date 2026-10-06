package com.carlos.datacenter.utils;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.carlos.datacenter.exception.DatacenterInstanceException;

import java.util.Map;

public class DatacenterHttpUtils {

    private static class DatacenterHttpHolder {
        private static final DatacenterHttpUtils INSTANCE = new DatacenterHttpUtils();
    }

    public static DatacenterHttpUtils instance() {
        return DatacenterHttpHolder.INSTANCE;
    }

    /**
     * 发送post json请求
     *
     * @param url     请求地址
     * @param headers 请求头
     * @param body    请求体(json格式字符串)
     * @return 返回体
     */
    public JSONObject postJson(String url, Map<String, String> headers, String body) {
        try (HttpResponse response = HttpRequest.post(url)
            .addHeaders(headers)
            .body(body)
            .execute()) {
            return JSONUtil.parseObj(response.body());
        } catch (Exception e) {
            throw new DatacenterInstanceException(e.getMessage());
        }
    }

    /**
     * 发送post json请求
     *
     * @param url     请求地址
     * @param headers 请求头
     * @param body    请求体(map格式请求体)
     * @return 返回体
     */
    public JSONObject postJson(String url, Map<String, String> headers, Map<String, Object> body) {
        return postJson(url, headers, JSONUtil.toJsonStr(body));
    }

    /**
     * 发送post form 请求
     *
     * @param url     请求地址
     * @param headers 请求头
     * @param body    请求体(map格式请求体)
     * @return 返回体
     */
    public JSONObject postFrom(String url, Map<String, String> headers, Map<String, Object> body) {
        try (HttpResponse response = HttpRequest.post(url)
            .addHeaders(headers)
            .form(body)
            .execute()) {
            return JSONUtil.parseObj(response.body());
        } catch (Exception e) {
            throw new DatacenterInstanceException(e.getMessage());
        }
    }

    /**
     * 发送post form 请求
     *
     * @param url     请求地址
     * @param headers 请求头
     * @param body    请求体(map格式请求体)
     * @param username 用户名
     * @param password 密码
     * @return 返回体
     */
    public JSONObject postBasicFrom(String url, Map<String, String> headers, String username, String password, Map<String, Object> body) {
        try (HttpResponse response = HttpRequest.post(url)
            .addHeaders(headers)
            .basicAuth(username, password)
            .form(body)
            .execute()) {
            return JSONUtil.parseObj(response.body());
        } catch (Exception e) {
            throw new DatacenterInstanceException(e.getMessage());
        }
    }

    /**
     * 发送post url 参数拼装url传输
     *
     * @param url     请求地址
     * @param headers 请求头
     * @param params  请求参数
     * @return 返回体
     */
    public JSONObject postUrl(String url, Map<String, String> headers, Map<String, Object> params) {
        String urlWithParams = url + "?" + URLUtil.buildQuery(params, null);
        try (HttpResponse response = HttpRequest.post(urlWithParams)
            .addHeaders(headers)
            .execute()) {
            return JSONUtil.parseObj(response.body());
        } catch (Exception e) {
            throw new DatacenterInstanceException(e.getMessage());
        }
    }

    /**
     * 发送get
     *
     * @param url 请求地址
     * @return 返回体
     */
    public JSONObject getBasic(String url, String username, String password) {
        try (HttpResponse response = HttpRequest.get(url)
            .basicAuth(username, password)
            .execute()) {
            return JSONUtil.parseObj(response.body());
        } catch (Exception e) {
            throw new DatacenterInstanceException(e.getMessage());
        }
    }

    /**
     * 发送get
     *
     * @param url 请求地址
     * @return 返回体
     */
    public JSONObject getUrl(String url) {
        try (HttpResponse response = HttpRequest.get(url)
            .execute()) {
            return JSONUtil.parseObj(response.body());
        } catch (Exception e) {
            throw new DatacenterInstanceException(e.getMessage());
        }
    }


    /**
     * get
     *
     * @param url 参数0
     * @param queryParams 参数1
     * @param body 参数2
     * @param headers 参数3
     * @return cn.hutool.json.JSONObject
     * @author Carlos
     * @date 2024-10-11 00:11
     */
    public JSONObject get(String url, Map<String, Object> queryParams, String body, Map<String, String> headers) {
        try {
            HttpRequest httpRequest = HttpRequest.get(url);
            if (StrUtil.isNotBlank(body)) {
                httpRequest.body(body);
            }
            if (MapUtil.isNotEmpty(queryParams)) {
                httpRequest.form(queryParams);
            }
            if (MapUtil.isNotEmpty(headers)) {
                httpRequest.addHeaders(headers);
            }
            HttpResponse response = httpRequest.execute();

            return JSONUtil.parseObj(response.body());
        } catch (Exception e) {
            throw new DatacenterInstanceException(e);
        }
    }
}
