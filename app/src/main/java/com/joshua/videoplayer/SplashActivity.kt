package com.joshua.videoplayer

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.PathParser
import androidx.core.view.WindowCompat
import com.joshua.videoplayer.data.LanguageManager
import com.joshua.videoplayer.ui.theme.VideoPlayerTheme
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/**
 * 启动页：虚线爱心 + 十字架；右下三条波纹循环高亮（1→2→3）模拟外扩；倒计时或跳过进入主页。
 * 界面仅负责展示；在 [Lifecycle.State.STARTED] 之后于后台调用 [VideoPlayerApp.ensureLibraryWarmPrefetch]（已授权则预热本地库，未授权则静默清理缓存），权限弹窗与说明页只在 [MainActivity] 展示。
 */
class SplashActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val language = LanguageManager.getCurrentLanguage()
        val locale = Locale.forLanguageTag(language.localeTag)
        val config = Configuration(newBase.resources.configuration)
        config.setLocales(LocaleList(locale))
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        val navigated = AtomicBoolean(false)
        fun goMain() {
            if (navigated.compareAndSet(false, true)) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
        setContent {
            VideoPlayerTheme {
                val readVideoPermission = remember {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        android.Manifest.permission.READ_MEDIA_VIDEO
                    } else {
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                }
                val lifecycleOwner = LocalLifecycleOwner.current
                // 启动页仅展示；权限与本地库扫描在后台处理（无权限时不弹窗，进入首页后再由 Main 处理）。
                LaunchedEffect(lifecycleOwner, readVideoPermission) {
                    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        (application as VideoPlayerApp).ensureLibraryWarmPrefetch(readVideoPermission)
                    }
                }
                SplashScreen(onFinish = { goMain() })
            }
        }
    }
}

// 与 stitch code.html 心形 path 一致（viewBox 0..100）
private const val SplashHeartPathData =
    "M50,85C50,85,15,65,15,35C15,15,40,10,50,30C60,10,85,15,85,35C85,45,80,55,70,65"

// 爱心右下三条波纹（viewBox 坐标；再经 translate(-4,14) 与 stitch code.html 对齐），由内到外 0→1→2
private val SplashRipplePathData = listOf(
    "M69,66 C72,64 75,61 76,58",
    "M72,70 C76,68 79,64 80,59",
    "M75,75 C82,72 87,65 88,57",
)

// 启动页「跳过 / 自动进入主页」倒计时总秒数：只改这里即可；下方 LaunchedEffect 按每秒减一，与 delay(1000) 对应
private const val SplashCountdownSeconds = 10

// 启动页右下三条波纹：animateFloat 0→1 为一整轮（三条线轮流高亮各约 1/3 周期）。毫秒越小转得越快，只改此处即可
private const val SplashRippleCycleMillis = 1200

@Composable
private fun SplashLogoMark(
    primary: Color,
    modifier: Modifier = Modifier,
) {
    val heartPath = remember {
        PathParser.createPathFromPathData(SplashHeartPathData).asComposePath()
    }
    val ripplePaths = remember {
        SplashRipplePathData.map { PathParser.createPathFromPathData(it).asComposePath() }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "splashRipple")
    val ripplePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SplashRippleCycleMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ripplePhase",
    )

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            scale(scaleX = size.width / 100f, scaleY = size.height / 100f, pivot = Offset.Zero) {
                drawPath(
                    heartPath,
                    color = primary.copy(alpha = 0.4f),
                    style = Stroke(
                        width = 2.5f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 4f), 0f),
                    ),
                )
            }
        }
        Image(
            painter = painterResource(R.drawable.ic_splash_agape),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
        Canvas(Modifier.fillMaxSize()) {
            val cycle = (ripplePhase * 3f) % 3f
            val active = floor(cycle.toDouble()).toInt().coerceIn(0, 2)
            val u = (cycle - floor(cycle.toDouble())).toFloat()
            val envelope = sin(u.toDouble() * PI).toFloat().coerceIn(0f, 1f)
            scale(scaleX = size.width / 100f, scaleY = size.height / 100f, pivot = Offset.Zero) {
                translate(-4f, 14f) {
                    repeat(3) { i ->
                        val isLit = i == active
                        val alpha = if (isLit) 0.28f + 0.52f * envelope else 0.24f
                        val strokeW = if (isLit) 1.25f + 0.9f * envelope else 1.35f
                        drawPath(
                            ripplePaths[i],
                            color = primary.copy(alpha = alpha),
                            style = Stroke(width = strokeW, cap = StrokeCap.Round),
                        )
                    }
                }
            }
        }
    }
}

