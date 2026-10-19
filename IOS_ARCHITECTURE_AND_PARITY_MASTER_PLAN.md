# manga-dl iOS — Architecture & Ecosystem Parity Master Plan

## 1. Executive Summary & Ecosystem Reality Check

The original **v1.0 Native App Spec (2026)** envisioned a lightweight self-contained model: an embedded JavaScript engine (QuickJS / JavaScriptCore) executing `.js` extension files shared with the web backend, a 4-table SQLite schema, direct CBZ chapter downloads, and standard paginated/webtoon readers.

However, during real-world implementation across **Android (`manga-dl-android`)** and **Desktop (`manga-dl-windows`)**, the manga-dl ecosystem evolved into a full-featured multimedia reader and tracker platform:

```
Original Spec (v1.0)                    Current Ecosystem Reality (v2.0)
────────────────────                    ────────────────────────────────
• 8 Manga sources via JS apiFetch       • 24 Native Sources (9 Manga + 15 Web Novels)
• QuickJS / JSC regex scraping          • Native HTML DOM parsing (Jsoup in Kotlin / AngleSharp in C#)
• Manga-only reader                     • Dual Reader Engine: Manga Pager/Webtoon + Novel Reader (TTS, fonts, margins)
• CBZ-only downloads                    • Dual Archive Engine: CBZ (manga) + EPUB 3 (novels)
• Flat 4-table SQLite schema            • Dynamic Shelves/Categories, reading streaks, progress tracking
• Basic Reader                          • Ambient Lighting (Ambilight), dual-page spread, tap zones, border crop
• No tracking                           • AniList + MyAnimeList 2-way OAuth chapter progress sync
• No gamification                       • Solo Leveling Hunter Rank system (EXP, ranks E→Monarch, 25+ badges)
• No social / community                 • Threaded chapter comments & Global Guild Leaderboard
• Flat backup                           • Dual Backup Engine: .mangadl JSON + Tachiyomi/Mihon .tachibk (Protobuf)
• No security                           • Biometric Lock (Face ID / Touch ID), Incognito mode
```

To deliver a **first-class iOS experience** that achieves 100% ecosystem parity, iOS must **not** regress to the minimal v1.0 prototype. It should adopt the proven patterns from Android and Desktop while leveraging the unique strengths of the Apple platform (SwiftUI, Metal, GRDB.swift, Nuke, Files.app, and BackgroundTasks).

---

## 2. Core Architectural Decisions

