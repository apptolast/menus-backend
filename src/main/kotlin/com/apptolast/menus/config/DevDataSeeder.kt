package com.apptolast.menus.config

import com.apptolast.menus.allergen.repository.AllergenRepository
import com.apptolast.menus.consumer.model.entity.UserAccount
import com.apptolast.menus.consumer.model.enum.UserRole
import com.apptolast.menus.consumer.repository.UserAccountRepository
import com.apptolast.menus.digitalcard.model.entity.DigitalCard
import com.apptolast.menus.digitalcard.repository.DigitalCardRepository
import com.apptolast.menus.dish.model.entity.Dish
import com.apptolast.menus.dish.model.entity.DishAllergen
import com.apptolast.menus.dish.model.enum.ContainmentLevel
import com.apptolast.menus.dish.repository.DishAllergenRepository
import com.apptolast.menus.dish.repository.DishRepository
import com.apptolast.menus.ingredient.model.entity.Ingredient
import com.apptolast.menus.ingredient.repository.IngredientRepository
import com.apptolast.menus.menu.model.entity.Menu
import com.apptolast.menus.menu.model.entity.MenuRecipe
import com.apptolast.menus.menu.model.entity.MenuSection
import com.apptolast.menus.menu.repository.MenuRecipeRepository
import com.apptolast.menus.menu.repository.MenuRepository
import com.apptolast.menus.menu.repository.MenuSectionRepository
import com.apptolast.menus.recipe.model.entity.Recipe
import com.apptolast.menus.recipe.model.entity.RecipeIngredient
import com.apptolast.menus.recipe.repository.RecipeIngredientRepository
import com.apptolast.menus.recipe.repository.RecipeRepository
import com.apptolast.menus.restaurant.model.entity.Restaurant
import com.apptolast.menus.restaurant.model.entity.Subscription
import com.apptolast.menus.restaurant.model.enum.SubscriptionTier
import com.apptolast.menus.restaurant.repository.RestaurantRepository
import com.apptolast.menus.restaurant.repository.SubscriptionRepository
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

