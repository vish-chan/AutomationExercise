package com.experitest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FinalReporter {
	
	private static FinalReporter finalReporter = null;
	final String newline = System.getProperty("line.separator");
	List<ReportRow> tableRows;
	final static Object LOCK = new Object();
	
	private FinalReporter() {
		tableRows = new ArrayList<>();
	}
	
	public static FinalReporter getInstance() {
		synchronized (LOCK) {
			if(finalReporter==null) {
				finalReporter = new FinalReporter();
			} 
		}
		return finalReporter;
	}
	
	class ReportRow implements Comparable<ReportRow>{
		int run;
		String deviceName;
		String deviceSn;
		int numTests;
		int numSuccess;
		int numFailures;
		Map<String, List<String>> failedTests;
		
		public ReportRow(int run, String deviceName, String deviceSn,int numTests, Map<String, List<String>> failedTests) {
			this.run = run;
			this.deviceName = deviceName;
			this.deviceSn = deviceSn;
			this.numTests = numTests;
			this.numFailures = failedTests.size();
			this.numSuccess = numTests - numFailures;
			this.failedTests = failedTests;
		}
		
		public String printRow() {
			StringBuilder row = new StringBuilder();
			row.append(this.run);
			row.append(",");
			row.append(this.deviceName);
			row.append(",");
			row.append(this.deviceSn);
			row.append(",");
			row.append(this.numTests);
			row.append(",");
			row.append(this.numSuccess);
			row.append(",");
			row.append(this.numFailures);
			row.append(newline);
			if(numFailures>0) {
				for(String key:failedTests.keySet()) {
					row.append(",,,,,,");
					row.append(key);
					row.append(",");
					row.append(failedTests.get(key));
					row.append(newline);
				}
			}
			return row.toString();
		}

		@Override
		public int compareTo(ReportRow o) {
			return this.deviceName.compareTo(o.deviceName);
		}
	}

	public void addRow(int run, String deviceName, String deviceSn,int numTests, Map<String, List<String>> failedTests) {
		synchronized (LOCK) {
			tableRows.add(new ReportRow(run, deviceName, deviceSn ,numTests, failedTests));
		}
	}
	
	public void printFinalTable() {
		File f = new File(BaseTest.getBaseDirectory()+"\\"+BaseTest.getReportsBaseDirectory()+"\\"+"final_report.csv");
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			fw = new FileWriter(f);
			bw = new BufferedWriter(fw);
			bw.write("Run,Device Name,Device Sn.,Tests,Success,Failures,Failed Test"+newline);
			tableRows.sort(null);
			for(ReportRow row:tableRows) {
				bw.write(row.printRow());
			}
		} catch (IOException e) {
			System.out.println("Unable to write in file!");
			e.printStackTrace();
		} finally {
			if(bw!=null)
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if(fw!=null)
				try {
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
}
