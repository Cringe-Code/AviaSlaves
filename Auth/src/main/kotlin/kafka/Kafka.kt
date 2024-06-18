package app.aviaslaves.auth.kafka

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import app.aviaslaves.auth.common.Environment
import org.apache.kafka.common.errors.TopicExistsException
import java.time.Duration
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean

class Kafka private constructor() {
    private lateinit var producer: KafkaProducer<String, String>
    private lateinit var consumer: KafkaConsumer<String, String>
    private lateinit var adminClient: AdminClient
    private lateinit var listenTopic: String
    private lateinit var sendTopic: String
    private var consumerThread: Thread? = null
    private val isClosed = AtomicBoolean(false)

    companion object {
        fun configure(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    class Builder {
        private val props = Properties()
        private var listenTopic: String = ""
        private var sendTopic: String = ""
        private var messageHandler: (Kafka, String, String) -> Unit = { _, _, _ -> }

        fun onServer(brokers: String) = apply {
            props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = brokers
            props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = brokers
            props[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = brokers
        }

        fun initializeTopics(vararg topics: String) = apply {
            val adminProps = Properties().apply {
                putAll(props)
            }
            AdminClient.create(adminProps).use { client ->
                try {
                    client.createTopics(topics.map { NewTopic(it, 1, 1.toShort()) }).all().get()
                } catch (e: ExecutionException) {
                    if (e.cause !is TopicExistsException) {
                        throw e
                    }
                }
            }
        }

        fun listen(topic: String) = apply {
            this.listenTopic = topic
        }

        fun send(topic: String) = apply {
            this.sendTopic = topic
        }

        fun onMessage(handler: (Kafka, String, String) -> Unit) = apply {
            this.messageHandler = handler
        }

        fun build(): Kafka {
            val kafka = Kafka()
            kafka.listenTopic = listenTopic
            kafka.sendTopic = sendTopic
            kafka.adminClient = AdminClient.create(props)
            kafka.producer = KafkaProducer(props.apply {
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            })
            kafka.consumer = KafkaConsumer(props.apply {
                put(ConsumerConfig.GROUP_ID_CONFIG, "my-group-id")
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            })
            kafka.startConsuming(messageHandler, kafka)
            return kafka
        }
    }

    fun sendMessage(key: String, value: String) {
        val record = ProducerRecord(sendTopic, key, value)
        producer.send(record) { metadata, exception ->
            if (exception == null) {
                Environment.logger.info("Message sent successfully to topic ${metadata.topic()} partition ${metadata.partition()} offset ${metadata.offset()}")
            } else {
                Environment.logger.error(exception.message)
            }
        }
    }

    private fun startConsuming(messageHandler: (Kafka, String, String) -> Unit, kafka: Kafka) {
        consumer.subscribe(listOf(listenTopic))
        Runtime.getRuntime().addShutdownHook(Thread {
            Environment.logger.info("Shutdown hook triggered, closing consumer.")
            close()
        })

        consumerThread = Thread {
            try {
                while (!Thread.currentThread().isInterrupted) {
                    val records = consumer.poll(Duration.ofMillis(1000))
                    for (record in records) {
                        Environment.logger.info("Received message: key = ${record.key()}, value = ${record.value()}, offset = ${record.offset()}")
                        messageHandler(kafka, record.key(), record.value())
                    }
                }
            } catch (e: InterruptedException) {
                Environment.logger.info("Consumer polling interrupted.")
                Thread.currentThread().interrupt() // Preserve interrupt status
            } catch (e: Exception) {
                Environment.logger.error("Unexpected error", e)
            } finally {
                try {
                    consumer.close()
                } catch (e: Exception) {
                    Environment.logger.error("Error closing consumer", e)
                }
                Environment.logger.info("Consumer closed.")
            }
        }
        consumerThread?.start()
    }

    @Synchronized
    fun close() {
        if (isClosed.compareAndSet(false, true)) {
            try {
                consumerThread?.interrupt()
                consumerThread?.join() // Wait for the consumer thread to finish
                producer.flush()
                producer.close()
                adminClient.close()
            } catch (e: Exception) {
                Environment.logger.error("Error closing Kafka client", e)
            }
        }
    }
}
