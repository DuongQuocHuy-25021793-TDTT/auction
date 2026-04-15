package app;

class Seller extends User {
    @Override
        public String toString() {
            return "Seller" + getName();

    }
    public Seller(String name) {
        this.name = name;
    }

    



    
}
