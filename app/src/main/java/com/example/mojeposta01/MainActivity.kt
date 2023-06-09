package com.example.mojeposta01
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Point
import android.graphics.RectF
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.ContextCompat
import com.google.firebase.database.core.Context
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleOpacity
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.textAllowOverlap
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.textColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.textField
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.textIgnorePlacement
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.textSize
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Reader
import java.io.StringWriter
import java.io.Writer
import java.net.URISyntaxException


class MainActivity : AppCompatActivity(),PermissionsListener {
    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    private lateinit var mapboxMap: MapboxMap
    private var mapView: MapView? = null
    private var button: Button? = null
    private var informace: ConstraintLayout? = null
    private var pomocnik: Guideline? = null
    //sdk pridani


    private fun getMapTilerKey(): String? {
        return "yuHbH532OVH3MntnEzcx"
    }
    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Create and customize the LocationComponent's options
            val customLocationComponentOptions = LocationComponentOptions.builder(this)
                .trackingGesturesManagement(true)
                .build()

            val locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(this, loadedMapStyle)
                    .locationComponentOptions(customLocationComponentOptions)
                    .build()

            // Get an instance of the LocationComponent and then adjust its settings
            mapboxMap.locationComponent.apply {

                // Activate the LocationComponent with options
                activateLocationComponent(locationComponentActivationOptions)

                // Enable to make the LocationComponent visible
                isLocationComponentEnabled = true

                // Set the LocationComponent's camera mode
                cameraMode = CameraMode.NONE

                // Set the LocationComponent's render mode
                renderMode = RenderMode.COMPASS
            }
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    private fun addSourcesAndLayers(style: Style) {
        try {
            val raw = resources.openRawResource(R.raw.postygps)
            val writer: Writer = StringWriter()
            val buffer = CharArray(1024)
            raw.use { rawData ->
                val reader: Reader = BufferedReader(InputStreamReader(rawData, "UTF-8"))
                var n: Int
                while (reader.read(buffer).also { n = it } != -1) {
                    writer.write(buffer, 0, n)
                }
            }

            val jsonString = writer.toString()

            style.addSource(
                GeoJsonSource("POSTY",
                    jsonString,
                    GeoJsonOptions()
                        .withCluster(true)
                        .withClusterMaxZoom(12)
                        .withClusterRadius(50))
            )
            var circleLayer = CircleLayer("CIRCLE", "POSTY")
            circleLayer.setProperties(
                circleColor(
                    ContextCompat.getColor(
                        this,
                        android.R.color.holo_blue_dark
                    )
                ),
                circleRadius(18f),
                circleOpacity(0.65f)
            )
            circleLayer.withFilter(Expression.has("cluster"))

            style.addLayer(circleLayer)

            val countLayer = SymbolLayer("POCET", "POSTY")
            countLayer.setProperties(
                textField(Expression.toString(Expression.get("point_count"))),
                textSize(10f),
                textColor(Color.BLACK),
                textIgnorePlacement(true),
                textAllowOverlap(true))

            style.addLayer(countLayer)

            //
            val poiDrawable = getDrawable(R.drawable.dobre)
            style.addImage("IKONAPOBOCKA", poiDrawable!!)

            val postaLayer = SymbolLayer("POBOCKA", "POSTY")
            postaLayer.setProperties(
                iconImage("IKONAPOBOCKA"),
                iconSize(0.33f)
            )
            postaLayer.withFilter(
                Expression.all(
                    Expression.not(Expression.has("cluster")),
                    Expression.not(Expression.eq(Expression.get("ZRUSENA"), 1))
                )
            )
            style.addLayer(postaLayer)

            val poiDrawableZrusena = getDrawable(R.drawable.zrusene)
            style.addImage("IKONAPOBOCKAZRUSENA", poiDrawableZrusena!!)

            val zrusenaPostaLayer = SymbolLayer("POBOCKA_ZRUSENA", "POSTY")
            zrusenaPostaLayer.setProperties(
                iconImage("IKONAPOBOCKAZRUSENA"),
                iconSize(0.33f)
            )
            zrusenaPostaLayer.withFilter(
                Expression.all(
                    Expression.not(Expression.has("cluster")),
                    Expression.eq(Expression.get("ZRUSENA"), 1)
                )
            )
            style.addLayer(zrusenaPostaLayer)


        } catch (exception: URISyntaxException) {
            Log.e("MainActivity", "Check the URL " + exception.message)
        }
    }
    private fun addRoute(start:LatLng, end:LatLng)
    {
        RoutingService(this).findRoute(start, end) { featureColection ->
            val style = mapboxMap.style ?: return@findRoute
            if(style.getSource("navigacniCara")==null) {
                style.addSource(
                    GeoJsonSource(
                        "navigacniCara",featureColection)
                )

                val layer = LineLayer("cara", "navigacniCara")
                    .withProperties(
                        PropertyFactory.lineColor("rgb(0,0,255)"),
                        PropertyFactory.lineWidth(2f),
                        PropertyFactory.lineGapWidth(2f)
                    )
                style.addLayer(layer)
            }
            else
            {
                style.getSourceAs<GeoJsonSource>("navigacniCara")?.setGeoJson(featureColection)
            }
        }
    }
    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mapTilerKey = getMapTilerKey()
        validateKey(mapTilerKey)
        val styleUrl = "https://api.maptiler.com/maps/streets-v2/style.json?key=${mapTilerKey}";

