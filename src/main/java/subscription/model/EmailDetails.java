// @@@SNIPSTART email-drip-campaign-java-send-email-subscriber-details-data-class
package subscription.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class EmailDetails {

    private String email;
    private String message;
    private int count;
    private boolean subscribed;

    public void increment() {
        count++;
    }
}
// @@@SNIPEND
