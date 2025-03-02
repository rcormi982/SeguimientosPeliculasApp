package com.example.seguimientopeliculas.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
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

    fun updatePhotoUrl(url: String) {
        _photoUrl.value = url
    }
}