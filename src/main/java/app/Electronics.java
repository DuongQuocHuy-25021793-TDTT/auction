package app;

public class Electronics extends Item {
    private int warrantyMonths;

    public Electronics(String id, String name, String description, double startingPrice, int warrantyMonths) {
        super(id, name, description, startingPrice);
        this.warrantyMonths = warrantyMonths;
    }
    @Override
    public void printInfo(){
        System.out.println("Đồ điện tử: " + name + " - Bảo hành: " + warrantyMonths + "Tháng");
    }
    
    
}
