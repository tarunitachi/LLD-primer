package org.tarunitachi.facebook;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

//Design a simplified version of Facebook where users can create/delete posts, follow/unfollow another user and are able to see the most recent posts in the user's news feed. Following methods to be implemented:
//1) createPost(userId, postId): Compose a new post.
//2) getNewsFeed(userId): Retrieve the 10 most recent post ids in the user's news feed. Each item in the news feed must be posted by users who the user followed or by the user herself (Order -> most to least recent)
//3) follow(followerId, followeeId): Follower follows a followee.
//4) unfollow(followerId, followeeId): Follower unfollows a followee.
//5) deletePost(userId, postId): Delete an existing post.
//6) getNewsFeedPaginated(userId, pageNumber): Retrieve the most recent post ids in the user's news feed in a paginated manner. Each item in the news feed must be posted by users who the user followed or by the user herself (Order -> most to least recent) Assume pageSize= 2.
//
//Evaluation points :
//1) Test cases passed
//2) Code structuring and cleanliness
//3) Scale and concurrency

@Data
@RequiredArgsConstructor
class FBUser {
    private final String id;
    //    private String emailId;
//    private String name;
    private List<String> followers = new ArrayList<>();
    private List<String> following = new ArrayList<>();
}

@Data
@RequiredArgsConstructor
class FBPost{
    private final String id;
    private final String postedUserId;
    private String text;
    private int timestamp;
}


interface UserService{
    void createUser(FBUser fbUser);

    FBUser getUser(String id);

    void follow(String followerId, String followeeId);

    void unfollow(String followerId, String followeeId);
}

class UserServiceImpl implements UserService{

    private UserRepo userRepo = UserInMemoryRepo.getInstance();
    @Override
    public void createUser(FBUser fbUser) {
        userRepo.upsert(fbUser);
    }

    @Override
    public FBUser getUser(String id) {
        return userRepo.fetchbyId(id);
    }

    @Override
    public void follow(String followerId, String followeeId) {
        FBUser follower = userRepo.fetchbyId(followerId);
        FBUser followee = userRepo.fetchbyId(followeeId);
        followee.getFollowers().add(followerId);
        follower.getFollowing().add(followeeId);
        userRepo.upsert(followee);
        userRepo.upsert(follower);
    }

    @Override
    public void unfollow(String followerId, String followeeId) {
        FBUser follower = userRepo.fetchbyId(followerId);
        FBUser followee = userRepo.fetchbyId(followeeId);
        followee.getFollowers().remove(followerId);
        follower.getFollowing().remove(followeeId);
        userRepo.upsert(followee);
        userRepo.upsert(follower);
    }

}

interface NewsFeedService{
    List<FBPost> getNewsFeed(String userId);

    List<FBPost> getNewsFeedPaginated(String userId, int pageNumber, int pageSize);

}

class NewsFeedServiceImpl implements NewsFeedService{

    private NewsFeedRepo newsFeedRepo = NewsFeedInMemoryRepo.getInstance();

    @Override
    public List<FBPost> getNewsFeed(String userId) {
        return newsFeedRepo.fetchNewsFeed(userId);
    }

    @Override
    public List<FBPost> getNewsFeedPaginated(String userId, int pageNumber, int pageSize) {
        return newsFeedRepo.fetchNewsFeedPaginated(userId, pageNumber, pageSize);
    }
}
interface PostService{

    void createPost(FBPost fbPost);
    void deletePost(String userId, String postId);
}

class PostServiceImpl implements PostService{
    private PostRepo postRepo = PostInMemoryRepo.getInstance();
    @Override
    public void createPost(FBPost fbPost) {
        postRepo.upsert(fbPost);
    }


    @Override
    public void deletePost(String userId, String postId) {
        FBPost post = postRepo.fetchbyId(postId);
        if(!userId.equals(post.getPostedUserId())) {
            throw new RuntimeException("user doesnt have permission to delete");
        }
        postRepo.delete(postId);
    }

}

interface PostRepo{
    void upsert(FBPost post);
    FBPost fetchbyId(String id);

    void delete(String id);
}

class PostInMemoryRepo implements PostRepo{

    private static PostInMemoryRepo obj;

    // private constructor to force use of
    // getInstance() to create Singleton object
    private PostInMemoryRepo() {}

    public static PostInMemoryRepo getInstance()
    {
        if (obj==null)
            obj = new PostInMemoryRepo();
        return obj;
    }

    ConcurrentHashMap<String, FBPost> postMap = new ConcurrentHashMap<>();

