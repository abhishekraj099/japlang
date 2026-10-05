package com.example.jap.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.data.Config
import com.example.jap.data.Course
import com.example.jap.data.Daily
import com.example.jap.data.Exercises
import com.example.jap.data.Item
import com.example.jap.data.Kind
import com.example.jap.data.Lesson
import com.example.jap.data.LessonType
import com.example.jap.data.Progress
import com.example.jap.data.Scenario
import com.example.jap.data.Scenarios
import com.example.jap.data.SessionKind
import com.example.jap.data.Video
import com.example.jap.data.VideoSection

private const val MIN_LEARNED = 5

/** Bottom tabs. A null icon means the label's first character is drawn instead. */
private val tabs = listOf<Pair<ImageVector?, String>>(
    Icons.AutoMirrored.Rounded.MenuBook to "Learn",
    null to "Alphabet",
    Icons.Rounded.AutoAwesome to "Lesson+",
    Icons.Rounded.SmartDisplay to "Video",
    Icons.Rounded.Person to "Me"
)

/** Where the learner is in the first-run flow. */
private enum class Stage { SPLASH, SIGN_IN, SETUP, STREAK_INTRO, MAIN }

/** A full-screen page shown on top of the tabs. */
private sealed interface Page {
    class Run(val session: Session) : Page
    class RolePlay(val scene: Lesson) : Page
    class Cards(val items: List<Item>) : Page
    data object Words : Page
    class Game(val script: Kind? = null) : Page
    data object Test : Page
    data object Placement : Page
    data object Streak : Page
    data object Practice : Page
    data object Trophies : Page
    data object Tips : Page
    data object Ranking : Page
    data object Settings : Page
    class Situation(val scenario: Scenario) : Page
    data object Situations : Page
    class Cert(val key: String, val course: String) : Page
    data object Talk : Page
    class Watch(val video: Video, val speaking: Boolean) : Page
    class VideoList(val section: VideoSection) : Page
    data object VideoHistory : Page
}

@Composable
fun JapApp(progress: Progress) {
    var splash by remember { mutableStateOf(true) }
    var page by remember { mutableStateOf<Page?>(null) }

    // Spend omamori on any days missed since the last visit before showing the streak.
    LaunchedEffect(Unit) { progress.checkStreak() }

    val stage = when {
        splash -> Stage.SPLASH
        !progress.signedIn -> Stage.SIGN_IN
        !progress.onboarded -> Stage.SETUP
        !progress.streakIntroSeen -> Stage.STREAK_INTRO
        else -> Stage.MAIN
    }
    // The splash and sign-in header are dark at the top; everything else follows the theme.
    StatusBarIcons(light = stage == Stage.SPLASH || stage == Stage.SIGN_IN || isSystemInDarkTheme())
    Crossfade(stage, label = "stage") { current ->
        when (current) {
            Stage.SPLASH -> SplashScreen { splash = false }
            Stage.SIGN_IN -> AuthScreen(onSignIn = progress::signIn)
            Stage.SETUP -> OnboardingScreen(progress) { takeTest ->
                if (takeTest) page = Page.Placement
                progress.finishOnboarding()
            }
            Stage.STREAK_INTRO -> StreakIntro(progress) {}
            Stage.MAIN -> Main(progress, page) { page = it }
        }
    }
}

