import subscription.activities.SendEmailActivitiesImpl;
import subscription.workflows.SendEmailWorkflow;
import subscription.workflows.SendEmailWorkflowImpl;
import subscription.model.WorkflowData;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class StarterTest {

    @RegisterExtension
    public static final TestWorkflowExtension testWorkflowExtension =
            TestWorkflowExtension.newBuilder()
                    .setWorkflowTypes(SendEmailWorkflowImpl.class)
                    .setDoNotStart(true)
                    .build();

    @Test
    public void testCreateEmail(TestWorkflowEnvironment testEnv, Worker worker, SendEmailWorkflow workflow) {

        WorkflowClient client = testEnv.getWorkflowClient();
        worker.registerActivitiesImplementations(new SendEmailActivitiesImpl());
        testEnv.start();

        WorkflowData data = new WorkflowData("test@example.com");

        WorkflowExecution execution = WorkflowClient.start(workflow::run,data);

        DescribeWorkflowExecutionResponse response = client.getWorkflowServiceStubs().blockingStub().describeWorkflowExecution(
                DescribeWorkflowExecutionRequest.newBuilder()
                        .setNamespace(testEnv.getNamespace())
                        .setExecution(execution)
                        .build()
        );

        WorkflowExecutionStatus status = response.getWorkflowExecutionInfo().getStatus();

        assertEquals(WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING, status);
        testEnv.close();
    }

    @Test
    public void testCancelWorkflow (TestWorkflowEnvironment testEnv, Worker worker, SendEmailWorkflow workflow) {

        WorkflowClient client = testEnv.getWorkflowClient();
        worker.registerActivitiesImplementations(new SendEmailActivitiesImpl());
        testEnv.start();

        WorkflowData data = new WorkflowData("test@example.com");

        WorkflowExecution execution = WorkflowClient.start(workflow::run,data);

        WorkflowStub workflowStub = client.newUntypedWorkflowStub(execution.getWorkflowId());
        workflowStub.cancel();

        DescribeWorkflowExecutionResponse response;
        WorkflowExecutionStatus status;
        do {
             response = client.getWorkflowServiceStubs().blockingStub().describeWorkflowExecution(
                    DescribeWorkflowExecutionRequest.newBuilder()
                            .setNamespace(testEnv.getNamespace())
                            .setExecution(execution)
                            .build()
            );

             status = response.getWorkflowExecutionInfo().getStatus();
        }
        while (status != WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_CANCELED);

        assertEquals(WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_CANCELED, status);
        testEnv.close();
    }
}
