package main.messages;

public class PMSGMessage extends Message {
    private String username;
    private String privateMessage;

    public PMSGMessage(String content) {
        super("PMSG", content);

        String[] splitcontent = content.split(" ");
        username = splitcontent[0];
        privateMessage = "";
        for (int i = 1; i < splitcontent.length-1; i++) {
            privateMessage += splitcontent[i] +" ";
        }
        privateMessage += splitcontent[splitcontent.length-1];
    }

    public PMSGMessage(String username,String message) {
        super("PMSG", username + " " + message);

        this.username = username;
        this.privateMessage = message;
    }

    public String getUsername() {
        return username;
    }

    public String getPrivateMessage() {
        return privateMessage;
    }
}
