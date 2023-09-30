package com.linkstar.visiongrader.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Patterns
import com.linkstar.visiongrader.data.LoginRepository
import com.linkstar.visiongrader.data.Result

import com.linkstar.visiongrader.R
import com.linkstar.visiongrader.ui.login.LoggedInUserView
import com.linkstar.visiongrader.ui.login.LoginFormState
import com.linkstar.visiongrader.ui.login.LoginResult
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LoginViewModel(private val loginRepository: LoginRepository) : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    init {

    }

    @OptIn(DelicateCoroutinesApi::class)
    fun login(username: String, password: String) {
        // can be launched in a separate asynchronous job

        GlobalScope.launch {
            val result = loginRepository.login(username, password)

            Log.d("LoginVM", result.toString())

            if (result is Result.Success) {
                _loginResult.postValue(LoginResult(success = LoggedInUserView(displayName = result.data.name)))
            } else {
                _loginResult.postValue(LoginResult(error = R.string.login_failed))
            }
        }

    }

    fun loginDataChanged(username: String, password: String) {
        if (!isUserNameValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    // A placeholder username validation check
    private fun isUserNameValid(username: String): Boolean {
        return if (username.contains("@")) {
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            username.isNotBlank()
        }
    }

    // A placeholder password validation check
    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }
}