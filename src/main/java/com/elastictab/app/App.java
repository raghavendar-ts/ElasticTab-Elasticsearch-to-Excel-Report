package com.elastictab.app;

public class App {
	public static void main(String[] args) {
		String jarPath = System.getProperties().getProperty("user.dir");
		System.out.println("Property files loaded from the path " + jarPath);
		
		JettyServerEmbedded jettyServerEmbedded = new JettyServerEmbedded(jarPath + "\\properties\\jetty.properties");
		jettyServerEmbedded.start();

		H2DatabaseEmbedded h2DatabaseEmbedded = new H2DatabaseEmbedded(jarPath + "\\properties\\h2.properties");

		QuartzServerEmbedded quartzServerEmbedded = new QuartzServerEmbedded(jarPath + "\\properties\\quartzServer.properties");
		quartzServerEmbedded.start();
	}
}
