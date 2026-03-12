# Menus Backend — API Contracts

> Generado por agente architect (T01). Fecha: 2026-03-10.
> Base URL: `https://menus-api.apptolast.com/api/v1`
> Swagger UI: `/swagger-ui/index.html`

---

## Convenciones Generales

- **Content-Type**: `application/json`
- **Autenticación**: `Authorization: Bearer <JWT>`
- **Paginación**: `?page=0&size=20&sort=createdAt,desc`
- **Versión de API**: en la URL (`/api/v1`), no en headers
- **Access token**: válido 15 minutos
- **Refresh token**: válido 7 días

---

## Formato de Error Estándar (todos los endpoints)

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Descripción legible del error",
    "status": 400,
    "timestamp": "2026-03-10T10:30:00Z",
    "path": "/api/v1/ruta/del/endpoint"
  }
}
```

### Códigos de Error Estándar

| Código HTTP | Error Code | Descripción |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Datos de entrada inválidos (campo requerido, formato incorrecto) |
| 401 | `UNAUTHORIZED` | Token JWT ausente, expirado o inválido |
| 401 | `INVALID_CREDENTIALS` | Email o contraseña incorrectos |
| 401 | `INVALID_GOOGLE_TOKEN` | El ID token de Google no es válido |
| 403 | `FORBIDDEN` | El usuario autenticado no tiene permiso para este recurso |
| 403 | `ACCOUNT_DISABLED` | La cuenta está desactivada |
| 403 | `ALLERGEN_PROFILE_CONSENT_REQUIRED` | Se requiere consentimiento explícito para acceder a datos de salud |
| 404 | `RESOURCE_NOT_FOUND` | El recurso solicitado no existe |
| 409 | `EMAIL_ALREADY_EXISTS` | Ya existe una cuenta con este email |
| 409 | `CONFLICT` | Conflicto de estado (ej. menú ya archivado) |
| 422 | `BUSINESS_RULE_VIOLATION` | Violación de regla de negocio (ej. límite de suscripción) |
| 500 | `INTERNAL_SERVER_ERROR` | Error interno del servidor |

---

## Módulo: Auth (`/api/v1/auth`)

### POST /api/v1/auth/register

Registro de nuevo usuario con email y contraseña.

**Autenticación**: Pública

**Request**:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "acceptTerms": true
}
```

| Campo | Tipo | Requerido | Validación |
|---|---|---|---|
| `email` | string | Sí | Formato email válido |
| `password` | string | Sí | Mínimo 8 caracteres, al menos 1 mayúscula, 1 número, 1 especial |
| `acceptTerms` | boolean | Sí | Debe ser `true` |

**Response 201 Created**:
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Errores**:
- `400 VALIDATION_ERROR`: Campos inválidos
- `409 EMAIL_ALREADY_EXISTS`: El email ya está registrado

---

### POST /api/v1/auth/login

Login con email y contraseña.

**Autenticación**: Pública

**Request**:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response 200 OK**:
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Errores**:
- `401 INVALID_CREDENTIALS`: Email o contraseña incorrectos
- `403 ACCOUNT_DISABLED`: Cuenta desactivada

---

### POST /api/v1/auth/refresh

Renueva el access token usando un refresh token válido.

**Autenticación**: Pública

**Request**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response 200 OK**:
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Errores**:
- `401 UNAUTHORIZED`: Refresh token expirado o inválido

---

### GET /api/v1/auth/oauth2/google

Inicia el flujo OAuth2 con Google. Redirige al consentimiento de Google.

**Autenticación**: Pública

**Response**: Redirect 302 a Google OAuth2 consent screen

---

### POST /api/v1/auth/oauth2/google/callback

Procesa el callback de Google. Crea cuenta si no existe. Devuelve JWT.

**Autenticación**: Pública

**Request**:
```json
{
  "idToken": "Google ID token recibido del frontend"
}
```

**Response 200 OK**:
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Errores**:
- `401 INVALID_GOOGLE_TOKEN`: ID token de Google inválido o expirado

---

### POST /api/v1/auth/consent

Da consentimiento explícito para el procesamiento de datos de salud (RGPD).

**Autenticación**: JWT requerido

**Request**:
```json
{
  "consentType": "HEALTH_DATA_PROCESSING",
  "granted": true
}
```

**Response 200 OK**:
```json
{
  "consentType": "HEALTH_DATA_PROCESSING",
  "granted": true,
  "grantedAt": "2026-03-10T10:30:00Z"
}
```

**Errores**:
- `401 UNAUTHORIZED`: JWT inválido

---

### DELETE /api/v1/auth/consent

Revoca el consentimiento de datos de salud. Elimina físicamente el perfil de alérgenos.

**Autenticación**: JWT requerido

**Response 204 No Content**

**Errores**:
- `401 UNAUTHORIZED`: JWT inválido

---

## Módulo: Allergen (`/api/v1/allergens`)

### GET /api/v1/allergens

Lista los 14 alérgenos EU con traducciones. Soporta filtro por locale.

**Autenticación**: Pública

**Query Parameters**:

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `locale` | string | No | Idioma de las traducciones: `es`, `en`, `ca`, `eu`, `gl`. Default: `es` |

**Response 200 OK**:
```json
[
  {
    "id": 1,
    "code": "GLUTEN",
    "name": "Gluten",
    "description": "Cereales que contienen gluten y productos derivados",
    "iconUrl": "https://cdn.apptolast.com/allergens/gluten.svg",
    "locale": "es"
  },
  {
    "id": 2,
    "code": "CRUSTACEANS",
    "name": "Crustáceos",
    "description": "Crustáceos y productos a base de crustáceos",
    "iconUrl": "https://cdn.apptolast.com/allergens/crustaceans.svg",
    "locale": "es"
  }
]
```

