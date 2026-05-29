package app.model;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuctionTest {

    private Auction auction;
    private Item item;

    @BeforeEach
    public void setUp() {
        
        item = new Art("I_TEST", "Tranh Hoa Hướng Dương", "Tranh sơn dầu", 1000.0, "Van Gogh", 1888);
        auction = new Auction("A_TEST", item, 1000.0);
    }

    @Test
    public void testValidateBid_Success() {
        
        BidTransaction validBid = new BidTransaction("BID_01", "A_TEST", "UserA", 1500.0, LocalDateTime.now(), "test");
        
        assertNull(auction.validateBid(validBid), "Giá 1500 phải được chấp nhận vì lớn hơn 1000");
    }

    @Test
    public void testValidateBid_TooLow() {
   
        BidTransaction lowBid = new BidTransaction("BID_02", "A_TEST", "UserB", 900.0, LocalDateTime.now(),"test");
     
        assertEquals("TOO_LOW", auction.validateBid(lowBid), "Phải báo lỗi TOO_LOW khi đặt giá thấp hơn giá khởi điểm");
    }

    @Test
    public void testValidateBid_InvalidAmount() {
        BidTransaction negativeBid = new BidTransaction("BID_03", "A_TEST", "UserC", -500.0, LocalDateTime.now(),"test");
      
        assertEquals("INVALID_AMOUNT", auction.validateBid(negativeBid), "Phải báo lỗi INVALID_AMOUNT khi giá trị âm");
    }

    @Test
    public void testPlaceBid_UpdatesHighestPrice() {
    
        BidTransaction newBid = new BidTransaction("BID_04", "A_TEST", "UserD", 2500.0, LocalDateTime.now(),"test");
        
        boolean isSuccess = auction.placeBid(newBid);
        
        assertTrue(isSuccess, "Hàm placeBid phải trả về true");
        assertEquals(2500.0, auction.getCurrentHighestPrice(), "Giá cao nhất của phiên phải được cập nhật lên 2500");
        assertEquals("UserD", auction.getHighestBidderId(), "Người trả giá cao nhất phải là UserD");
    }
}
