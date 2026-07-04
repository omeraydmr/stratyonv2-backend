# API Access Setup Guide

Before running the backend you must obtain credentials from three external platforms. Follow each section below in order. Start with **Google Ads** first since its approval can take 2–5 business days.

---

## 1. Google Ads API

### What you need
- `GOOGLE_ADS_DEVELOPER_TOKEN` — 22-character string
- `GOOGLE_ADS_CLIENT_ID` — OAuth 2.0 client ID
- `GOOGLE_ADS_CLIENT_SECRET` — OAuth 2.0 client secret
- `GOOGLE_ADS_REFRESH_TOKEN` — long-lived OAuth refresh token
- `GOOGLE_ADS_CUSTOMER_ID` — your 10-digit Ads account ID (no dashes)

### Steps

**Step 1 — Apply for a Developer Token**
1. Log into [ads.google.com](https://ads.google.com) with a **Manager (MCC) account**. If you don't have one, create one at ads.google.com/home/tools/manager-accounts.
2. Go to **Tools & Settings → Setup → API Center**.
3. Fill in the API Access form (contact email, use case description).
4. Accept the Terms and Conditions and submit.
5. You will receive an **Explorer-level** token immediately. Standard Access (needed for production) is reviewed in 2–5 days.
6. Copy the developer token — it looks like `AbCdEfGhIjKlMnOpQrSt12`.

**Step 2 — Enable the API in Google Cloud Console**
1. Go to [console.cloud.google.com](https://console.cloud.google.com) → create or select a project.
2. **APIs & Services → Library** → search "Google Ads API" → **Enable**.
3. **APIs & Services → Credentials → + Create Credentials → OAuth client ID**.
4. Application type: **Desktop app** (for backend server-to-server use).
5. Download or copy the **Client ID** and **Client Secret**.

**Step 3 — Get a Refresh Token**
1. Open [developers.google.com/oauthplayground](https://developers.google.com/oauthplayground).
2. Click the gear icon (top right) → check **"Use your own OAuth credentials"** → paste your Client ID and Secret.
3. In the left panel under **Google Ads API**, check the scope `https://www.googleapis.com/auth/adwords`.
4. Click **"Authorize APIs"** → sign in → allow.
5. Click **"Exchange authorization code for tokens"**.
6. Copy the **Refresh token** value.

**Step 4 — Get your Customer ID**
1. In Google Ads, your 10-digit Customer ID is shown in the top-right corner (format: `123-456-7890`).
2. Remove dashes: `1234567890`.

---

## 2. Google Analytics 4 Data API

### What you need
- `GA4_SERVICE_ACCOUNT_JSON` — absolute path to the downloaded JSON key file
- `GA4_PROPERTY_ID` — numeric GA4 property ID (e.g. `123456789`)

### Steps

**Step 1 — Enable the API**
1. In [console.cloud.google.com](https://console.cloud.google.com) (same project as above).
2. **APIs & Services → Library** → search "Google Analytics Data API" → **Enable**.

**Step 2 — Create a Service Account**
1. **APIs & Services → Credentials → + Create Credentials → Service Account**.
2. Name it (e.g. `ga4-reader`), click **Create and Continue**, skip optional steps, **Done**.
3. Click the new service account → **Keys tab → Add Key → Create new key → JSON → Create**.
4. A `.json` file downloads automatically. Save it securely (e.g. `secrets/ga4-service-account.json`).
5. Note the service account email — it looks like `ga4-reader@your-project.iam.gserviceaccount.com`.

**Step 3 — Grant access in Google Analytics**
1. Go to [analytics.google.com](https://analytics.google.com) → select your GA4 property.
2. **Admin (bottom left) → Account Access Management → + → Add users**.
3. Paste the service account email, assign role **Viewer**, click **Add**.

**Step 4 — Find your Property ID**
1. In GA4 Admin → **Property Settings** → copy the numeric **Property ID** (not the measurement ID starting with G-).

---

## 3. Meta Marketing API (Facebook / Instagram Ads)

### What you need
- `META_ACCESS_TOKEN` — System User access token (non-expiring)
- `META_AD_ACCOUNT_ID` — your ad account ID in format `act_XXXXXXXXX`

### Steps

**Step 1 — Create a Meta Developer Account & App**
1. Go to [developers.facebook.com](https://developers.facebook.com) → **Get Started** → sign in with Facebook.
2. **My Apps → Create App** → type: **Business** → fill name & contact email → **Create App**.

**Step 2 — Add the Marketing API**
1. In your app dashboard → **+ Add Product** → find **Marketing API** → **Set up**.

**Step 3 — Get Ad Account ID**
1. Go to [business.facebook.com/adsmanager](https://business.facebook.com/adsmanager).
2. Your ad account ID is shown in the top bar (format `act_123456789`).

**Step 4 — Create a System User Token (recommended for production)**
1. Go to [business.facebook.com](https://business.facebook.com) → **Settings → Users → System Users → Add**.
2. Name it, role: **Admin**, click **Create System User**.
3. Click on the system user → **Generate New Token**.
4. Select your app, enable scope `ads_read`, click **Generate Token**.
5. Copy and store the token — it does **not** expire.

> For development/testing you can use a regular User Token from **Marketing API → Tools → Access Token Generator** (expires after ~60 days).

---

## 4. Anthropic (Claude API)

### What you need
- `ANTHROPIC_API_KEY` — starts with `sk-ant-...`

### Steps
1. Go to [console.anthropic.com](https://console.anthropic.com) → sign in.
2. **API Keys → + Create Key** → name it, copy the key immediately (shown only once).

---

## 5. AWS S3

### What you need
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION` (e.g. `us-east-1`)
- `AWS_S3_BUCKET` (e.g. `stratyon-reports`)

### Steps
1. Sign into [aws.amazon.com/console](https://aws.amazon.com/console).
2. **S3 → Create bucket** → choose a unique name and region → leave defaults → **Create**.
3. **IAM → Users → Create user** → name it `stratyon-backend`.
4. **Attach policies directly** → search and attach `AmazonS3FullAccess` (or a custom policy scoped to your bucket).
5. **Security credentials tab → Create access key → Application running outside AWS → Create**.
6. Copy the Access Key ID and Secret Access Key.

---

## Summary — `.env` values to fill in

```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=stratyon
DB_USER=stratyon
DB_PASSWORD=yourpassword

# JWT
JWT_SECRET=a-random-256-bit-secret-string-here

# AWS S3
AWS_REGION=us-east-1
AWS_S3_BUCKET=stratyon-reports
AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

# Anthropic
ANTHROPIC_API_KEY=sk-ant-...

# Google Analytics 4
GA4_PROPERTY_ID=123456789
GA4_SERVICE_ACCOUNT_JSON=/absolute/path/to/ga4-service-account.json

# Google Ads
GOOGLE_ADS_DEVELOPER_TOKEN=AbCdEfGhIjKlMnOpQrSt12
GOOGLE_ADS_CLIENT_ID=123456789-abc.apps.googleusercontent.com
GOOGLE_ADS_CLIENT_SECRET=GOCSPX-...
GOOGLE_ADS_REFRESH_TOKEN=1//0e...
GOOGLE_ADS_CUSTOMER_ID=1234567890

# Meta
META_ACCESS_TOKEN=EAABwzLixnjYBO...
META_AD_ACCOUNT_ID=act_123456789
```
