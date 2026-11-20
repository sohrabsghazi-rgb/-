package com.example.model

import androidx.compose.ui.graphics.Color

/**
 * 50 Famous Ultra-Detailed 3D Raw Alpha Effects & Studio Pro Concepts (PRO-01 to PRO-50).
 * Covers Luxury 3D Frames, Half-Screen Rising Smoke, Diamond/Bokeh/Sakura/Snow Overlays,
 * Anamorphic Cinema Flares, Hyperspace/Aurora, and Sci-Fi HUD/Tesla/Portal effects.
 */
object NeonPro50Catalog {
    private fun pro(
        num: Int,
        slug: String,
        archetype: EffectArchetype,
        title: String,
        persianTitle: String,
        pHex: Long,
        sHex: Long,
        cHex: Long,
        chHex: Long,
        summary: String,
        persianDesc: String,
        layers: List<String>,
        harmonics: Int = 2
    ): NeonAssetSpec {
        val code = "PRO-${"%02d".format(num)}"
        return NeonAssetSpec(
            key = "${code}_$slug",
            id = NeonEffectId.PRO_FAMOUS_EFFECT,
            archetype = archetype,
            code = code,
            title = title,
            persianTitle = persianTitle,
            category = NeonModelCategory.FAMOUS_50_PRO,
            primaryColor = Color(pHex),
            secondaryColor = Color(sHex),
            coreColor = Color(cHex),
            chassisTint = Color(chHex),
            animationSummary = summary,
            persianDescription = persianDesc,
            materialLayers = layers,
            loopHarmonics = harmonics
        )
    }

