package hci.itba.edu.ar.tpe2.backend.data;

import android.content.Context;
import android.util.Log;

import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import hci.itba.edu.ar.tpe2.backend.network.API;
import hci.itba.edu.ar.tpe2.backend.network.NetworkRequestCallback;

/**
 * Class used to hold persistent data used across various activities.
 */
public class PersistentData {
    private Context context;
    private final FileManager fileManager;

    private static Map<String, Airline> airlines;
    private static Map<String, Airport> airports;
    private static Map<String, City> cities;
    private static Map<String, Country> countries;
    private static Map<String, Currency> currencies;
    private static Map<Integer, FlightStatus> watchedStatuses;
    private static Map<String, Language> languages;


    public PersistentData(Context context) {
        this.context = context;
        this.fileManager = new FileManager(context);
    }

    /**
     * Creates a new instance without context. <b>WARNING:</b> Attempts to save data or to
     * {@link #init(NetworkRequestCallback, NetworkRequestCallback)} with the returned instance will throw Exceptions.
     *
     * @return A persistent data manager instance. If data has been initialized, it will be available.
     */
    public static PersistentData getContextLessInstance() {
        return new PersistentData(null);
    }

    public Map<String, City> getCities() {
        return cities;
    }

    private void setCities(Map<String, City> cities) {
        PersistentData.cities = cities;
    }

    public Map<String, Country> getCountries() {
        return countries;
    }

    private void setCountries(Map<String, Country> countries) {
        PersistentData.countries = countries;
    }

    public Map<String, Currency> getCurrencies() {
        return currencies;
    }

    private void setLanguages(Map<String, Language> languages) {
        PersistentData.languages = languages;
    }

    public Map<String, Language> getLanguages() {
        return languages;
    }

    private void setCurrencies(Map<String, Currency> currencies) {
        PersistentData.currencies = currencies;
    }

    public Map<String, Airport> getAirports() {
        return airports;
    }

    private void setAirports(Map<String, Airport> airports) {
        PersistentData.airports = airports;
    }

    public Map<String, Airline> getAirlines() {
        return airlines;
    }

    private void setAirlines(Map<String, Airline> airlines) {
        PersistentData.airlines = airlines;
    }

    public Map<Integer, FlightStatus> getWatchedStatuses() {
        return watchedStatuses;
    }

    private void setWatchedStatuses(Map<Integer, FlightStatus> watchedStatuses) {
        PersistentData.watchedStatuses = watchedStatuses;
    }

    public void watchStatus(FlightStatus status) {
        if (watchedStatuses == null) {
            throw new IllegalStateException("Watched statuses have not been set, can't watch status.");
        }
        int key = status.getFlight().getID();
        if (watchedStatuses.containsKey(key)) {
            Log.w("VOLANDO", "Status for " + status.getFlight().toString() + " already watched");
            return;
        }

        watchedStatuses.put(key, status);
        fileManager.saveWatchedStatuses(watchedStatuses);
    }

    public void updateStatus(FlightStatus newStatus) {
        Flight flight = newStatus.getFlight();
        if (!watchedStatuses.containsKey(flight.getID())) {
            throw new IllegalArgumentException("Status for " + flight.toString() + " not watched, can't replace.");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("Can't replace status with null status, use stopWatchingStatus() to remove it.");
        }
        watchedStatuses.put(flight.getID(), newStatus);
        fileManager.saveWatchedStatuses(watchedStatuses);
    }

    public void stopWatchingStatus(FlightStatus status) {
        if (watchedStatuses == null) {
            throw new IllegalStateException("Watched statuses have not been set, can't unwatch status.");
        }
        Flight flight = status.getFlight();
        if(!watchedStatuses.containsKey(flight.getID())) {
            throw new IllegalArgumentException("Status for " + flight.toString() + " not watched, can't unwatch.");
        }
        watchedStatuses.remove(flight.getID());
        fileManager.saveWatchedStatuses(watchedStatuses);
    }

    public boolean isInited() {
        return cities != null &&
                countries != null &&
//                followedFlights != null &&
//                currencies != null && TODO incorporate currencies?
                languages != null &&
                airports != null &&
                airlines != null &&
                watchedStatuses != null;
    }

