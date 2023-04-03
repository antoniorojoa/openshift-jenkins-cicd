pipeline {
      agent {
        label 'maven'
      }

      stages {
        // ---------------------------------------
        // -- STAGE: Git clone
        // ---------------------------------------
        stage('Git clone') {
          steps {
            git branch: 'main', url: 'https://github.com/antoniorojoa/openshift-jenkins-cicd.git'
          }
        }

        // ---------------------------------------
        // -- STAGE: Read pom
        // ---------------------------------------
        stage("Read pom") {
          steps {
            script {
              IMAGE = readMavenPom().getArtifactId()
              VERSION = readMavenPom().getVersion() 
              println " version - ${VERSION} - ${IMAGE}"
            }
          }
        }

        // ---------------------------------------
        // -- STAGE: Compile
        // ---------------------------------------
        stage("Compile") {
          steps {
            script {
              sh "mvn clean compile -Dmaven.test.skip=true"  
            }   
          }
        }

        // ---------------------------------------
        // -- STAGE: Test
        // ---------------------------------------
        stage("Test") {
          steps {
            script {
              sh "mvn clean package test verify"
            }   
          }
        }

        // ---------------------------------------
        // -- STAGE: Sonarqube
        // ---------------------------------------
        stage("Sonarqube") {
          steps {
            script {
              echo "Analizing the project with SonarQube."
              sh "mvn sonar:sonar -Dsonar.host.url=http://sonar.sonarqube.svc.cluster.local:9000 -Dsonar.projectKey=openshift-jenkins-cicd -Dsonar.login=sqa_6a1a0dd1ac95bc0ecc7999ad550d8aa8da6a9c39"
            }   
          }
        }

        // ---------------------------------------
        // -- STAGE: Install
        // ---------------------------------------
        stage("Install") {
          steps {
            script {
              sh "mvn clean install -Dmaven.test.skip=true"  
            }
          }
        }

        // ---------------------------------------
        // -- STAGE: Create Image Builder
        // ---------------------------------------
        stage('Create Image Builder') {
          when {
            expression {
              openshift.withCluster() {
                openshift.withProject() {
                  return !openshift.selector("bc", "sample-app-jenkins").exists();
                }
              }
            }
          }
          steps {
            script {
              openshift.withCluster() {
                openshift.withProject() {
                  openshift.newBuild("--name=sample-app-jenkins", "--image-stream=redhat-openjdk18-openshift:1.12", "--binary=true")
                }
              }
            }
          }
        }

        // ---------------------------------------
        // -- STAGE: Build Image
        // ---------------------------------------
        stage('Build Image') {
          steps {
            sh "rm -rf ocp && mkdir -p ocp/deployments"
            sh "pwd && ls -la target "
            sh "cp target/openshiftjenkins-0.0.2-SNAPSHOT.jar ocp/deployments"
            script {
              openshift.withCluster() {
                openshift.withProject() {
                  openshift.selector("bc", "sample-app-jenkins").startBuild("--from-dir=./ocp","--follow", "--wait=true")
                }
              }
            }
          }
        }

        // ---------------------------------------
        // -- STAGE: Deploy
        // ---------------------------------------
        stage('Deploy') {
          when {
            expression {
              openshift.withCluster() {
                openshift.withProject() {
                  return !openshift.selector('dc', 'sample-app-jenkins').exists()
                }
              }
            }
          }
          steps {
            script {
              openshift.withCluster() {
                openshift.withProject() {
                  def app = openshift.newApp("sample-app-jenkins", "--as-deployment-config")
                  app.narrow("svc").expose();
                }
              }
            }
          }
        }
      }
    }
