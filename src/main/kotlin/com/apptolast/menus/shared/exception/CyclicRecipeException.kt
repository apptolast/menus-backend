package com.apptolast.menus.shared.exception

class CyclicRecipeException(
    message: String = "Sub-recipe chain contains a cycle"
) : BusinessException("CYCLIC_RECIPE_DETECTED", message, 422)
