// @@@SNIPSTART email-drip-campaign-java-send-email-activities-implementation
package subscription.activities;

import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;
import subscription.model.EmailDetails;
import java.text.MessageFormat;

@Component
@ActivityImpl(workers = "send-email-worker")
public class SendEmailActivitiesImpl implements SendEmailActivities {
    @Override
    public String sendEmail(EmailDetails details) {
        String response = MessageFormat.format(
            "Sending email to {0} with message: {1}, count: {2}",
            details.email, details.message, details.count);
        System.out.println(response);
        return "success";
    }
}
// @@@SNIPEND
