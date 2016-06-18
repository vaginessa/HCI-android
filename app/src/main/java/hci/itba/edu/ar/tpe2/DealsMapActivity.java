package hci.itba.edu.ar.tpe2;

import android.*;
import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import hci.itba.edu.ar.tpe2.backend.data.Airport;
import hci.itba.edu.ar.tpe2.backend.data.Deal;
import hci.itba.edu.ar.tpe2.backend.network.API;
import hci.itba.edu.ar.tpe2.backend.network.NetworkRequestCallback;

public class DealsMapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, NavigationView.OnNavigationItemSelectedListener {

    private GoogleMap mMap;
    private Deal[] deals;
    private GoogleApiClient mGoogleApiClient;
    private double latitude;
    private double longitude;
    private Airport closestAirport;
    private static final int PERM_LOCATION = 42;
    private boolean locationPermissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deals_map);

        //Set up the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //FAB
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Go to flights search activity
                Intent i = new Intent(DealsMapActivity.this, SearchActivity.class);
                startActivity(i);
            }
        });
        //Drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(2).setChecked(true);       //Set the flights option as selected TODO I don't think this is Android standard

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onConnected(Bundle connectionHint) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Facu, lee esto para pedirle permisos al usuario si no los dio y qué hacer si no da permiso:
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERM_LOCATION);
            if (!locationPermissionGranted) {
                Toast.makeText(DealsMapActivity.this, "Y U NO LET ME LOCATE U", Toast.LENGTH_SHORT).show();
                return;
            }
            // Consider calling ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }
        Location lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (lastKnownLocation != null) {
            latitude = lastKnownLocation.getLatitude();
            longitude = lastKnownLocation.getLongitude();
            API.getInstance().getAirportsByLocation(latitude, longitude, 10, this, new NetworkRequestCallback<Airport[]>() {
                @Override
                public void execute(Context c, Airport[] nearbyAirports) {
                    if (nearbyAirports.length == 0) {
                        Toast.makeText(DealsMapActivity.this, "No airport found near you =(", Toast.LENGTH_SHORT).show();    //TODO remove this, for debugging
                    } else {
                        Toast.makeText(DealsMapActivity.this, "Located you at " + nearbyAirports[0].toString(), Toast.LENGTH_SHORT).show();    //TODO remove?
                        closestAirport = nearbyAirports[0]; //TODO Discutir cómo devolvemos el mas cercano y demás
                        LatLng airportPosition = new LatLng(closestAirport.getLatitude(), closestAirport.getLongitude());
                        //Add special marker in the found airport
                        //TODO consider using a bigger, different marker rather than a differently-colored one (or use a contrasting color, see Material Design color guidelines)
                        mMap.addMarker(new MarkerOptions().position(airportPosition).title("Marker in" + closestAirport.getCity().toString()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
                        //Move the camera to it
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(airportPosition));

                        //Got closest airport, find deals for it
                        API.getInstance().getDeals(closestAirport, DealsMapActivity.this, new NetworkRequestCallback<Deal[]>() {
                            @Override
                            public void execute(Context c, Deal[] param) {
                                deals = param;
                                List<Deal> orderedDeals = Arrays.asList(deals);
                                Collections.sort(orderedDeals);
                                LatLng aux;
                                float average = 0;
                                int i = 0;
                                for (Deal d : deals) {
                                    average += d.getPrice();
                                    i++;
                                }
                                average = average / i;
                                float colorValue;
                                for (Deal d : deals) {
                                    aux = new LatLng(d.getCity().getLatitude(), d.getCity().getLongitude());
                                    colorValue = (float) (orderedDeals.indexOf(d) * 120.0 / (orderedDeals.size() - (orderedDeals.size() == 1 ? 0 : 1)));
                                    mMap.addMarker(new MarkerOptions().position(aux).title(d.toString()).icon(BitmapDescriptorFactory.defaultMarker(colorValue)));
                                }
                            }
                        });
                    }
                }
            });
        } else {
            Toast.makeText(DealsMapActivity.this, "WHER U AT BOI I CAN'T FIND U", Toast.LENGTH_SHORT).show();    //TODO remove plz
            Log.w("VOLANDO", "Location is null");
        }
    }


    @Override
    public void onConnectionSuspended(int i) {
        //TODO wat do jier?
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //TODO wat do jier?
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        mGoogleApiClient.connect();     //TODO this may not be necessary on EVERY resume, check docs (also, do we need to check the user's location on EVERY app start?)
        super.onResume();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERM_LOCATION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Intent i = null;
        if (id == R.id.drawer_flights) {
            i = new Intent(this, FlightsActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else if (id == R.id.drawer_search) {
            i = new Intent(this, SearchActivity.class);
        } else if (id == R.id.drawer_map) {
            i = new Intent(this, DealsMapActivity.class);
        } else if (id == R.id.drawer_settings) {

        } else if (id == R.id.drawer_help) {

        }

        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        }
        //else, unrecognized option selected, close drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
