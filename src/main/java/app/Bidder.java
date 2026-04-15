package app;
class Bidder extends User {

    @Override
    public String toString() {
        return "Bidder" + getName() ;

    }

    public Bidder(String name) {
        this.name = name;
    }
    
    
    
}