### 2.1 Language & UI Framework
* **Swift 6 & SwiftUI (iOS 16.0+ / iPadOS 16.0+ / macOS 13.0+)**:
  * Modern `NavigationStack(path:)` for type-safe route management (matching Android's `MangaDlNavHost`).
  * `NavigationSplitView` on iPadOS and macOS gives an adaptive Sidebar + Detail layout for free.
  * Custom Manga-DL design tokens: Dark & AMOLED Pure Black palettes, configurable accent colors, fluid typography.

### 2.2 The Source Engine Strategy (Native Swift vs. JavaScriptCore)
* **The Problem with a pure JS bridge on iOS**:
  Manga and novel sources continuously alter their HTML layout, employ canvas descrambling (Asura Scans), encrypted token handshakes (ComixTo), and require complex DOM tree navigation. Running raw `.js` in `JavaScriptCore` without browser DOM globals (`document`, `window`) requires bundling heavy polyfills (Cheerio/JSDOM), introducing memory spikes and performance degradation in iOS background workers.
* **The Recommended Approach for iOS**:
  * **Native Swift Engines with `SwiftSoup`**: Port Android's 24 Kotlin source classes directly to Swift using [SwiftSoup](https://github.com/scinfu/SwiftSoup) (the battle-tested Swift port of Jsoup). The syntax between Kotlin Jsoup and Swift SwiftSoup is nearly **1:1**:
    ```swift
    // Kotlin (Android):
    val title = doc.selectFirst("h1.entry-title")?.text().orEmpty()
    // Swift (iOS):
    let title = try? doc.select("h1.entry-title").first()?.text() ?? ""
    ```
  * **Hardware-Accelerated Descramblers**: Direct pixel/slice rearrangement for canvas-scrambled pages using Core Graphics / Accelerate, executing with native C-speed on Apple Silicon.
  * **Optional JS Engine Sidecar**: Keep `JavaScriptCore` available strictly as an auxiliary host for third-party user-imported scripts or dynamic community extensions.

### 2.3 Persistence & Database (GRDB.swift vs. SwiftData)
* **Choice**: **[GRDB.swift](https://github.com/groue/GRDB.swift)**.
* **Rationale**:
  * SwiftData lacks the stability, complex foreign key cascade triggers, and batch upsert performance needed for thousands of chapter records.
  * GRDB is raw SQLite wrapped in idiomatic Swift records with compile-time type safety.
  * Its **`ValueObservation`** API publishes reactive updates directly to SwiftUI views, functioning identically to Room's reactive `Flow` / `StateFlow` on Android.
  * Ensures 100% table and column schema parity with Android's Room DB and Supabase PostgreSQL.

### 2.4 Image Loading & Reader Pipeline
* **Choice**: **[Nuke](https://github.com/kean/Nuke)** (with NukeUI).
* **Rationale**:
  * Engineered specifically for high-throughput image rendering (manga page pagers and continuous webtoons).
  * Built-in prefetching pipeline: preloads the next 3–5 pages ahead of the current viewport and proactively releases offscreen pages to conserve memory.
  * Supports custom image processing pipelines for real-time descrambling.
  * Zero-copy disk mapping when reading locally downloaded chapters.

---

## 3. Comprehensive Layer-by-Layer Platform Mapping

| Capability | Android (`manga-dl-android`) | iOS Equivalent (`manga-dl-ios`) |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.0 | Swift 6 |
| **UI Framework** | Jetpack Compose + Material 3 | SwiftUI (iOS 16+) |
| **Navigation** | `NavHost` + Compose Destinations | `NavigationStack` + `NavigationSplitView` (iPad) |
| **Concurrency & Reactivity** | Coroutines + `Flow` / `StateFlow` | Swift Concurrency (`async`/`await`, `AsyncSequence`, `@Observable`) |
| **Database** | Room (SQLite) + KSP | **GRDB.swift** (SQLite) |
| **HTTP Client** | OkHttp 4 + Interceptors | `URLSession` with custom `URLSessionConfiguration` |
| **HTML Parsing** | Jsoup | **SwiftSoup** (1:1 Jsoup port) |
| **Image Loading** | Coil Compose | **NukeUI** / `LazyImage` |
| **Manga Reader** | HorizontalPager + LazyColumn | SwiftUI `TabView(.page)` / `UICollectionView` |
| **Ambient Ambilight** | `Modifier.blur(56.dp)` + `SubcomposeAsyncImage` | SwiftUI `.blur(radius: 56)` + `scaleEffect` + Metal |
| **Novel Reader** | Compose LazyColumn + Text Layout | SwiftUI ScrollView + CoreText typography |
| **Text-to-Speech (TTS)** | Android `TextToSpeech` API | iOS `AVSpeechSynthesizer` (`AVFoundation`) |
| **Manga Downloads** | Zip archive + `ComicInfo.xml` (CBZ) | `ZIPFoundation` + XML serializer (CBZ) |
| **Novel Downloads** | `NovelEpubBuilder` (EPUB 3) | Pure Swift EPUB 3 packager (`ZIPFoundation`) |
| **File Storage / Sharing** | MediaStore API / Downloads folder | `FileManager` Document Directory + `UIFileSharingEnabled` (Files.app) |
| **Biometric Auth** | `androidx.biometric.BiometricPrompt` | `LocalAuthentication` (`LAContext`, Face ID / Touch ID) |
| **Cloud Sync** | Supabase Kotlin SDK | `supabase-community/supabase-swift` |
| **Trackers (AniList / MAL)** | Custom OAuth + OkHttp | `ASWebAuthenticationSession` + `URLSession` |
| **Backup / Restore** | JSON + `TachiyomiBackupDecoder` (Protobuf) | `SwiftProtobuf` + Gzip decoder (.tachibk & .mangadl) |
| **Background Tasks** | Android `WorkManager` | iOS `BGAppRefreshTask` / `BGProcessingTask` |

---

## 4. Deep-Dive Module Breakdown

### Module 1: Sources & Catalog Architecture

```
┌────────────────────────────────────────────────────────┐
│                     SourceManager                      │
├──────────────────────────┬─────────────────────────────┤
│   MangaSource Protocol   │    NovelSource Protocol     │
├──────────────────────────┼─────────────────────────────┤
│ • MangaDex               │ • Royal Road                │
│ • Asura Scans (+Canvas)  │ • NovelBin                  │
│ • MangaKakalot           │ • Ranobes                   │
│ • MangaKatana            │ • LightNovelWorld           │
│ • Omega Scans            │ • NovelFire                 │
│ • Manganato              │ • FreeWebNovel              │
│ • Webtoons               │ • NovelFull                 │
│ • TCB Scans              │ • ReadNovelFull             │
│ • YaoiScan               │ • Chrysanthemum Garden      │
│                          │ • Comrade Mao               │
│                          │ • Asia Novel                │
│                          │ • ReadHive                  │
│                          │ • LibRead                   │
│                          │ • Bright Novel              │
│                          │ • Novel Buddy               │
└──────────────────────────┴─────────────────────────────┘
```

1. **Protocols**:
   * `MangaSource`: `search(query, page)`, `getPopular(page)`, `getLatest(page)`, `getDetail(id)`, `getPages(chapterId)`.
   * `NovelSource`: Same metadata lifecycle, but `getChapterContent(chapterId)` returns sanitized HTML / text paragraphs.
2. **Unified Global Search Engine**:
   * Uses Swift concurrency `TaskGroup` to fan out search queries across all active sources concurrently, returning an aggregated stream (`AsyncStream<SearchResult>`) in real time.
3. **Cloudflare & Turnstile Bypass**:
   * Offscreen `WKWebView` cookie collector with automatic session synchronization back into `URLSession.shared`.

---

### Module 2: The Reading Experiences

#### A. Manga Reader (Paginated & Webtoon)
* **Reading Modes**:
  * Left-to-Right Pager (`.ltr`)
  * Right-to-Left Pager (`.rtl`) — Standard Japanese manga reading mode
  * Vertical Pager (`.vertical`)
  * Continuous Webtoon (`.webtoon`) with adjustable side padding (`0%` to `40%`)
  * **Dual-Page Spread**: Automatically pairs consecutive pages side-by-side in landscape mode on iPad.
* **Ambient Lighting (Ambilight)**:
  * Leverages Metal-accelerated SwiftUI modifiers for smooth performance:
    ```swift
    ZStack {
        if ambilightEnabled, let currentPageUrl {
            LazyImage(url: currentPageUrl) { state in
                state.image?
                    .resizable()
                    .scaledToFill()
                    .scaleEffect(1.25)
                    .blur(radius: 56)
                    .opacity(0.45)
            }
            .overlay(
                RadialGradient(
                    colors: [Color.clear, Color.black.opacity(0.45), Color.black.opacity(0.85)],
                    center: .center,
                    startRadius: 80,
                    endRadius: 400
                )
            )
        }
        PageContentView()
    }
    ```
* **Interactive Controls**:
  * Configurable edge tap zones (Left/Right to advance vs. center tap to show controls).
  * Hardware volume key page turns (via `AVAudioSession` / `MPVolumeView` event monitoring).
  * In-reader Brightness slider, Crop borders toggle, Bookmark button, and Chapter comments sheet.

#### B. Novel Reader & Text-to-Speech (TTS)
* **Typography Controls**:
  * Dynamic font selection: SF Pro, New York Serif, Georgia, Atkinson Hyperlegible.
  * Sliders for Font Size (12pt–32pt), Line Spacing (1.2–2.0), Paragraph Spacing, and Margins.
  * Color Themes: Pure Black OLED, Charcoal Gray, Warm Sepia, Soft Cream, Pure White.
* **Native Audio Engine (TTS)**:
  * Uses `AVSpeechSynthesizer` with background audio playback entitlement (`audio` in `UIBackgroundModes`).
  * Real-time paragraph/sentence highlight tracking as text is spoken.
  * Lock screen and Control Center media controls via `MPNowPlayingInfoCenter` and `MPRemoteCommandCenter`.

---

### Module 3: Database & Local Shelves (GRDB.swift)

#### Database Schema (1:1 with Android Room v3):
```sql
CREATE TABLE categories (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE library (
    id TEXT PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    cover_url TEXT,
    provider TEXT NOT NULL,
    url TEXT NOT NULL,
    added_at INTEGER NOT NULL,
    last_read_chapter_id TEXT,
    last_read_at INTEGER,
    total_chapters INTEGER DEFAULT 0,
    read_count INTEGER DEFAULT 0
);

CREATE TABLE library_categories (
    manga_id TEXT NOT NULL REFERENCES library(id) ON DELETE CASCADE,
    category_id TEXT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (manga_id, category_id)
);

CREATE TABLE reading_progress (
    manga_id TEXT NOT NULL,
    chapter_id TEXT NOT NULL,
    provider TEXT NOT NULL,
    page INTEGER DEFAULT 0,
    total_pages INTEGER DEFAULT 0,
    read_at INTEGER NOT NULL,
    completed INTEGER DEFAULT 0,
    PRIMARY KEY (manga_id, chapter_id)
);

CREATE TABLE download_queue (
    id TEXT PRIMARY KEY NOT NULL,
    manga_id TEXT NOT NULL,
    manga_title TEXT NOT NULL,
    chapter_id TEXT NOT NULL,
    chapter_title TEXT NOT NULL,
    provider TEXT NOT NULL,
    status TEXT NOT NULL, -- 'queued', 'downloading', 'completed', 'failed'
    progress INTEGER DEFAULT 0,
    file_path TEXT,
    added_at INTEGER NOT NULL
);
```

#### Reactive Swift ViewModels:
```swift
class LibraryViewModel: ObservableObject {
    @Published var shelves: [CategoryRecord] = []
    @Published var selectedShelfId: String? = nil
    @Published var items: [LibraryMangaRecord] = []
    
    func startObserving() {
        ValueObservation.tracking { db in
            try LibraryMangaRecord.fetchAll(db)
        }.start(in: AppDatabase.shared.reader) { error in
            print("Database observation error: \(error)")
        } onChange: { [weak self] records in
            self?.items = records
        }
    }
}
```

---

### Module 4: Downloads, Packaging & Files.app Integration

1. **Storage Location & Files.app Access**:
   * Chapters are stored in:
     `FileManager.default.urls(for: .documentDirectory, in: .userDomainMask) / "manga-dl" / "<Manga Title>"`
   * **Crucial `Info.plist` settings**:
     * `UIFileSharingEnabled = true`
     * `LSSupportsOpeningDocumentsInPlace = true`
   * **User Experience**: Users can open the native iOS **Files app**, view, move, and AirDrop their downloaded CBZ and EPUB files to Mac or third-party readers (e.g., Panels, Chunky, Apple Books).
2. **CBZ Packaging (`ZIPFoundation`)**:
   * Images downloaded sequentially as `0001.jpg`, `0002.jpg`, etc.
   * Generates a standard `ComicInfo.xml` metadata file at the zip root.
3. **EPUB 3 Builder**:
   * Bundles novel chapters into compliant `.epub` archives:
     * `mimetype` (`application/epub+zip`).
     * `META-INF/container.xml`.
     * `OEBPS/content.opf` (manifest + reading spine).
     * `OEBPS/toc.ncx` & `nav.xhtml` (hierarchical Table of Contents).
     * Formatted chapter `.xhtml` pages with responsive CSS.
4. **Local Archive Import**:
   * Native `UIDocumentPickerViewController` allowing users to import external `.cbz`, `.zip`, and `.epub` archives directly into their library with natural chapter sort order.

---

### Module 5: Gamification, Social & Cloud Sync

```
┌─────────────────────────────────────────────────────────────┐
│                      manga-dl iOS Core                      │
├──────────────────────────────┬──────────────────────────────┤
│  Hunter Gamification Engine  │   Community Social Network   │
├──────────────────────────────┼──────────────────────────────┤
│ • EXP Calculation:           │ • Chapter Comments API       │
│   (Ch * 10) + (Streak * 50)  │   - Threaded replies         │
│   + (Library * 25)           │   - Optimistic Likes         │
│ • Ranks: E → D → C → B →     │   - Markdown support         │
│          A → S → Monarch     │ • Guild Leaderboard          │
│ • 25+ Badges across          │   - All-Time / Yearly /      │
│   Chapters, Streak & Library │     Monthly / Weekly podium  │
└──────────────────────────────┴──────────────────────────────┘
```

1. **Gamification (Solo Leveling System)**:
   * 100% mathematical parity with Android's `HunterRankModels.kt`:
     * Score Formula: `(chaptersRead * 10) + (streakDays * 50) + (libraryCount * 25)`
     * Ranks: **Shadow Monarch** (#1 Sovereign), **S-Rank** (15,000+), **A-Rank** (8,000+), **B-Rank** (4,000+), **C-Rank** (1,500+), **D-Rank** (500+), **E-Rank** (0+).
   * Badge showcase in user profile with pinned badges support.
2. **Community Chapter Comments**:
   * Integrated with `GET/POST /comments` endpoint.
   * Bottom sheet interface with optimistic likes, reply threading, and user avatars.
3. **Global Leaderboard**:
   * Integrated with `GET /users/leaderboard?period=...` endpoint.
   * Top 3 podium display (Gold Monarch, Silver S-Rank, Bronze S-Rank) and personal standing card.
4. **Trackers (AniList & MyAnimeList)**:
   * Uses `ASWebAuthenticationSession` for Safari OAuth authentication.
   * Automatically updates remote chapter count upon completing a chapter in the reader.
5. **Security & Privacy**:
   * `LocalAuthentication` framework for Face ID / Touch ID app lock.
   * Incognito mode (pauses history logging and reading progress tracking).

---

### Module 6: Supabase Backend Integration & Cloud Synchronization Engine

To maintain 100% cross-device parity with Android ([`SupabaseSyncManager.kt`](file:///home/zenmi/Projects/manga-dl-android/app/src/main/kotlin/com/mangadl/android/data/sync/supabase/SupabaseSyncManager.kt), [`SupabaseModels.kt`](file:///home/zenmi/Projects/manga-dl-android/app/src/main/kotlin/com/mangadl/android/data/sync/supabase/SupabaseModels.kt)), Web, and Windows Desktop, the iOS app integrates directly with the shared Supabase project (`Auth` + `PostgREST`) while keeping local **GRDB SQLite** as the offline-first source of truth.

#### 6.1 Client Initialization & Authentication Flows
* **SDK**: `supabase-community/supabase-swift` (`SupabaseClient`).
* **Supported Auth Providers**:
  1. **Sign in with Apple (Native iOS Requirement)**: Uses `AuthenticationServices` (`ASAuthorizationAppleIDProvider`) to request an `identityToken` and exchanges it directly with Supabase via `supabase.auth.signInWithIdToken(credentials: .init(provider: .apple, idToken: token))`.
  2. **Google Sign-In**: Exchanges the native Google ID token via `supabase.auth.signInWithIdToken(credentials: .init(provider: .google, idToken: googleIdToken))` (matching [`AuthViewModel.kt`](file:///home/zenmi/Projects/manga-dl-android/app/src/main/kotlin/com/mangadl/android/ui/viewmodels/AuthViewModel.kt#L94-L97)).
  3. **Email & Password**:
     * Sign-in: `supabase.auth.signIn(email: email, password: password)`
     * Sign-up: Passes `data: ["username": .string(username)]` in `user_metadata` so the user's Hunter handle is persisted immediately.
     * Password Reset: `supabase.auth.resetPasswordForEmail(email)`
  4. **Profile Update & Self-Deletion**:
     * Profile metadata (`username`, `bio`) is updated via `supabase.auth.update(user: UserAttributes(data: ["username": .string(name), "bio": .string(bio)]))` (matching [`AccountSettingsViewModel.kt`](file:///home/zenmi/Projects/manga-dl-android/app/src/main/kotlin/com/mangadl/android/ui/viewmodels/AccountSettingsViewModel.kt#L34-L39)).
     * Account deletion (App Store Mandate): Sends an authenticated `DELETE` request to `${SUPABASE_URL}/auth/v1/user` with `Authorization: Bearer <access_token>` and `apikey: <SUPABASE_ANON_KEY>`, then wipes local GRDB tables.

#### 6.2 Remote PostgreSQL Tables & Data Contracts (What iOS Pulls & Pushes)

| Supabase Table | Primary / Composite Key | Direction | Purpose & Payload |
| :--- | :--- | :--- | :--- |
| **`subscriptions`** | `id` (`"{provider}/{mangaId}"`) + `user_id` | **Pull & Push (Upsert / Delete)** | Primary table for bookmarked manga & novels (`id`, `user_id`, `provider`, `manga_id`, `title`, `cover_url`, `type` [`"manga"` \| `"novel"`], `added_at`). |
| **`read_tracking`** | Composite (`user_id`, `provider`, `manga_id`) | **Pull & Push (Upsert)** | Stores completed chapters per series as a `jsonb` string array (`chapter_ids: ["ch-1", "ch-2", ...]`) and `updated_at`. |
| **`reading_progress`** | Composite (`user_id`, `provider`, `manga_id`, `chapter_id`) | **Push (Upsert) & Pull** | Granular page-level progress (`last_page`, `manga_title`, `chapter_title`, `updated_at`) for resuming mid-chapter across devices. |
| **`manga`** *(Legacy)* | `id` (`"{provider}:{mangaId}:{userId}"`) | **Pull (Fallback) & Push** | Legacy compatibility table (`provider_manga_id`, `subscribed`, `url`, `last_synced`). Written alongside `subscriptions` and queried as a fallback if `subscriptions` is empty. |

#### 6.3 Swift `Codable` Models (1:1 Match with `SupabaseModels.kt`)
```swift
import Foundation

struct SupabaseSubscriptionRecord: Codable {
    let id: String              // "\(provider)/\(mangaId)"
    let userId: String          // UUID string
    let provider: String
    let mangaId: String
    let title: String
    let coverUrl: String?
    let type: String            // "manga" or "novel"
    let addedAt: String?        // ISO-8601 UTC string

    enum CodingKeys: String, CodingKey {
        case id, provider, title, type
        case userId = "user_id"
        case mangaId = "manga_id"
        case coverUrl = "cover_url"
        case addedAt = "added_at"
    }
}

struct SupabaseReadTrackingRecord: Codable {
    let userId: String
    let provider: String
    let mangaId: String
    let chapterIds: [String]
    let updatedAt: String?

    enum CodingKeys: String, CodingKey {
        case provider
        case userId = "user_id"
        case mangaId = "manga_id"
        case chapterIds = "chapter_ids"
        case updatedAt = "updated_at"
    }
}

struct SupabaseReadingProgressRecord: Codable {
    let userId: String
    let provider: String
    let mangaId: String
    let chapterId: String
    let lastPage: Int
    let mangaTitle: String?
    let chapterTitle: String?
    let updatedAt: String?

    enum CodingKeys: String, CodingKey {
        case provider
        case userId = "user_id"
        case mangaId = "manga_id"
        case chapterId = "chapter_id"
        case lastPage = "last_page"
        case mangaTitle = "manga_title"
        case chapterTitle = "chapter_title"
        case updatedAt = "updated_at"
    }
}

struct SupabaseMangaRecord: Codable {
    let id: String              // "\(provider):\(mangaId):\(userId)"
    let provider: String
    let providerMangaId: String
    let title: String
    let coverUrl: String?
    let description: String?
    let status: String?
    let url: String
    let subscribed: Bool
    let userId: String?
    let lastSynced: String?

    enum CodingKeys: String, CodingKey {
        case id, provider, title, description, status, url, subscribed
        case providerMangaId = "provider_manga_id"
        case coverUrl = "cover_url"
        case userId = "user_id"
        case lastSynced = "last_synced"
    }
}
```

#### 6.4 Detailed Pull & Push Operations (`SupabaseSyncEngine`)

1. **Pulling from Cloud (`pullFromCloud()`)**:
   * **Step 1 — Pull Library (`pullLibrary(userId)`)**:
     * Queries `supabase.from("subscriptions").select().eq("user_id", value: userId)`.
     * If non-empty, iterates through all remote records and inserts any series missing from local GRDB `library`.
     * If `subscriptions` returns empty, falls back to querying `supabase.from("manga").select().eq("user_id", value: userId).eq("subscribed", value: true)`.
   * **Step 2 — Pull Read Tracking (`pullReadTracking(userId)`)**:
     * Queries `supabase.from("read_tracking").select().eq("user_id", value: userId)`.
     * For every `chapterId` inside `entry.chapterIds`, upserts a row in local GRDB `reading_progress` with `completed = true` (preserving existing `page`/`totalPages` if already present).

2. **Pushing to Cloud (`pushToCloud()`)**:
   * **Step 1 — Push All Library (`pushAllLibrary(userId)`)**:
     * Reads all local `library` rows from GRDB.
     * Upserts each entry to `subscriptions` (with `type = isNovelSource(provider) ? "novel" : "manga"`) AND upserts the corresponding fallback row to `manga`.
   * **Step 2 — Push All Read Tracking (`pushAllReadTracking(userId)`)**:
     * Queries up to 1,000 recent completed chapters from GRDB `reading_progress`, groups them by `(provider, mangaId)`, deduplicates `chapterId`s, and upserts the consolidated `SupabaseReadTrackingRecord` to `read_tracking`.

3. **Real-Time Incremental Event Triggers**:
   * **On Library Bookmark / Unbookmark (`syncMangaSubscription(manga, subscribed)`)**:
     * If `subscribed == true`: Upserts `SupabaseSubscriptionRecord` to `subscriptions` and `SupabaseMangaRecord(subscribed: true)` to `manga`.
     * If `subscribed == false`: Executes `DELETE FROM subscriptions WHERE user_id = :userId AND id = :subId` and upserts `SupabaseMangaRecord(subscribed: false)` to `manga`.
   * **On Chapter Page Turn / Completion (`syncChapterRead(...)`)**:
     * Always upserts `SupabaseReadingProgressRecord` to `reading_progress` with `last_page = page`.
     * When `isCompleted == true`: Fetches existing `chapter_ids` for `(user_id, provider, manga_id)` from `read_tracking`, merges `chapterId` into a `Set<String>`, and upserts the updated array back to `read_tracking`.
   * **On Full Bidirectional Sync (`syncAll()`)**:
     * Triggered on app launch (if authenticated), immediately after Sign-In / Sign-Up / Library Import, and on manual Pull-to-Refresh: runs `pullLibrary` → `pullReadTracking` → `pushAllLibrary` → `pushAllReadTracking`.

#### 6.5 Conflict Resolution & Offline Resilience
* **Union Merge for Completed Chapters**: Once a chapter ID is added to `read_tracking.chapter_ids` on any device, it is merged via set union (`Set.union`) so syncing never accidentally marks a finished chapter as unread.
* **Non-Destructive Library Pull + Explicit Remote Delete**: Pulling from cloud only inserts missing titles into GRDB without overwriting local reading counters or shelf assignments; removing a title from the library while online issues an explicit `DELETE` against `subscriptions`.
* **Offline-First Execution**: All sync methods run asynchronously in a background `Task` using Swift `Result` / `do-catch` blocks. Network failures never block the UI or roll back local SQLite writes.

---

## 7. Recommended Project Layout & SPM Dependencies

### Swift Package Manager Dependencies
```swift
// Package.swift / Xcode SPM Configuration
dependencies: [
    // Database
    .package(url: "https://github.com/groue/GRDB.swift.git", from: "6.29.0"),
    
    // HTML Parsing (Jsoup port)
    .package(url: "https://github.com/scinfu/SwiftSoup.git", from: "2.7.2"),
    
    // Image Pipeline & Caching
    .package(url: "https://github.com/kean/Nuke.git", from: "12.8.0"),
    
    // CBZ & EPUB Archive Management
    .package(url: "https://github.com/weichsel/ZIPFoundation.git", from: "0.9.19"),
    
    // Cloud Sync & Auth
    .package(url: "https://github.com/supabase-community/supabase-swift.git", from: "2.14.0"),
    
    // Tachiyomi Protobuf Backups
    .package(url: "https://github.com/apple/swift-protobuf.git", from: "1.26.0")
]
```

### Xcode Project Structure
```
manga-dl-ios/
├── App/
│   ├── MangaDlApp.swift
│   ├── Navigation/
│   │   ├── AppRoute.swift
│   │   └── AppTab.swift
│   └── Configuration/
│       └── AppPreferences.swift
├── Core/
│   ├── Network/
│   │   ├── HTTPClient.swift
│   │   └── UserAgentInterceptor.swift
│   ├── Database/
│   │   ├── AppDatabase.swift
│   │   ├── Records/ (Library, Categories, Progress, Downloads)
│   │   └── Migrations/
│   ├── Sync/
│   │   ├── SupabaseManager.swift
│   │   ├── SupabaseModels.swift
│   │   └── SupabaseSyncEngine.swift
│   └── Archiving/
│       ├── CBZBuilder.swift
│       ├── EPUBBuilder.swift
│       └── ArchiveInspector.swift
├── Sources/
│   ├── Base/ (MangaSource.swift, NovelSource.swift)
│   ├── Manga/ (MangaDex, AsuraScans, MangaKakalot, etc.)
│   ├── Novel/ (RoyalRoad, NovelBin, Ranobes, etc.)
│   └── Descramblers/ (AsuraCanvasDescrambler.swift)
├── Features/
│   ├── Library/ (LibraryView, ShelvesSheet, FilterChips)
│   ├── Browse/ (BrowseView, SourcesDirectory, GlobalSearch)
│   ├── Detail/ (MangaDetailView, ChapterListView)
│   ├── Reader/
│   │   ├── Manga/ (MangaReaderView, AmbilightHalo, SpreadPager)
│   │   └── Novel/ (NovelReaderView, TypographySheet, TTSEngine)
│   ├── Downloads/ (DownloadQueueView, DownloadWorker)
│   ├── Social/ (CommentsSheetView, LeaderboardView)
│   ├── Profile/ (ProfileView, HunterRankCard, MilestoneGrid)
│   └── Settings/ (SettingsView, TrackerSettingsView, SecurityView)
└── Resources/
    ├── Assets.xcassets
    └── Info.plist
```

---

## 8. Phased Implementation Roadmap

```
Phase 1: Foundation & Data Stack
├── Set up Xcode project (Swift 6, iOS 16+) with SPM dependencies
├── Implement GRDB AppDatabase schema (Library, Categories, Progress, Downloads)
└── Create URLSession HTTP client with desktop User-Agent spoofing

Phase 2: Native Sources & Parsing
├── Implement MangaSource & NovelSource protocols with SwiftSoup
├── Port primary Manga engines (MangaDex, Asura Scans + Descrambler)
├── Port primary Novel engines (Royal Road, NovelBin, Ranobes)
└── Build GlobalSearch stream aggregator

Phase 3: Reader Engines & Ambilight
├── Build MangaReaderView (LTR, RTL, Webtoon, iPad Landscape Dual Spread)
├── Implement Metal-accelerated Ambilight halo backdrop
├── Build NovelReaderView with dynamic typography (fonts, spacing, themes)
└── Wire AVSpeechSynthesizer for background audio TTS

Phase 4: Library Shelves & Downloads
├── Implement Library grid with shelf chips & Category assignment sheet
├── Build CBZ packaging (ZIPFoundation + ComicInfo.xml)
├── Build EPUB 3 packaging for novel downloads
└── Expose downloads directory to iOS Files.app

Phase 5: Cloud Sync, Trackers & Gamification
├── Implement Supabase Swift Auth & background Postgres sync
├── Add AniList & MyAnimeList OAuth via ASWebAuthenticationSession
├── Implement Hunter Rank EXP engine & milestone unlock tracker
└── Wire Chapter Comments sheet & Global Guild Leaderboard API

Phase 6: Polish, Backup & iOS Integration
├── Implement LocalAuthentication (Face ID / Touch ID lock)
├── Add .tachibk (SwiftProtobuf) & .mangadl backup import/export
├── Optimize memory footprint for continuous reading on low-spec iPhones
└── Final App Store compliance review & TestFlight build
```
