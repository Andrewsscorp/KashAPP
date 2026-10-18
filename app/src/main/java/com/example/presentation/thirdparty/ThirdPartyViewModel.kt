package com.example.presentation.thirdparty

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.dao.FixedAssetDao
import com.example.data.local.entity.ThirdPartyEntity
import com.example.domain.repository.ThirdPartyRepository
import com.example.presentation.thirdparty.export.ThirdPartyPdfExporter
import com.example.presentation.thirdparty.model.ThirdPartyCategoryFilter
import com.example.presentation.thirdparty.model.ThirdPartyFormState
import com.example.presentation.thirdparty.model.ThirdPartyUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ThirdPartyViewModel(
    private val thirdPartyRepository: ThirdPartyRepository,
    private val fixedAssetDao: FixedAssetDao? = null,
    private val userId: String = "user_default"
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThirdPartyUiState(isLoading = true))
    val uiState: StateFlow<ThirdPartyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            thirdPartyRepository.seedDefaultThirdPartiesIfNeeded(userId)
        }

        viewModelScope.launch {
            thirdPartyRepository.observeThirdParties(userId).collect { list ->
                _uiState.update { current ->
                    val filtered = applyFilterAndSearch(list, current.searchQuery, current.selectedFilter)
                    current.copy(
                        thirdParties = list,
                        filteredList = filtered,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { current ->
            val filtered = applyFilterAndSearch(current.thirdParties, query, current.selectedFilter)
            current.copy(searchQuery = query, filteredList = filtered)
        }
    }

    fun onSelectFilter(filter: ThirdPartyCategoryFilter) {
        _uiState.update { current ->
            val filtered = applyFilterAndSearch(current.thirdParties, current.searchQuery, filter)
            current.copy(selectedFilter = filter, filteredList = filtered)
        }
    }

    private fun applyFilterAndSearch(
        list: List<ThirdPartyEntity>,
        query: String,
        filter: ThirdPartyCategoryFilter
    ): List<ThirdPartyEntity> {
        return list.filter { entity ->
            val matchesCategory = when (filter) {
                ThirdPartyCategoryFilter.TODOS -> true
                ThirdPartyCategoryFilter.FAVORITOS -> entity.isFavorite
                else -> entity.category.equals(filter.code, ignoreCase = true)
            }

            val q = query.trim().lowercase()
            val matchesQuery = if (q.isBlank()) true else {
                entity.name.lowercase().contains(q) ||
                (entity.commercialName?.lowercase()?.contains(q) == true) ||
                entity.idNumber.contains(q) ||
                (entity.primaryPhone?.contains(q) == true) ||
                (entity.city?.lowercase()?.contains(q) == true)
            }

            matchesCategory && matchesQuery
        }
    }

    fun onOpenCreateDialog(isExpress: Boolean = true, prefilledState: ThirdPartyFormState? = null) {
        _uiState.update {
            it.copy(
                showCreateDialog = true,
                formState = prefilledState ?: ThirdPartyFormState(isExpressMode = isExpress),
                errorMessage = null
            )
        }
    }

    fun onDismissCreateDialog() {
        _uiState.update {
            it.copy(showCreateDialog = false, errorMessage = null)
        }
    }

    fun onUpdateForm(reducer: (ThirdPartyFormState) -> ThirdPartyFormState) {
        _uiState.update { it.copy(formState = reducer(it.formState)) }
    }

    fun onImportFromContact(name: String, phone: String?, email: String?) {
        _uiState.update { current ->
            current.copy(
                showCreateDialog = true,
                formState = current.formState.copy(
                    name = name,
                    primaryPhone = phone ?: current.formState.primaryPhone,
                    email = email ?: current.formState.email,
                    isExpressMode = false
                ),
                successMessage = "Contacto importado: $name. Completa la identificación."
            )
        }
    }

    fun onToggleFavorite(id: String, isFavorite: Boolean) {
        viewModelScope.launch {
            thirdPartyRepository.setFavorite(id, isFavorite)
        }
    }

    fun onSelectThirdPartyForDetail(entity: ThirdPartyEntity?) {
        _uiState.update { it.copy(selectedThirdPartyForDetail = entity) }
    }

    fun onSaveThirdParty(onSuccess: (ThirdPartyEntity) -> Unit = {}) {
        val form = _uiState.value.formState
        if (form.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "El nombre o razón social es obligatorio.") }
            return
        }
        if (form.idNumber.isBlank()) {
            _uiState.update { it.copy(errorMessage = "El número de documento o NIT es obligatorio.") }
            return
        }

        viewModelScope.launch {
            val entity = form.toEntity(userId)
            val result = thirdPartyRepository.saveThirdParty(entity)
            result.onSuccess { saved ->
                _uiState.update {
                    it.copy(
                        showCreateDialog = false,
                        errorMessage = null,
                        successMessage = "Tercero '${saved.name}' guardado exitosamente."
                    )
                }
                onSuccess(saved)
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Error al guardar tercero.") }
            }
        }
    }

    fun onDeleteThirdParty(id: String) {
        viewModelScope.launch {
            thirdPartyRepository.deleteThirdParty(id)
            _uiState.update {
                it.copy(
                    selectedThirdPartyForDetail = null,
                    successMessage = "Tercero eliminado correctamente."
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun onExportPdf(context: Context, entity: ThirdPartyEntity, onResult: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingPdf = true) }
            val result = ThirdPartyPdfExporter.exportThirdPartyToPdf(
                context = context,
                thirdParty = entity
            )
            _uiState.update { it.copy(isExportingPdf = false) }
            onResult(result.message)
        }
    }

    class Factory(
        private val thirdPartyRepository: ThirdPartyRepository,
        private val fixedAssetDao: FixedAssetDao? = null,
        private val userId: String = "user_default"
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ThirdPartyViewModel(thirdPartyRepository, fixedAssetDao, userId) as T
        }
    }
}
