package com.experitest;

import com.experitest.client.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 *
 */
public class IOSSuite implements Runnable {

	private Client client = null;
	Map<String, List<String>> failures;
	String reportsBase;
	Map<String, Method> testFuncMap = new HashMap<>();
	String[] testNames = { "Eribank Login", "Eribank Payment", "ESPN" };
	String[] methodNames = { "testEribankLogin", "testEribankPayment", "testESPN" };

	public IOSSuite(String devname, String host, int port, String projectBaseDirectory, String reportsbase) {
		String test = BaseTest.getTestName();
		for (int i = 0; i < testNames.length; i++) {
			if (test.equals("all") || test.equals(testNames[i])) {
				try {
					testFuncMap.put(testNames[i], IOSSuite.class.getMethod(methodNames[i]));
				} catch (NoSuchMethodException | SecurityException e) {
					e.printStackTrace();
				}
			}
		}
		this.init(devname, host, port, projectBaseDirectory, reportsbase);
	}

	public void init(String devname, String host, int port, String projectBaseDirectory, String reportsbase) {
		System.out.println("Init for device: " + devname);
		client = new Client(host, port, true);
		client.setProjectBaseDirectory(projectBaseDirectory);
		client.setDevice(devname);
		this.reportsBase = reportsbase;
		failures = new HashMap<String, List<String>>();
	}

	@Override
	public void run() {
		for (String key:testFuncMap.keySet()) {
			Method m = testFuncMap.get(key);
			client.setReporter("xml", reportsBase, key);
			try {
				m.invoke(this);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| InternalException e) {
				List<String> l = failures.get(key);
				if (l == null)
					l = new ArrayList<>();
				l.add(e.getCause().getMessage());
				failures.put(key, l);
				System.out.println(failures.get(key));
				try {
					m.invoke(this);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
						| InternalException e2) {
					l = failures.get(key);
					if (l == null)
						l = new ArrayList<>();
					l.add(e.getCause().getMessage());
					failures.put(key, l);
					System.out.println(failures.get(key));
					try {
						client.reboot(150000);
						m.invoke(this);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
							| InternalException e3) {
						l = failures.get(key);
						if (l == null)
							l = new ArrayList<>();
						l.add(e.getCause().getMessage());
						failures.put(key, l);
						System.out.println(failures.get(key));
					}
				}
			}
			client.generateReport(false);
		}
		tearDown();
	}

	public void testEribankLogin() throws InternalException {
		try {
			client.launch("com.experitest.ExperiBank", true, true);
		} catch (InternalException e) {
			client.install("com.experitest.ExperiBank", true, false);
			client.launch("com.experitest.ExperiBank", true, true);
		}
		String csvUserName = null;
		String csvPassword = null;
		Scanner inputStream = null;
		try {
			inputStream = new Scanner(new File("users.csv"));
		} catch (FileNotFoundException e) {
			client.report("Unable to open file", false);
			e.printStackTrace();
			return;
		}
		while (inputStream.hasNext()) {
			String data = inputStream.nextLine(); // Read line
			String[] values = data.split(","); // Split the line to an array
			if (values.length != 2)
				continue;
			csvUserName = values[0];
			csvPassword = values[1];
			client.elementSendText("NATIVE", "xpath=//*[@placeholder='Username']", 0, csvUserName);
			client.elementSendText("NATIVE", "xpath=//*[@placeholder='Password']", 0, csvPassword);
			client.click("NATIVE", "xpath=//*[@text='Login']", 0, 1);
			if (client.isElementFound("NATIVE", "xpath=//*[@text='Invalid username or password!']", 0))
				client.click("NATIVE", "xpath=//*[@text='Dismiss']", 0, 1);
			else
				break;
		}
		if (inputStream != null)
			inputStream.close();
	}

	public void testEribankPayment() throws InternalException {
		try {
			client.launch("com.experitest.ExperiBank", true, true);
		} catch (InternalException e) {
			client.install("com.experitest.ExperiBank", true, false);
			client.launch("com.experitest.ExperiBank", true, true);
		}
		client.elementSendText("NATIVE", "xpath=//*[@placeholder='Username']", 0, "company");
		client.elementSendText("NATIVE", "xpath=//*[@placeholder='Password']", 0, "company");
		client.click("NATIVE", "xpath=//*[@text='Login']", 0, 1);

		client.waitForElement("NATIVE", "xpath=//*[@text='Make Payment']", 0, 5000);

		String str0 = client.getTextIn("NATIVE", "xpath=//*[@text='Make Payment']", 0, "TEXT", "Up", 0, 150);
		str0 = str0.trim();
		Double initial_balance = 0D;
		try {
			initial_balance = Double.parseDouble(str0.substring(0, str0.length() - 1));
		} catch (NumberFormatException e) {
			client.report("Not able to parse initial balance correctly, string received: " + str0, false);
		}
		client.click("NATIVE", "xpath=//*[@text='Make Payment']", 0, 1);
		client.elementSendText("NATIVE", "xpath=//*[@placeholder='Phone']", 0, "99999999");

		client.elementSendText("NATIVE", "xpath=//*[@placeholder='Name']", 0, "Demo");
		Double amount = 1000.0;
		client.elementSendText("NATIVE", "xpath=//*[@placeholder='Amount']", 0, "" + amount);
		client.click("NATIVE", "xpath=//*[@text='Select']", 0, 1);
		client.click("NATIVE", "xpath=//*[@text='Iceland']", 0, 1);
		client.click("NATIVE", "xpath=//*[@text='Send Payment']", 0, 1);
		client.click("NATIVE", "xpath=//*[@text='Yes']", 0, 1);

		String str1 = client.getTextIn("NATIVE", "xpath=//*[@text='Make Payment']", 0, "TEXT", "Up", 0, 150);
		str1 = str1.trim();
		Double final_balance = 0D;
		try {
			final_balance = Double.parseDouble(str1.substring(0, str1.length() - 1));
		} catch (NumberFormatException e) {
			client.report("Not able to parse final balance correctly, string received: " + str1, false);
		}

		client.report("Balance check result", (double) final_balance == (double) (initial_balance - amount));
	}

	public void testESPN() throws InternalException {
		try {
			if (!client.getNetworkConnection("wifi"))
				try {
					client.setNetworkConnection("wifi", true);
				} catch (InternalException e) {
					client.report("Wifi not set, cannot change Wifi state", false);
				}
		} catch (InternalException e) {
			client.report("Cannot check Wifi state", false);
		}
		client.launch("safari:http://www.espn.com", true, false);
		if (client.waitForElement("WEB", "id=global-nav-mobile-trigger", 0, 10000))
			client.click("WEB", "id=global-nav-mobile-trigger", 0, 1);
		String[] menuitems = { "Sports", "ESPN+", "Watch", "Listen", "Fantasy", "More" };
		for (String s : menuitems) {
			if (client.waitForElement("WEB", "text=" + s, 0, 5000))
				client.click("WEB", "text=" + s, 0, 1);
			else
				client.report("Element not found: " + s, false);
		}
	}

	public void tearDown() {
		client.releaseClient();
	}
}
