package com.desego.data
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RemoteStats(val salesToday: Double, val ordersToday: Int, val pendingOrders: Int)
object DatabaseApi {
    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        runCatching { BackendClient.baseUrl(); true }.getOrDefault(false)
    }
    suspend fun fetchStats(): RemoteStats? = withContext(Dispatchers.IO) {
        runCatching {
            val data=BackendClient.request("api/mobile/dashboard")
            RemoteStats(data.optDouble("sales",0.0),data.optInt("orders_today",0),data.optInt("pending",0))
        }.getOrNull()
    }
}
