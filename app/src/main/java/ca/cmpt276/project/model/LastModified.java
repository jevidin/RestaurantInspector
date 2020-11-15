package ca.cmpt276.project.model;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Stores the timestamp of when the app last checked for updates and the
 * timestamp of the current restaurant and inspection list modification.
 */
public class LastModified {
    public static final String LAST_CHECKED = "last checked";
    public static final String LAST_MODIFIED_REST = "last modified restaurant list";
    public static final String LAST_MODIFIED_INSP = "last modified inspection list";
    private LocalDateTime lastCheck;
    private LocalDateTime last_mod_restaurants;
    private LocalDateTime last_mod_inspections;

    // Singleton Support
    private static LastModified instance;

    private LastModified(Context context) {
        lastCheck = getLastUpdate(context, LAST_CHECKED);
        last_mod_restaurants = getLastUpdate(context, LAST_MODIFIED_REST);
        last_mod_inspections = getLastUpdate(context, LAST_MODIFIED_INSP);
    }

    public static LastModified getInstance(Context context) {
        if (instance == null) {
            instance = new LastModified(context);
            System.out.println("last modified NOT CREATED YET *******************");
        }
        return instance;
    }

    // Shared Preferences to store last time checked
    private LocalDateTime getLastUpdate(Context context, String modified) {
        long lastUpdated = readLastUpdated(context, 0, modified);

        // convert long to LocalTimeDate
        if (lastUpdated == 0) {
            return LocalDateTime.of(1971, Month.JULY, 29, 19, 30, 40);
        }
        return Instant.ofEpochMilli(lastUpdated).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private static Long readLastUpdated(Context context, long defaultTime, String modified) {
        SharedPreferences stored = context.getSharedPreferences("AppPrefs",Context.MODE_PRIVATE);
        return stored.getLong(modified, defaultTime);
    }

    public void writeLastUpdated(Context context, LocalDateTime time, String modified) {
        SharedPreferences stored = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = stored.edit();

        long save = convertToLong(time);

        editor.putLong(modified, save);
        editor.apply();
        System.out.println("************SAVEDDDDDD " + modified + ": " + time + "*******_-------------");
    }

    private long convertToLong(LocalDateTime date) {
        ZonedDateTime zdt = ZonedDateTime.of(date, ZoneId.systemDefault());
        return zdt.toInstant().toEpochMilli();
    }

/*    // https://www.javaguides.net/2020/03/convert-localdatetime-to-long-in-java.html
    // Convert LocalDateTime to a Long
    private long getDefaultTime(LocalDateTime time) {
        ZonedDateTime zdt = ZonedDateTime.of(time, ZoneId.systemDefault());
        return zdt.toInstant().toEpochMilli();
    }*/

    public LocalDateTime getLastCheck() {
        return lastCheck;
    }

    public LocalDateTime getLast_mod_restaurants() {
        return last_mod_restaurants;
    }

    public LocalDateTime getLast_mod_inspections() {
        return last_mod_inspections;
    }

    public void setLastCheck(Context context, LocalDateTime lastCheck) {
        writeLastUpdated(context, lastCheck, LAST_CHECKED);
        this.lastCheck = lastCheck;
    }

    public void setLast_mod_restaurants(Context context, LocalDateTime last_mod_restaurants) {
        writeLastUpdated(context, lastCheck, LAST_MODIFIED_REST);
        this.last_mod_restaurants = last_mod_restaurants;
    }

    public void setLast_mod_inspections(Context context, LocalDateTime last_mod_inspections) {
        writeLastUpdated(context, last_mod_inspections, LAST_MODIFIED_INSP);
        this.last_mod_inspections = last_mod_inspections;
    }
}
