package com.experitest;

import com.experitest.client.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 *
 */
public class IOSSuite implements Runnable {

	private CustomClient client = null;
	Map<String, List<String>> failuresMap = new HashMap<String, List<String>>();
	String reportsBase;
	boolean beReleased = false;
	Map<String, Method> testFuncMap = new HashMap<>();
	Map<String, String> testAppMap = new HashMap<>();
	String[] testNames = { "Eribank Login", "Eribank Payment", "ESPN Menu Text", "ESPN Menu Click", "Playstore Install",
			"Playstore Top Apps" };
	String[] methodNames = { "testEribankLogin", "testEribankPayment", "testESPNMenuText", "testESPNMenuClick",
			"testPlayStoreInstall", "testPlayStoreTopApps" };
	String[] appPaths = { "applications\\eribank.apk", "applications\\eribank.apk", null, null, null, null };

	public IOSSuite(String devname, String host, int port, String projectBaseDirectory, String reportsbase) {
		String test = BaseTest.getTestName();
		for (int i = 0; i < testNames.length; i++) {
			if (test.equals("all") || test.equals(testNames[i])) {
				try {
					testFuncMap.put(testNames[i], IOSSuite.class.getMethod(methodNames[i]));
					testAppMap.put(testNames[i], appPaths[i]);
				} catch (NoSuchMethodException | SecurityException e) {
					e.printStackTrace();
				}
			}
		}
		this.init(devname, host, port, projectBaseDirectory, reportsbase);
	}

	public void init(String devname, String host, int port, String baseDirectory, String reportsBaseDirectory) {
		System.out.println("Init for device: " + devname);
		client = new CustomClient(host, port, true);
		client.setProjectBaseDirectory(baseDirectory + "\\" + BaseTest.getProjectDirectory());
		if (devname.equals("cloud")) {
			try {
				devname = client.waitForDevice("@os='ios' and @category='PHONE'", 50000);
				beReleased = true;
				System.out.println("Init for device: " + devname);
			} catch (InternalException e) {
				System.err.println("Wait for device returned with error " + e.getMessage());
				return;
			}
		} else {
			try {
				client.setDevice(devname);
			} catch (InternalException e) {
				System.err.println("setDevice returned with error " + e.getMessage());
				return;
			}
		}
		client.setConnDeviceName(devname);
		client.openDevice();
		client.customSetNetworkConnection("wifi");

		this.reportsBase = baseDirectory + "\\" + reportsBaseDirectory + "\\" + devname.replaceAll("\\W", "_");
		try {
			Files.createDirectories(Paths.get(reportsBase));
		} catch (IOException e) {
			System.err.println("Couldn't create directory!");
		}
	}

	@Override
	public void run() {
		if(client.getConnDeviceName()==null)
			return;
		long test_duration = BaseTest.getTestDuration() * 60 * 1000;
		int i = 0, run = 1;
		while (true) {
			long start_time = System.currentTimeMillis();
			for (String key : testFuncMap.keySet()) {
				Method m = testFuncMap.get(key);
				client.setReporter("xml", reportsBase, key);
				try {
					m.invoke(this);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
						| InternalException e) {
					List<String> l = new ArrayList<>();
					l.add(e.getCause().getMessage());
					failuresMap.put(key, l);
					System.out.println(failuresMap.get(key));
					/*
					 * client.collectSupportData(this.reportsBase+"\\test"+i,
					 * BaseTest.getProjectbasedirectory() +
					 * "\\" + testAppMap.get(key), client.getConnDeviceName(), "", "",
					 * e.getCause().getMessage());
					 */
					try {
						m.invoke(this);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
							| InternalException e2) {
						l = failuresMap.get(key);
						l.add(e.getCause().getMessage());
						failuresMap.put(key, l);
						System.out.println(failuresMap.get(key));
						/*
						 * client.collectSupportData(this.reportsBase+"\\test"+i,
						 * BaseTest.getProjectbasedirectory() +
						 * "\\" + testAppMap.get(key), client.getConnDeviceName(), "", "",
						 * e.getCause().getMessage());
						 */
						try {
							client.reboot(150000);
							m.invoke(this);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
								| InternalException e3) {
							l = failuresMap.get(key);
							l.add(e.getCause().getMessage());
							failuresMap.put(key, l);
							System.out.println(failuresMap.get(key));
							/*
							 * client.collectSupportData(this.reportsBase+"\\test"+i,
							 * BaseTest.getProjectbasedirectory() +
							 * "\\" + testAppMap.get(key), client.getConnDeviceName(), "", "",
							 * e.getCause().getMessage());
							 */
						}
					}
				}
				client.generateReport(false);
				i++;
			}
			sendReportSummary(run);
			long end_time = System.currentTimeMillis();
			test_duration = test_duration - (end_time - start_time);
			if (test_duration <= 0)
				break;
			run++;
		}
		tearDown();
	}

