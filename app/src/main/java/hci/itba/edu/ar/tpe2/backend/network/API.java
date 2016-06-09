package hci.itba.edu.ar.tpe2.backend.network;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import hci.itba.edu.ar.tpe2.backend.FileManager;
import hci.itba.edu.ar.tpe2.backend.data.Airport;
import hci.itba.edu.ar.tpe2.backend.data.City;
import hci.itba.edu.ar.tpe2.backend.data.Flight;
import hci.itba.edu.ar.tpe2.backend.data.Deal;
import hci.itba.edu.ar.tpe2.backend.data.FlightStatus;
import hci.itba.edu.ar.tpe2.backend.data.Language;
import hci.itba.edu.ar.tpe2.backend.data.Place;
import hci.itba.edu.ar.tpe2.backend.data.Review;

/**
 * Singleton class used for making requests to the API.
 */
public class API {
    private static API instance = new API();
    private static DateFormat APIdateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static Gson gson = new Gson();
    private static final int DEFAULT_PAGE_SIZE = 30;

    private API() {}

    public enum Method {
        getcities, getlanguages, getairports, getflightstatus, getlastminuteflightdeals, getairlinereviews, reviewairline2
    }

    public enum Service {
        misc, geo, booking, review, status
    }

    public static final String API_BASE_URL = "http://eiffel.itba.edu.ar/hci/service4/",
                                LOG_TAG = "VOLANDO";

    /**
     * @return The singleton instance.
     */
    public static API getInstance() {
        return instance;
    }

    public void getAllFlights(String departureID, String arrivalID, /*Date */String departureDate, String airlineID, final Context context, final NetworkRequestCallback<List<Flight>> callback) {
        final Service service = Service.booking;
        final Bundle params = new Bundle();
        params.putString("method", "getonewayflights");
        params.putString("from", departureID);
        params.putString("to", arrivalID);
        params.putString("dep_date", departureDate/*APIdateFormat.format(departureDate)*/);
        if (airlineID != null) {
            params.putString("airline_id", airlineID);
        }
        //TODO OK to hardcode these?
        params.putString("adults", "1");
        params.putString("children", "0");
        params.putString("infants", "0");
        //Count flights to make sure we get all of them at once
        count(service, params, context, new NetworkRequestCallback<Integer>() {
            @Override
            public void execute(Context c, final Integer total) {
                if (total == 0) {    //No flights found, don't make a 2nd request
                    if (callback != null) {
                        callback.execute(context, Collections.EMPTY_LIST);
                    }
                    return;
                }
                params.putString("page_size", Integer.toString(total));
                new APIRequest(service, params) {
                    @Override
                    protected void successCallback(String result) {
                        if (callback == null) {
                            Log.d("VOLANDO", "Requested flights with no callback, useless network request =(");
                            return;
                        }
                        //Got all flights now, parse them
                        JsonArray data = gson.fromJson(result, JsonObject.class).getAsJsonArray("flights");
                        List<Flight> flights = new ArrayList<Flight>(total);
                        for (JsonElement flight : data) {
                            flights.add(Flight.fromJson(flight.getAsJsonObject()));
                        }
                        callback.execute(context, flights);
                    }

                    @Override
                    protected void errorCallback(String result) {
                        Log.w("VOLANDO", "Error searching flights:");
                        Log.w("VOLANDO", result);
                        callback.execute(context, Collections.EMPTY_LIST);
                    }
                }.execute();
            }
        });
    }

    //TODO use Flight object?
    public void getFlightStatus(String airlineId, int flightNum, final Context context, final NetworkRequestCallback<FlightStatus> callback) {
        Bundle params = new Bundle();
        params.putString("method", Method.getflightstatus.name());
        params.putString("airline_id", airlineId);
        params.putString("flight_number", Integer.toString(flightNum));
        new APIRequest(Service.status, params) {
            @Override
            protected void successCallback(String result) {
                JsonObject responseJson = API.gson.fromJson(result, JsonObject.class);
                FlightStatus status = FlightStatus.fromJson(responseJson.getAsJsonObject("status"));
                if (callback != null) {
                    callback.execute(context, status);
                }
            }
        }.execute();
    }

    public void getAllCities(final Context context, final NetworkRequestCallback<City[]> callback) {
        final Service service = Service.geo;
        final Bundle params = new Bundle();
        params.putString("method", Method.getcities.name());
        count(service, params, context, new NetworkRequestCallback<Integer>() {
            @Override
            public void execute(Context c, Integer cityCount) {
                //TODO handle null
                //Now that we have the total, fetch again with a page of size <cityCount>
                params.putString("page_size", Integer.toString(cityCount));
                new APIRequest(service, params) {
                    @Override
                    protected void successCallback(String result) {
                        int startIndex = result.indexOf("cities") + 8,   //Start at cities' [
                                endIndex = result.lastIndexOf(']') + 1;         //End at cities' ]
                        String data = result.substring(startIndex, endIndex);
                        City[] cities = gson.fromJson(data, City[].class);
                        //TODO stop saving them here, save them in the callback if anything
                        if(new FileManager(context).saveCities(cities)) {
                            if(callback != null) {
                                callback.execute(context, cities);
                            }
                        }
                        else {
                            Log.w(LOG_TAG, "Couldn't save cities.");
                        }
                    }
                }.execute();
            }
        });
    }

