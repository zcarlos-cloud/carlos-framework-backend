package com.carlos.org.service;

import java.util.concurrent.ThreadLocalRandom;

public class RandomPersonUtil {

    /* ==================== 1. 姓氏池 ==================== */
    private static final String[] SURNAME = {
        "赵", "钱", "孙", "李", "周", "吴", "郑", "王", "冯", "陈",
        "褚", "卫", "蒋", "沈", "韩", "杨", "朱", "秦", "尤", "许",
        "何", "吕", "施", "张", "孔", "曹", "严", "华", "金", "魏",
        "陶", "姜", "戚", "谢", "邹", "喻", "柏", "水", "窦", "章",
        "云", "苏", "潘", "葛", "奚", "范", "彭", "郎", "鲁", "韦",
        "昌", "马", "苗", "凤", "花", "方", "俞", "任", "袁", "柳",
        "酆", "鲍", "史", "唐", "费", "廉", "岑", "薛", "雷", "贺",
        "倪", "汤", "滕", "殷", "罗", "毕", "郝", "邬", "安", "常",
        "乐", "于", "时", "傅", "皮", "卞", "齐", "康", "伍", "余",
        "元", "卜", "顾", "孟", "平", "黄", "和", "穆", "萧", "尹"
    };

    /* ==================== 2. 名字池 ==================== */
    private static final String[] NAME_CHAR = {
        "伟", "芳", "娜", "敏", "静", "丽", "强", "磊", "洋", "勇",
        "艳", "杰", "娟", "涛", "明", "超", "秀", "兰", "欣", "波",
        "宇", "浩", "柏", "毅", "丞", "威", "建", "邦", "承", "乐",
        "诚", "睿", "涵", "航", "达", "鸿", "霖", "宏", "嘉", "哲",
        "怡", "海", "峰", "鑫", "宇", "阳", "建", "航", "浩", "华",
        "彤", "佳", "梓", "晨", "瑞", "博", "颖", "宇", "涵", "雨",
        "铭", "思", "彤", "子", "俊", "泽", "轩", "怡", "洋", "辰"
    };

    /* ==================== 3. 手机号段池 ==================== */
    private static final String[] MOBILE_PREFIX = {
        "133", "149", "153", "173", "177", "180", "181", "189", "199", // 电信
        "130", "131", "132", "145", "155", "156", "166", "175", "176", "185", "186", "196", // 联通
        "134", "135", "136", "137", "138", "139", "147", "150", "151", "152", "157", "158", "159", "172", "178", "182", "183", "184", "187", "188", "195", "197", "198" // 移动
    };

    /* ==================== 4. 生成姓名 ==================== */
    public static String randomName() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        String surname = SURNAME[r.nextInt(SURNAME.length)];
        int len = r.nextInt(2, 4); // 2 或 3 个字
        StringBuilder sb = new StringBuilder(surname);
        for (int i = 1; i < len; i++) {
            sb.append(NAME_CHAR[r.nextInt(NAME_CHAR.length)]);
        }
        return sb.toString();
    }

    /* 可指定性别（简单规则：男名常用字 / 女名常用字） */
    public static String randomName(boolean male) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        String surname = SURNAME[r.nextInt(SURNAME.length)];
        int len = r.nextInt(2, 4);
        StringBuilder sb = new StringBuilder(surname);
        for (int i = 1; i < len; i++) {
            // 简易性别倾向：男用“伟宇泽洋峰”，女用“娜婷雪欣雅”
            String pool = male ? "伟宇泽洋峰强斌健凯龙飞" : "娜婷雪欣雅娟妍婧雯婕茜";
            if (pool.length() > 0) {
                sb.append(pool.charAt(r.nextInt(pool.length())));
            } else {
                sb.append(NAME_CHAR[r.nextInt(NAME_CHAR.length)]);
            }
        }
        return sb.toString();
    }

    /* ==================== 5. 生成手机号 ==================== */
    public static String randomMobile() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        String prefix = MOBILE_PREFIX[r.nextInt(MOBILE_PREFIX.length)];
        StringBuilder sb = new StringBuilder(prefix);
        // 第 4 位 3-9
        sb.append(r.nextInt(3, 10));
        // 剩余 7 位随机
        for (int i = 0; i < 7; i++) {
            sb.append(r.nextInt(0, 10));
        }
        return sb.toString();
    }

    /* 带空格美观输出 139 1234 5678 */
    public static String randomMobilePretty() {
        String m = randomMobile();
        return new StringBuilder(m).insert(3, ' ').insert(8, ' ').toString();
    }

}
