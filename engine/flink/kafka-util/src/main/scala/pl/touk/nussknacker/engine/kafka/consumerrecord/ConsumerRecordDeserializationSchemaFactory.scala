package pl.touk.nussknacker.engine.kafka.consumerrecord

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.Deserializer
import pl.touk.nussknacker.engine.kafka.{KafkaConfig, serialization}
import pl.touk.nussknacker.engine.kafka.serialization.KafkaDeserializationSchemaFactory

import scala.reflect.classTag

/**
  * Produces deserialization schema that describes how to turn the Kafka raw [[org.apache.kafka.clients.consumer.ConsumerRecord]]
  * (with raw key-value of type Array[Byte]) into deserialized ConsumerRecord[K, V] (with proper key-value types).
  * It allows the source to provide event value AND event metadata to the stream.
  *
  * @tparam K - type of key of deserialized ConsumerRecord
  * @tparam V - type of value of deserialized ConsumerRecord
  */
abstract class ConsumerRecordDeserializationSchemaFactory[K, V] extends KafkaDeserializationSchemaFactory[ConsumerRecord[K, V]] with Serializable {

  protected def createKeyDeserializer(kafkaConfig: KafkaConfig): Deserializer[K]

  protected def createValueDeserializer(kafkaConfig: KafkaConfig): Deserializer[V]

  override def create(topics: List[String], kafkaConfig: KafkaConfig): serialization.KafkaDeserializationSchema[ConsumerRecord[K, V]] = {

    new serialization.KafkaDeserializationSchema[ConsumerRecord[K, V]] {

      @transient
      private lazy val keyDeserializer = createKeyDeserializer(kafkaConfig)
      @transient
      private lazy val valueDeserializer = createValueDeserializer(kafkaConfig)

      override def deserialize(record: ConsumerRecord[Array[Byte], Array[Byte]]): ConsumerRecord[K, V] = {
        val key = keyDeserializer.deserialize(record.topic(), record.key())
        val value = valueDeserializer.deserialize(record.topic(), record.value())
        new ConsumerRecord[K, V](
          record.topic(),
          record.partition(),
          record.offset(),
          record.timestamp(),
          record.timestampType(),
          ConsumerRecord.NULL_CHECKSUM.toLong, // ignore deprecated checksum
          record.serializedKeySize(),
          record.serializedValueSize(),
          key,
          value,
          record.headers(),
          record.leaderEpoch()
        )
      }

      override def isEndOfStream(nextElement: ConsumerRecord[K, V]): Boolean = false
    }
  }

}
