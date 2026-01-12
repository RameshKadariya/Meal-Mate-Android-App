package com.raka.mealmate.models;

import android.app.Activity;

public class AppFeature {
    private String name;
    private int iconResourceId;
    private Class<? extends Activity> activityClass;

    public AppFeature() {
        // Required empty constructor for Firebase
    }

    public AppFeature(String name, int iconResourceId, Class<? extends Activity> activityClass) {
        this.name = name;
        this.iconResourceId = iconResourceId;
        this.activityClass = activityClass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIconResourceId() {
        return iconResourceId;
    }

    public void setIconResourceId(int iconResourceId) {
        this.iconResourceId = iconResourceId;
    }

    public Class<? extends Activity> getActivityClass() {
        return activityClass;
    }

    public void setActivityClass(Class<? extends Activity> activityClass) {
        this.activityClass = activityClass;
    }
}
