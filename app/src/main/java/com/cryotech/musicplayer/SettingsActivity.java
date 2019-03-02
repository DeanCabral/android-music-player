package com.cryotech.musicplayer;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Settings");
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public static class MyPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener
    {

        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            Preference licence = findPreference("licences");
            Preference clearRecents = findPreference("clearRecents");

            licence.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {

                    String licence = "Copyright 2017 Dean Cabral\n" +
                            "\n" +
                            "Licensed under the Apache License, Version 2.0 (the \"License\"); " +
                            "you may not use this file except in compliance with the License. You may obtain a copy of the License at\n" +
                            "\n" +
                            "http://www.apache.org/licenses/LICENSE-2.0\n" +
                            "\n" +
                            "Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an \"AS IS\" BASIS," +
                            " WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. " +
                            "See the License for the specific language governing permissions and limitations under the License.";

                    final AlertDialog.Builder builderSingle = new AlertDialog.Builder(getActivity(), R.style.MyAlertDialogStyle);
                    builderSingle.setTitle("Licences");
                    builderSingle.setMessage(licence);
                    builderSingle.show();
                    return true;
                }
            });
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            if (key.equals("headphoneState")) {
                boolean state = sharedPreferences.getBoolean("headphoneState", true);

                if (state) {
                    Preferences.HEADPHONE_STATE = true;
                } else {
                    Preferences.HEADPHONE_STATE = false;
                }
            }

            if (key.equals("albumArtDisplay")) {
                boolean state = sharedPreferences.getBoolean("albumArtDisplay", true);

                if (state) {
                    Preferences.DISPLAY_ALBUM_ART = true;
                } else {
                    Preferences.DISPLAY_ALBUM_ART = false;
                }
            }

            if (key.equals("fadeEffect")) {
                boolean state = sharedPreferences.getBoolean("fadeEffect", false);

                if (state) {
                    Preferences.FADE_EFFECT = true;
                } else {
                    Preferences.FADE_EFFECT = false;
                }
            }

            if (key.equals("notifDisplay")) {
                boolean state = sharedPreferences.getBoolean("notifDisplay", false);
                Intent serviceIntent = new Intent(getActivity(), MusicService.class);
                if (state) {
                    Preferences.NOTIF_DISPLAY = true;
                    serviceIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                    getActivity().startService(serviceIntent);
                } else {
                    Preferences.NOTIF_DISPLAY = false;
                    serviceIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                    getActivity().startService(serviceIntent);
                }
            }

            if (key.equals("shakeSkip")) {
                boolean state = sharedPreferences.getBoolean("shakeSkip", false);

                if (state) {
                    Preferences.SHAKE_SKIP = true;
                } else {
                    Preferences.SHAKE_SKIP = false;
                }
            }

            if (key.equals("duplicateMembers")) {
                boolean state = sharedPreferences.getBoolean("duplicateMembers", false);

                if (state) {
                    Preferences.DUPLICATE_MEMBERS = true;
                } else {
                    Preferences.DUPLICATE_MEMBERS = false;
                }
            }

        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        }

        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }
    }

}
