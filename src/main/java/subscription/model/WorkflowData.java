// @@@SNIPSTART email-drip-campaign-java-send-email-workflow-data-class
package subscription.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
public class WorkflowData {
    public String email;

    public WorkflowData() {}

    public WorkflowData(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
// @@@SNIPEND
