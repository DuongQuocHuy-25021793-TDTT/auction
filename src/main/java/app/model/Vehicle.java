package app.model;

public class Vehicle extends Item {
    private String brand;
    private int mileage;
    private String condition;
    private String purchaseDate;
    private String isRepaired;
    private String repairDate;
    private String repairedParts;

    public Vehicle(String id, String name, String description, double startingPrice, String brand, int mileage, String condition, String purchaseDate, String isRepaired, String repairDate, String repairedParts) {
        super(id, name, description, startingPrice);
        this.brand = brand;
        this.mileage = mileage;
        this.condition = condition;
        this.purchaseDate = purchaseDate;
        this.isRepaired = isRepaired;
        this.repairDate = repairDate;
        this.repairedParts = repairedParts;
    }

    public String getBrand() {
        return brand;
    }

    public int getMileage() {
        return mileage;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(String purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public String getIsRepaired() {
        return isRepaired;
    }

    public void setIsRepaired(String isRepaired) {
        this.isRepaired = isRepaired;
    }

    public String getRepairDate() {
        return repairDate;
    }

    public void setRepairDate(String repairDate) {
        this.repairDate = repairDate;
    }

    public String getRepairedParts() {
        return repairedParts;
    }

    public void setRepairedParts(String repairedParts) {
        this.repairedParts = repairedParts;
    }

    @Override
    public void printInfo() {
        String base = "Phương tiện: " + name + " - Hãng: " + brand + " - Tình trạng: " + condition;
        if ("Cũ".equals(condition)) {
            base += " - Ngày mua: " + purchaseDate + " - Số km: " + mileage;
            if ("Có".equals(isRepaired)) {
                base += " - Ngày sửa: " + repairDate + " - Phụ tùng: " + repairedParts;
            }
        }
        System.out.println(base);
    }
}
