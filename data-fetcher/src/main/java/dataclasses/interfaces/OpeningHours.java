package dataclasses.interfaces;

import dataclasses.OpeningHoursImplementation;

import java.time.DayOfWeek;

public interface OpeningHours {
    DayOfWeek getWeekday();

    boolean isOpen();

    String getOpeningAt();

    String getClosingAt();

    String getGetAMealTill();

    @Override
    String toString();

    static OpeningHoursImplementation createOpeningHours(DayOfWeek weekday, boolean open, String openingAt, String closingAt, String getAMealTill) {
        return new OpeningHoursImplementation(weekday, open, openingAt, closingAt, getAMealTill);
    }
}
