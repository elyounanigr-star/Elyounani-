package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EsimRepository
import com.example.model.CountryPreset
import com.example.model.EsimAccent
import com.example.model.EsimCatalog
import com.example.model.EsimGenerator
import com.example.model.EsimPlanType
import com.example.model.EsimStatus
import com.example.model.VirtualEsim
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class WalletFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    STANDBY("Standby"),
    EXPIRED("Expired")
}

data class CreateEsimFormState(
    val selectedPresetCode: String = "GLOBAL",
    val label: String = "Global Travel 5G",
    val carrierName: String = "AeroLink Global",
    val countryCode: String = "GLOBAL",
    val countryName: String = "Global 120+",
    val planType: EsimPlanType = EsimPlanType.TRAVEL,
    val iccid: String = EsimGenerator.generateLuhnIccid("901"),
    val smdpAddress: String = "smdp.aerolink.esim",
    val activationCode: String = EsimGenerator.generateActivationCode("GLB"),
    val apn: String = "global.esim",
    val mccMnc: String = "90140",
    val dataLimitGb: Int = 10,
    val validityDays: Int = 30,
    val roamingEnabled: Boolean = true,
    val fiveGEnabled: Boolean = true,
    val unlimitedData: Boolean = false,
    val activateImmediately: Boolean = true,
    val accent: EsimAccent = EsimAccent.CYAN,
    val notes: String = "",
    val lpaImportInput: String = "",
    val isSubmitting: Boolean = false,
    val validationError: String? = null
)

