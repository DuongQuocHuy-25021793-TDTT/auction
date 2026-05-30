package app.database;

import app.model.User;

public class Session {
    private static User currentUser;
    private static boolean welcomeMessageShown;

    private Session() {
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
        welcomeMessageShown = false;
    }

    public static boolean isWelcomeMessageShown() {
        return welcomeMessageShown;
    }

    public static void setWelcomeMessageShown(boolean welcomeMessageShown) {
        Session.welcomeMessageShown = welcomeMessageShown;
    }
}
