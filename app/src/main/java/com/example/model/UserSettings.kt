package com.example.model

import androidx.compose.runtime.Immutable

enum class SliderPosition {
    RIGHT,
    LEFT
}

enum class DarkModeOption {
    SYSTEM,
    DARK,
    LIGHT
}

enum class SortOption(val displayName: String) {
    TITLE_ASC("Title (A to Z)"),
    TITLE_DESC("Title (Z to A)"),
    REVERSE("Reverse Order")
}

enum class MusicVisualTheme(val id: String, val displayNameEn: String, val displayNameAr: String, val descriptionEn: String, val descriptionAr: String) {
    VINYL(
        id = "vinyl",
        displayNameEn = "Classic Vinyl",
        displayNameAr = "أسطوانة الفينيل",
        descriptionEn = "Classic rotating vinyl turntable with grooved disc & needle tonearm",
        descriptionAr = "أسطوانة فينيل كلاسيكية دوارة مع نغمة الإبرة والأخاديد الصوتية"
    ),
    CYBER_PULSE(
        id = "cyber_pulse",
        displayNameEn = "Cyber Pulse Orb",
        displayNameAr = "هولوجرام نبض النيون",
        descriptionEn = "Futuristic cyber plasma core with glowing neon energy rings & orbital sound nodes",
        descriptionAr = "كرة بلازما هولوجرافية مستقبلية مع حلقات طاقة نيون ومدارات صوتية مشعة"
    ),
    SONIC_RADAR(
        id = "sonic_radar",
        displayNameEn = "Sonic Radar & Oscilloscope",
        displayNameAr = "رادار الأمواج الصوتية",
        descriptionEn = "Audiophile acoustic radar with scanning laser beam & oscillating audio waveforms",
        descriptionAr = "مذبذب ورادار صوتي احترافي مع شعاع مسح دقيق وموجات ترددية حية"
    )
}

enum class AccentColorOption(val displayName: String, val hexCode: Long, val category: String) {
    // 🌿 Herbs
    SAGE("Sage", 0xFFB3C1A8, "Herbs"),
    ROSEMARY("Rosemary", 0xFF52584D, "Herbs"),
    BASIL("Basil", 0xFF679E29, "Herbs"),
    DRIED_MINT("Dried Mint", 0xFF82AC7C, "Herbs"),
    WILD_THYME("Wild Thyme", 0xFF7C7E6C, "Herbs"),

    // 🌶️ Spices
    PAPRIKA("Paprika", 0xFF933617, "Spices"),
    TURMERIC("Turmeric", 0xFFDAAD12, "Spices"),
    CINNAMON("Cinnamon", 0xFF9A3A0A, "Spices"),
    CUMIN("Cumin", 0xFFB77D11, "Spices"),
    BURNT_CHILI("Burnt Chili", 0xFF711015, "Spices"),

    // 🏜️ Roots & Earth
    DESERT_SAND("Desert Sand", 0xFFDDBEBE, "Roots & Earth"),
    LIMESTONE("Limestone", 0xFFE1DAC8, "Roots & Earth"),
    CLAY("Clay", 0xFFCB997E, "Roots & Earth"),
    LIGHT_TRUFFLE("Light Truffle", 0xFFB2A68D, "Roots & Earth"),
    MUTED_WOOD("Muted Wood", 0xFF6F4E37, "Roots & Earth"),

    // 🌑 Deep Shades
    BLACK_PEPPER("Black Pepper", 0xFF311815, "Deep Shades"),
    DEEP_OLIVE("Deep Olive", 0xFF4A4C3A, "Deep Shades"),
    RAW_COCOA("Raw Cocoa", 0xFF563D39, "Deep Shades"),
    DRIED_LAVENDER("Dried Lavender", 0xFF7297A0, "Deep Shades"),
    DARK_SAFFRON("Dark Saffron", 0xFFB99F4B, "Deep Shades"),

    // 🎨 Vibrant
    PURPLE("Purple", 0xFFD0BCFF, "Vibrant"),
    CYAN("Cyan", 0xFF80DEEA, "Vibrant"),
    GREEN("Emerald", 0xFFA5D6A7, "Vibrant"),
    ORANGE("Amber", 0xFFFFCC80, "Vibrant"),
    ROSE("Rose", 0xFFF48FB1, "Vibrant"),
    BLUE("Blue", 0xFF90CAF9, "Vibrant"),
    RED("Red", 0xFFFFB4AB, "Vibrant")
}

@Immutable
data class UserSettings(
    val sliderPosition: SliderPosition = SliderPosition.RIGHT,
    val darkModeOption: DarkModeOption = DarkModeOption.DARK,
    val accentColorOption: AccentColorOption = AccentColorOption.PURPLE,
    val isBatteryOptimizationDisabled: Boolean = false,
    val customFolderUris: List<String> = emptyList(),
    val volumeSliderLength: Float = 0.8f,
    val coloredSpeedSlider: Boolean = false,
    val progressBarLength: Float = 1.0f,
    val speedSliderLength: Float = 1.0f,
    val volumeBarYOffsetDp: Float = 0f,
    val volumeBarXOffsetDp: Float = 0f,
    val volumeBarThicknessDp: Float = 6f,
    val vinylYOffsetDp: Float = 0f,
    val bottomControlsYOffsetDp: Float = 0f,
    val hideHeaderOnScroll: Boolean = true,
    val autoScanDevice: Boolean = false,
    val musicVisualTheme: MusicVisualTheme = MusicVisualTheme.VINYL
)
