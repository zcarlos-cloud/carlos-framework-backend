package com.carlos.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

class PasswordUtilTest {

    @Test
    void ruanzhu() {
        File end = FileUtil.newFile("D:\\sipu.txt");
        // ThreadPoolExecutor pool = ExecutorUtil.POOL;

        FileUtil.walkFiles(new File("D:\\ide_project\\census-backend"), new Consumer<File>() {
            @Override
            public void accept(File file) {
                // pool.execute(()->{
                if (file.isDirectory()) {
                    return;
                }
                if (!FileUtil.extName(file).equals("java")) {
                    return;
                }

                List<String> list = new LinkedList<>();
                List<String> strings = FileUtil.readUtf8Lines(file);
                for (String string : strings) {
                    String trim = StrUtil.trim(string);
                    if (StrUtil.isBlank(trim)) {
                        continue;
                    }
                    if (trim.startsWith("package")) {
                        continue;
                    }
                    if (trim.startsWith("/*")) {
                        continue;
                    }
                    if (trim.startsWith("*")) {
                        continue;
                    }
                    list.add(string);
                }
                FileUtil.appendUtf8Lines(list, end);
                // });
            }
        });


    }

    @Test
    void sipu() {
        File end = FileUtil.newFile("D:\\sipu_web.txt");
        // ThreadPoolExecutor pool = ExecutorUtil.POOL;

        FileUtil.walkFiles(new File("D:\\ide_project\\census-web\\src"), new Consumer<File>() {
            @Override
            public void accept(File file) {
                // pool.execute(()->{
                if (file.isDirectory()) {
                    return;
                }
                // if (!FileUtil.extName(file).equals("java")) {
                //     return;
                // }

                List<String> list = new LinkedList<>();
                List<String> strings = FileUtil.readUtf8Lines(file);
                for (String string : strings) {
                    String trim = StrUtil.trim(string);
                    if (StrUtil.isBlank(trim)) {
                        continue;
                    }
                    if (trim.startsWith("//")) {
                        continue;
                    }
                    if (trim.startsWith("/*")) {
                        continue;
                    }
                    if (trim.startsWith("*")) {
                        continue;
                    }
                    if (trim.startsWith("<!--")) {
                        continue;
                    }
                    list.add(string);
                }
                FileUtil.appendUtf8Lines(list, end);
                // });
            }
        });


    }
}
