package com.example.bmicalculationapplication

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bmicalculationapplication.databinding.ActivityMainBinding
import java.util.Locale

/**
 * Enumeration representing supported unit measurement systems.
 */
enum class UnitSystem {
    METRIC,
    IMPERIAL
}

/**
 * Enumeration representing WHO Body Mass Index (BMI) health categories.
 * Each category holds corresponding string resource IDs for labels and feedback,
 * as well as color-coding resource IDs for visual category distinction.
 */
enum class BmiCategory(
    val labelResId: Int,
    val feedbackResId: Int,
    val textColorResId: Int,
    val containerColorResId: Int
) {
    UNDERWEIGHT(
        R.string.category_underweight,
        R.string.feedback_underweight,
        R.color.bmi_underweight_text,
        R.color.bmi_underweight_container
    ),
    NORMAL(
        R.string.category_normal,
        R.string.feedback_normal,
        R.color.bmi_normal_text,
        R.color.bmi_normal_container
    ),
    OVERWEIGHT(
        R.string.category_overweight,
        R.string.feedback_overweight,
        R.color.bmi_overweight_text,
        R.color.bmi_overweight_container
    ),
    OBESE(
        R.string.category_obese,
        R.string.feedback_obese,
        R.color.bmi_obese_text,
        R.color.bmi_obese_container
    );

    companion object {
        /**
         * Abstract/Centralized Classification Function:
         * Classifies a numerical BMI score into its corresponding WHO health category.
         *
         * Category Cutoffs:
         * - Underweight: < 18.5
         * - Normal weight: 18.5 – 24.9
         * - Overweight: 25.0 – 29.9
         * - Obese: >= 30.0
         *
         * @param bmi Calculated BMI numeric value.
         * @return [BmiCategory] corresponding to the given score.
         */
        fun fromBmi(bmi: Double): BmiCategory {
            return when {
                bmi < 18.5 -> UNDERWEIGHT
                bmi < 25.0 -> NORMAL
                bmi < 30.0 -> OVERWEIGHT
                else -> OBESE
            }
        }
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentUnitSystem: UnitSystem = UnitSystem.METRIC

    private val minWeightKg = 1.0
    private val maxWeightKg = 500.0
    private val minHeightCm = 30.0
    private val maxHeightCm = 300.0

    private val minWeightLbs = 2.0
    private val maxWeightLbs = 1100.0
    private val minFeet = 1
    private val maxFeet = 9
    private val minInches = 0.0
    private val maxInches = 11.99

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupListeners()
        updateUnitSystemUi(currentUnitSystem)
    }

    /**
     * Centralized Initialization Function:
     * Registers event listeners for unit selection, calculate button, reset button, and text changes.
     */
    private fun setupListeners() {
        // Unit System Selection RadioGroup Listener
        binding.rgUnitSystem.setOnCheckedChangeListener { _, checkedId ->
            currentUnitSystem = if (checkedId == R.id.rbImperial) {
                    UnitSystem.IMPERIAL
            } else {
                UnitSystem.METRIC
            }
            updateUnitSystemUi(currentUnitSystem)
        }

        // Calculate Button Listener
        binding.btnCalculate.setOnClickListener {
            val parsedInput = validateAndParseInput(currentUnitSystem)
            if (parsedInput != null) {
                val (weight, totalHeightInBaseUnits) = parsedInput
                val bmi = calculateBmi(weight, totalHeightInBaseUnits, currentUnitSystem)
                displayBmiResult(bmi)
            }
        }

        // Reset Button Listener
        binding.btnReset.setOnClickListener {
            resetForm()
        }

        // TextWatcher to clear errors automatically as the user types
        val clearErrorWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearErrors()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.etWeight.addTextChangedListener(clearErrorWatcher)
        binding.etHeightCm.addTextChangedListener(clearErrorWatcher)
        binding.etHeightFt.addTextChangedListener(clearErrorWatcher)
        binding.etHeightIn.addTextChangedListener(clearErrorWatcher)
    }

    /**
     * Abstract/Centralized Input Validation Function:
     * Performs multi-step validation on user input fields based on the active unit system:
     * 1. Metric: Validates weight (kg) and height (cm).
     * 2. Imperial: Validates weight (lbs), height in feet (ft), and height in inches (in).
     *    Converts (feet, inches) into total inches: Total Inches = (Feet * 12) + Inches.
     *
     * Sets detailed error messages directly on the [TextInputLayout] controls when validation fails.
     *
     * @param unitSystem Active [UnitSystem] (Metric or Imperial).
     * @return [Pair] of (weight, height) where height is in cm (for Metric) or total inches (for Imperial). Returns null if invalid.
     */
    private fun validateAndParseInput(unitSystem: UnitSystem): Pair<Double, Double>? {
        var isValid = true
        clearErrors()

        // 1. Validate Weight Field
        val weightStr = binding.etWeight.text.toString().trim()
        val weight = weightStr.toDoubleOrNull()

        if (weightStr.isEmpty()) {
            binding.tilWeight.error = getString(R.string.error_weight_empty)
            isValid = false
        } else if (weight == null) {
            binding.tilWeight.error = getString(R.string.error_invalid_number)
            isValid = false
        } else {
            val (minW, maxW) = when (unitSystem) {
                UnitSystem.METRIC -> Pair(minWeightKg, maxWeightKg)
                UnitSystem.IMPERIAL -> Pair(minWeightLbs, maxWeightLbs)
            }

            if (weight < minW || weight > maxW) {
                val errorMsg = if (unitSystem == UnitSystem.METRIC) {
                    getString(R.string.error_weight_range_metric, minW, maxW)
                } else {
                    getString(R.string.error_weight_range_imperial, minW, maxW)
                }
                binding.tilWeight.error = errorMsg
                isValid = false
            }
        }

        // 2. Validate Height Field(s)
        var parsedHeight = 0.0

        if (unitSystem == UnitSystem.METRIC) {
            val heightCmStr = binding.etHeightCm.text.toString().trim()
            val heightCm = heightCmStr.toDoubleOrNull()

            if (heightCmStr.isEmpty()) {
                binding.tilHeightCm.error = getString(R.string.error_height_empty)
                isValid = false
            } else if (heightCm == null) {
                binding.tilHeightCm.error = getString(R.string.error_invalid_number)
                isValid = false
            } else if (heightCm < minHeightCm || heightCm > maxHeightCm) {
                binding.tilHeightCm.error = getString(
                    R.string.error_height_range_metric,
                    minHeightCm,
                    maxHeightCm
                )
                isValid = false
            } else {
                parsedHeight = heightCm
            }
        } else {
            // Imperial Unit Validation (Feet + Inches)
            val feetStr = binding.etHeightFt.text.toString().trim()
            val inchesStr = binding.etHeightIn.text.toString().trim()

            val feet = feetStr.toIntOrNull()
            val inches = if (inchesStr.isEmpty()) 0.0 else inchesStr.toDoubleOrNull()

            if (feetStr.isEmpty()) {
                binding.tilHeightFt.error = getString(R.string.error_feet_empty)
                isValid = false
            } else if (feet == null) {
                binding.tilHeightFt.error = getString(R.string.error_invalid_number)
                isValid = false
            } else if (feet < minFeet || feet > maxFeet) {
                binding.tilHeightFt.error = getString(R.string.error_feet_range)
                isValid = false
            }

            if (inchesStr.isNotEmpty() && inchesStr.toDoubleOrNull() == null) {
                binding.tilHeightIn.error = getString(R.string.error_invalid_number)
                isValid = false
            } else if (inches != null && (inches < minInches || inches > maxInches)) {
                binding.tilHeightIn.error = getString(R.string.error_inches_range)
                isValid = false
            }

            if (isValid && feet != null && inches != null) {
                // Formula to convert Feet + Inches -> Total Inches
                parsedHeight = (feet * 12) + inches
            }
        }

        return if (isValid && weight != null) {
            Pair(weight, parsedHeight)
        } else {
            null
        }
    }

    /**
     * Abstract/Centralized Calculation Function:
     * Computes the numerical Body Mass Index (BMI) using mathematical formulas tailored
     * to the selected measurement system.
     *
     * Mathematical Formulas:
     * - Metric:   BMI = weight (kg) / (height (m))^2 = weight (kg) / (height (cm) / 100)^2
     * - Imperial: BMI = 703 * weight (lbs) / (total height in inches)^2
     *
     * @param weight Parsed weight value (kg or lbs).
     * @param totalHeight Parsed height value in cm (Metric) or total inches (Imperial).
     * @param unitSystem The measurement system used.
     * @return Computed numerical BMI value.
     */
    private fun calculateBmi(weight: Double, totalHeight: Double, unitSystem: UnitSystem): Double {
        return when (unitSystem) {
            UnitSystem.METRIC -> {
                val heightMeters = totalHeight / 100.0
                weight / (heightMeters * heightMeters)
            }
            UnitSystem.IMPERIAL -> {
                703.0 * weight / (totalHeight * totalHeight)
            }
        }
    }

    /**
     * Abstract/Centralized UI Presentation Function:
     * Updates the result section of the UI with formatted score, category text, health advice feedback,
     * and applies dynamic category color-coding to text and background views.
     *
     * @param bmi Calculated BMI numeric score.
     */
    private fun displayBmiResult(bmi: Double) {
        val category = BmiCategory.fromBmi(bmi)

        // Format score to 1 decimal place (e.g., "22.5")
        binding.tvBmiValue.text = String.format(Locale.getDefault(), "%.1f", bmi)

        // Set health category label & color
        binding.tvCategory.text = getString(category.labelResId)
        val textColor = ContextCompat.getColor(this, category.textColorResId)
        binding.tvCategory.setTextColor(textColor)
        binding.tvBmiValue.setTextColor(textColor)

        // Set health advice guidance text
        binding.tvFeedback.text = getString(category.feedbackResId)

        // Dynamic background color coding for the result card
        val containerBgColor = ContextCompat.getColor(this, category.containerColorResId)
        binding.cardResult.setCardBackgroundColor(containerBgColor)
    }

    /**
     * Abstract/Centralized Layout Configuration Function:
     * Dynamically updates field hints, measurement unit suffixes (kg vs lbs), and toggles visibility
     * between Metric single height field (cm) and Imperial dual height fields (ft & in).
     *
     * @param unitSystem Target unit system to configure UI for.
     */
    private fun updateUnitSystemUi(unitSystem: UnitSystem) {
        clearErrors()

        when (unitSystem) {
            UnitSystem.METRIC -> {
                binding.tilWeight.suffixText = getString(R.string.unit_suffix_kg)
                binding.tilWeight.placeholderText = getString(R.string.weight_hint_metric)

                binding.tilHeightCm.visibility = View.VISIBLE
                binding.layoutImperialHeight.visibility = View.GONE
            }
            UnitSystem.IMPERIAL -> {
                binding.tilWeight.suffixText = getString(R.string.unit_suffix_lbs)
                binding.tilWeight.placeholderText = getString(R.string.weight_hint_imperial)

                binding.tilHeightCm.visibility = View.GONE
                binding.layoutImperialHeight.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Clears error states on all input layout controls.
     */
    private fun clearErrors() {
        binding.tilWeight.error = null
        binding.tilHeightCm.error = null
        binding.tilHeightFt.error = null
        binding.tilHeightIn.error = null
    }

    /**
     * Abstract/Centralized Form Reset Function:
     * Restores input fields, error states, unit choices, and result display components back to initial state.
     */
    private fun resetForm() {
        binding.etWeight.text?.clear()
        binding.etHeightCm.text?.clear()
        binding.etHeightFt.text?.clear()
        binding.etHeightIn.text?.clear()
        clearErrors()

        binding.rbMetric.isChecked = true
        currentUnitSystem = UnitSystem.METRIC
        updateUnitSystemUi(currentUnitSystem)

        binding.tvBmiValue.text = getString(R.string.result_bmi_value_placeholder)
        binding.tvCategory.text = getString(R.string.result_category_placeholder)
        binding.tvFeedback.text = getString(R.string.result_prompt)

        val defaultCardColor = ContextCompat.getColor(
            this,
            com.google.android.material.R.color.design_default_color_surface
        )
        binding.cardResult.setCardBackgroundColor(defaultCardColor)
    }
}