@Component
@Profile("dev")
class DevDataSeeder(
    private val userAccountRepository: UserAccountRepository,
    private val restaurantRepository: RestaurantRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val ingredientRepository: IngredientRepository,
    private val recipeRepository: RecipeRepository,
    private val recipeIngredientRepository: RecipeIngredientRepository,
    private val menuRepository: MenuRepository,
    private val menuSectionRepository: MenuSectionRepository,
    private val menuRecipeRepository: MenuRecipeRepository,
    private val dishRepository: DishRepository,
    private val dishAllergenRepository: DishAllergenRepository,
    private val digitalCardRepository: DigitalCardRepository,
    private val allergenRepository: AllergenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val entityManager: EntityManager
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(DevDataSeeder::class.java)

    @Transactional
    override fun run(vararg args: String) {
        if (restaurantRepository.existsBySlug("la-brava-piconera")) {
            logger.info("Dev seed data already exists (restaurant 'la-brava-piconera' found). Skipping seeder.")
            return
        }

        logger.info("Starting dev data seeder for 'La Brava Piconera'...")

        // --- 1. User Account ---
        val ownerEmail = "chef@labrava.es"
        val emailBytes = ownerEmail.toByteArray(StandardCharsets.UTF_8)
        val emailHash = sha256Hex(ownerEmail)

        val owner = userAccountRepository.save(
            UserAccount(
                email = emailBytes,
                emailHash = emailHash,
                passwordHash = passwordEncoder.encode("DevPassword123!"),
                role = UserRole.RESTAURANT_OWNER
            )
        )
        logger.info("Created dev user: {} with id {}", ownerEmail, owner.id)

        // --- 2. Restaurant (tenant_id = restaurant.id) ---
        val restaurantId = UUID.randomUUID()
        val tenantId = restaurantId

        val restaurant = restaurantRepository.save(
            Restaurant(
                id = restaurantId,
                tenantId = tenantId,
                ownerId = owner.id,
                name = "La Brava Piconera",
                slug = "la-brava-piconera",
                description = "Tapas de autor en el corazon de Sevilla. Cocina tradicional con un toque rebelde.",
                address = "Calle Sierpes 42, Sevilla",
                phone = "+34 954 123 456"
            )
        )
        logger.info("Created dev restaurant: {} with tenant_id {}", restaurant.name, tenantId)

        // --- 3. Subscription ---
        subscriptionRepository.save(
            Subscription(
                restaurantId = restaurantId,
                tier = SubscriptionTier.PROFESSIONAL,
                maxMenus = 5,
                maxDishes = 200
            )
        )
        logger.info("Created PROFESSIONAL subscription for restaurant {}", restaurantId)

        // --- Set tenant context for RLS-protected tables ---
        setTenant(tenantId)

        // --- 4. Ingredients ---
        val ingredients = seedIngredients(tenantId, owner.id)
        logger.info("Created {} dev ingredients", ingredients.size)

        // --- 5. Sub-elaboration Recipes ---
        val bechamel = recipeRepository.save(
            Recipe(
                tenantId = tenantId,
                restaurantId = restaurantId,
                name = "Bechamel Base",
                description = "Bechamel clasica para croquetas y gratinados",
                category = "Bases",
                isSubElaboration = true,
                createdBy = owner.id
            )
        )
        recipeIngredientRepository.saveAll(
            listOf(
                RecipeIngredient(
                    recipe = bechamel,
                    ingredient = ingredients["Harina de trigo"],
                    tenantId = tenantId,
                    quantity = BigDecimal("100"),
                    unit = "g",
                    sortOrder = 0
                ),
                RecipeIngredient(
                    recipe = bechamel,
                    ingredient = ingredients["Leche entera"],
                    tenantId = tenantId,
                    quantity = BigDecimal("500"),
                    unit = "ml",
                    sortOrder = 1
                )
            )
        )

        val alioli = recipeRepository.save(
            Recipe(
                tenantId = tenantId,
                restaurantId = restaurantId,
                name = "Alioli Casero",
                description = "Alioli emulsionado a mano con huevo y aceite de oliva",
                category = "Salsas",
                isSubElaboration = true,
                createdBy = owner.id
            )
        )
        recipeIngredientRepository.saveAll(
            listOf(
                RecipeIngredient(
                    recipe = alioli,
                    ingredient = ingredients["Huevos frescos"],
                    tenantId = tenantId,
                    quantity = BigDecimal("2"),
                    unit = "unidades",
                    sortOrder = 0
                ),
                RecipeIngredient(
                    recipe = alioli,
                    ingredient = ingredients["Aceite de oliva"],
                    tenantId = tenantId,
                    quantity = BigDecimal("200"),
                    unit = "ml",
                    sortOrder = 1
                )
            )
        )
        logger.info("Created 2 sub-elaboration recipes: Bechamel Base, Alioli Casero")

        // --- 6. Main Recipes ---
        val croquetasRecipe = createMainRecipe(
            tenantId, restaurantId, owner.id,
            "Croquetas Ibericas del Puchero",
            "Croquetas cremosas de jamon iberico con bechamel casera",
            "Entrantes",
            listOf(
                IngredientRef(subRecipe = bechamel, qty = "1", unit = "racion", order = 0),
                IngredientRef(ingredient = ingredients["Huevos frescos"], qty = "2", unit = "unidades", order = 1),
                IngredientRef(ingredient = ingredients["Pan rallado"], qty = "150", unit = "g", order = 2),
                IngredientRef(ingredient = ingredients["Cebollino"], qty = "20", unit = "g", order = 3)
            )
        )

        val bravasRecipe = createMainRecipe(
            tenantId, restaurantId, owner.id,
            "Patatas Rebeldes Bravas",
            "Patatas fritas con alioli casero y salsa brava picante",
            "Entrantes",
            listOf(
                IngredientRef(ingredient = ingredients["Patata"], qty = "400", unit = "g", order = 0),
                IngredientRef(subRecipe = alioli, qty = "1", unit = "racion", order = 1),
                IngredientRef(ingredient = ingredients["Mostaza Dijon"], qty = "10", unit = "g", order = 2),
                IngredientRef(ingredient = ingredients["Tomate"], qty = "100", unit = "g", order = 3)
            )
        )

        val ensaladillaRecipe = createMainRecipe(
            tenantId, restaurantId, owner.id,
            "Ensaladilla Ekaterina",
            "Ensaladilla rusa de la abuela con anchoas del Cantabrico",
            "Entrantes",
            listOf(
                IngredientRef(ingredient = ingredients["Patata"], qty = "300", unit = "g", order = 0),
                IngredientRef(ingredient = ingredients["Mayonesa comercial"], qty = "100", unit = "g", order = 1),
                IngredientRef(ingredient = ingredients["Anchoas en aceite"], qty = "30", unit = "g", order = 2),
                IngredientRef(ingredient = ingredients["Huevos frescos"], qty = "2", unit = "unidades", order = 3)
            )
        )

        val burrataRecipe = createMainRecipe(
            tenantId, restaurantId, owner.id,
            "Burrata Campana",
            "Burrata fresca con pesto de nueces, tomate cherry y reduccion de balsamico",
            "Principales",
            listOf(
                IngredientRef(ingredient = ingredients["Parmesano"], qty = "30", unit = "g", order = 0),
                IngredientRef(ingredient = ingredients["Nueces"], qty = "40", unit = "g", order = 1),
                IngredientRef(ingredient = ingredients["Tomate"], qty = "150", unit = "g", order = 2),
                IngredientRef(ingredient = ingredients["Aceite de oliva"], qty = "30", unit = "ml", order = 3)
            )
        )

        val butifarraRecipe = createMainRecipe(
            tenantId, restaurantId, owner.id,
            "Butifarra Cordobesa a la Brasa",
            "Butifarra artesanal a la brasa con huevo frito y mostaza antigua",
            "Principales",
            listOf(
                IngredientRef(ingredient = ingredients["Butifarra"], qty = "200", unit = "g", order = 0),
                IngredientRef(ingredient = ingredients["Huevos frescos"], qty = "1", unit = "unidades", order = 1),
                IngredientRef(ingredient = ingredients["Mostaza Dijon"], qty = "15", unit = "g", order = 2)
            )
        )
        logger.info("Created 5 main recipes")

        // --- 7. Menu ---
        val menu = menuRepository.save(
            Menu(
                restaurantId = restaurantId,
                tenantId = tenantId,
                name = "Carta Primavera 2026",
                description = "Nuestra carta de temporada con productos frescos de primavera",
                isPublished = true
            )
        )
        logger.info("Created menu: {}", menu.name)

        // --- 8. Menu Sections ---
        val sectionEntrantes = menuSectionRepository.save(
            MenuSection(
                menu = menu,
                tenantId = tenantId,
                name = "Entrantes",
                displayOrder = 0
            )
        )
        val sectionPrincipales = menuSectionRepository.save(
            MenuSection(
                menu = menu,
                tenantId = tenantId,
                name = "Principales",
                displayOrder = 1
            )
        )
        val sectionPostres = menuSectionRepository.save(
            MenuSection(
                menu = menu,
                tenantId = tenantId,
                name = "Postres",
                displayOrder = 2
            )
        )
        logger.info("Created 3 menu sections: Entrantes, Principales, Postres")

        // --- 9. Link recipes to menu via MenuRecipe ---
        val menuRecipes = listOf(
            MenuRecipe(menuId = menu.id, recipeId = croquetasRecipe.id, sectionName = "Entrantes", sortOrder = 0, tenantId = tenantId),
            MenuRecipe(menuId = menu.id, recipeId = bravasRecipe.id, sectionName = "Entrantes", sortOrder = 1, tenantId = tenantId),
            MenuRecipe(menuId = menu.id, recipeId = ensaladillaRecipe.id, sectionName = "Entrantes", sortOrder = 2, tenantId = tenantId),
            MenuRecipe(menuId = menu.id, recipeId = burrataRecipe.id, sectionName = "Principales", sortOrder = 0, tenantId = tenantId),
            MenuRecipe(menuId = menu.id, recipeId = butifarraRecipe.id, sectionName = "Principales", sortOrder = 1, tenantId = tenantId)
        )
        menuRecipeRepository.saveAll(menuRecipes)
        logger.info("Linked {} recipes to menu via MenuRecipe", menuRecipes.size)

        // --- 10. Dishes in sections ---
        val allergensByCode = allergenRepository.findAll().associateBy { it.code }

        val croquetasDish = dishRepository.save(
            Dish(
                section = sectionEntrantes,
                tenantId = tenantId,
                name = "Croquetas Ibericas del Puchero",
                description = "6 unidades de croquetas cremosas de jamon iberico",
                price = BigDecimal("12.50"),
                recipe = croquetasRecipe
            )
        )
        saveDishAllergens(croquetasDish, tenantId, allergensByCode, mapOf(
            "GLUTEN" to ContainmentLevel.CONTAINS,
            "MILK" to ContainmentLevel.CONTAINS,
            "EGGS" to ContainmentLevel.CONTAINS
        ))

        val bravasDish = dishRepository.save(
            Dish(
                section = sectionEntrantes,
                tenantId = tenantId,
                name = "Patatas Rebeldes Bravas",
                description = "Patatas fritas crujientes con alioli casero y brava picante",
                price = BigDecimal("8.00"),
                recipe = bravasRecipe
            )
        )
        saveDishAllergens(bravasDish, tenantId, allergensByCode, mapOf(
            "EGGS" to ContainmentLevel.CONTAINS,
            "MUSTARD" to ContainmentLevel.CONTAINS
        ))

        val ensaladillaDish = dishRepository.save(
            Dish(
                section = sectionEntrantes,
                tenantId = tenantId,
                name = "Ensaladilla Ekaterina",
                description = "Ensaladilla rusa con anchoas del Cantabrico y aceitunas",
                price = BigDecimal("9.50"),
                recipe = ensaladillaRecipe
            )
        )
        saveDishAllergens(ensaladillaDish, tenantId, allergensByCode, mapOf(
            "EGGS" to ContainmentLevel.CONTAINS,
            "FISH" to ContainmentLevel.CONTAINS
        ))

        val burrataDish = dishRepository.save(
            Dish(
                section = sectionPrincipales,
                tenantId = tenantId,
                name = "Burrata Campana",
                description = "Burrata fresca con pesto de nueces y tomate cherry",
                price = BigDecimal("14.00"),
                recipe = burrataRecipe
            )
        )
        saveDishAllergens(burrataDish, tenantId, allergensByCode, mapOf(
            "MILK" to ContainmentLevel.CONTAINS,
            "NUTS" to ContainmentLevel.CONTAINS,
            "PEANUTS" to ContainmentLevel.MAY_CONTAIN
        ))

        val butifarraDish = dishRepository.save(
            Dish(
                section = sectionPrincipales,
                tenantId = tenantId,
                name = "Butifarra Cordobesa a la Brasa",
                description = "Butifarra artesanal con huevo frito y mostaza antigua",
                price = BigDecimal("16.00"),
                recipe = butifarraRecipe
            )
        )
        saveDishAllergens(butifarraDish, tenantId, allergensByCode, mapOf(
            "EGGS" to ContainmentLevel.CONTAINS,
            "MUSTARD" to ContainmentLevel.CONTAINS
        ))
        logger.info("Created 5 dishes with allergen declarations")

        // --- 11. Digital Card ---
        digitalCardRepository.save(
            DigitalCard(
                restaurantId = restaurantId,
                menuId = menu.id,
                tenantId = tenantId,
                slug = "la-brava-piconera",
                isActive = true,
                customCss = """{"primaryColor":"#D4381F","fontFamily":"Playfair Display"}"""
            )
        )
        logger.info("Created digital card with slug 'la-brava-piconera'")

        logger.info("Dev data seeder completed successfully for 'La Brava Piconera'")
    }

    private fun setTenant(tenantId: UUID) {
        TenantContext.setTenant(tenantId.toString())
        entityManager.createNativeQuery("SELECT set_config('app.current_tenant', :tenantId, true)")
            .setParameter("tenantId", tenantId.toString())
            .singleResult
        logger.info("Set RLS tenant context to {}", tenantId)
    }

    private fun seedIngredients(tenantId: UUID, createdBy: UUID): Map<String, Ingredient> {
        data class IngredientSeed(
            val name: String,
            val brand: String?,
            val allergens: String,
            val traces: String = "[]"
        )

        val seeds = listOf(
            IngredientSeed("Harina de trigo", "La Meta",
                """[{"code":"GLUTEN","level":"CONTAINS"}]"""),
            IngredientSeed("Gambas peladas", "Pescanova",
                """[{"code":"CRUSTACEANS","level":"CONTAINS"}]"""),
            IngredientSeed("Leche entera", "Central Lechera",
                """[{"code":"MILK","level":"CONTAINS"}]"""),
            IngredientSeed("Huevos frescos", "Granja El Roble",
                """[{"code":"EGGS","level":"CONTAINS"}]"""),
            IngredientSeed("Aceite de cacahuete", null,
                """[{"code":"PEANUTS","level":"CONTAINS"}]"""),
            IngredientSeed("Anchoas en aceite", "Conservas Ortiz",
                """[{"code":"FISH","level":"CONTAINS"}]"""),
            IngredientSeed("Salsa de soja", "Kikkoman",
                """[{"code":"SOYBEANS","level":"CONTAINS"}]"""),
            IngredientSeed("Patata", null, "[]"),
            IngredientSeed("Cebollino", null, "[]"),
            IngredientSeed("Butifarra", null, "[]"),
            IngredientSeed("Tomate", null, "[]"),
            IngredientSeed("Aceite de oliva", null, "[]"),
            IngredientSeed("Pan rallado", null,
                """[{"code":"GLUTEN","level":"CONTAINS"}]"""),
            IngredientSeed("Mayonesa comercial", null,
                """[{"code":"EGGS","level":"CONTAINS"}]"""),
            IngredientSeed("Mostaza Dijon", null,
                """[{"code":"MUSTARD","level":"CONTAINS"}]"""),
            IngredientSeed("Parmesano", null,
                """[{"code":"MILK","level":"CONTAINS"}]"""),
            IngredientSeed("Nueces", null,
                """[{"code":"NUTS","level":"CONTAINS"}]""")
        )

        return seeds.associate { seed ->
            val ingredient = ingredientRepository.save(
                Ingredient(
                    tenantId = tenantId,
                    name = seed.name,
                    brand = seed.brand,
                    allergens = seed.allergens,
                    traces = seed.traces,
                    createdBy = createdBy
                )
            )
            seed.name to ingredient
        }
    }

    private data class IngredientRef(
        val ingredient: Ingredient? = null,
        val subRecipe: Recipe? = null,
        val qty: String,
        val unit: String,
        val order: Int
    )

    private fun createMainRecipe(
        tenantId: UUID,
        restaurantId: UUID,
        createdBy: UUID,
        name: String,
        description: String,
        category: String,
        refs: List<IngredientRef>
    ): Recipe {
        val recipe = recipeRepository.save(
            Recipe(
                tenantId = tenantId,
                restaurantId = restaurantId,
                name = name,
                description = description,
                category = category,
                isSubElaboration = false,
                createdBy = createdBy
            )
        )
        recipeIngredientRepository.saveAll(
            refs.map { ref ->
                RecipeIngredient(
                    recipe = recipe,
                    ingredient = ref.ingredient,
                    subRecipe = ref.subRecipe,
                    tenantId = tenantId,
                    quantity = BigDecimal(ref.qty),
                    unit = ref.unit,
                    sortOrder = ref.order
                )
            }
        )
        return recipe
    }

    private fun saveDishAllergens(
        dish: Dish,
        tenantId: UUID,
        allergensByCode: Map<String, com.apptolast.menus.allergen.model.entity.Allergen>,
        allergenLevels: Map<String, ContainmentLevel>
    ) {
        allergenLevels.forEach { (code, level) ->
            val allergen = allergensByCode[code]
            if (allergen != null) {
                dishAllergenRepository.save(
                    DishAllergen(
                        dish = dish,
                        allergen = allergen,
                        tenantId = tenantId,
                        containmentLevel = level
                    )
                )
            } else {
                logger.warn("Allergen with code '{}' not found in database. Skipping for dish '{}'.", code, dish.name)
            }
        }
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
