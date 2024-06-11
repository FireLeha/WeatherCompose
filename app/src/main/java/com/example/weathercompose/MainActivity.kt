package com.example.weathercompose

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.weathercompose.data.WeatherModel
import com.example.weathercompose.screens.DialogSearch
import com.example.weathercompose.screens.MainCard
import com.example.weathercompose.screens.TabLayout
import org.json.JSONObject

//const val API_KEY = "4ba1a1ae2a634f5d8ed142817241904"

class MainActivity : ComponentActivity() {
    private val apikey = BuildConfig.API_KEY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val daysList = rememberSaveable {
                mutableStateOf(listOf<WeatherModel>())
            }
            val currentDay = rememberSaveable {
                mutableStateOf(WeatherModel())
            }
            val dialogState = rememberSaveable {
                mutableStateOf(false)
            }
            val cityName = rememberSaveable {
                mutableStateOf("Санкт-Петербург")
            }

            if (dialogState.value) {
                DialogSearch(dialogState, onSubmit = {
                    cityName.value = it
                    getData(it, this@MainActivity, daysList, currentDay)
                })
            }
            getData(cityName.value, this, daysList, currentDay)
            Image(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.5f),
                painter = painterResource(id = R.drawable.sky),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
            )
            Column {
                MainCard(currentDay, onClickSync = {
                    getData(cityName.value, this@MainActivity, daysList, currentDay)
                }, onClickSearch = {
                    dialogState.value = true
                }
                )
                TabLayout(daysList, currentDay)
            }
        }
    }

    private fun getData(
        city: String,
        context: Context,
        daysList: MutableState<List<WeatherModel>>,
        currentDay: MutableState<WeatherModel>
    ) {
        val url = "https://api.weatherapi.com/v1/forecast.json?key=" +
                apikey +
                "&q=$city" +
                "&days=3" +
                "&lang=ru"
        val queue = Volley.newRequestQueue(context)
        val sRequest = StringRequest(
            Request.Method.GET,
            url,
            { response ->
                val responseRus = String(response.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
                val list = getWeatherByDays(responseRus)
                currentDay.value = list[0]
                daysList.value = list
            },
            {
                Log.d("MyLogs", "VolleyError: $it")
            }
        )
        queue.add(sRequest)
    }

    private fun getWeatherByDays(response: String): List<WeatherModel> {
        if (response.isEmpty()) return listOf()
        val list = ArrayList<WeatherModel>()
        val mainObject = JSONObject(response)
        val city = mainObject.getJSONObject("location").getString("name")
        val days = mainObject.getJSONObject("forecast").getJSONArray("forecastday")

        for (i in 0 until days.length()) {
            val item = days[i] as JSONObject
            list.add(
                WeatherModel(
                    city,
                    item.getString("date"),
                    "",
                    item.getJSONObject("day").getJSONObject("condition").getString("text"),
                    item.getJSONObject("day").getJSONObject("condition").getString("icon"),
                    item.getJSONObject("day").getString("maxtemp_c"),
                    item.getJSONObject("day").getString("mintemp_c"),
                    item.getJSONArray("hour").toString()
                )
            )
        }
        list[0] = list[0].copy(
            time = mainObject.getJSONObject("current").getString("last_updated"),
            currentTemp = mainObject.getJSONObject("current").getString("temp_c")
        )
        return list
    }

}

