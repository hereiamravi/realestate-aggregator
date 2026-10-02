Mock backend for RealEstate Aggregator

This mock server serves a minimal subset of the API to allow the Android sample app to run against local data.

How to run
1. Install Node.js (v14+) and npm locally.
2. From the project root, cd mock
3. Install dependencies:
   npm install
4. Start the server:
   npm start

By default the server listens on http://localhost:8080

Android emulator note
- If you run the Android sample on the emulator, use baseUrl = "http://10.0.2.2:8080/v1/" in MainActivity (this is already set in the sample MainActivity).
- If testing on a real device, use your machine's local IP (e.g., http://192.168.1.100:8080/v1/) and ensure the device can reach that IP.

Endpoints
- GET /v1/feed
  - Returns a PostListResponse JSON (supports page_size + cursor offset params).
- GET /v1/posts/{id}
  - Returns a single post by id or instagram_post_id.
- GET /v1/channels
  - Returns a list of channels inferred from posts.
- GET /v1/bookmarks
  - Returns paginated mock bookmarks (supports page_size + cursor).
- POST /v1/bookmarks { post_id }
  - Creates a mock bookmark (201).
- DELETE /v1/bookmarks/{id}
  - Deletes a mock bookmark by bookmark id or post_id (204).

Files
- mock/server.js - Express server
- mock/data/feed.json - Sample feed data used by the server

Notes
- This is a simple mock for development only. Replace with the real backend when available.
