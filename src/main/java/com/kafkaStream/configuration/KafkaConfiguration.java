package com.kafkaStream.configuration;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;


public class KafkaConfiguration {

	public static void main(String[] args) {

		Properties props = new Properties();
		props.put(StreamsConfig.APPLICATION_ID_CONFIG, "testappid");
		props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
		props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
		props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class);

		StreamsBuilder builder = new StreamsBuilder();
		Collection<String> col = Arrays.asList("TestTopic", "NewTopic");
		KStream<String, String> source = builder.stream(col);
//		source.foreach((K, V)-> System.out.println("Key is : " + K + ", Value is : " + V));
		
		source.flatMapValues(value -> Arrays.asList(value.split("\\W+")))
			.groupBy((key, value) -> value)
			.count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("count-store"))
			.toStream()
		    .to("NewTopicPartion2", Produced.with(Serdes.String(), Serdes.Long()));
		
		
		final Topology topology = builder.build();
		
		System.out.println(topology.describe());
		
		
		final KafkaStreams streams = new KafkaStreams(topology, props);
		final CountDownLatch latch = new CountDownLatch(1);
		
		Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });
 
        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }

}
