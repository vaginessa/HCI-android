package hci.itba.edu.ar.tpe2.backend.data;

import android.content.Context;
import android.util.Log;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hci.itba.edu.ar.tpe2.backend.data.Airline;
import hci.itba.edu.ar.tpe2.backend.data.Airport;
import hci.itba.edu.ar.tpe2.backend.data.City;
import hci.itba.edu.ar.tpe2.backend.data.Country;
import hci.itba.edu.ar.tpe2.backend.data.Flight;
import hci.itba.edu.ar.tpe2.backend.data.FlightStatus;
import hci.itba.edu.ar.tpe2.backend.data.Language;
import hci.itba.edu.ar.tpe2.backend.network.API;

/**
 * Class used for storing essential data for proper app functionality (e.g. cities, airports,
 * languages, currencies) in the device's internal storage.
 */
public class FileManager {
    public enum StorageFile {CITIES, COUNTRIES, AIRPORTS, LANGUAGES, CURRENCIES, AIRLINES, STATUSES}

    private Context context;

    public FileManager(Context c) {
        this.context = c;
    }

    public boolean saveCountries(Country[] countries) {
        try {
            return saveObjects(countries, StorageFile.COUNTRIES);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Country[] loadCountries() {
        List<Country> result = new ArrayList<>();
        try {
            loadObjects(context, StorageFile.COUNTRIES, result);
        } catch (IOException e) {
            e.printStackTrace();
            return new Country[0];
        }
        return result.toArray(new Country[0]);
    }

    public boolean saveCities(City[] cities) {
        try {
            return saveObjects(cities, StorageFile.CITIES);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public City[] loadCities() {
        List<City> result = new ArrayList<>();
        try {
            loadObjects(context, StorageFile.CITIES, result);
        } catch (IOException e) {
            e.printStackTrace();
            return new City[0];
        }
        return result.toArray(new City[0]);     //Empty array instead of length-sized array http://stackoverflow.com/a/4042464/2333689
    }

    public boolean saveLanguages(Language[] cities) {
        try {
            return saveObjects(cities, StorageFile.LANGUAGES);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Language[] loadLanguages() {
        List<Language> result = new ArrayList<>();
        try {
            loadObjects(context, StorageFile.LANGUAGES, result);
        } catch (IOException e) {
            e.printStackTrace();
            return new Language[0];
        }
        return result.toArray(new Language[0]);
    }

    public boolean saveAirports(Airport[] airports) {
        try {
            return saveObjects(airports, StorageFile.AIRPORTS);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Airport[] loadAirports() {
        List<Airport> result = new ArrayList<>();
        try {
            loadObjects(context, StorageFile.AIRPORTS, result);
        } catch (IOException e) {
            e.printStackTrace();
            return new Airport[0];
        }
        return result.toArray(new Airport[]{});
    }

    public boolean saveAirlines(Airline[] airlines) {
        try {
            return saveObjects(airlines, StorageFile.AIRLINES);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Airline[] loadAirlines() {
        List<Airline> result = new ArrayList<>();
        try {
            loadObjects(context, StorageFile.AIRLINES, result);
        } catch (IOException e) {
            e.printStackTrace();
            return new Airline[0];
        }
        return result.toArray(new Airline[]{});
    }

    public boolean saveWatchedStatuses(Map<Integer, FlightStatus> statuses) {
        try {
            return saveObjects(statuses.values().toArray(new FlightStatus[0]), StorageFile.STATUSES);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Map<Integer, FlightStatus> loadWatchedStatuses() {
        Map<Integer, FlightStatus> result = new HashMap<>();
        try {
            List<FlightStatus> loadedObjects = new ArrayList<>();
            loadObjects(context, StorageFile.STATUSES, loadedObjects);
            for(FlightStatus status : loadedObjects) {
                result.put(status.getFlight().getID(), status);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>(); //Collections.EMPTY_MAP does not support put()
        }
        return result;
    }

    private boolean saveObjects(Serializable[] objects, StorageFile destFile) throws IOException {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = context.openFileOutput(destFile.name().toLowerCase(), Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);
            oos.writeInt(objects.length);
            for(Serializable o : objects) {
                oos.writeObject(o);
            }
//            Log.i(API.LOG_TAG, "Saved " + objects.length + " objects to " + destFile);
        } catch (FileNotFoundException e) {
            Log.wtf(API.LOG_TAG, "Wut, " + destFile + " file not found, even though we're creating it...");
            return false;
        }
        finally {
            if(fos != null) {
                fos.close();
            }
            if(oos != null) {
                oos.close();
            }
        }
        return true;
    }

    private static <T> boolean loadObjects(Context context, StorageFile srcFile, Collection<T> dest) throws IOException {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            File f = new File(context.getFilesDir(), srcFile.name().toLowerCase());
            if (!f.exists()) {
                f.createNewFile();
            }
            fis = new FileInputStream(f);
            ois = new ObjectInputStream(fis);
            int numObjects = ois.readInt();
            for(int i = 0; i < numObjects; i++) {
                dest.add((T) ois.readObject());
            }
//            Log.i(API.LOG_TAG, "Read " + numObjects + " objects from " + srcFile);
        } catch (FileNotFoundException e) {
            Log.wtf(API.LOG_TAG, "Wut, " + srcFile + " file not found, even though we're creating it...");
            return false;
        } catch (EOFException e) {
//            Log.d("VOLANDO", "Empty " + srcFile.name() + " file.");
            return false;
        } catch (ClassNotFoundException e) {
            Log.e(API.LOG_TAG, "Error reading objects from " + srcFile + ": " + e.getMessage());
            return false;
        } finally {
            if(fis != null) {
                fis.close();
            }
            if(ois != null) {
                ois.close();
            }
        }
        return true;
    }
}
