// © 2024 Luca Filipozzi. All rights reserved.

package com.github.lucafilipozzi.camel.ex01;

import org.apache.camel.main.Main;

public class MainApp {

    public static void main(String... args) throws Exception {
        Main main = new Main();
        main.configure().addRoutesBuilder(new MyRouteBuilder());
        main.run(args);
    }
}