    /**
     * Initializes necessary data. Some data depends on previous data (i.e. cities have countries
     * inside, airports have cities inside) the network requests are chained one after the other.
     *
     * @param doneCallback  Callback to run when done initializing everything
     * @param errorCallback Callback to run if any errors occur.
     */
    public void init(final NetworkRequestCallback<Void> doneCallback, NetworkRequestCallback<String> errorCallback) {
        //Watched statuses (done before the other data because it needs to be complete for either path to "finish")
        setWatchedStatuses(fileManager.loadWatchedStatuses());
        Log.d("VOLANDO", "Loaded " + watchedStatuses.size() + " watched statuses.");


        //Basic data (countries, cities, airports, airlines)
        if (fileManager.loadCountries().length == 0 || fileManager.loadCities().length == 0 || fileManager.loadAirports().length == 0 || fileManager.loadAirlines().length == 0) {
            NetworkRequestCallback<Void> megaCallback = new NetworkRequestCallback<Void>() {
                AtomicInteger requestsLeft = new AtomicInteger(3);  //Number of downloads needed to complete (countries etc., airlines, languages)

                @Override
                public void execute(Context c, Void param) {
                    if (requestsLeft.decrementAndGet() == 0) {   //Will wait until both (countries, cities, airports) and airlines requests complete
                        if (doneCallback != null) {
                            doneCallback.execute(c, null);
                        }
                    }
                }
            };
            downloadCountries(megaCallback, errorCallback);     //Will download cities and airports in chain
            downloadAirlines(megaCallback, errorCallback);      //Will download airlines independently of countries, cities, airports
            downloadLanguages(megaCallback, errorCallback);     //Will download languages independently
        } else {  //Persistent data found, load from files
            //Countries
            Country[] countries = fileManager.loadCountries();
            Map<String, Country> countriesMap = new HashMap<>(countries.length);
            for (Country c : countries) {
                countriesMap.put(c.getID(), c);
            }
            setCountries(countriesMap);
            Log.d("VOLANDO", "Loaded " + countries.length + " countries from local storage.");
            //Cities
            City[] cities = fileManager.loadCities();
            Map<String, City> citiesMap = new HashMap<>(cities.length);
            for (City c : cities) {
                citiesMap.put(c.getID(), c);
            }
            Log.d("VOLANDO", "Loaded " + cities.length + " cities from local storage.");
            setCities(citiesMap);
            //Airports
            Airport[] airports = fileManager.loadAirports();
            Map<String, Airport> airportsMap = new HashMap<>(airports.length);
            for (Airport a : airports) {
                airportsMap.put(a.getID(), a);
            }
            setAirports(airportsMap);
            Log.d("VOLANDO", "Loaded " + airports.length + " airports from local storage.");
            //Airlines
            Airline[] airlines = fileManager.loadAirlines();
            Map<String, Airline> airlinesMap = new HashMap<>(airlines.length);
            for (Airline a : airlines) {
                airlinesMap.put(a.getID(), a);
            }
            setAirlines(airlinesMap);
            //Languages
            Language[] languages = fileManager.loadLanguages();
            Map<String, Language> languagesMap = new HashMap<>(languages.length);
            for (Language l : languages) {
                languagesMap.put(l.getID(), l);
            }
            setLanguages(languagesMap);
            Log.d("VOLANDO", "Loaded " + languages.length + " languages from local storage.");
            //TODO currencies?
            doneCallback.execute(context, null);
        }
    }

    /**
     * Downloads countries from network and saves them. Calls {@link #downloadCities(NetworkRequestCallback, NetworkRequestCallback)} when done.
     */
    private void downloadCountries(final NetworkRequestCallback<Void> doneCallback, final NetworkRequestCallback<String> errorCallback) {
        API.getInstance().getAllCountries(
                context,
                new NetworkRequestCallback<Country[]>() {
                    @Override
                    public void execute(Context c, Country[] countries) {
                        Map<String, Country> la = new HashMap<>(countries.length);
                        for (Country country : countries) {
                            la.put(country.getID(), country);
                        }
                        if (fileManager.saveCountries(countries)) {
                            Log.d("VOLANDO", countries.length + " countries saved from network.");
                            setCountries(la);
                            //Done saving countries, get cities using the saved countries
                            downloadCities(doneCallback, errorCallback);
                        } else {
                            errorCallback.execute(c, "Couldn't save countries");
                        }
                    }
                },
                errorCallback);
    }

