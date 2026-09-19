package com.fastiptv.di

import com.fastiptv.data.repository.IptvRepositoryImpl
import com.fastiptv.domain.repository.IptvRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindIptvRepository(impl: IptvRepositoryImpl): IptvRepository
}
