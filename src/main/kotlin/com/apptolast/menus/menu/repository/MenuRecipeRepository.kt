package com.apptolast.menus.menu.repository

import com.apptolast.menus.menu.model.entity.MenuRecipe
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface MenuRecipeRepository : JpaRepository<MenuRecipe, UUID> {
    @Modifying
    @Query("DELETE FROM MenuRecipe mr WHERE mr.menu.id = :menuId")
    fun deleteByMenuId(menuId: UUID)

    fun findByMenuId(menuId: UUID): List<MenuRecipe>
}
