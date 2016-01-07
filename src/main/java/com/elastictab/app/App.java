package com.elastictab.app;

import java.io.File;

import com.elastictab.report.ESReport;
import com.elastictab.util.QuartzUtil;

public class App {
	public static void main(String[] args) {
		String jarPath = System.getProperties().getProperty("user.dir");
		System.out.println("Property files loaded from the path " + jarPath);

		JettyServerEmbedded jettyServerEmbedded = new JettyServerEmbedded(jarPath + File.separatorChar + "properties" + File.separatorChar + "jetty.properties");
		jettyServerEmbedded.start();

		H2DatabaseEmbedded h2DatabaseEmbedded = new H2DatabaseEmbedded(jarPath + File.separatorChar + "properties" + File.separatorChar + "h2.properties");

		QuartzServerEmbedded quartzServerEmbedded = new QuartzServerEmbedded(jarPath + File.separatorChar + "properties" + File.separatorChar + "quartzServer.properties");
		quartzServerEmbedded.start();
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		QuartzUtil.initializeSchedulerClient();
		ESReport.initializeESClient();
		System.out.println("**************************ElasticTab Started**************************");
	}
}
