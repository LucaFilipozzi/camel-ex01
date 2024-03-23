// © 2024 Luca Filipozzi. Some rights reserved. See LICENSE.

package com.github.lucafilipozzi.camel.ex01;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.camel.builder.RouteBuilder;

public class MyRouteBuilder extends RouteBuilder {

    public void configure() {
        getContext().getRegistry().bind("cache", Caffeine.newBuilder().build());

        from("file:src/data?noop=true")
            .choice()
                .when(xpath("/person/city = 'London'"))
                    .log("UK message")
                    .to("direct:start")
                    .to("file:target/messages/uk")
                .otherwise()
                    .log("Other message")
                    .to("direct:start")
                    .to("file:target/messages/others");

        from("direct:start")
            .to("caffeine-cache://cache?action=PUT&key=1")
            .to("caffeine-cache://cache?action=GET&key=1")
            .log("test: ${body}")
            .to("mock:result");
    }
}
