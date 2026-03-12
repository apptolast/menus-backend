package com.apptolast.menus.dashboard.service

import com.apptolast.menus.dashboard.dto.response.DashboardStatsResponse
import java.util.UUID

interface DashboardService {
    fun getStats(restaurantId: UUID): DashboardStatsResponse
}
