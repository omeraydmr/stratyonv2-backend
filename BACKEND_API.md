# Stratyon v2 — Backend API Specification

This document is the single source of truth for the backend API contract consumed by the Stratyon frontend. Every endpoint, request shape, response shape, and enum value listed here is already wired in the frontend services (`src/services/`). Build to this spec and the frontend will work without any further changes — just flip `NEXT_PUBLIC_USE_MOCK=false` in `.env.local`.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Authentication](#2-authentication)
3. [Firms](#3-firms)
4. [Reports](#4-reports)
5. [Analytics](#5-analytics)
6. [Chatbot](#6-chatbot)
7. [Notifications](#7-notifications)
8. [Data Schemas](#8-data-schemas)
9. [Error Format](#9-error-format)
10. [Enum Reference](#10-enum-reference)

---

## 1. Overview

### Base URL

```
http://localhost:8000/api/v1
```

All paths below are relative to this base. The frontend reads `NEXT_PUBLIC_API_URL` from the environment; the default value above is the expected local development URL.

### Headers

| Header | Required | Value |
|---|---|---|
| `Content-Type` | All non-GET requests | `application/json` |
| `Authorization` | All protected endpoints | `Bearer <token>` |

The frontend automatically injects the `Authorization` header from `localStorage.auth_token` via an Axios request interceptor.

### Response Envelope

All successful responses **must** follow this envelope:

```json
{
  "data": <payload>,
  "message": "optional human-readable string"
}
```

The frontend unwraps `.data` from every response via a response interceptor. If you return the payload directly (no envelope), the frontend will still work — but adding the envelope is recommended for consistency.

### Timestamps

All `createdAt`, `updatedAt`, `completedAt`, and `lastReportDate` fields must be **ISO 8601 UTC strings** — e.g. `"2026-01-20T14:30:00Z"`.

---

## 2. Authentication

### `POST /auth/register`

Creates a new user account. Called after the registration form is submitted.

**Request body**
```json
{
  "email": "user@company.com",
  "password": "minimum8chars",
  "companyName": "Acme Technologies"
}
```

**Response `200`**
```json
{
  "data": {
    "id": "user_abc123",
    "email": "user@company.com",
    "companyName": "Acme Technologies",
    "firmId": null,
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

> `firmId` is `null` at registration time. It is set after the firm survey is submitted (`POST /firms`). The frontend stores `firmId` in `localStorage.auth_user` and attaches it to subsequent requests.

---

### `POST /auth/login`

Authenticates an existing user.

**Request body**
```json
{
  "email": "user@company.com",
  "password": "minimum8chars"
}
```

**Response `200`**
```json
{
  "data": {
    "id": "user_abc123",
    "email": "user@company.com",
    "companyName": "Acme Technologies",
    "firmId": "firm_abc123",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

---

### `POST /auth/logout`

Invalidates the current token server-side (if using token blocklisting). The frontend clears local storage regardless of the response.

**Request body** — empty

**Response `200`**
```json
{ "data": null, "message": "Logged out successfully." }
```

---

## 3. Firms

A **Firm** is the company profile created during the onboarding survey. Each authenticated user belongs to one firm. All firm endpoints are scoped to the authenticated user's firm — the backend derives `firmId` from the JWT, not from the URL.

---

### `POST /firms`

Creates the firm profile. Called at the end of the onboarding survey (step 4).

**Request body**
```json
{
  "name": "Acme Technologies Inc.",
  "industry": "technology",
  "size": "51-200",
  "goals": ["digital_transformation", "operational_efficiency", "revenue_growth"]
}
```

**Response `201`**
```json
{
  "data": {
    "id": "firm_abc123",
    "name": "Acme Technologies Inc.",
    "industry": "technology",
    "size": "51-200",
    "goals": ["digital_transformation", "operational_efficiency", "revenue_growth"],
    "createdAt": "2026-05-10T09:00:00Z",
    "updatedAt": "2026-05-10T09:00:00Z"
  }
}
```

> After this call succeeds, the frontend redirects to `/dashboard`. You should also update the user record's `firmId` at this point.

---

### `GET /firms/me`

Returns the authenticated user's firm profile.

**Response `200`**
```json
{
  "data": {
    "id": "firm_abc123",
    "name": "Acme Technologies Inc.",
    "industry": "technology",
    "size": "51-200",
    "goals": ["digital_transformation", "operational_efficiency", "revenue_growth"],
    "createdAt": "2025-11-01T00:00:00Z",
    "updatedAt": "2026-04-10T00:00:00Z"
  }
}
```

---

### `PATCH /firms/me`

Partially updates the firm profile. Called from the Settings → Edit Firm modal. Only fields included in the request body should be updated.

**Request body** (all fields optional)
```json
{
  "name": "Acme Technologies Inc.",
  "industry": "technology",
  "size": "201-1000",
  "goals": ["digital_transformation", "risk_management"]
}
```

**Response `200`** — returns the full updated firm object (same shape as `GET /firms/me`).

---

### `GET /firms/me/metrics`

Returns computed KPI metrics for the firm's dashboard. This is the data-heavy endpoint — your backend generates these numbers from the firm's report history.

**Response `200`**
```json
{
  "data": {
    "overallScore": 76,
    "trend": "up",
    "trendPercent": 8.4,
    "reportCount": 3,
    "lastReportDate": "2026-04-28T08:00:00Z",
    "kpis": [
      {
        "label": "Overall Score",
        "value": 76,
        "unit": "/100",
        "change": 8.4,
        "status": "good"
      },
      {
        "label": "Reports Completed",
        "value": 2,
        "change": null,
        "status": "good"
      },
      {
        "label": "Risk Level",
        "value": "Medium",
        "change": null,
        "status": "warning"
      },
      {
        "label": "Efficiency Index",
        "value": 83,
        "unit": "%",
        "change": 5.2,
        "status": "good"
      }
    ]
  }
}
```

**Field notes**

| Field | Type | Description |
|---|---|---|
| `overallScore` | `number` 0–100 | Aggregate firm health score across all completed reports |
| `trend` | `"up" \| "down" \| "stable"` | Direction vs. previous period |
| `trendPercent` | `number` | Absolute percentage change vs. previous period |
| `reportCount` | `number` | Total reports ever created for this firm |
| `lastReportDate` | `string (ISO 8601)` | `createdAt` of the most recent report |
| `kpis` | `KPI[]` | Array of key metric pills shown on the dashboard |

**KPI object**

| Field | Type | Required | Description |
|---|---|---|---|
| `label` | `string` | Yes | Display name |
| `value` | `string \| number` | Yes | The metric value |
| `unit` | `string` | No | Appended to value in UI — e.g. `"/100"`, `"%"` |
| `change` | `number \| null` | No | % change vs. prior period. Null if not applicable |
| `status` | `"good" \| "warning" \| "critical"` | Yes | Drives colour coding in the UI |

> The number of KPI items is not fixed. The frontend renders whatever array you return. 4 items is the current sweet spot for the dashboard layout.

---

## 4. Reports

Reports are the core product. A report is created by the user, analysed asynchronously by the backend, and then returned with findings and recommendations when complete.

---

### `GET /reports`

Returns all reports for the authenticated user's firm, newest first.

**Response `200`**
```json
{
  "data": [
    {
      "id": "rpt_001",
      "firmId": "firm_abc123",
      "title": "Q1 2026 Strategic Performance Analysis",
      "status": "completed",
      "createdAt": "2026-01-15T10:00:00Z",
      "completedAt": "2026-01-20T14:30:00Z",
      "summary": "Overall performance improved by 12% ...",
      "score": 82,
      "tags": ["quarterly", "strategy", "performance"]
    },
    {
      "id": "rpt_003",
      "firmId": "firm_abc123",
      "title": "Q2 2026 Market Positioning Report",
      "status": "in_progress",
      "createdAt": "2026-04-28T08:00:00Z",
      "completedAt": null,
      "summary": null,
      "score": null,
      "tags": ["quarterly", "market", "positioning"]
    }
  ]
}
```

> `summary`, `score`, and `completedAt` are `null` / omitted while `status` is `pending` or `in_progress`.

---

### `POST /reports`

Creates a new report and enqueues it for analysis.

**Request body**
```json
{
  "title": "Q3 2026 Strategic Review",
  "tags": ["quarterly", "strategy"]
}
```

> `tags` is optional. An empty array is valid.

**Response `201`**
```json
{
  "data": {
    "id": "rpt_new_xyz",
    "firmId": "firm_abc123",
    "title": "Q3 2026 Strategic Review",
    "status": "pending",
    "createdAt": "2026-05-10T11:00:00Z",
    "completedAt": null,
    "summary": null,
    "score": null,
    "tags": ["quarterly", "strategy"]
  }
}
```

> The report starts as `"pending"`. Your backend should transition it through `"in_progress"` → `"completed"` (or `"failed"`) as analysis runs. The frontend can poll `GET /reports` or use WebSockets/SSE to pick up status transitions.

---

### `GET /reports/:id`

Returns a single report by ID.

**Response `200`** — same shape as a single item in `GET /reports`.

**Response `404`**
```json
{ "message": "Report not found." }
```

---

### `GET /reports/:id/detail`

Returns the detailed findings and recommendations for a **completed** report. The frontend only calls this endpoint when `report.status === "completed"`. Return `null` (or `404`) for non-completed reports.

**Response `200`**
```json
{
  "data": {
    "findings": [
      {
        "id": "f1",
        "severity": "positive",
        "title": "Digital Transformation Leadership",
        "detail": "Your firm's digital adoption rate of 78% exceeds the industry median..."
      },
      {
        "id": "f2",
        "severity": "warning",
        "title": "Technology Debt Accumulation",
        "detail": "Legacy system components account for 23% of the current stack..."
      },
      {
        "id": "f3",
        "severity": "critical",
        "title": "Talent Retention Risk",
        "detail": "Voluntary attrition in technical roles reached 18% in H2 2025..."
      }
    ],
    "recommendations": [
      {
        "id": "r1",
        "priority": "high",
        "title": "Accelerate Legacy System Migration",
        "detail": "Prioritise migration of the top 3 legacy components...",
        "effort": "High",
        "impact": "High"
      },
      {
        "id": "r2",
        "priority": "medium",
        "title": "Expand Automation Coverage",
        "detail": "Extend RPA coverage to Finance and HR workflows...",
        "effort": "Medium",
        "impact": "Medium"
      }
    ]
  }
}
```

**Finding object**

| Field | Type | Values |
|---|---|---|
| `id` | `string` | Unique within the report |
| `severity` | `string` | `"positive"` \| `"warning"` \| `"critical"` |
| `title` | `string` | Short headline |
| `detail` | `string` | 1–3 sentence explanation |

**Recommendation object**

| Field | Type | Values |
|---|---|---|
| `id` | `string` | Unique within the report |
| `priority` | `string` | `"high"` \| `"medium"` \| `"low"` |
| `title` | `string` | Short action title |
| `detail` | `string` | Actionable description |
| `effort` | `string` | `"High"` \| `"Medium"` \| `"Low"` (display string) |
| `impact` | `string` | `"High"` \| `"Medium"` \| `"Low"` (display string) |

---

## 5. Analytics

The analytics section provides aggregated, time-series, and comparative data derived from the firm's report history. These endpoints power the Analytics page charts.

---

### `GET /analytics/score-trend`

Returns the score history for all completed reports, ordered chronologically. Used to render the Score Trend Over Time sparkline chart.

**Query parameters**

| Param | Type | Default | Description |
|---|---|---|---|
| `limit` | `number` | `10` | Maximum number of data points to return (newest first, then reversed for display) |

**Response `200`**
```json
{
  "data": {
    "points": [
      {
        "reportId": "rpt_005",
        "reportTitle": "Q2 2025 Baseline Assessment",
        "score": 58,
        "completedAt": "2025-07-09T11:20:00Z"
      },
      {
        "reportId": "rpt_006",
        "reportTitle": "Q3 2025 Operational Review",
        "score": 63,
        "completedAt": "2025-09-22T14:45:00Z"
      },
      {
        "reportId": "rpt_007",
        "reportTitle": "Q4 2025 Year-End Performance Report",
        "score": 71,
        "completedAt": "2025-12-10T17:00:00Z"
      },
      {
        "reportId": "rpt_001",
        "reportTitle": "Q1 2026 Strategic Performance Analysis",
        "score": 82,
        "completedAt": "2026-01-20T14:30:00Z"
      },
      {
        "reportId": "rpt_002",
        "reportTitle": "Risk Assessment — Technology Infrastructure",
        "score": 64,
        "completedAt": "2026-02-14T16:00:00Z"
      },
      {
        "reportId": "rpt_004",
        "reportTitle": "Competitive Landscape & Market Positioning",
        "score": 74,
        "completedAt": "2026-03-25T15:30:00Z"
      },
      {
        "reportId": "rpt_008",
        "reportTitle": "Q2 2026 Revenue & Growth Analysis",
        "score": 79,
        "completedAt": "2026-05-08T13:15:00Z"
      }
    ],
    "average": 70,
    "highest": 82,
    "lowest": 58,
    "trend": "up",
    "trendPercent": 36.2
  }
}
```

**ScoreTrendPoint object**

| Field | Type | Description |
|---|---|---|
| `reportId` | `string` | ID of the completed report |
| `reportTitle` | `string` | Title — used as tooltip label on hover |
| `score` | `number` | 0–100 |
| `completedAt` | `string (ISO 8601)` | X-axis timestamp |

**Envelope fields**

| Field | Type | Description |
|---|---|---|
| `points` | `ScoreTrendPoint[]` | Ordered oldest → newest |
| `average` | `number` | Mean score across all returned points |
| `highest` | `number` | Max score in the period |
| `lowest` | `number` | Min score in the period |
| `trend` | `"up" \| "down" \| "stable"` | Direction comparing first and last point |
| `trendPercent` | `number` | % change from first to last point |

> **Frontend note:** The Analytics page currently derives the sparkline directly from the `GET /reports` response (filtering for `status === "completed"`). Once the backend is live, this endpoint provides richer metadata (average, highest, lowest, tooltip titles) and avoids forcing the client to load the full report list just to draw the chart. The frontend service swap is in `src/services/analytics.ts` (to be created when the backend is ready).

---

### `GET /analytics/reports-by-month`

Returns a count of reports created and completed per calendar month. Used to render the Reports by Month bar chart.

**Query parameters**

| Param | Type | Default | Description |
|---|---|---|---|
| `months` | `number` | `12` | How many trailing months to return |

**Response `200`**
```json
{
  "data": [
    {
      "month": "2025-07",
      "created": 1,
      "completed": 1
    },
    {
      "month": "2025-09",
      "created": 1,
      "completed": 1
    },
    {
      "month": "2025-12",
      "created": 1,
      "completed": 1
    },
    {
      "month": "2026-01",
      "created": 1,
      "completed": 1
    },
    {
      "month": "2026-02",
      "created": 1,
      "completed": 1
    },
    {
      "month": "2026-03",
      "created": 1,
      "completed": 1
    },
    {
      "month": "2026-05",
      "created": 2,
      "completed": 1
    }
  ]
}
```

**ReportsByMonth object**

| Field | Type | Description |
|---|---|---|
| `month` | `string` | `YYYY-MM` format |
| `created` | `number` | Reports created in this month |
| `completed` | `number` | Reports completed in this month |

> Months with zero activity should still be included as `{ "created": 0, "completed": 0 }` so the chart X-axis is continuous.

---

### `GET /analytics/goal-progress`

Returns a progress score (0–100) for each of the firm's selected strategic goals, derived from findings across all completed reports.

**Response `200`**
```json
{
  "data": [
    { "goal": "digital_transformation", "label": "Digital Transformation", "score": 78, "change": 20 },
    { "goal": "operational_efficiency", "label": "Operational Efficiency", "score": 85, "change": 12 },
    { "goal": "revenue_growth", "label": "Revenue Growth", "score": 79, "change": 15 },
    { "goal": "risk_management", "label": "Risk Management", "score": 61, "change": 8 }
  ]
}
```

**GoalProgress object**

| Field | Type | Description |
|---|---|---|
| `goal` | `Goal` | Goal enum value (see Enum Reference) |
| `label` | `string` | Display name |
| `score` | `number` | 0–100 derived from relevant report findings |
| `change` | `number` | Point change vs. first report baseline |

> Only goals the firm selected during onboarding should appear in the response.

---

## 6. Chatbot

The chatbot is an AI advisor scoped to the authenticated firm. It has access to the firm's report history and can answer questions about findings, scores, and recommendations.

---

### `GET /chatbot/history`

Returns the full conversation history for the authenticated firm.

**Response `200`**
```json
{
  "data": [
    {
      "id": "msg_001",
      "role": "assistant",
      "content": "Hello! I'm your strategic advisor...",
      "createdAt": "2026-04-28T09:00:00Z"
    },
    {
      "id": "msg_002",
      "role": "user",
      "content": "What are the key takeaways from our latest report?",
      "createdAt": "2026-04-28T09:01:00Z"
    },
    {
      "id": "msg_003",
      "role": "assistant",
      "content": "Based on your Q1 2026 Strategic Performance Analysis...",
      "createdAt": "2026-04-28T09:01:30Z"
    }
  ]
}
```

> Return messages in chronological order (oldest first). The frontend renders them top-to-bottom.

---

### `POST /chatbot/message`

Sends a user message and returns the assistant's reply. The frontend adds the user message optimistically to the UI and appends the assistant reply when this response arrives.

**Request body**
```json
{
  "content": "What are our biggest risks right now?",
  "firmId": "firm_abc123"
}
```

> `firmId` is included so the backend can scope the AI context to the correct firm's data even if your architecture runs the chatbot service separately.

**Response `200`** — returns only the **assistant's reply message** (not the user message — the frontend already rendered that optimistically):
```json
{
  "data": {
    "id": "msg_xyz",
    "role": "assistant",
    "content": "Based on your latest reports, your top 3 risks are...",
    "createdAt": "2026-05-10T11:05:00Z"
  }
}
```

> The backend should persist both the user message and the assistant reply to the conversation history so subsequent `GET /chatbot/history` calls include them.

---

## 7. Notifications

Notifications are currently stored in the frontend Zustand store (seeded from mock data). When you build the backend, expose these endpoints and the frontend will fetch live data.

> **Note:** The notifications feature currently reads from a local mock store. To switch to real-time notifications, replace the Zustand store initialisation in `src/store/notification-store.ts` with a `useQuery` call to the endpoints below.

---

### `GET /notifications`

Returns all notifications for the authenticated user.

**Response `200`**
```json
{
  "data": [
    {
      "id": "notif_001",
      "title": "Report completed",
      "body": "Q1 2026 Strategic Performance Analysis is ready to view.",
      "type": "report",
      "read": false,
      "createdAt": "2026-01-20T14:30:00Z",
      "href": "/reports/rpt_001"
    }
  ]
}
```

**Notification object**

| Field | Type | Required | Description |
|---|---|---|---|
| `id` | `string` | Yes | |
| `title` | `string` | Yes | Short headline |
| `body` | `string` | Yes | One-sentence description |
| `type` | `string` | Yes | `"report"` \| `"info"` \| `"warning"` |
| `read` | `boolean` | Yes | Whether the user has dismissed it |
| `createdAt` | `string` | Yes | ISO 8601 |
| `href` | `string` | No | Frontend route to navigate to on click |

---

### `PATCH /notifications/:id/read`

Marks a single notification as read.

**Request body** — empty

**Response `200`**
```json
{ "data": { "id": "notif_001", "read": true } }
```

---

### `PATCH /notifications/read-all`

Marks all of the authenticated user's notifications as read.

**Response `200`**
```json
{ "data": null, "message": "All notifications marked as read." }
```

---

## 8. Data Schemas

Full TypeScript interfaces used by the frontend — build your database models to match these shapes exactly.

### Auth

```typescript
interface RegisterPayload {
  email: string;
  password: string;
  companyName: string;
}

interface LoginPayload {
  email: string;
  password: string;
}

interface AuthUser {
  id: string;
  email: string;
  companyName: string;
  firmId: string | null;  // null before survey completion
  token: string;          // JWT
}
```

### Firm

```typescript
interface Firm {
  id: string;
  name: string;
  industry: Industry;     // see Enum Reference
  size: CompanySize;      // see Enum Reference
  goals: Goal[];          // see Enum Reference
  createdAt: string;      // ISO 8601
  updatedAt: string;      // ISO 8601
}

interface FirmMetrics {
  overallScore: number;           // 0–100
  trend: "up" | "down" | "stable";
  trendPercent: number;           // absolute %, e.g. 8.4
  reportCount: number;
  lastReportDate: string;         // ISO 8601
  kpis: KPI[];
}

interface KPI {
  label: string;
  value: string | number;
  unit?: string;                  // e.g. "/100", "%"
  change?: number | null;         // % change, null if N/A
  status: "good" | "warning" | "critical";
}
```

### Report

```typescript
interface Report {
  id: string;
  firmId: string;
  title: string;
  status: "pending" | "in_progress" | "completed" | "failed";
  createdAt: string;      // ISO 8601
  completedAt?: string;   // ISO 8601 — only when status === "completed"
  summary?: string;       // only when status === "completed"
  score?: number;         // 0–100 — only when status === "completed"
  tags: string[];
}

interface ReportDetail {
  findings: Finding[];
  recommendations: Recommendation[];
}

interface Finding {
  id: string;
  severity: "positive" | "warning" | "critical";
  title: string;
  detail: string;
}

interface Recommendation {
  id: string;
  priority: "high" | "medium" | "low";
  title: string;
  detail: string;
  effort: "High" | "Medium" | "Low";
  impact: "High" | "Medium" | "Low";
}
```

### Chatbot

```typescript
interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  createdAt: string;   // ISO 8601
}

interface SendMessagePayload {
  content: string;
  firmId: string;
}
```

### Notification

```typescript
interface Notification {
  id: string;
  title: string;
  body: string;
  type: "report" | "info" | "warning";
  read: boolean;
  createdAt: string;   // ISO 8601
  href?: string;       // frontend route, e.g. "/reports/rpt_001"
}
```

### Analytics

```typescript
interface ScoreTrendPoint {
  reportId: string;
  reportTitle: string;
  score: number;        // 0–100
  completedAt: string;  // ISO 8601
}

interface ScoreTrendResponse {
  points: ScoreTrendPoint[];
  average: number;
  highest: number;
  lowest: number;
  trend: "up" | "down" | "stable";
  trendPercent: number;
}

interface ReportsByMonth {
  month: string;    // "YYYY-MM"
  created: number;
  completed: number;
}

interface GoalProgress {
  goal: Goal;       // see Enum Reference
  label: string;
  score: number;    // 0–100
  change: number;   // points vs. baseline
}
```

---

## 9. Error Format

All error responses must use this shape. The frontend reads `err.response.data.message` and surfaces it in form error states and toast notifications.

```json
{
  "message": "Human-readable error description.",
  "code": "OPTIONAL_ERROR_CODE",
  "status": 422
}
```

### Standard HTTP status codes

| Status | When to use |
|---|---|
| `400` | Malformed request body |
| `401` | Missing or invalid token |
| `403` | Valid token but insufficient permissions |
| `404` | Resource not found |
| `409` | Conflict — e.g. email already registered |
| `422` | Validation failure (wrong field values) |
| `500` | Internal server error |

---

## 10. Enum Reference

### `Industry`

| Value | Display |
|---|---|
| `technology` | Technology |
| `finance` | Finance & Banking |
| `healthcare` | Healthcare |
| `retail` | Retail & E-Commerce |
| `manufacturing` | Manufacturing |
| `consulting` | Consulting |
| `education` | Education |
| `other` | Other |

### `CompanySize`

| Value | Display |
|---|---|
| `1-10` | 1–10 employees |
| `11-50` | 11–50 employees |
| `51-200` | 51–200 employees |
| `201-1000` | 201–1,000 employees |
| `1000+` | 1,000+ employees |

### `Goal`

| Value | Display |
|---|---|
| `cost_reduction` | Cost Reduction |
| `revenue_growth` | Revenue Growth |
| `operational_efficiency` | Operational Efficiency |
| `risk_management` | Risk Management |
| `digital_transformation` | Digital Transformation |

### `ReportStatus`

| Value | Meaning |
|---|---|
| `pending` | Enqueued, not yet started |
| `in_progress` | Analysis running |
| `completed` | Analysis done; findings and score available |
| `failed` | Analysis failed; no score |

---

## Appendix — API Endpoint Summary

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/auth/register` | No | Create user account |
| `POST` | `/auth/login` | No | Authenticate and receive token |
| `POST` | `/auth/logout` | Yes | Invalidate token |
| `POST` | `/firms` | Yes | Create firm after survey |
| `GET` | `/firms/me` | Yes | Get authenticated firm profile |
| `PATCH` | `/firms/me` | Yes | Update firm profile |
| `GET` | `/firms/me/metrics` | Yes | Get firm KPIs and score |
| `GET` | `/reports` | Yes | List all reports for the firm |
| `POST` | `/reports` | Yes | Create a new report |
| `GET` | `/reports/:id` | Yes | Get a single report |
| `GET` | `/reports/:id/detail` | Yes | Get findings and recommendations |
| `GET` | `/analytics/score-trend` | Yes | Score history points for sparkline chart |
| `GET` | `/analytics/reports-by-month` | Yes | Report counts per calendar month |
| `GET` | `/analytics/goal-progress` | Yes | Progress score per strategic goal |
| `GET` | `/chatbot/history` | Yes | Get full conversation history |
| `POST` | `/chatbot/message` | Yes | Send a message, receive AI reply |
| `GET` | `/notifications` | Yes | List notifications |
| `PATCH` | `/notifications/:id/read` | Yes | Mark one notification read |
| `PATCH` | `/notifications/read-all` | Yes | Mark all notifications read |
