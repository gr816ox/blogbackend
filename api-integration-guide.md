# Blog Backend API Guide

## Base Information

- Base URL: `http://localhost:8081`
- Auth header: `Authorization: Bearer <token>`
- Content type: `application/json`

## Auth Flow

### Register

Request:

```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "editor01",
  "password": "StrongPass123"
}
```

Successful response:

```json
{
  "message": "Register success"
}
```

### Login

Request:

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "your-password"
}
```

Successful response:

```json
{
  "token": "eyJhbGciOi...",
  "user": {
    "id": 1,
    "username": "admin",
    "role": "ADMIN"
  }
}
```

### Current User

Request:

```http
GET /api/auth/me
Authorization: Bearer <token>
```

Successful response:

```json
{
  "id": 1,
  "username": "admin",
  "role": "ADMIN"
}
```

## Articles

### Paginated Article List

Purpose: public list endpoint with pagination and lightweight fields only. This endpoint does not return `content`.

Request:

```http
GET /api/articles?page=1&size=10&keyword=spring&category=backend
```

Successful response:

```json
{
  "items": [
    {
      "id": 12,
      "title": "Spring Security Notes",
      "category": "backend",
      "summary": "JWT and role-based access control.",
      "createdAt": "2026-03-10T08:30:00.000+00:00"
    }
  ],
  "page": 1,
  "size": 10,
  "total": 1,
  "totalPages": 1,
  "hasNext": false
}
```

Query parameters:

- `page`: default `1`, minimum `1`
- `size`: default `10`, range `1-100`
- `keyword`: optional, fuzzy match against `title` and `summary`
- `category`: optional, exact match

Legacy aliases still available:

- `GET /api/public/articles`

### Article Detail

Request:

```http
GET /api/articles/12
```

Successful response:

```json
{
  "articleId": 12,
  "title": "Spring Security Notes",
  "createdAt": "2026-03-10T08:30:00.000+00:00",
  "category": "backend",
  "summary": "JWT and role-based access control.",
  "content": "Full article content..."
}
```

Legacy alias still available:

- `GET /api/public/articles/12`

### Create Article

Permission: `ADMIN`

Request:

```http
POST /api/articles
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "title": "Spring Security Notes",
  "category": "backend",
  "summary": "JWT and role-based access control.",
  "content": "Full article content..."
}
```

Successful response: `201 Created`

Legacy alias still available:

- `POST /api/private/articles`

### Update Article

Permission: `ADMIN`

Request:

```http
PUT /api/articles/12
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "title": "Spring Security Notes Updated",
  "category": "backend",
  "summary": "Updated summary.",
  "content": "Updated full article content..."
}
```

Legacy alias still available:

- `PUT /api/private/articles/12`

### Delete Article

Permission: `ADMIN`

Request:

```http
DELETE /api/articles/12
Authorization: Bearer <admin-token>
```

Successful response: `204 No Content`

Legacy alias still available:

- `DELETE /api/private/articles/12`

## Common Error Responses

401 example:

```json
{
  "timestamp": "2026-04-09T08:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication required",
  "path": "/api/auth/me"
}
```

403 example:

```json
{
  "timestamp": "2026-04-09T08:00:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied",
  "path": "/api/articles"
}
```

400 example:

```json
{
  "timestamp": "2026-04-09T08:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "size must be between 1 and 100",
  "path": "/api/articles"
}
```

## Integration Checklist

1. Login first and persist `token` plus `user.role`.
2. Use `GET /api/articles` for lists and render `items` instead of assuming a raw array.
3. Read full article content only through `GET /api/articles/{id}`.
4. Hide create, update, and delete actions unless `role === 'ADMIN'`.
5. Attach `Authorization: Bearer <token>` to `/api/auth/me` and all write requests.
6. When list filters change, send `page=1` again to avoid requesting empty pages.
7. Handle `401` by clearing local session and redirecting to login.
8. Handle `403` as a permission error, not a login-expired error.