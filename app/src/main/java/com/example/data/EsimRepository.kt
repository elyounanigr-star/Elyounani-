package com.example.data

import android.content.Context
import com.example.R
import com.example.model.EsimCatalog
import com.example.model.EsimStatus
import com.example.model.VirtualEsim
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class EsimRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireSignedInUid(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user operation." }
        return uid
    }

    fun observeEsims(userId: String): Flow<List<VirtualEsim>> = flow {
        val uid = requireSignedInUid(userId)
        val path = "users/$uid/esims"
        emitAll(
            db.collection("users")
                .document(uid)
                .collection("esims")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VirtualEsim::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createEsim(userId: String, esim: VirtualEsim): Result<String> {
        var docPath = "users/$userId/esims"
        return try {
            val uid = requireSignedInUid(userId)
            validateCreateFields(esim)
            val colRef = db.collection("users").document(uid).collection("esims")
            val docRef = colRef.document()
            docPath = docRef.path
            docRef.set(esim.toCreateMap(uid)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun seedStarterEsims(userId: String): Result<Unit> {
        var path = "users/$userId/esims"
        return try {
            val uid = requireSignedInUid(userId)
            val colRef = db.collection("users").document(uid).collection("esims")
            val batch = db.batch()
            EsimCatalog.starterProfiles(uid).forEach { profile ->
                val docRef = colRef.document()
                path = docRef.path
                batch.set(docRef, profile.toCreateMap(uid))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun activateEsimSlot(
        userId: String,
        targetEsimId: String,
        allEsims: List<VirtualEsim>
    ): Result<Unit> {
        var docPath = "users/$userId/esims/$targetEsimId"
        return try {
            val uid = requireSignedInUid(userId)
            require(targetEsimId.isNotBlank()) { "eSIM ID cannot be blank." }
            val colRef = db.collection("users").document(uid).collection("esims")
            val batch = db.batch()

            allEsims.filter { it.id != targetEsimId && it.status == EsimStatus.ACTIVE.name }
                .forEach { currentActive ->
                    batch.update(
                        colRef.document(currentActive.id),
                        mapOf(
                            "status" to EsimStatus.STANDBY.name,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

            val targetRef = colRef.document(targetEsimId)
            docPath = targetRef.path
            batch.update(
                targetRef,
                mapOf(
                    "status" to EsimStatus.ACTIVE.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateStatusAndUsage(
        userId: String,
        esimId: String,
        status: String? = null,
        dataUsedMb: Long? = null,
        dataLimitMb: Long? = null,
        validityDays: Long? = null,
        roamingEnabled: Boolean? = null,
        fiveGEnabled: Boolean? = null,
        unlimitedData: Boolean? = null
    ): Result<Unit> {
        var docPath = "users/$userId/esims/$esimId"
        return try {
            val uid = requireSignedInUid(userId)
            require(esimId.isNotBlank()) { "eSIM ID cannot be blank." }
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            status?.let { updates["status"] = EsimStatus.fromRaw(it).name }
            dataUsedMb?.let { updates["dataUsedMb"] = it.coerceIn(0L, 10_485_760L) }
            dataLimitMb?.let { updates["dataLimitMb"] = it.coerceIn(0L, 10_485_760L) }
            validityDays?.let { updates["validityDays"] = it.coerceIn(1L, 3650L) }
            roamingEnabled?.let { updates["roamingEnabled"] = it }
            fiveGEnabled?.let { updates["fiveGEnabled"] = it }
            unlimitedData?.let { updates["unlimitedData"] = it }

            val docRef = db.collection("users").document(uid).collection("esims").document(esimId)
            docPath = docRef.path
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateProfileDetails(
        userId: String,
        esimId: String,
        label: String,
        carrierName: String,
        countryCode: String,
        countryName: String,
        planType: String,
        apn: String,
        mccMnc: String,
        colorAccent: String,
        notes: String
    ): Result<Unit> {
        var docPath = "users/$userId/esims/$esimId"
        return try {
            val uid = requireSignedInUid(userId)
            require(esimId.isNotBlank()) { "eSIM ID cannot be blank." }
            require(label.trim().length in 1..50) { "Label must be 1–50 characters." }
            require(carrierName.trim().length in 1..40) { "Carrier name must be 1–40 characters." }
            require(apn.trim().length in 1..60) { "APN must be 1–60 characters." }

            val docRef = db.collection("users").document(uid).collection("esims").document(esimId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "label" to label.trim().take(50),
                    "carrierName" to carrierName.trim().take(40),
                    "countryCode" to countryCode.trim().uppercase().take(10),
                    "countryName" to countryName.trim().take(40),
                    "planType" to planType,
                    "apn" to apn.trim().take(60),
                    "mccMnc" to mccMnc.trim().take(10),
                    "colorAccent" to colorAccent,
                    "notes" to notes.trim().take(250),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteEsim(userId: String, esimId: String): Result<Unit> {
        var docPath = "users/$userId/esims/$esimId"
        return try {
            val uid = requireSignedInUid(userId)
            require(esimId.isNotBlank()) { "eSIM ID cannot be blank." }
            val docRef = db.collection("users").document(uid).collection("esims").document(esimId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun validateCreateFields(esim: VirtualEsim) {
        val cleanIccid = esim.iccid.filter { it.isDigit() }
        require(esim.label.trim().length in 1..50) { "Profile label must be 1–50 characters." }
        require(esim.carrierName.trim().length in 1..40) { "Carrier name must be 1–40 characters." }
        require(esim.countryCode.trim().length in 2..10) { "Country code must be 2–10 characters." }
        require(esim.countryName.trim().length in 1..40) { "Country name must be 1–40 characters." }
        require(cleanIccid.length in 15..22) { "ICCID must contain 15–22 digits." }
        require(esim.smdpAddress.trim().length in 3..120) { "SM-DP+ address must be 3–120 characters." }
        require(esim.activationCode.trim().length in 3..120) { "Activation code must be 3–120 characters." }
        require(esim.apn.trim().length in 1..60) { "APN must be 1–60 characters." }
        require(esim.mccMnc.trim().length in 3..10) { "MCC/MNC must be 3–10 characters." }
        require(esim.validityDays in 1L..3650L) { "Validity must be between 1 and 3650 days." }
    }
}
