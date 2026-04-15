package app;

class Vehicle extends Item {

    @Override
    public String toString() {
        return "Vehicle" + getName() + getPrice();
    }

    public Vehicle(String id , String name , int price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }
    
    
    
}
