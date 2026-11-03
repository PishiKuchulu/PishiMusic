package com.tuned.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Material's shape roles mapped onto [Radius]. */
val TunedShapes = Shapes(
    extraSmall = AppShape.xs,
    small = AppShape.sm,
    medium = AppShape.md,
    large = AppShape.lg,
    extraLarge = AppShape.lg,
)

/** Pop-up menus (⋮): rounded like the app's cards, not Material's 4dp default. */
val MenuShape = RoundedCornerShape(20.dp)

// Shapes named by the original app. They now point at the Tuned radius scale
// so existing screens pick it up; new code uses AppShape directly.
val ExpressiveHeroShape = AppShape.lg
val ExpressivePillShape = AppShape.full
val HeroInnerShape = AppShape.md
val StatPillShape = AppShape.md
val ListContainerShape = AppShape.lg
val BadgePillShape = AppShape.full
val NowPlayingCardShape = AppShape.md
val TrackRowShape = AppShape.sm
/** Row covers: 10dp, the same as Library's and Downloads' rows (4dp read as square). */
val ArtworkShape = RoundedCornerShape(10.dp)

/** The original 10-step corner scale, collapsed onto [Radius]. */
object M3ExpressiveShape {
    val None = RoundedCornerShape(Radius.xs * 0)
    val ExtraSmall = AppShape.xs
    val Small = AppShape.sm
    val Medium = AppShape.md
    val Large = AppShape.lg
    val LargeIncreased = AppShape.lg
    val ExtraLarge = AppShape.lg
    val ExtraLargeIncreased = AppShape.lg
    val ExtraExtraLarge = AppShape.lg
    val Full = AppShape.full

    // Connected button groups: outer corners rounded, inner corners tight.
    val GroupLeft = RoundedCornerShape(topStart = Radius.md, bottomStart = Radius.md, topEnd = Radius.xs, bottomEnd = Radius.xs)
    val GroupMiddle = AppShape.xs
    val GroupRight = RoundedCornerShape(topStart = Radius.xs, bottomStart = Radius.xs, topEnd = Radius.md, bottomEnd = Radius.md)
    val GroupSingle = AppShape.md
}
