package app.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class AuctionProposalRequest {
    public static final String FLEXIBLE_SCHEDULE_NOTE = "Nếu để trống sẽ được xếp ưu tiên sau các sản phẩm đã được đặt lịch trước.";

    // Thông tin cá nhân không được sửa
    private String bidderId;
    private String bidderUsername;

    //Thông tin phiên sản phẩm muốn đấu giá
    private String productType;
    private String productName;
    private String productDescription;
    private Map<String, String> productAttributes; //Các thuộc tính riêng của các loại sản phẩm
    private double desiredPrice; // Số tiền mong muốn

    //Thời gian có thể để trống nếu Bidder không yêu cầu
    private long requestedDurationMinutes;
    private LocalDateTime requestedStartTime;

    private boolean flexibleSchedule;
    private String scheduleNote;

    public AuctionProposalRequest(String bidderId,
                                  String bidderUsername,
                                  String productType,
                                  String productName,
                                  String productDescription,
                                  Map<String, String> productAttributes,
                                  double desiredPrice,
                                  long requestedDurationMinutes,
                                  LocalDateTime requestedStartTime) {
        this.bidderId = bidderId;
        this.bidderUsername = bidderUsername;
        this.productType = productType;
        this.productName = productName;
        this.productDescription = productDescription;
        this.productAttributes = productAttributes != null ? productAttributes : new HashMap<>();
        this.desiredPrice = desiredPrice;
        this.requestedDurationMinutes = requestedDurationMinutes;
        this.requestedStartTime = requestedStartTime;
        refreshScheduleFlag();
    }

    public String getBidderId() {
        return bidderId;
    } // Chỉ lấy thông tin cá nhân
    public String getBidderUsername() { return bidderUsername; }

    public String getProductType() {
        return productType;
    }
    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getProductName() {return productName;}
    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductDescription() {
        return productDescription;
    }
    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public Map<String, String> getProductAttributes() {
        return productAttributes;
    }
    public void setProductAttributes(Map<String, String> productAttributes) {
        this.productAttributes = productAttributes;
    }

    public double getDesiredPrice() {
        return desiredPrice;
    }
    public void setDesiredPrice(double desiredPrice) {
        this.desiredPrice = desiredPrice;
    }

    public long getRequestedDurationMinutes() {
        return requestedDurationMinutes;
    }
    public void setRequestedDurationMinutes(long requestedDurationMinutes) {
        this.requestedDurationMinutes = requestedDurationMinutes;
    }

    public LocalDateTime getRequestedStartTime() {
        return requestedStartTime;
    }
    public void setRequestedStartTime(LocalDateTime requestedStartTime) {
        this.requestedStartTime = requestedStartTime;
        refreshScheduleFlag();
    }

    public boolean isFlexibleSchedule() {
        return flexibleSchedule;
    }
    public String getScheduleNote() {return scheduleNote;}

    private void refreshScheduleFlag() {
        this.flexibleSchedule = (requestedStartTime == null) || (requestedDurationMinutes <= 0);
        this.scheduleNote = this.flexibleSchedule ? FLEXIBLE_SCHEDULE_NOTE : "";
    }
}
