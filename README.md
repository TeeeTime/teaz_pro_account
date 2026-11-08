# teaz_pro_account
Teaz Account, at `https://account.teaz.pro`: the account of a Teaz user.

- **Profile picture:** upload, crop, remove. Account owns the pictures for
  the whole ecosystem and serves them to every app.
- **Username, password, 2FA:** links to Keycloak's own pages, in the Teaz
  login theme. No account data lives here.
- **Wiki MCP:** disconnects AI assistants from the wiki on all devices.

Monorepo: `frontend/` (Vue 3 + TypeScript) and `backend/` (Spring Boot, `pro.teaz:account`).

Stack, conventions, style guide and color tokens shared by every Teaz app
live in **`teaz_pro`**: start with `docs/TECH_STACK.md` there. This
README only covers what is specific to this app.

## Running locally

```sh
cd backend && ./mvnw spring-boot:run
cd frontend && npm run dev
```

Or use the IntelliJ run configs in `.idea/runConfigurations/`:
**Backend (Spring Boot)**, **Frontend (npm run dev)** and
**Run All (Frontend + Backend)**. Locally, auth is mocked on both sides, so
no Keycloak is needed. The Vite dev server forwards `/api/*` to the backend
on `localhost:8080`, the same routing as in production. Pictures land in
`backend/data/` (gitignored).

## Profile pictures

| Endpoint | Auth | What |
| -------- | ---- | ---- |
| `GET /api/avatars/{sub}?size=128` | public | JPEG of 64, 128, 256 or 512 px (the smallest of at least `size`). `{sub}` is the Keycloak user id. 404 without a picture. |
| `PUT /api/me/avatar` | Bearer | Multipart field `file`: PNG, JPEG or GIF up to 5 MB and 16 megapixels. Replaces the caller's picture. |
| `DELETE /api/me/avatar` | Bearer | Removes the caller's picture. |

- **For other apps:** `<Avatar>` and `TopBar` from `teaz_pro_ui` take the URL
  from `avatarUrl(user.id, size)`. See `teaz_pro/docs/TECH_STACK.md`.
- **Linked to Keycloak by id only:** a picture is stored under the user's
  `sub`, so Keycloak isn't written to and this app holds no admin credentials.
- **Public on purpose:** an `<img>` can't send a Bearer token, and public
  wikis show them to guests. The id is a random UUID, not a name.
- **Uploads are never served as they came:** `AvatarImage` decodes them and
  writes new JPEGs. This drops EXIF data such as GPS positions. The browser
  crops first (`CropDialog.vue`), which also handles WebP and phone rotation.
- **Caching:** `max-age=300` with an ETag, 60 seconds for a 404. After a
  change, other apps show the new picture within about five minutes.
- **Storage:** `<storage-dir>/<sub>/<size>.jpg`. There is no database. In
  production that's the Docker volume `files`: **back it up**, it holds the
  only copy.
- A picture stays behind when the user is deleted in Keycloak, as this app
  isn't told about it.

## Account changes in Keycloak

Each button runs `keycloak.login({ action })`, which sends Keycloak's
`kc_action` parameter: Keycloak shows the page, then redirects back with the
outcome (`onActionUpdate`).

| Button | `kc_action` |
| ------ | ----------- |
| Username and profile | `UPDATE_PROFILE` |
| Password | `UPDATE_PASSWORD` |
| Set up 2FA | `CONFIGURE_TOTP` |

Removing a 2FA device has no `kc_action` without knowing the device, so it's
a link to the Account Console, which isn't in the Teaz theme.

**Disconnect Wiki MCP** calls
`DELETE /realms/teazpro/account/applications/wiki-mcp/consent` on Keycloak's
Account REST API, with the user's own token. It revokes the consent and all
offline tokens of `wiki-mcp` on every device and leaves the browser logged
in. There is no per-device variant: ending one session (`/account/sessions/{id}`,
or the Admin API with `isOffline=true`) also ends the browser session it came
from. Access tokens already issued stay valid until they expire.

## Shared UI

`TopBar`, `useTheme()`, the theme tokens and every other shared component
come from **`teaz_pro_ui`** (`@teeetime/teaz-pro-ui`, pinned to a tag in
`frontend/package.json`). See `teaz_pro/docs/UI_LIBRARY.md`.

## Auth

- Frontend `frontend/src/auth/`: Keycloak (`.env.production`: `auth.teaz.pro`,
  realm `teazpro`, client `teaz-account`) in builds, mock user in `npm run dev`.
- Backend `backend/src/main/java/pro/teaz/account/security/`: Bearer JWT
  required with the `prod` profile, except for reading pictures; everything
  permitted otherwise. Any realm user may use Account, no extra role.

Keycloak setup (realm `teazpro`):

1. Client `teaz-account`, as in the Auth section of `teaz_pro/docs/TECH_STACK.md`.
   Unlike other apps, the id isn't the app's name: every realm already has a
   built-in client `account` (Keycloak's Account Console and REST API).
2. **Realm settings → Login → Edit username** on, otherwise `UPDATE_PROFILE`
   doesn't offer the username.
3. **Authentication → Required actions:** `Update Password`, `Configure OTP`
   and `Update Profile` enabled.

Users need the default `account` role `manage-account`, which also gives
the token the `account` audience the Account REST API asks for.

## Deployment

On the VPS:

```sh
git pull && docker compose up -d --build
```

`compose.yml` routes `account.teaz.pro` to the frontend (nginx) and
`account.teaz.pro/api/*` to the backend, via the shared Traefik
(network `proxy`, cert resolver `le`). Backend endpoints are therefore
mapped under `/api`.
