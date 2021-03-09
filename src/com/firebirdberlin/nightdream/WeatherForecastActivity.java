package com.firebirdberlin.nightdream;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.firebirdberlin.openweathermapapi.WeatherLocationDialogFragment;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

public class WeatherForecastActivity
        extends BillingHelperActivity
        implements
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
    private Settings settings;
    private boolean locationAccessGranted = false;
    private boolean autoLocationEnabled = false;
    private ArrayList<City> cities = null;
    private City selectedCity = null;
    private Snackbar snackbar;
    private WeatherForecastTabAdapter adapter;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    private LocationManager locationManager = null;
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            Log.d(TAG,"onLocationChanged");
            City city = new City();
            city.lat = location.getLatitude();
            city.lon = location.getLongitude();
            city.name = "current";
            city.id = -1;
            Log.i(TAG, "location updated " + city.toJson());
            if (locationManager != null && locationListener != null) {
                locationManager.removeUpdates(locationListener);
            }
            ((WeatherForecastTabWeather) adapter.getItem(0)).requestWeather(city, settings);
            if (settings.showPollen) {
                ((WeatherForecastTabPollen) adapter.getItem(1)).addPollen(city);
            }
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
        Log.d(TAG,"onCreate");
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_weather_forecast);
        }
        else{
            setContentView(R.layout.activity_weather_forecast_land);
        }
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        cities = Settings.getFavoriteWeatherLocations(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
        locationAccessGranted = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        settings = new Settings(this);
        settings.initWeatherAutoLocationEnabled();
        selectedCity = settings.getCityForWeather();
        autoLocationEnabled = settings.getWeatherAutoLocationEnabled();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        adapter = new WeatherForecastTabAdapter(getSupportFragmentManager());
        adapter.addFragment(new WeatherForecastTabWeather(), getResources().getString(R.string.forecast));

        if (settings.showPollen) {
            adapter.addFragment(new WeatherForecastTabPollen(), getResources().getString(R.string.showPollen));
        }

        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public void onResume() {
        Log.d(TAG,"onResume()");
        super.onResume();
    }

    void init() {
        Log.d(TAG,"init()");

        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);

        if (!isPurchased(ITEM_WEATHER_DATA)) {
            adapter = new WeatherForecastTabAdapter(getSupportFragmentManager());
            adapter.addFragment(new WeatherForecastTabWeather(), getResources().getString(R.string.forecast));

            viewPager.setAdapter(adapter);
            tabLayout.setupWithViewPager(viewPager);

            ((WeatherForecastTabWeather)adapter.getItem(0)).setupForecastPreview(settings);
            return;
        }

        if (autoLocationEnabled) {
            Log.i(TAG, "starting with auto location (GPS)");
            initTabView(new City());
        } else {
            selectedCity = settings.getCityForWeather();
            if (selectedCity != null && selectedCity.id > 0) {
                Log.i(TAG, "starting with " + selectedCity.toJson());
                addToFavoriteCities(selectedCity);
                initTabView(selectedCity);
            }
        }

        conditionallyShowSnackBar();
    }

    private void initTabView(City city) {
        Log.d(TAG, "initTabView");

        if ((autoLocationEnabled)){
            ((WeatherForecastTabWeather)adapter.getItem(0)).getWeatherForCurrentLocation(locationManager,locationListener, settings);
        }
        else {
            WeatherEntry entrie = settings.weatherEntry;
            Log.d(TAG,"addWeatherNow() city: "+entrie.cityName);
            ((WeatherForecastTabWeather) adapter.getItem(0)).requestWeather(city, settings);
        }

        if (settings.showPollen) {
            ((WeatherForecastTabPollen) adapter.getItem(1)).addPollen(city);
        }
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

    @Override
    protected void onPause() {
        super.onPause();
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
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
                initTabView(new City());
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
        Log.d(TAG,"onWeatherLocationSelected");

        setAutoLocationEnabled(false);
        settings.setWeatherLocation(city);
        selectedCity = city;
        addToFavoriteCities(city);
        settings = new Settings(this);

        initTabView(city);
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
                    //getWeatherForCurrentLocation();
                    initTabView(new City());
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                    locationAccessGranted = false;
                }
                invalidateOptionsMenu();
        }
        // Other 'case' lines to check for other
        // permissions this app might request.
    }

    @Override
    protected void onItemPurchased(String sku) {
        super.onItemPurchased(sku);
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
