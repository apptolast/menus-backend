# Guia de Despliegue -- Menus Backend

> **Estado del proyecto:** Esta guia describe la arquitectura de despliegue planificada.
> Algunos manifiestos K8s o pasos de CI/CD pueden estar en desarrollo o pendientes de
> configuracion final. Verifica los archivos en `k8s/` y `.github/workflows/` para
> confirmar el estado actual.

## Prerrequisitos

- Cluster Kubernetes con Rancher
- Traefik instalado como Ingress Controller
- Longhorn instalado para almacenamiento persistente
- cert-manager con ClusterIssuer `letsencrypt` configurado
- Acceso a GitHub Container Registry (`ghcr.io/apptolast/menus-backend`)

---

## 1. Configurar Secrets

**Nunca** commites valores reales. Edita `k8s/secret.yaml` con valores base64 reales:

```bash
# Generar base64 para cada valor
echo -n "jdbc:postgresql://postgres:5432/menusdb" | base64
echo -n "menus" | base64
echo -n "tu-password-segura" | base64
echo -n "tu-jwt-secret-de-al-menos-64-bytes-de-longitud-minima" | base64
echo -n "tu-clave-aes-de-32bytes!" | base64
echo -n "tu-google-client-id.apps.googleusercontent.com" | base64
echo -n "tu-google-client-secret" | base64
```

O usa External Secrets Operator (recomendado para produccion):

```bash
# Para el entorno de desarrollo:
kubectl create secret generic menus-backend-secret \
  --from-literal=DATABASE_URL="jdbc:postgresql://postgres:5432/menusdb" \
  --from-literal=DATABASE_USERNAME="menus" \
  --from-literal=DATABASE_PASSWORD="<password>" \
  --from-literal=JWT_SECRET="<64+ bytes secret>" \
  --from-literal=ENCRYPTION_KEY="<32 bytes key>" \
  --from-literal=GOOGLE_CLIENT_ID="<client-id>" \
  --from-literal=GOOGLE_CLIENT_SECRET="<client-secret>" \
  -n apptolast-menus-dev

# Para produccion:
kubectl create secret generic menus-backend-secret \
  --from-literal=DATABASE_URL="jdbc:postgresql://postgres:5432/menusdb" \
  --from-literal=DATABASE_USERNAME="menus" \
  --from-literal=DATABASE_PASSWORD="<password>" \
  --from-literal=JWT_SECRET="<64+ bytes secret>" \
  --from-literal=ENCRYPTION_KEY="<32 bytes key>" \
  --from-literal=GOOGLE_CLIENT_ID="<client-id>" \
  --from-literal=GOOGLE_CLIENT_SECRET="<client-secret>" \
  -n apptolast-menus-prod
```

### Variables de entorno requeridas

| Variable | Descripcion | Ejemplo |
|---|---|---|
| `DATABASE_URL` | JDBC URL de PostgreSQL | `jdbc:postgresql://postgres:5432/menusdb` |
| `DATABASE_USERNAME` | Usuario de la base de datos | `menus` |
| `DATABASE_PASSWORD` | Password de la base de datos | (secreto) |
| `JWT_SECRET` | Clave para firmar JWT, minimo 64 bytes | (secreto, base64) |
| `ENCRYPTION_KEY` | Clave AES-256 para cifrado pgcrypto, 32 bytes | (secreto) |
| `GOOGLE_CLIENT_ID` | OAuth2 client ID de Google | `xxx.apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | OAuth2 client secret de Google | (secreto) |

---

## 2. Crear Secret para GHCR

```bash
# Para desarrollo:
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=<github-username> \
  --docker-password=<github-pat> \
  -n apptolast-menus-dev

# Para produccion:
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=<github-username> \
  --docker-password=<github-pat> \
  -n apptolast-menus-prod
```

---

## 3. Aplicar Manifiestos K8s (en orden)

> **Nota:** Los manifiestos en `k8s/` actualmente usan el namespace `menus-backend` en algunos
> archivos. El namespace definitivo del proyecto es `apptolast-menus-dev` (desarrollo) y
> `apptolast-menus-prod` (produccion), como se define en `k8s/namespace.yaml`. Verifica que
> los manifiestos sean consistentes antes de aplicarlos.

```bash
# 1. Namespaces (crea apptolast-menus-dev y apptolast-menus-prod)
kubectl apply -f k8s/namespace.yaml

