package org.tarunitachi.truecaller;

import lombok.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Come up with application that can perform
 *
 * Caller identification
 * Call blocking
 * Spam Detection
 * Store users contacts
 * Search for users contacts by name and number
 * Use cases
 * Users should be able to register -> User
 * Users should be able to add contacts -> User, GlobalContact
 * Users should be able to import contacts -> User, GlobalContact
 * Users should be able to block contacts ->  User
 * Users should be able to report spam -> GlobalContact
 * Users should be able to unblock numbers -> User
 * Users should be notified when suspected junk caller calls ->GlobalContact
 * Users should be able to identify caller when call comes -> GlobalContact
 * Users should be able to upgrade to premium plans -> User
 * Users should be able to search for contacts by name -> User
 * Users should be able to search for contacts by number -> User
 * Post registration and addition of contacts register with global directory -> GlobalContact
 * Users should be able to search from global directory -> GlobalContact
 */

enum PlanType{
    FREE, PREMIUM
}
@Data
@RequiredArgsConstructor
class User{
    private final String id;
    private Map<String, UserContact> contactList = new HashMap<>();
    private PlanType planType = PlanType.FREE;

}

@Data
@AllArgsConstructor
class Contact{
    private String number;
}

@Setter
@Getter
@ToString(callSuper = true)
class UserContact extends Contact{
    private boolean isBlocked;
    private String savedName;
    public UserContact(String number, boolean isBlocked, String savedName) {
        super(number);
        this.isBlocked =isBlocked;
        this.savedName= savedName;
    }
}

@Getter
@Setter
@ToString(callSuper = true)
class GlobalContact extends Contact{
    private boolean isJunkCaller;
    private String globalName;
    private Set<String> reportedUserIds = new HashSet<>();
    public GlobalContact(String number, boolean isJunkCaller, String globalName,  Set<String> reportedUserIds) {
        super(number);
        this.isJunkCaller = isJunkCaller;
        this.globalName = globalName;
        this.reportedUserIds = reportedUserIds;
    }
}


class UserRepo{

    private GlobalContactRepo globalContactRepo = GlobalContactRepo.getInstance();

    Map<String, User> userMap = new ConcurrentHashMap<>();

    public void createUser(User user){
        userMap.put(user.getId(), user);
    }

    public User fetch(String userId){
        if(!checkIfUserExists(userId)){
            throw new RuntimeException("User doesn't exist");
        }
        return userMap.get(userId);
    }

    public void addContact(String userId, UserContact contact){
        if(!checkIfUserExists(userId)){
            throw new RuntimeException("User doesn't exist");
        }
        userMap.get(userId).getContactList().put(contact.getNumber(), contact);
        globalContactRepo.upsert(new GlobalContact(contact.getNumber(), false, contact.getSavedName(), new HashSet<>()));
    }

    public void addContacts(String userId, List<UserContact> contacts){
        if(!checkIfUserExists(userId)){
            throw new RuntimeException("User doesn't exist");
        }
        contacts.forEach((contact) -> {
            userMap.get(userId).getContactList().put(contact.getNumber(), contact);
            globalContactRepo.upsert(new GlobalContact(contact.getNumber(), false, contact.getSavedName(),new HashSet<>()));
        });
    }

    public void blockContact(String userId, String number){
        if(!checkIfUserExists(userId)){
            throw new RuntimeException("User doesn't exist");
        }
        if(!checkIfNumberExists(userId, number)){
            throw new RuntimeException("Number doesn't exist");
        }
        userMap.get(userId).getContactList().get(number).setBlocked(true);
    }

    public void unblockContact(String userId, String number){
        if(!checkIfUserExists(userId)){
            throw new RuntimeException("User doesn't exist");
        }
        if(!checkIfNumberExists(userId, number)){
            throw new RuntimeException("Number doesn't exist");
        }
        userMap.get(userId).getContactList().get(number).setBlocked(false);
    }

    public void upgradeToPremium(String userId){
        if(!checkIfUserExists(userId)){
            throw new RuntimeException("User doesn't exist");
        }

        userMap.get(userId).setPlanType(PlanType.PREMIUM);
    }

    public List<UserContact> searchUserContacts(String userId, String searchName, String searchNumber){
        if(!checkIfUserExists(userId)){
            throw new RuntimeException("User doesn't exist");
        }
        User user = userMap.get(userId);
        if(searchNumber!=null && !searchNumber.isEmpty()){
            return Collections.singletonList(user.getContactList().get(searchNumber));
        }

        List<UserContact> contactList = new ArrayList<>(user.getContactList().values());
        List<UserContact> result= new ArrayList<>();
        if(searchName!=null && !searchName.isEmpty()){
            contactList.forEach((contact) ->{
                if(contact.getSavedName().matches(searchName + ".*")){
                    result.add(contact);
                }
            });

        }
        return result;
    }

