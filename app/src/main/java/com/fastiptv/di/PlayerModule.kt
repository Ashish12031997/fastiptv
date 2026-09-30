package com.fastiptv.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.fastiptv.data.api.AuthInterceptor
import com.fastiptv.player.StreamErrorHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun provideLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 4_000,
                /* maxBufferMs = */ 15_000,
                /* bufferForPlaybackMs = */ 800,
                /* bufferForPlaybackAfterRebufferMs = */ 1_500
            )
            .setTargetBufferBytes(8 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(/* backBufferDurationMs = */ 0, /* retainBackBufferFromKeyframe = */ false)
            .build()
    }

    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
        loadControl: LoadControl,
        errorHandler: StreamErrorHandler
    ): ExoPlayer {
        val streamOkHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectionPool(okhttp3.ConnectionPool(8, 2, java.util.concurrent.TimeUnit.MINUTES))
            .connectTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(18, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", AuthInterceptor.USER_AGENT)
                    .header("Accept", "*/*")
                    .header("Connection", "keep-alive")
                    .build()
                chain.proceed(request)
            }
            .build()

        val httpDataSourceFactory = OkHttpDataSource.Factory(streamOkHttpClient)
            .setUserAgent(AuthInterceptor.USER_AGENT)

        val tsFlags = androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS or
                androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS or
                androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_IGNORE_SPLICE_INFO_STREAM

        val extractorsFactory = androidx.media3.extractor.DefaultExtractorsFactory()
            .setTsExtractorFlags(tsFlags)
            .setTsExtractorTimestampSearchBytes(2000 * androidx.media3.extractor.ts.TsExtractor.TS_PACKET_SIZE)
            .setConstantBitrateSeekingEnabled(true)

        val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory, extractorsFactory)

        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
            setAllowedVideoJoiningTimeMs(5000L)
            setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        }

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        return ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setSeekForwardIncrementMs(10_000)
            .setSeekBackIncrementMs(10_000)
            .build()
            .also { player ->
                player.setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
                player.addListener(errorHandler)
            }
    }

    @Provides
    @Singleton
    fun provideStreamPlayerController(streamPlayer: com.fastiptv.player.StreamPlayer): com.fastiptv.player.StreamPlayerController {
        return streamPlayer
    }
}
