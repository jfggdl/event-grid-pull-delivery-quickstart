package com.azure.messaging.eventgrid.samples;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.models.CloudEvent;
import com.azure.messaging.eventgrid.EventGridClient;
import com.azure.messaging.eventgrid.EventGridClientBuilder;
import com.azure.messaging.eventgrid.EventGridMessagingServiceVersion;
import com.azure.messaging.eventgrid.models.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Simple demo consumer app of CloudEvents from queue event subscriptions created for namespace topics.
 * This code samples should use Java 1.8 level or above to avoid compilation errors.
 * You should consult the resources below to use the client SDK and set up your project using maven.
 * @see <a href="https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/eventgrid/azure-messaging-eventgrid">Event Grid data plane client SDK documentation</a>
 * @see <a href="https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/boms/azure-sdk-bom/README.md">Azure BOM for client libraries</a>
 * @see <a href="https://aka.ms/spring/versions">Spring Version Mapping</a> if you are using Spring.
 * @see <a href="https://aka.ms/azsdk">Tool with links to control plane and data plane SDKs across all languages supported</a>.
 *</p>
 */
public class NamespaceTopicConsumer {
    private static final String TOPIC_NAME = "jfgTpc1";
    public static final String EVENT_SUBSCRIPTION_NAME = "evtsub1";
    public static final String ENDPOINT = "https://jfgegns1.westus2-1.eventgrid.azure.net";
    public static final int MAX_NUMBER_OF_EVENTS_TO_RECEIVE = 10;
    public static final Duration MAX_WAIT_TIME_FOR_EVENTS = Duration.ofSeconds(10);

    private static EventGridClient eventGridClient;
    private static List<String> receivedCloudEventLockTokens = new ArrayList<>();
    private static List<CloudEvent> receivedCloudEvents = new ArrayList<>();

    //TODO  Do NOT include keys in source code. This code's objective is to give you a succinct sample about using Event Grid, not to provide an authoritative example for handling secrets in applications.
    /**
     * For security concerns, you should not have keys or any other secret in any part of the application code.
     * You should use services like Azure Key Vault for managing your keys.
     */
    public static final AzureKeyCredential CREDENTIAL = new AzureKeyCredential("mlIlp3DnJ/0ZDlkPXmhL7WuKIVdrqZ4UkYNp+FJ4mWE=");
    public static void main(String[] args) {
        //TODO Update Event Grid version number to your desired version. You can find more information on data plane APIs here:
        //https://learn.microsoft.com/en-us/rest/api/eventgrid/.
        eventGridClient = new EventGridClientBuilder()
                .httpClient(HttpClient.createDefault())  // Requires Java 1.8 level
                .endpoint(ENDPOINT)
                .serviceVersion(EventGridMessagingServiceVersion.V2023_06_01_PREVIEW)
                .credential(CREDENTIAL).buildClient();   // you may want to use .buildAsyncClient() for an asynchronous (project reactor) client.

        System.out.println("Waiting " +  MAX_WAIT_TIME_FOR_EVENTS.toSecondsPart() + " seconds for events to be read...");
        List<ReceiveDetails> receiveDetails = eventGridClient.receiveCloudEvents(TOPIC_NAME, EVENT_SUBSCRIPTION_NAME,
                MAX_NUMBER_OF_EVENTS_TO_RECEIVE, MAX_WAIT_TIME_FOR_EVENTS).getValue();

        for (ReceiveDetails detail : receiveDetails) {
            // Add order message received to a tracking list
            CloudEvent orderCloudEvent = detail.getEvent();
            receivedCloudEvents.add(orderCloudEvent);
            // Add lock token to a tracking list. Lock token functions like an identifier to a cloudEvent
            BrokerProperties metadataForCloudEventReceived = detail.getBrokerProperties();
            String lockToken = metadataForCloudEventReceived.getLockToken();
            receivedCloudEventLockTokens.add(lockToken);
        }
        System.out.println("<-- Number of events received: " + receivedCloudEvents.size());

        // Acknowledge (i.e. delete from Event Grid the) events
        acknowledge(receivedCloudEventLockTokens);

//        // Next: alternative operations to *acknowledge*.
//
//        // For simplicity, all lock tokens are passed. In production environments,
//        // you would only pass specific lock tokens that need to be renewed or the
//        // lock tokens associated to events that you need to be released or rejected.
//
//        // Release (back to Event Grid) events that your app is not ready to process.
//        release(receivedCloudEventLockTokens);
//
//        // Reject events that cannot be processed by your app (message malformed or applicaiton problem).
//        // Rejected events are dead-lettered, if any configured. Otherwise, events are dropped.
//        reject(receivedCloudEventLockTokens);
//
//        // Renew lock tokens so that your client app has more time to process the event.
//        // The lock token is extended using the number of seconds set in the
//        // event subscription's receiveLockDurationInSeconds property.
//        renewLockToken(receivedCloudEventLockTokens);
    }