        // Get the MapBox context
        Mapbox.getInstance(this, null)

        // Set the map view layout
        setContentView(R.layout.activity_main)

        button= findViewById(R.id.button)
        button?.setOnClickListener {
            val location = mapboxMap.locationComponent.lastKnownLocation ?: return@setOnClickListener
            val latlng = LatLng(location.latitude, location.longitude)
            mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng,16.0))
        }
        pomocnik = findViewById(R.id.pomocnik) as? Guideline
        informace = findViewById(R.id.infoview) as? ConstraintLayout
        pomocnik?.setGuidelinePercent(1f)
        // Create map view
        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync { map ->
            mapboxMap = map
            mapboxMap.addOnMapClickListener {
                handleMapViewClick(it)
                return@addOnMapClickListener true
            }
            // Set the style after mapView was loaded
            map.setStyle(styleUrl) {
                map.uiSettings.setAttributionMargins(15, 0, 0, 15)
                // Set the map view center
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(49.8038, 15.4749))
                    .zoom(5.3)
                    .build()
                enableLocationComponent(it)
                addSourcesAndLayers(it)

                val searchEditText = findViewById<EditText>(R.id.searchEditText)
                searchEditText.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH) {
                        val adresaProHledani = searchEditText.text.toString()

                        GeocodingService(this).geocodeAddress(adresaProHledani) { vysledek ->
                            vysledek?.let { lokace ->
                                val zoomLevel = 12.0
                                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(lokace, zoomLevel)
                                mapboxMap.animateCamera(cameraUpdate)
                            }
                        }

                        return@setOnEditorActionListener true
                    }
                    false
                }

            }
        }
    }

    private fun handleMapViewClick(latLng: LatLng) {
        val centerPoint = mapboxMap.projection.toScreenLocation(latLng)
        val distanceTolerance = 0.5f

        val searchArea = RectF(
            centerPoint.x - distanceTolerance,
            centerPoint.y - distanceTolerance,
            centerPoint.x - distanceTolerance,
            centerPoint.y - distanceTolerance
        )
        val features = mapboxMap.queryRenderedFeatures(searchArea,"POBOCKA", "POBOCKA_ZRUSENA")

        features.firstOrNull()?.let {
            val properties = it.properties()
                ?: return

            val nazev = properties.get("NAZEV").asString
            val adresa = properties.get("ADRESA").asString


            //Toast.makeText(this, "$nazev $adresa", Toast.LENGTH_LONG).show()
            val nazevTextView = findViewById<TextView>(R.id.nazevpobocky)
            nazevTextView.text = nazev
            val adresaTextView = findViewById<TextView>(R.id.adresapobocky)
            adresaTextView.text = adresa
            if(!properties.get("ZRUSENA").isJsonNull)
                adresaTextView.text = adresaTextView.text.toString() + "\nPobočka bude uzavřena 1. 7. 2023"

            pomocnik?.setGuidelinePercent(0.8f)
            mapboxMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
            val gpsPoloha = mapboxMap.locationComponent.lastKnownLocation
            val polohaPobocky = it.geometry() as? com.mapbox.geojson.Point
            if (gpsPoloha != null && polohaPobocky != null)
            {
                addRoute(
                    LatLng(gpsPoloha.latitude, gpsPoloha.longitude),
                    LatLng(polohaPobocky.latitude(), polohaPobocky.longitude())
                )
            }
        } ?: kotlin.run { pomocnik?.setGuidelinePercent(1f) }
    }
    private fun validateKey(mapTilerKey: String?) {
        if (mapTilerKey == null) {
            throw Exception("Failed to read MapTiler key from info.plist")
        }
        if (mapTilerKey.toLowerCase() == "placeholder") {
            throw Exception("Please enter correct MapTiler key in module-level gradle.build file in defaultConfig section")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(this, R.string.prms, Toast.LENGTH_LONG).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted){
            enableLocationComponent(mapboxMap.style!!)
        }
    }
}
