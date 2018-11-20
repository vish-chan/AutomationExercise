package com.experitest;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.experitest.client.Client;

public class AndroidRunner extends BaseTest {

	Client client = null;

	@Before
	public void setUp() {
		System.out.println("AndroidTest setUp");
		client = new Client(BaseTest.getHost(), BaseTest.getPort(), true);
	}

	@Test
	public void test() {
		String device = BaseTest.getDeviceName();
		ExecutorService es = Executors.newCachedThreadPool();
		if (device.equals("all")) {
			String conn_dev = client.getConnectedDevices();
			String[] devices = conn_dev.split("\n");
			List<String> android_devices = new ArrayList<>();
			for (int i = 0; i < devices.length; i++) {
				if (devices[i].contains("adb:")) {
					android_devices.add(devices[i]);
				}
			}
			System.out.println("Total android devices found: " + android_devices.size());
			for (String android_device : android_devices) {
				System.out.println("Starting test for device " + device);
				AndroidSuite as = new AndroidSuite(android_device, BaseTest.getHost(), BaseTest.getPort(),
						BaseTest.getBaseDirectory(), BaseTest.getReportsBaseDirectory());
				es.execute(as);
			}
		} else {
			System.out.println("Starting test for device " + device);
			AndroidSuite as = new AndroidSuite(device, BaseTest.getHost(), BaseTest.getPort(),
					BaseTest.getBaseDirectory(), BaseTest.getReportsBaseDirectory());
			es.execute(as);
		}
		try {
			es.shutdown();
			long duration = BaseTest.getTestDuration();
			if(duration<0)
				duration = 30;
			else
				duration = duration+10;
			es.awaitTermination(duration, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			fail("Some threads couldn't complete execution");
			e.printStackTrace();
		}
	}
}
