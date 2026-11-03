package com.tuned.app.widget

/** The four widgets in the picker, each a fixed size. Medium is [NowPlayingWidgetReceiver]. */
enum class WidgetStyle { FULL_COVER, SMALL, MEDIUM, WIDE }

/** 2x2: the cover edge to edge, title and artist on a soft fade at the bottom. */
class FullCoverWidgetReceiver : NowPlayingWidgetReceiver() {
    override val style get() = WidgetStyle.FULL_COVER
}

/** 2x2: cover top-left and the flower top-right on a card tinted from the cover. */
class SmallWidgetReceiver : NowPlayingWidgetReceiver() {
    override val style get() = WidgetStyle.SMALL
}

/** 4x2: the cover across the left half, title, artist, album and controls on the right. */
class WideWidgetReceiver : NowPlayingWidgetReceiver() {
    override val style get() = WidgetStyle.WIDE
}

/** Every provider and the style it draws, for pushing updates to all placed widgets. */
internal val widgetProviders: List<Pair<Class<out NowPlayingWidgetReceiver>, WidgetStyle>> = listOf(
    FullCoverWidgetReceiver::class.java to WidgetStyle.FULL_COVER,
    SmallWidgetReceiver::class.java to WidgetStyle.SMALL,
    NowPlayingWidgetReceiver::class.java to WidgetStyle.MEDIUM,
    WideWidgetReceiver::class.java to WidgetStyle.WIDE,
)
