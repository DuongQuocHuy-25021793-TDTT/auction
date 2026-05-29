package app.model;

public class Electronics extends Item {
    private int warrantyMonths;
    private String condition;
    private String purchaseDate;
    private String isRepaired;
    private String repairDate;
    private String repairedParts;

    public Electronics(String id, String name, String description, double startingPrice, int warrantyMonths, String condition, String purchaseDate, String isRepaired, String repairDate, String repairedParts) {
        super(id, name, description, startingPrice);
        this.warrantyMonths = warrantyMonths;
        this.condition = condition;
        this.purchaseDate = purchaseDate;
        this.isRepaired = isRepaired;
        this.repairDate = repairDate;
        this.repairedParts = repairedParts;
    }

    public Electronics(String id, String name, String description, double startingPrice, int warrantyMonths) {
        super(id, name, description, startingPrice);
        this.warrantyMonths = warrantyMonths;
        this.condition = "Mới";
        this.purchaseDate = "";
        this.isRepaired = "Không";
        this.repairDate = "";
        this.repairedParts = "";
    }


    public int getWarrantyMonths() {
        return warrantyMonths;
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
    public void printInfo(){
        String base = "Đồ điện tử: " + name + " - Bảo hành: " + warrantyMonths + " Tháng - Tình trạng: " + condition;
        if ("Cũ".equals(condition)) {
            base += " - Ngày mua: " + purchaseDate;
            if ("Có".equals(isRepaired)) {
                base += " - Ngày sửa: " + repairDate + " - Phụ tùng: " + repairedParts;
            }
        }
        System.out.println(base);
    }
}
