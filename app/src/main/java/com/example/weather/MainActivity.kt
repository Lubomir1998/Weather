package com.example.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.weather.databinding.ActivityMainBinding
import com.google.android.gms.location.*
import org.json.JSONObject
import java.lang.Exception
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    lateinit var lat: String
    lateinit var lon: String

    val API = "747671bc849ba7f8b4f93260601d43df"
    val PERMISSION_ID = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

     // set the view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    // ---------------------------------------



        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getLastLocation()

       binding.swipeRefresh.setOnRefreshListener {
           getLastLocation()
           binding.swipeRefresh.isRefreshing = false
       }

    }



    private val mLocationCallback = object: LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation

            lat = mLastLocation.latitude.toString()
            lon = mLastLocation.longitude.toString()

            WeatherTask().execute()

        }
    }

    private fun checkPermissions(): Boolean {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ){
                return true
            }

            return false
    }

    private fun requestPermission(){
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_ID)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode == PERMISSION_ID){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getLastLocation()
            }
        }
    }

    private fun isLocationEnabled(): Boolean{
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermissions")
    private fun getLastLocation() {

        if(checkPermissions()){
            if(isLocationEnabled()){


                mFusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if(location != null){
                       lat = location.latitude.toString()
                       lon = location.longitude.toString()

                        WeatherTask().execute()

//                        binding.cityTextView.text = location.latitude.toString()
//                        binding.updatedAttextview.text = location.longitude.toString()
                    }
                    else{
                        requestNewLocationData()

                    }
                }
            }
            else{
                Toast.makeText(this, "Turn on location", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }

        }
        else{
            requestPermission()
        }

    }

    @SuppressLint("MissingPermissions")
    private fun requestNewLocationData(){
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())

    }


    inner class WeatherTask(): AsyncTask<String, Void, String>(){

        override fun onPreExecute() {
            super.onPreExecute()

            binding.mainContainer.visibility = View.GONE
            binding.loader.visibility = View.VISIBLE
            binding.errorText.visibility = View.GONE
        }

        override fun doInBackground(vararg params: String?): String? {

            var response: String?

                try {
                    response =
                        URL("https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&units=metric&appid=$API").readText(
                            Charsets.UTF_8
                        )
                } catch (e: Exception) {
                    response = null
                }

                return response
            }


        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            try {
                val jsonObj = JSONObject(result)

                val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
                val main = jsonObj.getJSONObject("main")
                val wind = jsonObj.getJSONObject("wind")
                val sys = jsonObj.getJSONObject("sys")

                val city = jsonObj.getString("name")
                val country = sys.getString("country")
                val address = "$city, $country"

                val weatherDescription = weather.getString("description").capitalize()
                val updatedAt = jsonObj.getLong("dt")
                val windSpeed = wind.getString("speed")
                val pressure = main.getString("pressure")
                val humidity = main.getString("humidity")
                val temp = main.getString("temp") + "°C"
                val sunrise: Long = sys.getLong("sunrise")
                val sunset: Long = sys.getLong("sunset")

                // Populating the views

                binding.cityTextView.text = address
                binding.updatedAttextview.text =
                    "Updated at: ${SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(
                        Date(updatedAt * 1000)
                    )}"
                binding.detailWeatherTextView.text = weatherDescription
                binding.degreeTextView.text = temp
                binding.sunriseTextView.text =
                    SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunrise * 1000))
                binding.sunsetTextView.text =
                    SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunset * 1000))
                binding.pressureTextView.text = "$pressure hPa"
                binding.humidityTextView.text = "$humidity%"
                binding.windTextView.text = "$windSpeed km/hr"

                binding.mainContainer.visibility = View.VISIBLE
                binding.loader.visibility = View.GONE
            }
            catch (e: Exception){
                binding.errorText.visibility = View.VISIBLE
                binding.loader.visibility = View.GONE
            }
        }

    }


}