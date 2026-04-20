package app.models;


public abstract class Item extends Entity {
    protected  String name ;
    protected  int price;
    public String getName() {
        return name;
    }
    public int getPrice() {
        return price;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setPrice(int price) {
        this.price = price;
    }
    
}