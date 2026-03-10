package com.apptolast.menus.allergen.model.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "allergen_translation",
    uniqueConstraints = [UniqueConstraint(columnNames = ["allergen_id", "locale"])]
)
class AllergenTranslation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allergen_id", nullable = false)
    val allergen: Allergen,

    @Column(name = "locale", nullable = false, length = 5)
    val locale: String = "",

    @Column(name = "name", nullable = false, length = 100)
    val name: String = "",

    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null
)
