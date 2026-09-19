package com.fastiptv.di

import android.content.Context
import androidx.room.Room
import com.fastiptv.data.db.AppDatabase
import com.fastiptv.data.db.dao.CategoryDao
import com.fastiptv.data.db.dao.ChannelDao
import com.fastiptv.data.db.dao.FavoriteDao
import com.fastiptv.data.db.dao.RecentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "fastiptv.db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
        }

    @Provides
    fun provideChannelDao(database: AppDatabase): ChannelDao = database.channelDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun provideRecentDao(database: AppDatabase): RecentDao = database.recentDao()

    @Provides
    fun provideEpgDao(database: AppDatabase): com.fastiptv.data.db.dao.EpgDao = database.epgDao()

    @Provides
    fun provideVodDao(database: AppDatabase): com.fastiptv.data.db.dao.VodDao = database.vodDao()

    @Provides
    fun provideSeriesDao(database: AppDatabase): com.fastiptv.data.db.dao.SeriesDao = database.seriesDao()
}
