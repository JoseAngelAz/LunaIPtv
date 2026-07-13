package com.lunaiptv.core.database

import androidx.room.TypeConverter
import com.lunaiptv.core.model.DownloadStatus
import com.lunaiptv.core.model.MediaType
import com.lunaiptv.core.model.SourceType
import com.lunaiptv.core.tv.TvProviderSurface

/** Stores the app's enums as their stable names (survives reordering). */
class Converters {
    @TypeConverter fun mediaTypeToString(v: MediaType): String = v.name
    @TypeConverter fun stringToMediaType(v: String): MediaType = MediaType.valueOf(v)

    @TypeConverter fun sourceTypeToString(v: SourceType): String = v.name
    @TypeConverter fun stringToSourceType(v: String): SourceType = SourceType.valueOf(v)

    @TypeConverter fun downloadStatusToString(v: DownloadStatus): String = v.name
    @TypeConverter fun stringToDownloadStatus(v: String): DownloadStatus = DownloadStatus.valueOf(v)

    @TypeConverter fun tvProviderSurfaceToString(v: TvProviderSurface): String = v.name
    @TypeConverter fun stringToTvProviderSurface(v: String): TvProviderSurface = TvProviderSurface.valueOf(v)
}