    NewsFeedRepo newsFeedRepo = NewsFeedInMemoryRepo.getInstance();
    UserRepo userRepo = UserInMemoryRepo.getInstance();

    @Override
    public void upsert(FBPost post) {
        postMap.put(post.getId(), post);
        //can be made async
        newsFeedRepo.addPostToFollowersNewsFeed(userRepo.fetchbyId(post.getPostedUserId()), post);
    }

    @Override
    public FBPost fetchbyId(String id) {
        return postMap.get(id);
    }

    @Override
    public void delete(String id) {
        postMap.remove(id);
    }
}

interface UserRepo{
    void upsert(FBUser user);
    FBUser fetchbyId(String id);
}

class UserInMemoryRepo implements UserRepo{
    ConcurrentHashMap<String, FBUser> userMap = new ConcurrentHashMap<>();

    private static UserInMemoryRepo obj;

    // private constructor to force use of
    // getInstance() to create Singleton object
    private UserInMemoryRepo() {}

    public static UserInMemoryRepo getInstance()
    {
        if (obj==null)
            obj = new UserInMemoryRepo();
        return obj;
    }

    @Override
    public void upsert(FBUser user) {
        userMap.put(user.getId(), user);
    }

    @Override
    public FBUser fetchbyId(String id) {
        return userMap.get(id);
    }
}

interface NewsFeedRepo{
    void addPostToFollowersNewsFeed(FBUser postedUser, FBPost fbPost);

    List<FBPost> fetchNewsFeed(String userId);

    public List<FBPost> fetchNewsFeedPaginated(String userId, int pageNumber, int pageSize);
}

class NewsFeedInMemoryRepo implements NewsFeedRepo{
    ConcurrentHashMap<String, TreeSet<FBPost>> newsFeedMap = new ConcurrentHashMap<>();

    private static NewsFeedInMemoryRepo obj;

    // private constructor to force use of
    // getInstance() to create Singleton object
    private NewsFeedInMemoryRepo() {}

    public static NewsFeedInMemoryRepo getInstance()
    {
        if (obj==null)
            obj = new NewsFeedInMemoryRepo();
        return obj;
    }
    @Override
    public void addPostToFollowersNewsFeed(FBUser postedUser, FBPost fbPost) {
        List<String> followerIds = postedUser.getFollowers();
        for(String followerId : followerIds){
            TreeSet<FBPost> newsFeedPosts = newsFeedMap.get(followerId);
            if(newsFeedPosts==null){
                newsFeedPosts= new TreeSet<>((a, b) -> a.getTimestamp()-b.getTimestamp());
            }
            newsFeedPosts.add(fbPost);
//            newsFeedMap.put(followerId, newsFeedPosts);
        }
    }

    @Override
    public List<FBPost> fetchNewsFeed(String userId) {
        TreeSet<FBPost> posts = newsFeedMap.get(userId);
        if(posts == null || posts.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(newsFeedMap.get(userId));
    }

    @Override
    public List<FBPost> fetchNewsFeedPaginated(String userId, int pageNumber, int pageSize) {
        List<FBPost> list = fetchNewsFeed(userId);
        int start = pageNumber*pageSize;
        Integer end = Math.min(start + pageSize, list.size());
        return list.subList(start, end);
    }

}

//
//class PostSQLRepo implements PostRepo{
//
//}




public class FacebookLauncher {
    private static void printNewsFeed(List<FBPost> posts, String userId){
        System.out.println("Newsfeed for " + userId);
        for(FBPost post: posts){
            System.out.println(post.toString());
        }
    }
    public static void main(String[] args) {
        UserService userService = new UserServiceImpl();
        PostService postService = new PostServiceImpl();
        NewsFeedService newsFeedService = new NewsFeedServiceImpl();

        userService.createUser(new FBUser("U1"));
        userService.createUser(new FBUser("U2"));
        userService.createUser(new FBUser("U3"));
//        userService.createUser(new FBUser("U2", Arrays.asList("U1"), Arrays.asList("U1") ));
        userService.follow("U2", "U1");
        userService.follow("U3", "U1");
        userService.follow("U3", "U2");
        userService.follow("U1", "U3");

        postService.createPost(new FBPost("pu2", "U2"));
        postService.createPost(new FBPost("pu1", "U1"));
        postService.createPost(new FBPost("pu3", "U3"));

        printNewsFeed(newsFeedService.getNewsFeed("U1"), "U1");
        printNewsFeed(newsFeedService.getNewsFeed("U2"), "U2");
        printNewsFeed(newsFeedService.getNewsFeed("U3"), "U3");
    }


}


