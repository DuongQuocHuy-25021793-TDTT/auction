package app;

class Art extends Item {

    @Override
    public String toString() {
        return "Art" + getName() + getPrice();
    }

    public Art(String id , String name , int price) {
        this.id = id;
        this.name = name;
        this.price = price;
        
    }
    
    
}
