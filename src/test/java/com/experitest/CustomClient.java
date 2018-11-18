package com.experitest;

import com.experitest.client.Client;
import com.experitest.client.InternalException;

public class CustomClient extends Client {
	String connDeviceName;

	public String getConnDeviceName() {
		return connDeviceName;
	}

	public void setConnDeviceName(String connDeviceName) {
		this.connDeviceName = connDeviceName;
	}

	public CustomClient(String host, int port) {
		super(host, port);
	}

	public CustomClient(String host, int port, boolean useSessionID) {
		super(host, port, useSessionID);
	}
	
	public void customClick(String zone, String element, int index, int clickCount, String zipDestination, String applicationPath) {
		try {
			super.click(zone, element, index, clickCount);
		} catch (InternalException e) {
			super.collectSupportData(zipDestination, applicationPath, connDeviceName, "click error", "click to work", e.getMessage());
		}
	}
	
	public void customElementSendText(String zone, String element, int index, String text, String zipDestination, String applicationPath ) {
		try {
			super.elementSendText(zone, element, index, text);
		} catch (InternalException e) {
			super.collectSupportData(zipDestination, applicationPath, connDeviceName, "send text error", "send text experted to work", e.getMessage());
		}
	}
	
	public void customInstallInstrumented(String app) {
		try {
			super.install("com.experitest.ExperiBank/.LoginActivity", true, true);
		} catch (InternalException e) {
			super.install("com.experitest.ExperiBank/.LoginActivity", true, false);
		}
	} 
	
	public void customInstallUninstrumented(String app) {
		try {
			super.install("com.experitest.ExperiBank/.LoginActivity", true, true);
		} catch (InternalException e) {
			super.install("com.experitest.ExperiBank/.LoginActivity", true, false);
		}
	}
	
	public boolean customWaitForElement(String zone, String element, int index, int timeout, String zipDestination, String applicationPath ) {
		try {
			return super.waitForElement(zone, element, index, timeout);
		} catch (InternalException e) {
			super.collectSupportData(zipDestination, applicationPath, connDeviceName, "waitForElement error", "element expected to appear", e.getMessage());
			return false;
		}
	}
	
	public void customSetNetworkConnection(String connection) {
		try {
			if (!super.getNetworkConnection(connection))
				try {
					super.setNetworkConnection(connection, true);
				} catch (InternalException e) {
					super.report("Wifi not set, cannot change Wifi state", false);
				}
		} catch (InternalException e) {
			super.report("Cannot check Wifi state", false);
		}
	}
	
	
}
