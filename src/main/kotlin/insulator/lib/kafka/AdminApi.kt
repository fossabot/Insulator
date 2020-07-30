package insulator.lib.kafka

import arrow.core.Either
import arrow.core.extensions.fx
import insulator.lib.helpers.map
import insulator.lib.helpers.toCompletableFuture
import insulator.lib.kafka.model.ConsumerGroup
import insulator.lib.kafka.model.ConsumerGroupMember
import insulator.lib.kafka.model.Topic
import insulator.lib.kafka.model.TopicPartitionLag
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.TopicDescription
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition

class AdminApi(private val admin: AdminClient, private val consumer: Consumer<Any, Any>) {

    fun listTopics() = admin.listTopics().names().toCompletableFuture().map { it.toList() }

    fun describeTopic(vararg topicNames: String) =
        admin.describeTopics(topicNames.toList()).all().toCompletableFuture()
            .map { description ->
                val recordCount = description.values
                    .map { it.name() to it.toTopicPartitions() }
                    .map { (name, partitions) -> name to consumer.endOffsets(partitions).values.sum() - consumer.beginningOffsets(partitions).values.sum() }
                    .toMap()
                description.values.map {
                    Topic(
                        name = it.name(),
                        messageCount = recordCount.getOrDefault(it.name(), null),
                        isInternal = it.isInternal,
                        partitionCount = it.partitions().size
                    )
                }
            }

    fun listConsumerGroups() = admin.listConsumerGroups().all().toCompletableFuture()
        .map { consumerGroup -> consumerGroup.map { it.groupId() } }

    fun describeConsumerGroup(groupId: String) =
        admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().toCompletableFuture()
            .thenCombineAsync(admin.describeConsumerGroups(listOf(groupId)).all().toCompletableFuture().map { it.values.first() }) { partitionToOffset, description ->
                Either.fx<Throwable, ConsumerGroup> {
                    ConsumerGroup(
                        (!description).groupId(),
                        (!description).state(),
                        (!description).members().map {
                            ConsumerGroupMember(
                                it.clientId(),
                                it.assignment().topicPartitions().map { tp -> TopicPartitionLag(tp.topic(), tp.partition(), getLag(tp, (!partitionToOffset)[tp])) }
                            )
                        }
                    )
                }
            }

    private fun getLag(partition: TopicPartition, currentOffset: OffsetAndMetadata?) =
        consumer.endOffsets(mutableListOf(partition)).values.first() - (currentOffset?.offset() ?: 0)

//    fun alterConsumerGroupOffset(consumerGroupId: String) =
//        admin.listConsumerGroupOffsets("123").partitionsToOffsetAndMetadata()
//        admin.alterConsumerGroupOffsets(consumerGroupId, mapOf(TopicPartition("name", 1) to OffsetAndMetadata()))

    private fun TopicDescription.toTopicPartitions() = this.partitions().map { TopicPartition(this.name(), it.partition()) }
}
