// @@@SNIPSTART email-drip-campaign-java-send-email-subscription-workflow-implementation
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
        emailDetails.email = data.email;
        emailDetails.message = "Welcome to our Subscription Workflow!";
        emailDetails.subscribed = true;
        emailDetails.count = 0;

        while (emailDetails.subscribed) {

            emailDetails.count += 1;
            if (emailDetails.count > 1) {
                emailDetails.message = "Thank you for staying subscribed!";
            }

            try {
                activities.sendEmail(emailDetails);
                Workflow.sleep(Duration.ofSeconds(duration));
            }
            catch (CanceledFailure e) {
                emailDetails.subscribed = false;
                emailDetails.message = "Sorry to see you go";
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
