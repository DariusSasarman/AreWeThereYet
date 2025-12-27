package com.example.arewethereyet;

public class CurrentTripTracker {
    private static Vehicle choice = Vehicle.NOTYET;
    public static Vehicle getChoice() {
        return choice;
    }
    public static void setChoice(Vehicle choice) {
        CurrentTripTracker.choice = choice;
    }
}
