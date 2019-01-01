package com.experitest.training;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import com.experitest.client.Client;

public class TestScripts {

	public TestScripts() {
		// TODO Auto-generated constructor stub
	}

	@Test
	public void test() {
		Client client = new Client("localhost", 8889, true);
		//System.out.println(getPaymentFromString("the payment is: -90o.1o$"));
		client.setDevice("adb:xiaomi-mi_a2-13edf0e");
		client.getDeviceProperty("device.name");
	}

	public double getPaymentFromString(String s) {
		String regex = "[\\s]*[-]?[0-9oO]+\\.[0-9oO]+\\$";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(s);
		double d = 0d;
		if (m.find()) {
			System.out.println(m.group());
			if (m.group() != null && !(m.group()).equals("")) {
				String match = m.group();
				match = match.trim();
				match = match.replaceAll("[oO]", "0");
				match = match.replaceAll("\\$", "");
				try {
					d = Double.parseDouble(match);
				} catch (NumberFormatException e) {
					System.out.println("Couldn't parse payment.");
				}
			}
		}
		return d;
	}

}
