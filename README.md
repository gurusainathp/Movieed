# 🎬 Movieed — Personal Movie & TV Journal

**Movieed** is a modern, native Java Android application built for cinema enthusiasts to track movies and TV shows, log personal emoji reactions, discover streaming providers, and explore personalized recommendations. Built following clean MVVM architecture, Room Database (v3), Retrofit TMDB Integration, and Material Design 3.

---

## 🌟 Key Features

### 1. 5-Tier Emoji Reaction System
Replaced legacy star ratings and text reviews with an intuitive 5-tier reaction classification:
- 😍 **Love**: Strong positive preference (`+3.0` algorithm weight)
- 🙂 **Like**: Moderate positive preference (`+1.5` algorithm weight)
- 😐 **Umm**: Neutral reaction (`0.0` algorithm weight)
- 🙁 **Dislike**: Negative preference (`-1.5` algorithm weight)
- 😡 **Hate**: Strong negative preference (`-3.0` algorithm weight)

### 2. Independent Watchlist & Reaction Tracking
- Watchlist state (`isWatchlist`) and Reaction state (`reaction`) operate 100% independently.
- Rate a film without removing it from Watchlist, or add a previously reacted film to Watchlist.
- Full reaction editing and toggle-off support.

### 3. Redesigned Tabbed Diary Grid
- **4-Across Edge-to-Edge Poster Grid**: Flush 2:3 theatrical aspect ratio poster cards with top-right overlay badges (reaction emoji or `+` watchlist indicator).
- **ViewPager2 + TabLayout Integration**: Independent **Watchlist** (left) and **Watched** (right) tabs with smooth swipe gestures.
- **Swipeable Filter Bars**:
  - **Watched Tab**: Single unified swipeable filter bar combining reactions (All, 😍, 🙂, 😐, 🙁, 😡) and local streaming providers.
  - **Watchlist Tab**: Horizontal swipeable provider filter bar showing all supported streaming services with custom empty state messages.
- **Gesture Conflict Resolution**: Built-in `NestedHorizontalScrollView` with touch slop disallow intercept ensures smooth horizontal filter scrolling without accidental tab switching.

### 4. Watch Providers (India - `IN`)
- Displays streaming, rental, and purchase availability for India directly on the Movie Details header.
- **Deduplicated & Grouped**: Shows each provider brand once per category (Subscription vs Paid).
- **Subtle Extension Badges**: Sleek bottom border extension tags (`BUY`, `RENT`, `BUY/RENT`) with high-contrast text.
- **Custom Vector Drawables**: High-resolution, standardized 16:10 vector logos for all major services (Netflix, Prime Video, JioHotstar, SonyLIV, ZEE5, Apple TV+, MX Player, YouTube Premium, Crunchyroll, Lionsgate+, Paramount+, Plex, Tubi, MUBI).

### 5. Smart Multi-Seed Recommendation Engine
- **Hybrid Architecture**: Combines TMDB's remote recommendation endpoints with client-side local intelligence.
- **Reaction Weighting**: Calculates user taste profiles by weighting reaction history.
- **Multi-Seed Aggregation**: Samples 3–5 random seed movies across the user's top-loved films simultaneously and awards overlap bonuses (`+3.0` per additional seed match).
- **Decoupled Director Pool**: Aggregates unique directors across history into an independent pool and samples 2–3 directors for filmography recommendations.
- **Exclusion Guardrail**: Automatically filters out any movie already watched, logged, or in Watchlist.
- **100% Strict Non-Consecutive Interleaving**: Category-based round-robin interleaver (`MOVIE`, `DIRECTOR`, `CLASSIC`, `TRENDING`) ensures adjacent cards in the Explore reel never share the same recommendation type.
- **Dynamic Seed Rotation**: Prefetches fresh seed combinations every 10–15 scrolls.

### 6. Cast, Crew & Person Details
- Full actor and director details page (`PersonDetailsActivity`).
- Expandable biography ("Read More").
- Top 5 "KNOWN FOR" works.
- Reverse-chronologically sorted filmography with character roles and gold TMDB star ratings (`★ 8.5`).

---

## 🏗 Architecture & Tech Stack

Movieed follows modern Android development practices and the official **MVVM (Model-View-ViewModel)** architectural pattern.

- **Language**: Java 17 (Min SDK 26, Target SDK 36)
- **Architecture**: MVVM + LiveData + Repository Pattern
- **Database**: Room Database (Version 3)
  - `MovieEntity`: Stores cached metadata keyed by `tmdbId`.
  - `MovieLogEntity`: Stores user reaction, watchlist state, and timestamps.
  - `MovieLogWithDetails`: Room `@Relation` POJO joining log and movie.
- **Network**: Retrofit 2 + OkHttp 4 + Gson
  - TMDB v3 REST API (`search/multi`, `movie/{id}`, `tv/{id}`, `credits`, `watch/providers`, `recommendations`, `top_rated`, `popular`, `now_playing`).
- **UI Components**:
  - Material Design 3 components
  - `ViewPager2` + `TabLayoutMediator`
  - `RecyclerView` with `GridLayoutManager` & `LinearLayoutManager`
  - Custom `NestedHorizontalScrollView`
  - Glide image loading pipeline

---

## 📁 Project Structure

```
com.gurusainathp.movieed/
├── data/
│   ├── local/
│   │   ├── dao/             # MovieDao, MovieLogDao
│   │   ├── database/        # AppDatabase (Room v3)
│   │   └── entity/          # MovieEntity, MovieLogEntity, MovieLogWithDetails
│   ├── remote/
│   │   ├── api/             # TmdbApiService, TmdbClient
│   │   └── dto/             # TMDB DTOs & WatchProviderItem
│   └── repository/          # MovieRepository
├── domain/
│   ├── model/               # Movie, RecommendedMovie
│   └── recommendation/      # RecommendationEngine
└── ui/
    ├── diary/               # DiaryFragment, DiaryTabFragment, DiaryAdapter, NestedHorizontalScrollView
    ├── explore/             # ExploreFragment, ExploreViewModel, ExploreAdapter
    ├── home/                # HomeFragment, HomeViewModel, HomeMovieAdapter, HomeRecentLogAdapter
    ├── movie/               # MovieDetailsActivity, MovieDetailsViewModel, ReactionPickerBottomSheetDialogFragment, ProviderAdapter, LocalProviderMapper
    ├── person/              # PersonDetailsActivity, PersonViewModel, PersonFilmographyAdapter
    ├── profile/             # ProfileFragment, ProfileViewModel
    └── search/              # SearchFragment, SearchViewModel, SearchMovieAdapter
```

---

## 🛠 Setup & Installation

### Prerequisites
- Android Studio Ladybug (2024.2) or newer
- JDK 17
- Android Device or Emulator (API 26+)
- TMDB API Key (v3)

### Configuration

1. **Clone Repository**:
   ```bash
   git clone https://github.com/gurusainathp/Movieed.git
   cd Movieed
   ```

2. **Add TMDB API Key**:
   Open `local.properties` in the project root and add your TMDB v3 API key:
   ```properties
   TMDB_API_KEY=your_tmdb_api_key_here
   ```

3. **Build & Run**:
   Compile and install the debug APK via Gradle wrapper:
   ```bash
   .\gradlew.bat installDebug
   ```

---

## 📄 License

Copyright © 2026 Guru Sainath P. All rights reserved.
