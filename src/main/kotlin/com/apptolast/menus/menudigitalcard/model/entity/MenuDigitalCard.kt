package com.apptolast.menus.menudigitalcard.model.entity

import com.apptolast.menus.dish.model.entity.Dish
import com.apptolast.menus.menu.model.entity.Menu
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "menu_digital_cards",
    uniqueConstraints = [UniqueConstraint(columnNames = ["menu_id", "dish_id"])]
)
class MenuDigitalCard(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "menu_id", nullable = false)
    val menu: Menu,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dish_id", nullable = false)
    var dish: Dish,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
