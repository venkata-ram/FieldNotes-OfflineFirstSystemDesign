package com.venkatsvision.offlinefirstsystemdesign.di

import com.venkatsvision.offlinefirstsystemdesign.data.connectivity.AndroidConnectivityObserver
import com.venkatsvision.offlinefirstsystemdesign.data.connectivity.ConnectivityObserver
import com.venkatsvision.offlinefirstsystemdesign.data.sync.NotesSyncScheduler
import com.venkatsvision.offlinefirstsystemdesign.data.sync.SyncScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {
    @Binds
    @Singleton
    abstract fun bindConnectivityObserver(observer: AndroidConnectivityObserver): ConnectivityObserver

    @Binds
    @Singleton
    abstract fun bindSyncScheduler(scheduler: NotesSyncScheduler): SyncScheduler
}
