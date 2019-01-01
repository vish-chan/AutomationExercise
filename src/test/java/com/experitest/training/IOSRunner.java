package com.experitest.training;

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
		String device = BaseTest.getiOSDeviceName();
		int isGrid = BaseTest.getGrid();
		long duration = BaseTest.getTestDuration();
		if (duration < 0)
			duration = 30;
		else
			duration = duration + 10;
		ExecutorService es = Executors.newCachedThreadPool();
		if (isGrid == 0) {
			if (device.equals("cloud")) {
				for (int i = 0; i < BaseTest.getCloudiOS(); i++) {
					System.out.println("Starting test for device " + device);
					IOSSuite ios = new IOSSuite(device, BaseTest.getHost(), BaseTest.getPort(),
							BaseTest.getBaseDirectory(), BaseTest.getReportsBaseDirectory());
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
		} else {
			for (int i = 0; i < BaseTest.getCloudiOS(); i++) {
				System.out.println("Starting test on grid for ios device");
				IOSSuite as = new IOSSuite(BaseTest.getUsername(), BaseTest.getPassword(), "Default", BaseTest.getURL(),
						BaseTest.getBaseDirectory(), BaseTest.getReportsBaseDirectory(), (int) duration);
				es.execute(as);
			}
		}
		try {
			es.shutdown();
			es.awaitTermination(duration, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			fail("Some threads couldn't complete execution");
			e.printStackTrace();
		}
	}

}
