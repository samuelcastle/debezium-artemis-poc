package be.i8c.dataliberation.poc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;

import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.config.Configuration;
import io.debezium.embedded.Connect;
//import io.debezium.engine.ChangeEvent; // from version 1.2.x
import io.debezium.engine.DebeziumEngine;
// import io.debezium.engine.format.CloudEvents; // from version 1.2.x

public class MyDebeziumEngine {
	
	private static final Logger LOG = LoggerFactory.getLogger(MyDebeziumEngine.class);
	
	private ClientProducer artemisProducer;
	private ClientSession artemisSession;
	
	public MyDebeziumEngine() throws IOException , ActiveMQException  {
		LOG.info("Constructor called");
		
		try {
			LOG.info("Loading configuration ... ");
			Properties config = loadPropsFile("config.properties");

			
			LOG.info("Connecting to ActiveMQ Artemis ... ");
			ServerLocator locator = ActiveMQClient.createServerLocator(config.getProperty("amqartemis.url"));
			ClientSessionFactory factory =  locator.createSessionFactory();
			
			artemisSession = factory.createSession(config.getProperty("amqartemis.username"), config.getProperty("amqartemis.password"), false, true, true, false, 0);
			artemisProducer = artemisSession.createProducer(config.getProperty("amqartemis.address"));
			
			LOG.info("Initiate debezium engine ... ");
			try (
				//DebeziumEngine<ChangeEvent<String, String>> engine = DebeziumEngine.create(CloudEvents.class) // Version 1.2.x
				DebeziumEngine<SourceRecord> engine = DebeziumEngine.create(Connect.class)	// Version 1.1.x
				.using(config)
		        //.notifying(this::sendRecord)		// version 1.2.x
		        .notifying(this::sendRecordOld) 	// version 1.1.x
				.build()
			){
				Executors.newSingleThreadExecutor().execute(engine);
			    ExecutorService executor = Executors.newSingleThreadExecutor();
			    executor.execute(engine);
			}
			// Engine is stopped when the main code is finished
		
		} catch (Exception e) {
			LOG.error("Unexpected exception:" + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(e);
			
		} finally {
			
			artemisProducer.close();
			artemisSession.close();
			
		}
	}
	
	public static void main(String[] args) throws IOException , ActiveMQException {
		LOG.info("Starting application");
		new MyDebeziumEngine();
	}

	private void sendRecordOld(SourceRecord record) {
		LOG.info("Record received");
		LOG.info(record.value().toString());
		
	}
	
	/*
	private void sendRecord(ChangeEvent<String, String> r) {
		LOG.info("Record received to be send ... ");
		
		LOG.info("ChangeEvent Key" + r.key());
		LOG.info("Event received" + r.value());
		
		String cloudeventmessage = r.value();
		
		
		ClientMessage message = artemisSession.createMessage(false);
		message.getBodyBuffer().writeString(cloudeventmessage);
		
		try {
			artemisProducer.send(message);
			
		} catch (ActiveMQException e) {
			LOG.error("");
			// activemq artemis connection exception handling
			e.printStackTrace();
			throw new RuntimeException("s");
		}
	}*/
	
	
	private Properties loadPropsFile(String propsFile) {
		Configuration config = Configuration.empty();
		try {
			
			InputStream propsInputStream = MyDebeziumEngine.class.getClassLoader().getResourceAsStream(propsFile);
			config = Configuration.load(propsInputStream);
			
		} catch (Exception e) {
			LOG.error("Couldn't load properties file: " + propsFile, e);
			throw new RuntimeException("Couldn't load properties file: " + propsFile);
		}
		return config.asProperties();
	}

}