    val pro50Assets: List<NeonAssetSpec> = listOf(
        // 01–10: Iconic Luxury & Sci-Fi 3D Frames
        pro(
            1, "ROYAL_CARTIER_DOUBLE_GOLD", EffectArchetype.FRAME_DOUBLE_CHAMBER_LUXURY_3D,
            "Royal Cartier Double Gold 3D", "قاب دوبل طلای کارتیه سه‌بعدی",
            0xFFFFD166, 0xFFB87A14, 0xFFFFF9E6, 0xFF231807,
            "Dual-nested 24K brushed gold bevels with sweeping specular glints & transparent inner core",
            "قاب لوکس دولایه طلایی پخ‌خورده با انعکاس متالیک ۲۴ عیار و مرکز ۱۰۰٪ شفاف.",
            listOf("Outer 24K Gold Bevel", "Inner Sapphire Channel", "Counter-Rotating Glint", "Pure Alpha Core"), 2
        ),
        pro(
            2, "AEROSPACE_IRON_HUD_FRAME", EffectArchetype.FRAME_LASER_GRID_HUD_3D,
            "Aerospace Iron HUD Frame 3D", "فریم هاد هوافضای آیرون سه‌بعدی",
            0xFF00E5FF, 0xFFFF4D4D, 0xFFFFFFFF, 0xFF0B1422,
            "Tactical precision aerospace corner brackets, titanium frame & laser targeting sweeps",
            "قاب مهندسی هوافضا با براکت‌های سه‌بعدی تیتانیومی، نشانگر لیزری و جزئیات تاکتیکی.",
            listOf("Titanium Corner Brackets", "Laser Crosshair Rail", "Cyan Core Tube", "True Alpha Viewport"), 3
        ),
        pro(
            3, "ANGEL_WING_SAPPHIRE_BEVEL", EffectArchetype.FRAME_CYBER_WING_EMBLEM_3D,
            "Angel Wing Sapphire Bevel 3D", "فریم بال‌دار یاقوت کبود سه‌بعدی",
            0xFF38BDF8, 0xFF818CF8, 0xFFF0F9FF, 0xFF0A1429,
            "Sculpted aerodynamic wing accents flanking a sapphire-beveled 3D neon glass chassis",
            "قاب سه‌بعدی با بال‌های نورانی قرینه در طرفین، لبه یاقوتی و هاله کریستالی شفاف.",
            listOf("Sculpted Corner Wings", "Sapphire Glass Rail", "Feather Plasma Rim", "16K Specular Glint"), 2
        ),
        pro(
            4, "OBSIDIAN_ROSEGOLD_GLASS", EffectArchetype.FRAME_DOUBLE_CHAMBER_LUXURY_3D,
            "Obsidian Rose-Gold Glass 3D", "قاب رزگلد و شیشه دودی ابسیدین",
            0xFFFFB5A7, 0xFFC96868, 0xFFFFF5F2, 0xFF1F1215,
            "Dark smoked obsidian glass nested inside warm rose-gold beveled frames with silk shimmer",
            "ترکیب شیشه دودی ابسیدین با پخ رزگلد گرم و درخشش لوکس پیوسته.",
            listOf("Smoked Obsidian Slab", "Rose-Gold Outer Rail", "Fresnel Corner Flare", "Zero-Bg Alpha"), 2
        ),
        pro(
            5, "QUANTUM_HYPER_PRISM_FRAME", EffectArchetype.FRAME_PRISM_HOLOGRAPHIC,
            "Quantum Hyper-Prism Frame 3D", "قاب ابرمنشور کوانتومی سه‌بعدی",
            0xFF00F5D4, 0xFFFF00E5, 0xFFFFFFFF, 0xFF120B24,
            "Full-spectrum chromatic prism dispersion with crisp diamond facets around 3D frame",
            "شکست نور طیف کوانتومی روی لبه‌های تراش‌خورده سه‌بعدی با درخشش الماسی.",
            listOf("Chromatic Dispersion", "Crystal Facet Edges", "Spectral Phase Orbit", "Clear Alpha Center"), 3
        ),
        pro(
            6, "VALKYRIE_CYBER_WING_MAGENTA", EffectArchetype.FRAME_CYBER_WING_EMBLEM_3D,
            "Valkyrie Cyber Wing Magenta 3D", "فریم بال‌سایبری سرخابی سه‌بعدی",
            0xFFFF2A85, 0xFF7B2CBF, 0xFFFFECF5, 0xFF1E081A,
            "Futuristic neon magenta flared wings & cybernetic bevels with high-contrast rim bloom",
            "فریم سایبرپانک بال‌دار سرخابی-بنفش با لبه‌های متالیک سه‌بعدی و انرژی پلاسمایی.",
            listOf("Flared Cyber Wings", "Magenta Arc Emitter", "Obsidian Alloy Rib", "Volumetric Neon Bloom"), 3
        ),
        pro(
            7, "SWISS_WATCH_SAPPHIRE_BEZEL", EffectArchetype.FRAME_DOUBLE_CHAMBER_LUXURY_3D,
            "Swiss Watch Sapphire Bezel 3D", "زه کریستال ساعت سوییسی سه‌بعدی",
            0xFFE2E8F0, 0xFF0EA5E9, 0xFFFFFFFF, 0xFF0F172A,
            "Micro-milled stainless steel & sapphire bezel sheen with razor-sharp watchmaker polish",
            "دقت ساعت‌سازی سوییسی با بدنه استیل برس‌خورده، شیشه یاقوتی و بازتاب لنزی واقعی.",
            listOf("316L Brushed Steel", "Sapphire AR Coating", "Precision Bevel Step", "HDRI Specular Sweep"), 2
        ),
        pro(
            8, "LAVA_BASALT_MAGMA_FRAME", EffectArchetype.FRAME_MOLTEN_COPPER_3D,
            "Lava Basalt Magma Frame 3D", "قاب بازالت و گدازه آتشفشانی سه‌بعدی",
            0xFFFF5400, 0xFFFFBD00, 0xFFFFF3CC, 0xFF1D0904,
            "Volcanic dark basalt frame fissures glowing with flowing molten magma & rising sparks",
            "قاب سنگی بازالت با رگه‌های گداخته آتشفشانی و اخگرهای صعودی در پس‌زمینه شفاف.",
            listOf("Volcanic Basalt Rim", "Molten Magma Core", "Ember Spark Trail", "Heat Distortion Bloom"), 2
        ),
        pro(
            9, "EMERALD_GATSBY_ARTDECO", EffectArchetype.FRAME_DOUBLE_CHAMBER_LUXURY_3D,
            "Emerald Gatsby Art-Deco 3D", "قاب آرت‌دکو گتسبی زمردی سه‌بعدی",
            0xFF10B981, 0xFFFFC966, 0xFFECFDF5, 0xFF071C15,
            "Gilded emerald & antique gold twin rails inspired by classic luxury Art-Deco cinema",
            "سبک مجلل گتسبی با ترکیب خطوط دوگانه زمردی و طلایی روی شیشه سه‌بعدی شفاف.",
            listOf("Gilded Gold Trim", "Emerald Glass Core", "Art-Deco Corner Step", "Warm Studio Glow"), 2
        ),
        pro(
            10, "PLATINUM_DIAMOND_FROST_FRAME", EffectArchetype.FRAME_DIAMOND_CRYSTAL_3D,
            "Platinum Diamond Frost Frame 3D", "فریم پلاتینیوم و برفک الماس سه‌بعدی",
            0xFFE0F7FA, 0xFF94A3B8, 0xFFFFFFFF, 0xFF111827,
            "Platinum-faceted 3D frame encrusted with sparkling diamond refractions & frosted rim",
            "فریم پلاتینیوم نقره‌ای با وجوه الماس‌تراش و درخشش کریستالی سفید یخی.",
            listOf("Platinum Facets", "Diamond Cut Refraction", "Frost Rim Specular", "Zero-Bg Pure Alpha"), 3
        ),

        // 11–20: Atmospheric Smoke, Velvet Fog & Half-Screen Rising Plumes
        pro(
            11, "VELVET_INDIGO_FLOOR_SMOKE", EffectArchetype.VFX_RISING_SMOKE_HALF_FADE,
            "Velvet Indigo Floor Smoke Fade", "دود مخملی نیلی از زمین تا نیمه‌صفحه",
            0xFF818CF8, 0xFF4F46E5, 0xFFE0E7FF, 0xFF090A1F,
            "Slow-motion velvet indigo smoke curling from floor & dissolving smoothly before mid-screen",
            "دود مخملی نیلی بسیار آرام که از لبه پایین برخاسته و پیش از نیمه صفحه کاملاً محو می‌شود.",
            listOf("Bottom Volumetric Plume", "Mid-Screen Alpha Dissolve", "Indigo Silk Curl", "Raw Transparent Overlay"), 1
        ),
        pro(
            12, "GOLDEN_IMPERIAL_INCENSE", EffectArchetype.VFX_RISING_SMOKE_HALF_FADE,
            "Golden Imperial Incense Plumes", "دود طلایی عود سلطنتی با محو نرم",
            0xFFFFD166, 0xFFD97706, 0xFFFFFBEB, 0xFF1E1405,
            "Refined golden incense tendrils rising lazily from bottom border & vanishing seamlessly",
            "رشته‌های دود طلایی ابریشمی با حرکت آهسته از پایین به میانه صفحه و محوشدن کامل.",
            listOf("Golden Incense Ribbon", "Soft Mid-Screen Fade", "Warm Silk Scatter", "Clean Alpha Edges"), 1
        ),
        pro(
            13, "EMERALD_CRYO_VAPOR_FADE", EffectArchetype.VFX_RISING_SMOKE_HALF_FADE,
            "Emerald Cryo Vapor Fade", "بخار کرایو زمردی صعودکننده نیمه‌صفحه",
            0xFF34D399, 0xFF059669, 0xFFD1FAE5, 0xFF061A14,
            "Cool emerald liquid-nitrogen vapor drifting upward & fading cleanly before center",
            "بخار سرد نیتروژن زمردی که از پایین بالا آمده و پیش از نیمه صفحه محو می‌گردد.",
            listOf("Liquid Cryo Plumes", "Emerald Vapor Glow", "Half-Screen Vanish", "Pure Straight Alpha"), 1
        ),
        pro(
            14, "CRIMSON_PHOENIX_ASH_SMOKE", EffectArchetype.VFX_SMOKE_EMBERS,
            "Crimson Phoenix Ash & Smoke", "دود و خاکستر ققنوس زرشکی سه‌بعدی",
            0xFFFF3366, 0xFFFF6B35, 0xFFFFE5EC, 0xFF21070E,
            "Curling phoenix smoke layered with glowing crimson sparks & rising warm embers",
            "دود پیچ‌درپیچ ققنوس همراه با خاکسترهای نورانی سرخ و نارنجی روی آلفای خالص.",
            listOf("3D Phoenix Smoke", "Crimson Hot Embers", "Rising Ash Trails", "Transparent Overlay"), 2
        ),
        pro(
            15, "ARCTIC_DRY_ICE_FLOOR_FOG", EffectArchetype.VFX_RISING_SMOKE_HALF_FADE,
            "Arctic Dry Ice Rolling Fog", "مه سنگین یخ خشک قطبی روی زمین",
            0xFFE0F2FE, 0xFF7DD3FC, 0xFFFFFFFF, 0xFF08141E,
            "Dense low-lying dry ice fog hugging the bottom horizon with ultra-soft feather fade",
            "مه غلیظ و سرد یخ خشک در بخش پایینی کادر با محوشدن نرم به سمت مرکز تصویر.",
            listOf("Low Horizon Dry Ice", "Feathered Alpha Falloff", "Frost Mist Scatter", "Seamless 15s Drift"), 1
        ),
        pro(
            16, "BIOLUMINESCENT_DEEP_MIST", EffectArchetype.VFX_DARK_DUST_MIST,
            "Bioluminescent Deep-Sea Mist", "مه بیولومینسنت اعماق اقیانوس",
            0xFF2DD4BF, 0xFF0284C7, 0xFFCCFBF1, 0xFF04171D,
            "Weightless bioluminescent ocean mist with drifting glowing plankton motes",
            "مه درخشان اعماق اقیانوس همراه با ذرات معلق بیولومینسنت فیروزه‌ای و آبی.",
            listOf("Oceanic Mist Volume", "Glowing Plankton Dots", "Parallax Depth Layers", "Pure Alpha Channel"), 1
        ),
        pro(
            17, "ROSE_QUARTZ_DREAM_HAZE", EffectArchetype.VFX_RISING_SMOKE_HALF_FADE,
            "Rose Quartz Dream Haze Fade", "مه رویایی رزکوارتز با محوشدن نرم",
            0xFFF9A8D4, 0xFFC084FC, 0xFFFFF1F2, 0xFF1F0C19,
            "Romantic rose-quartz pastel vapor rising gently from bottom frame and fading cleanly",
            "بخار لطیف و رویایی صورتی رزکوارتز از لبه پایین تا نیمه‌صفحه با محو ابریشمی.",
            listOf("Pastel Silk Mist", "Smooth Half-Fade", "Soft Lavender Rim", "Zero-Bg Alpha"), 1
        ),
        pro(
            18, "SILVER_CATHEDRAL_FOG", EffectArchetype.VFX_RING_BOKEH_FOG,
            "Silver Cathedral Volumetric Fog", "مه نقره‌ای کلیسایی با عمق سه‌بعدی",
            0xFFE2E8F0, 0xFF64748B, 0xFFFFFFFF, 0xFF0F141C,
            "Atmospheric silver mist drifting across layered focal planes with faint optical discs",
            "مه سینمایی نقره‌ای چندلایه با عمق میدان سه‌بعدی و بوکه‌های بسیار محو.",
            listOf("Multi-Plane Fog", "Soft Optical Halo", "Neutral Silver Tint", "Cinema Alpha Output"), 1
        ),
        pro(
            19, "NEBULA_VIOLET_PLASMA_CLOUD", EffectArchetype.VFX_LIQUID_AURORA_NEBULA_3D,
            "Nebula Violet Plasma Cloud", "ابر پلاسمای سحابی بنفش سه‌بعدی",
            0xFFC084FC, 0xFF7C3AED, 0xFFF3E8FF, 0xFF140726,
            "Deep-space violet nebula clouds & luminous ionized gas ribbons undulating slowly",
            "ابرهای سحابی بنفش و نوارهای گاز یونیزه سه‌بعدی با حرکت موجی نرم.",
            listOf("Volumetric Nebula Gas", "Ionized Violet Ribbons", "Embedded Star Motes", "True Alpha Blend"), 2
        ),
        pro(
            20, "SUNSET_AMBER_SILK_VAPOR", EffectArchetype.VFX_GOLDEN_SILK,
            "Sunset Amber Silk Vapor", "بخار ابریشمی کهربایی غروب",
            0xFFFB923C, 0xFFEA580C, 0xFFFFEDD5, 0xFF221006,
            "Translucent amber silk currents flowing organically across the frame in pure alpha",
            "موج‌های ابریشمی کهربایی گرم با شفافیت بالا و ذرات نورانی غروب.",
            listOf("Amber Silk Ribbons", "Warm Sunset Sheen", "Micro-Sparkle Dust", "Seamless 15s Loop"), 2
        ),

        // 21–30: Famous Sparkles, Diamond Shards, Sakura Petals, Snow & Bokeh
        pro(
            21, "HOLLYWOOD_GOLD_GLINT", EffectArchetype.VFX_GLITTER_SHIMMER_FADE,
            "Hollywood Red-Carpet Gold Glint", "درخشش طلایی فرش قرمز هالیوود",
            0xFFFFD700, 0xFFFF8C00, 0xFFFFFFFF, 0xFF1C1403,
            "Crisp 4-point paparazzi diamond flashes & golden stardust blooming and fading gently",
            "درخشش‌های ۴پر طلایی استودیویی و فلاش‌های الماسی نرم با ظاهرشدن و محو تدریجی.",
            listOf("4-Point Star Flares", "Warm Gold Bokeh", "Smooth Dissolve", "16K Vector Glints"), 2
        ),
        pro(
            22, "SAKURA_BLOSSOM_PETALS_3D", EffectArchetype.VFX_PETALS_LEAVES_SAKURA_3D,
            "Sakura Blossom 3D Petals Fade", "گلبرگ‌های سه‌بعدی شکوفه ساکورا با محو",
            0xFFFFB7C5, 0xFFFF6B9D, 0xFFFFF0F5, 0xFF210B14,
            "3D sculpted sakura blossom petals tumbling slowly in breeze & fading with soft pink aura",
            "گلبرگ‌های سه‌بعدی شکوفه گیلاس ساکورا در باد ملایم با چرخش طبیعی و محوشدن آرام.",
            listOf("3D Sculpted Petals", "Specular Silk Rim", "Breeze Drift Path", "Pure Transparent Alpha"), 1
        ),
        pro(
            23, "CHAMPAGNE_FOIL_CONFETTI_3D", EffectArchetype.VFX_PRISM_SHARDS_CONFETTI_3D,
            "24K Champagne Foil Confetti 3D", "پولک‌ها و ورق‌های طلای ۲۴ عیار سه‌بعدی",
            0xFFFFD166, 0xFFF59E0B, 0xFFFFFFFF, 0xFF211605,
            "Tumbling 3D metallic gold foil facets catching directional studio light with sparkle flares",
            "ورقه‌های پولک طلایی ۲۴ عیار با چرخش سه‌بعدی و بازتاب نور نقطه‌ای پرجزئیات.",
            listOf("Metallic 24K Foil", "3D Faceted Tumble", "Specular Glint Catch", "Alpha-Clean Overlay"), 2
        ),
        pro(
            24, "FAIRY_FOREST_FIREFLIES", EffectArchetype.VFX_GLITTER_SHIMMER_FADE,
            "Enchanted Fairy Fireflies Loop", "کرم‌های شب‌تاب جادویی جنگل با محو نرم",
            0xFFD9F99D, 0xFF4ADE80, 0xFFFEFCE8, 0xFF0A190B,
            "Warm lime-gold fireflies weaving organic arcs with soft glowing tails & gentle fade",
            "کرم‌های شب‌تاب نورانی با مسیر پروازی نرم، درخشش گرم و محوشدن تدریجی در لوپ ۱۵ ثانیه.",
            listOf("Organic Firefly Trails", "Warm Bioluminescent Halo", "Smooth Sine Fade", "Pure Alpha Channel"), 2
        ),
        pro(
            25, "DIAMOND_CRYSTAL_SHARDS_RAIN", EffectArchetype.VFX_PRISM_SHARDS_CONFETTI_3D,
            "Diamond Crystal Shards Rain", "بارش خرده‌های کریستال و الماس شفاف",
            0xFFE0F2FE, 0xFF38BDF8, 0xFFFFFFFF, 0xFF091522,
            "Refractive 3D crystal prism shards tumbling across depth planes with rainbow edge glints",
            "خرده‌های سه‌بعدی الماس و کریستال شفاف با شکست نور و درخشش لبه‌های تراش‌خورده.",
            listOf("Crystal Facet Shards", "Refractive Rim Specular", "3D Depth Parallax", "Clean Straight Alpha"), 2
        ),
        pro(
            26, "NORDIC_SNOWFLAKE_BLIZZARD", EffectArchetype.VFX_SNOW_CRYSTAL_BLIZZARD_3D,
            "Nordic 6-Point Snowflake Blizzard", "برف کریستالی شش‌ضلعی نوردیک سه‌بعدی",
            0xFFE0F7FA, 0xFF93C5FD, 0xFFFFFFFF, 0xFF081421,
            "Multi-plane 6-branch crystalline snowflakes & soft winter depth bokeh drifting cleanly",
            "دانه‌های برف کریستالی شش‌پر واقعی در چند عمق لنز همراه با بوکه زمستانی شفاف.",
            listOf("6-Branch Crystal Vectors", "Near/Far Lens Bokeh", "Graceful Snow Drift", "True Alpha Output"), 1
        ),
        pro(
            27, "AUTUMN_MAPLE_GOLD_LEAVES", EffectArchetype.VFX_PETALS_LEAVES_SAKURA_3D,
            "Autumn Maple Gold Leaves 3D", "برگ‌های پاییزی طلایی سه‌بعدی معلق",
            0xFFF59E0B, 0xFFDC2626, 0xFFFEF3C7, 0xFF211006,
            "Warm amber & crimson autumn leaves gliding softly through golden volumetric air",
            "برگ‌های پاییزی طلایی و قرمز در حرکت آرام سه‌بعدی با رگبرگ نورانی و شفافیت کامل.",
            listOf("Curved Autumn Silhouettes", "Warm Rim Illumination", "Soft Air Turbulence", "Transparent Canvas"), 1
        ),
        pro(
            28, "MILKY_WAY_STARDUST_GLITTER", EffectArchetype.VFX_GLITTER_SHIMMER_FADE,
            "Stardust Milky-Way Glitter", "گرد ستاره کهکشان راه‌شیری با درخشش الماسی",
            0xFFE0E7FF, 0xFFA855F7, 0xFFFFFFFF, 0xFF0E081F,
            "Ultra-fine cosmic diamond dust shimmering across subtle violet-silver stardust clouds",
            "غبار ستاره‌ای بسیار ظریف با هزاران درخشش الماسی نقره‌ای و بنفش و محو تدریجی.",
            listOf("Micro Cosmic Glints", "Violet Stardust Haze", "Twinkle Phase Matrix", "Zero-Bg Raw Alpha"), 2
        ),
        pro(
            29, "RUBY_HEART_ROSE_SPARKLES", EffectArchetype.VFX_GLITTER_SHIMMER_FADE,
            "Ruby Heart & Rose Sparkles", "درخشش یاقوت سرخ و ذرات مخملی",
            0xFFFB7185, 0xFFE11D48, 0xFFFFF1F2, 0xFF240710,
            "Passionate ruby star sparkles & soft velvet rose bokeh rising gracefully in pure alpha",
            "ذرات براق یاقوتی و بوکه رمانتیک سرخ مخملی با ظاهرشدن و محو نرم ۱۵ ثانیه‌ای.",
            listOf("Ruby Star Flares", "Rose Bokeh Spheres", "Velvet Bloom Halo", "Transparent Straight Alpha"), 2
        ),
        pro(
            30, "IRIDESCENT_BUBBLE_BOKEH", EffectArchetype.VFX_RING_BOKEH_FOG,
            "Iridescent Soap-Bubble Bokeh", "حباب‌های رنگین‌کمانی سه‌بعدی و بوکه صابونی",
            0xFF67E8F9, 0xFFF472B6, 0xFFFFFFFF, 0xFF0D1420,
            "Iridescent translucent spheres with thin-film rainbow rim highlights floating weightlessly",
            "حباب‌های شفاف سه‌بعدی با بازتاب رنگین‌کمانی روی لبه‌ها و عمق میدان استودیویی.",
            listOf("Thin-Film Iridescent Rim", "Specular Catchlight", "Soft Lens Depth", "Clean Alpha Blend"), 1
        ),

        // 31–40: Cinema Lighting, Anamorphic Flares, Hyperspace & Aurora
        pro(
            31, "ANAMORPHIC_CYAN_LENS_FLARE", EffectArchetype.VFX_ANAMORPHIC_LENS_FLARE_3D,
            "Anamorphic Cyan Lens Flare", "فلر آنامورفیک سینمایی فیروزه‌ای",
            0xFF00E5FF, 0xFF2563EB, 0xFFFFFFFF, 0xFF051324,
            "Iconic Hollywood horizontal anamorphic cyan light streaks with optical ghost rings",
            "خطوط نور افقی آنامورفیک سینمای هالیوود با رنگ فیروزه‌ای و هاله‌های لنزی واقعی.",
            listOf("Horizontal Anamorphic Streak", "Optical Ghost Rings", "Diamond Aperture Core", "Pure Alpha Additive"), 2
        ),
        pro(
            32, "GOLDEN_SUNSET_GOD_RAYS", EffectArchetype.VFX_GOD_RAYS_MIST,
            "Warm Golden Sunset God-Rays", "پرتوهای طلایی خورشید هنگام غروب",
            0xFFFFBE0B, 0xFFFB5607, 0xFFFFF8DC, 0xFF211104,
            "Warm diagonal cathedral sunbeams slicing atmospheric mist with golden dust motes",
            "پرتوهای مورب و گرم آفتاب طلایی همراه با رقص ذرات معلق در مه سبک.",
            listOf("Golden Volumetric Shafts", "Sunlit Floating Motes", "Smooth Feather Alpha", "15s Gentle Drift"), 1
        ),
        pro(
            33, "HYPERSPACE_STARFIELD_WARP", EffectArchetype.VFX_STARFIELD_HYPERSPACE_3D,
            "Hyperspace 3D Starfield Warp", "پرش فضایی ستارگان سه‌بعدی هایپراسپیس",
            0xFF7DD3FC, 0xFF6366F1, 0xFFFFFFFF, 0xFF04091A,
            "High-speed 3D starlight streaks rushing radially from center warp vanishing point",
            "حرکت ستارگان سه‌بعدی از عمق فضا به سمت دوربین با دنباله نورانی پرسرعت.",
            listOf("Radial Z-Depth Streaks", "Blue-White Core Flares", "3D Perspective Tunnel", "Transparent Background"), 2
        ),
        pro(
            34, "PERSEIDS_METEOR_SHOWER", EffectArchetype.VFX_STARFIELD_HYPERSPACE_3D,
            "Perseids Diamond Meteor Shower", "بارش شهابی الماسی پرسئید سه‌بعدی",
            0xFFA5F3FC, 0xFF38BDF8, 0xFFFFFFFF, 0xFF060E1F,
            "Streaking diamond meteors & cosmic stardust arcs crossing transparent night sky",
            "شهاب‌سنگ‌های الماسی با دنباله کشیده درخشان و جرقه ذرات ستاره‌ای.",
            listOf("Meteor Ion Trails", "Sparkling Head Glint", "Multi-Depth Starfield", "Clean Alpha Vector"), 2
        ),
        pro(
            35, "CATHEDRAL_SUNBEAM_DUST", EffectArchetype.VFX_GOD_RAYS_MIST,
            "Cathedral Sunbeam & Dust Dance", "رقص ذرات طلایی در پرتو آفتاب",
            0xFFFDE68A, 0xFF93C5FD, 0xFFFFFFFF, 0xFF111520,
            "Soft silver-gold volumetric light shafts illuminating slow dancing airborne specks",
            "ستون‌های نور ملایم همراه با رقص بسیار آرام ذرات درخشان در مسیر پرتوها.",
            listOf("Soft Mist Beams", "Illuminated Micro-Specks", "Balanced Cinema Contrast", "Raw Alpha Channel"), 1
        ),
        pro(
            36, "ANAMORPHIC_AMBER_FLARE", EffectArchetype.VFX_ANAMORPHIC_LENS_FLARE_3D,
            "Anamorphic Amber Director Flare", "فلر لنز کارگردانی کهربایی گرم",
            0xFFFF9F1C, 0xFFE71D36, 0xFFFFFAF0, 0xFF220E05,
            "Rich amber-crimson anamorphic cinema streaks & warm optical bokeh ghosts",
            "فلر آنامورفیک گرم کهربایی-یاقوتی مخصوص تیتراژ و ویدیوهای سینمایی لوکس.",
            listOf("Amber Horizontal Flare", "Optical Lens Polygons", "Warm Core Bloom", "Transparent Alpha"), 2
        ),
        pro(
            37, "AURORA_BOREALIS_CURTAINS", EffectArchetype.VFX_LIQUID_AURORA_NEBULA_3D,
            "Aurora Borealis Green Curtains", "پرده‌های زنده شفق قطبی سبز-فیروزه‌ای",
            0xFF00F5A0, 0xFF00D9F5, 0xFFE6FFF9, 0xFF041718,
            "Undulating volumetric emerald-cyan aurora curtains shimmering in 3D atmospheric depth",
            "پرده‌های زنده و موج‌دار شفق قطبی با طیف سبز یشمی و فیروزه‌ای در عمق سه‌بعدی.",
            listOf("Aurora Vertical Curtains", "Emerald-Cyan Shimmer", "Subtle Star Dust", "Pure Transparent Alpha"), 2
        ),
        pro(
            38, "DEEP_SPACE_MAGENTA_NEBULA", EffectArchetype.VFX_LIQUID_AURORA_NEBULA_3D,
            "Deep Space Magenta Nebula", "سحابی فضایی سرخابی-بنفش سه‌بعدی",
            0xFFF43F5E, 0xFF8B5CF6, 0xFFFFE4E6, 0xFF1A0716,
            "Luminous cosmic nebula wisps blending rose magenta & ultraviolet gas clouds",
            "سحابی درخشان کیهانی با ترکیب ابرهای سرخابی و بنفش فرابنفش در فضای باز شفاف.",
            listOf("Cosmic Gas Volumes", "Magenta Core Glow", "Stardust Highlights", "Seamless 15s Drift"), 2
        ),
        pro(
            39, "LUNAR_SILVER_ECLIPSE_HALO", EffectArchetype.VFX_CYBER_ORBITAL_ARCS,
            "Lunar Silver Eclipse Halo", "هاله نقره‌ای خورشیدگرفتگی و ماه کامل",
            0xFFE2E8F0, 0xFF38BDF8, 0xFFFFFFFF, 0xFF0A101B,
            "Luminous coronal silver eclipse arcs & diamond-ring solar flares rotating smoothly",
            "هاله تاج نقره‌ای کسوف و قوس‌های درخشان الماسی دور مرکز کاملاً شفاف.",
            listOf("Coronal Corona Arcs", "Diamond Ring Glint", "Silver Mist Scatter", "Zero-Bg Pure Alpha"), 2
        ),
        pro(
            40, "UNDERWATER_CAUSTIC_RIPPLES", EffectArchetype.VFX_CAUSTIC_WATER_RIPPLES_3D,
            "Underwater Caustic Sun Ripples", "امواج شکست نور زیر آب سه‌بعدی",
            0xFF22D3EE, 0xFF0284C7, 0xFFECFEFF, 0xFF041824,
            "Refractive turquoise water caustic rings & ascending pearl air bubbles in 3D depth",
            "حلقه‌های شکست نور زیر آب فیروزه‌ای همراه با حباب‌های صعودی شفاف.",
            listOf("Caustic Wave Rings", "Ascending Pearl Bubbles", "Aqua Light Refraction", "16K Vector Ripples"), 2
        ),

        // 41–50: Sci-Fi HUD, Tesla Lightning, Cyber Rain, Portals & Magic
        pro(
            41, "ZEUS_BLUE_TESLA_LIGHTNING", EffectArchetype.VFX_LIGHTNING_TESLA_WEB_3D,
            "Zeus Blue Tesla Lightning Web", "شبکه رعدوبرق تسلا آبی زئوس سه‌بعدی",
            0xFF38BDF8, 0xFF3B82F6, 0xFFFFFFFF, 0xFF061126,
            "Forking high-voltage blue Tesla lightning bolts & ionized plasma nodes with crisp core",
            "شاخه‌های پرانرژی آذرخش تسلا آبی الکتریک با گره‌های پلاسمایی و درخشش شفاف.",
            listOf("Branched Tesla Forks", "White-Hot Ion Core", "Electric Bloom Halo", "Zero-Bg Alpha"), 3
        ),
        pro(
            42, "JARVIS_TACTICAL_HUD_RETICLE", EffectArchetype.VFX_SACRED_HUD_GEOMETRY_3D,
            "Jarvis Tactical HUD Reticle 3D", "رادار تاکتیکی و هاد جارویس سه‌بعدی",
            0xFF00E5FF, 0xFF0EA5E9, 0xFFE0FFFF, 0xFF051422,
            "Multi-ring counter-rotating holographic HUD dials, degree ticks & targeting arcs",
            "دایره‌ها و رینگ‌های چرخان هولوگرافیک هاد تاکتیکی با خط‌کش زاویه و قوس‌های دقیق.",
            listOf("Counter-Rotating Dials", "Degree Calibration Ticks", "Holographic Scan Arcs", "16K Razor Lines"), 2
        ),
        pro(
            43, "EMERALD_MATRIX_CYBER_RAIN", EffectArchetype.VFX_DIGITAL_MATRIX_RUNES_3D,
            "Emerald Matrix Cyber Rain 3D", "باران کد سایبری ماتریکس سه‌بعدی",
            0xFF00E676, 0xFF059669, 0xFFDCFFE4, 0xFF04170C,
            "Multi-depth vertical cyber data streams & luminous emerald glyph nodes falling smoothly",
            "ستون‌های سه‌بعدی باران دیتا و سیگنال‌های سبز ماتریکس با عمق و محوشدن نرم.",
            listOf("3D Depth Data Columns", "Glowing Head Nodes", "Parallax Speed Layers", "Pure Alpha Blend"), 2
        ),
        pro(
            44, "THOR_VIOLET_PLASMA_THUNDER", EffectArchetype.VFX_LIGHTNING_TESLA_WEB_3D,
            "Violet Thor Plasma Thunder", "آذرخش پلاسمایی بنفش با شاخه‌های سه‌بعدی",
            0xFFD946EF, 0xFF7C3AED, 0xFFFFFFFF, 0xFF170726,
            "Crackling ultraviolet thunder forks pulsing cleanly across transparent dark-energy arcs",
            "رعدوبرق پلاسمایی بنفش-ارغوانی با شاخه‌های شکسته دقیق و هاله پرقدرت فرابنفش.",
            listOf("UV Plasma Branches", "Specular Bolt Core", "Violet Arc Scatter", "Transparent Alpha"), 3
        ),
        pro(
            45, "QUANTUM_SONAR_SHOCKWAVE", EffectArchetype.VFX_CAUSTIC_WATER_RIPPLES_3D,
            "Quantum Sonar Shockwave Rings", "حلقه‌های موج انفجار و سونار کوانتومی",
            0xFF00F5D4, 0xFF6366F1, 0xFFFFFFFF, 0xFF080D20,
            "Expanding concentric sonic shockwave rings & micro distortion particles radiating cleanly",
            "امواج شوک و حلقه‌های سونار کوانتومی در حال گسترش از مرکز با درخشش سه‌بعدی.",
            listOf("Expanding Shock Rings", "Chromatic Rim Ripple", "Acoustic Glow Motes", "Seamless 15s Wave"), 3
        ),
        pro(
            46, "MYSTIC_ORANGE_MANDALA_3D", EffectArchetype.VFX_SACRED_HUD_GEOMETRY_3D,
            "Mystic Gold Mandala Magic 3D", "مندالای جادویی پرتقالی و قوس نور سه‌بعدی",
            0xFFFF7B00, 0xFFFFCA3A, 0xFFFFF8E6, 0xFF241003,
            "Rotating arcane geometric rings, rune polygons & ember sparks inspired by mystic portals",
            "دایره‌ها و هندسه اسرارآمیز پرتقالی-طلایی در حال چرخش سه‌بعدی همراه با جرقه نور.",
            listOf("Arcane Rotating Rings", "Sacred Square Geometry", "Golden Rune Sparks", "True Alpha Canvas"), 2
        ),
        pro(
            47, "CHRONOS_GOLD_CLOCKWORK_HUD", EffectArchetype.VFX_SACRED_HUD_GEOMETRY_3D,
            "Chronos Gold Clockwork HUD 3D", "چرخ‌دنده‌های زمانی و هاد طلایی کرونوس",
            0xFFFFC966, 0xFFD4AF37, 0xFFFFFFFF, 0xFF1D1506,
            "Luxury gold chronometer rings, compass ticks & sweeping orbital sweepers in crisp 16K",
            "رینگ‌های زمانی طلایی لوکس با تقسیم‌بندی دقیق کرونومتر و حرکت مکانیکی سه‌بعدی.",
            listOf("Chronometer Tick Ring", "Brushed Gold Sweep", "Precision Orbit Nodes", "16K Crisp Vectors"), 2
        ),
        pro(
            48, "HYPERSONIC_PLASMA_SHOCK", EffectArchetype.VFX_ANAMORPHIC_LENS_FLARE_3D,
            "Hyper-Sonic Plasma Shock Cone", "موج شوک مافوق صوت فیروزه‌ای سه‌بعدی",
            0xFF22D3EE, 0xFF818CF8, 0xFFFFFFFF, 0xFF071326,
            "Intense cyan-violet optical flash bursts & horizontal sonic light blades in pure alpha",
            "انفجار نوری مافوق صوت فیروزه‌ای-بنفش با تیغه‌های افقی نور و حلقه‌های پلاسمایی.",
            listOf("Sonic Light Blades", "Radial Plasma Burst", "Optical Ghost Rings", "Transparent Alpha"), 3
        ),
        pro(
            49, "CYBERPUNK_DATA_GRID_WARP", EffectArchetype.VFX_DIGITAL_MATRIX_RUNES_3D,
            "Cyberpunk Data Stream Warp", "جریان داده سایبرپانک سرخابی-آبی",
            0xFFFF2A85, 0xFF00E5FF, 0xFFFFFFFF, 0xFF130820,
            "Dual-tone neon magenta & cyan cyber telemetry streaks racing vertically in depth",
            "ستون‌های دیتا و سیگنال‌های سایبرپانک دو رنگ سرخابی و فیروزه‌ای با عمق میدان سه‌بعدی.",
            listOf("Dual-Tone Data Streams", "Magenta/Cyan Telemetry", "Soft Edge Alpha Fade", "15s Seamless Loop"), 2
        ),
        pro(
            50, "INFINITY_COSMIC_PORTAL_AURA", EffectArchetype.VFX_CYBER_ORBITAL_ARCS,
            "Infinity Cosmic Portal Aura", "پورتال کهکشانی بی‌نهایت با قوس‌های سه‌بعدی",
            0xFF00F5D4, 0xFFFF4ECD, 0xFFFFFFFF, 0xFF120822,
            "3D interlocking orbital portal rings, chromatic plasma arcs & stardust vortex halo",
            "پورتال فضایی سه بعدی با قوس‌های مداری درهم‌تنیده و هاله رنگین‌کمانی شفاف.",
            listOf("Interlocking 3D Arcs", "Chromatic Portal Core", "Orbiting Stardust", "Pure Transparent Alpha"), 3
        )
    )
}
