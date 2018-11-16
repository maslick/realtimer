package io.maslick.sandbox.realtimer.ws;

import io.maslick.sandbox.realtimer.cluster.Cluster;

import java.util.Collections;

public class Starter {
    public static void main(String[] args) {
        new Cluster(Collections.singletonList(new WebsocketVert())).run();
    }
}
