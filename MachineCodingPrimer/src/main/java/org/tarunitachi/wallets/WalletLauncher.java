package org.tarunitachi.wallets;


import javafx.util.Pair;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

//Pojos
@Data
class User{
    private String id;
    private String name;
    private String emailId;
    private String walletId;
}

@Data
@AllArgsConstructor
class Transaction {
    private String id;
    private String srcWalletId;
    private String destWalletId;
    private double amount;
    private Long timestamp;
    private TxnStatus txnStatus;
}

@Data
@AllArgsConstructor
class Wallet {
    private String id;
    private String userId;
    private Double balance;
    private List<Transaction> transactions;

    public synchronized void incrementBalance(int amount){
        this.balance+=amount;
    }
    public synchronized void decrementBalance(int amount){
        this.balance-=amount;
    }
}

enum TxnStatus{
    STARTED, IN_PROGRESS, SUCCESS, FAILURE
}

//Repos
class TxnRepo {
    //    Map<String, Transaction> transactionMap= new ConcurrentHashMap<>();
    TreeSet<Transaction> transactionTreeSet = new TreeSet<>((a, b) -> (int)(a.getTimestamp()-b.getTimestamp()));
//      TreeSet<Transaction> transactionTreeSet = new TreeSet<>();

    Map<String, TreeSet<Transaction>> walletTransactions = new ConcurrentHashMap<>();

    public void addTransaction(Transaction transaction){
        transactionTreeSet.add(transaction);
        if(!walletTransactions.containsKey(transaction.getSrcWalletId())){
            walletTransactions.put(transaction.getSrcWalletId(), new TreeSet<>((a, b) -> (int)(a.getTimestamp()-b.getTimestamp())));
        }
        if(!walletTransactions.containsKey(transaction.getDestWalletId())){
            walletTransactions.put(transaction.getDestWalletId(), new TreeSet<>((a, b) -> (int)(a.getTimestamp()-b.getTimestamp())));
        }
        walletTransactions.get(transaction.getSrcWalletId()).add(transaction);
        walletTransactions.get(transaction.getDestWalletId()).add(transaction);

    }

    public TreeSet<Transaction> getAllTransactions(String walletId){
        return walletTransactions.get(walletId);
    }

}
class WalletRepo {

    private static WalletRepo walletRepo = new WalletRepo();

    private WalletRepo(){};

    public static WalletRepo getInstance(){
        return walletRepo;
    }
    ConcurrentHashMap<String, Wallet> walletMap = new ConcurrentHashMap<>();
    Map<String, Lock> lockMap = new ConcurrentHashMap<>();

    public void upsertWallet(Wallet wallet){
        if(!lockMap.containsKey(wallet.getId())){
            lockMap.put(wallet.getId(), new ReentrantLock());
        }
        lockMap.get(wallet.getId()).lock();
        walletMap.put(wallet.getId(), wallet);
        lockMap.get(wallet.getId()).unlock();
    }
    public Wallet fetchWallet(String walletId){
        if(!lockMap.containsKey(walletId)){
            lockMap.put(walletId, new ReentrantLock());
        }
        lockMap.get(walletId).lock();
        Wallet wallet = walletMap.get(walletId);
        lockMap.get(walletId).unlock();
        return wallet;
    }

    public List<Wallet> fetchAllWallets(){
        return new ArrayList<>(walletMap.values());

    }

    public void transferBalance(String srcWalletId, String destWalletId, int amount){
        decrementBalance(srcWalletId, amount);
        incrementBalance(destWalletId, amount);
    }
    private void decrementBalance(String walletId, int amount){
        Wallet wallet = fetchWallet(walletId);
        wallet.decrementBalance(amount);
    }
    private void incrementBalance(String walletId, int amount){
        Wallet wallet = fetchWallet(walletId);
        wallet.incrementBalance(amount);
    }
}

class WalletService {
    private WalletRepo walletRepo = WalletRepo.getInstance();
    private TxnRepo txnRepo = new TxnRepo();


    public void createWallet(Wallet wallet){
        validateWallet(wallet);
        walletRepo.upsertWallet(wallet);
    }

    private void validateWallet(Wallet wallet) {
//        if(!wallet.getTransactions().isEmpty()){
//            throw new RuntimeException("Txns should be empty");
//        }
    }

    public void transferAmount(String srcUserId, String destUserId, String srcWalletId, String destWalletId, int amount){
        //Execute in transaction
        Wallet srcWallet = walletRepo.fetchWallet(srcWalletId);
        Wallet destWallet = walletRepo.fetchWallet(destWalletId);
        //validate ownership of the wallets
        if(!srcWallet.getUserId().equals(srcUserId) || !destWallet.getUserId().equals(destUserId)){
            throw new RuntimeException("Alerted security team for taking action");
        }
        //Initiate the transaction
        walletRepo.transferBalance(srcWalletId, destWalletId, amount);

        //if transaction fails, roll back and update txn status as failed

        txnRepo.addTransaction(new Transaction(UUID.randomUUID().toString(), srcWalletId, destWalletId, amount, System.currentTimeMillis(), TxnStatus.SUCCESS));

    }

    public Pair<Wallet, TreeSet<Transaction>> fetchAccountStatement(String walletId){
        Wallet wallet = walletRepo.fetchWallet(walletId);
        TreeSet<Transaction> transactionsList = txnRepo.getAllTransactions(walletId);
        return new Pair<>(wallet, transactionsList);
    }

    public void printOverview(){
        walletRepo.fetchAllWallets().forEach((w) -> System.out.println(w.toString()));
    }

    public void exit(){

    }
}


public class WalletLauncher {
    public static void main(String[] args) throws InterruptedException {
        WalletService service = new WalletService();
        Wallet wallet1 = new Wallet("Wallet1", "User1", 100000.0, new ArrayList<>());
        Wallet wallet2 = new Wallet("Wallet2", "User2", 100000.0, new ArrayList<>());
        service.createWallet(wallet1);
        service.createWallet(wallet2);
        //        service.createWallet(new Wallet("Wallet2", "User2", 2000.0, new ArrayList<>()));
        //        service.createWallet(new Wallet("Wallet3", "User3", 2000.0, new ArrayList<>()));
//            service.transferAmount("User1", "User2", "Wallet1", "Wallet2", 100);


        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for(int i =0;i<1000;i++){
            executorService.submit(() -> {
                service.transferAmount("User1", "User2", "Wallet1", "Wallet2", 100);
            });
            executorService.submit(() -> {
                service.transferAmount("User2", "User1", "Wallet2", "Wallet1", 50);
            });
        }
        executorService.shutdown();
        Thread.sleep(10000);
        service.printOverview();

    }
}
