package app.database;

import app.model.User;

public class Session {
<<<<<<< HEAD
   
    private static Account currentAccount = null;
=======
    private static User currentUser;
    private static boolean welcomeMessageShown;

    private Session() {
    }
>>>>>>> 08664749faefa4fa4fb6e7af2e904320dd937533

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }
<<<<<<< HEAD
    
    
    public static boolean isLoggedIn() {
        return currentAccount != null;
=======

    public static void clear() {
        currentUser = null;
        welcomeMessageShown = false;
    }

    public static boolean isWelcomeMessageShown() {
        return welcomeMessageShown;
    }

    public static void setWelcomeMessageShown(boolean welcomeMessageShown) {
        Session.welcomeMessageShown = welcomeMessageShown;
>>>>>>> 08664749faefa4fa4fb6e7af2e904320dd937533
    }
}