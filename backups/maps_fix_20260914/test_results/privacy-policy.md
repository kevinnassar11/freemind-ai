# Privacy Policy for FreeMind AI

**Last updated:** September 14, 2026

FreeMind AI ("the app") is a place-search app that lets you search locations, save favorite places, and keep a private history of your searches. This policy explains what data we collect and how it's used.

## Data We Collect

**Account information** (via Google Sign-In / Firebase Authentication):
- Email address
- Display name
- Profile photo URL

**App data you generate** (stored in our backend, tied to your account):
- Places you search for, the search mode used (prompt/cities/countries/places), and the top result (`place_search_history`)
- Places you mark as favorites, including their name, address, and coordinates (`favorite_places`)
- Your theme preference (light/dark/system)
- A device push-notification token, if notifications are enabled

**We do not collect:**
- Your precise real-time location (search is text-based, not GPS-driven)
- Payment information (the app has no purchases)
- Any data from third parties about you

## How We Use Your Data

- To show your search history and favorites back to you, scoped only to your own account
- To sign you in and keep your session secure
- To send optional update/notification messages, if you've enabled them

We do not sell your data, and we do not share it with third parties for advertising.

## Where Your Data Is Stored

App data is stored in a Supabase-hosted PostgreSQL database with row-level security enabled — every query is scoped to your own authenticated user ID, so no other user can read or modify your data. Authentication is handled by Firebase Authentication (Google).

## Third-Party Services

- **Google Sign-In / Firebase Authentication** — for account sign-in
- **Google Maps SDK / Places API** — to render maps and search for places
- **Supabase** — to store your search history, favorites, and profile

Each of these services has its own privacy policy governing data they process on their platforms.

## Data Retention & Deletion

You can delete your account and all associated data at any time from **Settings → Delete account** in the app. This permanently removes your profile, search history, and favorite places. This action cannot be undone.

If you'd prefer to request deletion without using the app, see our [Account Deletion](account-deletion.md) page.

## Children's Privacy

FreeMind AI is not directed at children under 13, and we do not knowingly collect data from children under 13.

## Changes to This Policy

We may update this policy from time to time. Material changes will be reflected by updating the "Last updated" date above.

## Contact

Questions about this policy? Contact: [your support email here]
