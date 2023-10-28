package com.azure.messaging.eventgrid.samples;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.models.CloudEvent;
import com.azure.core.models.CloudEventDataFormat;
import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridClient;
import com.azure.messaging.eventgrid.EventGridClientBuilder;
import com.azure.messaging.eventgrid.EventGridMessagingServiceVersion;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * <p>Simple demo publisher of CloudEvents to Event Grid namespace topics.
 *
 * This code samples should use Java 1.8 level or above to avoid compilation errors.
 *
 * You should consult the resources below to use the client SDK and set up your project using maven.
 * @see <a href="https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/eventgrid/azure-messaging-eventgrid">Event Grid data plane client SDK documentation</a>
 * @see <a href="https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/boms/azure-sdk-bom/README.md">Azure BOM for client libraries</a>
 * @see <a href="https://aka.ms/spring/versions">Spring Version Mapping</a> if you are using Spring.
 * @see <a href="https://aka.ms/azsdk">Tool with links to control plane and data plane SDKs across all languages supported</a>.
 *</p>
 */
public class NamespaceTopicPublisher {
    private static final String TOPIC_NAME = "<yourNamespaceTopicName>";
    public static final String ENDPOINT =  "<yourFullHttpsUrlToTheNamespaceEndpoint>";
    public static final int NUMBER_OF_EVENTS_TO_BUILD_THAT_DOES_NOT_EXCEED_100 = 10;

    //TODO  Do NOT include keys in source code. This code's objective is to give you a succinct sample about using Event Grid, not to provide an authoritative example for handling secrets in applications.
    /**
    *  For security concerns, you should not have keys or any other secret in any part of the application code.
     *  You should use services like Azure Key Vault for managing your keys.
     */
    public static final AzureKeyCredential CREDENTIAL = new AzureKeyCredential("<namespace key>");

    public static void main(String[] args) {
        //TODO Update Event Grid version number to your desired version. You can find more information on data plane APIs here:
        //https://learn.microsoft.com/en-us/rest/api/eventgrid/.
        EventGridClient eventGridClient = new EventGridClientBuilder()
                .httpClient(HttpClient.createDefault())  // Requires Java 1.8 level
                .endpoint(ENDPOINT)
                .serviceVersion(EventGridMessagingServiceVersion.V2023_06_01_PREVIEW)
                .credential(CREDENTIAL).buildClient();   // you may want to use .buildAsyncClient() for an asynchronous (project reactor) client.

        List<CloudEvent> cloudEvents = buildCloudEvents(NUMBER_OF_EVENTS_TO_BUILD_THAT_DOES_NOT_EXCEED_100);
        eventGridClient.publishCloudEvents(TOPIC_NAME, cloudEvents);
        System.out.println("--> Number of events published: " + NUMBER_OF_EVENTS_TO_BUILD_THAT_DOES_NOT_EXCEED_100); // There is no partial publish. Either all succeed or none.
    }

    /**
     * <p>Builds a list of valid CloudEvents for testing purposes</p>
     * @param numberOfEventsToBuild this should not exceed 100, which is the maximum number of events allowed in a single HTTP request or 1MB in size, whichever is met first.
     * @return the list of CloudEvents
     */
    private static List<CloudEvent> buildCloudEvents(int numberOfEventsToBuild) {
        List<CloudEvent> cloudEvents = new ArrayList<>(numberOfEventsToBuild);
        while (numberOfEventsToBuild >= 1) {
            cloudEvents.add(buildCloudEvent());
            numberOfEventsToBuild--;
        }
        return cloudEvents;
    }

    /**
     * <p>Builds a list of valid CloudEvent for testing purposes.</p>
     * @return a CloudEvent
     */
    private static CloudEvent buildCloudEvent() {
        String orderId = Integer.toString(new Random().nextInt(1000-10+1) + 10);  // Generates a random integer between 1000 and 1 (exclusive)

        return new CloudEvent("/account/a-4305/orders", "com.MyCompanyName.OrderCreated",
                BinaryData.fromObject(new HashMap<String, String>() {
                    {
                        put("orderId", orderId);
                        put("orderResourceURL", "https://www.MyCompanyName.com/orders/" + orderId);
                        put("isRushOrder", "true");
                        put("customerType", "Institutional");
                    }
                }), CloudEventDataFormat.JSON, "application/json")
                .setTime(OffsetDateTime.now());
    }
}