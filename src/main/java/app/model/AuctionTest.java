package app.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

public class AuctionTest {

    private Auction auction;
    private Item item;

    @BeforeEach
    public void setUp() {
        // Hàm này tự động chạy trước mỗi bài test để tạo ra một phiên đấu giá mẫu
        item = new Art("I_TEST", "Tranh Hoa Hướng Dương", "Tranh sơn dầu", 1000.0, "Van Gogh", 1888);
        auction = new Auction("A_TEST", item, 1000.0);
    }

    @Test
    public void testValidateBid_Success() {
        // Kịch bản 1: Người dùng đặt giá hợp lệ (1500 > 1000)
        BidTransaction validBid = new BidTransaction("BID_01", "A_TEST", "UserA", 1500.0, LocalDateTime.now());
        
        // Trả về null nghĩa là không có lỗi (Hợp lệ)
        assertNull(auction.validateBid(validBid), "Giá 1500 phải được chấp nhận vì lớn hơn 1000");
    }

    @Test
    public void testValidateBid_TooLow() {
        // Kịch bản 2: Người dùng đặt giá thấp hơn giá hiện tại (900 < 1000)
        BidTransaction lowBid = new BidTransaction("BID_02", "A_TEST", "UserB", 900.0, LocalDateTime.now());
        
        // Phải trả về lỗi "TOO_LOW"
        assertEquals("TOO_LOW", auction.validateBid(lowBid), "Phải báo lỗi TOO_LOW khi đặt giá thấp hơn giá khởi điểm");
    }

    @Test
    public void testValidateBid_InvalidAmount() {
        // Kịch bản 3: Người dùng cố tình hack hệ thống, đặt giá âm (-500)
        BidTransaction negativeBid = new BidTransaction("BID_03", "A_TEST", "UserC", -500.0, LocalDateTime.now());
      
        assertEquals("INVALID_AMOUNT", auction.validateBid(negativeBid), "Phải báo lỗi INVALID_AMOUNT khi giá trị âm");
    }

    @Test
    public void testPlaceBid_UpdatesHighestPrice() {
    
        BidTransaction newBid = new BidTransaction("BID_04", "A_TEST", "UserD", 2500.0, LocalDateTime.now());
        
        boolean isSuccess = auction.placeBid(newBid);
        
        assertTrue(isSuccess, "Hàm placeBid phải trả về true");
        assertEquals(2500.0, auction.getCurrentHighestPrice(), "Giá cao nhất của phiên phải được cập nhật lên 2500");
        assertEquals("UserD", auction.getHighestBidderId(), "Người trả giá cao nhất phải là UserD");
    }
}