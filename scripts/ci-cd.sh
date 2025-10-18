#!/bin/bash
set -e

# ===== CONFIGURATION =====
GHCR_USER="laguerrefranckjimmy"
# export GHCR_PAT=<your_personal_access_token>

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

VM_IP="10.0.2.15"   # Ubuntu VM IP
NIP_DOMAIN="nip.io"

INGRESS_FILE="k8s/ingress.yaml"
INGRESS_TMP="k8s/ingress.tmp.yaml"

# ===== CLEANUP FUNCTION =====
cleanup_docker() {
    echo "🧹 Cleaning up old and dangling Docker images..."
    docker image prune -af
    docker container prune -f
    docker volume prune -f
    docker network prune -f
    echo "✅ Docker cleanup complete!"
}

# ===== CLEANUP BEFORE BUILD =====
cleanup_docker

# ===== BUILD PROJECTS =====
echo "Building Spring Boot..."
cd $SPRING_DIR
if [ -f "./mvnw" ]; then
    ./mvnw clean package -DskipTests
else
    mvn clean package -DskipTests
fi
cd ..

echo "Building Vert.x..."
cd $VERTX_DIR
if [ -f "./mvnw" ]; then
    ./mvnw clean package -DskipTests
else
    mvn clean package -DskipTests
fi
cd ..

echo "Building React..."
cd $REACT_DIR
npm install
npm run build
cd ..

# ===== DETECT JAR FILES =====
SPRING_JAR=$(find $SPRING_DIR/target -name "*.jar" | head -n1)
VERTX_JAR=$(find $VERTX_DIR/target -name "*.jar" | head -n1)
echo "Spring JAR: $SPRING_JAR"
echo "Vert.x JAR: $VERTX_JAR"

# ===== VERIFY JAR FILES =====
if ! jar tf "$SPRING_JAR" | grep -q "Main-Class"; then
    echo "❌ Spring Boot JAR is missing Main-Class. Check your Maven configuration."
    exit 1
fi

if ! jar tf "$VERTX_JAR" | grep -q "Main-Class"; then
    echo "❌ Vert.x JAR is missing Main-Class. Check your Maven configuration."
    exit 1
fi

# ===== BUILD DOCKER IMAGES =====
declare -A IMAGES
IMAGES=(
    ["spring-api"]=$SPRING_DOCKERFILE
    ["vertx-service"]=$VERTX_DOCKERFILE
    ["react-app"]=$REACT_DOCKERFILE
)

for service in "${!IMAGES[@]}"; do
    echo "Building image for $service..."
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

echo "✅ Docker images built locally!"

# ===== OPTIONAL: PUSH TO GHCR IF TOKEN AVAILABLE =====
if [ -n "$GHCR_PAT" ]; then
    echo "🔐 GHCR_PAT found — pushing images to GHCR..."
    echo $GHCR_PAT | docker login ghcr.io -u $GHCR_USER --password-stdin
    for service in "${!IMAGES[@]}"; do
        docker tag $service:latest ghcr.io/$GHCR_USER/$service:latest
        docker push ghcr.io/$GHCR_USER/$service:latest
    done
else
    echo "⚠️ GHCR_PAT not set — skipping push to GHCR."
fi

# ===== IMPORT IMAGES INTO K3S =====
echo "📦 Importing images into K3s..."
SUDO=""
if [ "$EUID" -ne 0 ]; then
    SUDO="sudo"
fi

for service in "${!IMAGES[@]}"; do
    TAR_FILE="/tmp/${service}.tar"
    docker save $service:latest -o $TAR_FILE
    $SUDO k3s ctr images import $TAR_FILE
    rm -f $TAR_FILE
done
echo "✅ Images imported into K3s!"

# ===== APPLY K8S MANIFESTS =====
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/deploy-spring.yaml
kubectl apply -f k8s/deploy-vertx.yaml
kubectl apply -f k8s/deploy-react.yaml

# ===== UPDATE INGRESS HOSTNAMES =====
if [ -f "$INGRESS_FILE" ]; then
    sed -e "s/APP_HOST/app.$VM_IP.$NIP_DOMAIN/" \
        -e "s/API_HOST/api.$VM_IP.$NIP_DOMAIN/" \
        -e "s/VERTX_HOST/vertx.$VM_IP.$NIP_DOMAIN/" \
        $INGRESS_FILE > $INGRESS_TMP

    echo "Applying updated Ingress..."
    kubectl apply -f $INGRESS_TMP
fi

# ===== RESTART & WAIT FOR DEPLOYMENTS =====
kubectl -n $K8S_NAMESPACE rollout restart deployment/$K8S_DEPLOY_SPRING
kubectl -n $K8S_NAMESPACE rollout restart deployment/$K8S_DEPLOY_VERTX
kubectl -n $K8S_NAMESPACE rollout restart deployment/$K8S_DEPLOY_REACT

kubectl -n $K8S_NAMESPACE rollout status deployment/$K8S_DEPLOY_SPRING
kubectl -n $K8S_NAMESPACE rollout status deployment/$K8S_DEPLOY_VERTX
kubectl -n $K8S_NAMESPACE rollout status deployment/$K8S_DEPLOY_REACT

# ===== LIST PODS & SERVICES =====
kubectl -n $K8S_NAMESPACE get pods,svc

# ===== CLEANUP AFTER BUILD =====
cleanup_docker

# ===== PRINT ACCESS URLS =====
echo "✅ All deployments updated successfully!"
echo "React frontend: http://app.$VM_IP.$NIP_DOMAIN"
echo "Spring Boot API: http://api.$VM_IP.$NIP_DOMAIN/spring/api/items"
echo "Vert.x stream: http://vertx.$VM_IP.$NIP_DOMAIN/stream"