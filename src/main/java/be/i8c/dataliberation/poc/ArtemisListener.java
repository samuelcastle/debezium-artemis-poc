package be.i8c.dataliberation.poc;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArtemisListener {
	private static final Logger LOG = LoggerFactory.getLogger(ArtemisListener.class);
	
	public static void main(String[] args) throws IOException {
		LOG.info("Starting application");
		new ArtemisListener();
	}
	
	public ArtemisListener ( ) {
		try {
			
			ServerLocator locator = ActiveMQClient.createServerLocator("tcp://localhost:61616");
			ClientSessionFactory factory =  locator.createSessionFactory();
			ClientSession session = factory.createSession("artemis", "simetraehcapa", false, true, true, false, 0);
		

			ClientConsumer consumer = session.createConsumer("myQueue");

			
			
			session.start();

			LOG.info("Start waiting for new messages ...");
			
			while (true) {
				ClientMessage msgReceived = consumer.receive();
				LOG.info(msgReceived.getBodyBuffer().readString());
			}
			
			
			//session.close();
			
			//LOG.info("tot hier dus");
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
