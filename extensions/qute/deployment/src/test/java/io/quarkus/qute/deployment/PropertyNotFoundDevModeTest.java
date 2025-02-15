package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.vertx.web.Route;
import io.restassured.RestAssured;

public class PropertyNotFoundDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest testConfig = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Routes.class)
                    .addAsResource(new StringAsset("{foo.surname}"), "templates/foo.html")
                    .addAsResource(new StringAsset("{bar.name}"), "templates/bar.html"));

    @Test
    public void testExceptionIsThrown() {
        assertEquals("Entry \"foo\" not found in the data map in expression {foo.surname} in template foo.html on line 1",
                RestAssured.get("test-foo").then().statusCode(200).extract().body().asString());
        assertEquals(
                "Property \"name\" not found on the base object \"java.lang.String\" in expression {bar.name} in template bar.html on line 1",
                RestAssured.get("test-bar").then().statusCode(200).extract().body().asString());
    }

    @Singleton
    public static class Routes {

        @Inject
        Template foo;

        @Route(produces = "text/plain")
        String testFoo() {
            try {
                return foo.render();
            } catch (TemplateException e) {
                return e.getMessage();
            }
        }

        @Inject
        Template bar;

        @Route(produces = "text/plain")
        String testBar() {
            try {
                return bar.data("bar", "alpha").render();
            } catch (TemplateException e) {
                return e.getMessage();
            }
        }

    }

}
