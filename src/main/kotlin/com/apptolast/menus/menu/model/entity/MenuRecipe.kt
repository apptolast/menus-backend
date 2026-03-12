package com.apptolast.menus.menu.model.entity

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

    @Column(name = "menu_id", nullable = false)
    val menuId: UUID = UUID.randomUUID(),

    @Column(name = "recipe_id", nullable = false)
    val recipeId: UUID = UUID.randomUUID(),

    @Column(name = "section_name", length = 255)
    var sectionName: String = "General",

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID()
) {
    constructor() : this(id = UUID.randomUUID())
}
