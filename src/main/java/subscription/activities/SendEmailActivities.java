package subscription.activities;

import subscription.model.EmailDetails;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface SendEmailActivities {

    @ActivityMethod
    public String sendEmail(EmailDetails details);
}
