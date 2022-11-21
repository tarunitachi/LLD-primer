package org.tarunitachi.pubsub;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

/**
 You have to make a in-memory pull based queue library which will:
 - There can be multiple queues maintained by the Library
 - Each queue must support multiple publisher and consumers.
 - Each message has a maximum retention period beyond which it should not reside in memory.
 - Each message inside the queue can have a TTL value. Any message with expired TTL should not be consumed by any consumer and should not reside in the memory as well
 **/

class Data{
    static Integer globalTTL = 100;
    String message;
    Integer publishingTime;
    Integer customTTL;
    Integer staleTime;
    public Data(String message, Integer customTTL){
        this.message = message;
        publishingTime = (int) (System.currentTimeMillis()/1000);
        if(customTTL == null) customTTL = globalTTL;
        staleTime = publishingTime+customTTL;
    }
}


class QueueCleanUpJob implements Runnable{
    Map<Integer, Queue<Data>> queueMap;
    public QueueCleanUpJob( Map<Integer, Queue<Data>> queueMap){
        this.queueMap = queueMap;
    }

    public void run(){
        startDaemon();
    }
    public void startDaemon(){
        while(true){
            cleanup();
            try{
                Thread.sleep(60000);
            }
            catch(Exception e){
                System.out.println("Exceptoin during sleep");
            }
        }
    }
    void cleanup(){
        for(Integer queueNumber:queueMap.keySet()){
            Queue<Data> q = queueMap.get(queueNumber);
            for(Object item: q){
                Data data = (Data) item;
                if(data.staleTime<(int)(System.currentTimeMillis()/1000)){

                    q.remove(item);
                }
            }
        }
    }
}
class QueueLibrary{
    // Map<Integer, QueueNode> staleTimeMap;
    Map<Integer, Queue<Data>> queueMap;

    public QueueLibrary(){
        queueMap = new HashMap<>();
        QueueCleanUpJob job = new QueueCleanUpJob(queueMap);
        Thread t1 = new Thread(job);
        t1.start();
    }

    //Debugging
    public int getQueuesize(Integer queueNumber){
        return queueMap.get(queueNumber).size();
    }

    public void addQueue(Integer queueNumber,int size){
        Queue<Data> q = new LinkedBlockingQueue(size);
        queueMap.put(queueNumber, q);
    }

    public Set<Integer> getQueueIds(){
        return queueMap.keySet();
    }

    public void addElement(Integer queueNumber, Data data){
        Queue<Data> q = queueMap.get(queueNumber);
        if(q==null) throw new RuntimeException();
        q.add(data);
    }
    public Data peekElement(Integer queueNumber){
        Queue<Data> q = queueMap.get(queueNumber);
        if(q==null) throw new RuntimeException();
        return q.peek();
    }

    public Data pollElement(Integer queueNumber){
        Queue<Data> q = queueMap.get(queueNumber);
        if(q==null) throw new RuntimeException();
        return q.poll();
    }

}

class Publisher{
    QueueLibrary queueLibrary;
    public Publisher(QueueLibrary queueLibrary){
        this.queueLibrary = queueLibrary;
    }
    void publish(Integer queueNumber, Data data){
        queueLibrary.addElement(queueNumber, data);
    }
}

class Consumer{
    QueueLibrary queueLibrary;
    public Consumer(QueueLibrary queueLibrary){
        this.queueLibrary = queueLibrary;
    }
    Data fetch(Integer queueNumber){
        Data data =  queueLibrary.pollElement(queueNumber);
        if(data.staleTime<(int)(System.currentTimeMillis()/1000)){
            //Don't consume here. Ignore.
            Data errorData = new Data("Stale entry.. Not consuming", null);
            return errorData;
        }
        return data;
    }
}

class Admin{
    QueueLibrary queueLibrary;
    public Admin(QueueLibrary queueLibrary){
        this.queueLibrary = queueLibrary;
    }
    void createQueue(Integer queueNumber, int size){
        queueLibrary.addQueue(queueNumber, size);
    }
}

class QueueLibraryLauncher {
    public static void main(String[] args) {
        QueueLibrary queueLibrary = new QueueLibrary();

        Admin admin  = new Admin(queueLibrary);
        admin.createQueue(100, 100);

        Publisher p1 = new Publisher(queueLibrary);
        Publisher p2 = new Publisher(queueLibrary);
        Consumer c1 = new Consumer(queueLibrary);
        Consumer c2 = new Consumer(queueLibrary);

        Data data1 = new Data("p1 message", 100);
        p1.publish(100, data1);
        System.out.println(queueLibrary.getQueuesize(100));

        Data data2= new Data("p2 message", 100);
        p2.publish(100, data2);

        System.out.println(queueLibrary.getQueuesize(100));
        try{
            Thread.sleep(2000);
        }
        catch(Exception e){

        }
        System.out.println(queueLibrary.getQueuesize(100));

        // System.out.println(c1.fetch(100).message);
        // System.out.println(c2.fetch(100).message);



    }
}



