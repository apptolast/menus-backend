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
