
# Simple Java Email Subscription Workflow with Temporal

This project is a Java translation of Temporal's project-based tutorial for [an email subscription Workflow with Temporal and Python](https://learn.temporal.io/tutorials/python/subscriptions/).
It recreates Temporal's Python and Flask "Build an email subscription workflow" tutorial to reflect the process of building this application in Java. 
This GitHub repo includes instructions for running the application and the Java application code using the Temporal Java SDK and Spring Boot.

[Read the tutorial](http://learn.temporal.io/tutorials/java/subscriptions/) and follow along on the Temporal learn site.

Ported to Java by [Sean Sullivan](https://github.com/SeanSullivan3), Summer 2024.

## Run

1. Open a terminal and start the temporal server.
```bash
temporal server start-dev
```

2. In second terminal, compile the project and start the Spring Boot application.
```bash
gradle build
./gradlew bootRun
```

3. In a third terminal, run the curl commands below.
    -  Check your second terminal for subscription updates.
    -  Check `http://localhost:8233/` for the temporal web UI's workflow details.

## Curl commands

### Subscribe

Use the curl command to send a POST request to `http://localhost:8080/subscribe` with the email address as a JSON payload.

```bash
curl -X POST -H "Content-Type: application/json" -d '{"email": "example@example.com"}' http://localhost:8080/subscribe
```

### Get Details

The email address should be included in the query string parameter of the URL.

```bash
curl -X GET -H "Content-Type: application/json" 'http://localhost:8080/get_details?email=example@example.com'

```

### Unsubscribe

Send a DELETE request with the email address in a JSON payload.

```bash
curl -X DELETE -H "Content-Type: application/json" -d '{"email": "example@example.com"}' http://localhost:8080/unsubscribe
```