	public void testEribankLogin() throws InternalException {
		client.customLaunchInstrument("com.experitest.ExperiBank");

		String csvUserName = null;
		String csvPassword = null;
		Scanner inputStream = null;
		String finalMessage = null;
		boolean failure = false;
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
			if (client.isElementFound("NATIVE", "xpath=//*[@text='Invalid username or password!']", 0)) {
				if ((csvUserName.equals("company") && csvPassword.equals("company"))) {
					finalMessage = "Unable to login for username-password: " + csvUserName + "-" + csvPassword;
					failure = true;
					break;
				}
				client.click("NATIVE", "xpath=//*[@text='Dismiss']", 0, 1);
			} else if (client.isElementFound("NATIVE", "text=Make Payment", 0)) {
				if (!(csvUserName.equals("company") && csvPassword.equals("company"))) {
					finalMessage = "Wrong login for username-password: " + csvUserName + "-" + csvPassword;
					failure = true;
				}
				break;
			} else
				break;
		}
		if (inputStream != null)
			inputStream.close();
		if (failure) {
			client.report(finalMessage, false);
			throw new InternalException(null, finalMessage, null);
		}
	}

	public void testEribankPayment() throws InternalException {
		client.customLaunchInstrument("com.experitest.ExperiBank");

		client.elementSendText("NATIVE", "xpath=//*[@placeholder='Username']", 0, "company");
		client.elementSendText("NATIVE", "xpath=//*[@placeholder='Password']", 0, "company");
		client.click("NATIVE", "xpath=//*[@text='Login']", 0, 1);

		client.waitForElement("NATIVE", "xpath=//*[@text='Make Payment']", 0, 5000);

		String initial_balance_str = client.getTextIn("NATIVE", "xpath=//*[@text='Make Payment']", 0, "TEXT", "Up", 0,
				150);
		Double initial_balance = 0D;
		try {
			initial_balance = client.getPaymentFromString(initial_balance_str);
		} catch (InternalException e) {
			client.report(e.getMessage(), false);
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

		String final_balance_str = client.getTextIn("NATIVE", "xpath=//*[@text='Make Payment']", 0, "TEXT", "Up", 0,
				150);
		Double final_balance = 0D;
		try {
			final_balance = client.getPaymentFromString(final_balance_str);
		} catch (InternalException e) {
			client.report(e.getMessage(), false);
		}

		if ((double) final_balance == (double) (initial_balance - amount))
			client.report("Balance check result correct", true);
		else {
			client.report("Balance check result incorrect", false);
			throw new InternalException(null, "Wrong balance", null);
		}
	}

	public void testPlayStoreInstall() throws InternalException {
		client.customLaunchUnInstrument("com.apple.AppStore");
		client.click("NATIVE", "xpath=//*[@text='Games']", 0, 1);
		if (!client.swipeWhileNotFound("Down", 592, 3215, "default", "app_ios", 0, 0, 6, true)) {
			throw new InternalException(null, "Unable to find app for installation", null);
		}
		client.sleep(1000);
		client.click("NATIVE", "xpath=//*[@text='GET']", 0, 1);
		client.click("NATIVE", "xpath=//*[@text='Install']", 0, 1);
		client.click("NATIVE", "xpath=//*[@text='Cancel']", 0, 1);
	}

	public void testPlayStoreTopApps() throws InternalException {
		client.customLaunchUnInstrument("com.apple.AppStore");
		client.click("NATIVE", "xpath=//*[@text='Games']", 0, 1);
		if (!client.swipeWhileNotFound("Down", 500, 2000, "NATIVE",
				"xpath=//*[@text='See All' and ./parent::*[@text='Top Free']]", 0, 1000, 5, true)) {
			throw new InternalException(null, "Unable to find top free section", null);
		}
		for (int i = 0; i < 10; i++) {
			String app_name = client.elementGetText("NATIVE",
					"xpath=((//*[@class='UIAView' and ./parent::*[@class='UIAView' and ./parent::*[@class='UIAView' and ./parent::*[@class='UIAView']]]]/*/*[@class='UIACollectionView'])[2]/*[@text and @class='UIAView' and ./*[@text]])",
					i);
			if (app_name == null || app_name.equals(""))
				throw new InternalException(null, "Unable to find top app no. " + i, null);
			client.report("App no. " + (i + 1) + " is " + app_name, true);
		}
	}

	public void testESPNMenuText() throws InternalException {
		client.launch("chrome:http://www.espn.com", true, false);
		client.waitForElement("WEB", "text=Menu", 0, 10000);
		client.click("WEB", "text=Menu", 0, 1);
		String[] menuitems = { "Search", "Sports", "ESPN+", "Watch", "Listen", "Fantasy", "More" };
		StringBuilder failures = new StringBuilder();
		boolean failed = false;
		for (String menuitem : menuitems) {
			if (!(client.waitForElement("WEB", "text=" + menuitem, 0, 10000))) {
				client.report("Text not found: " + menuitem, false);
				failed = true;
				failures.append(menuitem + ",");
			}
		}
		if (failed)
			throw new InternalException(null, "Following elements not found: " + failures.toString(), null);
	}

	public void testESPNMenuClick() throws InternalException {
		client.launch("chrome:http://www.espn.com", true, false);
		client.waitForElement("WEB", "text=Menu", 0, 10000);
		client.click("WEB", "text=Menu", 0, 1);
		String[] menuitems = { "Search", "Sports", "ESPN+", "Watch", "Listen", "Fantasy", "More" };
		StringBuilder failures = new StringBuilder();
		boolean failed = false;
		for (String menuitem : menuitems) {
			try {
				client.click("WEB", "text=" + menuitem, 0, 1);
			} catch (InternalException e) {
				client.report("Unable to click: " + menuitem, false);
				failed = true;
				failures.append(menuitem + ",");
			}
		}
		if (failed)
			throw new InternalException(null, "Unable to click following elements: " + failures.toString(), null);
	}

	public void sendReportSummary(int run) {
		FinalReporter finalReporter = FinalReporter.getInstance();
		finalReporter.addRow(run, client.getConnDeviceName(), client.getDeviceProperty("device.sn"), testFuncMap.size(),
				failuresMap);
	}

	public void tearDown() {
		client.closeDevice();
		if (beReleased)
			client.releaseDevice("", false, false, true);
		client.releaseClient();
	}
}
