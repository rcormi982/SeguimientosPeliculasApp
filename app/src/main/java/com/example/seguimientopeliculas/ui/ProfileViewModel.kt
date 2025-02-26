package com.example.seguimientopeliculas.ui

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seguimientopeliculas.data.remote.UserRemoteDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource
) : ViewModel() {

    private val _photo = MutableStateFlow<Uri>(Uri.EMPTY)
    val photo: StateFlow<Uri>
        get() = _photo.asStateFlow()

    private val _photoUrl = MutableStateFlow<String?>(null)
    val photoUrl: StateFlow<String?>
        get() = _photoUrl.asStateFlow()

    fun onImageCaptured(uri: Uri?) {
        viewModelScope.launch {
            uri?.let {
                _photo.value = uri
            }
        }
    }

    suspend fun uploadPhoto(photoUri: Uri, moviesUserId: Int): Boolean {
        return try {
            val uploadResult = userRemoteDataSource.uploadProfilePhoto(photoUri, moviesUserId)
            if (uploadResult != null) {
                _photoUrl.value = uploadResult.url  // Ahora usamos .url del PhotoUploadResult
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun updatePhotoUrl(url: String) {
        _photoUrl.value = url
    }
}