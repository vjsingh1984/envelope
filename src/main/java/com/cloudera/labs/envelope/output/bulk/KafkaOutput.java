package com.cloudera.labs.envelope.output.bulk;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;

import com.cloudera.labs.envelope.plan.MutationType;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.typesafe.config.Config;

import scala.Tuple2;

public class KafkaOutput extends BulkOutput {
    
    public static final String BROKERS_CONFIG_NAME = "brokers";
    public static final String TOPIC_CONFIG_NAME = "topic";
    public static final String FIELD_DELIMITER_CONFIG_NAME = "field.delimiter";
    
    public KafkaOutput(Config config) {
        super(config);
    }
    
    @SuppressWarnings("serial")
    @Override
    public void applyMutations(List<Tuple2<MutationType, DataFrame>> planned) {
        for (Tuple2<MutationType, DataFrame> mutation : planned) {
            MutationType mutationType = mutation._1();
            DataFrame mutationDF = mutation._2();
            
            if (mutationType.equals(MutationType.INSERT)) {
                mutationDF.javaRDD().foreach(new VoidFunction<Row>() {
                    private KafkaProducer<String, String> producer;
                    private String topic;
                    private Joiner joiner;
                    
                    @Override
                    public void call(Row mutation) throws Exception {
                        if (producer == null) {
                            initialize();
                        }
                        
                        List<Object> values = Lists.newArrayList();
                        
                        for (int fieldIndex = 0; fieldIndex < mutation.size(); fieldIndex++) {
                            values.add(mutation.get(fieldIndex));
                        }
                        
                        String message = joiner.join(values);
                        
                        producer.send(new ProducerRecord<String, String>(topic, message));
                    }
                    
                    private void initialize() {
                        String brokers = config.getString(BROKERS_CONFIG_NAME);
                        
                        Properties producerProps = new Properties();
                        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
                        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
                        producerProps.put("bootstrap.servers", brokers);
                        
                        Serializer<String> serializer = new StringSerializer();
                        producer = new KafkaProducer<String, String>(producerProps, serializer, serializer);
                        
                        topic = config.getString(TOPIC_CONFIG_NAME);
                        String delimiter = getDelimiter();
                        joiner = Joiner.on(delimiter);
                    }
                });
            }
        }
    }
    
    @Override
    public Set<MutationType> getSupportedMutationTypes() {
        return Sets.newHashSet(MutationType.INSERT);
    }
    
    private String getDelimiter() {
        if (!config.hasPath(FIELD_DELIMITER_CONFIG_NAME)) return ",";
        
        String delimiter = config.getString(FIELD_DELIMITER_CONFIG_NAME);
        
        if (delimiter.startsWith("chars:")) {
            String[] codePoints = delimiter.substring("chars:".length()).split(",");
            
            StringBuilder delimiterBuilder = new StringBuilder();
            for (String codePoint : codePoints) {
                delimiterBuilder.append(Character.toChars(Integer.parseInt(codePoint)));
            }
            
            return delimiterBuilder.toString();
        }
        else {
            return delimiter;
        }
    }
    
}