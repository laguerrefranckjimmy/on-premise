#!/bin/bash
set -e

# ===== CONFIGURATION =====
GITHUB_USER="laguerrefranckjimmy"
REPO_NAME="on-premise"
BRANCH="main"

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

DOCKER_COMPOSE_FILE="docker-compose-services.yml"

# ===== CLONE OR UPDATE REPO =====
if [ ! -d "$HOME/$REPO_NAME" ]; then
    echo "Cloning repository..."
    git clone https://github.com/$GITHUB_USER/$REPO_NAME.git
else
    echo "Repository exists. Pulling latest changes..."
    cd $HOME/$REPO_NAME
    git checkout $BRANCH
    git pull origin $BRANCH
fi

cd $HOME/$REPO_NAME

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
mvn clean package -DskipTests
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

# ===== LOGIN TO GHCR =====
echo $GHCR_PAT | docker login ghcr.io -u $GHCR_USER --password-stdin

# ===== BUILD AND PUSH DOCKER IMAGES =====
declare -A IMAGES
IMAGES=(
    ["spring-api"]=$SPRING_DOCKERFILE
    ["vertx-service"]=$VERTX_DOCKERFILE
    ["react-app"]=$REACT_DOCKERFILE
)

for service in "${!IMAGES[@]}"; do
    echo "Processing $service..."
    case $service in
        "spring-api")
            if [ ! -f "$SPRING_JAR" ]; then
                echo "Skipping $service Docker build: JAR not found."
                continue
            fi
            docker build -t ghcr.io/$GHCR_USER/$service:latest -f $SPRING_DOCKERFILE $SPRING_DIR
            ;;
        "vertx-service")
            if [ ! -f "$VERTX_JAR" ]; then
                echo "Skipping $service Docker build: JAR not found."
                continue
            fi
            docker build -t ghcr.io/$GHCR_USER/$service:latest -f $VERTX_DOCKERFILE $VERTX_DIR
            ;;
        "react-app")
            if [ ! -d "$REACT_DIR/build" ]; then
                echo "Skipping $service Docker build: build directory not found."
                continue
            fi
            docker build -t ghcr.io/$GHCR_USER/$service:latest -f $REACT_DOCKERFILE $REACT_DIR
            ;;
    esac
    docker push ghcr.io/$GHCR_USER/$service:latest
done

echo "✅ Docker images built and pushed!"

# ===== START SERVICES USING EXISTING DOCKER COMPOSE FILE =====
echo "Starting Couchbase & Kafka..."
for service in couchbase kafka; do
    if [ "$(docker ps -q -f name=$service)" ]; then
        echo "$service already running, skipping..."
    else
        echo "Starting $service..."
        docker compose -f $DOCKER_COMPOSE_FILE up -d $service
    fi
done

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

# ===== WAIT FOR DEPLOYMENTS =====
kubectl -n $K8S_NAMESPACE rollout status deployment/$K8S_DEPLOY_SPRING
kubectl -n $K8S_NAMESPACE rollout status deployment/$K8S_DEPLOY_VERTX
kubectl -n $K8S_NAMESPACE rollout status deployment/$K8S_DEPLOY_REACT

# ===== LIST PODS & SERVICES =====
kubectl -n $K8S_NAMESPACE get pods,svc

# ===== PRINT ACCESS URLS =====
echo "✅ All deployments updated successfully!"
echo "React frontend: http://app.$VM_IP.$NIP_DOMAIN"
echo "Spring Boot API: http://api.$VM_IP.$NIP_DOMAIN/spring/api/items"
echo "Vert.x stream: http://vertx.$VM_IP.$NIP_DOMAIN/stream"
echo "Couchbase UI: http://$VM_IP:8091"
echo "Kafka broker: $VM_IP:9092"
