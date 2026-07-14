package com.lunaiptv.phone.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.lunaiptv.core.database.dao.ProfileDao
import com.lunaiptv.core.database.entity.ProfileEntity
import com.lunaiptv.features.settings.data.SettingsRepository

class PhoneProfileViewModel(
    private val profileDao: ProfileDao,
    private val settings: SettingsRepository,
) : ViewModel() {

    val profiles: StateFlow<List<ProfileEntity>> = profileDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfileId: StateFlow<Long> = settings.activeProfileId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1L)

    fun switchProfile(id: Long) {
        viewModelScope.launch {
            settings.setActiveProfile(id)
        }
    }

    fun createProfile(name: String) {
        viewModelScope.launch {
            val colors = intArrayOf(
                0xFF2196F3.toInt(), 0xFFE91E63.toInt(), 0xFF4CAF50.toInt(),
                0xFFFF9800.toInt(), 0xFF9C27B0.toInt(), 0xFF00BCD4.toInt(),
                0xFFD32F2F.toInt(), 0xFF3F51B5.toInt(),
            )
            val count = profileDao.count()
            val entity = ProfileEntity(
                name = name,
                avatarColor = colors[count % colors.size],
                avatarId = count % 12,
            )
            val newId = profileDao.insert(entity)
            settings.setActiveProfile(newId)
        }
    }

    fun deleteProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            val current = activeProfileId.value
            profileDao.delete(profile)
            if (current == profile.id) {
                val remaining = profileDao.getAllOnce()
                if (remaining.isNotEmpty()) {
                    settings.setActiveProfile(remaining.first().id)
                }
            }
        }
    }
}