    /**
     * Downloads cities from network and saves them. Calls {@link #downloadAirports(NetworkRequestCallback, NetworkRequestCallback)} when done.
     */
    private void downloadCities(final NetworkRequestCallback<Void> doneCallback, final NetworkRequestCallback<String> errorCallback) {
        API.getInstance().getAllCities(
                context,
                new NetworkRequestCallback<City[]>() {
                    @Override
                    public void execute(Context c, City[] cities) {
                        Map<String, City> la = new HashMap<>(cities.length);
                        for (City city : cities) {
                            //City has an incomplete Country object stored. Replace it with the complete one
                            city.setCountry(getCountries().get(city.getCountry().getID()));
                            la.put(city.getID(), city);
                        }
                        if (fileManager.saveCities(cities)) {
                            Log.d("VOLANDO", cities.length + " cities loaded from network.");
                            setCities(la);
                            //Done saving cities, get airports using the saved cities
                            downloadAirports(doneCallback, errorCallback);
                        } else {
                            errorCallback.execute(c, "Couldn't save cities");
                        }
                    }
                },
                errorCallback);
    }

    /**
     * Downloads airports from network and saves them.
     */
    private void downloadAirports(final NetworkRequestCallback<Void> doneCallback, final NetworkRequestCallback<String> errorCallback) {
        API.getInstance().getAllAirports(
                context,
                new NetworkRequestCallback<Airport[]>() {
                    @Override
                    public void execute(Context c, Airport[] airports) {
                        Map<String, Airport> la = new HashMap<>(airports.length);
                        for (Airport airport : airports) {
                            //Airport has incomplete City object stored, replace it with the complete one
                            airport.setCity(getCities().get(airport.getCity().getID()));
                            la.put(airport.getID(), airport);
                        }
                        if (fileManager.saveAirports(airports)) {
                            Log.d("VOLANDO", airports.length + " airports loaded from network.");
                            setAirports(la);
                            //DONE with everything
                            doneCallback.execute(c, null);
                        } else {
                            errorCallback.execute(c, "Couldn't save airports");
                        }
                    }
                },
                errorCallback);
    }