    private static void acknowledge(List<String> lockTokens) {
        AcknowledgeResult acknowledgeResult = eventGridClient.acknowledgeCloudEvents(TOPIC_NAME, EVENT_SUBSCRIPTION_NAME, new AcknowledgeOptions(lockTokens));
        List<String> succeededLockTokens = acknowledgeResult.getSucceededLockTokens();
        if (succeededLockTokens != null && lockTokens.size() >= 1)
            System.out.println("@@@ " + succeededLockTokens.size() + " events were successfully acknowledged:");
        for (String lockToken : succeededLockTokens) {
            System.out.println("    Acknowledged event lock token: " + lockToken);
        }
        // Print the information about failed lock tokens
        if (succeededLockTokens.size() < lockTokens.size()) {
            System.out.println("    At least one event was not acknowledged (deleted from Event Grid)");
            writeFailedLockTokens(acknowledgeResult.getFailedLockTokens());
        }
    }
    private static void release(List<String> lockTokens) {
        ReleaseResult releaseResult = eventGridClient.releaseCloudEvents(TOPIC_NAME, EVENT_SUBSCRIPTION_NAME, new ReleaseOptions(lockTokens));
        List<String> succeededLockTokens = releaseResult.getSucceededLockTokens();
        if (succeededLockTokens != null && lockTokens.size() >= 1)
            System.out.println("^^^ " + succeededLockTokens.size() + " events were successfully released:");
        for (String lockToken : succeededLockTokens) {
            System.out.println("    Released event lock token: " + lockToken);
        }
        // Print the information about failed lock tokens
        if (succeededLockTokens.size() < lockTokens.size()) {
            System.out.println("    At least one event was not released back to Event Grid.");
            writeFailedLockTokens(releaseResult.getFailedLockTokens());
        }
    }

    private static void reject(List<String> lockTokens) {
        RejectResult rejectResult = eventGridClient.rejectCloudEvents(TOPIC_NAME, EVENT_SUBSCRIPTION_NAME, new RejectOptions(lockTokens));
        List<String> succeededLockTokens = rejectResult.getSucceededLockTokens();
        if (succeededLockTokens != null && lockTokens.size() >= 1)
            System.out.println("--- " + succeededLockTokens.size() + " events were successfully rejected:");
        for (String lockToken : succeededLockTokens) {
            System.out.println("    Rejected event lock token: " + lockToken);
        }
        // Print the information about failed lock tokens
        if (succeededLockTokens.size() < lockTokens.size()) {
            System.out.println("    At least one event was not rejected.");
            writeFailedLockTokens(rejectResult.getFailedLockTokens());
        }
    }

    private static void renewLockToken(List<String> lockTokens) {
//        RenewLockResult renewLockResult = eventGridClient.renewLockCloudEvents(TOPIC_NAME, EVENT_SUBSCRIPTION_NAME, new RenewLockOptions(lockTokens));
//        List<String> succeededLockTokens = renewLockResult.getSucceededLockTokens();
//        if(succeededLockTokens != null && lockTokens.size() >=1 )
//            System.out.println("%%% " + succeededLockTokens.size() + " lock tokens were successfully renewed:");
//        for(String lockToken: succeededLockTokens){
//            System.out.println("    Renewed lock token: " + lockToken);
//        }
//        // Print the information about failed lock tokens
//        if(succeededLockTokens.size() < lockTokens.size()){
//            System.out.println("    At least one lock token was not renewed.");
//            writeFailedLockTokens(renewLockResult.getFailedLockTokens());
//        }
    }
    private static void writeFailedLockTokens(List<FailedLockToken> failedLockTokens) {
        for (FailedLockToken failedLockToken : failedLockTokens) {
            System.out.println("    Failed lock token: " + failedLockToken.getLockToken());
            System.out.println("    Error code: " + failedLockToken.getErrorCode());
            System.out.println("    Error description: " + failedLockToken.getErrorDescription());
        }
    }

}