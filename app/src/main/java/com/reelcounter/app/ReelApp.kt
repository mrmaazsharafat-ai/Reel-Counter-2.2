package com.reelcounter.app

/**
 * Wo apps jin mein reels ginni hain.
 *
 * reelIds: us app ki screen mein kisi view ke id mein ye lafz hon to samjha jata hai
 * ke abhi reels/shorts wali screen khuli hai. null ka matlab poori app reels wali hai (TikTok).
 * Agar kisi app mein ginti galat aaye to yahan ke lafz badal kar theek kiye ja sakte hain.
 */
enum class ReelApp(
    val key: String,
    val label: String,
    val packages: List<String>,
    val reelIds: List<String>?
) {
    TIKTOK(
        "tiktok", "TikTok",
        listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.zhiliaoapp.musically.go"),
        null
    ),
    INSTAGRAM(
        "instagram", "Instagram",
        listOf("com.instagram.android", "com.instagram.lite"),
        listOf("clips_viewer", "clips_video", "clips_swipe")
    ),
    FACEBOOK(
        "facebook", "Facebook",
        listOf("com.facebook.katana", "com.facebook.lite"),
        listOf("reels_viewer", "reel_viewer", "reels_video")
    ),
    YOUTUBE(
        "youtube", "YouTube Shorts",
        listOf("com.google.android.youtube"),
        listOf("reel_player", "reel_recycler", "reel_watch", "shorts_player")
    );

    companion object {
        fun fromPackage(pkg: String): ReelApp? = values().firstOrNull { pkg in it.packages }
        fun fromKey(key: String): ReelApp? = values().firstOrNull { it.key == key }
    }
}
