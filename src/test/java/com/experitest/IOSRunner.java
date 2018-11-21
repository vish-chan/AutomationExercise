package com.experitest;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.experitest.client.Client;

public class IOSRunner extends BaseTest {

	Client client = null;

	@Before
	public void setUp() {
		System.out.println("iOS setUp");
		client = new Client(BaseTest.getHost(), BaseTest.getPort(), true);
	}

	@Test
	public void test() {
		String device = BaseTest.getDeviceName();
		ExecutorService es = Executors.newCachedThreadPool();
		if (device.equals("cloud")) {
			for (int i = 0; i < BaseTest.getCloudiOS(); i++) {
				System.out.println("Starting test for device " + device);
				IOSSuite ios = new IOSSuite(device, BaseTest.getHost(), BaseTest.getPort(), BaseTest.getBaseDirectory(),
						BaseTest.getReportsBaseDirectory());
				es.execute(ios);
			}
		} else {
			String conn_dev = client.getConnectedDevices();
			String[] devices = conn_dev.split("\n");
			List<String> ios_devices = new ArrayList<>();
			for (int i = 0; i < devices.length; i++) {
				if (devices[i].contains("ios_app:")) {
					ios_devices.add(devices[i]);
				}
			}
			System.out.println("Total IOS devices found: " + ios_devices.size());
			for (String ios_device : ios_devices) {
				if (device.equals("all") || device.equals(ios_device)) {
					System.out.println("Starting test for device " + device);
					IOSSuite ios = new IOSSuite(ios_device, BaseTest.getHost(), BaseTest.getPort(),
							BaseTest.getBaseDirectory(), BaseTest.getReportsBaseDirectory());
					es.execute(ios);
				}

			}

		}
		try {
			es.shutdown();
			long duration = BaseTest.getTestDuration();
			if (duration < 0)
				duration = 30;
			else
				duration = duration + 10;
			es.awaitTermination(duration, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			fail("Some threads couldn't complete execution");
			e.printStackTrace();
		}
	}

}
