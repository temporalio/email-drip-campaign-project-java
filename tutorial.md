
#  Build an email subscription workflow with Temporal and Java

###  Introduction

In this tutorial, you'll build an email subscription web application using Temporal and Java. You'll create a web server using the Spring Boot framework to handle requests and use Temporal Workflows, Activities, and Queries to build the core of the application. Your web server will handle requests from the end user and interact with a Temporal Workflow to manage the email subscription process. Since you're building the business logic with Temporal's Workflows and Activities, you'll be able to use Temporal to manage each subscription rather than relying on a separate database or task queue. This reduces the complexity of the code you have to write and support.  

You'll create an endpoint for users to give their email address, and then create a new Workflow execution using that email address which will simulate sending an email message at certain intervals. The user can check on the status of their subscription, which you'll handle using a Query, and they can end the subscription at any time by unsubscribing, which you'll handle by cancelling the Workflow Execution. You can view the user's entire process through Temporal's Web UI. For this tutorial, you'll simulate sending emails, but you can adapt this example to call a live email service in the future.  

By the end of this tutorial, you'll have a clear understand how to use Temporal to create and manage long-running Workflows within a web application.  

You'll find the code for this tutorial on GitHub in the [email-subscription-project-java](https://github.com/SeanSullivan3/email-subscription-project-java) repository.  

##  Prerequisites

Before starting this tutorial:

