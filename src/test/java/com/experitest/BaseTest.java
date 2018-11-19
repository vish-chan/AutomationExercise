package com.experitest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

public class BaseTest {
	final static private String host = "localhost";
	final static private int port = 8889;
	final static private String os="all";
	final static private String device_name="all";
	final static private String test_name="all";
	final static private String projectBaseDirectory = System.getProperty("user.dir");
	final static private String reportsBase = "Run_" + System.currentTimeMillis();
	final static private long duration = -1;
	static private Properties props = null;

	public static void loadProperties() {
		props = new Properties();
		File f = new File("test.properties");
		InputStream is = null;
		try {
			is = new FileInputStream(f);
			props.load(is);
		} catch (IOException e1) {
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		Enumeration<?> e = props.propertyNames();
		while(e.hasMoreElements()) {
			String k = (String) e.nextElement();
			System.out.println(k+":"+props.getProperty(k));
		}
	}

	public static String getHost() {
		return props.getProperty("host", host);
	}

	public static int getPort() {
		return Integer.parseInt(props.getProperty("port", ""+port));
	}

	public static String getOS() {
		return props.getProperty("os", os);
	}

	public static String getDeviceName() {
		return props.getProperty("device_name", device_name);
	}

	public static String getTestName() {
		return props.getProperty("test_name", test_name);
	}
	
	public static long getTestDuration() {
		return Long.parseLong(props.getProperty("test_duration", ""+duration));
	}

	public static String getProjectbasedirectory() {
		return projectBaseDirectory;
	}

	public static String getReportsbase() {
		return reportsBase;
	}
	
	
}
