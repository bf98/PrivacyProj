pipeline {
	agent any
		stages {
			stage('Checkout') {
				steps {
					git url: 'https://github.com/shashirajraja/onlinebookstore.git', branch: 'master'
				}
			}
			stage('Build & SonarScanner') {
				steps {
					withSonarQubeEnv(installationName: 'sq1') {
						sh 'mvn clean install org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.inclusions=**/*.java -Dsonar.java.binaries=.'
					}
				}
			}
			stage('SpotBugs') {
				steps {
					sh 'spotbugs -html -output target/spotbugs-report.html -effort:max -xml:withMessages -sourcepath src/main/java target/'
				}
			}
			stage('OWASP Dependency-Check Vulnerabilities') {
				steps {
					dependencyCheck additionalArguments: '''
						-o './'
						-s './target'
						-f 'ALL'
						--prettyPrint''', odcInstallation: 'dependency-check'

						dependencyCheckPublisher pattern: 'dependency-check-report.xml'
				}
			}
		}
	post {
		success {
			echo 'Build completata con successo'
		}
		failure {
			echo 'Build fallita'
		}
		always {
			sh "echo -e 'Build Status: ${currentBuild.currentResult}\nProject: ${env.JOB_NAME}\nBuild Number: ${env.BUILD_NUMBER}\nBuild URL: ${env.BUILD_URL}' | mail -s 'Build Report - ${env.BUILD_NUMBER}' jenkins"
		}
	}
}
