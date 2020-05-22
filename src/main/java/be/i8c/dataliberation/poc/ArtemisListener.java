package be.i8c.dataliberation.poc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.SimpleString;
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

	public ArtemisListener() {
		try {

			// Read properties file
			LOG.info("Reading properties file ...");
			InputStream propsInputStream = ArtemisListener.class.getClassLoader()
					.getResourceAsStream("config.properties");
			Properties props = new Properties();
			props.load(propsInputStream);

			// Connect to ActiveMQ
			LOG.info("Connectin to ActiveMQ Artemis ...");
			ServerLocator locator = ActiveMQClient.createServerLocator(props.getProperty("amqartemis.url"));
			ClientSessionFactory factory = locator.createSessionFactory();
			ClientSession session = factory.createSession(props.getProperty("amqartemis.username"), props.getProperty("amqartemis.password"), false, true, true, false, 0);
			session.createTemporaryQueue(props.getProperty("amqartemis.address"), "myQueue");

			ClientConsumer consumer = session.createConsumer("myQueue");

			LOG.info("Start waiting for new messages ...");
			session.start();
			while (!Thread.interrupted()) {
				ClientMessage msgReceived = consumer.receive();
				LOG.info(msgReceived.getBodyBuffer().readString());
			}
			session.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
