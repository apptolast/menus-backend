package com.apptolast.menus.allergen.model.entity

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "allergen")
class Allergen(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(name = "code", unique = true, nullable = false, length = 20)
    val code: String = "",

    @Column(name = "icon_url", length = 500)
    val iconUrl: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @OneToMany(mappedBy = "allergen", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val translations: MutableList<AllergenTranslation> = mutableListOf()
)
