package com.apptolast.menus.menu.model.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "sections")
class MenuSection(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "menu_id", nullable = false)
    val menu: Menu = Menu(),

    @Column(name = "name", nullable = false, length = 255)
    var name: String = "",

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0
)
