package com.desego.data

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.*
import org.json.JSONObject

object StoreSync {
    fun schedule(context: Context) {
        val scheduler = context.getSystemService(JobScheduler::class.java)
        scheduler.schedule(JobInfo.Builder(7301, ComponentName(context, StoreSyncJob::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true)
            .setBackoffCriteria(30_000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            .build())
    }

    /** A restored cookie is verified before sending any user's pending changes. */
    fun refresh(context: Context, scope: String?): JSONObject = synchronized(BackendClient) {
        BackendClient.configure(context)
        OfflineStore(context).use { database ->
            val session = BackendClient.request("api/mobile/session")
            val email = session.optJSONObject("user")?.optString("email")?.lowercase()
            if (scope != null && email == scope) {
                database.pending(scope).filter { it.error == null }.forEach { mutation ->
                    try {
                        BackendClient.request(mutation.path, mutation.method, mutation.payload)
                        database.acknowledge(mutation)
                    } catch (error: BackendException) {
                        if (error.status in listOf(401, 403, 408, 419, 425, 429) || error.status >= 500) throw error
                        database.reject(mutation, error.message ?: "Revisa el cambio pendiente.")
                    }
                }
            }
            val snapshot = BackendClient.request("api/mobile/sync")
            val catalog = snapshot.getJSONObject("catalog")
            val publicCatalog = JSONObject(catalog.toString()).apply {
                remove("csrfToken"); remove("customer_profile"); remove("customer_addresses"); remove("favorite_ids")
            }
            database.write("public", "catalog", publicCatalog)
            if (scope != null && snapshot.optJSONObject("user")?.optString("email")?.lowercase() == scope) {
                snapshot.optJSONObject("account")?.let { database.write(scope, "account", it) }
            }
            snapshot
        }
    }
}

class StoreSyncJob : JobService() {
    private var work: Job? = null
    override fun onStartJob(params: JobParameters): Boolean {
        work = CoroutineScope(Dispatchers.IO).launch {
            val retry = runCatching {
                BackendClient.configure(applicationContext)
                val scope = DemoAuthStore(applicationContext).cachedUser()?.email?.lowercase()
                StoreSync.refresh(applicationContext, scope)
            }.isFailure
            withContext(Dispatchers.Main) { jobFinished(params, retry) }
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean { work?.cancel(); return true }
    override fun onDestroy() { work?.cancel(); super.onDestroy() }
}
