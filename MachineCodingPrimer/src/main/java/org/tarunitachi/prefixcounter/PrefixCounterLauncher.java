package org.tarunitachi.prefixcounter;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;


// Main class should be named 'Solution'
class PrefixCounterLauncher {

    public static void main(String[] args) throws InterruptedException {
        TrackingService trackingService = new TrackingServiceImpl();

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        for(int i =0;i<100;i++){
            executorService.submit(() -> {
                int rand = new Random().nextInt(1000);
                trackingService.startTracking(rand, Arrays.asList("UPI", "Karnataka", "Bangalore"));
                trackingService.startTracking(rand+1, Arrays.asList("UPI", "Karnataka", "Bangalore"));
                trackingService.startTracking(rand+2, Arrays.asList("UPI", "Karnataka", "Mysore"));
                trackingService.stopTracking(rand);

            });
        }
        executorService.shutdown();
        Thread.sleep(7000);
        System.out.println(trackingService.getCounts(Arrays.asList("UPI")));
        System.out.println(trackingService.getCounts(Arrays.asList("UPI", "Karnataka")));
        System.out.println(trackingService.getCounts(Arrays.asList("UPI", "Karnataka", "Bangalore")));
        System.out.println(trackingService.getCounts(Arrays.asList("Bangalore")));

    }
}

interface TrackingService{
    void startTracking(Integer id, List<String> tags);
    void stopTracking(Integer id);
    int getCounts(List<String> tags);
}

class TrackingServiceImpl implements TrackingService{

    private final static String DELIMITER = "_";

    //Can be injected using spring
    private TrackingRepo trackingRepo = new InMemoryTrackingRepoImpl();
    private PrefixCounterRepo prefixCounterRepo = new InMemoryPrefixCounterRepoImpl();

    public void startTracking(Integer id, List<String> tags){
        if(tags.isEmpty()){
            return;
        }
        trackingRepo.put(id, tags);
        String prefixTag = "";
        for(String tag: tags){
            prefixTag += (DELIMITER + tag);
            incrementPrefixCounts(prefixTag);
        }
    }

    public void stopTracking(Integer id){
        List<String> tags = trackingRepo.get(id);
        if(tags == null || tags.isEmpty()){
            return;
        }
        String prefixTag = "";
        for(String tag: tags){
            prefixTag += (DELIMITER + tag);
            decrementPrefixCounts(prefixTag);
        }
        trackingRepo.remove(id);
    }

    public int getCounts(List<String> tags){
        if(tags.isEmpty()){
            return 0;
        }
        String prefixTag = getPrefixTag(tags);
        if(!prefixCounterRepo.containsKey(prefixTag)){
            return 0;
        }
        return prefixCounterRepo.get(prefixTag);
    }

    private void incrementPrefixCounts(String prefixTag){
        prefixCounterRepo.incrementAndGet(prefixTag);
    }

    private void decrementPrefixCounts(String prefixTag){
        prefixCounterRepo.decrementAndGet(prefixTag);
    }

    private String getPrefixTag(List<String> tags){
        String prefixTag = "";
        for(String tag: tags){
            prefixTag += (DELIMITER + tag);
        }
        return prefixTag;
    }

}

interface TrackingRepo{
    void put(Integer id, List<String> tags);
    List<String> get(Integer id);
    void remove(Integer id);
}

interface PrefixCounterRepo{
    public void incrementAndGet(String key);
    public void decrementAndGet(String key);
    Integer get(String key);
    void remove(String key);
    boolean containsKey(String key);
}

class InMemoryTrackingRepoImpl implements TrackingRepo{
    private static Map<Integer, List<String>> tracks = new ConcurrentHashMap<>();

    public void put(Integer id, List<String> tags){
        tracks.put(id, tags);
    }

    public List<String> get(Integer id){
        return tracks.get(id);
    }

    public void remove(Integer id){
        tracks.remove(id);
    }
}

class InMemoryPrefixCounterRepoImpl implements PrefixCounterRepo{
    //We need to use atomic integer here because ConcurrentHashMap locks for writes only.
    // But reads can happen during writes and can lead into incosistent stages if we update without locks/atomic
    private static Map<String, AtomicInteger> prefixCounts = new ConcurrentHashMap<>();

    public void incrementAndGet(String key){
        if(!prefixCounts.containsKey(key)){
            prefixCounts.put(key, new AtomicInteger(0));
        }
        prefixCounts.get(key).incrementAndGet();
    }

    public void decrementAndGet(String key){
        if(!prefixCounts.containsKey(key) && prefixCounts.get(key).get()<=0){
            throw new RuntimeException("Wtf is happening here");
        }
        prefixCounts.get(key).decrementAndGet();
    }

    public Integer get(String key){
        return prefixCounts.get(key).get();
    }

    public void remove(String key){
        prefixCounts.remove(key);
    }

    public boolean containsKey(String key){
        return prefixCounts.containsKey(key);
    }
}

//Future use cases:
// class SQLTrackingRepoImpl implements TrackingRepo{
//    //Can be implemented and injected into spring beans if required in future
// }

// class MongoTrackingRepoImpl implements TrackingRepo{
//    //Can be implemented and injected into spring beans if required in future
// }

//Same for PrefixCounterRepo as well

//Assuming that the tags can't be empty in all the 3 API inputs