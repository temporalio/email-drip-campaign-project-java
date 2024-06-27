
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;
import subscription.model.EmailDetails;

// @@@SNIPSTART email-drip-campaign-java-send-email-activities-implementation
package subscription.activities;
@Component
@ActivityImpl(workers = "send-email-worker")
public class SendEmailActivitiesImpl implements SendEmailActivities {

    @Override
    public String sendEmail(EmailDetails details) {

        System.out.println( "Sending email to " + details.getEmail() +
                            " with message: " + details.getMessage() +
                            ", count: " + details.getCount());

        return "success";
    }
}
// @@@SNIPEND
