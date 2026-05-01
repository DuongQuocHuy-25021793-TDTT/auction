package app.model;

public class Art extends Item {
    private String artist;
    private int creationYear;
    public Art(String id, String name, String description, double startingPrice, String artist, int creationYear) {
        super(id, name, description, startingPrice);
        this.artist = artist;
        this.creationYear = creationYear;
    }
    
    public String getArtist() {
        return artist;
    }

    public int getCreationYear() {
        return creationYear;
    }

    @Override
    public void printInfo(){
        System.out.println("Tác phẩm nghệ thuật: " + name + " - Tác giả: " + artist + " (" + creationYear + ")");
    }
    
}
