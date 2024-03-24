// © 2024 Luca Filipozzi. Some rights reserved. See LICENSE.

package com.github.lucafilipozzi.camel.ex01;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.SimpleUuidGenerator;
import com.github.benmanes.caffeine.cache.Caffeine;

public class MyRouteBuilder extends RouteBuilder {

    private static final String PROCESSING_LOG_MESSAGE = "${id}: processing";
    private static final String WHEN_RESULT_LOG_MESSAGE = "${id}: when result";
    private static final String OTHERWISE_RESULT_LOG_MESSAGE = "${id}: otherwise result";
    private static final String MOCK_RESULT = "mock:result";
    private static final String DIRECT_XML = "direct:xml";
    private static final String DIRECT_XPATH = "direct:xpath";
    private static final String DIRECT_XQUERY = "direct:xquery";
    private static final String DIRECT_JSON = "direct:json";
    private static final String DIRECT_JSONPATH = "direct:jsonpath";
    private static final String DIRECT_JQ = "direct:jq";

    public void configure() {
        CamelContext camelContext = getCamelContext();
        camelContext.getRegistry().bind("cache", Caffeine.newBuilder().build());
        camelContext.setTracing(false);
        camelContext.setUuidGenerator(new SimpleUuidGenerator());

        // https://camel.apache.org/components/4.4.x/timer-component.html
        from("timer:trigger?repeatCount=5&fixedRate=true")
            .routeId("timer:trigger")
            .log("hi mum")
            .to(MOCK_RESULT);

        // https://camel.apache.org/components/4.4.x/file-component.html
        from("file:src/data?noop=true")
            .routeId("file:src/data")
            .to(DIRECT_XML)
            .to(DIRECT_JSON);

        from(DIRECT_XML)
            .routeId(DIRECT_XML)
            .to(DIRECT_XPATH)
            .to(DIRECT_XQUERY);

        // https://camel.apache.org/components/4.4.x/xj-component.html
        from(DIRECT_JSON)
            .routeId(DIRECT_JSON)
            .to("xj:identity?transformDirection=XML2JSON&failOnNullBody=true")
            .to(DIRECT_JQ)
            .to(DIRECT_JSONPATH);

        // https://camel.apache.org/components/4.4.x/languages/xpath-language.html
        from(DIRECT_XPATH)
            .routeId(DIRECT_XPATH)
            .log(PROCESSING_LOG_MESSAGE)
            .choice()
                .when().xpath("/person/city = 'London'")
                    .log(WHEN_RESULT_LOG_MESSAGE)
                    .to(MOCK_RESULT)
                .otherwise()
                    .log(OTHERWISE_RESULT_LOG_MESSAGE)
                    .to(MOCK_RESULT);

        // https://camel.apache.org/components/4.4.x/languages/xquery-language.html
        from(DIRECT_XQUERY)
            .routeId(DIRECT_XQUERY)
            .log(PROCESSING_LOG_MESSAGE)
            .choice()
                .when().xquery("/person[city = 'London']")
                    .log(WHEN_RESULT_LOG_MESSAGE)
                    .to(MOCK_RESULT)
                .otherwise()
                    .log(OTHERWISE_RESULT_LOG_MESSAGE)
                    .to(MOCK_RESULT);

        // https://camel.apache.org/components/4.4.x/languages/jq-language.html
        from(DIRECT_JQ)
            .routeId(DIRECT_JQ)
            .log(PROCESSING_LOG_MESSAGE)
            .choice()
                .when().jq(".city == \"London\"")
                    .log(WHEN_RESULT_LOG_MESSAGE)
                    .to(MOCK_RESULT)
                .otherwise()
                    .log(OTHERWISE_RESULT_LOG_MESSAGE)
                    .to(MOCK_RESULT);

        // https://camel.apache.org/components/4.4.x/languages/jsonpath-language.html
        from(DIRECT_JSONPATH)
            .routeId(DIRECT_JSONPATH)
            .log(PROCESSING_LOG_MESSAGE)
            .choice()
                .when().jsonpath("$[?(@.city == 'London')]")
                    .log(WHEN_RESULT_LOG_MESSAGE)
                    .to(MOCK_RESULT)
                .otherwise()
                    .log(OTHERWISE_RESULT_LOG_MESSAGE)
                    .to(MOCK_RESULT);

        from("direct:cache")
            .routeId("direct:cache")
            .to("caffeine-cache://cache?action=PUT&key=1")
            .to("caffeine-cache://cache?action=GET&key=1")
            .to(MOCK_RESULT);
    }
}
