package com.apptolast.menus.menu.model.entity

import com.apptolast.menus.recipe.model.entity.Recipe
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "menu_recipes",
    uniqueConstraints = [UniqueConstraint(columnNames = ["menu_id", "recipe_id"])]
)
class MenuRecipe(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "menu_id", nullable = false)
    val menu: Menu,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recipe_id", nullable = false)
    val recipe: Recipe
)
