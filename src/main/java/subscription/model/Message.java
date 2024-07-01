// @@@SNIPSTART email-drip-campaign-java-send-email-message-data-class
package subscription.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
public class Message {
    public String message;

    public Message() {}

    public Message(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
// @@@SNIPEND
