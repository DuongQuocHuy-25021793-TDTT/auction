package app.database;

import app.model.User;

public class Session {
   
    private static Account currentAccount = null;

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }
    
    
    public static boolean isLoggedIn() {
        return currentAccount != null;
    }
}