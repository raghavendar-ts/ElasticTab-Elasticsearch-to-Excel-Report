package com.elastictab.app;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import com.elastictab.util.Constants;

public class JettyServerEmbedded {
	Properties jettyServerProperties = new Properties();

	JettyServerEmbedded(String configPath) {
		try {
			InputStream input = new FileInputStream(configPath);
			jettyServerProperties.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void start() {
		Thread t = new Thread() {
			public void run() {

				Server server = new Server(Integer.valueOf(jettyServerProperties.getProperty(Constants.PORT)));
				WebAppContext context = new WebAppContext();
				context.setDescriptor("src/main/webapp/WEB-INF/web.xml");
				context.setResourceBase("src/main/webapp/WEB-INF/views");
				context.setExtractWAR(true);
				context.setCopyWebInf(true);
				context.setCopyWebDir(true);
								
				context.setContextPath("/");
				context.setParentLoaderPriority(true);
				server.setHandler(context);
				
				try {
					server.start();
					server.join();

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		t.start();
	}
}
