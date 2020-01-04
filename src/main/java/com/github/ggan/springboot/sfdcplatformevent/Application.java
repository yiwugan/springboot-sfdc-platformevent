/*
 * Copyright 2016 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */
package com.github.ggan.springboot.sfdcplatformevent;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import com.salesforce.emp.connector.EmpConnector;
import com.salesforce.emp.connector.TopicSubscription;

@SpringBootApplication
public class Application {
	@Value("${salesforce.sourceEvent.list}")
	private String[] salesforceEventList;

	@Autowired
	private BeanFactory beanFactory;

	@Autowired
	private ApplicationContext context;

	@Autowired
	BuildProperties buildProperties;

	private static Logger logger = LoggerFactory.getLogger(Application.class);


	/**
	 * A main method to start this application.
	 */
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@PostConstruct
	public void startPlatformEventSubsciptions() {

		EmpConnector emp = context.getBean(EmpConnector.class);

		// start platform event subscription
		logger.info("starting salesforce platform event subscription");
		for (int i = 0; i < salesforceEventList.length; i++) {
			String event = salesforceEventList[i];
			beanFactory.getBean(TopicSubscription.class, emp, event);
			logger.info("created new salesforce platform event subscription " + event);
		}
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

}
