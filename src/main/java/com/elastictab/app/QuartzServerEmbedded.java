package com.elastictab.app;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;

public class QuartzServerEmbedded {
	Properties quartzServerProperties = new Properties();

	QuartzServerEmbedded(String configPath) {
		try {
			InputStream input = new FileInputStream(configPath);
			quartzServerProperties.load(input);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public void start() {
		Thread t = new Thread() {
			public void run() {
				try {
					SchedulerFactory stdSchedulerFactory = new StdSchedulerFactory(quartzServerProperties);
					Scheduler scheduler = stdSchedulerFactory.getScheduler();
					scheduler.start();
				} catch (SchedulerException e) {
					e.printStackTrace();
				}
			}
		};
		t.start();
	}
}