class EsimViewModel(
    private val repository: EsimRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(WalletFilter.ALL)
    val selectedFilter: StateFlow<WalletFilter> = _selectedFilter.asStateFlow()

    private val _selectedEsimId = MutableStateFlow<String?>(null)
    val selectedEsimId: StateFlow<String?> = _selectedEsimId.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _createForm = MutableStateFlow(CreateEsimFormState())
    val createForm: StateFlow<CreateEsimFormState> = _createForm.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val esimsState: StateFlow<UiState<List<VirtualEsim>>> = repository.observeEsims(currentUserId)
        .map<List<VirtualEsim>, UiState<List<VirtualEsim>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing virtual eSIMs", rootError)
            emit(UiState.Error(rootError.message ?: "Unable to sync virtual eSIM profiles."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val filteredEsims: StateFlow<List<VirtualEsim>> = combine(
        esimsState,
        _searchQuery,
        _selectedFilter
    ) { state, query, filter ->
        val list = (state as? UiState.Success)?.data ?: emptyList()
        val q = query.trim().lowercase()
        list.filter { esim ->
            val matchesFilter = when (filter) {
                WalletFilter.ALL -> true
                WalletFilter.ACTIVE -> esim.status == EsimStatus.ACTIVE.name
                WalletFilter.STANDBY -> esim.status == EsimStatus.STANDBY.name
                WalletFilter.EXPIRED -> esim.status == EsimStatus.EXPIRED.name
            }
            val matchesQuery = q.isBlank() ||
                esim.label.lowercase().contains(q) ||
                esim.carrierName.lowercase().contains(q) ||
                esim.countryName.lowercase().contains(q) ||
                esim.iccid.contains(q)
            matchesFilter && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    val selectedEsim: StateFlow<VirtualEsim?> = combine(
        esimsState,
        _selectedEsimId
    ) { state, selectedId ->
        val list = (state as? UiState.Success)?.data ?: emptyList()
        selectedId?.let { id -> list.firstOrNull { it.id == id } }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: WalletFilter) {
        _selectedFilter.value = filter
    }

    fun openDetail(esimId: String) {
        _selectedEsimId.value = esimId
    }

    fun closeDetail() {
        _selectedEsimId.value = null
    }

    fun loadStarterProfiles() {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.seedStarterEsims(currentUserId)
            _isBusy.value = false
            result.onSuccess {
                _events.tryEmit("Loaded 3 starter virtual eSIM profiles.")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to load starter eSIMs.")
            }
        }
    }

    fun activateSlot(esimId: String) {
        val currentList = (esimsState.value as? UiState.Success)?.data ?: emptyList()
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.activateEsimSlot(currentUserId, esimId, currentList)
            _isBusy.value = false
            result.onSuccess {
                _events.tryEmit("Virtual eSIM activated in primary slot.")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to activate eSIM.")
            }
        }
    }

    fun setStatus(esimId: String, status: EsimStatus) {
        viewModelScope.launch {
            val result = repository.updateStatusAndUsage(
                userId = currentUserId,
                esimId = esimId,
                status = status.name
            )
            result.onSuccess {
                _events.tryEmit("Status updated to ${status.displayName}.")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to update status.")
            }
        }
    }

    fun simulateDataUsage(esim: VirtualEsim, addMb: Long = 512L) {
        val nextUsed = if (esim.unlimitedData) {
            (esim.dataUsedMb + addMb).coerceAtMost(10_485_760L)
        } else {
            (esim.dataUsedMb + addMb).coerceAtMost(esim.dataLimitMb.coerceAtLeast(addMb))
        }
        val nextStatus = if (!esim.unlimitedData && esim.dataLimitMb > 0L && nextUsed >= esim.dataLimitMb) {
            EsimStatus.EXPIRED.name
        } else {
            null
        }
        viewModelScope.launch {
            val result = repository.updateStatusAndUsage(
                userId = currentUserId,
                esimId = esim.id,
                status = nextStatus,
                dataUsedMb = nextUsed
            )
            result.onSuccess {
                val msg = if (nextStatus == EsimStatus.EXPIRED.name) {
                    "Data allowance exhausted; profile marked Expired."
                } else {
                    "Simulated +$addMb MB usage."
                }
                _events.tryEmit(msg)
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to update usage.")
            }
        }
    }

    fun topUpData(esim: VirtualEsim, addMb: Long = 5120L) {
        val nextLimit = (esim.dataLimitMb + addMb).coerceAtMost(10_485_760L)
        val nextValidity = (esim.validityDays + 15L).coerceAtMost(3650L)
        val nextStatus = if (esim.status == EsimStatus.EXPIRED.name) EsimStatus.STANDBY.name else null
        viewModelScope.launch {
            val result = repository.updateStatusAndUsage(
                userId = currentUserId,
                esimId = esim.id,
                status = nextStatus,
                dataLimitMb = nextLimit,
                validityDays = nextValidity
            )
            result.onSuccess {
                _events.tryEmit("Added +5 GB and +15 days validity.")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to top up data.")
            }
        }
    }

    fun toggleRoaming(esim: VirtualEsim) {
        viewModelScope.launch {
            val result = repository.updateStatusAndUsage(
                userId = currentUserId,
                esimId = esim.id,
                roamingEnabled = !esim.roamingEnabled
            )
            result.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to toggle roaming.")
            }
        }
    }

    fun toggleFiveG(esim: VirtualEsim) {
        viewModelScope.launch {
            val result = repository.updateStatusAndUsage(
                userId = currentUserId,
                esimId = esim.id,
                fiveGEnabled = !esim.fiveGEnabled
            )
            result.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to toggle 5G.")
            }
        }
    }

    fun updateProfileDetails(
        esim: VirtualEsim,
        newLabel: String,
        newCarrier: String,
        newApn: String,
        newNotes: String,
        newAccent: EsimAccent
    ) {
        val cleanLabel = newLabel.trim()
        val cleanCarrier = newCarrier.trim()
        val cleanApn = newApn.trim()
        if (cleanLabel.isEmpty() || cleanCarrier.isEmpty() || cleanApn.isEmpty()) {
            _events.tryEmit("Label, carrier, and APN cannot be empty.")
            return
        }
        viewModelScope.launch {
            val result = repository.updateProfileDetails(
                userId = currentUserId,
                esimId = esim.id,
                label = cleanLabel,
                carrierName = cleanCarrier,
                countryCode = esim.countryCode,
                countryName = esim.countryName,
                planType = esim.planType,
                apn = cleanApn,
                mccMnc = esim.mccMnc,
                colorAccent = newAccent.name,
                notes = newNotes.trim()
            )
            result.onSuccess {
                _events.tryEmit("eSIM profile updated.")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to save changes.")
            }
        }
    }

    fun deleteEsim(esimId: String) {
        viewModelScope.launch {
            val result = repository.deleteEsim(currentUserId, esimId)
            result.onSuccess {
                if (_selectedEsimId.value == esimId) {
                    _selectedEsimId.value = null
                }
                _events.tryEmit("Virtual eSIM removed from wallet.")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to delete eSIM.")
            }
        }
    }

    // --- Create Builder Form Controls ---

    fun applyPreset(preset: CountryPreset) {
        _createForm.update { state ->
            state.copy(
                selectedPresetCode = preset.code,
                label = "${preset.name} 5G",
                carrierName = preset.defaultCarrier,
                countryCode = preset.code,
                countryName = preset.name,
                mccMnc = preset.mccMnc,
                apn = preset.defaultApn,
                smdpAddress = preset.smdpHost,
                iccid = EsimGenerator.generateLuhnIccid(preset.mccMnc.take(3)),
                activationCode = EsimGenerator.generateActivationCode(preset.code),
                accent = preset.accent,
                validationError = null
            )
        }
    }

    fun updateCreateForm(transform: (CreateEsimFormState) -> CreateEsimFormState) {
        _createForm.update { state -> transform(state).copy(validationError = null) }
    }

    fun regenerateIccid() {
        _createForm.update { state ->
            state.copy(iccid = EsimGenerator.generateLuhnIccid(state.mccMnc.take(3)))
        }
    }

    fun regenerateActivationCode() {
        _createForm.update { state ->
            state.copy(activationCode = EsimGenerator.generateActivationCode(state.countryCode))
        }
    }

    fun parseLpaImport(): Boolean {
        val raw = _createForm.value.lpaImportInput
        val parsed = EsimGenerator.parseLpaUri(raw)
        return if (parsed != null) {
            _createForm.update { state ->
                state.copy(
                    smdpAddress = parsed.smdpAddress,
                    activationCode = parsed.activationCode,
                    label = if (state.label.isBlank()) "Imported eSIM" else state.label,
                    validationError = null
                )
            }
            _events.tryEmit("Decoded SM-DP+ and activation code from LPA URI.")
            true
        } else {
            _createForm.update { it.copy(validationError = "Invalid LPA string. Expected format: LPA:1\$smdp.host\$CODE") }
            false
        }
    }

    fun createVirtualEsim(onCreated: () -> Unit) {
        val form = _createForm.value
        if (form.isSubmitting) return

        val cleanLabel = form.label.trim()
        val cleanCarrier = form.carrierName.trim()
        val cleanIccid = form.iccid.filter { it.isDigit() }
        val cleanSmdp = form.smdpAddress.trim()
        val cleanCode = form.activationCode.trim()
        val cleanApn = form.apn.trim()

        val errorMsg = when {
            cleanLabel.isEmpty() || cleanLabel.length > 50 -> "Profile label must be 1–50 characters."
            cleanCarrier.isEmpty() || cleanCarrier.length > 40 -> "Carrier name must be 1–40 characters."
            cleanIccid.length !in 15..22 -> "ICCID must contain 15–22 digits."
            cleanSmdp.length !in 3..120 -> "SM-DP+ host must be 3–120 characters."
            cleanCode.length !in 3..120 -> "Activation code must be 3–120 characters."
            cleanApn.isEmpty() || cleanApn.length > 60 -> "APN must be 1–60 characters."
            else -> null
        }

        if (errorMsg != null) {
            _createForm.update { it.copy(validationError = errorMsg) }
            return
        }

        val currentList = (esimsState.value as? UiState.Success)?.data ?: emptyList()

        viewModelScope.launch {
            _createForm.update { it.copy(isSubmitting = true, validationError = null) }
            val newEsim = VirtualEsim(
                userId = currentUserId,
                label = cleanLabel,
                carrierName = cleanCarrier,
                countryCode = form.countryCode.trim().uppercase().ifBlank { "GLB" }.take(10),
                countryName = form.countryName.trim().ifBlank { "Global" }.take(40),
                planType = form.planType.name,
                status = if (form.activateImmediately) EsimStatus.ACTIVE.name else EsimStatus.STANDBY.name,
                iccid = cleanIccid,
                smdpAddress = cleanSmdp,
                activationCode = cleanCode,
                apn = cleanApn,
                mccMnc = form.mccMnc.trim().ifBlank { "90140" }.take(10),
                dataLimitMb = if (form.unlimitedData) 0L else form.dataLimitGb.toLong() * 1024L,
                dataUsedMb = 0L,
                validityDays = form.validityDays.toLong().coerceIn(1L, 3650L),
                roamingEnabled = form.roamingEnabled,
                fiveGEnabled = form.fiveGEnabled,
                unlimitedData = form.unlimitedData,
                colorAccent = form.accent.name,
                notes = form.notes.trim().take(250)
            )

            val createResult = repository.createEsim(currentUserId, newEsim)
            createResult.onSuccess { newId ->
                if (form.activateImmediately && currentList.any { it.status == EsimStatus.ACTIVE.name }) {
                    repository.activateEsimSlot(currentUserId, newId, currentList)
                }
                val preset = EsimCatalog.presets.first()
                _createForm.value = CreateEsimFormState(
                    iccid = EsimGenerator.generateLuhnIccid(preset.mccMnc.take(3)),
                    activationCode = EsimGenerator.generateActivationCode(preset.code)
                )
                _events.tryEmit("Virtual eSIM \"$cleanLabel\" provisioned.")
                onCreated()
            }.onFailure { err ->
                _createForm.update {
                    it.copy(
                        isSubmitting = false,
                        validationError = err.localizedMessage ?: "Failed to create virtual eSIM."
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "EsimViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
