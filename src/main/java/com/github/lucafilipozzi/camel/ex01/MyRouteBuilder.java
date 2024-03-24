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

        from("file:src/data?noop=true")
            .routeId("mainflow")
            .log("${id}: sending to subflow1 and subflow2")
            .to("direct:subflow1")  // subflow1 processes xml using xpath
            .to("direct:subflow4")  // subflow4 processes xml using xquery
            .to("xj:identity?transformDirection=XML2JSON")
            .to("direct:subflow2")  // subflow2 processes json using jq
            .to("direct:subflow3")  // subflow3 processes json using jsonpath
        .end();

        // https://camel.apache.org/components/4.4.x/languages/xpath-language.html
        from("direct:subflow1")
            .routeId("subflow1")
            .log("${id}: processing")
            .choice()
                .when().xpath("/person/city = 'London'")
                    .log("${id}: UK message")
                    .to("file:target/messages/uk")
                .otherwise()
                    .log("${id}: Other message")
                    .to("file:target/messages/others");

        // https://camel.apache.org/components/4.4.x/languages/xquery-language.html
        from("direct:subflow4")
            .routeId("subflow4")
            .log("${id}: processing")
                .choice()
                    .when().xquery("/person[city = 'London']")
                        .log("${id}: UK message")
                        .to("file:target/messages/uk")
                    .otherwise()
                        .log("${id}: Other message")
                        .to("file:target/messages/others");

        // https://camel.apache.org/components/4.4.x/languages/jq-language.html
        from("direct:subflow2")
            .routeId("subflow2")
            .log("${id}: processing")
            .choice()
                .when().jq(".city == \"London\"")
                    .log("${id}: UK message")
                    .to("mock:result")
                .otherwise()
                    .log("${id}: Other message")
                    .to("mock:result");

        // https://camel.apache.org/components/4.4.x/languages/jsonpath-language.html
        from("direct:subflow3")
            .routeId("subflow3")
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
