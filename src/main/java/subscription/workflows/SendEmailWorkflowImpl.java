package subscription.workflows;

import io.temporal.spring.boot.WorkflowImpl;
import subscription.activities.SendEmailActivities;
import subscription.model.EmailDetails;
import subscription.model.WorkflowData;
import io.temporal.activity.ActivityOptions;
import io.temporal.failure.CanceledFailure;
import io.temporal.workflow.CancellationScope;
import io.temporal.workflow.Workflow;

import java.time.Duration;

// @@@SNIPSTART email-drip-campaign-java-send-email-subscription-workflow-implementation
@WorkflowImpl(workers = "send-email-worker")
public class SendEmailWorkflowImpl implements SendEmailWorkflow {

    private EmailDetails emailDetails = new EmailDetails();

    private final ActivityOptions options =
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .build();

    private final SendEmailActivities activities =
            Workflow.newActivityStub(SendEmailActivities.class, options);

    @Override
    public void run(WorkflowData data) {

        int duration = 12;
        emailDetails.setEmail(data.getEmail());
        emailDetails.setMessage("Welcome to our Subscription Workflow!");
        emailDetails.setSubscribed(true);
        emailDetails.setCount(0);

        while (emailDetails.isSubscribed()) {

            emailDetails.increment();
            if (emailDetails.getCount() > 1) {
                emailDetails.setMessage("Thank you for staying subscribed!");
            }

            try {
                activities.sendEmail(emailDetails);
                Workflow.sleep(Duration.ofSeconds(duration));
            }
            catch (CanceledFailure e) {
                emailDetails.setSubscribed(false);
                emailDetails.setMessage("Sorry to see you go");
                CancellationScope sendGoodbye =
                        Workflow.newDetachedCancellationScope(() -> activities.sendEmail(emailDetails));
                sendGoodbye.run();

                throw e;
            }
        }
    }

    @Override
    public EmailDetails details() {

        return emailDetails;
    }
}
// @@@SNIPEND
