package app;

class Electronics extends Item {

    @Override
    public String toString() {
        return "Electronics" + getName() + getPrice();
    }

    public Electronics(String id , String name , int price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }
    
    
    
}
