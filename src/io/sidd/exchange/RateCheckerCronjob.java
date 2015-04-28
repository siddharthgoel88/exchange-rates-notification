package io.sidd.exchange;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.TimeZone;

import io.sidd.rateproviders.DBSRateProviderImpl;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Schedules;
import javax.ejb.Singleton;

import org.apache.commons.codec.binary.Base64;

import com.sendgrid.SendGrid;
import com.sendgrid.SendGrid.Email;
import com.sendgrid.SendGridException;

@Singleton
public class RateCheckerCronjob {
	
	private String subject;
	private String body = "This is a generated mail. Do not reply to this email.\n Read the Disclaimer below !!";
	private String mailTime;
	private double previousRate = 0.01;
	private static final double alertThreshold = 1.0;
	private static boolean mailedToday = false;
	
	@EJB
	DBSRateProviderImpl rateProvider;
	
	/*
	 * Rate checker cron job. Runs after every 15 min checks for
	 */
	@Schedules ({
		@Schedule(second="*", minute="*/15",hour="9-18", timezone="Asia/Singapore", persistent=false),
		@Schedule(dayOfWeek="1-5", persistent=false)
	})
	public void rateChecker() {
		double currentRate = rateProvider.sgd2inr();
		double changePercentage = checkChange(currentRate);
		if (!mailedToday && Math.abs(changePercentage) >= alertThreshold) {
			if (changePercentage < 0) {
				alertDropMail(currentRate, Math.abs(changePercentage));
			} else {
				alertIncreaseMail(currentRate, Math.abs(changePercentage));
			}
		}
	}
	
	private void alertIncreaseMail(double currentRate, double changePercentage) {
		subject = "Attention!!! " + changePercentage + "% increase from yesterday. 1 SGD = Rs." + 
				round(currentRate, 2) + " . Checked @ " + currentDateTime();
		sendMailToAllSubscribers();
		mailedToday = true;
	}

	private void alertDropMail(double currentRate, double changePercentage) {
		subject = "Attention!!! " + changePercentage + "% drop from yesterday. 1 SGD = Rs." + 
				round(currentRate, 2) + " . Checked @ " + currentDateTime();
		sendMailToAllSubscribers();
		mailedToday = true;
	}

	private double checkChange(double currentRate) {
		return ((currentRate - previousRate) * 100) / previousRate;
	}

	/*
	 * Daily mail trigger @ 10:00 AM
	 */
	@Schedule(dayOfWeek="1-5", hour="10", timezone="Asia/Singapore", persistent=false)
	public void dailyMail() {
		mailTime = currentDateTime();
		subject = "DBS exchange rate (1 SGD = Rs." + round(rateProvider.sgd2inr(), 2) + " )"
				+ " @ " + mailTime;
		sendMailToAllSubscribers();
	}
	
	/*
	 * Check previous day closing exchange rate
	 */
	@Schedule(hour="6", dayOfWeek="1-5", timezone="Asia/Singapore", persistent=false)
	public void updatePreviousRate() {
		previousRate = rateProvider.sgd2inr();
		mailedToday = false;
	}

	private void sendMailToAllSubscribers() {
		Scanner emailList = null;
		try {
			emailList = new Scanner(new File("/app/email-subscribers.txt"));
		} catch (FileNotFoundException e) {
			System.err.println("Oops file not found");
			return;
		}
		
		while(emailList.hasNext()) {
			String email = emailList.next();
			System.out.println(email);
			sendMail(email);
		}
		emailList.close();
	}

	private void sendMail(String to) {
		//Here key in the credential of SendGrid
		SendGrid sendgrid = new SendGrid("", "");
		Email email = new Email();
		email.addTo(to);
		email.addToName("Sidd");
		email.setFrom("dbs.daily.updates@no-reply.com");
		email.setSubject(subject);
		email.setText(body);
		try {
			System.out.println("Sending email to " + to);
			System.out.println("Message status => " + sendgrid.send(email).getMessage());
			System.out.println("Sent email to " + to);
		} catch (SendGridException e) {
			e.printStackTrace();
		}
	}
	
	private String currentDateTime() {
		DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
		Date dateobj = new Date();
		df.setTimeZone(TimeZone.getTimeZone("Asia/Singapore"));
		return df.format(dateobj);
	}
	
	private static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
}
