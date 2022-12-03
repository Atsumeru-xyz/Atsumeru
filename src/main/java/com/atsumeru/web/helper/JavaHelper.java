package com.atsumeru.web.helper;

import lombok.Getter;
import lombok.Setter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.JarURLConnection;
import java.text.SimpleDateFormat;

public class JavaHelper {
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyy-MM-dd");
    @Getter
    @Setter
    private static boolean isDebug = false;

    public static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }

    public static boolean isMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac");
    }

    public static boolean isUnix() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("nix") || os.contains("nux");
    }

    public static boolean isAndroid() {
        boolean isAndroid;
        try {
            Class.forName("android.app.Activity");
            isAndroid = true;
        } catch (ClassNotFoundException e) {
            isAndroid = false;
        }
        return isAndroid;
    }

    public static String getAppVersion(Class<?> cls) {
        String jarVersion = cls.getPackage().getImplementationVersion();
        String result;
        if (jarVersion != null && jarVersion.length() > 0) {
            result = jarVersion;
        } else {
            result = "debug";
        }
        try {
            String rn = cls.getName().replace('.', '/') + ".class";
            JarURLConnection j = (JarURLConnection)ClassLoader.getSystemResource(rn).openConnection();
            long time = j.getJarFile().getEntry("META-INF/MANIFEST.MF").getTime();
            return result + "-" + JavaHelper.SIMPLE_DATE_FORMAT.format(time);
        }
        catch (Exception e) {
            return result;
        }
    }

    public static String stackTraceToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            e.printStackTrace(pw);
            return sw.toString();
        }
        finally {
            pw.close();
        }
    }
}