---

### GET /api/v1/allergens/{code}

Detalle de un alérgeno por su código.

**Autenticación**: Pública

**Path Parameters**:

| Parámetro | Tipo | Descripción |
|---|---|---|
| `code` | string | Código del alérgeno: `GLUTEN`, `CRUSTACEANS`, `EGGS`, `FISH`, `PEANUTS`, `SOYBEANS`, `MILK`, `NUTS`, `CELERY`, `MUSTARD`, `SESAME`, `SULPHITES`, `LUPIN`, `MOLLUSCS` |

**Response 200 OK**:
```json
{
  "id": 1,
  "code": "GLUTEN",
  "translations": {
    "es": { "name": "Gluten", "description": "Cereales que contienen gluten..." },
    "en": { "name": "Gluten", "description": "Cereals containing gluten..." },
    "ca": { "name": "Gluten", "description": "Cereals que contenen gluten..." },
    "eu": { "name": "Glutena", "description": "..." },
    "gl": { "name": "Glute", "description": "..." }
  },
  "iconUrl": "https://cdn.apptolast.com/allergens/gluten.svg"
}
```

**Errores**:
- `404 RESOURCE_NOT_FOUND`: Código de alérgeno no existe

---

## Módulo: Consumer — Restaurantes (público)

### GET /api/v1/restaurants

Búsqueda de restaurantes activos.

**Autenticación**: Pública

**Query Parameters**:

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `name` | string | No | Filtro por nombre (contains, case-insensitive) |
| `page` | integer | No | Número de página. Default: 0 |
| `size` | integer | No | Tamaño de página. Default: 20, máximo: 100 |
| `sort` | string | No | Campo de ordenación. Default: `name,asc` |

