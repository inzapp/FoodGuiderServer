package com.foodcam;

/**
 * Main 클래스
 * @author root
 */
public final class FoodCamServer extends ServerInitializer{
    public static void main(String[] args) {
        FoodCamServer foodCamServer = new FoodCamServer();
        foodCamServer.activate();
    }
}
