package app.database;

import app.model.Account;

public class Session {
   
    private static Account currentAccount = null;

    public static Account getCurrentAccount() {
        return currentAccount;
    }

    public static void setCurrentAccount(Account account) {
        currentAccount = account;
    }
    
    
    public static boolean isLoggedIn() {
        return currentAccount != null;
    }
}