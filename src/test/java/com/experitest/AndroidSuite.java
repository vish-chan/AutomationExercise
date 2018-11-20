package com.experitest;

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

import com.experitest.client.InternalException;

/**
 *
 */
public class AndroidSuite implements Runnable {

	private CustomClient client = null;
	String reportsBase;
	boolean beReleased = false;
	Map<String, Method> testFuncMap = new HashMap<>();
	Map<String, String> testAppMap = new HashMap<>();
	Map<String, List<String>> failuresMap = new HashMap<String, List<String>>();
	String[] testNames = { "Eribank Login", "Eribank Payment", "TouchMeNot Login", "TouchMeNot Play", "ESPN Menu Text",
			"ESPN Menu Click",
			"Playstore Install", "Playstore Top Apps" };
	String[] methodNames = { "testEribankLogin", "testEribankPayment", "testTouchMeNotLogin", "testTouchMeNotPlay",
			"testESPNMenuText", "testESPNMenuClick", "testPlayStoreInstall", "testPlayStoreTopApps" };
	String[] appPaths = { "applications\\eribank.apk", "applications\\eribank.apk", "applications\\TouchMeNot.apk",
			"applications\\TouchMeNot.apk", null, null, null, null };

	public AndroidSuite(String devname, String host, int port, String projectBaseDirectory, String reportsBase) {
		String test = BaseTest.getTestName();
		for (int i = 0; i < testNames.length; i++) {
			if (test.equals("all") || test.equals(testNames[i])) {
				try {
					testFuncMap.put(testNames[i], AndroidSuite.class.getMethod(methodNames[i]));
					testAppMap.put(testNames[i], appPaths[i]);
				} catch (NoSuchMethodException | SecurityException e) {
					e.printStackTrace();
				}
			}
		}
		this.init(devname, host, port, projectBaseDirectory, reportsBase);
	}

	public void init(String devname, String host, int port, String baseDirectory, String reportsBaseDirectory) {
		System.out.println("Init for device: " + devname);
		client = new CustomClient(host, port, true);
		client.setProjectBaseDirectory(baseDirectory+"\\"+BaseTest.getProjectDirectory());
		if (devname.equals("cloud")) {
			devname = client.waitForDevice("@os='android' AND @added='false'", 30000);
			beReleased = true;
			System.out.println("Init for device: " + devname);
		} else {
			client.setDevice(devname);
		}
		client.setConnDeviceName(devname);
		client.openDevice();
		this.reportsBase = baseDirectory + "\\" + reportsBaseDirectory + "\\" + devname.replaceAll("\\W", "_");
		try {
			Files.createDirectories(Paths.get(reportsBase));
		} catch (IOException e) {
			System.err.println("Couldn't create directory!");
		}
	}

