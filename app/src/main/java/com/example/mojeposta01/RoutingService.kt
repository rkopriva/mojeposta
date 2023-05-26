package com.example.mojeposta01

import android.util.Log
import android.content.Context
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.TextNode
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng


class RoutingService(private val context: Context) {
    private val objectMapper = ObjectMapper()

    fun findRoute(start: LatLng, end: LatLng, callback: (FeatureCollection) -> Unit) {
        val url = "https://api.openrouteservice.org/v2/directions/driving-car?api_key=5b3ce3597851110001cf6248c365994dd8414d868e024567d160ba87&start=${start.longitude},${start.latitude}&end=${end.longitude},${end.latitude}"

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener { response ->
                val geoJson = response.toString()
                val collection = FeatureCollection.fromJson(geoJson) ?: kotlin.run {
                    callback(FeatureCollection.fromFeatures(emptyArray()))
                    return@Listener
                }
                callback(collection)
            }
        ) { error ->
            Log.e("RoutingsService", "Error routing ${error.message}")
            callback(FeatureCollection.fromFeatures(emptyArray()))
        }

        // Add the request to the Volley request queue
        Volley.newRequestQueue(context).add(request)
    }
}