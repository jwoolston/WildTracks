package com.jwoolston.wildtracks.settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.jwoolston.wildtracks.R;

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
