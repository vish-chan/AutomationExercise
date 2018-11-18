package com.experitest;


import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.experimental.ParallelComputer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class JUnitRunner {

	public static void main(String[] args) {
		Class<?> android = AndroidRunner.class;
		Class<?> ios = IOSRunner.class;
		BaseTest.loadProperties();
		String os = BaseTest.getOS();
		List<Class<?>> class_list = new ArrayList<>();
		if(os.equals("android"))
			class_list.add(android);
		else if(os.equals("ios"))
			class_list.add(ios);
		else if(os.equals("all")) {
			class_list.add(android);
			class_list.add(ios);
		} else 
			Assert.fail("Incorrect OS property in test.properties.");
		
		Class<?>[] classes = new Class<?>[class_list.size()];
		for(int i=0;i<classes.length;i++)
			classes[i] = class_list.get(i);
		JUnitCore jc = new JUnitCore();
		Result result = jc.run(new ParallelComputer(true, false), classes);
		for(Failure f:result.getFailures()) {
			System.out.println(f.getMessage());
		}
		
		if(result.wasSuccessful())
			System.out.println("All tests successfuly executed.");
		else {
			System.out.println("Some tests failed to execute!");
		}
	}
}
