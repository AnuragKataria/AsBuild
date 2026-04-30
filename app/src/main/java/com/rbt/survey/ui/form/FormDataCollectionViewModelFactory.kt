package com.rbt.survey.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rbt.survey.data.local.UserPreferences
import com.rbt.survey.data.repository.FormRepository
import com.rbt.survey.dgps.DgpsManager

class FormDataCollectionViewModelFactory(
    private val formId: Int,
    private val blockCode: String?,
    private val repository: FormRepository,
    private val preferences: UserPreferences,
    private val gpName: String?,
    private val dgpsManager: DgpsManager,
    private val submissionId: Int? = null,
    private val radius: Int? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FormDataCollectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FormDataCollectionViewModel(formId, blockCode, repository, preferences, gpName, dgpsManager, submissionId,radius) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
