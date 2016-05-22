package net.rakugakibox.springbootext.logback.access;

import ch.qos.logback.access.spi.IAccessEvent;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import static net.rakugakibox.springbootext.logback.access.test.AccessEventAssert.assertThat;
import net.rakugakibox.springbootext.logback.access.test.SingletonQueueAppender;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The base class to test of access event.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration
@WebIntegrationTest(
        value = "logback.access.config=classpath:logback-access-test.singleton-queue.xml",
        randomPort = true
)
public abstract class AbstractAccessEventTest {

    /**
     * The server port.
     */
    @Value("${local.server.port}")
    private int port;

    /**
     * The REST template.
     */
    private RestTemplate rest;

    /**
     * Sets up resources.
     */
    @Before
    public void setup() {

        // Initializes the REST template.
        rest = new TestRestTemplate();

        // Initializes the event queue.
        SingletonQueueAppender.clear();

    }

    /**
     * Tests the basic attributes of access event.
     */
    @Test
    public void basicAttributes() {

        RequestEntity<Void> request = RequestEntity
                .get(url("/text").build().toUri())
                .build();

        LocalDateTime startTime = LocalDateTime.now();
        ResponseEntity<String> response = rest.exchange(request, String.class);
        IAccessEvent event = SingletonQueueAppender.pop();
        LocalDateTime endTime = LocalDateTime.now();

        assertThat(response.getBody()).isEqualTo("text");
        assertThat(event)
                .hasTimestamp(startTime, endTime)
                .hasServerName("localhost")
                .hasLocalPort(port)
                .hasProtocol("HTTP/1.1")
                .hasMethod(HttpMethod.GET)
                .hasRequestUri("/text")
                .hasQueryString("")
                .hasRequestUrl(HttpMethod.GET, "/text", "HTTP/1.1")
                .hasRemoteAddr("127.0.0.1")
                .hasRemoteHost("127.0.0.1")
                .hasRemoteUser(null)
                .hasStatusCode(HttpStatus.OK)
                .hasContentLength(response.getBody().getBytes().length)
                .hasElapsedTime(startTime, endTime)
                .hasElapsedSeconds(startTime, endTime)
                .hasThreadName();
        assertThat(SingletonQueueAppender.isEmpty()).isTrue();

    }

    /**
     * Tests the query string of access event.
     */
    @Test
    public void queryString() {

        RequestEntity<Void> request = RequestEntity
                .get(url("/text").query("query").build().toUri())
                .build();

        ResponseEntity<String> response = rest.exchange(request, String.class);
        IAccessEvent event = SingletonQueueAppender.pop();

        assertThat(response.getBody()).isEqualTo("text");
        assertThat(event)
                .hasQueryString("?query")
                .hasRequestUrl(HttpMethod.GET, "/text?query", "HTTP/1.1");
        assertThat(SingletonQueueAppender.isEmpty()).isTrue();

    }

    /**
     * Tests the content length of access event.
     */
    @Test
    public void contentLength_withHeader() {

        RequestEntity<Void> request = RequestEntity
                .get(url("/text").build().toUri())
                .build();

        ResponseEntity<String> response = rest.exchange(request, String.class);
        IAccessEvent event = SingletonQueueAppender.pop();

        assertThat(response.getHeaders()).containsKey(HttpHeaders.CONTENT_LENGTH);
        assertThat(response.getBody()).isEqualTo("text");
        assertThat(event).hasContentLength(response.getBody().getBytes().length);
        assertThat(SingletonQueueAppender.isEmpty()).isTrue();

    }

    /**
     * Tests the content length of access event.
     */
    @Test
    public void contentLength_withoutHeader() {

        RequestEntity<Void> request = RequestEntity
                .get(url("/json").build().toUri())
                .build();

        ResponseEntity<String> response = rest.exchange(request, String.class);
        IAccessEvent event = SingletonQueueAppender.pop();

        assertThat(response.getHeaders()).doesNotContainKey(HttpHeaders.CONTENT_LENGTH);
        assertThat(response.getBody()).isEqualTo("{\"json-key\":\"json-value\"}");
        assertThat(event).hasContentLength(response.getBody().getBytes().length);
        assertThat(SingletonQueueAppender.isEmpty()).isTrue();

    }

    /**
     * Starts building the URL.
     *
     * @param path the path of URL.
     * @return a URI components builder.
     */
    private UriComponentsBuilder url(String path) {
        return UriComponentsBuilder.newInstance()
                .scheme("http")
                .host("localhost")
                .port(port)
                .path(path);
    }

    /**
     * The context configuration.
     */
    @Configuration
    @EnableAutoConfiguration
    public static class TestContextConfiguration {

        /**
         * Creates a controller.
         *
         * @return a controller.
         */
        @Bean
        public TestController testController() {
            return new TestController();
        }

    }

    /**
     * The controller.
     */
    @RestController
    public static class TestController {

        /**
         * Gets the text.
         *
         * @return the text.
         */
        @RequestMapping(path = "/text", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
        public String getText() {
            return "text";
        }

        /**
         * Gets the JSON.
         *
         * @return the JSON.
         */
        @RequestMapping(path = "/json", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
        public Map<String, String> getJson() {
            Map<String, String> map = new HashMap<>();
            map.put("json-key", "json-value");
            return map;
        }

    }

}