    /**
     * Downloads airlines from network and saves them.
     */
    private void downloadAirlines(final NetworkRequestCallback<Void> doneCallback, final NetworkRequestCallback<String> errorCallback) {
//        String json = "{\"meta\":{\"uuid\":\"ca56b436-297f-4edc-b1b2-06475c784d85\",\"time\":\"11340.121ms\"},\"page\":1,\"page_size\":30,\"total\":13,\"airlines\":[{\"id\":\"AR\",\"name\":\"Aerolineas Argentinas\",\"logo\":\"http://eiffel.itba.edu.ar/hci/service4/images/AR.png\",\"taxes\":0.17,\"charges\":0.01,\"rating\":6.41},{\"id\":\"LA\",\"name\":\"Lan\",\"logo\":\"http://eiffel.itba.edu.ar/hci/service4/images/LA.png\",\"taxes\":0.19,\"charges\":0.06,\"rating\":6},{\"id\":\"8R\",\"name\":\"SOL\",\"logo\":\"http://eiffel.itba.edu.ar/hci/service4/images/8R.png\",\"taxes\":0.2,\"charges\":0.04,\"rating\":null},{\"id\":\"JJ\",\"name\":\"TAM\",\"logo\":\"http://eiffel.itba.edu.ar/hci/service4/images/JJ.png\",\"taxes\":0.17,\"charges\":0.03,\"rating\":4.33},{\"id\":\"BA\",\"name\":\"British Airways\",\"logo\":\"http://eiffel.itba.edu.ar/hci/service4/images/BA.png\",\"taxes\":0.2,\"charges\":0.05,\"rating\":5.77},{\"id\":\"AF\",\"name\":\"Air France\",\"logo\":\"http://eiffel.itba.edu.ar/hci/service4/images/AF.png\",\"taxes\":0.18,\"charges\":0.03,\"rating\":5.87},{\"id\":\"AZ\",\"name\":\"Alitalia\",\"logo\":\"http://eiffel.itba.edu.ar/hci/service4/images/AZ.png\",\"taxes\":0.15,\"charges\":0.03,\"rating\":7.04},{\"id\":\"AA\",\"name\":\"American Airlines\",\"logo\":\"http://eiffel.itba.edu.ar/hci/service4/images/AA.png\",\"taxes\":0.17,\"charges\":0.04,\"rating\":6.25},{\"id\":\"IB\",\"name\":\"Iberia\",\"logo\":\"http://eiffel.itba.edu.ar/hci/service4/images/IB.png\",\"taxes\":0.19,\"charges\":0.02,\"rating\":6.31},{\"id\":\"AM\",\"name\":\"Aeromexico\",\"logo\":\"http://eiffel.itba.edu.ar/hci/service4/images/AM.png\",\"taxes\":0.15,\"charges\":0.07,\"rating\":7.13},{\"id\":\"TA\",\"name\":\"Taca\",\"logo\":\"http://eiffel.itba.edu.ar/hci/service4/images/TA.png\",\"taxes\":0.16,\"charges\":0.06,\"rating\":5},{\"id\":\"CM\",\"name\":\"Copa\",\"logo\":\"http://eiffel.itba.edu.ar/hci/service4/images/CM.png\",\"taxes\":0.17,\"charges\":0.05,\"rating\":6.54},{\"id\":\"AV\",\"name\":\"Avianca\",\"logo\":\"http://eiffel.itba.edu.ar/hci/service4/images/AV.png\",\"taxes\":0.19,\"charges\":0.05,\"rating\":6}]}";
//        Gson g = new Gson();
//        JsonObject data = g.fromJson(json, JsonObject.class);
//        Airline[] airlines = g.fromJson(data.get("airlines"), Airline[].class);
//        Map<String, Airline> la = new HashMap<>(airlines.length);
//        for (Airline airline : airlines) {
//            la.put(airline.getID(), airline);
//        }
//        if (fileManager.saveAirlines(airlines)) {
//            Log.d("VOLANDO", airlines.length + " airlines loaded from network.");
//            setAirlines(la);
//            doneCallback.execute(context, null);
//        } else {
//            errorCallback.execute(context, "Couldn't save airlines");
//        }


        Log.w("VOLANDO", "Querying API for airlines");
        API.getInstance().getAllAirlines(
                context,
                new NetworkRequestCallback<Airline[]>() {
                    @Override
                    public void execute(Context c, Airline[] airlines) {
                        Map<String, Airline> la = new HashMap<>(airlines.length);
                        for (Airline airline : airlines) {
                            la.put(airline.getID(), airline);
                        }
                        if (fileManager.saveAirlines(airlines)) {
                            Log.d("VOLANDO", airlines.length + " airlines loaded from network.");
                            setAirlines(la);
                            doneCallback.execute(c, null);
                        } else {
                            errorCallback.execute(c, "Couldn't save airlines");
                        }
                    }
                },
                errorCallback);
    }

    /**
     * Downloads languages from network and saves them.
     */
    private void downloadLanguages(final NetworkRequestCallback<Void> doneCallback, final NetworkRequestCallback<String> errorCallback) {
        Log.w("VOLANDO", "Querying API for languages");
        API.getInstance().getLanguages(
                context,
                new NetworkRequestCallback<Language[]>() {
                    @Override
                    public void execute(Context c, Language[] languages) {
                        Map<String, Language> la = new HashMap<>(languages.length);
                        for (Language language : languages) {
                            la.put(language.getID(), language);
                        }
                        if (fileManager.saveLanguages(languages)) {
                            Log.d("VOLANDO", languages.length + " languages loaded from network.");
                            setLanguages(la);
                            doneCallback.execute(c, null);
                        } else {
                            errorCallback.execute(c, "Couldn't save languages");
                        }
                    }
                },
                errorCallback);
    }
}
