package app.model;

public class Message {
    private String action;
    private String data;

    public Message(String action, String data) {
        this.action = action;
        this.data = data;
    }

    public String getAction() {
        return action;
    }

    public String getData() {
        return data;
    }


    public String getCommand() {
        return action; 
    }

    
    public void setAction(String action) {
        this.action = action;
    }

    public void setData(String data) {
        this.data = data;
    }
}