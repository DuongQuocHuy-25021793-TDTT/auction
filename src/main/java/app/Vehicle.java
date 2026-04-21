package app;
public class Vehicle extends Item {
    private String  brand;
    private int mileage;
    public Vehicle(String id, String name, String description, double startingPrice, String brand, int mileage) {
        super(id, name, description, startingPrice);
        this.brand = brand;
        this.mileage = mileage;
    }
    @Override
    public void printInfo() {
        System.out.println("Phương tiện: " + name + " - Hãng: " + brand + " - Số km: " + mileage);
    }
}
