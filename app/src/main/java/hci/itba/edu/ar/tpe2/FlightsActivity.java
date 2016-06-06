package hci.itba.edu.ar.tpe2;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import hci.itba.edu.ar.tpe2.backend.network.API;
import hci.itba.edu.ar.tpe2.backend.data.City;
import hci.itba.edu.ar.tpe2.backend.data.Language;
import hci.itba.edu.ar.tpe2.backend.network.NetworkRequestCallback;
import hci.itba.edu.ar.tpe2.fragment.TextFragment;

public class FlightsActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, TextFragment.OnFragmentInteractionListener {

    private TextFragment textFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flights);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Add the text fragment
        if(savedInstanceState == null) {    //Creating for the first time
            textFragment = new TextFragment();
//            textFragment.setArguments(getIntent().getExtras());   //Pass it any parameters we might have received via Intent
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, textFragment).commit(); //Add it
        }
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textFragment.clear();
                API.loadCities(FlightsActivity.this, new NetworkRequestCallback<City[]>() {
                    @Override
                    public void execute(Context c, City[] cities) {
                        textFragment.appendText("\n" + cities.length + " cities available.\n");
                        for(City city : cities) {
                            Log.i("VOLANDO", city.toString());
                        }
                    }
                });
                API.loadLanguages(FlightsActivity.this, new NetworkRequestCallback<Language[]>() {
                    @Override
                    public void execute(Context c, Language[] langs) {
                        textFragment.appendText("\n" + langs.length + " languages available.\n");
                        for (Language l : langs) {
                            Log.i("VOLANDO", l.toString());
                        }
                    }
                });
                Snackbar.make(view, "Loading data cities and languages...", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.flights, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.drawer_flights) {
            // Handle the camera action
        } else if (id == R.id.drawer_search) {

        } else if (id == R.id.drawer_map) {

        } else if (id == R.id.drawer_settings) {

        } else if (id == R.id.drawer_help) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    /**
     * Needs to be implemented for Text Fragment to work.
     */
    public void onFragmentInteraction(Uri uri) {
        System.out.println("Some interaction happened with the TextFragment");
    }
}
