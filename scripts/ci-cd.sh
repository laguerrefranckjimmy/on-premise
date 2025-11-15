#!/bin/bash
set -e

# ===== CONFIGURATION =====
GHCR_USER="laguerrefranckjimmy"

SPRING_DIR="spring-api"
VERTX_DIR="vertx-service"
REACT_DIR="react-app"

SPRING_DOCKERFILE="docker/Dockerfile.spring"
VERTX_DOCKERFILE="docker/Dockerfile.vertx"
REACT_DOCKERFILE="docker/Dockerfile.react"

K8S_NAMESPACE="learning01"
K8S_DEPLOY_SPRING="spring-api"
K8S_DEPLOY_VERTX="vertx"
K8S_DEPLOY_REACT="react"

VM_IP="10.0.2.15"
NIP_DOMAIN="nip.io"

INGRESS_FILE="k8s/ingress.yaml"
INGRESS_TMP="k8s/ingress.tmp.yaml"

# ===== CLEANUP FUNCTION =====
cleanup_docker() {
    echo "🧹 Cleaning up old Docker resources..."
    docker image prune -af
    docker container prune -f
    docker volume prune -f
    docker network prune -f
    echo "✅ Docker cleanup complete!"
}

# ===== PARALLEL BUILDS =====
echo "🏗️ Building Spring Boot, Vert.x, and React in parallel..."

(
  echo "⚙️ Building Spring Boot..."
  cd $SPRING_DIR
  mvn -B -DskipTests clean package
) &

(
  echo "⚙️ Building Vert.x..."
  cd $VERTX_DIR
  mvn -B -DskipTests clean package
) &

(
  echo "⚙️ Building React frontend..."
  cd $REACT_DIR
  npm ci --prefer-offline --no-audit
  npm run build
) &

wait
echo "✅ All components built successfully!"

# ===== DETECT JAR FILES =====
SPRING_JAR=$(find $SPRING_DIR/target -name "*.jar" | head -n1)
VERTX_JAR=$(find $VERTX_DIR/target -name "*.jar" | head -n1)
echo "Spring Boot JAR: $SPRING_JAR"
echo "Vert.x JAR: $VERTX_JAR"

# ===== BUILD DOCKER IMAGES =====
declare -A IMAGES=(
  ["spring-api"]=$SPRING_DOCKERFILE
  ["vertx-service"]=$VERTX_DOCKERFILE
  ["react-app"]=$REACT_DOCKERFILE
)

for service in "${!IMAGES[@]}"; do
  echo "🐳 Building Docker image for $service..."
  case $service in
    "spring-api")
      docker build -t $service:latest -f $SPRING_DOCKERFILE $SPRING_DIR
      ;;
    "vertx-service")
      docker build -t $service:latest -f $VERTX_DOCKERFILE $VERTX_DIR
      ;;
    "react-app")
      docker build -t $service:latest -f $REACT_DOCKERFILE $REACT_DIR
      ;;
  esac
done

echo "✅ All Docker images built!"

# ===== IMPORT INTO K3S =====
echo "📦 Importing images into K3s..."
SUDO=""
[ "$EUID" -ne 0 ] && SUDO="sudo"

for service in "${!IMAGES[@]}"; do
  TAR_FILE="/tmp/${service}.tar"
  docker save $service:latest -o $TAR_FILE
  $SUDO k3s ctr images import $TAR_FILE
  rm -f $TAR_FILE
done
echo "✅ Imported into K3s!"

# ===== DEPLOY TO K8S =====
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/deploy-spring.yaml
kubectl apply -f k8s/deploy-vertx.yaml
kubectl apply -f k8s/deploy-react.yaml

# ===== UPDATE INGRESS HOSTS =====
if [ -f "$INGRESS_FILE" ]; then
  sed -e "s/APP_HOST/app.$VM_IP.$NIP_DOMAIN/" \
      -e "s/API_HOST/api.$VM_IP.$NIP_DOMAIN/" \
      -e "s/VERTX_HOST/vertx.$VM_IP.$NIP_DOMAIN/" \
      "$INGRESS_FILE" > "$INGRESS_TMP"
  kubectl apply -f "$INGRESS_TMP"
fi

# ===== ROLLOUT RESTART =====
for DEPLOY in $K8S_DEPLOY_SPRING $K8S_DEPLOY_VERTX $K8S_DEPLOY_REACT; do
  kubectl -n $K8S_NAMESPACE rollout restart deployment/$DEPLOY
  kubectl -n $K8S_NAMESPACE rollout status deployment/$DEPLOY
done

kubectl -n $K8S_NAMESPACE get pods,svc

cleanup_docker