# 2. Storage
kubectl apply -f k8s/pvc.yaml

# 3. Configuracion
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml

# 4. Base de datos
kubectl apply -f k8s/postgres.yaml
kubectl wait --for=condition=ready pod -l app=postgres -n apptolast-menus-dev --timeout=120s

# 5. Aplicacion
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

# 6. Ingress y middleware (elegir dev o prod)
kubectl apply -f k8s/middleware.yaml
kubectl apply -f k8s/ingress-dev.yaml   # o ingress-prod.yaml

# 7. Verificar
kubectl get pods -n apptolast-menus-dev
kubectl logs -l app=menus-backend -n apptolast-menus-dev --tail=50
```

---

## 4. Verificar Despliegue

```bash
# Health check
curl https://menus-api-dev.apptolast.com/actuator/health

# Listar alergenos (endpoint publico)
curl https://menus-api-dev.apptolast.com/api/v1/allergens

# Swagger UI
open https://menus-api-dev.apptolast.com/swagger-ui/index.html
```

---

## 5. CI/CD con GitHub Actions

### Flujo automatico:
1. Push a `feature/**` o `develop` -> ejecuta **CI** (build + tests + quality gates)
2. Push a `main` -> ejecuta **CD** (build Docker image -> push a ghcr.io -> deploy a dev)

### Variables requeridas en GitHub Actions:

| Variable/Secret | Descripcion |
|---|---|
| `KUBE_CONFIG_DEV` | kubeconfig del cluster de desarrollo (secret) |
| `KUBE_CONFIG_PROD` | kubeconfig del cluster de produccion (secret) |
| `GHCR_TOKEN` | Token con permisos `write:packages` para push a ghcr.io |

---

## 6. Actualizar Imagen en K8s

```bash
# Forzar redeploy con nueva imagen
kubectl rollout restart deployment/menus-backend -n apptolast-menus-dev
kubectl rollout status deployment/menus-backend -n apptolast-menus-dev

# Para produccion:
kubectl rollout restart deployment/menus-backend -n apptolast-menus-prod
kubectl rollout status deployment/menus-backend -n apptolast-menus-prod
```

---

## 7. Base de Datos

### Flyway migraciones
Las migraciones se ejecutan automaticamente al arrancar la aplicacion. Versiones V1-V9:
- V1: extensiones pgcrypto + uuid-ossp
- V2: tablas de referencia (allergen, allergen_translation)
- V3: tablas de usuario (user_account, user_allergen_profile, consent_record, oauth_account)
- V4: tablas de restaurante (restaurant, subscription)
- V5: tablas de menu (menu, menu_section)
- V6: tablas de platos (dish, dish_allergen)
- V7: tabla de auditoria (allergen_audit_log)
- V8: Row-Level Security (RLS) policies
- V9: seed 14 alergenos EU con traducciones (ES/EN/CA/EU/GL)

### Backup PostgreSQL
```bash
kubectl exec -n apptolast-menus-dev deploy/postgres -- \
  pg_dump -U menus menusdb > backup_$(date +%Y%m%d).sql
```

---

## 8. Troubleshooting

| Sintoma | Posible causa | Solucion |
|---|---|---|
| Pod en CrashLoopBackOff | Secret mal configurado | `kubectl logs <pod> -n apptolast-menus-dev` |
| 401 en todas las rutas | JWT_SECRET incorrecto | Verificar secret y reiniciar pod |
| Flyway falla al arrancar | PostgreSQL no ready | Verificar readinessProbe de postgres |
| RLS bloquea todas las queries | `app.current_tenant` no configurado | Verificar TenantFilter en logs |
| 503 desde Traefik | Service no encontrado | `kubectl get svc -n apptolast-menus-dev` |
| Namespace mismatch | Manifiestos usan namespace incorrecto | Verificar que todos los manifiestos usen `apptolast-menus-dev` o `apptolast-menus-prod` |
| ImagePullBackOff | GHCR secret no creado | `kubectl get secret ghcr-secret -n apptolast-menus-dev` |
