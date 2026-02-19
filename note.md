Installazione docker Jenkins + Maven: https://www.educative.io/answers/run-maven-with-jenkins-docker-image-via-custom-docker-build
Installazione docker Jenkins + SonarQube: https://funnelgarden.com/sonarqube-jenkins-docker/
Installazione docker Jenkins + SonarQube + Dependency-Check: https://jay75chauhan.medium.com/jenkins-ci-cd-with-docker-trivy-sonarqube-owasp-dependency-check-fc9b643aef90
Configurazione OWASP Dependency-Check su Jenkins: https://thelinuxcode.com/owasp-dependency-check-jenkins/
Configurazione OWASP Dependency-Check con Maven: https://www.baeldung.com/java-maven-owasp-dependency-check
Configurazione OWASP Dependency-Check: https://security.docs.wso2.com/en/latest/security-guidelines/secure-engineering-guidelines/external-dependency-analysis-analysis-using-owasp-dependency-check/
Video configurazione jenkins + sonarqube (no docker): https://www.youtube.com/watch?v=KsTMy0920go
Docker compose jenkins + sonarqube con network condiviso?: https://www.devopsroles.com/sonarqube-from-a-jenkins-pipeline-job-in-docker
Repository da compilare: https://github.com/shashirajraja/onlinebookstore

### TODO
[x] Controllare per installazione/configurazione SonarQube, OWASP Dependency-Check;
    Forse bisogna aggiungere 'maven install' all'inizio della pipeline, in modo che installi le dipendenze;
    Esempio:
        stage('Install Dependencies') {
        steps {
            sh 'mvn install' 
        }
    }
[x] Aggiungere gestione fallimento/successo build (sezione post nella pipeline?);
[ ] Salvare artefatto .jar in folder precisa (solo se build non fallisce);

[ ] Aggiungere SpotBugs alla pipeline Jenkins (utilizzato sempre per analisi statica codice e.g. SonarQube, quindi eseguirlo prima di OWASP Dependecy-Check etc.);
    Capire perché dà un report strano.

Nel caso si usasse Podman invece che Docker, per qualche motivo non si può usare la codifica
<nome_immagine>:latest di default.
Per permetterlo bisogna aggiungere al file /etc/containers/registries.conf la stringa:
unqualified-search-registries=["docker.io"]

Comandi comodi per git:
    // Clona una repository via ssh (utile per repo private)
    git clone git@github.com:<github_username>/<repo_name>.git

Comandi comodi per Docker:
    // Mostrare container avviati
    docker ps
    // Mostrare tutti i container, sia avviati che spenti
    docker ps -a
    // Fermare un container
    docker stop <id_container>
    // Avviare un container
    docker start <id_container>
    // Stampare lista immagini salvate
    docker images
    // Eliminare un'immagine salvata
    docker image rm <nome_image>
    // Eliminare un container (prima bisognerebbe fermarlo)
    docker rm <id_container>
    // Creazione immagine personalizzata
    docker build -t my-jenkins . 
    // Avviare container con immagine personalizzata, forse la flag -v (volume) non è necessaria per il nostro caso
    docker run -d -p 8080:8080 -p 50000:50000 -v jenkins_home:/var/jenkins_home my-jenkins 
    // Recupera password auto-generata di Jenkins
    docker exec <container_id> cat /var/jenkins_home/secrets/initialAdminPassword
    // Download immagine SonarQube
    docker pull sonarqube
    // Avviare container con immagine SonarQube
    docker run -d --name sonarqube -p 9000:9000 sonarqube
    // Accedere al container con cli 
    docker exec -it <container_id> bash
    // Cancellare dati inutilizzati Docker
    docker system prune -a
    // Cancellare dati Docker in maniera intensiva (tabula rasa)
    docker system reset
    // Avviare docker-compose per prima volta (con file compose.yaml nella directory corrent)
    docker compose up
    // Stoppare docker-compose ed eliminare i container (sempre nella stessa directory)
    docker compose down
    // Riavviare i container del compose
    docker compose start
    // Spegnere i container del compose, mantenendo i dati
    docker compose stop
    // Controllare i container compose 
    docker compose ps -a

Nel caso di errori, controllare che l'IP del container SonarQube non sia cambiato. Si può fare con podman network
inspect <nome_rete>.
Nel caso fosse cambiato, bisogna aggiornarlo nelle impostazioni di Jenkins.

Script Pipeline:

pipeline {
	agent any

		stages {
			stage('Checkout') {
				steps {
					git url: 'https://github.com/shashirajraja/onlinebookstore.git', branch: 'master'
				}
			}

			stage('Maven Build and Test') {
				steps {
					sh 'mvn clean test'
				}
			}

			stage('Package') {
				steps {
					sh 'mvn package'
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

NUOVO script pipeline, che sembra funzionare, però bisogna capire perché non analizza i file .java del progetto:

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
						sh 'mvn clean org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.exclusions=**/*.java'
					}
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

NUOVO script pipeline, che sembra funzionare (eccetto Dependency-Check) ed analizzare i file .java del progetto:

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
						sh 'mvn clean org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.inclusions=**/*.java -Dsonar.java.binaries=.'
					}
				}
			}

			stage('OWASP Dependency-Check Vulnerabilities') {
				steps {
					dependencyCheck additionalArguments: '''
						-o './'
						-s './'
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

Bisognava lanciare "maven clean install ..." e specificare a Dependecy-Check la directory targets creata con quest'ultimo comando.
NUOVA pipeline:

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

Praticamente specifica la root del progetto come destinazione dei binari.

NUOVA pipeline con spotbugs:

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

Sul ThinkPad sembra andare, quindi è corretto. Capire perché sul fisso dava certi errori riguardo i Build Executors (forse bisogna veramente pulire il disco?). 
Sì, era piena la partizione di root, e forse pure la home.

Perché il container di SonarQube sul fisso si arresta mentre sul ThinkPad no?
Sul ThinkPad la versione di docker è 5.4.2. 
Forse è inutile, ma sul portatile non risulta Java installato nel sistema.
Riguardava sempre la partizione root, almeno 10% della capienza deve essere libero. Penso stessa cosa per la home, in
quanto dopo aver cancellato dei file si è avviato correttamente.

Per formattare decentemente i Jenkinsfile con Vim, copia-incollarli in una nuova tab, settare la sintassi in 'groovy'
(:set syntax=groovy) e formattarlo (comando: gg=G). 
