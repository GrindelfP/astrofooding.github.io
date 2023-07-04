package to.grindelf.astrofooding.dietlogics

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import to.grindelf.astrofooding.domain.*
import to.grindelf.astrofooding.utility.SimplexSolver
import java.io.File

/**
 * This class contains all the logic for calculating optimal macronutrients and diet
 */
class DietProcessor(
    private val astronaut: Astronaut
) {

    /**
     * Amount of macronutrients that are optimal for the astronaut
     */
    private val optimalMacronutrients: Macronutrients

    companion object {

        /**
         * Activity factor for calculating calories (it is constant
         * because we assume that astronauts are very active people;
         * they have to perform a lot of physical exercises a day
         * to stay in shape and to avoid muscle atrophy because of
         * microgravity)
         */
        private const val ACTIVITY_FACTOR = 1.725

        /**
         * Menu file
         */
        private val MENU_FILE = File("src/main/resources/menus/menu1.json")
    }

    init {
        optimalMacronutrients = calculateOptimalMacronutrients()
    }

    /**
     * Calculates optimal macronutrients for an astronaut
     * @return Macronutrients
     */
    fun calculateOptimalMacronutrients(): Macronutrients {
        val calories = calculateCalories()
        val protein = calculateProteins()
        val fats = calculateFats(protein)
        val carbs = calculateCarbs(calories, protein, fats)

        return Macronutrients(calories, protein, fats, carbs) // TODO: update the formulae
    }

    /**
     * Calculates optimal diet for an astronaut
     * @return Results
     */
    fun calculateOptimalDiet(): Diet {
        val limits = initializeLimits() // declare limits for each macronutrient
        val menu = getMenu() // read menu from JSON file
        val matrix = initializeMatrix(menu) // declare matrix of macronutrients for each meal of the menu
        val simplexSolutions = SimplexSolver.solve(matrix, limits) // solve the problem
        val mealsByQuantity = getQuantifiedMeals(simplexSolutions) // create a list of meals with their quantities

        return Diet(
            astronaut = astronaut,
            macronutrients = optimalMacronutrients,
            mealsByQuantity = mealsByQuantity
        )
    }

    private fun calculateCalories(): Double = when (astronaut.gender) {
        "male" -> (66.5 + 13.75 * astronaut.weight + 5.003 * astronaut.height -
                6.755 * astronaut.age) * ACTIVITY_FACTOR

        "female" -> (655.1 + 9.6 * astronaut.weight + 1.9 * astronaut.height -
                4.7 * astronaut.age) * ACTIVITY_FACTOR

        else -> throw IllegalArgumentException("This gender is not supported yet")
    }

    private fun calculateProteins(): Double = astronaut.weight * 2.2 * 1.2

    private fun calculateFats(calories: Double): Double = calories * 0.3 / 9

    private fun calculateCarbs(
        calories: Double,
        proteins: Double,
        fats: Double
    ): Double = (calories - proteins * 4 - fats * 9) / 4

    private fun initializeLimits(): List<Double> = listOf(
        optimalMacronutrients.protein,
        optimalMacronutrients.fat,
        optimalMacronutrients.carbs
    )

    /**
     * Returns a menu of meals read from JSON file
     * @return List<Meal>
     */
    private fun getMenu(): List<Meal> {
        val dataAsText = MENU_FILE.readText()

        fun objectMapper(): ObjectMapper = ObjectMapper().registerModule(kotlinModule())
        val mapper: ObjectMapper = objectMapper()

        return mapper.readValue<List<Meal>>(dataAsText)
    }

    private fun initializeMatrix(menu: List<Meal>): List<List<Double>> {
        val matrix = mutableListOf<List<Double>>()

        menu.forEach { meal ->
            matrix.add(
                listOf(
                    meal.protein,
                    meal.fat,
                    meal.carbs
                )
            )
        }

        return matrix
    }

    private fun getQuantifiedMeals(simplexSolutions: List<Int>): List<MealByQuantity> {
        val mealsByQuantity = mutableListOf<MealByQuantity>()

        simplexSolutions.forEachIndexed { index, quantity ->
            mealsByQuantity.add(
                MealByQuantity(
                    meal = getMenu()[index], quantity = quantity
                )
            )
        }

        return mealsByQuantity
    }
}
