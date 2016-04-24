package com.nfd.common.spring.example;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.nfd.common.spring.ReloadConfiguration;

/**
 */
public class Main {
	public static void main(String[] args) throws IOException {
		final ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("dynamic.xml");
		Timer t = new Timer();
		t.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				ReloadConfiguration con = (ReloadConfiguration)applicationContext.getBean("reloadConfiguration");
				con.run();
			}
		}, 0, 1000);
		t.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				MyBean b = (MyBean) applicationContext.getBean("mybean");
				System.out.println(b.getCachesize());
				MyBean2 b2 = (MyBean2) applicationContext.getBean("mybean2");
				System.out.println(b2.getCachesize());
			}
		}, 0, 3000);
		System.out.println(
				"change the file src/main/resources/net/wuenschenswert/spring/example/config.properties to trigger a reload... press return when satisfied.");
		System.in.read();
		applicationContext.close();

	}
}
