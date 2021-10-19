package com.sontme.mapit;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

class NearbyHandler {
    public static ConnectionLifecycleCallback connectionLifecycleCallback;
    public static EndpointDiscoveryCallback endpointDiscoveryCallback;
    public static PayloadCallback payloadCallback;
    public static String SERVICE_ID;
    public Context ctx;
    public static Strategy STRATEGY;

    public NearbyHandler(Context ctx, Strategy STRATEGY) {
        this.ctx = ctx;
        this.STRATEGY = STRATEGY;
        //SERVICE_ID = ctx.getPackageName();
        SERVICE_ID = "com.sontme.wirelessmapper";
        payloadCallback = new PayloadCallback() {
            @Override
            public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {
                Log.d("NEARBY_", "PAYLOAD RECEIVED " + s);
                Toast.makeText(ctx, "NEARBY RECEIVED PAYLOAD! " + s, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
                Log.d("NEARBY_", "PAYLOAD TRANSFER UPDATE " + s);
                Toast.makeText(ctx, "NEARBY PAYLOAD TRANSFER UPDATE: " + s + " / " + payloadTransferUpdate.getTotalBytes() + "/" + payloadTransferUpdate.getBytesTransferred(), Toast.LENGTH_LONG).show();
            }
        };
        connectionLifecycleCallback = new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(@NonNull String s, @NonNull ConnectionInfo connectionInfo) {
                Nearby.getConnectionsClient(ctx)
                        .acceptConnection(s, payloadCallback)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("NEARBY_", "SUCCESSFULLY CONNECTED");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                e.printStackTrace();
                                Log.d("NEARBY_", "FAILURE " + e.getMessage());
                            }
                        });
            }

            @Override
            public void onConnectionResult(@NonNull String s, @NonNull ConnectionResolution connectionResolution) {
                Log.d("NEARBY_", "CONNECTION RESULT " + s + connectionResolution.getStatus().getStatusMessage());
                switch (connectionResolution.getStatus().getStatusCode()) {
                    case ConnectionsStatusCodes.STATUS_OK:
                        // We're connected! Can now start sending and receiving data.
                        Nearby.getConnectionsClient(ctx).stopAdvertising();
                        Nearby.getConnectionsClient(ctx).stopDiscovery();

                        String string = "TesztString123_";
                        InputStream stream = new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
                        Payload payload = Payload.fromStream(stream);
                        Nearby.getConnectionsClient(ctx).sendPayload(s, payload);

                        break;
                    case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                        // The connection was rejected by one or both sides.
                        break;
                    case ConnectionsStatusCodes.STATUS_ERROR:
                        // The connection broke before it was able to be accepted.
                        break;
                }
            }

            @Override
            public void onDisconnected(@NonNull String s) {
                Log.d("NEARBY_", "DISCONNECTED " + s);
            }
        };
        endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(@NonNull String s, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
                Log.d("NEARBY_", "endpointFound: " + s + " _ " + discoveredEndpointInfo.getServiceId() + " _ " + discoveredEndpointInfo.getEndpointName());
                Nearby.getConnectionsClient(ctx)
                        .requestConnection(
                                discoveredEndpointInfo.getEndpointName(),
                                SERVICE_ID,
                                connectionLifecycleCallback)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("NEARBY_", "ENDPOINT CONNECTED");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d("NEARBY_", "error: " + e.getMessage());
                                e.printStackTrace();
                            }
                        });
            }

            @Override
            public void onEndpointLost(@NonNull String s) {
                Log.d("NEARBY_", "ENDPOINT LOST: " + s);
                Toast.makeText(ctx, "NEARBY ENDPOINT LOST: " + s, Toast.LENGTH_LONG).show();
            }
        };
    }

    public void startDiscovering() {
        Nearby.getConnectionsClient(ctx)
                .startDiscovery(
                        SERVICE_ID,
                        endpointDiscoveryCallback,
                        new DiscoveryOptions.Builder()
                                .setStrategy(CONSTANTS.PEERTOPEER_STRATEGY).build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("NEARBY_DISCOVERER_", "onSuccess");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("NEARBY_CO", "not connected");
                        e.printStackTrace();
                    }
                });
    }

    public void startAdvertising() {
        Nearby.getConnectionsClient(ctx)
                .startAdvertising(
                        Build.MODEL,
                        SERVICE_ID,
                        connectionLifecycleCallback,
                        new AdvertisingOptions.Builder()
                                .setStrategy(CONSTANTS.PEERTOPEER_STRATEGY).build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("NEARBY_", "advertising onsuccess");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("NEARBY_ADVERTISER_", "onFailure");
                        e.printStackTrace();
                    }
                });
    }
}

