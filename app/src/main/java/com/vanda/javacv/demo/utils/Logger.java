package com.vanda.javacv.demo.utils;

import android.util.Log;

import com.vanda.javacv.demo.BuildConfig;

/**
 * Date    21/09/2017
 * Author  WestWang
 * Log日志工具类
 */

public final class Logger {

    private static String APP_NAME = "IM-";
    private static boolean LOG_E = BuildConfig.DEBUG_LOG;
    private static boolean LOG_W = BuildConfig.DEBUG_LOG;
    private static boolean LOG_I = BuildConfig.DEBUG_LOG;
    private static boolean LOG_D = BuildConfig.DEBUG_LOG;

    public static void e(String tag, String msg) {
        if (LOG_E) {
            tag = APP_NAME + tag;
            Log.e(tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable t) {
        if (LOG_E) {
            tag = APP_NAME + tag;
            Log.e(tag, msg, t);
        }
    }

    public static void w(String tag, String msg) {
        if (LOG_W) {
            tag = APP_NAME + tag;
            Log.w(tag, msg);
        }
    }

    public static void w(String tag, String msg, Throwable t) {
        if (LOG_W) {
            tag = APP_NAME + tag;
            Log.w(tag, msg, t);
        }
    }

    public static void i(String tag, String msg) {
        if (LOG_I) {
            tag = APP_NAME + tag;
            Log.i(tag, msg);
        }
    }

    public static void i(String tag, String msg, Throwable t) {
        if (LOG_I) {
            tag = APP_NAME + tag;
            Log.i(tag, msg, t);
        }
    }

    public static void d(String tag, String msg) {
        if (LOG_D) {
            tag = APP_NAME + tag;
            Log.d(tag, msg);
        }
    }

    public static void d(String tag, String msg, Throwable t) {
        if (LOG_D) {
            tag = APP_NAME + tag;
            Log.d(tag, msg, t);
        }
    }
}
