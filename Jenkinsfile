pipeline {
    agent any
    
    environment {
        SONAR_HOME = tool "sonar-scanner"
        AWS_ACCOUNT_ID = "093435167670YOUR_AWS_ACCOUNT_ID"
        AWS_REGION = "ap-south-1"
        ECR_REPO = "shopeasy-app"
        IMAGE_TAG = "${BUILD_NUMBER}"
        ECR_URI = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}"
        CLUSTER_NAME = "shopeasy-cluster"
    }
    
    stages {

        stage("Clone Code") {
            steps {
                git branch: 'main',
                url: 'https://github.com/centurion-ck/shopeasy-app.git'
                echo "✅ Code cloned successfully!"
            }
        }

        stage("Maven Build") {
            steps {
                sh "mvn clean package -DskipTests=true"
                echo "✅ Build successful!"
            }
        }

        stage("Unit Tests") {
            steps {
                sh "mvn test"
                echo "✅ Tests passed!"
            }
        }

        stage("SonarQube Code Quality Scan") {
            steps {
                withSonarQubeEnv("sonarqube-server") {
                    sh """
                        mvn sonar:sonar \
                        -Dsonar.projectKey=shopeasy-app \
                        -Dsonar.projectName=shopeasy-app
                    """
                }
                echo "✅ SonarQube scan completed!"
            }
        }

        stage("Quality Gate Check") {
            steps {
                timeout(time: 2, unit: "MINUTES") {
                    waitForQualityGate abortPipeline: false
                }
                echo "✅ Quality gate passed!"
            }
        }

        stage("OWASP Dependency Check") {
            steps {
                dependencyCheck additionalArguments: '--scan ./ --format HTML --out dependency-check-report.html', odcInstallation: 'owasp-dc'
                echo "✅ OWASP scan completed!"
            }
        }

        stage("Trivy File System Scan") {
            steps {
                sh "trivy fs --format table -o trivy-fs-report.html ."
                echo "✅ Trivy FS scan completed!"
            }
        }

        stage("Docker Build") {
            steps {
                sh "docker build -t ${ECR_REPO}:${IMAGE_TAG} ."
                echo "✅ Docker image built!"
            }
        }

        stage("Trivy Docker Image Scan") {
            steps {
                sh "trivy image --format table -o trivy-image-report.html ${ECR_REPO}:${IMAGE_TAG}"
                echo "✅ Docker image scanned!"
            }
        }

        stage("Push to AWS ECR") {
            steps {
                sh """
                    aws ecr get-login-password --region ${AWS_REGION} | \
                    docker login --username AWS \
                    --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com

                    docker tag ${ECR_REPO}:${IMAGE_TAG} ${ECR_URI}:${IMAGE_TAG}
                    docker tag ${ECR_REPO}:${IMAGE_TAG} ${ECR_URI}:latest

                    docker push ${ECR_URI}:${IMAGE_TAG}
                    docker push ${ECR_URI}:latest
                """
                echo "✅ Image pushed to ECR!"
            }
        }

        stage("Update K8s Manifests") {
            steps {
                sh """
                    sed -i 's|REPLACE_ECR_URI|${ECR_URI}|g' kubernetes/dev/deployment.yaml
                    sed -i 's|REPLACE_ECR_URI|${ECR_URI}|g' kubernetes/staging/deployment.yaml
                    sed -i 's|REPLACE_ECR_URI|${ECR_URI}|g' kubernetes/production/deployment.yaml
                """
                echo "✅ Manifests updated!"
            }
        }

        stage("Deploy to DEV") {
            steps {
                sh """
                    aws eks update-kubeconfig --region ${AWS_REGION} --name ${CLUSTER_NAME}
                    kubectl apply -f kubernetes/dev/deployment.yaml
                    kubectl apply -f kubernetes/dev/service.yaml
                    kubectl rollout status deployment/shopeasy-deployment -n dev
                """
                echo "✅ Deployed to DEV!"
            }
        }

        stage("DEV Smoke Test") {
            steps {
                sh """
                    sleep 30
                    kubectl get pods -n dev
                    kubectl get svc -n dev
                """
                echo "✅ DEV smoke test passed!"
            }
        }

        stage("Deploy to STAGING") {
            steps {
                sh """
                    kubectl apply -f kubernetes/staging/deployment.yaml
                    kubectl apply -f kubernetes/staging/service.yaml
                    kubectl rollout status deployment/shopeasy-deployment -n staging
                """
                echo "✅ Deployed to STAGING!"
            }
        }

        stage("STAGING Smoke Test") {
            steps {
                sh """
                    sleep 30
                    kubectl get pods -n staging
                    kubectl get svc -n staging
                """
                echo "✅ STAGING smoke test passed!"
            }
        }

        stage("Approval for PRODUCTION") {
            steps {
                timeout(time: 30, unit: "MINUTES") {
                    input message: "🚨 PRODUCTION DEPLOYMENT APPROVAL REQUIRED! Do you approve?",
                          ok: "Yes Deploy to Production!",
                          submitter: "admin"
                }
                echo "✅ Production approved!"
            }
        }

        stage("Deploy to PRODUCTION") {
            steps {
                sh """
                    kubectl apply -f kubernetes/production/deployment.yaml
                    kubectl apply -f kubernetes/production/service.yaml
                    kubectl apply -f kubernetes/production/hpa.yaml
                    kubectl rollout status deployment/shopeasy-deployment -n production
                """
                echo "✅ Deployed to PRODUCTION!"
            }
        }

        stage("Production Smoke Test") {
            steps {
                sh """
                    sleep 30
                    kubectl get pods -n production
                    kubectl get svc -n production
                    kubectl get hpa -n production
                """
                echo "✅ Production smoke test passed!"
            }
        }
    }

    post {
        success {
            echo "🎉 Pipeline SUCCESS! ShopEasy deployed to Production!"
        }
        failure {
            echo "❌ Pipeline FAILED! Rolling back!"
            sh "kubectl rollout undo deployment/shopeasy-deployment -n production || true"
        }
    }
}