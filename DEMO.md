# RealEstate Aggregator — Demo Instructions

How to run the end-to-end local demo: mock backend + sample Android app.
No credentials needed for the demo (the app uses a placeholder API key against local mock data).

## 1. Prerequisites

- Node.js 18+ LTS and npm
- Android Studio (bundled Gradle is fine) with:
  - SDK `platforms;android-34`, `build-tools;34.0.0`
  - Java 17
- An emulator (e.g. Pixel, API 34) **or** a physical Android device on the same Wi-Fi as your PC

## 2. Start the mock backend (PowerShell)

```powershell
cd D:\Work\Explore\POC\realestate-aggregator\mock
npm install
npm start
```

The server listens on `http://localhost:8080`.

Verify in a second terminal:

```powershell
curl "http://localhost:8080/v1/feed?page_size=2"
```

Expected: the 2 mock posts (`lakeview-plot`, `downtown-2bhk`) with `next_cursor: null`.

Available mock endpoints:

| Endpoint | Behaviour |
|---|---|
| `GET /v1/feed?page_size=&cursor=` | Cursor-offset paginated `PostListResponse` |
| `GET /v1/posts/{id}` | Single post by `id` or `instagram_post_id` |
| `GET /v1/channels` | Channels inferred from posts |
| `GET /v1/bookmarks` | Paginated mock bookmarks |
| `POST /v1/bookmarks { post_id }` | Creates a mock bookmark (201) |
| `DELETE /v1/bookmarks/{id}` | Deletes by bookmark id or `post_id` (204) |

## 3. Run the Android app

1. Launch your emulator (or connect the physical device).
2. Open the project folder `realestate-aggregator` in Android Studio and let Gradle sync.
   (The repo has no `gradlew` wrapper yet — Android Studio's bundled Gradle works for the demo.
   To add one: `gradle wrapper --gradle-version 8.4.1`.)
3. Run the `app` module.
4. Base URL notes:
   - **Emulator (default, already set in `MainActivity`):** `http://10.0.2.2:8080/v1/`
     (`10.0.2.2` is the emulator's alias for your PC's localhost; the `/v1/` prefix is required
     because the SDK uses relative paths per `openapi.yaml`'s server URL.)
   - **Physical device:** replace with `http://<your-PC-LAN-IP>:8080/v1/`
     (find the IP via `ipconfig`), keep phone + PC on the same Wi-Fi, and allow Node
     through Windows Firewall. Alternative: `adb reverse tcp:8080 tcp:8080`, then use
     `http://localhost:8080/v1/`.

## 4. What you should see

- 2 feed cards, each with thumbnail image, caption, and `property_type | price`.
- Tap a card → opens the Instagram app if installed, else the web fallback
  (`instagram://media?id=…` → `https://www.instagram.com/p/{shortcode}/` → `original_url`).
- Tap the star icon → bookmark; bookmarks persist across app restarts (Room).
- Feed is cached locally, so the last loaded feed still shows offline.
- Scrolling to the bottom loads nothing further (only 2 mock items, `next_cursor` is null —
  correct behaviour, no endless spinner).

## 5. Troubleshooting

| Symptom | Check |
|---|---|
| Empty feed | Is the mock server running? `curl` the `/v1/feed` URL from step 2. |
| Connection refused (emulator) | Use `10.0.2.2`, not `localhost` (emulator localhost is the emulator itself). |
| 404 on `/feed` | Base URL must end with `/v1/` (SDK paths are relative). |
| Images don't load | Emulator needs internet access (thumbnails come from `picsum.photos`). |
| No network at all | `INTERNET` permission + `usesCleartextTraffic` are set in the sample manifest; check `adb logcat` for the HTTP call. |
| Gradle sync fails | Install SDK `platforms;android-34` + `build-tools;34.0.0`, use Java 17. |

## 6. After the demo

The demo uses local mock data. The agreed next step is a **third-party provider**
(e.g. Apify Instagram actor) behind a backend fetcher:

1. Pick the provider and copy its API token into a backend `.env` as `PROVIDER_TOKEN`
   (never into the app).
2. Implement the fetcher mapping provider JSON → the `Post` schema
   (`migrations/001_create_schema.sql` is ready; runs logged to `fetch_logs`).
3. Point the app at the real backend URL instead of the mock.
