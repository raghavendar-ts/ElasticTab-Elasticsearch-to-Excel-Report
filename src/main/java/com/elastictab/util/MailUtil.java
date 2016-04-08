package com.elastictab.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.poi.ss.usermodel.Workbook;
import org.json.JSONArray;

public class MailUtil {
	static Properties mailProperties = new Properties();
	static Session session;
	MimeMessage message;
	Multipart multipart = new MimeMultipart();
	BodyPart messageBodyPart = null;

	public MailUtil() {
		try {
			String jarPath = System.getProperties().getProperty("user.dir");
			mailProperties.load(new FileInputStream(jarPath + File.separatorChar + "properties" + File.separatorChar + "mail.properties"));
			session = Session.getInstance(mailProperties, null);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public void initializeMessage() {
		message = new MimeMessage(session);		
	}

	public void setFrom(String fromLocal) {
		InternetAddress from;
		try {
			from = new InternetAddress(fromLocal);
			message.setFrom(from);
		} catch (AddressException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}

	public void setSubject(String subject) throws MessagingException {
		message.setSubject(subject);
	}

	public void addRecipients(List<String> eMailList) throws AddressException, MessagingException {
		for (int i = 0; i < eMailList.size(); i++) {
			message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(eMailList.get(i)));
		}
	}

	public void addRecipients(JSONArray eMailList) {
		for (int i = 0; i < eMailList.length(); i++) {
			try {
				message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(eMailList.getString(i)));
			} catch (AddressException e) {
				e.printStackTrace();
			} catch (MessagingException e) {
				e.printStackTrace();
			}
		}
	}

	public void setText(String mailContent) throws MessagingException {
		messageBodyPart = new MimeBodyPart();
		messageBodyPart.setText(mailContent);
		multipart.addBodyPart(messageBodyPart);
	}

	public void attachWB(Workbook wb, String fileName) throws IOException, MessagingException {
		messageBodyPart = new MimeBodyPart();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataSource ds = null;

		wb.write(baos);
		byte[] bytes = baos.toByteArray();
		ds = new ByteArrayDataSource(bytes, "application/excel");
		DataHandler dh = new DataHandler(ds);
		messageBodyPart.setDataHandler(dh);
		messageBodyPart.setFileName(fileName + ".xls");
		multipart.addBodyPart(messageBodyPart);
	}

	public void send() throws MessagingException {
		Transport transport;
		message.setContent(multipart);
		transport = session.getTransport("smtp");
		transport.connect(mailProperties.getProperty("username"), mailProperties.getProperty("password"));
		transport.sendMessage(message, message.getAllRecipients());
	}


}
