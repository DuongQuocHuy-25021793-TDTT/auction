package app.model;

import java.util.ArrayList;
import java.util.List;

public class Seller extends User {
    private final List<Item> myItems;

    public Seller(String id, String username, String password) {
        super(id, username, password);
        this.myItems = new ArrayList<>();
    }

    public void addItem(Item item) {
        this.myItems.add(item);
        System.out.println("Seller " + this.username + " đã thêm sản phẩm: " + item.getName());
    }

    public void removeItem(Item item) {
        this.myItems.remove(item);
        System.out.println("Đã xóa sản phẩm: " + item.getName());
    }

    public List<Item> getMyItems() {
        return myItems;
    }

    @Override
    public AccountRole getRole() {
        return AccountRole.SELLER;
    }
}