	@Override
	public void run() {
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
						 * BaseTest.getProjectbasedirectory() + "\\" + testAppMap.get(key),
						 * client.getConnDeviceName(), "", "", e.getCause().getMessage());
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
							 * BaseTest.getProjectbasedirectory() + "\\" + testAppMap.get(key),
							 * client.getConnDeviceName(), "", "", e.getCause().getMessage());
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
		// client.customInstallInstrumented("com.experitest.ExperiBank/.LoginActivity");
		client.customLaunchInstrument("com.experitest.ExperiBank/.LoginActivity");
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
			client.elementSendText("NATIVE", "hint=Username", 0, csvUserName);
			client.elementSendText("NATIVE", "hint=Password", 0, csvPassword);
			client.click("NATIVE", "text=Login", 0, 1);
			if (client.isElementFound("NATIVE", "xpath=//*[@text='Invalid username or password!']", 0)) {
				if ((csvUserName.equals("company") && csvPassword.equals("company"))) {
					finalMessage = "Unable to login for username-password: " + csvUserName + "-" + csvPassword;
					failure = true;
					break;
				}
				client.click("NATIVE", "text=Close", 0, 1);
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
		client.customLaunchInstrument("com.experitest.ExperiBank/.LoginActivity");
		client.elementSendText("NATIVE", "hint=Username", 0, "company");
		client.elementSendText("NATIVE", "hint=Password", 0, "company");
		client.click("NATIVE", "text=Login", 0, 1);
		client.waitForElement("NATIVE", "text=Make Payment", 0, 5000);

		String initial_balance_str = client.getTextIn("NATIVE", "text=Make Payment", 0, "TEXT", "Up", 0, 200);
		Double initial_balance = 0D;
		try {
			initial_balance = client.getPaymentFromString(initial_balance_str);
		} catch (InternalException e) {
			client.report(e.getMessage(), false);
		}
		client.click("NATIVE", "text=Make Payment", 0, 1);
		client.elementSendText("NATIVE", "hint=Phone", 0, "99999999");
		client.elementSendText("NATIVE", "hint=Name", 0, "Demo");

		Double amount = 10.0;
		client.elementSendText("NATIVE", "hint=Amount", 0, "" + amount);
		client.click("NATIVE", "text=Select", 0, 1);
		client.click("NATIVE", "text=Greenland", 0, 1);
		client.click("NATIVE", "text=Send Payment", 0, 1);
		client.click("NATIVE", "text=Yes", 0, 1);

		String final_balance_str = client.getTextIn("NATIVE", "text=Make Payment", 0, "TEXT", "Up", 0, 200);
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

	public void testTouchMeNotLogin() throws InternalException {
		client.customLaunchInstrument("experitest.com.touchmenot/.LoginActivity");
		String csvUserName = null;
		String csvPassword = null;
		boolean failure = false;
		String finalMessage = null;
		if (client.isElementFound("NATIVE", "xpath=//*[@text='ALLOW']", 0))
			client.click("NATIVE", "xpath=//*[@text='ALLOW']", 0, 1);
		else if (client.isElementFound("NATIVE", "xpath=//*[@text='Allow']", 0))
			client.click("NATIVE", "xpath=//*[@text='Allow']", 0, 1);
		Scanner inputStream = null;
		try {
			inputStream = new Scanner(new File("users2.csv"));
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
			client.elementSendText("NATIVE", "hint=Username", 0, csvUserName);
			client.elementSendText("NATIVE", "hint=Re-enter Username", 0, csvPassword);
			client.click("NATIVE", "text=Login", 0, 1);
			if (client.isElementFound("NATIVE", "text=Play Game", 0)) {
				if (!csvUserName.equals(csvPassword)) {
					failure = true;
					finalMessage = "Wrong login for username-password " + csvUserName + ":" + csvPassword;
				}
				break;
			}
		}
		if (inputStream != null)
			inputStream.close();
		if (failure) {
			client.report(finalMessage, false);
			throw new InternalException(null, finalMessage, null);
		}
	}

	public void testTouchMeNotPlay() throws InternalException {
		client.customLaunchInstrument("experitest.com.touchmenot/.LoginActivity");

		if (client.isElementFound("NATIVE", "xpath=//*[@text='ALLOW']", 0))
			client.click("NATIVE", "xpath=//*[@text='ALLOW']", 0, 1);
		else if (client.isElementFound("NATIVE", "xpath=//*[@text='Allow']", 0))
			client.click("NATIVE", "xpath=//*[@text='Allow']", 0, 1);

		client.elementSendText("NATIVE", "hint=Username", 0, "Demo");
		client.elementSendText("NATIVE", "hint=Re-enter Username", 0, "Demo");
		client.click("NATIVE", "text=Login", 0, 1);
		client.click("NATIVE", "text=Play Game", 0, 1);
		client.click("default", "Dot", 0, 1);
		client.click("default", "Dot", 0, 1);
		client.click("NATIVE", "text=RePlay", 0, 1);
		client.click("default", "Dot", 0, 1);
		client.click("default", "Dot", 0, 1);
		client.click("NATIVE", "text=RePlay", 0, 1);
		client.click("default", "Dot", 0, 1);
		client.click("NATIVE", "text=Exit", 0, 1);
	}

	public void testPlayStoreInstall() throws InternalException {
		client.customSetNetworkConnection("wifi");
		client.customLaunchUnInstrument("com.android.vending/.AssetBrowserActivity");
		client.click("default", "app", 0, 1);
		client.click("NATIVE", "xpath=//*[@text='INSTALL']", 0, 1);
		client.click("NATIVE", "xpath=//*[@id='cancel_download']", 0, 1);

	}

	public void testPlayStoreTopApps() throws InternalException {
		client.customSetNetworkConnection("wifi");
		client.customLaunchUnInstrument("com.android.vending/.AssetBrowserActivity");
		client.click("NATIVE", "xpath=//*[@contentDescription='Home, Top Charts']", 0, 1);
		int i = 0, retries = 0, to_get_rank = 1;
		while (true) {
			if (client.isElementFound("NATIVE",
					"xpath=//*[@id='play_card' and @width>0 and @height>0 and ./*[@height>0]] //*[@id='li_title']",
					i)) {
				String rank = client.elementGetText("NATIVE",
						"xpath=//*[@id='play_card' and @width>0 and @height>0 and ./*[@height>0]] //*[@id='li_ranking']",
						i);
				if (rank != null) {
					int irank = Integer.parseInt(rank);
					if (to_get_rank == irank) {
						String app_name = client.elementGetText("NATIVE",
								"xpath=//*[@id='play_card' and @width>0 and @height>0 and ./*[@height>0]] //*[@id='li_title']",
								i);
						client.report("App no. "+to_get_rank+" is "+app_name, true);
						i++;
						to_get_rank++;
					} else if (to_get_rank < irank) {
						if (i == 0)
							client.swipe("UP", 400, 100);
						else
							i--;
					} else if (to_get_rank > irank) {
						i++;
					}
				} else break;
			} else {
				client.swipe("DOWN", 400, 100);
				i = 0;
				retries++;
			}
			if (to_get_rank >= 11 || retries > 10)
				break;
		}
		if(to_get_rank <= 10) {
			throw new InternalException(null, "Unable to find all top 10 apps, found until "+(to_get_rank-1), null);
		}
	}

	public void testESPNMenuText() throws InternalException {
		client.customSetNetworkConnection("wifi");
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
		client.customSetNetworkConnection("wifi");
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

	public void tearDown() {
		client.closeDevice();
		if (beReleased)
			client.releaseDevice("", false, true, true);
		client.releaseClient();
	}

	public void sendReportSummary(int run) {
		FinalReporter finalReporter = FinalReporter.getInstance();
		finalReporter.addRow(run, client.getConnDeviceName(), client.getDeviceProperty("device.sn"), testFuncMap.size(),
				failuresMap);
	}
}
