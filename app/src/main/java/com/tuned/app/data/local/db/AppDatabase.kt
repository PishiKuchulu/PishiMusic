package com.tuned.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tuned.app.data.podcast.PodcastProgressDao
import com.tuned.app.data.podcast.PodcastProgressEntity

@Database(
    entities = [
        ArtworkCacheEntity::class,
        RecommendationExclusionEntity::class,
        SavedPlaylistEntity::class,
        DownloadedTrackEntity::class,
        SongPlayStatsEntity::class,
        RelatedGraphEdgeEntity::class,
        PodcastProgressEntity::class,
        YearlyListenEntity::class,
    ],
    version = 17,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun artworkCacheDao(): ArtworkCacheDao
    abstract fun recommendationExclusionDao(): RecommendationExclusionDao
    abstract fun savedPlaylistDao(): SavedPlaylistDao
    abstract fun downloadedTrackDao(): DownloadedTrackDao
    abstract fun songPlayStatsDao(): SongPlayStatsDao
    abstract fun relatedGraphDao(): RelatedGraphDao
    abstract fun podcastProgressDao(): PodcastProgressDao
    abstract fun yearlyListenDao(): YearlyListenDao
}
