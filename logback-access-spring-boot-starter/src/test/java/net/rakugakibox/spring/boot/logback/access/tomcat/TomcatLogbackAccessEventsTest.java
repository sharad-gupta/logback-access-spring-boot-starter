package net.rakugakibox.spring.boot.logback.access.tomcat;

import net.rakugakibox.spring.boot.logback.access.AbstractLogbackAccessEventsTest;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * The test of Logback-access events for Tomcat.
 */
public class TomcatLogbackAccessEventsTest extends AbstractLogbackAccessEventsTest {

    /**
     * The context configuration.
     */
    @Configuration
    @Import(ServletWebServerFactoryAutoConfiguration.EmbeddedTomcat.class)
    public static class ContextConfiguration extends AbstractContextConfiguration {
    }

}