@Composable
private fun Main(progress: Progress, page: Page?, go: (Page?) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val scheme = MaterialTheme.colorScheme
    // Where closing a video returns to: its list, history, or the tabs.
    val back = remember { mutableStateOf<Page?>(null) }

    fun run(title: String, kind: SessionKind, xp: Int, exercises: List<com.example.jap.data.Exercise>, lessonId: String? = null, color: Color = scheme.secondary) {
        go(Page.Run(Session(title, lessonId, color, xp, kind, exercises)))
    }

    fun practice(title: String, items: List<Item>) = run(title, SessionKind.PRACTICE, Config.practiceXp, Exercises.forReview(items))

    fun lesson(lesson: Lesson) = run(
        lesson.title, SessionKind.LESSON, Config.lessonXp, Exercises.forLesson(lesson),
        lessonId = lesson.id, color = Color(Course.unitOf(lesson).color)
    )

    when (page) {
        is Page.Run -> LessonScreen(
            session = page.session,
            progress = progress,
            onExit = { go(null) },
            onFinish = { o ->
                progress.finish(page.session.lessonId, o.xp, o.results, page.session.kind, o.perfect, o.answered, o.correct, o.bestRun)
                val unit = page.session.testOut
                if (unit != null && o.answered > 0 && o.correct * 100 / o.answered >= Config.testOutPercent) progress.completeUnit(unit)
                go(null)
            }
        )
        is Page.RolePlay -> RolePlayScreen(page.scene, progress, onBack = { go(null) })
        is Page.Cards -> FlashcardScreen(page.items, onBack = { go(Page.Words) })
        Page.Words -> WordsScreen(progress, onBack = { go(null) }, onCards = { go(Page.Cards(it)) })
        is Page.Game -> GameScreen(progress, onBack = { go(null) }, script = page.script)
        Page.Test -> ExamScreen(progress, onBack = { go(null) })
        Page.Placement -> PlacementScreen(progress, onDone = { go(null) })
        Page.Streak -> StreakScreen(progress, onBack = { go(null) }, onStudy = {
            tab = 0
            Course.lessons.firstOrNull { it.id !in progress.done }?.let(::lesson) ?: go(null)
        })
        Page.Trophies -> TrophiesScreen(progress, onBack = { go(null) })
        Page.Tips -> TipsScreen(progress, onBack = { go(null) })
        is Page.Cert -> CertificateScreen(progress, page.key, page.course, onBack = { go(null) })
        is Page.Situation -> ScenarioScreen(page.scenario, progress, onBack = { go(null) }, onLearn = { i ->
            val s = page.scenario
            run("${s.title} · ${s.parts[i].title}", SessionKind.LESSON, Config.lessonXp, Exercises.forLesson(Scenarios.lesson(s, i)), lessonId = s.partId(i), color = Color(s.color))
        })
        Page.Situations -> AllScenariosScreen(progress, onScenario = { go(Page.Situation(it)) }, onBack = { go(null) })
        Page.Talk -> SubScreen("Role-play", onBack = { go(null) }) { TalkScreen(onScene = { go(Page.RolePlay(it)) }) }
        is Page.Watch -> VideoPlayerScreen(page.video, progress, page.speaking, onBack = { go(back.value) })
        is Page.VideoList -> VideoListScreen(page.section, progress, onOpen = { v, s -> back.value = page; go(Page.Watch(v, s)) }, onBack = { go(null) })
        Page.VideoHistory -> VideoHistoryScreen(progress, onOpen = { v, s -> back.value = page; go(Page.Watch(v, s)) }, onBack = { go(null) })
        Page.Settings -> SettingsScreen(progress, onRetakeTest = { go(Page.Placement) }, onBack = { go(null) })
        Page.Ranking -> SubScreen("Ranking", onBack = { go(null) }) { LeagueScreen(progress) }
        Page.Practice -> SubScreen("Practice", onBack = { go(null) }) {
            PracticeScreen(
                progress,
                onReview = ::practice,
                onWords = { go(Page.Words) },
                onSpeak = { run("Speaking", SessionKind.PRACTICE, Config.practiceXp, Exercises.forSpeaking(Course.phrases)) },
                onDictate = {
                    // Characters the learner has met; beginners start with the basic hiragana.
                    val learned = progress.srs.keys.mapNotNull(Course::item).filter { it.kind != Kind.WORD }
                    val pool = if (learned.size >= MIN_LEARNED) learned
                    else Course.kanaChart(Kind.HIRAGANA).first().second.flatten().filterNotNull()
                    run("Listen & write", SessionKind.PRACTICE, Config.practiceXp, Exercises.forDictation(pool))
                },
                onGame = { go(Page.Game()) },
                onExam = { go(Page.Test) }
            )
        }
        null -> Scaffold(
            containerColor = scheme.background,
            bottomBar = {
                NavigationBar(containerColor = scheme.surface, tonalElevation = 0.dp) {
                    tabs.forEachIndexed { i, (icon, label) ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = {
                                if (icon != null) Icon(icon, contentDescription = null)
                                else Text("あ", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = scheme.onPrimary,
                                selectedTextColor = scheme.primary,
                                indicatorColor = scheme.primary,
                                unselectedIconColor = scheme.onSurfaceVariant,
                                unselectedTextColor = scheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { padding ->
            val modifier = Modifier.padding(padding)
            when (tab) {
                0 -> LearnScreen(
                    progress,
                    onOpen = ::lesson,
                    onTestOut = { unit ->
                        go(
                            Page.Run(
                                Session(
                                    "Test out: ${unit.title}", null, Color(unit.color), Config.practiceXp, SessionKind.PRACTICE,
                                    Exercises.forLesson(unit.lessons.last()), testOut = unit
                                )
                            )
                        )
                    },
                    onChallenge = {
                        run("Daily challenge", SessionKind.CHALLENGE, Config.challengeXp, Daily.challenge(progress))
                    },
                    onStreak = { go(Page.Streak) },
                    onKanaGame = { go(Page.Game(it)) },
                    onSettings = { go(Page.Settings) },
                    modifier = modifier
                )
                1 -> AlphabetScreen(
                    progress,
                    onCharacter = { item, row, id ->
                        run("${item.jp}  ${item.romaji}", SessionKind.PRACTICE, Config.practiceXp, Exercises.forCharacter(item, row), lessonId = id)
                    },
                    onRow = { title, items, id ->
                        val asLesson = Lesson(id, title, LessonType.KANA, items.first().jp, items = items)
                        run(title, SessionKind.PRACTICE, Config.practiceXp, Exercises.forLesson(asLesson), lessonId = id)
                    },
                    onTest = { title, items, id ->
                        run(title, SessionKind.PRACTICE, Config.practiceXp, Exercises.forReview(items), lessonId = id)
                    },
                    onDrill = ::practice,
                    onCertificate = { key, course -> go(Page.Cert(key, course)) },
                    modifier = modifier
                )
                2 -> TopicsScreen(
                    progress,
                    onScenario = { go(Page.Situation(it)) },
                    onAll = { go(Page.Situations) },
                    onCourseTopic = { title, lessons ->
                        run(title, SessionKind.PRACTICE, Config.practiceXp, Exercises.forTopic(lessons))
                    },
                    modifier = modifier
                )
                3 -> VideoScreen(
                    progress,
                    onOpen = { v, s -> back.value = null; go(Page.Watch(v, s)) },
                    onSeeMore = { go(Page.VideoList(it)) },
                    onHistory = { go(Page.VideoHistory) },
                    modifier = modifier
                )
                else -> MeScreen(
                    progress,
                    onLink = { link ->
                        go(
                            when (link) {
                                MeLink.PRACTICE -> Page.Practice
                                MeLink.TALK -> Page.Talk
                                MeLink.WORDS -> Page.Words
                                MeLink.GAME -> Page.Game()
                                MeLink.EXAM -> Page.Test
                                MeLink.TROPHIES -> Page.Trophies
                                MeLink.TIPS -> Page.Tips
                                MeLink.RANKING -> Page.Ranking
                                MeLink.SETTINGS -> Page.Settings
                            }
                        )
                    },
                    modifier = modifier
                )
            }
        }
    }
}
