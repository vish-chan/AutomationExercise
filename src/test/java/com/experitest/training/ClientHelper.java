package com.experitest.training;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.experitest.client.Client;
import com.experitest.client.InternalException;

public class ClientHelper {
	
	Client client;
	String connDeviceName = null;

	public ClientHelper(Client c) {
		client = c;
	}
	public String getConnDeviceName() {
		return connDeviceName;
	}

	public void setConnDeviceName(String connDeviceName) {
		this.connDeviceName = connDeviceName;
	}

	public void customClick(String zone, String element, int index, int clickCount, String zipDestination,
			String applicationPath) {
		try {
			client.click(zone, element, index, clickCount);
		} catch (InternalException e) {
			client.collectSupportData(zipDestination, applicationPath, connDeviceName, "click error", "click to work",
					e.getMessage());
		}
	}

	public void customElementSendText(String zone, String element, int index, String text, String zipDestination,
			String applicationPath) {
		try {
			client.elementSendText(zone, element, index, text);
		} catch (InternalException e) {
			client.collectSupportData(zipDestination, applicationPath, connDeviceName, "send text error",
					"send text experted to work", e.getMessage());
		}
	}

	public void customInstallInstrumented(String app) {
		try {
			client.install(app, true, true);
		} catch (InternalException e) {
			try {
				client.install(app, true, false);
			} catch (InternalException e2) {
				System.err.println("Unable to install app instrumented");
			}
		}
	}

	public void customInstallUninstrumented(String app) {
		try {
			client.install(app, false, true);
		} catch (InternalException e) {
			try {
				client.install(app, false, false);
			} catch (InternalException e2) {
				System.err.println("Unable to install app uninstrumented");
			}
		}
	}

	public void customLaunchInstrument(String app) {
		try {
			client.launch(app, true, true);
		} catch (InternalException e) {
			System.err.println("Unable to launch app instrumented, reinstalling and trying again.");
			customInstallInstrumented(app);
			client.launch(app, true, true);
		}
	}
	
	public void customLaunchUnInstrument(String app) {
		try {
			client.launch(app, false, true);
		} catch (InternalException e) {
			System.err.println("Unable to launch app Uninstrumented, reinstalling and trying again.");
			customInstallUninstrumented(app);
			client.launch(app, true, true);
		}
	}

	public boolean customWaitForElement(String zone, String element, int index, int timeout, String zipDestination,
			String applicationPath) {
		try {
			return client.waitForElement(zone, element, index, timeout);
		} catch (InternalException e) {
			client.collectSupportData(zipDestination, applicationPath, connDeviceName, "waitForElement error",
					"element expected to appear", e.getMessage());
			return false;
		}
	}

	public void customSetNetworkConnection(String connection) {
		try {
			if (!client.getNetworkConnection(connection))
				try {
					client.setNetworkConnection(connection, true);
				} catch (InternalException e) {
					client.report("Wifi not set, cannot change Wifi state", false);
				}
		} catch (InternalException e) {
			client.report("Cannot check Wifi state", true);
		}
	}

	public double getPaymentFromString(String s) {
		String regex = "[\\s]*[-]?[0-9oO]+\\.[0-9oO]+\\$";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(s);
		double d = 0d;
		boolean fail = false;
		if (m.find()) {
			if (m.group() != null && !(m.group()).equals("")) {
				String match = m.group();
				match = match.trim();
				match = match.replaceAll("[oO]", "0");
				match = match.replaceAll("\\$", "");
				try {
					d = Double.parseDouble(match);
				} catch (NumberFormatException e) {
					fail = true;
				}
			}
		} else {
			fail = true;
		}
		if (fail)
			throw new InternalException(null, "Unable to parse double from string "+s, null);
		return d;
	}

}
