package com.firebirdberlin.nightdream;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentManager;

import com.firebirdberlin.nightdream.models.FontCache;
import com.firebirdberlin.nightdream.ui.WeatherForecastLayout;
import com.firebirdberlin.nightdream.ui.WeatherLayout;
import com.firebirdberlin.openweathermapapi.ForecastRequestTask;
import com.firebirdberlin.openweathermapapi.WeatherLocationDialogFragment;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WeatherForecastActivity
        extends BillingHelperActivity
        implements ForecastRequestTask.AsyncResponse,
        WeatherLocationDialogFragment.WeatherLocationDialogListener {
    final static String TAG = "WeatherForecastActivity";
    final int PERMISSIONS_REQUEST_ACCESS_LOCATION = 4000;
    final int MENU_ITEM_SEARCH = 3000;
    final int MENU_ITEM_ENABLE_GPS = 3001;
    final int MENU_ITEM_LOCATION_0 = 3002;
    final int MENU_ITEM_LOCATION_1 = 3003;
    final int MENU_ITEM_LOCATION_2 = 3004;
    final int MENU_ITEM_LOCATION_3 = 3005;
    final int MENU_ITEM_LOCATION_4 = 3006;
    final int MENU_ITEM_PREFERENCES = 3007;
    final int[] MENU_ITEMS_LOCATION = new int[] {
            MENU_ITEM_LOCATION_0,
            MENU_ITEM_LOCATION_1,
            MENU_ITEM_LOCATION_2,
            MENU_ITEM_LOCATION_3,
            MENU_ITEM_LOCATION_4,
    };
    private ScrollView scrollView = null;
    private LinearLayout scrollViewLayout = null;
    private Settings settings;
    private boolean locationAccessGranted = false;
    private boolean autoLocationEnabled = false;
    private ArrayList<City> cities = null;
    private City selectedCity = null;
    private int fadeDuration = 2000;
    private Snackbar snackbar;

    private LocationManager locationManager = null;
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            City city = new City();
            city.lat = location.getLatitude();
            city.lon = location.getLongitude();
            city.name = "current";
            Log.i(TAG, "location updated " + city.toJson());
            if (locationManager != null && locationListener != null) {
                locationManager.removeUpdates(locationListener);
            }
            requestWeather(city);
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    public static void start(Context context) {
        Intent intent = new Intent(context, WeatherForecastActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_forecast);
        scrollView = findViewById(R.id.scroll_view);
        scrollViewLayout = findViewById(R.id.scroll_view_layout);
        cities = Settings.getFavoriteWeatherLocations(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart()");
        locationAccessGranted = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        settings = new Settings(this);
        settings.initWeatherAutoLocationEnabled();
        selectedCity = settings.getCityForWeather();
        autoLocationEnabled = settings.getWeatherAutoLocationEnabled();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();
        hide();
    }

    void init() {
        if (! isPurchased(ITEM_WEATHER_DATA)) {
            setupForecastPreview();
            return;
        }

        if (autoLocationEnabled) {
            Log.i(TAG, "starting with auto location (GPS)");
            getWeatherForCurrentLocation();
        } else {

            selectedCity = settings.getCityForWeather();
            if (selectedCity != null && selectedCity.id > 0) {
                Log.i(TAG, "starting with " + selectedCity.toJson());
                addToFavoriteCities(selectedCity);
                requestWeather(selectedCity);
            }
        }
        conditionallyShowSnackBar();
    }

    void addToFavoriteCities(City city) {
        if (city != null && cities != null ) {
            Log.i(TAG, "addToFavoriteCities(" + city.toJson() + ")");
            if (!cities.contains(city)) {
                cities.add(city);
                while (cities.size() > MENU_ITEMS_LOCATION.length) {
                    cities.remove(0);
                }
            }
            selectedCity = city;
            Settings.setFavoriteWeatherLocations(this, cities);
            invalidateOptionsMenu();
        }
    }

    void setupForecastPreview() {
        List<WeatherEntry> entries = getFakeEntries();
        scrollViewLayout.removeAllViews();

        TextView textView = new TextView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textView.setLayoutParams(layoutParams);
        int color = Utility.getRandomMaterialColor(this);
        int textColor = Utility.getContrastColor(color);
        textView.setGravity(Gravity.CENTER);
        textView.setBackgroundColor(color);
        textView.setTextColor(textColor);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        textView.setPadding(10, 10, 10,  10);
        textView.setText(getString(R.string.product_name_weather));

        Typeface typeface = FontCache.get(this, "fonts/dancingscript_regular.ttf");
        textView.setTypeface(typeface);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Drawable icon = getDrawable(R.drawable.ic_googleplay);
            icon.setColorFilter(new PorterDuffColorFilter(textColor, PorterDuff.Mode.SRC_ATOP));
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, icon, null);
        }
        View.OnClickListener purchaseListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPurchaseDialog();
            }
        };
        textView.setOnClickListener(purchaseListener);
        scrollViewLayout.addView(textView);

        TextView textView1 = new TextView(this);
        textView1.setLayoutParams(layoutParams);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            textView1.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
        } else {
            textView1.setGravity(Gravity.CENTER);
        }
        int dp20 = Utility.dpToPx(this, 12);
        textView1.setPadding(dp20, dp20, dp20, dp20);
        textView1.setText(getString(R.string.product_description_weather));
        textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        scrollViewLayout.addView(textView1);

        TextView textView2 = new TextView(this);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams2.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
        textView2.setLayoutParams(layoutParams2);
        textView2.setBackgroundColor(color);
        textView2.setTextColor(textColor);
        textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        textView2.setPadding(10, dp20, 10, 0);
        textView2.setText(getString(R.string.upgrade_now));

        textView2.setTypeface(typeface);
        textView2.setOnClickListener(purchaseListener);
        scrollViewLayout.addView(textView2);

        WeatherLayout statusLine = new WeatherLayout(this);
        statusLine.setColor(Color.WHITE);
        statusLine.setGravity(Gravity.CENTER);
        statusLine.setWindSpeed(true, WeatherEntry.METERS_PER_SECOND);
        statusLine.update(entries.get(0));
        statusLine.setPadding(dp20, dp20, dp20, dp20);

        scrollViewLayout.addView(statusLine);
        addWeatherEntries(entries);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    private void hide() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            // avoid flickering during build
            scrollView.setAlpha(0);
        }
    }

    public void onRequestFinished(List<WeatherEntry> entries) {
        Log.i(TAG, "onRequestFinished()");
        hide();
        scrollViewLayout.removeAllViews();
        addWeatherEntries(entries);
    }

    void addWeatherEntries(List<WeatherEntry> entries) {
        Log.i(TAG, String.format(" > got %d entries", entries.size()));
        String timeFormat = settings.getFullTimeFormat();

        if (entries.size() > 0) {
            WeatherEntry firstEntry = entries.get(0);
            actionBarSetup(firstEntry.cityName);
        }

        int day = -1;
        long now = System.currentTimeMillis();
        for (WeatherEntry entry : entries) {
            if (entry.timestamp * 1000 < now - 600000) {
                continue;
            }
            TextView dateView = new TextView(this);
            if (Build.VERSION.SDK_INT >= 23) {
                dateView.setTextAppearance(android.R.style.TextAppearance_Medium);
                dateView.setTextColor(getResources().getColor(R.color.blue, null));
            } else {
                dateView.setTextAppearance(this, android.R.style.TextAppearance_Medium);
                dateView.setTextColor(getResources().getColor(R.color.blue));
            }
            dateView.setTypeface(null, Typeface.BOLD);

            Calendar mCalendar = Calendar.getInstance();
            mCalendar.setTimeInMillis(entry.timestamp * 1000);
            if (day != mCalendar.get(Calendar.DAY_OF_MONTH)) {
                day = mCalendar.get(Calendar.DAY_OF_MONTH);
                DateFormat sdf = DateFormat.getDateInstance(DateFormat.FULL);
                String text = sdf.format(mCalendar.getTime());
                dateView.setText(text);
                scrollViewLayout.addView(dateView);

            }

            WeatherForecastLayout layout = new WeatherForecastLayout(this);
            layout.setTimeFormat(timeFormat);
            layout.setTemperature(true, settings.temperatureUnit);
            layout.setWindSpeed(true, settings.speedUnit);
            layout.update(entry);
            scrollViewLayout.addView(layout);
        }

        // avoid flickering of the UI
        scrollView.animate().setDuration(fadeDuration).alpha(1);
        fadeDuration = 1000;
    }

    private void actionBarSetup(String subtitle) {
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.forecast);
            ab.setSubtitle(subtitle);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (cities != null) {
            for (int i = 0; i < cities.size(); i++) {
                City city = cities.get(i);
                if (city != null) {
                    menu.add(Menu.NONE, MENU_ITEMS_LOCATION[i], Menu.NONE, city.name)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                }
            }
        }
        menu.add(Menu.NONE, MENU_ITEM_ENABLE_GPS, Menu.NONE, getString(R.string.weather_current_location))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, MENU_ITEM_SEARCH, Menu.NONE, getString(R.string.weather_city_id_title))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, MENU_ITEM_PREFERENCES, Menu.NONE, getString(R.string.preferences))
                .setIcon(R.drawable.ic_settings)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        int num_items = cities.size() + 3;
        for (int i = 0; i < cities.size(); i++ ){
            City city = cities.get(i);
            final MenuItem item = menu.getItem(i);
            item.setCheckable(true);
            item.setChecked(city.equals(selectedCity) && !autoLocationEnabled);
        }
        menu.getItem(num_items - 3).setCheckable(true);
        menu.getItem(num_items - 3).setChecked(locationAccessGranted && autoLocationEnabled);

        boolean on = isPurchased(ITEM_WEATHER_DATA);
        for (int i = 0; i < num_items; i++ ) {
            menu.getItem(i).setEnabled(on);
        }
        MenuItem item = menu.getItem(num_items - 1);
        item.setVisible(on);

        return true;
    }

    void setAutoLocationEnabled(boolean autoLocationEnabled) {
        this.autoLocationEnabled = autoLocationEnabled;
        settings.setWeatherAutoLocationEnabled(autoLocationEnabled);
        if (autoLocationEnabled) {
            settings.setWeatherLocation(null);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ITEM_PREFERENCES:
                WeatherPreferenceActivity.start(this);
                return true;
            case MENU_ITEM_SEARCH:
                showWeatherLocationDialog();
                invalidateOptionsMenu();
                return true;
            case MENU_ITEM_ENABLE_GPS:
                autoLocationEnabled = !autoLocationEnabled;
                setAutoLocationEnabled(autoLocationEnabled);
                getWeatherForCurrentLocation();
                invalidateOptionsMenu();
                return true;
            case MENU_ITEM_LOCATION_0:
                onWeatherLocationSelected(cities.get(0));
                return true;
            case MENU_ITEM_LOCATION_1:
                onWeatherLocationSelected(cities.get(1));
                return true;
            case MENU_ITEM_LOCATION_2:
                onWeatherLocationSelected(cities.get(2));
                return true;
            case MENU_ITEM_LOCATION_3:
                onWeatherLocationSelected(cities.get(3));
                return true;
            case MENU_ITEM_LOCATION_4:
                onWeatherLocationSelected(cities.get(4));
                return true;
            default:
                return false;
        }
    }

    private void showWeatherLocationDialog() {
        FragmentManager fm = getSupportFragmentManager();
        WeatherLocationDialogFragment dialog = new WeatherLocationDialogFragment();
        dialog.show(fm, "weather_location_selection_dialog");
    }

    public void onWeatherLocationSelected(City city) {
        setAutoLocationEnabled(false);
        settings.setWeatherLocation(city);
        selectedCity = city;
        addToFavoriteCities(city);
        requestWeather(city);
    }

    void requestWeather(City city) {
        if (city != null) {
            new ForecastRequestTask(this, settings.getWeatherProvider()).execute(city.toJson());
        }
    }

    @SuppressLint("MissingPermission")
    private void getWeatherForCurrentLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSIONS_REQUEST_ACCESS_LOCATION
                );
                return;
            }
        }
        Log.i(TAG, "searching location");
        getLastKnownLocation();
        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 0, 0, locationListener
        );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults
    ) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    Log.i(TAG, "Permission granted");
                    locationAccessGranted = true;
                    autoLocationEnabled = true;
                    getWeatherForCurrentLocation();
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                    locationAccessGranted = false;
                }
                invalidateOptionsMenu();
                return;
        }
        // Other 'case' lines to check for other
        // permissions this app might request.
    }

    private void getLastKnownLocation() {
        if (!hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            return;
        }
        List<String> providers = locationManager.getProviders(true);
        for (String provider : providers) {
            if (LocationManager.GPS_PROVIDER.equals(provider)) {
                continue;
            }
            @SuppressLint("MissingPermission")
            Location location = locationManager.getLastKnownLocation(provider);
            if (location == null) continue;
            City city = new City();
            city.lat = location.getLatitude();
            city.lon = location.getLongitude();
            city.name = "current";
            if (locationManager != null && locationListener != null) {
                locationManager.removeUpdates(locationListener);
            }
            requestWeather(city);
        }
    }

    List<WeatherEntry> getFakeEntries() {
        List<WeatherEntry> entries = new ArrayList<>();

        Calendar now = Calendar.getInstance();
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        int hour = now.get(Calendar.HOUR);
        int diff = ((23 + 1) / 3 * 3 + 2) - hour;
        now.add(Calendar.HOUR, diff);
        WeatherEntry entry1 = new WeatherEntry();
        entry1.apparentTemperature = 292.15;
        entry1.cityName = "Bullerby";
        entry1.clouds = 71;
        entry1.humidity = 53;
        entry1.temperature = 293.15;
        entry1.timestamp = now.getTimeInMillis() / 1000;
        entry1.request_timestamp = System.currentTimeMillis();
        entry1.weatherIcon = "04n";
        entry1.windDirection = 247;
        entry1.windSpeed = 3.4;
        entries.add(entry1);
        WeatherEntry entry2 = new WeatherEntry();
        now.add(Calendar.HOUR, 3);
        entry2.apparentTemperature = 294.15;
        entry2.clouds = 57;
        entry2.humidity = 57;
        entry2.rain3h = 1.3;
        entry2.temperature = 295.15;
        entry2.timestamp = now.getTimeInMillis() / 1000;
        entry1.request_timestamp = System.currentTimeMillis();
        entry2.weatherIcon = "03n";
        entry2.windDirection = 266;
        entry2.windSpeed = 2.3;
        entries.add(entry2);
        return entries;
    }

    @Override
    protected void onItemPurchased(String sku) {
        super.onItemPurchased(sku);
        scrollViewLayout.removeAllViews();
        init();
        invalidateOptionsMenu();
        City city = settings.getCityForWeather();
        if (!autoLocationEnabled && (city == null || city.id == 0)) {
            showWeatherLocationDialog();
        }
    }

    @Override
    protected void onPurchasesInitialized() {
        super.onPurchasesInitialized();
        init();
        invalidateOptionsMenu();
        Settings.storeWeatherDataPurchase(
                this,
                isPurchased(BillingHelperActivity.ITEM_WEATHER_DATA),
                isPurchased(BillingHelperActivity.ITEM_DONATION)
        );
    }

    private boolean isLocationProviderEnabled() {
        int locationMode = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = android.provider.Settings.Secure.getInt(
                        getContentResolver(), android.provider.Settings.Secure.LOCATION_MODE
                );

            } catch (android.provider.Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }

            return locationMode != android.provider.Settings.Secure.LOCATION_MODE_OFF;

        } else {
            String locationProviders = android.provider.Settings.Secure.getString(
                    getContentResolver(),
                    android.provider.Settings.Secure.LOCATION_PROVIDERS_ALLOWED
            );
            return !locationProviders.isEmpty();
        }
    }

    private void conditionallyShowSnackBar(){
        if (!isLocationProviderEnabled()) {
            View view = findViewById(android.R.id.content);
            snackbar = Snackbar.make(view, R.string.permission_request_location_mode, Snackbar.LENGTH_INDEFINITE);
            int color = Utility.getRandomMaterialColor(this);
            int textColor = Utility.getContrastColor(color);
            View snackbarView = snackbar.getView();
            snackbarView.setBackgroundColor(color);
            snackbar.setActionTextColor(textColor);

            TextView tv = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
            tv.setTextColor(textColor);

            snackbar.setAction(R.string.action_change, new LocationProviderSettingListener());
            snackbar.show();
        } else {
            dismissSnackBar();
        }
    }

    void dismissSnackBar() {
        if (snackbar != null && snackbar.isShown()) {
            snackbar.dismiss();
            snackbar = null;
        }
    }

    public class LocationProviderSettingListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            Intent viewIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(viewIntent);
        }
    }
}
