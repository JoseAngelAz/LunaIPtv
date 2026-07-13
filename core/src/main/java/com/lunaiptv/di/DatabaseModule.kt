package com.lunaiptv.di

import androidx.room.Room
import androidx.room.RoomDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import com.lunaiptv.core.database.LunaIPtvDatabase

/**
 * Provides the Room database (WAL journal mode for fast concurrent reads during large imports) and
 * each DAO. Foreign-key enforcement is on by default in Room.
 *
 * Destructive fallback is enabled while the schema is still evolving (pre-1.0); real migrations
 * arrive before release.
 */
val databaseModule = module {
    single {
        Room.databaseBuilder(androidContext(), LunaIPtvDatabase::class.java, LunaIPtvDatabase.NAME)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(
                LunaIPtvDatabase.MIGRATION_1_2,
                LunaIPtvDatabase.MIGRATION_2_3,
                LunaIPtvDatabase.MIGRATION_3_4,
                LunaIPtvDatabase.MIGRATION_4_6,
                LunaIPtvDatabase.MIGRATION_6_7,
                LunaIPtvDatabase.MIGRATION_7_8,
                LunaIPtvDatabase.MIGRATION_8_9,
                LunaIPtvDatabase.MIGRATION_9_10,
                LunaIPtvDatabase.MIGRATION_10_11,
                LunaIPtvDatabase.MIGRATION_11_12,
                LunaIPtvDatabase.MIGRATION_12_13,
            )
            .fallbackToDestructiveMigration(dropAllTables = true) // safety net for unforeseen jumps
            .build()
    }

    single { get<LunaIPtvDatabase>().profileDao() }
    single { get<LunaIPtvDatabase>().sourceDao() }
    single { get<LunaIPtvDatabase>().categoryDao() }
    single { get<LunaIPtvDatabase>().channelDao() }
    single { get<LunaIPtvDatabase>().movieDao() }
    single { get<LunaIPtvDatabase>().seriesDao() }
    single { get<LunaIPtvDatabase>().favoriteDao() }
    single { get<LunaIPtvDatabase>().historyDao() }
    single { get<LunaIPtvDatabase>().progressDao() }
    single { get<LunaIPtvDatabase>().contentOrderDao() }
    single { get<LunaIPtvDatabase>().tvProviderProgramDao() }
    single { get<LunaIPtvDatabase>().downloadDao() }
    single { get<LunaIPtvDatabase>().epgDao() }
    single { get<LunaIPtvDatabase>().metadataDao() }
}
