package com.github.ggan.springboot.sfdcplatformevent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SalesforcePlatformeventHandler {
	private static Logger logger = LoggerFactory.getLogger(SalesforcePlatformeventHandler.class);

	public synchronized void handleRequest(String payload, String salesforceEventName, Long replayId) {
		logger.info("Received payload from platform event " + salesforceEventName + " with replayId: " + replayId);
		logger.info(payload);
	}

}
