// © 2024 Luca Filipozzi. Some rights reserved. See LICENSE.

package com.github.lucafilipozzi.camel.ex01;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.SimpleUuidGenerator;
import org.apache.camel.CamelContext;

public class MyRouteBuilder extends RouteBuilder {

    public void configure() {
        CamelContext camelContext = getCamelContext();
        camelContext.getRegistry().bind("cache", Caffeine.newBuilder().build());
        camelContext.setTracing(false);
        camelContext.setUuidGenerator(new SimpleUuidGenerator());

        // https://camel.apache.org/components/4.4.x/timer-component.html
        from("timer:trigger?repeatCount=5&fixedRate=true")
            .routeId("trigger")
            .log("hi mum")
            .to("direct:cache");

        // https://camel.apache.org/components/4.4.x/file-component.html
        // https://camel.apache.org/components/4.4.x/xj-component.html
        from("file:src/data?noop=true")
            .routeId("mainflow")
            .to("direct:xmlHandler")
            .to("xj:identity?transformDirection=XML2JSON")
            .to("direct:jsonHandler");

        from("direct:xmlHandler")
            .routeId("xmlHandler")
            .to("direct:xmlflow1")  // xmlflow1 processes xml using xpath
            .to("direct:xmlflow2"); // xmlflow2 processes xml using xquery

        from("direct:jsonHandler")
            .routeId("jsonHandler")
            .to("direct:jsonflow1")  // jsonflow1 processes json using jq
            .to("direct:jsonflow2"); // jsonflow2 processes json using jsonpath

        // https://camel.apache.org/components/4.4.x/languages/xpath-language.html
        from("direct:xmlflow1")
            .routeId("xmlflow1")
            .log("${id}: processing")
            .choice()
                .when().xpath("/person/city = 'London'")
                    .log("${id}: UK message")
                    .to("file:target/messages/uk")
                .otherwise()
                    .log("${id}: Other message")
                    .to("file:target/messages/others");

        // https://camel.apache.org/components/4.4.x/languages/xquery-language.html
        from("direct:xmlflow2")
            .routeId("xmlflow2")
            .log("${id}: processing")
            .choice()
                .when().xquery("/person[city = 'London']")
                    .log("${id}: UK message")
                    .to("file:target/messages/uk")
                .otherwise()
                    .log("${id}: Other message")
                    .to("file:target/messages/others");

        // https://camel.apache.org/components/4.4.x/languages/jq-language.html
        from("direct:jsonflow1")
            .routeId("jsonflow1")
            .log("${id}: processing")
            .choice()
                .when().jq(".city == \"London\"")
                    .log("${id}: UK message")
                    .to("mock:result")
                .otherwise()
                    .log("${id}: Other message")
                    .to("mock:result");

        // https://camel.apache.org/components/4.4.x/languages/jsonpath-language.html
        from("direct:jsonflow2")
            .routeId("jsonflow2")
            .log("${id}: processing")
            .choice()
                .when().jsonpath("$[?(@.city == 'London')]")
                    .log("${id}: UK message")
                    .to("mock:result")
                .otherwise()
                    .log("${id}: Other message")
                    .to("mock:result");

        from("direct:cache")
            .routeId("cache")
            .to("caffeine-cache://cache?action=PUT&key=1")
            .to("caffeine-cache://cache?action=GET&key=1")
            .to("mock:result");
    }
}