**Response 200 OK**:
```json
{
  "content": [
    {
      "id": "uuid",
      "name": "Cobre y Picón",
      "slug": "cobre-y-picon",
      "description": "Restaurante de cocina tradicional española",
      "address": "Calle Mayor 1, Madrid",
      "phone": "+34 91 000 0000",
      "logoUrl": "https://cdn.apptolast.com/restaurants/cobre-y-picon.jpg"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### GET /api/v1/restaurants/{id}

Detalle de un restaurante.

**Autenticación**: Pública

**Path Parameters**:

| Parámetro | Tipo | Descripción |
|---|---|---|
| `id` | UUID | ID del restaurante |

**Response 200 OK**:
```json
{
  "id": "uuid",
  "name": "Cobre y Picón",
  "slug": "cobre-y-picon",
  "description": "Restaurante de cocina tradicional española",
  "address": "Calle Mayor 1, Madrid",
  "phone": "+34 91 000 0000",
  "logoUrl": "https://cdn.apptolast.com/restaurants/cobre-y-picon.jpg"
}
```

**Errores**:
- `404 RESOURCE_NOT_FOUND`: Restaurante no existe o no está activo

---

### GET /api/v1/restaurants/{id}/menu

Menú del restaurante con filtrado semáforo (SAFE/RISK/DANGER) si el usuario tiene JWT y consentimiento activo.

**Autenticación**: Opcional (JWT). Sin JWT: se devuelve menú sin filtrado semáforo.

**Path Parameters**:

| Parámetro | Tipo | Descripción |
|---|---|---|
| `id` | UUID | ID del restaurante |

**Response 200 OK** (con JWT + consentimiento activo):
```json
{
  "restaurantId": "uuid",
  "restaurantName": "Cobre y Picón",
  "sections": [
    {
      "sectionId": "uuid",
      "name": "Entrantes",
      "displayOrder": 1,
      "dishes": [
        {
          "dishId": "uuid",
          "name": "Ensalada César",
          "description": "Lechuga romana, pollo, parmesano, picatostes",
          "price": 8.50,
          "imageUrl": "https://cdn.apptolast.com/dishes/ensalada-cesar.jpg",
          "isAvailable": true,
          "safetyLevel": "RISK",
          "matchedAllergens": ["EGGS"],
          "allergens": [
            {
              "code": "EGGS",
              "name": "Huevos",
              "containmentLevel": "MAY_CONTAIN"
            },
            {
              "code": "GLUTEN",
              "name": "Gluten",
              "containmentLevel": "CONTAINS"
            },
            {
              "code": "MILK",
              "name": "Leche",
              "containmentLevel": "CONTAINS"
            },
            {
              "code": "FISH",
              "name": "Pescado",
              "containmentLevel": "CONTAINS"
            }
          ]
        }
      ]
    }
  ]
}
```

**Response 200 OK** (sin JWT, o sin consentimiento):
```json
{
  "restaurantId": "uuid",
  "restaurantName": "Cobre y Picón",
  "sections": [
    {
      "sectionId": "uuid",
      "name": "Entrantes",
      "displayOrder": 1,
      "dishes": [
        {
          "dishId": "uuid",
          "name": "Ensalada César",
          "price": 8.50,
          "isAvailable": true,
          "safetyLevel": null,
          "matchedAllergens": [],
          "allergens": [
            {
              "code": "EGGS",
              "name": "Huevos",
              "containmentLevel": "MAY_CONTAIN"
            }
          ]
        }
      ]
    }
  ]
}
```

**Lógica safetyLevel**:
- `DANGER`: El plato CONTAINS al menos un alérgeno del perfil del usuario
- `RISK`: El plato MAY_CONTAIN al menos un alérgeno del perfil (y ninguno es CONTAINS)
- `SAFE`: El plato no tiene alérgenos del perfil, o todos son FREE_OF
- `null`: Usuario sin JWT o sin consentimiento activo

**Errores**:
- `404 RESOURCE_NOT_FOUND`: Restaurante no existe

---

## Módulo: Consumer — Perfil de Alérgenos

### GET /api/v1/users/me/allergen-profile

Obtiene el perfil de alérgenos del usuario autenticado.

**Autenticación**: JWT requerido + Consentimiento HEALTH_DATA_PROCESSING activo

**Response 200 OK**:
```json
{
  "profileUuid": "uuid",
  "allergenCodes": ["GLUTEN", "MILK", "EGGS"],
  "severityNotes": "Alergia severa al gluten, intolerancia a la lactosa",
  "updatedAt": "2026-03-10T10:30:00Z"
}
```

**Errores**:
- `401 UNAUTHORIZED`: JWT inválido
- `403 ALLERGEN_PROFILE_CONSENT_REQUIRED`: No hay consentimiento activo

---

### PUT /api/v1/users/me/allergen-profile

Actualiza el perfil de alérgenos del usuario.

**Autenticación**: JWT requerido + Consentimiento HEALTH_DATA_PROCESSING activo

**Request**:
```json
{
  "allergenCodes": ["GLUTEN", "MILK", "EGGS"],
  "severityNotes": "Alergia severa al gluten, intolerancia a la lactosa"
}
```

| Campo | Tipo | Requerido | Validación |
|---|---|---|---|
| `allergenCodes` | string[] | Sí | Array de códigos válidos: GLUTEN, CRUSTACEANS, EGGS, FISH, PEANUTS, SOYBEANS, MILK, NUTS, CELERY, MUSTARD, SESAME, SULPHITES, LUPIN, MOLLUSCS |
| `severityNotes` | string | No | Texto libre, máximo 1000 caracteres |

**Response 200 OK**:
```json
{
  "profileUuid": "uuid",
  "allergenCodes": ["GLUTEN", "MILK", "EGGS"],
  "severityNotes": "Alergia severa al gluten, intolerancia a la lactosa",
  "updatedAt": "2026-03-10T10:30:00Z"
}
```

**Errores**:
- `400 VALIDATION_ERROR`: allergenCodes contiene código inválido
- `401 UNAUTHORIZED`: JWT inválido
- `403 ALLERGEN_PROFILE_CONSENT_REQUIRED`: No hay consentimiento activo

---

## Módulo: Admin — Restaurante (`/api/v1/admin`)

Todos los endpoints requieren JWT con rol `RESTAURANT_OWNER`. El tenant se establece automáticamente desde el JWT.

### GET /api/v1/admin/restaurant

Obtiene los datos del restaurante del propietario autenticado.

**Autenticación**: JWT — RESTAURANT_OWNER

**Response 200 OK**:
```json
{
  "id": "uuid",
  "name": "Cobre y Picón",
  "slug": "cobre-y-picon",
  "description": "Restaurante de cocina tradicional española",
  "address": "Calle Mayor 1, Madrid",
  "phone": "+34 91 000 0000",
  "logoUrl": "https://cdn.apptolast.com/restaurants/cobre-y-picon.jpg",
  "isActive": true,
  "createdAt": "2026-01-01T00:00:00Z",
  "updatedAt": "2026-03-10T10:30:00Z"
}
```

---

### PUT /api/v1/admin/restaurant

Actualiza los datos del restaurante propio.

**Autenticación**: JWT — RESTAURANT_OWNER

**Request**:
```json
{
  "name": "Cobre y Picón",
  "description": "Restaurante de cocina tradicional española",
  "address": "Calle Mayor 1, Madrid",
  "phone": "+34 91 000 0000",
  "logoUrl": "https://cdn.apptolast.com/restaurants/cobre-y-picon.jpg"
}
```

**Response 200 OK**: Mismo schema que GET /admin/restaurant

**Errores**:
- `400 VALIDATION_ERROR`: Campos inválidos
- `409 CONFLICT`: Slug ya en uso

---

### GET /api/v1/admin/menus

Lista los menús del restaurante.

**Autenticación**: JWT — RESTAURANT_OWNER

**Query Parameters**:

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `archived` | boolean | No | Si `true`, incluye menús archivados. Default: `false` |

**Response 200 OK**:
```json
[
  {
    "id": "uuid",
    "name": "Menú de Temporada",
    "description": "Menú de primavera 2026",
    "isArchived": false,
    "displayOrder": 1,
    "sectionCount": 3,
    "createdAt": "2026-01-01T00:00:00Z",
    "updatedAt": "2026-03-10T10:30:00Z"
  }
]
```

---

### POST /api/v1/admin/menus

Crea un nuevo menú.

**Autenticación**: JWT — RESTAURANT_OWNER

**Request**:
```json
{
  "name": "Menú de Temporada",
  "description": "Menú de primavera 2026",
  "displayOrder": 1
}
```

**Response 201 Created**:
```json
{
  "id": "uuid",
  "name": "Menú de Temporada",
  "description": "Menú de primavera 2026",
  "isArchived": false,
  "displayOrder": 1,
  "sectionCount": 0,
  "createdAt": "2026-03-10T10:30:00Z",
  "updatedAt": "2026-03-10T10:30:00Z"
}
```

**Errores**:
- `422 BUSINESS_RULE_VIOLATION`: Límite de menús de la suscripción alcanzado

---

### PUT /api/v1/admin/menus/{id}

Actualiza un menú.

**Autenticación**: JWT — RESTAURANT_OWNER

**Path Parameters**: `id` (UUID)

**Request**: Mismo schema que POST /admin/menus

**Response 200 OK**: Mismo schema que POST response

**Errores**:
- `404 RESOURCE_NOT_FOUND`: Menú no existe en este tenant

---

### DELETE /api/v1/admin/menus/{id}

Archiva un menú (soft delete: `is_archived = true`).

**Autenticación**: JWT — RESTAURANT_OWNER

**Path Parameters**: `id` (UUID)

**Response 204 No Content**

**Errores**:
- `404 RESOURCE_NOT_FOUND`: Menú no existe en este tenant

---

### POST /api/v1/admin/menus/{menuId}/sections

Crea una sección en un menú.

**Autenticación**: JWT — RESTAURANT_OWNER

**Path Parameters**: `menuId` (UUID)

**Request**:
```json
{
  "name": "Entrantes",
  "displayOrder": 1
}
```

**Response 201 Created**:
```json
{
  "id": "uuid",
  "menuId": "uuid",
  "name": "Entrantes",
  "displayOrder": 1
}
```

**Errores**:
- `404 RESOURCE_NOT_FOUND`: Menú no existe en este tenant

---

### PUT /api/v1/admin/menus/{menuId}/sections/{id}

Actualiza una sección.

**Autenticación**: JWT — RESTAURANT_OWNER

**Path Parameters**: `menuId` (UUID), `id` (UUID)

**Request**: Mismo schema que POST sections

**Response 200 OK**: Mismo schema que POST response

---

### DELETE /api/v1/admin/menus/{menuId}/sections/{id}

Elimina una sección y sus platos asociados.

**Autenticación**: JWT — RESTAURANT_OWNER

**Path Parameters**: `menuId` (UUID), `id` (UUID)

**Response 204 No Content**

---

### GET /api/v1/admin/dishes

Lista los platos del restaurante.

**Autenticación**: JWT — RESTAURANT_OWNER o KITCHEN_STAFF

**Query Parameters**: `page`, `size`, `sort` (paginación estándar)

**Response 200 OK**:
```json
{
  "content": [
    {
      "id": "uuid",
      "sectionId": "uuid",
      "sectionName": "Entrantes",
      "name": "Ensalada César",
      "description": "Lechuga romana, pollo, parmesano, picatostes",
      "price": 8.50,
      "imageUrl": null,
      "isAvailable": true,
      "allergens": [
        {
          "allergenCode": "EGGS",
          "allergenName": "Huevos",
          "containmentLevel": "MAY_CONTAIN"
        }
      ],
      "createdAt": "2026-01-01T00:00:00Z",
      "updatedAt": "2026-03-10T10:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### POST /api/v1/admin/dishes

Crea un plato con sus alérgenos.

**Autenticación**: JWT — RESTAURANT_OWNER

**Request**:
```json
{
  "sectionId": "uuid",
  "name": "Paella Valenciana",
  "description": "Arroz con gambas, mejillones, pollo y azafrán",
  "price": 18.00,
  "imageUrl": "https://cdn.apptolast.com/dishes/paella.jpg",
  "allergens": [
    { "allergenCode": "CRUSTACEANS", "containmentLevel": "CONTAINS" },
    { "allergenCode": "MOLLUSCS", "containmentLevel": "CONTAINS" },
    { "allergenCode": "GLUTEN", "containmentLevel": "MAY_CONTAIN" }
  ]
}
```

| Campo | Tipo | Requerido | Validación |
|---|---|---|---|
| `sectionId` | UUID | Sí | Debe pertenecer al tenant del restaurante |
| `name` | string | Sí | No vacío, máximo 255 caracteres |
| `description` | string | No | Máximo 2000 caracteres |
| `price` | decimal | No | >= 0, máximo 2 decimales |
| `imageUrl` | string | No | URL válida |
| `allergens` | array | No | Lista de alérgenos con nivel de contenencia |
| `allergens[].allergenCode` | string | Sí | Código válido de AllergenCode |
| `allergens[].containmentLevel` | string | Sí | `CONTAINS`, `MAY_CONTAIN`, o `FREE_OF` |

**Response 201 Created**: Schema completo de plato (mismo que GET /admin/dishes item)

**Errores**:
- `400 VALIDATION_ERROR`: Campos inválidos
- `404 RESOURCE_NOT_FOUND`: sectionId no existe en este tenant
- `422 BUSINESS_RULE_VIOLATION`: Límite de platos de la suscripción alcanzado

---

### PUT /api/v1/admin/dishes/{id}

Actualiza un plato. Registra cambios en allergen_audit_log.

**Autenticación**: JWT — RESTAURANT_OWNER

**Path Parameters**: `id` (UUID)

**Request**: Mismo schema que POST /admin/dishes (todos los campos opcionales en actualización)

**Response 200 OK**: Schema completo del plato actualizado

**Errores**:
- `404 RESOURCE_NOT_FOUND`: Plato no existe en este tenant

---

### DELETE /api/v1/admin/dishes/{id}

Elimina un plato.

**Autenticación**: JWT — RESTAURANT_OWNER

**Path Parameters**: `id` (UUID)

**Response 204 No Content**

---

### POST /api/v1/admin/dishes/{id}/allergens

Añade o actualiza un alérgeno en un plato. Registra en audit_log.

**Autenticación**: JWT — RESTAURANT_OWNER

**Path Parameters**: `id` (UUID del plato)

**Request**:
```json
{
  "allergenCode": "GLUTEN",
  "containmentLevel": "CONTAINS",
  "notes": "Uso de harina de trigo en la masa"
}
```

**Response 200 OK**:
```json
{
  "allergenCode": "GLUTEN",
  "allergenName": "Gluten",
  "containmentLevel": "CONTAINS",
  "notes": "Uso de harina de trigo en la masa"
}
```

**Errores**:
- `404 RESOURCE_NOT_FOUND`: Plato o alérgeno no existe

---

### DELETE /api/v1/admin/dishes/{id}/allergens/{allergenId}

Elimina un alérgeno de un plato. Registra en audit_log.

**Autenticación**: JWT — RESTAURANT_OWNER

**Path Parameters**: `id` (UUID del plato), `allergenId` (integer)

**Response 204 No Content**

**Errores**:
- `404 RESOURCE_NOT_FOUND`: Plato o asociación alérgeno-plato no existe

---

### GET /api/v1/admin/subscription

Obtiene la suscripción actual del restaurante.

**Autenticación**: JWT — RESTAURANT_OWNER

**Response 200 OK**:
```json
{
  "id": "uuid",
  "tier": "BASIC",
  "maxMenus": 1,
  "maxDishes": 50,
  "currentMenuCount": 1,
  "currentDishCount": 23,
  "startsAt": "2026-01-01T00:00:00Z",
  "expiresAt": null,
  "isActive": true
}
```

---

### GET /api/v1/admin/analytics

Estadísticas del restaurante.

**Autenticación**: JWT — RESTAURANT_OWNER

**Response 200 OK**:
```json
{
  "totalDishes": 45,
  "availableDishes": 42,
  "totalMenus": 1,
  "totalSections": 4,
  "allergenDistribution": {
    "GLUTEN": 28,
    "MILK": 15,
    "EGGS": 12,
    "FISH": 8
  }
}
```

---

### POST /api/v1/admin/qr/generate

Genera el código QR para el menú del restaurante.

**Autenticación**: JWT — RESTAURANT_OWNER

**Request**:
```json
{
  "menuId": "uuid",
  "format": "PNG"
}
```

**Response 200 OK**:
```json
{
  "qrUrl": "https://cdn.apptolast.com/qr/uuid.png",
  "menuUrl": "https://app.apptolast.com/menu/cobre-y-picon"
}
```

---

## Módulo: GDPR (`/api/v1/users/me`)

### GET /api/v1/users/me/data-export

Exporta todos los datos personales del usuario (derecho de portabilidad RGPD Art. 20).

**Autenticación**: JWT requerido

**Response 200 OK**:
```json
{
  "exportedAt": "2026-03-10T10:30:00Z",
  "userId": "uuid",
  "personalData": {
    "email": "user@example.com",
    "role": "CONSUMER",
    "createdAt": "2026-01-01T00:00:00Z"
  },
  "allergenProfile": {
    "allergenCodes": ["GLUTEN", "MILK"],
    "severityNotes": "...",
    "updatedAt": "2026-03-10T10:30:00Z"
  },
  "consentHistory": [
    {
      "consentType": "HEALTH_DATA_PROCESSING",
      "granted": true,
      "grantedAt": "2026-01-15T00:00:00Z",
      "revokedAt": null
    }
  ],
  "oauthAccounts": [
    {
      "provider": "GOOGLE",
      "linkedAt": "2026-01-01T00:00:00Z"
    }
  ]
}
```

---

### DELETE /api/v1/users/me/data-delete

Elimina la cuenta del usuario (derecho al olvido RGPD Art. 17).

**Autenticación**: JWT requerido

**Proceso de eliminación**:
1. `user_account.email` → reemplazado por hash irreversible
2. `user_account.password_hash` → `null`
3. `user_allergen_profile` → eliminado físicamente
4. `consent_record` → `revoked_at = now()`
5. `oauth_account` → eliminado físicamente
6. Restaurante y platos → se mantienen (pertenecen al negocio, no al usuario)

**Response 204 No Content**

**Errores**:
- `401 UNAUTHORIZED`: JWT inválido

---

### PUT /api/v1/users/me/data-rectification

Rectifica datos personales incorrectos (derecho de rectificación RGPD Art. 16).

**Autenticación**: JWT requerido

**Request**:
```json
{
  "email": "newemail@example.com",
  "currentPassword": "SecurePass123!"
}
```

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `email` | string | No | Nuevo email (requiere verificación) |
| `currentPassword` | string | Condicional | Requerido si se cambia el email |

**Response 200 OK**:
```json
{
  "message": "Los datos han sido actualizados correctamente",
  "updatedAt": "2026-03-10T10:30:00Z"
}
```

**Errores**:
- `400 VALIDATION_ERROR`: Formato de email inválido
- `401 UNAUTHORIZED`: JWT inválido o contraseña actual incorrecta
- `409 EMAIL_ALREADY_EXISTS`: El nuevo email ya está registrado

---

## Resumen de Endpoints por Módulo

| Módulo | Método | Path | Auth | Rol |
|---|---|---|---|---|
| **Auth** | POST | `/auth/register` | Pública | — |
| | POST | `/auth/login` | Pública | — |
| | POST | `/auth/refresh` | Pública | — |
| | GET | `/auth/oauth2/google` | Pública | — |
| | POST | `/auth/oauth2/google/callback` | Pública | — |
| | POST | `/auth/consent` | JWT | CONSUMER |
| | DELETE | `/auth/consent` | JWT | CONSUMER |
| **Allergen** | GET | `/allergens` | Pública | — |
| | GET | `/allergens/{code}` | Pública | — |
| **Consumer** | GET | `/restaurants` | Pública | — |
| | GET | `/restaurants/{id}` | Pública | — |
| | GET | `/restaurants/{id}/menu` | Opcional JWT | — |
| | GET | `/users/me/allergen-profile` | JWT + Consent | CONSUMER |
| | PUT | `/users/me/allergen-profile` | JWT + Consent | CONSUMER |
| **Admin** | GET | `/admin/restaurant` | JWT | RESTAURANT_OWNER |
| | PUT | `/admin/restaurant` | JWT | RESTAURANT_OWNER |
| | GET | `/admin/menus` | JWT | RESTAURANT_OWNER |
| | POST | `/admin/menus` | JWT | RESTAURANT_OWNER |
| | PUT | `/admin/menus/{id}` | JWT | RESTAURANT_OWNER |
| | DELETE | `/admin/menus/{id}` | JWT | RESTAURANT_OWNER |
| | POST | `/admin/menus/{menuId}/sections` | JWT | RESTAURANT_OWNER |
| | PUT | `/admin/menus/{menuId}/sections/{id}` | JWT | RESTAURANT_OWNER |
| | DELETE | `/admin/menus/{menuId}/sections/{id}` | JWT | RESTAURANT_OWNER |
| | GET | `/admin/dishes` | JWT | RESTAURANT_OWNER, KITCHEN_STAFF |
| | POST | `/admin/dishes` | JWT | RESTAURANT_OWNER |
| | PUT | `/admin/dishes/{id}` | JWT | RESTAURANT_OWNER |
| | DELETE | `/admin/dishes/{id}` | JWT | RESTAURANT_OWNER |
| | POST | `/admin/dishes/{id}/allergens` | JWT | RESTAURANT_OWNER |
| | DELETE | `/admin/dishes/{id}/allergens/{allergenId}` | JWT | RESTAURANT_OWNER |
| | GET | `/admin/subscription` | JWT | RESTAURANT_OWNER |
| | GET | `/admin/analytics` | JWT | RESTAURANT_OWNER |
| | POST | `/admin/qr/generate` | JWT | RESTAURANT_OWNER |
| **GDPR** | GET | `/users/me/data-export` | JWT | CONSUMER |
| | DELETE | `/users/me/data-delete` | JWT | CONSUMER |
| | PUT | `/users/me/data-rectification` | JWT | CONSUMER |

---

## Modulo: Admin — Ingredients (`/api/v1/admin/ingredients`)

> Added in R2 (2026-03-11). Tenant-scoped ingredient catalog with JSONB allergens.

All endpoints require JWT with role `RESTAURANT_OWNER`. Tenant isolation via RLS.

### GET /api/v1/admin/ingredients

List all ingredients for the authenticated restaurant's tenant.

**Authentication**: JWT -- RESTAURANT_OWNER

**Query Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `name` | string | No | Filter by name (contains, case-insensitive) |
| `page` | integer | No | Page number. Default: 0 |
| `size` | integer | No | Page size. Default: 20, max: 100 |

**Response 200 OK**:
```json
{
  "content": [
    {
      "id": "uuid",
      "name": "Harina de trigo",
      "brand": "Harimsa",
      "supplier": "Distribuidora Madrid",
      "allergens": [
        { "code": "GLUTEN", "level": "CONTAINS" }
      ],
      "traces": [
        { "code": "SOYBEANS", "source": "shared equipment" }
      ],
      "notes": null,
      "createdAt": "2026-03-10T10:30:00Z",
      "updatedAt": "2026-03-10T10:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### POST /api/v1/admin/ingredients

Create a new ingredient in the restaurant's tenant.

**Authentication**: JWT -- RESTAURANT_OWNER

**Request**:
```json
{
  "name": "Harina de trigo",
  "brand": "Harimsa",
  "supplier": "Distribuidora Madrid",
  "allergens": [
    { "code": "GLUTEN", "level": "CONTAINS" }
  ],
  "traces": [
    { "code": "SOYBEANS", "source": "shared equipment" }
  ],
  "ocrRawText": "Ingredients: wheat flour. May contain traces of soy.",
  "notes": "Paquete de 1kg"
}
```

| Field | Type | Required | Validation |
|---|---|---|---|
| `name` | string | Yes | Non-empty, max 255 chars |
| `brand` | string | No | Max 255 chars |
| `supplier` | string | No | Max 255 chars |
| `allergens` | array | No | Each entry: `code` (valid AllergenCode), `level` (CONTAINS/MAY_CONTAIN/FREE_OF) |
| `traces` | array | No | Each entry: `code` (valid AllergenCode), `source` (string, optional) |
| `ocrRawText` | string | No | Raw text from label OCR scanning |
| `notes` | string | No | Free text |

**Response 201 Created**: Same schema as GET list item

**Errors**:
- `400 VALIDATION_ERROR`: Invalid allergen code or level
- `409 CONFLICT`: Ingredient with same name already exists in this tenant

---

### PUT /api/v1/admin/ingredients/{id}

Update an existing ingredient.

**Authentication**: JWT -- RESTAURANT_OWNER

**Path Parameters**: `id` (UUID)

**Request**: Same schema as POST (all fields optional for partial update)

**Response 200 OK**: Full ingredient object

**Errors**:
- `404 RESOURCE_NOT_FOUND`: Ingredient not found in this tenant

---

### DELETE /api/v1/admin/ingredients/{id}

Delete an ingredient. Fails if referenced by active recipes.

**Authentication**: JWT -- RESTAURANT_OWNER

**Path Parameters**: `id` (UUID)

**Response 204 No Content**

**Errors**:
- `404 RESOURCE_NOT_FOUND`: Ingredient not found
- `409 CONFLICT`: Ingredient is referenced by active recipes

---

### POST /api/v1/admin/ingredients/analyze-text

Analyze raw text (from label OCR or manual input) to detect potential allergens.

**Authentication**: JWT -- RESTAURANT_OWNER

**Request**:
```json
{
  "text": "Ingredientes: harina de trigo, leche entera, huevos, mantequilla. Puede contener trazas de frutos de cascara."
}
```

**Response 200 OK**:
```json
{
  "detectedAllergens": [
    { "code": "GLUTEN", "level": "CONTAINS", "matchedKeyword": "harina de trigo" },
    { "code": "MILK", "level": "CONTAINS", "matchedKeyword": "leche" },
    { "code": "EGGS", "level": "CONTAINS", "matchedKeyword": "huevos" },
    { "code": "MILK", "level": "CONTAINS", "matchedKeyword": "mantequilla" },
    { "code": "NUTS", "level": "MAY_CONTAIN", "matchedKeyword": "trazas de frutos de cascara" }
  ],
  "rawText": "Ingredientes: harina de trigo..."
}
```

---

## Modulo: Admin -- Recipes (`/api/v1/admin/recipes`)

> Added in R2 (2026-03-11). Recipe management with sub-elaboration support and recursive allergen computation.

All endpoints require JWT with role `RESTAURANT_OWNER`. Tenant isolation via RLS.

### GET /api/v1/admin/recipes

List all recipes for the authenticated restaurant.

**Authentication**: JWT -- RESTAURANT_OWNER

**Query Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `includeSubElaborations` | boolean | No | Include sub-elaborations. Default: true |
| `category` | string | No | Filter by category |
| `page` | integer | No | Default: 0 |
| `size` | integer | No | Default: 20 |

**Response 200 OK**:
```json
{
  "content": [
    {
      "id": "uuid",
      "name": "Paella Valenciana",
      "description": "Arroz con gambas, mejillones y azafran",
      "category": "Arroces",
      "isSubElaboration": false,
      "price": 18.00,
      "imageUrl": null,
      "isActive": true,
      "ingredientCount": 8,
      "computedAllergens": [
        { "code": "CRUSTACEANS", "level": "CONTAINS" },
        { "code": "MOLLUSCS", "level": "CONTAINS" },
        { "code": "GLUTEN", "level": "MAY_CONTAIN" }
      ],
      "createdAt": "2026-03-10T10:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### GET /api/v1/admin/recipes/{id}

Get recipe details with full ingredient list and computed allergens (recursive through sub-recipes).

**Authentication**: JWT -- RESTAURANT_OWNER

**Path Parameters**: `id` (UUID)

**Response 200 OK**:
```json
{
  "id": "uuid",
  "name": "Paella Valenciana",
  "description": "...",
  "category": "Arroces",
  "isSubElaboration": false,
  "price": 18.00,
  "imageUrl": null,
  "isActive": true,
  "ingredients": [
    {
      "id": "uuid",
      "type": "INGREDIENT",
      "ingredientId": "uuid",
      "ingredientName": "Gambas",
      "subRecipeId": null,
      "subRecipeName": null,
      "quantity": 0.5,
      "unit": "kg",
      "notes": null,
      "sortOrder": 1
    },
    {
      "id": "uuid",
      "type": "SUB_RECIPE",
      "ingredientId": null,
      "ingredientName": null,
      "subRecipeId": "uuid",
      "subRecipeName": "Sofrito base",
      "quantity": 0.2,
      "unit": "kg",
      "notes": "Usar el sofrito del dia",
      "sortOrder": 2
    }
  ],
  "computedAllergens": [
    { "code": "CRUSTACEANS", "level": "CONTAINS", "sources": ["Gambas"] },
    { "code": "MOLLUSCS", "level": "CONTAINS", "sources": ["Mejillones"] },
    { "code": "GLUTEN", "level": "MAY_CONTAIN", "sources": ["Sofrito base > Pan rallado"] }
  ],
  "createdAt": "2026-03-10T10:30:00Z",
  "updatedAt": "2026-03-10T10:30:00Z"
}
```

**Errors**:
- `404 RESOURCE_NOT_FOUND`: Recipe not found in this tenant
- `422 CYCLIC_RECIPE_DETECTED`: Sub-recipe chain contains a cycle
- `422 MAX_RECIPE_DEPTH_EXCEEDED`: Sub-recipe nesting exceeds maximum depth (10)

---

### POST /api/v1/admin/recipes

Create a new recipe.

**Authentication**: JWT -- RESTAURANT_OWNER

**Request**:
```json
{
  "name": "Paella Valenciana",
  "description": "Arroz con gambas, mejillones y azafran",
  "category": "Arroces",
  "isSubElaboration": false,
  "price": 18.00,
  "imageUrl": null,
  "ingredients": [
    {
      "ingredientId": "uuid",
      "quantity": 0.5,
      "unit": "kg",
      "sortOrder": 1
    },
    {
      "subRecipeId": "uuid",
      "quantity": 0.2,
      "unit": "kg",
      "notes": "Sofrito del dia",
      "sortOrder": 2
    }
  ]
}
```

| Field | Type | Required | Validation |
|---|---|---|---|
| `name` | string | Yes | Non-empty, max 255 chars |
| `description` | string | No | Max 2000 chars |
| `category` | string | No | Max 100 chars |
| `isSubElaboration` | boolean | No | Default: false |
| `price` | decimal | No | >= 0 |
| `ingredients` | array | No | Each entry must have EITHER `ingredientId` OR `subRecipeId` (not both) |
| `ingredients[].ingredientId` | UUID | Conditional | Must exist in tenant's ingredients |
| `ingredients[].subRecipeId` | UUID | Conditional | Must exist in tenant's recipes |
| `ingredients[].quantity` | decimal | No | >= 0 |
| `ingredients[].unit` | string | No | Max 30 chars |
| `ingredients[].sortOrder` | integer | No | Default: 0 |

**Response 201 Created**: Full recipe object (same as GET /{id})

**Errors**:
- `400 VALIDATION_ERROR`: Both ingredientId and subRecipeId provided, or neither
- `404 RESOURCE_NOT_FOUND`: Referenced ingredient or sub-recipe not found
- `422 CYCLIC_RECIPE_DETECTED`: Adding this sub-recipe would create a cycle

---

### PUT /api/v1/admin/recipes/{id}

Update a recipe.

**Authentication**: JWT -- RESTAURANT_OWNER

**Path Parameters**: `id` (UUID)

**Request**: Same schema as POST (all fields optional)

**Response 200 OK**: Full recipe object

---

### DELETE /api/v1/admin/recipes/{id}

Delete a recipe. Soft-delete (isActive = false) if referenced by dishes.

**Authentication**: JWT -- RESTAURANT_OWNER

**Path Parameters**: `id` (UUID)

**Response 204 No Content**

**Errors**:
- `404 RESOURCE_NOT_FOUND`: Recipe not found

---

## Modulo: Admin -- Digital Cards (`/api/v1/admin/digital-cards`)

> Added in R2 (2026-03-11). Digital menu card management with custom slugs and QR codes.

### POST /api/v1/admin/digital-cards

Create a digital card for a menu.

**Authentication**: JWT -- RESTAURANT_OWNER

**Request**:
```json
{
  "menuId": "uuid",
  "slug": "cobre-y-picon-primavera",
  "customCss": {
    "primaryColor": "#2c3e50",
    "fontFamily": "Roboto",
    "logoPosition": "top-center"
  }
}
```

**Response 201 Created**:
```json
{
  "id": "uuid",
  "menuId": "uuid",
  "slug": "cobre-y-picon-primavera",
  "qrCodeUrl": "https://cdn.apptolast.com/qr/cobre-y-picon-primavera.png",
  "publicUrl": "https://carta.apptolast.com/cobre-y-picon-primavera",
  "isActive": true,
  "customCss": { ... },
  "createdAt": "2026-03-10T10:30:00Z"
}
```

**Errors**:
- `409 CONFLICT`: Slug already taken
- `404 RESOURCE_NOT_FOUND`: Menu not found in tenant

---

### GET /api/v1/admin/digital-cards

List all digital cards for the restaurant.

**Authentication**: JWT -- RESTAURANT_OWNER

**Response 200 OK**: Array of digital card objects

---

### PUT /api/v1/admin/digital-cards/{id}

Update a digital card (slug, CSS, active status).

**Authentication**: JWT -- RESTAURANT_OWNER

---

### DELETE /api/v1/admin/digital-cards/{id}

Deactivate a digital card.

**Authentication**: JWT -- RESTAURANT_OWNER

**Response 204 No Content**

---

## Modulo: Public -- Digital Card (`/carta/{slug}`)

### GET /api/v1/carta/{slug}

Public endpoint to view a restaurant's digital menu card.

**Authentication**: Public (optional JWT for allergen filtering)

**Path Parameters**: `slug` (string)

**Response 200 OK**:
```json
{
  "restaurantName": "Cobre y Picon",
  "restaurantLogo": "https://...",
  "menuName": "Menu de Primavera",
  "customCss": { ... },
  "sections": [
    {
      "name": "Entrantes",
      "recipes": [
        {
          "name": "Ensalada Cesar",
          "description": "...",
          "price": 8.50,
          "imageUrl": null,
          "allergens": [
            { "code": "GLUTEN", "level": "CONTAINS" },
            { "code": "EGGS", "level": "MAY_CONTAIN" }
          ],
          "safetyLevel": "DANGER"
        }
      ]
    }
  ]
}
```

**Errors**:
- `404 RESOURCE_NOT_FOUND`: Slug not found or digital card is inactive

---

## Updated Endpoint Summary (R2)

| Module | Method | Path | Auth | Role |
|---|---|---|---|---|
| **Ingredients** | GET | `/admin/ingredients` | JWT | RESTAURANT_OWNER |
| | POST | `/admin/ingredients` | JWT | RESTAURANT_OWNER |
| | PUT | `/admin/ingredients/{id}` | JWT | RESTAURANT_OWNER |
| | DELETE | `/admin/ingredients/{id}` | JWT | RESTAURANT_OWNER |
| | POST | `/admin/ingredients/analyze-text` | JWT | RESTAURANT_OWNER |
| **Recipes** | GET | `/admin/recipes` | JWT | RESTAURANT_OWNER |
| | GET | `/admin/recipes/{id}` | JWT | RESTAURANT_OWNER |
| | POST | `/admin/recipes` | JWT | RESTAURANT_OWNER |
| | PUT | `/admin/recipes/{id}` | JWT | RESTAURANT_OWNER |
| | DELETE | `/admin/recipes/{id}` | JWT | RESTAURANT_OWNER |
| **Digital Cards** | GET | `/admin/digital-cards` | JWT | RESTAURANT_OWNER |
| | POST | `/admin/digital-cards` | JWT | RESTAURANT_OWNER |
| | PUT | `/admin/digital-cards/{id}` | JWT | RESTAURANT_OWNER |
| | DELETE | `/admin/digital-cards/{id}` | JWT | RESTAURANT_OWNER |
| **Public Card** | GET | `/carta/{slug}` | Public (opt JWT) | -- |
