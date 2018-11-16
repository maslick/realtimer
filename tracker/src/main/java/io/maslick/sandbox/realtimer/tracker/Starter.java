package io.maslick.sandbox.realtimer.tracker;

import io.maslick.sandbox.realtimer.cluster.Cluster;

import java.util.Arrays;

public class Starter {
    public static void main(String[] args) {
        new Cluster(Arrays.asList(
                new HttpServerVert(),
                new RouterVert()
        )).run();
    }
}