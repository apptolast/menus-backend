package com.apptolast.menus.allergen.model.enum

enum class AllergenType(
    val displayName: String,
    val abbreviation: String,
    val emoji: String,
    val keywords: List<String>
) {
    GLUTEN(
        displayName = "Gluten",
        abbreviation = "GLU",
        emoji = "\uD83C\uDF3E",
        keywords = listOf("gluten", "wheat", "trigo", "barley", "cebada", "rye", "centeno", "oats", "avena", "spelt", "espelta", "kamut")
    ),
    CRUSTACEANS(
        displayName = "Crustaceans",
        abbreviation = "CRU",
        emoji = "\uD83E\uDD80",
        keywords = listOf("crustaceans", "crustaceos", "shrimp", "gamba", "crab", "cangrejo", "lobster", "langosta", "crayfish")
    ),
    EGGS(
        displayName = "Eggs",
        abbreviation = "EGG",
        emoji = "\uD83E\uDD5A",
        keywords = listOf("eggs", "huevos", "egg", "huevo", "albumin", "albumina", "lysozyme", "lisozima", "mayonnaise", "mayonesa")
    ),
    FISH(
        displayName = "Fish",
        abbreviation = "FSH",
        emoji = "\uD83D\uDC1F",
        keywords = listOf("fish", "pescado", "salmon", "tuna", "atun", "cod", "bacalao", "anchovy", "anchoa", "sardine", "sardina")
    ),
    PEANUTS(
        displayName = "Peanuts",
        abbreviation = "PNT",
        emoji = "\uD83E\uDD5C",
        keywords = listOf("peanuts", "cacahuetes", "peanut", "cacahuete", "groundnut", "mani")
    ),
    SOYA(
        displayName = "Soy",
        abbreviation = "SOY",
        emoji = "\uD83E\uDED8",
        keywords = listOf("soybeans", "soja", "soy", "soya", "tofu", "edamame", "tempeh", "miso")
    ),
    MILK(
        displayName = "Milk",
        abbreviation = "MLK",
        emoji = "\uD83E\uDD5B",
        keywords = listOf("milk", "leche", "dairy", "lacteo", "lactosa", "lactose", "cheese", "queso", "butter", "mantequilla", "cream", "nata", "yogurt")
    ),
    TREE_NUTS(
        displayName = "Tree Nuts",
        abbreviation = "NUT",
        emoji = "\uD83C\uDF30",
        keywords = listOf("nuts", "frutos de cascara", "almond", "almendra", "hazelnut", "avellana", "walnut", "nuez", "cashew", "anacardo", "pecan", "pistachio", "pistacho", "macadamia")
    ),
    CELERY(
        displayName = "Celery",
        abbreviation = "CEL",
        emoji = "\uD83E\uDD66",
        keywords = listOf("celery", "apio", "celeriac")
    ),
    MUSTARD(
        displayName = "Mustard",
        abbreviation = "MUS",
        emoji = "\uD83C\uDF2D",
        keywords = listOf("mustard", "mostaza", "mustard seed", "semilla de mostaza")
    ),
    SESAME(
        displayName = "Sesame",
        abbreviation = "SES",
        emoji = "\uD83C\uDF6A",
        keywords = listOf("sesame", "sesamo", "tahini", "tahina")
    ),
    SULPHITES(
        displayName = "Sulphites",
        abbreviation = "SUL",
        emoji = "\uD83C\uDF77",
        keywords = listOf("sulphites", "sulfitos", "sulphur dioxide", "dioxido de azufre", "so2", "sulfites", "bisulphite", "metabisulphite")
    ),
    LUPIN(
        displayName = "Lupin",
        abbreviation = "LUP",
        emoji = "\uD83C\uDF3B",
        keywords = listOf("lupin", "altramuces", "lupine", "altramuz")
    ),
    MOLLUSCS(
        displayName = "Molluscs",
        abbreviation = "MOL",
        emoji = "\uD83D\uDC19",
        keywords = listOf("molluscs", "moluscos", "squid", "calamar", "octopus", "pulpo", "mussel", "mejillon", "clam", "almeja", "oyster", "ostra", "snail", "caracol")
    );

    companion object {
        fun fromKeyword(keyword: String): AllergenType? {
            val lower = keyword.lowercase()
            return entries.firstOrNull { type ->
                type.keywords.any { it.equals(lower, ignoreCase = true) }
            }
        }
    }
}