    private boolean checkIfUserExists(String userID){
        return userMap.containsKey(userID);
    }

    private boolean checkIfNumberExists(String userID, String number){
        return userMap.get(userID).getContactList().containsKey(number);
    }

}

class GlobalContactRepo{

    private static GlobalContactRepo repo = new GlobalContactRepo();

    private GlobalContactRepo(){};

    public static GlobalContactRepo getInstance(){
        return repo;
    }
    private Map<String, GlobalContact> globalContactMap = new ConcurrentHashMap<>();

    public void upsert(GlobalContact globalContact){
        if(globalContactMap.containsKey(globalContact.getNumber())){

            return;
        }
        globalContactMap.put(globalContact.getNumber(),globalContact);
    }

    public void reportSpam(String userId, String number){
        //Check if userId exists
        if(!globalContactMap.containsKey(number)){
            globalContactMap.put(number, new GlobalContact(number, true, "DEFAULT", new HashSet<>(Arrays.asList(userId))));
            return;
        }
        globalContactMap.get(number).getReportedUserIds().add(userId);
        int spamCount = globalContactMap.get(number).getReportedUserIds().size();
        if(spamCount>=5){
            globalContactMap.get(number).setJunkCaller(true);
        }
    }

    public List<GlobalContact> searchGlobalContacts(String searchName, String searchNumber){
        if(searchNumber!=null && !searchNumber.isEmpty()){
            GlobalContact globalContact = globalContactMap.get(searchNumber);
            return Collections.singletonList(globalContactMap.get(searchNumber));
        }
        List<GlobalContact> contactList = fetchAll();
        List<GlobalContact> result= new ArrayList<>();
        if(searchName!=null && !searchName.isEmpty()){
            contactList.forEach((contact) ->{
                if(contact.getGlobalName().matches(searchName + ".*")){
                    result.add(contact);
                }
            });
        }
        return result;
    }

    public GlobalContact fetch(String number){
        return globalContactMap.get(number);
    }

    public List<GlobalContact> fetchAll(){
        return new ArrayList<>(globalContactMap.values());
    }

    public void upsertBulk(List<GlobalContact> globalContacts){
        for (GlobalContact globalContact : globalContacts) {
            upsert(globalContact);
        }
    }

}

public class TrueCallerLauncher {
    public static void main(String[] args) {
        UserRepo userRepo = new UserRepo();
        GlobalContactRepo globalContactRepo= GlobalContactRepo.getInstance();
        userRepo.createUser(new User("u1"));
        userRepo.createUser(new User("u2"));
        userRepo.addContacts("u1", Arrays.asList(new UserContact("1234", false, "Tarun"),
                new UserContact("5678", false, "Kiran")));
        printUserContactInfo("u1", new ArrayList<>(userRepo.fetch("u1").getContactList().values()));
        userRepo.blockContact("u1", "5678");
        printUserContactInfo("u1", new ArrayList<>(userRepo.fetch("u1").getContactList().values()));
        userRepo.unblockContact("u1", "5678");
        printUserContactInfo("u1", new ArrayList<>(userRepo.fetch("u1").getContactList().values()));
        printGlobalContactInfo(globalContactRepo.fetchAll());

        globalContactRepo.reportSpam("u1", "1234");
        globalContactRepo.reportSpam("u2", "1234");
        globalContactRepo.reportSpam("u3", "1234");
        globalContactRepo.reportSpam("u4", "1234");
        printGlobalContactInfo(globalContactRepo.fetchAll());

        globalContactRepo.reportSpam("u5", "1234");

        printGlobalContactInfo(globalContactRepo.fetchAll());

        List<UserContact> userContacts = userRepo.searchUserContacts("u1", null, "1234");
        System.out.println("Searched contacts 1 :");
        printUserContactInfo("u1", userContacts);

        userContacts = userRepo.searchUserContacts("u1", "Kir", null);
        System.out.println("Searched contacts 2 :");
        printUserContactInfo("u1", userContacts);

        List<GlobalContact> globalContacts = globalContactRepo.searchGlobalContacts(null, "1234");
        System.out.println("Global search contacts 1 :");
        printGlobalContactInfo(globalContacts);

         globalContacts = globalContactRepo.searchGlobalContacts("Kir", null);
        System.out.println("Global search contacts 2 :");
        printGlobalContactInfo(globalContacts);

        GlobalContact globalContact = globalContactRepo.fetch("1234");
        printGlobalContactInfo(Arrays.asList(globalContact));
    }

    private static void printUserContactInfo(String userId, List<UserContact> contacts){
        System.out.println("Contacts for:: " + userId);
        contacts.forEach((contact)-> {
            System.out.println(contact.toString());
        });
    }

    private static void printGlobalContactInfo(List<GlobalContact> contacts){
        System.out.println("Global contacts");
        contacts.forEach((contact)-> {
            System.out.println(contact.toString());
        });
    }

}
