package com.masdika.elcuaca.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import com.masdika.elcuaca.R
import com.masdika.elcuaca.databinding.FragmentContentBinding

class ContentFragment : Fragment() {

    private var _binding: FragmentContentBinding? = null
    private val binding get() = _binding!!

    private lateinit var address: String
    private lateinit var currentDate: String
    private var weatherCode: Int = 0
    private var temperature: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContentBinding.inflate(inflater, container, false)

        address = arguments?.getString("address").toString()
        currentDate = arguments?.getString("currentDate").toString()
        weatherCode = arguments?.getInt("weatherCode")!!
        temperature = arguments?.getDouble("temperature")!!

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setWeatherImageSrc(weatherCode)
        binding.tvDate.text = currentDate
        binding.tvAddress.text = address
        binding.tvTemperature.text = temperature.let { "${Math.round(it).toInt()}°C" }

        val animationFadeInUp = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_up)
        binding.mainFragmentContainer.startAnimation(animationFadeInUp)
    }

    private fun setWeatherImageSrc(weatherCode: Int) {
        when (weatherCode) {
            1000 -> {
                binding.weatherImage.setImageResource(R.drawable.clear)
                binding.tvWeatherCode.text = getString(R.string.weather_clear)
            }

            1001 -> {
                binding.weatherImage.setImageResource(R.drawable.cloudy)
                binding.tvWeatherCode.text = getString(R.string.weather_cloudy)
            }

            1101 -> {
                binding.weatherImage.setImageResource(R.drawable.partly_cloudy)
                binding.tvWeatherCode.text = getString(R.string.weather_partly_cloudy)
            }

            4000 -> {
                binding.weatherImage.setImageResource(R.drawable.drizzle)
                binding.tvWeatherCode.text = getString(R.string.weather_drizzle)
            }

            4200 -> {
                binding.weatherImage.setImageResource(R.drawable.light_rain)
                binding.tvWeatherCode.text = getString(R.string.weather_light_rain)
            }

            4001 -> {
                binding.weatherImage.setImageResource(R.drawable.rain)
                binding.tvWeatherCode.text = getString(R.string.weather_rain)
            }

            5000 -> {
                binding.weatherImage.setImageResource(R.drawable.snow)
                binding.tvWeatherCode.text = getString(R.string.weather_snow)
            }

            8000 -> {
                binding.weatherImage.setImageResource(R.drawable.thunderstorm)
                binding.tvWeatherCode.text = getString(R.string.weather_thunderstorm)
            }
        }
    }

}