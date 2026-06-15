package com.rbt.survey.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rbt.survey.data.local.UserPreferences
import com.rbt.survey.data.model.LoginRequest
import com.rbt.survey.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val message: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(
    private val repository: AuthRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val response = repository.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body()?.success == true) {
                    val loginData = response.body()!!
                    preferences.saveAuthData(
                        token = loginData.token ?: loginData.accessToken ?: "",
                        refreshToken = loginData.refreshToken ?: "",
                        name = loginData.fullName ?: "",
                        email = loginData.email ?: "",
                        userId = loginData.userId?.toString() ?: ""
                    )
                    _uiState.value = LoginUiState.Success("Login Successful")
                } else {
                    _uiState.value = LoginUiState.Error(response.body()?.message ?: "Login Failed")
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.localizedMessage ?: "Network error")
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
