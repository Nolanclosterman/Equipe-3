# GCP Deployment

Deploy this app as two Cloud Run services:

- `equipe3-backend`: Spring Boot API
- `equipe3-frontend`: Angular static app served by Nginx, proxying `/api` to the backend

Run these commands in Google Cloud Shell from the Google Cloud Console.

## 1. Set variables

```bash
export PROJECT_ID="empire-mapper"
export REGION="europe-west1"
gcloud config set project "$PROJECT_ID"
```

## 2. Enable required APIs

```bash
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com
```

## 3. Create Artifact Registry repository

```bash
gcloud artifacts repositories create equipe3 \
  --repository-format=docker \
  --location="$REGION" \
  --description="Equipe-3 containers"
```

If the repository already exists, continue with the next step.

## 4. Store API keys as secrets

```bash
printf '%s' 'your-anthropic-key' | gcloud secrets create anthropic-api-key --data-file=-
printf '%s' 'your-openai-key' | gcloud secrets create openai-api-key --data-file=-
```

If a secret already exists and needs a new value:

```bash
printf '%s' 'your-new-key' | gcloud secrets versions add anthropic-api-key --data-file=-
printf '%s' 'your-new-key' | gcloud secrets versions add openai-api-key --data-file=-
```

Grant the Cloud Run runtime service account permission to read those secrets:

```bash
export PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"
export RUNTIME_SERVICE_ACCOUNT="$PROJECT_NUMBER-compute@developer.gserviceaccount.com"

gcloud secrets add-iam-policy-binding anthropic-api-key \
  --member="serviceAccount:$RUNTIME_SERVICE_ACCOUNT" \
  --role="roles/secretmanager.secretAccessor"

gcloud secrets add-iam-policy-binding openai-api-key \
  --member="serviceAccount:$RUNTIME_SERVICE_ACCOUNT" \
  --role="roles/secretmanager.secretAccessor"
```

## 5. Build and push the backend image

```bash
gcloud builds submit backend \
  --timeout=30m \
  --tag "$REGION-docker.pkg.dev/$PROJECT_ID/equipe3/backend:latest"
```

## 6. Deploy the backend

```bash
gcloud run deploy equipe3-backend \
  --image "$REGION-docker.pkg.dev/$PROJECT_ID/equipe3/backend:latest" \
  --region "$REGION" \
  --platform managed \
  --allow-unauthenticated \
  --port 8080 \
  --memory 1Gi \
  --cpu 1 \
  --set-secrets ANTHROPIC_API_KEY=anthropic-api-key:latest,OPENAI_API_KEY=openai-api-key:latest \
  --set-env-vars ANTHROPIC_MODEL=claude-opus-4-8,OPENAI_IMAGE_MODEL=gpt-image-1-mini,ASSETS_DIR=/tmp/generated-assets,FRONTEND_ORIGIN=https://*.a.run.app
```

Capture the deployed backend URL:

```bash
export BACKEND_URL="$(gcloud run services describe equipe3-backend --region "$REGION" --format='value(status.url)')"
echo "$BACKEND_URL"
```

## 7. Build and push the frontend image

```bash
gcloud builds submit frontend \
  --timeout=30m \
  --tag "$REGION-docker.pkg.dev/$PROJECT_ID/equipe3/frontend:latest"
```

## 8. Deploy the frontend

```bash
gcloud run deploy equipe3-frontend \
  --image "$REGION-docker.pkg.dev/$PROJECT_ID/equipe3/frontend:latest" \
  --region "$REGION" \
  --platform managed \
  --allow-unauthenticated \
  --port 80 \
  --memory 512Mi \
  --cpu 1 \
  --set-env-vars BACKEND_URL="$BACKEND_URL"
```

Open the frontend URL printed by Cloud Run.

Capture the deployed frontend URL:

```bash
export FRONTEND_URL="$(gcloud run services describe equipe3-frontend --region "$REGION" --format='value(status.url)')"
```

## 9. Smoke test

```bash
curl -I "$FRONTEND_URL"
curl "$FRONTEND_URL/api/profiles/joueur1"
curl -i -X OPTIONS "$FRONTEND_URL/api/profiles/joueur1" \
  -H "Origin: $FRONTEND_URL" \
  -H "Access-Control-Request-Method: POST"
```
