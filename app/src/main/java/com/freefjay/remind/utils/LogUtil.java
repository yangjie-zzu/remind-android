package com.freefjay.remind.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.freefjay.remind.MyApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

public class LogUtil {

    private static final String prefix = "app";

    public static Logger configureLogback(Class<?> clazz) {
        File file = MyApplication.getInstance().getExternalFilesDir("log");
        String logDir = file.getAbsolutePath();
        Log.i("", "configureLogback: logDir: " + file.getAbsolutePath());
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.w("", "configureLogback: 日志目录创建失败");
            }
        }
        Log.i("", "configureLogback: 配置日志");
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.stop();

        // setup FileAppender
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(lc);
        encoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.start();

        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(lc);

        String outputPath = logDir + "/"+prefix+".txt";

        Log.d("debug", "outputpath = " + outputPath);
        fileAppender.setFile(outputPath);
        fileAppender.setEncoder(encoder);

        //设置rolling policy
        TimeBasedRollingPolicy<?> rollingPolicy = new TimeBasedRollingPolicy<>();
        rollingPolicy.setFileNamePattern(logDir + "/" + prefix + "_%d{yyyyMMdd}.txt");   //rollover action based on filepattern
        rollingPolicy.setMaxHistory(60);    //设置最大的存档文件个数
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setContext(lc);
        rollingPolicy.start();
        fileAppender.setRollingPolicy(rollingPolicy);

        fileAppender.start();

        LogcatAppender logcatAppender = new LogcatAppender();
        logcatAppender.setContext(lc);
        logcatAppender.setEncoder(encoder);
        logcatAppender.start();

        // add the newly created appenders to the root logger;
        // qualify Logger to disambiguate from org.slf4j.Logger
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(clazz);
        root.addAppender(fileAppender);
        root.addAppender(logcatAppender);
        return root;
    }

    public static Logger getLogger(Class<?> clazz) {
        return configureLogback(clazz);
    }

}
