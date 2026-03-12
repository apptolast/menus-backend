package com.apptolast.menus.shared.exception

class MaxDepthExceededException(
    message: String = "Sub-recipe nesting exceeds maximum depth of 10"
) : BusinessException("MAX_RECIPE_DEPTH_EXCEEDED", message, 422)