// 与 stitch code.html 中 @keyframes audio-spectrum / .spectrum-bar 一致：scaleY 1↔2.2、底部锚点、ease-in-out、各柱独立 duration 与首次 delay
// 七根竖条对应音阶：哆、瑞、咪、发、嗦、啦、西（柱数与 stitch 五柱不同，为音阶含义）
@Composable
private fun SplashSpectrumBars(primary: Color) {
    val bars = listOf(10.dp, 13.dp, 17.dp, 9.dp, 19.dp, 15.dp, 11.dp)
    val alphas = listOf(0.22f, 0.26f, 0.3f, 0.36f, 0.32f, 0.28f, 0.24f)
    val cycleMs = listOf(820, 960, 1120, 880, 1180, 1020, 940)
    val initialDelayMs = listOf(90, 200, 340, 150, 450, 280, 380)
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(7) { i ->
            SplashSpectrumBar(
                height = bars[i],
                color = primary.copy(alpha = alphas[i]),
                cycleMs = cycleMs[i],
                initialDelayMs = initialDelayMs[i],
            )
        }
    }
}

@Composable
private fun SplashSpectrumBar(
    height: Dp,
    color: Color,
    cycleMs: Int,
    initialDelayMs: Int,
) {
    val scale = remember { Animatable(1f) }
    val halfMs = (cycleMs / 2).coerceAtLeast(1)
    LaunchedEffect(cycleMs, initialDelayMs) {
        delay(initialDelayMs.toLong())
        while (true) {
            scale.animateTo(2.2f, tween(halfMs, easing = FastOutSlowInEasing))
            scale.animateTo(1f, tween(halfMs, easing = FastOutSlowInEasing))
        }
    }
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(height)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 1f)
                scaleY = scale.value
            }
            .background(color, RoundedCornerShape(999.dp)),
    )
}

@Composable
private fun SplashScreen(onFinish: () -> Unit) {
    // 初始秒数见文件顶部 SplashCountdownSeconds
    var secondsLeft by remember { mutableIntStateOf(SplashCountdownSeconds) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000) // 每格一秒；改总时长只调 SplashCountdownSeconds，勿漏改此处逻辑
            secondsLeft--
        }
        onFinish()
    }

    val splashTop = Color(0xFFFCF8FF)
    val splashBottom = Color(0xFFEFEBFF)
    val onSurfaceVariant = Color(0xFF464555)
    val primaryContainer = Color(0xFF4F46E5)
    // 「跳过」胶囊背景约 70% 不透明（alpha 0xB2 ≈ 178/255）
    val skipBg = Color(0xB2DDD6F5)
    val skipBorder = Color(0x66FFFFFF)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(splashTop, splashBottom))),
    ) {
        // 中央柔光（与 code.html 中 glass-glow 类似）
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(maxWidth * 0.9f, maxHeight * 0.55f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryContainer.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Surface(
                    modifier = Modifier
                        .border(1.dp, skipBorder, RoundedCornerShape(999.dp))
                        .clickable { onFinish() },
                    shape = RoundedCornerShape(999.dp),
                    color = skipBg,
                    shadowElevation = 0.dp,
                ) {
                    Text(
                        text = stringResource(R.string.splash_skip, secondsLeft),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 0.1.sp,
                            fontWeight = FontWeight.Medium,
                            color = onSurfaceVariant,
                        ),
                    )
                }
            }

            // 主区块略偏上（上下 weight 不均分），避免整屏垂直居中显得过「沉」在中间
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.weight(0.26f))
                SplashLogoMark(
                    primary = primaryContainer,
                    modifier = Modifier.size(128.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(1.dp)
                        .background(Color(0x4DC7C4D8)),
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.splash_tagline_zh),
                    color = onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                    lineHeight = 25.6.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.splash_tagline_en),
                    color = onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    lineHeight = 19.6.sp,
                    fontWeight = FontWeight.Light,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(36.dp))

                // 与 stitch code.html 底部 visualizer 一致：五根竖条（音律动态 scaleY 动画）
                // 现为七根竖条，对应哆瑞咪发嗦啦西
                SplashSpectrumBars(primary = primaryContainer)
                Spacer(modifier = Modifier.weight(0.74f))
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
