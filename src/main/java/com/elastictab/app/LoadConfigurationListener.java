package com.elastictab.app;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import com.elastictab.util.Constants;

public class LoadConfigurationListener implements ServletContextListener {
	Properties properties = new Properties();

	public void contextInitialized(ServletContextEvent sce) {
		ServletContext context = sce.getServletContext();
		String hostname = "localhost";
		String jarPath = System.getProperties().getProperty("user.dir");

		try {
			InputStream input = new FileInputStream(jarPath + "\\properties\\elasticsearch.properties");
			properties.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (!properties.get("hostname").equals(null) && !properties.get("hostname").equals("")) {
			hostname = (String) properties.get("hostname");
		}

		Builder builder = ImmutableSettings.settingsBuilder();
		builder.put("client.transport.sniff", true);
		if (!properties.get("clustername").equals(null) && !properties.get("clustername").equals("")) {
			builder.put("cluster.name", (String) properties.get("clustername"));
		}

		Settings settings = builder.build();
		Client esClient = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(hostname, 9300));
		System.out.println("Elasticsearch Transport Client Initialized:" + esClient);
		context.setAttribute(Constants.ES_CLIENT, esClient);
	}

	public void contextDestroyed(ServletContextEvent sce) {
		ServletContext context = sce.getServletContext();
		Client esClient = (Client) context.getAttribute(Constants.ES_CLIENT);
		esClient.close();
		context.removeAttribute(Constants.ES_CLIENT);
	}

}
