package com.apptolast.menus.menudigitalcard.repository

import com.apptolast.menus.menudigitalcard.model.entity.MenuDigitalCard
import com.apptolast.menus.menudigitalcard.repository.jpa.JpaMenuDigitalCardRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class MenuDigitalCardRepositoryAdapter(
    private val jpa: JpaMenuDigitalCardRepository
) : MenuDigitalCardRepository {

    override fun findByMenuId(menuId: UUID): List<MenuDigitalCard> =
        jpa.findByMenuId(menuId)

    override fun findById(id: UUID): MenuDigitalCard? =
        jpa.findById(id).orElse(null)

    override fun save(entity: MenuDigitalCard): MenuDigitalCard =
        jpa.save(entity)

    override fun deleteById(id: UUID) =
        jpa.deleteById(id)

    override fun existsByMenuIdAndDishId(menuId: UUID, dishId: UUID): Boolean =
        jpa.existsByMenuIdAndDishId(menuId, dishId)

    override fun existsById(id: UUID): Boolean =
        jpa.existsById(id)
}