class CONSTANTS {
    public static Strategy PEERTOPEER_STRATEGY = Strategy.P2P_CLUSTER;
}

public class MainActivity extends Activity {

    public static int loaded_wifi = 0;
    public static int loaded_blue = 0;
    public static TextView txtLoaded;
    public static Location lastLocation;
    public static ArrayList<Location> locationList;
    public static MapView map;
    public static String httpResponse = "";
    public static String httpResponse_bl = "";
    public static GeoPoint home;
    public static ArrayList<GeoPoint> points;
    public static ArrayList<GeoPoint> points_bl;
    //public static ItemizedIconOverlay<OverlayItem> itemizedIconOverlay;
    public static RadiusMarkerClusterer clusterer_wifi;
    public static RadiusMarkerClusterer clusterer_bl;
    public static Drawable markerDraw_wifi;
    public static Drawable markerDraw_bl;
    public static IMapController mapController;
    public static LocationManager locationManager;
    public static LocationListener locationListener;
    //public static ConnectionLifecycleCallback cb;
    //public static EndpointDiscoveryCallback endpointDiscoveryCallback;

    @RequiresApi(api = Build.VERSION_CODES.DONUT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread thread_bl = new Thread() {
            @Override
            public void run() {
                getbl();
                super.run();
            }
        };


        Thread thread_wifi = new Thread() {
            @Override
            public void run() {
                getwifi();
                super.run();
            }
        };

        thread_wifi.start();
        thread_bl.start();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose one. Or two.");
        builder.setMessage("Which one you want to show on map?");
        builder.setCancelable(false);
        builder.setPositiveButton("WiFi", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                thread_wifi.start();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Bluetooth", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                thread_bl.start();
                dialog.dismiss();
            }
        });
        builder.setNeutralButton("Both", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                thread_wifi.start();
                thread_bl.start();
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        //alert.show();

        //region INIT
        try {
            Configuration.getInstance().load(MainActivity.this,
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);
        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });
        txtLoaded = findViewById(R.id.txtloaded);
        points = new ArrayList<GeoPoint>();
        points_bl = new ArrayList<GeoPoint>();

        map = findViewById(R.id.map);

        clusterer_wifi = new RadiusMarkerClusterer(getApplicationContext());
        clusterer_wifi.setIcon(colorize(BitmapFactory.decodeResource(getResources(),
                R.drawable.marker_cluster), Color.CYAN));
        clusterer_wifi.setRadius(120); // 60
        clusterer_bl = new RadiusMarkerClusterer(getApplicationContext());
        clusterer_bl.setIcon(colorize(BitmapFactory.decodeResource(getResources(),
                R.drawable.marker_cluster), Color.MAGENTA));
        clusterer_bl.setRadius(120); //60
        Bitmap blicon = BitmapFactory.decodeResource(getResources(),
                R.drawable.marker_cluster);
        Bitmap bbl = colorize(blicon, Color.GREEN);
        markerDraw_bl = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bbl,
                50, 50, true));
        markerDraw_bl.setAlpha(50);

        Bitmap wifiicon = BitmapFactory.decodeResource(getResources(),
                R.drawable.marker_cluster);
        markerDraw_wifi = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(wifiicon,
                50, 50, true));
        markerDraw_wifi.setAlpha(50);
        home = new GeoPoint(47.936291, 20.367531);
        mapController = map.getController();
        mapController.setCenter(home);
        mapController.setZoom(17);
        mapController.animateTo(home);
        //map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        AndroidNetworking.initialize(getApplicationContext());

        CMarker homeMarker = new CMarker(map);
        homeMarker.setInfoWindow(new BasicInfoWindow(R.layout.bonuspack_bubble, map));
        homeMarker.setPosition(home);
        homeMarker.setCtx(getApplicationContext());
        homeMarker.setActivity(MainActivity.this);
        homeMarker.setEnc("LOCALHOST");
        homeMarker.setTitle("Home");
        homeMarker.setTextIcon("texticon");
        Drawable homeIconDrawable = ContextCompat.getDrawable(getApplicationContext(),
                R.drawable.home);
        Bitmap homeBitmap = ((BitmapDrawable) homeIconDrawable).getBitmap();
        Bitmap homeBitmapColorized = colorize(homeBitmap,
                Color.RED);
        Drawable convertedIcon = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(homeBitmapColorized, 50, 50, true));

        //add table for icons


        homeMarker.setIcon(convertedIcon);
        homeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        homeMarker.setId("homemarker");
        map.getOverlays().add(homeMarker);
        map.invalidate();
        //endregion

        Button btn1 = findViewById(R.id.button1);
        Button btn2 = findViewById(R.id.button2);
        Button btn3 = findViewById(R.id.button3);
        Button btn4 = findViewById(R.id.button4);

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapController.animateTo(home);
                mapController.setCenter(home);
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                locationManager.requestSingleUpdate(
                        LocationManager.NETWORK_PROVIDER, locationListener, null);
                try {
                    GeoPoint pos = new GeoPoint(lastLocation);
                    mapController.animateTo(pos);
                    mapController.setCenter(pos);
                    mapController.zoomTo(22);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //map.getOverlays().clear();
                for (int i = 0; i < map.getOverlays().size(); i++) {
                    try {
                        Overlay overlay = map.getOverlays().get(i);
                        if (overlay instanceof CMarker && ((CMarker) overlay).getId().equals("homemarker")) {
                            Log.d("MARKER_TEST_", overlay.getClass() + " _ " + overlay.toString());
                            map.getOverlays().remove(overlay);
                            map.invalidate();
                        } else if (overlay instanceof Polyline && ((Polyline) overlay).getId().equals("line")) {
                            Log.d("MARKER_TEST_", overlay.getClass() + " _ " + overlay.toString());
                        } else if (overlay instanceof RadiusMarkerClusterer) {
                            Log.d("MARKER_TEST_", overlay.getClass() + " _ " + overlay.toString());
                            map.getOverlays().remove(overlay);
                            map.invalidate();
                        } else if (overlay instanceof ItemizedIconOverlay) {
                            Log.d("MARKER_TEST_", overlay.getClass() + " _ " + overlay.toString());
                            map.getOverlays().remove(overlay);
                            map.invalidate();
                        } else {
                            Log.d("MARKER_TEST_", overlay.getClass() + " _ " + overlay.toString());
                            map.getOverlays().remove(overlay);
                            map.invalidate();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLine();
            }
        });

        final String[] wifiCount = {""};
        final String[] blCount = {""};

        TextView rowCount = findViewById(R.id.txtcnt);
        rowCount.setText("Loading..");

        Handler handler_wifiCount = new Handler();
        handler_wifiCount.postDelayed(new Thread() {
            public void run() {
                try {
                    AndroidNetworking.get("https://sont.sytes.net/wifi/wifi_count.php")
                            .build()
                            .getAsString(new StringRequestListener() {
                                @Override
                                public void onResponse(String response) {
                                    Log.d("HTTP_CNT_", response);
                                    String count = getStringbetweenStrings(response, "_START_", "_END_");
                                    //rowCount.setText("#" + count);
                                    wifiCount[0] = count;
                                    rowCount.setText("#Blue: " + blCount[0] + " #WiFi: " + wifiCount[0]);
                                }

                                @Override
                                public void onError(ANError anError) {
                                    rowCount.setText("Error");
                                }
                            });
                } catch (Exception e) {
                    Log.d("HTTP_ERROR", e.getMessage());
                    rowCount.setText("Error");
                    e.printStackTrace();
                }
                handler_wifiCount.postDelayed(this, 5 * 1000);
            }
        }, 5000);

        Handler handler_blueCount = new Handler();
        handler_blueCount.postDelayed(new Thread() {
            public void run() {
                try {
                    AndroidNetworking.get("https://sont.sytes.net/wifi/bl_count.php")
                            .build()
                            .getAsString(new StringRequestListener() {
                                @Override
                                public void onResponse(String response) {
                                    Log.d("HTTP_CNT_", response);
                                    String count = getStringbetweenStrings(response, "_START_", "_END_");
                                    blCount[0] = count;
                                    rowCount.setText("#Blue: " + blCount[0] + " #WiFi: " + wifiCount[0]);
                                }

                                @Override
                                public void onError(ANError anError) {
                                    //rowCount.setText("Error");
                                }
                            });
                } catch (Exception e) {
                    Log.d("HTTP_ERROR", e.getMessage());
                    rowCount.setText("Error");
                    e.printStackTrace();
                }
                handler_blueCount.postDelayed(this, 5 * 1000);
            }
        }, 5000);

        ImageView img1 = findViewById(R.id.imageView1);
        ImageView img2 = findViewById(R.id.imageView2);
        ImageView img3 = findViewById(R.id.imageView3);
        ImageView img4 = findViewById(R.id.imageView4);
        TextView rowtxt1 = findViewById(R.id.rowtxt1);
        TextView rowtxt2 = findViewById(R.id.rowtxt2);
        TextView rowtxt3 = findViewById(R.id.rowtxt3);
        TextView rowtxt4 = findViewById(R.id.rowtxt4);

        Bitmap cluster_bl_bmp = colorize(BitmapFactory.decodeResource(getResources(),
                R.drawable.marker_cluster), Color.MAGENTA);
        Bitmap img1bmp = colorize(cluster_bl_bmp, Color.MAGENTA);
        Drawable img1drw = new BitmapDrawable(getResources(),
                Bitmap.createScaledBitmap(img1bmp,
                        50, 50, true));
        img1.setImageDrawable(img1drw);
        rowtxt1.setText("Bluetooth Cluster");

        Bitmap marker_bl_bmp = colorize(BitmapFactory.decodeResource(getResources(),
                R.drawable.marker_cluster), Color.GREEN);
        Bitmap img2bmp = colorize(marker_bl_bmp, Color.GREEN);
        Drawable img2drw = new BitmapDrawable(getResources(),
                Bitmap.createScaledBitmap(img2bmp,
                        50, 50, true));
        img2.setImageDrawable(img2drw);
        rowtxt2.setText("Bluetooth Marker");


        Bitmap cluster_w_bmp = colorize(BitmapFactory.decodeResource(getResources(),
                R.drawable.marker_cluster), Color.CYAN);
        Bitmap img3bmp = colorize(cluster_w_bmp, Color.CYAN);
        Drawable img3drw = new BitmapDrawable(getResources(),
                Bitmap.createScaledBitmap(img3bmp,
                        50, 50, true));
        img3.setImageDrawable(img3drw);
        rowtxt3.setText("WiFi Cluster");

        Bitmap img4bmp = BitmapFactory.decodeResource(getResources(),
                R.drawable.marker_cluster);
        Drawable img4drw = new BitmapDrawable(getResources(),
                Bitmap.createScaledBitmap(img4bmp,
                        50, 50, true));
        img4.setImageDrawable(img4drw);
        rowtxt4.setText("WiFi Marker");

        TableLayout tableLayout = findViewById(R.id.tablelayout);
        TableLayout.LayoutParams params = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        tableLayout.setGravity(Gravity.CENTER);
        tableLayout.setLayoutParams(params);

        locationList = new ArrayList<>();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                lastLocation = location;
                locationList.add(location);
                addLine();
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestSingleUpdate(
                LocationManager.NETWORK_PROVIDER, locationListener, null);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);


    }

    public static void addLine() {
        for (int i = 0; i < map.getOverlays().size(); i++) {
            Overlay overlay = map.getOverlays().get(i);
            if (overlay instanceof Polyline && ((Polyline) overlay).getId().equals("line")) {
                map.getOverlays().remove(overlay);
                map.invalidate();
            }
        }
        Polyline line = new Polyline(map);
        line.setId("line");
        line.setWidth(45);
        //line.setColor(Color.argb(80,255,0,0));
        line.setColor(getRandomColor());
        List<GeoPoint> pts = new ArrayList<>();
        for (Location loc : locationList) {
            pts.add(new GeoPoint(loc));
        }
        line.setGeodesic(true);
        line.setPoints(pts);
        map.getOverlayManager().add(line);
        map.invalidate();
    }

    public void getbl() {
        String url_bl = "https://sont.sytes.net/wifi/mapmarks_bl_today.php";
        //String url_bl = "https://sont.sytes.net/wifi/mapmarks_bl.php";
        AndroidNetworking.get(url_bl)
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        httpResponse_bl = response;
                        try {
                            parseResponse_bl(getApplicationContext(),
                                    MainActivity.this, response);
                        } catch (Exception e) {
                            Log.d("HTTP_ERROR_PARSE", e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        getbl();
                        Log.d("HTTP_ERROR_", anError.toString());
                        Log.d("HTTP_ERROR_", anError.getErrorBody());
                        Log.d("HTTP_ERROR_", anError.getErrorDetail());
                        Log.d("HTTP_ERROR_", anError.getResponse().message());
                        try {
                            Log.d("HTTP_ERROR_", anError.getResponse().body().string());
                        } catch (IOException e) {
                            Log.d("HTTP_ERROR_", "ERRBODY_" + e.getMessage());
                        }
                    }
                });
    }

    public void getwifi() {
        String url = "https://sont.sytes.net/wifi/mapmarks_today.php";
        //String url = "https://sont.sytes.net/wifi/mapmarks.php";
        AndroidNetworking.get(url)
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        httpResponse = response;
                        try {
                            parseResponse(getApplicationContext(), MainActivity.this, response);
                        } catch (Exception e) {
                            Log.d("HTTP_ERROR_PARSE", e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        getwifi();
                        Log.d("HTTP_ERROR_", anError.toString());
                        Log.d("HTTP_ERROR_", anError.getErrorBody());
                        Log.d("HTTP_ERROR_", anError.getErrorDetail());
                        Log.d("HTTP_ERROR_", anError.getResponse().message());
                        try {
                            Log.d("HTTP_ERROR_", anError.getResponse().body().string());
                        } catch (IOException e) {
                            Log.d("HTTP_ERROR_", "ERRBODY_" + e.getMessage());
                        }
                    }
                });
    }

    public static void parseResponse_bl(Context ctx, Activity activity, String response) {
        String[] lines = response.split("\r\n|\r|\n");
        Log.d("VALUES_LOADED_", "SIZE=" + lines.length + " -> " + response.length() / 1024 + " kb");
        final List<Overlay> overlays = map.getOverlays();
        for (String row : lines) {
            try {
                String address;
                String name;
                double latitude;
                double longitude;
                GeoPoint geoPoint;
                try {
                    name = getStringbetweenStrings(row, "_START_NAME_", "_END_NAME_");
                } catch (Exception e) {
                    name = "Error";
                }
                try {
                    address = getStringbetweenStrings(row, "_START_ADD_", "_END_ADD_");
                } catch (Exception e) {
                    address = "Error";
                }
                try {
                    latitude = Double.parseDouble(getStringbetweenStrings(row, "_START_LAT_", "_END_LAT_"));
                    longitude = Double.parseDouble(getStringbetweenStrings(row, "_START_LNG_", "_END_LNG_"));
                    geoPoint = new GeoPoint(latitude, longitude);
                } catch (Exception el) {
                    geoPoint = home;
                }
                CMarker marker = new CMarker(map);
                marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker, MapView mapView) {
                        Toast.makeText(ctx, "MARKER TAP: "
                                + marker.getTitle(), Toast.LENGTH_LONG).show();
                        return false;
                    }
                });
                marker.setEnc(address);
                marker.setActivity(activity);
                marker.setPosition(geoPoint);
                marker.setIcon(markerDraw_bl);
                marker.setTitle(name);
                clusterer_bl.add(marker);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        overlays.add(clusterer_bl);
        ArrayList<OverlayItem> a = new ArrayList<>();
        ItemizedIconOverlay<OverlayItem> itemizedIconOverlay = new ItemizedIconOverlay<OverlayItem>(ctx, a, new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
            @Override
            public boolean onItemSingleTapUp(int index, OverlayItem item) {
                Toast.makeText(ctx, "TAP: "
                        + item.getTitle() + "_" + index, Toast.LENGTH_LONG).show();
                return false;
            }

            @Override
            public boolean onItemLongPress(int index, OverlayItem item) {
                return false;
            }
        });
        loaded_blue = lines.length;
        txtLoaded.setText("Bluetooth: " + loaded_blue + " WiFi: " + loaded_wifi);
        map.getOverlays().add(itemizedIconOverlay);
        map.invalidate();
    }

    public static void parseResponse(Context ctx, Activity activity, String response) {
        String[] lines = response.split("\r\n|\r|\n");
        Log.d("VALUES_LOADED_", "SIZE=" + lines.length + " -> " + response.length() / 1024 + " kb");
        final List<Overlay> overlays = map.getOverlays();
        for (String row : lines) {
            try {
                String enc;
                String name;
                double latitude;
                double longitude;
                GeoPoint geoPoint;
                try {
                    name = getStringbetweenStrings(row, "_START_SSID_", "_END_SSID_");
                } catch (Exception e) {
                    name = "Error";
                }
                try {
                    enc = getStringbetweenStrings(row, "_START_ENC_", "_END_ENC_");
                } catch (Exception e) {
                    enc = "Error";
                }
                try {
                    latitude = Double.parseDouble(getStringbetweenStrings(row, "_START_LAT_", "_END_LAT_"));
                    longitude = Double.parseDouble(getStringbetweenStrings(row, "_START_LNG_", "_END_LNG_"));
                    geoPoint = new GeoPoint(latitude, longitude);
                } catch (Exception el) {
                    geoPoint = home;
                }
                CMarker marker = new CMarker(map);
                marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker, MapView mapView) {
                        Toast.makeText(ctx, "MARKER TAP: " + marker.getTitle(), Toast.LENGTH_LONG).show();
                        return false;
                    }
                });
                marker.setEnc(enc);
                marker.setActivity(activity);
                marker.setPosition(geoPoint);
                marker.setIcon(markerDraw_wifi);
                marker.setTitle(name);
                clusterer_wifi.add(marker);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        overlays.add(clusterer_wifi);
        ArrayList<OverlayItem> a = new ArrayList<>();
        ItemizedIconOverlay<OverlayItem> itemizedIconOverlay = new ItemizedIconOverlay<OverlayItem>(ctx, a, new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
            @Override
            public boolean onItemSingleTapUp(int index, OverlayItem item) {
                Toast.makeText(ctx, "TAP: " + item.getTitle() + "_" + index, Toast.LENGTH_LONG).show();
                return false;
            }

            @Override
            public boolean onItemLongPress(int index, OverlayItem item) {
                return false;
            }
        });
        loaded_wifi = lines.length;
        txtLoaded.setText("Bluetooth: " + loaded_blue + " WiFi: " + loaded_wifi);
        map.getOverlays().add(itemizedIconOverlay);
        map.invalidate();
    }

    public Bitmap colorize(Bitmap srcBmp, int dstColor) {
        int width = srcBmp.getWidth();
        int height = srcBmp.getHeight();
        float srcHSV[] = new float[3];
        float dstHSV[] = new float[3];
        Bitmap dstBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int pixel = srcBmp.getPixel(col, row);
                int alpha = Color.alpha(pixel);
                Color.colorToHSV(pixel, srcHSV);
                Color.colorToHSV(dstColor, dstHSV);
                dstHSV[2] = srcHSV[2];  // value
                dstBitmap.setPixel(col, row, Color.HSVToColor(alpha, dstHSV));
            }
        }
        return dstBitmap;
    }

    public static String getStringbetweenStrings(String gotString, String whatStringStart, String whatStringEnd) {
        String result = "";
        try {
            result =
                    gotString.substring(
                            gotString.indexOf(whatStringStart) + whatStringStart.length()
                    );
            result =
                    result.substring(
                            0,
                            result.indexOf(whatStringEnd));
            return result;
        } catch (Exception e) {
            //e.printStackTrace();
            return "";
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            permissionsToRequest.add(permissions[i]);
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    1);
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    1);
        }
    }

    public static String locationToStringAddress(Context ctx, Location location) {
        String strAdd = "";
        Geocoder geocoder = new Geocoder(ctx, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null) {
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder();

                for (int i = 0; i <= returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("");
                }
                strAdd = strReturnedAddress.toString();
            }
        } catch (Exception e) {
            Log.d("LOCATION CONVERSION Error_", e.toString());
            e.printStackTrace();
            //return "Unknown";
            return locationToStringAddress(ctx, location);
        }
        return strAdd;
    }

    public static int getRandomColor() {
        Random rnd = new Random();
        return Color.argb(255 / 2, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
    }
}

class CMarker extends Marker {
    String enc;
    Context ctx;
    Activity activity;

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void setCtx(Context applicationContext) {
        this.ctx = applicationContext;
    }

    public String getEnc() {
        return enc;
    }

    public void setEnc(String mac) {
        String avoidNull;
        try {
            if (mac == null || mac.length() < 0) {
                avoidNull = "Error";
            } else {
                avoidNull = mac;
            }
            this.enc = avoidNull;
        } catch (Exception e) {
            this.enc = "Error";
        }
    }

    public CMarker(MapView mapView) {
        super(mapView);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event, MapView mapView) {
        /*AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
        alertDialog.setTitle("Extra Information");
        alertDialog.setMessage(enc);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();*/
        return true;
        //return super.onSingleTapConfirmed(event, mapView);
    }

    public CMarker(MapView mapView, String enc) {
        super(mapView);
        this.enc = enc;
    }


}