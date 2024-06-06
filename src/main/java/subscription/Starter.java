package subscription;

import io.temporal.serviceclient.WorkflowServiceStubs;
import subscription.model.*;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import subscription.workflows.SendEmailWorkflow;

@RestController
@SpringBootApplication
public class Starter {

    private static WorkflowClient client;

    public static void main(String[] args) {

        SpringApplication.run(Starter.class, args);

        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        client = WorkflowClient.newInstance(service);
    }

    @PostMapping(value = "/subscribe", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Message startSubscription(@RequestBody WorkflowData data) {

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(data.getEmail())
                .setTaskQueue(Constants.TASK_QUEUE_NAME)
                .build();

        SendEmailWorkflow workflow = client.newWorkflowStub(SendEmailWorkflow.class, options);
        WorkflowClient.start(workflow::run,data);

        return new Message("Resource created successfully");
    }

    @GetMapping(value = "/get_details", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public EmailDetails getQuery(@RequestParam String email) {

        SendEmailWorkflow workflow = client.newWorkflowStub(SendEmailWorkflow.class, email);
        return workflow.details();
    }

    @DeleteMapping(value = "/unsubscribe", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Message endSubsciption(@RequestBody WorkflowData data) {

        SendEmailWorkflow workflow = client.newWorkflowStub(SendEmailWorkflow.class, data.getEmail());
        WorkflowStub workflowStub = WorkflowStub.fromTyped(workflow);
        workflowStub.cancel();

        return new Message("Requesting cancellation");
    }
}
