package com.carlos.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.*;

public class DataGet {


    public static void export(Set<String> ids) {

        final String token = "12b6f8d2bae24f68b3439abb474fbc31";

        Map<String, String> map = new HashMap<>(4);
        map.put("busAdd", "注册地址");
        map.put("currencyKind", "货币类型");
        map.put("regCapital", "注册资本");
        map.put("dishonest", "是否严重违法失信");
        map.put("violateLaw", "是否违法违规");
        map.put("companyName", "企业名称");
        map.put("validStart", "有效期开");
        map.put("adminPunish", "是否受到行政处罚");
        map.put("uscc", "社会统一信用代码");
        map.put("partDate", "分区日期");
        map.put("prinType", "主体类型");
        map.put("regMngName", "登记管理部门");
        map.put("estabDate", "成立日期");
        map.put("orgType", "机构类型");
        map.put("vaildEnd", "有效期结");
        map.put("lawSuit", "是否涉案涉诉");
        map.put("abnormalOp", "是否异常经营");
        map.put("lar", "法定代表");
        map.put("isTax", "是否纳税");
        map.put("busScope", "经营范围");


        List<Map<String, Object>> list = new ArrayList<>();
        for (String id : ids) {
            HttpRequest request = HttpUtil.createPost("https://119.4.191.25:8081/execute/query/getcompanylicenseillegal");
            request.header("Auth-Code", token);
            HashMap<String, String> param = Maps.newHashMap();
            param.put("uscc", id);
            request.body(JSONUtil.toJsonStr(param));
            HttpResponse response = request.execute();
            Result result = JSONUtil.toBean(response.body(), Result.class);

            List<Map<String, Object>> datas = result.getDatas();
            if (CollUtil.isEmpty(datas)) {
                continue;
            }

            for (Map<String, Object> item : datas) {
                Map<String, Object> d = new HashMap<>(4);
                for (Map.Entry<String, Object> entry : item.entrySet()) {
                    String key = entry.getKey();
                    String s = map.get(key);
                    if (StrUtil.isNotBlank(s)) {
                        key = s;
                    }
                    Object value = entry.getValue();
                    d.put(key, value);
                }
                list.add(d);
            }
        }
        final String title = "统一社会信用代码数据查询";
        ExcelWriter writer = ExcelUtil.getWriter("D:\\data\\" + title + "_" + System.currentTimeMillis() + ".xlsx");
        writer.write(list);
        writer.flush();
    }


    @NoArgsConstructor
    @Data
    public static class Result {

        @JsonProperty("resp_code")
        private Integer code;
        @JsonProperty("resp_code")
        private String message;
        @JsonProperty("datas")
        private List<Map<String, Object>> datas;

        @NoArgsConstructor
        @Data
        public static class DataObject {

            @JsonProperty("busAdd")
            private String busAdd;
            @JsonProperty("currencyKind")
            private String currencyKind;
            @JsonProperty("regCapital")
            private String regCapital;
            @JsonProperty("dishonest")
            private String dishonest;
            @JsonProperty("violateLaw")
            private String violateLaw;
            @JsonProperty("companyName")
            private String companyName;
            @JsonProperty("validStart")
            private String validStart;
            @JsonProperty("adminPunish")
            private String adminPunish;
            @JsonProperty("uscc")
            private String uscc;
            @JsonProperty("partDate")
            private String partDate;
            @JsonProperty("prinType")
            private String prinType;
            @JsonProperty("regMngName")
            private String regMngName;
            @JsonProperty("estabDate")
            private String estabDate;
            @JsonProperty("orgType")
            private String orgType;
            @JsonProperty("vaildEnd")
            private String vaildEnd;
            @JsonProperty("lawSuit")
            private String lawSuit;
            @JsonProperty("abnormalOp")
            private String abnormalOp;
            @JsonProperty("lar")
            private String lar;
            @JsonProperty("isTax")
            private String isTax;
            @JsonProperty("busScope")
            private String busScope;
        }
    }

    @Test
    public void test() {
        HashSet<String> strings = Sets.newHashSet(
                "91510100633124353R",
                "91460000201268919H",
                "915101006721511356",
                "915101007253796015",
                "915101007436412639",
                "91510000X15557143W",
                "91510132577389946W",
                "915101325826249478",
                "911400000870963902",
                "915101003956287821",
                "91350200072810897A",
                "91510132767256861Y",
                "915101325920949787",
                "91510132MA6BTJY55Q",
                "91510106331941155B",
                "91510132558965143B",
                "91510107556420713P",
                "915101145696738161",
                "915100007089237087",
                "91510107MA6CX0AN0T",
                "91510132792161135Q",
                "91510132788121173T",
                "91510132MA61UFUF7G",
                "91510100MABTL84C3R",
                "91510131MA62PKBQ3L",
                "91510100MA6CNEUA58",
                "91510100201919152C",
                "91510108580007881N",
                "9151000058839592XL",
                "91510107774529047H",
                "91510132MA68YL9X50",
                "9151013233206908X0",
                "915100005534711955",
                "91510132MA6CPDQR7Q",
                "91510100MA62NW9T74",
                "915101326962748853",
                "91510100MA69R2TT0C",
                "91510000777927315D",
                "91510000621601108W",
                "915101326675561386",
                "91510132677154271K",
                "91320413MA1NH20242",
                "91510800314545417W",
                "91513330MA62G35901",
                "915101005644952136",
                "91510104780117866B",
                "9151010063316403X6",
                "91510106667563995X",
                "91510132693659456Q",
                "91510132MADX2QHA26",
                "915101007280467143",
                "91510112768602615Q",
                "91510100202370288U",
                "91510100556401299Q",
                "91510132MADBX1310K",
                "91510132690931645U",
                "91510107562015706B",
                "91510100MA61XT788E",
                "510132410210014",
                "91510132758768929G",
                "915101325902150193",
                "91510100201919208F",
                "915101327253613728",
                "91510132MA61WQCG1J",
                "915101002019667683",
                "91510132057498970K",
                "91510104MAACN0YT57",
                "91510132MA6A5XRG78",
                "91510132553562791D",
                "91510132590249796X",
                "91510104MA6APHGH0K",
                "91510100MA6CPP3J8A",
                "91510132MA62BP5A1C",
                "91510100698860212C",
                "91510100MABTBJJ22E",
                "91510132720386386A",
                "915101226796986433",
                "510132746401548",
                "91510132MA6BTMPJ0L",
                "91510132MA6BK7NP7E",
                "91510132MA66PLTK5T",
                "91510132MA6BKPDQ0A",
                "91510100684595999T",
                "91510107MA62MLYM3D",
                "91510100597261907Q",
                "915101070549289399",
                "915101327464391403",
                "91510132396287481B",
                "91530100676570660R",
                "91510132689002291C",
                "91510100592088623B",
                "914201007119306771",
                "911306007006711044",
                "91440300MA5HHTUX4F",
                "91620000665445709L",
                "915101327587639768",
                "915120026627719478",
                "91512002MAD7F2AU35",
                "915000007339743120",
                "91500000MA7EXF4A0M"
        );
        export(strings);
    }

}
