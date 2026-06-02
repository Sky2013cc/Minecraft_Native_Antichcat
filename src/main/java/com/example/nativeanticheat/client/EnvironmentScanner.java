package com.example.nativeanticheat.client;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * 多维度检测：从 JVM、线程、系统属性等角度寻找作弊客户端痕迹。
 * 所有结果作为“可疑项”上报，由服务端综合判断，不在客户端做封禁。
 */
public class EnvironmentScanner {

    // 已知作弊客户端常见的类名/包名特征
    private static final String[] KNOWN_CHEAT_CLASSES = {
            "net.wurstclient",       // Wurst
            "me.zero.alpine",        // 部分客户端使用的事件库
            "com.lambda",            // Lambda
            "minegame159.meteor",    // Meteor (旧包名)
            "meteordevelopment",     // Meteor
            "dev.boze",
            "wtf.expensive"
    };

    private static final String[] SUSPICIOUS_THREAD_NAMES = {
            "wurst", "meteor", "baritone-cheat", "clicker", "autoclick"
    };

    public static List<String> scan() {
        List<String> hits = new ArrayList<>();

        scanLoadedClasses(hits);
        scanThreads(hits);
        scanJvmArgs(hits);
        scanSystemProperties(hits);

        return hits;
    }

    /**
     * 检测已加载的可疑类（通过反射访问类加载器尝试加载）
     */
    private static void scanLoadedClasses(List<String> hits) {
        for (String className : KNOWN_CHEAT_CLASSES) {
            try {
                // 不初始化类，仅检测是否可被加载
                Class.forName(className, false,
                        EnvironmentScanner.class.getClassLoader());
                hits.add("class:" + className);
            } catch (Throwable ignored) {
                // 未找到则正常
            }
        }
    }

    /**
     * 检测可疑线程名（很多作弊功能会开后台线程，如自动点击）
     */
    private static void scanThreads(List<String> hits) {
        try {
            ThreadGroup root = Thread.currentThread().getThreadGroup();
            while (root.getParent() != null) {
                root = root.getParent();
            }
            Thread[] threads = new Thread[root.activeCount() * 2];
            int count = root.enumerate(threads, true);

            for (int i = 0; i < count; i++) {
                Thread t = threads[i];
                if (t == null) continue;
                String name = t.getName().toLowerCase();
                for (String sus : SUSPICIOUS_THREAD_NAMES) {
                    if (name.contains(sus)) {
                        hits.add("thread:" + t.getName());
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 检测 JVM 启动参数中的注入痕迹
     */
    private static void scanJvmArgs(List<String> hits) {
        try {
            List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
            for (String arg : args) {
                if (arg.contains("-javaagent") || arg.contains("-agentlib")) {
                    hits.add("jvmarg:" + arg);
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 检测系统属性中的异常配置
     */
    private static void scanSystemProperties(List<String> hits) {
        // 检测 Mixin 调试导出等可能伴随破解工具出现的属性
        String mixinDebug = System.getProperty("mixin.debug.export");
        if ("true".equalsIgnoreCase(mixinDebug)) {
            hits.add("prop:mixin.debug.export");
        }
    }
}