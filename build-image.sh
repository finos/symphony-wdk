GIT_HASH=$(git rev-parse --verify --short  HEAD)
IMAGE_NAME='workflow-bot'

# Build and push image do GCP
gcp_image="gcr.io/sym-dev-plat/${IMAGE_NAME}:${GIT_HASH}"

echo "[workflow-bot] Build workflow-bot image"
./gradlew bootBuildImage

echo "[workflow-bot] Tag bot as ${gcp_image}"
docker tag ${IMAGE_NAME} ${gcp_image}

echo "[workflow-bot] Push to GCP"
gcloud auth configure-docker gcr.io
docker -- push ${gcp_image}
echo "[workflow-bot] Image ${gcp_image} pushed to GCP"

echo "[workflow-bot] Deploying into Kubernetes cluster"
gcloud container clusters get-credentials devx-autopilot-private-cluster --zone us-central1-b --project sym-dev-plat
kubectl -n dev set image deployment/workflow-bot workflow-bot-1=${gcp_image}
