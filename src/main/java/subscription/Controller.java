// @@@SNIPSTART email-drip-campaign-java-send-email-constroller-for-services-headers
package subscription;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import subscription.model.*;
import subscription.workflows.SendEmailWorkflow;

@RestController
public class Controller {

    @Autowired
    WorkflowClient client;

// @@@SNIPEND
// @@@SNIPSTART email-drip-campaign-java-send-email-constroller-for-services-subscribe
    @PostMapping(value = "/subscribe", produces = MediaType.APPLICATION_JSON_VALUE)
    public Message startSubscription(@RequestBody WorkflowData data) {

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(data.getEmail())
                .setTaskQueue(Constants.TASK_QUEUE_NAME)
                .build();

        SendEmailWorkflow workflow = client.newWorkflowStub(SendEmailWorkflow.class, options);
        WorkflowClient.start(workflow::run,data);

        return new Message("Resource created successfully");
    }
// @@@SNIPEND

// @@@SNIPSTART email-drip-campaign-java-send-email-constroller-for-services-details
    @GetMapping(value = "/get_details", produces = MediaType.APPLICATION_JSON_VALUE)
    public EmailDetails getQuery(@RequestParam String email) {

        SendEmailWorkflow workflow = client.newWorkflowStub(SendEmailWorkflow.class, email);
        return workflow.details();
    }
// @@@SNIPEND

// @@@SNIPSTART email-drip-campaign-java-send-email-constroller-for-services-unsubscribe
    @DeleteMapping(value = "/unsubscribe", produces = MediaType.APPLICATION_JSON_VALUE)
    public Message endSubscription(@RequestBody WorkflowData data) {

        SendEmailWorkflow workflow = client.newWorkflowStub(SendEmailWorkflow.class, data.getEmail());
        WorkflowStub workflowStub = WorkflowStub.fromTyped(workflow);
        workflowStub.cancel();

        return new Message("Requesting cancellation");
    }
// @@@SNIPEND
}
