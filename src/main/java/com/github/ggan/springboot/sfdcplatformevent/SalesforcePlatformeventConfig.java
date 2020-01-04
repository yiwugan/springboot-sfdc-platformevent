package com.github.ggan.springboot.sfdcplatformevent;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.cometd.bayeux.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import com.google.gson.Gson;
import com.salesforce.emp.connector.BayeuxParameters;
import com.salesforce.emp.connector.EmpConnector;
import com.salesforce.emp.connector.TopicSubscription;
import com.salesforce.emp.connector.example.BearerTokenProvider;

@Configuration
public class SalesforcePlatformeventConfig {

	private static Logger logger = LoggerFactory.getLogger(SalesforcePlatformeventConfig.class);

	@Value("${salesforce.host}")
	private String salesforceHost;

	@Value("${salesforce.version:46.0}")
	private String salesforceVersion;

	@Value("${salesforce.consumerKey:}")
	private String salesforceConsumerKey;

	@Value("${salesforce.secret:}")
	private String salesforceSecret;

	@Value("${salesforce.username:}")
	private String salesforceUsername;

	@Value("${salesforce.password:}")
	private String salesforcePassword;

	@Autowired
	protected SalesforcePlatformeventHandler eventHandler;

	@Autowired
	SalesforceAccessTokenProvider salesforceAccessTokenProvider;

	@Autowired
	private Gson gson;

	@Bean
	public BayeuxParameters bayeuxParameters() throws Exception {
		BayeuxParameters bayeuxParams = initBayeuxParam(salesforceVersion, salesforceHost);
		Supplier<BayeuxParameters> sessionSupplier = new Supplier<BayeuxParameters>() {
			@Override
			public BayeuxParameters get() {
				return salesforceAccessTokenProvider.updateBayeuxParametersWithSalesforceAccessToken(salesforceHost,
						salesforceUsername, salesforcePassword, salesforceConsumerKey, salesforceSecret, bayeuxParams);
			}
		};
		BearerTokenProvider tokenProvider = new BearerTokenProvider(sessionSupplier);
		logger.debug("initiate BayeuxParameters for EMP Connector");
		return tokenProvider.login();
	}

	@Bean(destroyMethod = "stop")
	public EmpConnector empConnector(BayeuxParameters bayeuxParameters) {
		logger.debug("BayeuxParameters url {} version {}", bayeuxParameters.host(), bayeuxParameters.version());
		EmpConnector empConnector = new EmpConnector(bayeuxParameters);
		logger.debug("EmpConnector isConnected {} isHandshook {}", empConnector.isConnected(),
				empConnector.isHandshook());
		return empConnector;
	}

	private static BayeuxParameters initBayeuxParam(String apiVersion, String host) {
		BayeuxParameters params = new BayeuxParameters() {
			@Override
			public String version() {
				return apiVersion;
			}

			@Override
			public String bearerToken() {
				return null;
			}

			@Override
			public URL host() {
				try {
					return new URL(host);
				} catch (MalformedURLException e) {
					throw new IllegalArgumentException(String.format("Unable to create url: %s", host), e);
				}
			}
		};
		return params;
	}

	@Bean(destroyMethod = "cancel")
	@Lazy
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	public TopicSubscription startPlatformEventSubscription(EmpConnector empConnector, String salesforceEventName) {
		TopicSubscription subscription = null;
		try {
			// default replay id -1, means new message
			long replayFrom = -1;
			logger.info("Setup event consumer with replyFrom {}", replayFrom);

			Consumer<Map<String, Object>> consumer = new Consumer<Map<String, Object>>() {
				@SuppressWarnings("unchecked")
				@Override
				public void accept(Map<String, Object> event) {
					eventHandler.handleRequest(gson.toJson(event.get("payload")), salesforceEventName,
							((Map<String, Long>) event.get("event")).get("replayId"));
				}
			};
			empConnector.start().get(5, TimeUnit.SECONDS);
			logger.debug("Subscribing to event {}", salesforceEventName);
			subscription = empConnector.subscribe(salesforceEventName, replayFrom, consumer).get(5, TimeUnit.SECONDS);
		} catch (java.util.concurrent.ExecutionException sube) {
			logger.error("ExecutionException Error from platform event " + salesforceEventName, sube);
			System.exit(1);
		} catch (Exception e) {
			logger.error("Error when starting TopicSubscription " + salesforceEventName, e);
			System.exit(1);
		}
		return subscription;
	}
}