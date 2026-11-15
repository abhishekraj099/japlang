package com.example.jap.ui

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.Crossfade
import com.example.jap.ui.theme.isAppDark
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.data.Config
import com.example.jap.audio.LocalVoice
import com.example.jap.data.Course
import com.example.jap.data.Daily
import com.example.jap.data.Exercises
import com.example.jap.data.Item
import com.example.jap.data.Kind
import com.example.jap.data.Lesson
import com.example.jap.data.Memory
import com.example.jap.data.LessonType
import com.example.jap.data.Progress
import com.example.jap.data.Review
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
private enum class Stage { SPLASH, DEMO, SIGN_IN, SETUP, STREAK_INTRO, MAIN }

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
    class Legal(val doc: LegalDoc) : Page
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
        // The welcome demo plays once on a new install, before the first sign-in.
        !progress.signedIn && !progress.demoSeen -> Stage.DEMO
        !progress.signedIn -> Stage.SIGN_IN
        !progress.onboarded -> Stage.SETUP
        !progress.streakIntroSeen -> Stage.STREAK_INTRO
        else -> Stage.MAIN
    }
    // The splash and sign-in header are dark at the top; everything else follows the theme.
    StatusBarIcons(light = stage == Stage.SPLASH || stage == Stage.SIGN_IN || isAppDark())
    Crossfade(stage, label = "stage") { current ->
        when (current) {
            Stage.SPLASH -> SplashScreen { splash = false }
            Stage.DEMO -> DemoScreen(progress, onDone = progress::finishDemo)
            Stage.SIGN_IN -> AuthScreen(onSignIn = progress::signIn)
            Stage.SETUP -> OnboardingScreen(progress) { takeTest ->
                progress.updateBeginner(!takeTest)
                if (takeTest) page = Page.Placement
                progress.finishOnboarding()
            }
            // The last intro card goes straight into the first unfinished lesson, unless the placement test is waiting.
            Stage.STREAK_INTRO -> StreakIntro(progress) { if (page == null) page = nextLesson(progress)?.let { Page.Run(lessonSession(it)) } }
            Stage.MAIN -> {
                // The guide sits above every page so it can point at anything on screen.
                val voice = LocalVoice.current
                val guide = remember { GuideController(progress, voice) }
                CompositionLocalProvider(LocalGuide provides guide) {
                    Box(Modifier.fillMaxSize()) {
                        Main(progress, page) { page = it }
                        GuideOverlay(guide)
                    }
                }
            }
        }
    }
}

/** The first lesson on the path the learner has not finished. */
/** Due grammar points added to one revision session, on top of its items. */
internal const val GRAMMAR_PER_SESSION = 3

private fun nextLesson(progress: Progress): Lesson? = Course.lessons.firstOrNull { it.id !in progress.done }

/** A run through one course lesson, in its unit's colour. */
private fun lessonSession(lesson: Lesson) = Session(
    lesson.title, lesson.id, Color(Course.unitOf(lesson).color), Config.lessonXp, SessionKind.LESSON, Exercises.forLesson(lesson)
)

