//Final Pipeline Of Angular to ngnix

pipeline {
    agent { label 'dhruv_ubantu' } // Use the specific agent

    tools {
        nodejs 'node 18'  // Ensure Node.js 18 is available
    }

    environment {
        NEXUS_REPOSITORY = 'http://localhost:8081/repository/windows' 
        NEXUS_CREDENTIALS_ID = 'nexus_dhruv'
        ansible_password = "ANSIBLE_PASSWORD"
    }

    stages {
        stage('Checkout Code') {
            steps {
                git branch: 'main', url: ''
            }
        }

        stage('Install Dependencies') {
            steps {
                sh 'npm install'
            }
        }

        stage('Build Angular App') {
            steps {
                sh 'npm run build'
            }
        }

        stage('Zip Artifact') {
            steps {
                script {
                    def timestamp = new Date().format("yyyyMMddHHmmss")
                    def artifactPath = "dist"
                    def zipFileName = "dist-${timestamp}.zip"
                    sh "zip -r ${zipFileName} ${artifactPath}"
                    env.ARTIFACT_PATH = zipFileName
                }
            }
        }

        stage('Upload to Nexus') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: env.NEXUS_CREDENTIALS_ID, usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
                        def nexusUrl = "${NEXUS_REPOSITORY}/${env.ARTIFACT_PATH}" 
                        sh """
                        curl -v -u $NEXUS_USERNAME:$NEXUS_PASSWORD --upload-file ${env.ARTIFACT_PATH} ${nexusUrl}
                        """
                    }
                }
            }
        }

        stage('Run Ansible Playbook') {
            steps {
                
                    sh "ansible-playbook -i hosts /home/dhruv/nginx_playbook.yml --extra-vars $ansible_password" 
            }
        }
    }

    post {
        always {
            echo 'Build finished'
        }
        success {
            echo 'Build succeeded'
        }
        failure {
            echo 'Build failed'
        }
    }
}
