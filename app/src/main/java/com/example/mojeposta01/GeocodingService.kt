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
import com.mapbox.mapboxsdk.geometry.LatLng


class GeocodingService(private val context: Context) {
    private val objectMapper = ObjectMapper()

    fun geocodeAddress(address: String, callback: (LatLng?) -> Unit) {
        val encodedAddress = java.net.URLEncoder.encode(address, "UTF-8")
        val url = "https://nominatim.openstreetmap.com/search?q=$encodedAddress&country=Czechia&format=geojson"

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener { response ->
                val geoJson = response.toString()
                val geoJsonData: JsonNode = objectMapper.readTree(geoJson)
                val features = geoJsonData["features"]
                val firstFeature = features.firstOrNull() ?: run {
                    callback(null)
                    return@Listener
                }
                val geometryNode = firstFeature["geometry"] as JsonNode
                val type = (geometryNode["type"] as? TextNode)?.textValue()
                if (type == "Point") {
                    val coordinates = geometryNode["coordinates"] as ArrayNode
                    val longitude = (coordinates[0] as DoubleNode).doubleValue()
                    val latitude = (coordinates[1] as DoubleNode).doubleValue()
                    val location = LatLng(latitude, longitude)
                    callback(location)
                } else {
                    callback(null)
                    return@Listener
                }
            }
        ) { error ->
            Log.e("GeocodingService", "Error geocoding address: ${error.message}")
            callback(null)
        }

        // Add the request to the Volley request queue
        Volley.newRequestQueue(context).add(request)
    }
}