    public void getLanguages(final Context context, final NetworkRequestCallback<Language[]> callback) {
        Bundle params = new Bundle();
        params.putString("method", Method.getlanguages.name());
        new APIRequest(Service.misc, params) {
            @Override
            protected void successCallback(String result) {
                int startIndex = result.indexOf("languages") + 11,
                        endIndex = result.lastIndexOf(']') + 1;
                String data = result.substring(startIndex, endIndex);
                Language[] langs = gson.fromJson(data, Language[].class);
                //TODO stop saving them here, save them in the callback if anything
                if(new FileManager(context).saveLanguages(langs)) {
                    if(callback != null) {
                        callback.execute(context, langs);
                    }
                }
                else {
                    Log.w(LOG_TAG, "Couldn't save languages.");
                }
            }
        }.execute();
    }

    /**
     * Gets last-minute flight deals from the specified place.
     * @param from The place from which to search for deals, airport or city.
     * @param context The context under which to run the specified callback.
     * @param callback Function to run once network request is complete.
     */
    public void getDeals(Place from, final Context context, final NetworkRequestCallback<Deal[]> callback) {
        getDeals(from.getID(), context, callback);
    }

    /**
     * Gets last-minute flight deals from the specified origin.
     *
     * @param fromID Valid ID of origin (city or airport)
     * @param context The context under which to run the specified callback.
     * @param callback Function to run once network request is complete.
     */
    public void getDeals(String fromID, final Context context, final NetworkRequestCallback<Deal[]> callback) {
        Bundle params = new Bundle();
        params.putString("method", Method.getlastminuteflightdeals.name());
        params.putString("from", fromID);
        new APIRequest(Service.booking, params) {
            @Override
            protected void successCallback(String result) {
                Gson g = new Gson();
                JsonObject json = g.fromJson(result, JsonObject.class);
                Deal[] deals = g.fromJson(json.get("deals"), Deal[].class);
                if(callback != null) {
                    callback.execute(context, deals);
                }
            }
        }.execute();
    }

    public void getPageOfReviews(Flight flight, int pageNumber, int pageSize, final Context context, final NetworkRequestCallback<Review[]> callback) {
        Bundle params = new Bundle();
        params.putString("method", Method.getairlinereviews.name());
        params.putString("airline_id", flight.getAirlineID());
        params.putString("flight_number", Integer.toString(flight.getNumber()));
        params.putString("page_size", Integer.toString(pageSize));
        params.putString("page", Integer.toString(pageNumber));
        new APIRequest(Service.review, params) {
            @Override
            protected void successCallback(String result) {
                JsonObject json = gson.fromJson(result, JsonObject.class);
                JsonArray reviewsJson = json.getAsJsonArray("reviews");
                Review[] reviews = new Review[reviewsJson.size()];
                for (int i = 0; i < reviewsJson.size(); i++) {
                    reviews[i] = Review.fromJson(reviewsJson.get(i).getAsJsonObject());
                }
                if(callback != null) {
                    callback.execute(context, reviews);
                }
            }
        }.execute();
    }

    public void getAllReviews(Flight flight, final Context context, final NetworkRequestCallback<Review[]> callback) {
        final Bundle params = new Bundle();
        params.putString("method", Method.getairlinereviews.name());
        params.putString("airline_id", flight.getAirlineID());
        params.putString("flight_number", Integer.toString(flight.getNumber()));
        final Service service = Service.review;
        count(service, params, context, new NetworkRequestCallback<Integer>() {
            @Override
            public void execute(Context c, Integer count) {
                params.putString("page_size", Integer.toString(count));
                new APIRequest(service, params) {
                    @Override
                    protected void successCallback(String result) {
                        JsonObject json = gson.fromJson(result, JsonObject.class);
                        JsonArray reviewsJson = json.getAsJsonArray("reviews");
                        Review[] reviews = new Review[reviewsJson.size()];
                        for (int i = 0; i < reviewsJson.size(); i++) {
                            reviews[i] = Review.fromJson(reviewsJson.get(i).getAsJsonObject());
                        }
                        if(callback != null) {
                            callback.execute(context, reviews);
                        }
                    }
                }.execute();
            }
        });
    }

    public void submitReview(Review review, final Context context, final NetworkRequestCallback<Void> callback) {
        Bundle params = new Bundle();
        params.putString("method", Method.reviewairline2.name());
        params.putString("review", review.toJson());
        new APIRequest(Service.review, params) {
            @Override
            protected void successCallback(String result) {
                if(callback != null) {
                    callback.execute(context, null);
                }
            }
        }.execute();
    }

    /**
     * Gets the count of results returned by the specified query, or {@code null} if the query
     * doesn't specify a total, and passes it to the specified callback function.
     *
     * @param service The service the request is for.
     * @param requestParams Request parameters.
     * @param context Context to run the callback with.
     * @param callback Callback which will receive the total, or {@code null} if not specified in
     *                 the request's response.
     */
    public void count(Service service, Bundle requestParams, final Context context, final NetworkRequestCallback<Integer> callback) {
        new APIRequest(service, requestParams) {
            @Override
            protected void successCallback(String result) {
                if(callback != null) {
                    JsonObject json = gson.fromJson(result, JsonObject.class);
                    JsonElement totalObj = json.get("total");
                    Integer total = null;
                    if (totalObj != null) {
                        total = totalObj.getAsInt();
                    }
                    callback.execute(context, total);
                }
            }
        }.execute();
    }
}