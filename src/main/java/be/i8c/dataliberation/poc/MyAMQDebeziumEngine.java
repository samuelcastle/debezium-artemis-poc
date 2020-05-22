package be.i8c.dataliberation.poc;

import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.debezium.config.Configuration;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.CloudEvents;

public class MyAMQDebeziumEngine implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(MyAMQDebeziumEngine.class);

	private final Properties config = loadPropsFile("config.properties");

	private ClientProducer artemisProducer;
	private ClientSession artemisSession;

	private MyAMQDebeziumEngine() {

	}

	private static Properties loadPropsFile(String propsFile) {
		Configuration config = Configuration.empty();
		try {

			InputStream propsInputStream = MyAMQDebeziumEngine.class.getClassLoader().getResourceAsStream(propsFile);
			config = Configuration.load(propsInputStream);

		} catch (Exception e) {
			LOG.error("Couldn't load properties file: " + propsFile, e);
			throw new RuntimeException("Couldn't load properties file: " + propsFile);
		}
		return config.asProperties();
	}

	@Override
	public void run() {

		try {
			ServerLocator locator = ActiveMQClient.createServerLocator(config.getProperty("amqartemis.url"));
			ClientSessionFactory factory = locator.createSessionFactory();

			artemisSession = factory.createSession(config.getProperty("amqartemis.username"),
					config.getProperty("amqartemis.password"), false, true, true, false, 0);
			artemisProducer = artemisSession.createProducer(config.getProperty("amqartemis.address"));

		} catch (Exception e) {
			LOG.error("Failed connecting to ActiveMQ Artemis: " + e.getMessage());
			throw new RuntimeException(e);
		}

		try (
				final DebeziumEngine<ChangeEvent<String, String>> engine = DebeziumEngine.create(CloudEvents.class) // Version	1.2.x
				// final DebeziumEngine<SourceRecord> engine = DebeziumEngine.create(Connect.class) // Version 1.1.x
				.using(config).notifying(this::sendRecord) // version 1.2.x
				// .notifying(this::sendRecord) // version 1.1.x
				.build()) {
			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.execute(engine);

			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				LOG.info("Requesting embedded engine to shut down");

			}));

			// The submitted task keeps running, only no more new ones can be added
			executor.shutdown();

			awaitTermination(executor);

			cleanUp();

		} catch (Exception e) {
			LOG.error("Unexpected exception: " + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private void sendRecord(ChangeEvent<String, String> event) {
		LOG.info("New event received: " + event.value().toString());
		try {

			// Map CDC event to external Event
			String cloudeventmessage = eventDataMapper(event.value());
			LOG.info("converted event: " + cloudeventmessage);

			// Send to ActiveMQ Artemis
			ClientMessage message = artemisSession.createMessage(true);
			message.getBodyBuffer().writeString(cloudeventmessage);
			if (artemisProducer.isClosed()) {
				artemisSession.createProducer(config.getProperty("amqartemis.address"));
			}
			artemisProducer.send(message);
			LOG.info("successfully send event");

		} catch (ActiveMQException e) {
			LOG.error("Failed sending messag to activeMQ");
			e.printStackTrace();
			// throw new RuntimeException(e);
		}

	}

	private String eventDataMapper(String cloudeventJsonMessage) {
		String transformedEvent = null;

		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(cloudeventJsonMessage);
			node.get("iodebeziumname");

			// some removals
			((ObjectNode) node).remove("id");
			((ObjectNode) node).remove("iodebeziumop");
			((ObjectNode) node).remove("iodebeziumversion");
			((ObjectNode) node).remove("iodebeziumconnector");
			((ObjectNode) node).remove("iodebeziumname");

			transformedEvent = node.toString();

		} catch (JsonProcessingException e) {
			LOG.error("Event data mapping failed: " + e.getMessage());
			e.printStackTrace();
			// throw new RuntimeException(e);
		}

		return transformedEvent;
	}

	private void cleanUp() {
		LOG.info("cleaning up ...");

		try {
			artemisProducer.close();
			artemisSession.close();
		} catch (ActiveMQException e) {
			LOG.error("Could not  properly close ActiveMQ connection: " + e.getMessage());
		}
	}

	private void awaitTermination(ExecutorService executor) {
		try {
			while (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
				LOG.info("Waiting another 10 seconds for the embedded engine to complete");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public static void main(String[] args) {
		new MyAMQDebeziumEngine().run();
	}
}
