pipeline {
	agent any

		stages {
			stage('Checkout') {
				steps {
					git url: 'https://github.com/shashirajraja/onlinebookstore.git', branch: 'master'
				}
			}

			stage('Check') {
				steps {
					withSonarQubeEnv(installationName: 'sq1') {
						sh 'mvn clean install org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.inclusions=**/*.java -Dsonar.java.binaries=.'
					}
				}
			}
			stage('SpotBugs') {
				steps {
					sh 'spotbugs -html -output my-report.html -effort:max -xml:withMessages -sourcepath src/main/java target/classes'
				}
			}

			// Dependency-Check
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
			echo 'Pipeline completata con successo!'
		}
		failure {
			echo 'Pipeline fallita!'
		}
	}
}