-  [Set up a local development environment for Temporal and Java](https://learn.temporal.io/getting_started/java/dev_environment/).
-  Complete the [Hello World](https://learn.temporal.io/getting_started/java/hello_world_in_java/) tutorial to ensure you understand the basics of creating Workflows and Activities with Temporal.

### Gradle Build Configuration

In the example code, this application is built using Gradle. Make sure you have [Gradle](https://gradle.org/install/) installed and use [Spring Initializr](https://start.spring.io/) to generate a project with a `build.gradle` file for Java 17. Make sure to add the Spring Web and Lombok dependencies before you generate the project. Once you have a `build.gradle` file, add the temporal-sdk and temporal-spring-boot dependencies. Your `build.gradle` dependencies section should look like this:

[build.gradle](/build.gradle)
```gradle
ext {
    javaSDKVersion = '1.23.2'
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    implementation "io.temporal:temporal-spring-boot-starter-alpha:$javaSDKVersion"
}
```  

Next, create a file named `settings.gradle` in the root of your directory which contains the following line:

[settings.gradle](/settings.gradle)
```gradle
rootProject.name = 'email-subscription'
```
 With the gradle configurations complete, you can begin coding your srping boot web application.

##  Develop the Workflow

A Workflow defines a sequence of steps defined by writing code, known as a Workflow Definition, and are carried out by running that code, which results in a Workflow Execution.

The Temporal Java SDK recommends the use of a single class for parameters and return types. This lets you add fields without breaking compatibility. Before writing the Workflow Definition, you'll define the classes used by the Workflow Definitions. You'll also define a class to hold the Task Queue name you'll use when starting workflows and a class to hold a message that Spring Boot can return in json format through the web service.

Create a new package called `subscription` in the `src/main/java` directory. This package will contain all the code used for this application, so make sure to update the `group` variable in your `build.gradle` to equal `'subscription'`. Then, create another package called `model` in the `src/main/java/subscription` directory.

Add the following classes to the `src/main/java/subscription/model` directory:

-  `Constants.java`
-  `EmailDetails.java`
-  `Message.java`
-  `WorkflowData.java`  

1.  In `Constants.java` define a public string variable `TASK_QUEUE_NAME` and set it to `"email_subscription"`.
2.  In `Message.java`, `EmailDetails.java`, and `WorkflowData.java` use lombok to implement each class.

[Constants.java](/src/main/java/subscription/model/Constants.java)
```java
package EmailSubscription.model;

public class Constants {

    public static final String TASK_QUEUE_NAME = "email_subscription";
}
```

[Message.java](/src/main/java/subscription/model/Message.java)
```Java
package subscription.model;

import lombok.NoArgsConstructor
import lombok.AllArgsConstructor;
import lombok.Data;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Message {

    private String message;
}
```  

[EmailDetails.java](/src/main/java/subscription/model/EmailDetails.java)
```Java
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
```  

[WorkflowData.java](/src/main/java/subscription/model/WorkflowData.java)
```Java
package subscription.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class WorkflowData {

    private String email;
}

```  
  
The following describes each data class and their objects.

-  Message: this class holds message data to return via Spring Boot web service
    -  It will contain the following field:
        -  message: a string to pass the server response message after a request
     

-  EmailDetails: this class holds data about the current state of the subscription.
    -  It will contain the following field:
        -  email: as a string to pass a user's email
        -  message: as a string to pass a message to the user
        -  count: as an integer to track the number of emails sent
        -  subscribed: as a boolean to track whether the user is currently subscribed

-  WorkflowData: this class starts the Workflow Execution.
    -  It will contain the following field:
        -  email: a string to pass the user's email

When you Query your Workflow to retrieve the current statue of the Workflow, you'll use the `EmailDetails` class.

Now that you have the Task Queue and the data classes defined, you can write the Workflow Definition.

To create a new Workflow Definition, create a new package in the `src/main/java/subscription` directory called `workflows`. In the `src/main/java/subscription/workflows` directory, create a new file called `SendEmailWorkflow.java`. This file will be an interface to define your Workflow methods.

Create another file in the `src/main/java/subscription/workflows` directory called `SendEmailWorkflowImpl.java` that implements your `SendEmailWorkflow` interface. Then, write deterministic logic inside your Workflow method and execute the Activity.

Add the following code to `SendEmailWorkflow.java` to define the Workflow:

[SendEmailWorkflow.java](/src/main/java/subscription/workflows/SendEmailWorkflow.java)
```java
package subscription.workflows;

import subscription.model.EmailDetails;
import subscription.model.WorkflowData;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface SendEmailWorkflow {

    @WorkflowMethod
    public void run(WorkflowData data);
}
```

Add the following code to `SendEmailWorkflowImpl.java` to implement `SendEmailWorkflow`:

[SendEmailWorkflowImpl.java](/src/main/java/subscription/workflows/SendEmailWorkflowImpl.java)
```java
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
}
```

The interface `SendEmailWorkflow`, decorated with `@WorkflowInterface`, defines the `run()` method, decorated with `@WorkflowMethod`, which takes in the email address as an argument.  

In the implementation of the `SendEmailWorkflow` interface in the class `SendEmailWorkflowImpl`, the `run()` method initializes the email, message, subscribed, and count attributes of the private variable `emailDetails` for a `SendEmailWorkflowImpl` instance. The `SendEmailWorkflowImpl` class has a loop that checks if the subscription is active by checking if `emailDetails.isSubscribed()` is True. If it is, it starts the `sendEmail()` Activity.  

The while loop increments `emailDetails`'s count attribute and calls the `sendEmail()` Activity with the current `EmailDetails` object. The loop continues as long as `emailDetails`'s subscribed attribute is true.  

The `SendEmailActivities` object `activities` is used to call the activity method `sendEmail()`. `actvities` is initialized with the `SendEmailActivities` defintion and `ActivityOptions` that specify a start to close timeout of 10 seconds, which tells the Temporal Server to time out the Activity 10 seconds from when the Activity starts.  

The loop also includes a `Workflow.sleep()` statement that causes the Workflow to pause for a set amount of time between email. You can define this in seconds, days, months, or even years, depending on your business logic.  

If there's a cancellation request, the request throws a `CanceledFailure` error which you can catch and respond. In this application, you'll use cancellation requests to unsubscribe users. By utilizing a Detached Cancellation Scope, the workflow will be able to call the `sendEmail()` Activity to send one last email when they unsubscribe, before completing the Workflow Execution.  

Since the user's email address is set to the Workflow Id, attempting to subscribe with the same email address twice will result in a Workflow Execution already started error and prevent the Workflow Execution from spawning again.  

Therefore, only one running Workflow Execution per email address can exist within the associated Namespace. This ensures that the user won't receive multiple email subscriptions. This also helps reduce the complexity of the code you have to write and maintain.  

With this Workflow Definition in place, you can now develop an Activity to send emails.  

##  Develop an Activity

You'll need an Activity to send the email to the subscriber so you can handle failures.

Create a new package in the `src/main/java/subscription` directory called `activities`. In the `src/main/java/subscription/activities` directory, create a new file called `SendEmailActivities.java`. This file will be your Activity interface and will define your Activity.

Create another file in the `src/main/java/subscription/activities` directory called `SendEmailActivitiesImpl.java` to implement your Activity interface and wirite the logic of your Activity methods.

Add the following code to `SendEmailActivities.java`:

[SendEmailActivities.java](/src/main/java/subscription/activities/SendEmailActivities.java)
```java
package subscription.activities;

import subscription.model.EmailDetails;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface SendEmailActivities {

    @ActivityMethod
    public String sendEmail(EmailDetails details);
}
```

Add the following code to `SendEmailActivitiesImpl.java`:

[SendEmailActivitiesImpl.java](/src/main/java/subscription/activities/SendEmailActivitiesImpl.java)
```java
package subscription.activities;

import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;
import subscription.model.EmailDetails;

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
```

This implementation only prints a message, but you could replace the implementation with one that uses an email API.

Each iteration of the Workflow loop will execute this Activity, which simulates sending a message to the user.

Now that you have the Activity Definition and Workflow Definition, it's time to write the Worker process.

##  Create the Worker to handle the Workflow and Activity Executions

With the use of gradle and the Temporal Java SDK's [spring boot integration package](https://github.com/temporalio/sdk-java/tree/master/temporal-spring-boot-autoconfigure-alpha), you can write a Worker process for our Workflows and Activites without creating a dedicated Worker class. This simplifies the steps needed to run our Temporal Spring Boot application since your worker will be started automatically by running your Spring Boot application.

Create a new file in the `src/main/resources` directory called `application.yml` and provide the specifications of your application.

[application.yaml](/src/main/resources/application.yaml)
```yml
spring:
  application:
    name: email-subscription
  temporal:
    namespace: default
    connection:
      target: 127.0.0.1:7233
    workers:
      - name: send-email-worker
        task-queue: email_subscription
    workersAutoDiscovery:
      packages:
        - subscription.workflows
        - subscription.activities
```

Notice, specifiying `rootProject.name = 'email-subscription` in `settings.gradle` is necessary to link our `application.yml` file through the `application: name:` specification. Moreover, the `task-queue:` specification must be identical to the variable `TASK_QUEUE_NAME` in `Constants.java` and both the Workflows and Activities packages must be specified separately under the `packages:` specification.  

Lastly, for your Spring-integrated Worker to run your Workflows and Activities, you must use the `@WorkflowImpl(workers = "send-email-worker")` and `@ActivityImpl(workers = "send-email-worker")` decorations in your Workflow and Activity implementation classes respectively.  

Now that you've written the logic to execute the Workflow and Activity Definitions, try to build the gateway.

##  Build the API server to handle subscription requests

This tutorial uses the Spring Boot web framework to build a web server that acts as the entry point for initiating Workflow Execution and communicating with the subscribe, get-details, and unsubscribe routes. The web server will handle HTTP requests and perform the appropriate operations with the Workflow.

Create a new file in the `src/main/java/subscription` directory called `Controller.java` to develop your Spring Boot endpoints.

First, register the Temporal Client to run before the first request to this instance of the application. A Temporal Client enables you to communicate with the Temporal Cluster. Communication with a Temporal Cluster includes, but isn't limited to, the following:

-  Starting Workflow Executions
-  Sending Queries to Workflow Executions
-  Getting the results of a Workflow Execution

Add the following code to import your libraries and connect to the Temporal Server.

[Controller.java](/src/main/java/subscription/Controller.java)
```java
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
}
```

The `WorkflowClient` private variable `client` is initialized using Spring's `@Autowired` annotation which gets the Temporal WorkflowClient from our specifications in `application.yml`. You will use the `client` variable in each endpoint to handle starting, querying, and cancelling the Workflow.

Now that your connection to the Temporal Server is open, define your first Spring Boot endpoint.

First, build the `/subscribe` endpoint.

In the `Controller.java` file, define a `/subscribe` endpoint as function, so that users can subscribe to the emails.

[Starter.java](/src/main/java/subscription/Controller.java)
```java
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
```

In the `startSubscription()` function, use the `WorkflowClient` to start our Workflow Execution asyncronously. The `WorkflowData` object is used to pass the email address given by the user to the Workflow Execution and sets the Workflow Id in our `WorkflowOptions`. This ensures that the email is unique across all Workflows so that the user can't sign up multiple times, only receive the emails they've subscribed to, and when they cancel; they cancel the Workflow run.

With this endpoint in place, you can now send a `POST` request to `/subscribe` with an email address in the request body to start a new Workflow that sends an email to that address.

But how would you get details about the subscription? In the next section, you'll query your Workflow to get back information on the state of things in the next section.

##  Add a Query

Now create a method in which a user can get information about their subscription details. Define a new method called `details()` in the `SendEmailWorkflow` interface and use the `@QueryMethod` decorator.

To allow users to retrieve information about their subscription details, implement the `details()` Query method from the `SendEmailWorkflow` interface in the `SendEmailWorkflowImpl` class.

Add the following code to `SendEmailWorkflow.java`:

[SendEmailWorkflow.java](/src/main/java/subscription/workflows/SendEmailWorkflow.java)
```java
@QueryMethod
public EmailDetails details();
```

Add the following code to `SendEmailWorkflowImpl.java`:

[SendEmailWorkflowImpl.java](/src/main/java/subscription/workflows/SendEmailWorkflowImpl.java)
```java
@Override
public EmailDetails details() {

    return emailDetails;
}
```

The `emailDetails` object is an instance of `EmailDetails`. Queries can be used even if after the Workflow completes, which is useful for when the user unsubscribes but still wants to retrieve information about their subscription.

Queries should never mutate anything in the Workflow.

Now that you've added the ability to Query your Workflow, add the ability to Query from the Spring Boot application.

To enable users to query the Workflow from the Spring Boot application, add a new endpoint called `/get_details` to the `Controller.java` file.

Use the `client.newWorkflowStub()` function to return a `SendEmailWorkflow` object by a Workflow Id.

[Controller.java](/src/main/java/subscription/Controller.java)
```java
@GetMapping(value = "/get_details", produces = MediaType.APPLICATION_JSON_VALUE)
@ResponseBody
public EmailDetails getQuery(@RequestParam String email) {

    SendEmailWorkflow workflow = client.newWorkflowStub(SendEmailWorkflow.class, email);
    return workflow.details();
}
```

Using `client.newWorkflowStub()` retrieves a `SendEmailWorklfow` object that can be have the Query method called on it to get the value of the its variables. This object enables you to return all the information about the user's email subscription that's declared in the Workflow.

Now that users can subscribe and view the details of their subscription, you need to provide them with a way to unsubscribe.

##  Unsubscribe users with a Workflow Cancellation Request

Users will want to unsubscribe from the email list at some point, so give them a way to do that.

You cancel a Workflow by sending a cancellation request to the Workflow Execution. Your Workflow code can respond to this cancellation and perform additional operations in response. This is how you will handle unsubscribe requests.

With the `Controller.java` file open, add a new endpoint called `/unsubscribe` to the Spring Boot application.

To send a cancellation notice to an endpoint, use the HTTP `DELETE` method on the unsubscribe endpoint to instruct a given email's workflow to be cancelled.

[Controller.java](/src/main/java/subscription/Controller.java)
```java
@DeleteMapping(value = "/unsubscribe", produces = MediaType.APPLICATION_JSON_VALUE)
@ResponseBody
public Message endSubsciption(@RequestBody WorkflowData data) {

    SendEmailWorkflow workflow = client.newWorkflowStub(SendEmailWorkflow.class, data.getEmail());
    WorkflowStub workflowStub = WorkflowStub.fromTyped(workflow);
    workflowStub.cancel();

    return new Message("Requesting cancellation");
}
```
The `workflowStub.cancel()` method sends a cancellation request to the Workflow Execution that was started with the `/subscribe` endpoint.

When the Temporal Service receives the cancellation request, it will cancel the Workflow Execution and throw a `CanceledFailure` error to the Workflow Execution, which your Workflow Definition already handles in the try/catch block. Here's the relevant section as a reminder:

[SendEmailWorkflowImpl.java](/src/main/java/subscription/workflows/SendEmailWorkflowImpl.java)
```java
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
```

With this endpoint in place, users can send a `DELETE` request to `/unsubscribe` with an email address in the request body to cancel the Workflow associated with that email address. This allows users to unsubscribe from the email list and prevent any further emails from sending.

Now that you've added the ability to unsubscribe from the email list, your API server is complete, and you only need to add a class to run the server.  

In the `src/main/java/subscription` directory, create a class called `Starter.java` to run your Spring Boot application. In `Starter.java`, add the following:

[Starter.java](/src/main/java/subscription/Starter.java)
```java
package subscription;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Starter {

    public static void main(String[] args) {

        SpringApplication.run(Starter.class, args);
    }
}
```  

Next, test your application code to ensure it works as expected.

##  Create an Integration Test

Integration testing is an essential part of software development that helps ensure that different components of an application work together correctly.

The Temporal Java SDK includes classes and methods that help you test your Workflow Executions.

Workflow testing can be done in an integration test fashion against a test server or from a given Client.

In this section, you'll write an integration test using the Temporal Java SDK to test the cancellation of a Workflow. Now, you can add tests to the application to ensure the Cancellation works as expected.

To set up the test environment, create a file in the `src/test/java` directory called `StarterTest.java`

The Temporal Java SDK includes classes and methods that help you test your Workflow Executions. In this section, you will import the necessary modules and classes to test the cancellation of a Workflow.

In this code, you are defining two test functions `testCreateEmail()` and `testCancelWorkflow()` that use the Temporal SDK to create and cancel a Workflow Execution.

[StarterTest.java](/src/test/java/StarterTest.java)
```java
import EmailSubscription.SendEmailActivitiesImpl;
import EmailSubscription.SendEmailWorkflow;
import EmailSubscription.SendEmailWorkflowImpl;
import EmailSubscription.model.WorkflowData;
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
```

The `testCreateEmail()` function creates a Workflow Execution by starting the `SendEmailWorkflow` with some test data. The function then asserts that the status of the Workflow Execution is `RUNNING`.

The `testCancelWorkflow()` function also starts a Workflow Execution, but it then immediately cancels it using the `cancel()` method on the `WorkflowStub`. It then enters a while loop to repeatedly get the status of the Workflow Execution until the status is returned as `CANCELED`.

Now that you've created a test function for the Workflow Cancellation, run the test to see if that works.

To test the function, run `./gradlew test --info` from the command line to execute your tests and view the info on your test's execution. Important to note, gradle will automatically run your tests without displaying execution information everytime you run `gradle build`, so you can use this command as an alternative if you are only interested in the successful result of the tests.

You've successfully written, executed, and passed a Cancellation Workflow test, just as you would any other code written in Java. Temporal's Java SDK provides a number of classes and methods that help you test your Workflow Executions. By following the best practices for testing your code, you can be confident that your Workflows are reliable and performant.

##  Conclusion

This tutorial demonstrates how to build an email subscription web application using Temporal, Java, Spring Boot, and Gradle. By leveraging Temporal's Workflows, Activities, and Queries, the tutorial shows how to create a web server that interacts with Temporal to manage the email subscription process.

With this knowledge, you will be able to take on more complex Workflows and Activities to create even stronger applications.

