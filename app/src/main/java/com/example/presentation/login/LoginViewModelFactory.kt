package com.example.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.datasource.BiometricDataSource
import com.example.data.datasource.GoogleAuthDataSource
import com.example.domain.repository.AuthRepository
import com.example.domain.usecase.AuthenticateUserUseCase

class LoginViewModelFactory(
    private val authenticateUserUseCase: AuthenticateUserUseCase,
    private val authRepository: AuthRepository,
    private val googleAuthDataSource: GoogleAuthDataSource,
    private val biometricDataSource: BiometricDataSource
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(
                authenticateUserUseCase = authenticateUserUseCase,
                authRepository = authRepository,
                googleAuthDataSource = googleAuthDataSource,
                biometricDataSource = biometricDataSource
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
