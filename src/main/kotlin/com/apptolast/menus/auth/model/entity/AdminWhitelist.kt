package com.apptolast.menus.auth.model.entity

import jakarta.persistence.*

@Entity
@Table(name = "admin_whitelist")
class AdminWhitelist(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(name = "email", nullable = false, unique = true, length = 255)
    var email: String = ""
)
