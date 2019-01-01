package com.experitest.training;

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
		String device = BaseTest.getAndroidDeviceName();
		ExecutorService es = Executors.newCachedThreadPool();
		int isGrid = BaseTest.getGrid();
		long duration = BaseTest.getTestDuration();
		if (duration < 0)
			duration = 30;
		else
			duration = duration + 10;
		if(isGrid==0) {
			if (device.equals("cloud")) {
				for (int i = 0; i < BaseTest.getCloudAndroid(); i++) {
					System.out.println("Starting test for device " + device);
					AndroidSuite as = new AndroidSuite(device, BaseTest.getHost(), BaseTest.getPort(),
							BaseTest.getBaseDirectory(), BaseTest.getReportsBaseDirectory());
					es.execute(as);
				}
			} else {
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
					if (device.equals("all") || device.equals(android_device)) {
						System.out.println("Starting test for device " + device);
						AndroidSuite as = new AndroidSuite(android_device, BaseTest.getHost(), BaseTest.getPort(),
								BaseTest.getBaseDirectory(), BaseTest.getReportsBaseDirectory());
						es.execute(as);
					}
				}
			}
		} else {
			for (int i = 0; i < BaseTest.getCloudAndroid(); i++) {
				System.out.println("Starting test on grid for Android device");
				AndroidSuite as = new AndroidSuite(BaseTest.getUsername(), BaseTest.getPassword(),"Default", BaseTest.getURL(),
						BaseTest.getBaseDirectory(), BaseTest.getReportsBaseDirectory(), (int)duration);
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
