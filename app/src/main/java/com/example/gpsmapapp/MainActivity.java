package com.example.gpsmapapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";

    // Código de solicitud de permisos de ubicación
    private static final int LOCATION_REQUEST_CODE = 101;

    // Coordenadas por defecto para Ovalle, Chile
    private static final double OVALLE_LATITUDE = -30.6011;
    private static final double OVALLE_LONGITUDE = -71.1990;
    private static final float DEFAULT_ZOOM = 15.0f;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialización de FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtención del SupportMapFragment e inicialización asíncrona del mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Habilitar controles de zoom en el mapa
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Verificar permisos de ubicación al estar listo el mapa
        checkLocationPermissionAndGetLocation();
    }

    /**
     * Verifica los permisos de ubicación. Si están concedidos, obtiene la ubicación.
     * Si no lo están, solicita los permisos en tiempo de ejecución.
     */
    private void checkLocationPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                if (mMap != null) {
                    mMap.setMyLocationEnabled(true);
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Error al habilitar capa de ubicación", e);
            }
            getDeviceLocation();
        } else {
            // Solicitar permisos de ubicación en tiempo de ejecución (LOCATION_REQUEST_CODE = 101)
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_REQUEST_CODE
            );
        }
    }

    /**
     * Intenta obtener la última ubicación conocida del dispositivo (getLastLocation).
     * Anima la cámara hacia las coordenadas con un zoom de 15.0f y agrega el marcador "Ubicación Actual".
     * Si la ubicación retorna nula, establece la ubicación fallback en Ovalle, Chile.
     */
    private void getDeviceLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            setFallbackLocation();
            return;
        }

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // Ubicación obtenida exitosamente
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

                        // Animar la cámara hacia la ubicación actual con zoom 15.0f
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM));

                        // Agregar marcador en la ubicación actual
                        mMap.addMarker(new MarkerOptions()
                                .position(currentLocation)
                                .title("Ubicación Actual"));
                    } else {
                        // Ubicación retorna nula
                        setFallbackLocation();
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Error al obtener la última ubicación", e);
                    setFallbackLocation();
                });
    }

    /**
     * Establece como fallback las coordenadas por defecto de Ovalle, Chile (-30.6011, -71.1990)
     * con zoom de 15.0f y marcador "Ovalle, Chile".
     */
    private void setFallbackLocation() {
        LatLng ovalle = new LatLng(OVALLE_LATITUDE, OVALLE_LONGITUDE);

        // Animar cámara hacia Ovalle con zoom 15.0f
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ovalle, DEFAULT_ZOOM));

        // Agregar marcador "Ovalle, Chile"
        mMap.addMarker(new MarkerOptions()
                .position(ovalle)
                .title("Ovalle, Chile"));

        Toast.makeText(this, "Mostrando ubicación por defecto: Ovalle, Chile", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permisos concedidos
                checkLocationPermissionAndGetLocation();
            } else {
                // Permisos denegados: se aplica el fallback a Ovalle, Chile
                Toast.makeText(this, "Permiso denegado. Cargando ubicación en Ovalle, Chile.", Toast.LENGTH_SHORT).show();
                setFallbackLocation();
            }
        }
    }
}
