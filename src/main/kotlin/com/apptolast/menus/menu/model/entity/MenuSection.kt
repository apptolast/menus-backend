package com.apptolast.menus.menu.model.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "menu_section")
class MenuSection(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    val menu: Menu = Menu(),

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),

    @Column(name = "name", nullable = false, length = 255)
    var name: String = "",

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @OneToMany(mappedBy = "section", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val dishes: MutableList<com.apptolast.menus.dish.model.entity.Dish> = mutableListOf()
)
