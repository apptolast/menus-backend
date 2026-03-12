package com.apptolast.menus.allergen.model.entity

import jakarta.persistence.*

@Entity
@Table(name = "allergens")
class Allergen(
    @Id
    val id: Int = 0,

    @Column(name = "code", unique = true, nullable = false, length = 20)
    val code: String = "",

    @Column(name = "name_es", nullable = false, length = 100)
    val nameEs: String = "",

    @Column(name = "name_en", nullable = false, length = 100)
    val nameEn: String = "",

    @Column(name = "icon_url", length = 500)
    val iconUrl: String? = null,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int = 0
)