@Composable
private fun Main(progress: Progress, page: Page?, go: (Page?) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val scheme = MaterialTheme.colorScheme
    // Where closing a video returns to: its list, history, or the tabs.
    val back = remember { mutableStateOf<Page?>(null) }

    fun run(
        title: String, kind: SessionKind, xp: Int, exercises: List<com.example.jap.data.Exercise>,
        lessonId: String? = null, color: Color = scheme.secondary, review: String? = null
    ) {
        go(Page.Run(Session(title, lessonId, color, xp, kind, exercises, review = review)))
    }

    /** A practice run over [items]; its "Keep going" builds the next one from the memory as saved by then. */
    fun revision(due: Boolean, items: List<Item>): Session {
        // Both kinds are revision games; asked in the way that suits how well each item is known.
        val entries = items.map { it to Memory.level(progress.srs[it.id]) }
        return Session(
            if (due) "Revision" else "Practice", null, scheme.secondary, Config.practiceXp, SessionKind.PRACTICE,
            // A due revision also brings back a few due grammar points, graded and saved like items.
            Exercises.forRevision(entries) + if (due) Exercises.forGrammar(Review.dueGrammar(progress.srs).take(GRAMMAR_PER_SESSION)) else emptyList(),
            review = if (due) "reading" else "practice",
            next = {
                val plan = Review.plan(progress.dueSchedule(writing = false, System.currentTimeMillis()), progress.reviewedToday(writing = false), progress.daysAway)
                val more = Review.pick(due, plan, progress.srs, SESSION_SIZE)
                // Nothing left due: carry on with the weakest words instead.
                if (more.isNotEmpty() || (due && Review.dueGrammar(progress.srs).isNotEmpty())) revision(due, more)
                else if (due) Review.pick(false, plan, progress.srs, SESSION_SIZE).takeIf { it.isNotEmpty() }?.let { revision(false, it) }
                else null
            }
        )
    }

    fun practice(title: String, items: List<Item>) = run(title, SessionKind.PRACTICE, Config.practiceXp, Exercises.forReview(items))

    fun lesson(lesson: Lesson) = go(Page.Run(lessonSession(lesson)))

    /** Saves a finished session; returns where to go next (the streak page when the day's first session waters the tree). */
    fun finish(session: Session, o: Outcome): Page? {
        val firstToday = !progress.studiedToday
        progress.finish(session.lessonId, o.xp, o.results, session.kind, o.perfect, o.answered, o.correct, o.bestRun, o.writeResults)
        when (session.review) {
            "reading" -> progress.addReviewed(writing = false, count = o.results.size)
            "writing" -> progress.addReviewed(writing = true, count = o.writeResults.size)
        }
        val unit = session.testOut
        if (unit != null && o.answered > 0 && o.correct * 100 / o.answered >= Config.testOutPercent) progress.completeUnit(unit)
        // The first session of the day fills the watering can, so show the tree waiting to be watered.
        return if (firstToday && progress.tree.canWater) Page.Streak else null
    }

    when (page) {
        // Keyed on the session so "Keep going" starts with fresh state instead of the finished summary.
        is Page.Run -> key(page.session) { LessonScreen(
            session = page.session,
            progress = progress,
            onExit = { go(null) },
            onFinish = { o -> go(finish(page.session, o)) },
            onKeepGoing = { o ->
                // Saved first, so the next session is built from the updated memory.
                val streak = finish(page.session, o)
                val next = page.session.next?.invoke()
                go(if (next != null && next.exercises.isNotEmpty()) Page.Run(next) else streak)
            }
        ) }
        is Page.RolePlay -> RolePlayScreen(page.scene, progress, onBack = { go(null) })
        is Page.Cards -> FlashcardScreen(page.items, onBack = { go(Page.Words) })
        Page.Words -> WordsScreen(progress, onBack = { go(null) }, onCards = { go(Page.Cards(it)) })
        is Page.Game -> GameScreen(progress, onBack = { go(null) }, script = page.script)
        Page.Test -> ExamScreen(progress, onBack = { go(null) })
        Page.Placement -> PlacementScreen(progress, onLeave = { go(null) })
        Page.Streak -> StreakScreen(progress, onBack = { go(null) }, onStudy = {
            tab = 0
            nextLesson(progress)?.let(::lesson) ?: go(null)
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
        Page.Settings -> SettingsScreen(progress, onRetakeTest = { go(Page.Placement) }, onLegal = { go(Page.Legal(it)) }, onBack = { go(null) })
        is Page.Legal -> LegalScreen(page.doc, onBack = { go(Page.Settings) })
        Page.Ranking -> SubScreen("Ranking", onBack = { go(null) }) { LeagueScreen(progress) }
        Page.Practice -> SubScreen("Practice", onBack = { go(null) }, action = { GuideHelpButton("practice") }) {
            PracticeScreen(
                progress,
                onReview = { _, items, due -> go(Page.Run(revision(due, items))) },
                onWords = { go(Page.Words) },
                onSpeak = { run("Speaking", SessionKind.PRACTICE, Config.practiceXp, Exercises.forSpeaking(Course.phrases)) },
                onDictate = {
                    // Characters the learner has met; beginners start with the basic hiragana.
                    val learned = progress.srs.keys.mapNotNull(Course::item).filter { it.kind != Kind.WORD }
                    val pool = if (learned.size >= MIN_LEARNED) learned
                    else Course.kanaChart(Kind.HIRAGANA).first().second.flatten().filterNotNull()
                    run("Listen & write", SessionKind.PRACTICE, Config.practiceXp, Exercises.forDictation(pool))
                },
                onWriting = { items -> run("Writing practice", SessionKind.PRACTICE, Config.practiceXp, Exercises.forWriting(items), review = "writing") },
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
                                // Same 24dp box as the icons, so this tab's label lines up with the others.
                                else Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                    Text("あ", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 20.sp)
                                }
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
            Box(Modifier.fillMaxSize()) {
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
            // The guide's helper button, in the same corner on every main tab.
            if (progress.guideOn) {
                GuideHelperButton(
                    listOf("learn", "alphabet", "lessonplus", "video", "me")[tab],
                    // Peeks in from the right edge, so it never sits on top of cards and labels.
                    Modifier.align(Alignment.BottomEnd).padding(padding).padding(bottom = 12.dp).offset(x = 18.dp)
                )
            }
            }
        }
    }
}
