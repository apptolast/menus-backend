# Guía de Despliegue — Menus Backend

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

O usa External Secrets Operator (recomendado para producción):

```bash
kubectl create secret generic menus-backend-secret \
  --from-literal=DATABASE_URL="jdbc:postgresql://postgres:5432/menusdb" \
  --from-literal=DATABASE_USERNAME="menus" \
  --from-literal=DATABASE_PASSWORD="<password>" \
  --from-literal=JWT_SECRET="<64+ bytes secret>" \
  --from-literal=ENCRYPTION_KEY="<32 bytes key>" \
  --from-literal=GOOGLE_CLIENT_ID="<client-id>" \
  --from-literal=GOOGLE_CLIENT_SECRET="<client-secret>" \
  -n menus-backend
```

---

## 2. Crear Secret para GHCR

```bash
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=<github-username> \
  --docker-password=<github-pat> \
  -n menus-backend
```

---

## 3. Aplicar Manifiestos K8s (en orden)

```bash
# 1. Namespace
kubectl apply -f k8s/namespace.yaml

# 2. Storage
kubectl apply -f k8s/pvc.yaml

# 3. Configuración
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml

# 4. Base de datos
kubectl apply -f k8s/postgres.yaml
kubectl wait --for=condition=ready pod -l app=postgres -n menus-backend --timeout=120s

# 5. Aplicación
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

# 6. Ingress y middleware (elegir dev o prod)
kubectl apply -f k8s/middleware.yaml
kubectl apply -f k8s/ingress-dev.yaml   # o ingress-prod.yaml

# 7. Verificar
kubectl get pods -n menus-backend
kubectl logs -l app=menus-backend -n menus-backend --tail=50
```

---

## 4. Verificar Despliegue

```bash
# Health check
curl https://menus-api-dev.apptolast.com/actuator/health

# Listar alérgenos (endpoint público)
curl https://menus-api-dev.apptolast.com/api/v1/allergens

# Swagger UI
open https://menus-api-dev.apptolast.com/swagger-ui/index.html
```

---

## 5. CI/CD con GitHub Actions

### Flujo automático:
1. Push a `feature/**` o `develop` → ejecuta **CI** (build + tests + quality gates)
2. Push a `main` → ejecuta **CD** (build Docker image → push a ghcr.io → deploy a dev)

### Variables requeridas en GitHub Actions:
- `KUBE_CONFIG_DEV`: kubeconfig del cluster de desarrollo (secret)

---

## 6. Actualizar Imagen en K8s

```bash
# Forzar redeploy con nueva imagen
kubectl rollout restart deployment/menus-backend -n menus-backend
kubectl rollout status deployment/menus-backend -n menus-backend
```

---

## 7. Base de Datos

### Flyway migraciones
Las migraciones se ejecutan automáticamente al arrancar la aplicación. Versiones V1–V9:
- V1: extensiones pgcrypto + uuid-ossp
- V2–V7: tablas de dominio
- V8: Row-Level Security (RLS)
- V9: seed 14 alérgenos EU

### Backup PostgreSQL
```bash
kubectl exec -n menus-backend deploy/postgres -- \
  pg_dump -U menus menusdb > backup_$(date +%Y%m%d).sql
```

---

## 8. Troubleshooting

| Síntoma | Posible causa | Solución |
|---|---|---|
| Pod en CrashLoopBackOff | Secret mal configurado | `kubectl logs <pod> -n menus-backend` |
| 401 en todas las rutas | JWT_SECRET incorrecto | Verificar secret y reiniciar pod |
| Flyway falla al arrancar | PostgreSQL no ready | Verificar readinessProbe de postgres |
| RLS bloquea todas las queries | `app.current_tenant` no configurado | Verificar TenantFilter en logs |
| 503 desde Traefik | Service no encontrado | `kubectl get svc -n menus-backend` |
