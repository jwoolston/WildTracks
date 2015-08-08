package com.jwoolston.huntinglogger.settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.jwoolston.huntinglogger.R;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class SettingsActivity extends PreferenceActivity {

    public SettingsActivity() {
        super();
        // Empty constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}
