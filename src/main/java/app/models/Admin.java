package app.models;

public class Admin extends User {

    public Admin(String name) {
        this.name = name;
    }

    @Override
        public String toString(){
            return "Admin" + getName();
        }
    
}
