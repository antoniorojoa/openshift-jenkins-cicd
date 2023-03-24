pipeline
    {
       agent {
            label 'maven'
        }

        stages
        {
          stage('Git clone') {
            steps
             {
              checkout([$class: 'GitSCM', branches: [[name: '*/develop']], extensions: [[$class: 'SparseCheckoutPaths', sparseCheckoutPaths: [[path: '/openshift-jenkins-cicd']]]], userRemoteConfigs: [[credentialsId: 'bf6f7b2d-a082-4f4b-ae79-2f44ee3aa774', url: 'https://gitlab.alten.es/laboratorio_qa_devops/openshift-pipelines.git']]])
	      //git branch: 'main', url: 'https://github.com/antoniorojoa/openshift-jenkins-cicd.git'
	      //checkout([$class: 'GitSCM', branches: [[name: '*/develop']], extensions: [], userRemoteConfigs: [[credentialsId: 'bf6f7b2d-a082-4f4b-ae79-2f44ee3aa774', url: 'https://gitlab.alten.es/laboratorio_qa_devops/openshift-pipelines.git']]])
              sh "pwd"
	      sh "ls -la"
	      //sh "ls -la jenkins/openshift-jenkins-cicd"
          //    sh "mvn install"
            }
          }
	  stage("read pom") {
            steps {
              script {
                IMAGE = readMavenPom().getArtifactId()
                VERSION = readMavenPom().getVersion() 
                println " version - ${VERSION} - ${IMAGE}"
              }
            }
          }
          stage("Compile") {
			steps {
				script {
					sh "mvn clean compile -Dmaven.test.skip=true"  
				}   
			}
		  }
          stage("test") {
			steps {
				script {
					sh "mvn clean package test verify"
				}   
			}
          }
          stage("sonarqube") {
			steps {
				script {
				    	echo "Analizing the project with SonarQube."
					//sh "mvn sonar:sonar -Dsonar.host.url=https://sonar.sdos.es  -Dsonar.projectKey=${SONAR_PROJECT_KEY} -Dsonar.sources=src/main/java -Dsonar.tests=src/test/java -Dsonar.login=a1abce7023aa782d0d5721c5ef37d61494dd3860   -Dsonar.exclusions=**/com/gurock/testrail/APIClient.java,**/com/gurock/testrail/APIException.java   -Dsonar.binaries=target "
					sh "mvn sonar:sonar -Dsonar.host.url=http://sonar.sonarqube.svc.cluster.local:9000 -Dsonar.projectKey=openshift-jenkins-cicd -Dsonar.login=sqa_6a1a0dd1ac95bc0ecc7999ad550d8aa8da6a9c39"
				}   
			}
          }
          stage("install") {
			steps {
				script {
					sh "mvn clean install -Dmaven.test.skip=true"  
				}   
			}
		  }
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
          stage('deploy') {
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
