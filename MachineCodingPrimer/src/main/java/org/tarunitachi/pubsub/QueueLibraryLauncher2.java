package org.tarunitachi.pubsub;

/**
 *
 * Design an efficient in-memory queueing system with low latency requirements and also write producer and consumer using multi threading with following constraints :
 *
 * Must Have Tasks :
 *
 * There should be a queue, that can receive the message from the producer, and send the message to the consumer.
 * Queue is bounded in size and completely held in-memory. Size should be configurable.
 * Queue should only hold JSON messages.
 * Queue will have at least one producer and multiple consumers.
 * Consumers register callbacks that will be invoked whenever there is a new message
 * Allow subscription of consumers to messages that match a particular expression
 * Consumers might have dependency relationships between them.
 * For ex :
 * if there are three consumers A, B and C. One dependency relationship can be that C cannot consume a particular message before A and B have consumed it.
 * C -> (A,B) (-> means must process after).
 *
 * Handle concurrent writes and reads consistently between producer and consumers.
 * Bonus Tasks :
 *
 * Provide retry mechanisms to handle failures in message processing. It could be failure in publishing or consumption.
 * Handle the message TTL. means the message could be expired after some time T. if a message is expired, it should not be delivered to the consumer.
 * Suggestions :
 * Try completing the tasks one by one, run it, test it, then move on to the next. Pick the task in any order that you want.
 * Think about the extension of the problem before choosing your LLD. You might be asked to add some new features in this problem during evaluation time.
 *
 * Restriction :
 * You are not allowed to use in-built queue data structure provided by any language. Expected to implement the queue.
 *
 * Allowed :
 * You can use library for JSON
 */
public class QueueLibraryLauncher2 {

}
