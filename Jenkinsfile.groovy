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
              git branch: 'main', url: 'https://github.com/kuldeepsingh99/openshift-jenkins-cicd.git'
              script {
                    def pom = readMavenPom file: 'pom.xml'
                    version = pom.version
              }
          //    sh "mvn install"
            }
          }
          stage("Compile") {
			steps {
				script {
					sh "mvn clean compile -P maven-https -Dmaven.test.skip=true"  
				}   
			}
		  }
          stage("test") {
			steps {
				script {
					sh "mvn clean package test verify  -P maven-https   verify -Dpw=CaliD@d789  -Dvar1=value1 -DreportPath=target/template3.txt -DreportPathTemplate=target/template2.txt -DreportStReplace=XXX"  
				}   
			}
          }
          stage("sonarqube") {
			 when {
                expression { "${JOB_SONAR_ENABLE}" == "true" }
            }
			steps {
				script {
				    echo "Analizing the project with SonarQube."
					//sh "mvn sonar:sonar -Dsonar.host.url=https://sonar.sdos.es  -Dsonar.projectKey=${SONAR_PROJECT_KEY} -Dsonar.sources=src/main/java -Dsonar.tests=src/test/java -Dsonar.login=a1abce7023aa782d0d5721c5ef37d61494dd3860   -Dsonar.exclusions=**/com/gurock/testrail/APIClient.java,**/com/gurock/testrail/APIException.java   -Dsonar.binaries=target "
					sh "mvn sonar:sonar -Dsonar.host.url=http://sonar.sonarqube.svc.cluster.local:9000  -Dsonar.projectKey=openshift-jenkins-cicd -Dsonar.login=a1abce7023aa782d0d5721c5ef37d61494dd3860"
				}   
			}
          }
          stage("install") {
			steps {
				script {
					sh "mvn clean install -P maven-https -Dmaven.test.skip=true"  
				}   
			}
		  }
          stage('Create Image Builder') {
            when {
              expression {
                openshift.withCluster() {
                  openshift.withProject() {
                    return !openshift.selector("bc", "sample-app-jenkins-new").exists();
                  }
                }
              }
            }
            steps {
              script {
                openshift.withCluster() {
                  openshift.withProject() {
                    openshift.newBuild("--name=sample-app-jenkins-new", "--image-stream=redhat-openjdk18-openshift:1.12", "--binary=true")
                  }
                }
              }
            }
          }
          stage('Build Image') {
            steps {
              sh "rm -rf ocp && mkdir -p ocp/deployments"
              sh "pwd && ls -la target "
              sh "cp target/openshiftjenkins-0.0.1-SNAPSHOT.jar ocp/deployments"

              script {
                openshift.withCluster() {
                  openshift.withProject() {
                    openshift.selector("bc", "sample-app-jenkins-new").startBuild("--from-dir=./ocp","--follow", "--wait=true")
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
                    return !openshift.selector('dc', 'sample-app-jenkins-new').exists()
                  }
                }
              }
            }
            steps {
              script {
                openshift.withCluster() {
                  openshift.withProject() {
                    def app = openshift.newApp("sample-app-jenkins-new", "--as-deployment-config")
                    app.narrow("svc").expose();
                  }
                }
              }
            }
          }
        }
    }