package com.fastiptv.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fastiptv.data.db.dao.CategoryDao
import com.fastiptv.data.db.dao.ChannelDao
import com.fastiptv.data.db.dao.EpgDao
import com.fastiptv.data.db.dao.FavoriteDao
import com.fastiptv.data.db.dao.RecentDao
import com.fastiptv.data.db.entity.CategoryEntity
import com.fastiptv.data.db.entity.ChannelEntity
import com.fastiptv.data.db.entity.ChannelFtsEntity
import com.fastiptv.data.db.entity.EpgProgramEntity
import com.fastiptv.data.db.entity.FavoriteEntity
import com.fastiptv.data.db.entity.RecentEntity
import com.fastiptv.data.db.entity.SeriesEntity
import com.fastiptv.data.db.entity.VodEntity
import com.fastiptv.data.db.entity.VodFtsEntity

@Database(
    entities = [
        CategoryEntity::class,
        ChannelEntity::class,
        ChannelFtsEntity::class,
        FavoriteEntity::class,
        RecentEntity::class,
        VodEntity::class,
        VodFtsEntity::class,
        SeriesEntity::class,
        EpgProgramEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun categoryDao(): CategoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentDao(): RecentDao
    abstract fun epgDao(): EpgDao
    abstract fun vodDao(): com.fastiptv.data.db.dao.VodDao
    abstract fun seriesDao(): com.fastiptv.data.db.dao.SeriesDao
}
