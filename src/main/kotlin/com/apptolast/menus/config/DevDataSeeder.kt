package com.apptolast.menus.config

import com.apptolast.menus.allergen.repository.AllergenRepository
import com.apptolast.menus.auth.model.entity.AdminWhitelist
import com.apptolast.menus.auth.repository.AdminWhitelistRepository
import com.apptolast.menus.consumer.model.entity.UserAccount
import com.apptolast.menus.consumer.model.entity.UserAllergenProfile
import com.apptolast.menus.consumer.model.entity.UserFavoriteRestaurant
import com.apptolast.menus.consumer.model.enum.UserRole
import com.apptolast.menus.consumer.repository.UserAccountRepository
import com.apptolast.menus.consumer.repository.UserAllergenProfileRepository
import com.apptolast.menus.consumer.repository.UserFavoriteRestaurantRepository
import com.apptolast.menus.dish.model.entity.Dish
import com.apptolast.menus.dish.model.entity.DishAllergen
import com.apptolast.menus.dish.model.enum.ContainmentLevel
import com.apptolast.menus.dish.repository.DishAllergenRepository
import com.apptolast.menus.dish.repository.DishRepository
import com.apptolast.menus.ingredient.model.entity.Ingredient
import com.apptolast.menus.ingredient.model.entity.IngredientAllergen
import com.apptolast.menus.ingredient.repository.IngredientAllergenRepository
import com.apptolast.menus.ingredient.repository.IngredientRepository
import com.apptolast.menus.menu.model.entity.Menu
import com.apptolast.menus.menu.model.entity.MenuSection
import com.apptolast.menus.menu.repository.MenuRepository
import com.apptolast.menus.menu.repository.MenuSectionRepository
import com.apptolast.menus.recipe.model.entity.Recipe
import com.apptolast.menus.recipe.model.entity.RecipeIngredient
import com.apptolast.menus.recipe.repository.RecipeIngredientRepository
import com.apptolast.menus.recipe.repository.RecipeRepository
import com.apptolast.menus.restaurant.model.entity.Restaurant
import com.apptolast.menus.restaurant.repository.RestaurantRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Component
@Profile("dev")
class DevDataSeeder(
    private val userAccountRepository: UserAccountRepository,
    private val adminWhitelistRepository: AdminWhitelistRepository,
    private val restaurantRepository: RestaurantRepository,
    private val ingredientRepository: IngredientRepository,
    private val ingredientAllergenRepository: IngredientAllergenRepository,
    private val recipeRepository: RecipeRepository,
    private val recipeIngredientRepository: RecipeIngredientRepository,
    private val menuRepository: MenuRepository,
    private val menuSectionRepository: MenuSectionRepository,
    private val dishRepository: DishRepository,
    private val dishAllergenRepository: DishAllergenRepository,
    private val allergenRepository: AllergenRepository,
    private val userAllergenProfileRepository: UserAllergenProfileRepository,
    private val userFavoriteRestaurantRepository: UserFavoriteRestaurantRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(DevDataSeeder::class.java)

    @Transactional
    override fun run(vararg args: String) {
        if (restaurantRepository.existsBySlug("la-brava-piconera")) {
            logger.info("Dev seed data already exists. Skipping seeder.")
            return
        }

        logger.info("Starting dev data seeder...")

        // --- 1. Admin whitelist + Admin user ---
        val adminEmail = "admin@apptolast.com"
        adminWhitelistRepository.save(AdminWhitelist(email = adminEmail))
        val admin = userAccountRepository.save(
            UserAccount(
                email = adminEmail,
                passwordHash = passwordEncoder.encode("Admin123!"),
                name = "Admin User",
                role = UserRole.ADMIN,
                gdprConsent = true
            )
        )
        logger.info("Created admin user: {}", admin.email)

        // --- 2. Consumer (USER) user ---
        val consumer = userAccountRepository.save(
            UserAccount(
                email = "consumer@example.com",
                passwordHash = passwordEncoder.encode("User1234!"),
                name = "Test Consumer",
                role = UserRole.USER,
                gdprConsent = true
            )
        )
        logger.info("Created consumer user: {}", consumer.email)

        // --- 3. Restaurants ---
        val restaurant1 = restaurantRepository.save(
            Restaurant(
                name = "La Brava Piconera",
                slug = "la-brava-piconera",
                description = "Tapas de autor en el corazon de Sevilla. Cocina tradicional con un toque rebelde.",
                address = "Calle Sierpes 42, Sevilla",
                phone = "+34 954 123 456"
            )
        )
        restaurantRepository.save(
            Restaurant(
                name = "El Rincon del Mar",
                slug = "el-rincon-del-mar",
                description = "Marisqueria con productos frescos del Atlantico.",
                address = "Paseo Maritimo 10, Cadiz",
                phone = "+34 956 789 012"
            )
        )
        logger.info("Created 2 restaurants")

        // --- 4. Global ingredients with relational allergens ---
        val allergensByCode = allergenRepository.findAll().associateBy { it.code }
        val ingredients = seedIngredients(allergensByCode)
        logger.info("Created {} global ingredients with allergens", ingredients.size)

        // --- 5. Recipes for restaurant 1 (flat, no sub-recipes) ---
        val croquetasRecipe = createRecipe(
            restaurant1, "Croquetas Ibericas del Puchero",
            "Croquetas cremosas de jamon iberico con bechamel casera", "Entrantes",
            BigDecimal("12.50"),
            listOf(
                ingredients["Harina de trigo"]!! to ("100" to "g"),
                ingredients["Leche entera"]!! to ("500" to "ml"),
                ingredients["Huevos frescos"]!! to ("2" to "unidades"),
                ingredients["Pan rallado"]!! to ("150" to "g")
            )
        )
        val bravasRecipe = createRecipe(
            restaurant1, "Patatas Rebeldes Bravas",
            "Patatas fritas con alioli casero y salsa brava picante", "Entrantes",
            BigDecimal("8.00"),
            listOf(
                ingredients["Patata"]!! to ("400" to "g"),
                ingredients["Huevos frescos"]!! to ("1" to "unidades"),
                ingredients["Mostaza Dijon"]!! to ("10" to "g"),
                ingredients["Aceite de oliva"]!! to ("200" to "ml")
            )
        )
        val ensaladillaRecipe = createRecipe(
            restaurant1, "Ensaladilla Ekaterina",
            "Ensaladilla rusa con anchoas del Cantabrico", "Entrantes",
            BigDecimal("9.50"),
            listOf(
                ingredients["Patata"]!! to ("300" to "g"),
                ingredients["Mayonesa comercial"]!! to ("100" to "g"),
                ingredients["Anchoas en aceite"]!! to ("30" to "g"),
                ingredients["Huevos frescos"]!! to ("2" to "unidades")
            )
        )
        val burrataRecipe = createRecipe(
            restaurant1, "Burrata Campana",
            "Burrata fresca con pesto de nueces y tomate cherry", "Principales",
            BigDecimal("14.00"),
            listOf(
                ingredients["Parmesano"]!! to ("30" to "g"),
                ingredients["Nueces"]!! to ("40" to "g"),
                ingredients["Tomate"]!! to ("150" to "g"),
                ingredients["Aceite de oliva"]!! to ("30" to "ml")
            )
        )
        val butifarraRecipe = createRecipe(
            restaurant1, "Butifarra Cordobesa a la Brasa",
            "Butifarra artesanal con huevo frito y mostaza antigua", "Principales",
            BigDecimal("16.00"),
            listOf(
                ingredients["Butifarra"]!! to ("200" to "g"),
                ingredients["Huevos frescos"]!! to ("1" to "unidades"),
                ingredients["Mostaza Dijon"]!! to ("15" to "g")
            )
        )
        logger.info("Created 5 recipes for '{}'", restaurant1.name)

        // --- 6. Menu with sections ---
        val menu = menuRepository.save(
            Menu(
                restaurantId = restaurant1.id,
                name = "Carta Primavera 2026",
                description = "Nuestra carta de temporada con productos frescos de primavera",
                published = true
            )
        )
        val sectionEntrantes = menuSectionRepository.save(
            MenuSection(menu = menu, name = "Entrantes", displayOrder = 0)
        )
        val sectionPrincipales = menuSectionRepository.save(
            MenuSection(menu = menu, name = "Principales", displayOrder = 1)
        )
        menuSectionRepository.save(
            MenuSection(menu = menu, name = "Postres", displayOrder = 2)
        )
        logger.info("Created menu '{}' with 3 sections", menu.name)

        // --- 7. Dishes with allergen declarations ---
        createDishWithAllergens(
            sectionEntrantes, croquetasRecipe,
            "Croquetas Ibericas del Puchero", "6 unidades de croquetas cremosas de jamon iberico",
            0, allergensByCode,
            mapOf("GLUTEN" to ContainmentLevel.CONTAINS, "MILK" to ContainmentLevel.CONTAINS, "EGGS" to ContainmentLevel.CONTAINS)
        )
        createDishWithAllergens(
            sectionEntrantes, bravasRecipe,
            "Patatas Rebeldes Bravas", "Patatas fritas crujientes con alioli casero y brava picante",
            1, allergensByCode,
            mapOf("EGGS" to ContainmentLevel.CONTAINS, "MUSTARD" to ContainmentLevel.CONTAINS)
        )
        createDishWithAllergens(
            sectionEntrantes, ensaladillaRecipe,
            "Ensaladilla Ekaterina", "Ensaladilla rusa con anchoas del Cantabrico y aceitunas",
            2, allergensByCode,
            mapOf("EGGS" to ContainmentLevel.CONTAINS, "FISH" to ContainmentLevel.CONTAINS)
        )
        createDishWithAllergens(
            sectionPrincipales, burrataRecipe,
            "Burrata Campana", "Burrata fresca con pesto de nueces y tomate cherry",
            0, allergensByCode,
            mapOf("MILK" to ContainmentLevel.CONTAINS, "TREE_NUTS" to ContainmentLevel.CONTAINS, "PEANUTS" to ContainmentLevel.MAY_CONTAIN)
        )
        createDishWithAllergens(
            sectionPrincipales, butifarraRecipe,
            "Butifarra Cordobesa a la Brasa", "Butifarra artesanal con huevo frito y mostaza antigua",
            1, allergensByCode,
            mapOf("EGGS" to ContainmentLevel.CONTAINS, "MUSTARD" to ContainmentLevel.CONTAINS)
        )
        logger.info("Created 5 dishes with allergen declarations")

        // --- 8. Consumer allergen profile (allergic to GLUTEN and EGGS) ---
        userAllergenProfileRepository.save(
            UserAllergenProfile(
                userId = consumer.id,
                allergenCodes = listOf("GLUTEN", "EGGS"),
                severityNotes = "Celiac disease + egg allergy"
            )
        )
        logger.info("Created allergen profile for consumer user")

        // --- 9. Favorite restaurant ---
        userFavoriteRestaurantRepository.save(
            UserFavoriteRestaurant(user = consumer, restaurant = restaurant1)
        )
        logger.info("Added restaurant '{}' to consumer favorites", restaurant1.name)

        logger.info("Dev data seeder completed successfully!")
    }

    private fun seedIngredients(
        allergensByCode: Map<String, com.apptolast.menus.allergen.model.entity.Allergen>
    ): Map<String, Ingredient> {
        data class IngredientSeed(
            val name: String,
            val brand: String?,
            val allergens: Map<String, ContainmentLevel> = emptyMap()
        )

        val seeds = listOf(
            IngredientSeed("Harina de trigo", "La Meta", mapOf("GLUTEN" to ContainmentLevel.CONTAINS)),
            IngredientSeed("Gambas peladas", "Pescanova", mapOf("CRUSTACEANS" to ContainmentLevel.CONTAINS)),
            IngredientSeed("Leche entera", "Central Lechera", mapOf("MILK" to ContainmentLevel.CONTAINS)),
            IngredientSeed("Huevos frescos", "Granja El Roble", mapOf("EGGS" to ContainmentLevel.CONTAINS)),
            IngredientSeed("Aceite de cacahuete", null, mapOf("PEANUTS" to ContainmentLevel.CONTAINS)),
            IngredientSeed("Anchoas en aceite", "Conservas Ortiz", mapOf("FISH" to ContainmentLevel.CONTAINS)),
            IngredientSeed("Salsa de soja", "Kikkoman", mapOf("SOYA" to ContainmentLevel.CONTAINS)),
            IngredientSeed("Patata", null),
            IngredientSeed("Cebollino", null),
            IngredientSeed("Butifarra", null),
            IngredientSeed("Tomate", null),
            IngredientSeed("Aceite de oliva", null),
            IngredientSeed("Pan rallado", null, mapOf("GLUTEN" to ContainmentLevel.CONTAINS)),
            IngredientSeed("Mayonesa comercial", null, mapOf("EGGS" to ContainmentLevel.CONTAINS)),
            IngredientSeed("Mostaza Dijon", null, mapOf("MUSTARD" to ContainmentLevel.CONTAINS)),
            IngredientSeed("Parmesano", null, mapOf("MILK" to ContainmentLevel.CONTAINS)),
            IngredientSeed("Nueces", null, mapOf("TREE_NUTS" to ContainmentLevel.CONTAINS))
        )

        return seeds.associate { seed ->
            val ingredient = ingredientRepository.save(
                Ingredient(name = seed.name, brand = seed.brand)
            )
            seed.allergens.forEach { (code, level) ->
                val allergen = allergensByCode[code]
                if (allergen != null) {
                    ingredientAllergenRepository.save(
                        IngredientAllergen(
                            ingredient = ingredient,
                            allergen = allergen,
                            containmentLevel = level
                        )
                    )
                }
            }
            seed.name to ingredient
        }
    }

    private fun createRecipe(
        restaurant: Restaurant,
        name: String,
        description: String,
        category: String,
        price: BigDecimal? = null,
        ingredientRefs: List<Pair<Ingredient, Pair<String, String>>>
    ): Recipe {
        val recipe = recipeRepository.save(
            Recipe(
                restaurantId = restaurant.id,
                name = name,
                description = description,
                category = category,
                price = price
            )
        )
        ingredientRefs.forEach { (ingredient, qtyUnit) ->
            recipeIngredientRepository.save(
                RecipeIngredient(
                    recipe = recipe,
                    ingredient = ingredient,
                    quantity = BigDecimal(qtyUnit.first),
                    unit = qtyUnit.second
                )
            )
        }
        return recipe
    }

    private fun createDishWithAllergens(
        section: MenuSection,
        recipe: Recipe?,
        name: String,
        description: String,
        displayOrder: Int,
        allergensByCode: Map<String, com.apptolast.menus.allergen.model.entity.Allergen>,
        allergenLevels: Map<String, ContainmentLevel>
    ) {
        val dish = dishRepository.save(
            Dish(
                section = section,
                recipe = recipe,
                name = name,
                description = description,
                displayOrder = displayOrder
            )
        )
        allergenLevels.forEach { (code, level) ->
            val allergen = allergensByCode[code]
            if (allergen != null) {
                dishAllergenRepository.save(
                    DishAllergen(
                        dish = dish,
                        allergen = allergen,
                        containmentLevel = level
                    )
                )
            } else {
                logger.warn("Allergen '{}' not found. Skipping for dish '{}'.", code, dish.name)
            }
        }
    }
}
