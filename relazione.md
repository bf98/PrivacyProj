## Panoramica

In questo progetto abbiamo provato a configurare un ambiente di sviluppo CI/CD (Continuous Integration / Continuos Deployment) con Jenkins, 
utilizzando diversi tool di analisi per valutare sia la sicurezza del codice che quella della build.

Abbiamo deciso di utilizzare Docker, in quanto permette di implementare un sistema portabile e separato dall'infrastruttura che lo ospita.
L'utilizzo di questo ambiente non solo migliora l'efficienza del ciclo di vita del software, ma assicura anche che buone pratiche di sicurezza vengano 
rispettate in ogni fase del processo. 
L'obiettivo finale è sviluppare un flusso di lavoro che non solo automatizzi le operazioni di build e deployment, ma che garantisca anche un'analisi 
della sicurezza.

La codebase utilizzata proviene da un repository GitHub di terze parti.
Si tratta di un applicativo web con backend in Java.

Per il recupero delle dipendenze, la compilazione e l'assemblaggio verrà usato Apache Maven.

I tool che abbiamo utilizzato per analizzare la qualità del codice sono SonarQube e SpotBugs.
Per identificare le vulneribilità delle dipendenze, invece, abbiamo utilizzato OWASP Dependency-Check.
Più nel dettaglio, SonarScanner effettua un'analisi statica del codice sorgente, nel nostro caso file .java. 
In pratica controlla la qualità del codice scritto, sia per quanto riguarda la struttura del codice che la difficoltà per lo sviluppatore 
nel comprenderlo.
Evidenzia se vengono seguite le Best Practices (standard e linee guida di stesura del codice), riducendo la probabilità di introdurre bug, 
se parti di codice vengono ripetute inutilmente, eccetera.

SpotBugs, invece, effettua un'analisi del bytecode generato durante la compilazione.
Rispetto ai sorgenti java, l'analisi del bytecode Java (file .class) permette di identificare possibili bug e vulnerabilità nel codice 
in un formato più vicino a quello eseguito dalla macchina virtuale Java (la JVM).
In questo modo si potrebbero rilevare problemi run-time, per esempio provenienti da ottimizzazioni del compilatore.

## Installazione e Configurazione dell'ambiente

* Creare immagine Jenkins custom (così da avere Maven e SpotBugs);

* Creare directory "jenkins_home", dentro la quale si potrà accedere al filesystem del container Jenkins.
  Impostare i permessi per lettura/scrittura directory (chmod -R 777);

* Avviare compose, così da orchestrare un container Jenkins e un container SonarQube;

* Aprire le loro interfacce web, raggiungibili su porta 8080 e 9000 (localhost:8080 e localhost:9000);

* Creare account per entrambi. La password default di Jenkins è recuperabile all'interno del container.
  Per quanto riguarda SonarQube bisogna prima eseguire l'accesso come admin con password "admin", poi bisogna mettere
  cambiare necessariamente cambiare password.

* Su Jenkins, andare nelle impostazioni "Plugins" e installare "SonarQube Scanner" e "OWASP Dependency-Check".
  Non è necessario riavviare l'istanza Jenkins, basta poi cliccare su "Go back to the top page";

* Configurare SonarQube: andare in impostazioni "System", cercare SonarQube servers, abilitare Environment variables,
  cliccare su Add SonarQube, come nome digitare "sq1" (senza apici).
  ### IP SonarQube
  Per recuperare l'IP del container SonarQube, guardare con docker network inspect <nome_rete> e annotare IP del
  container. 
  Se non si conosce il nome della rete, fare docker inspect sonarqube e trovare nome Network. Probabilmente si trova
  l'IP del container pure lì, comunque.
  Nel form di Jenkins bisogna inserire l'IP formattato come http://<ip_sonarqube>:9000 .
  ### Token SonarQube
  Successivamente risulta necessario generare un token per ottenere l'accesso al server SonarQube.
  Andare nell'interfaccia web di SonarQube, cliccare su Administration -> Security -> Users.
  Ci sarà solamente l'utente Administrator. 
  Individuata la colonna Tokens, cliccare sui tre puntini vicini al numero.
  Come nome token inserire jenkins. Copiare il token generato, in quanto non sarà più recuperabile.
  Tornare su Jenkins, cliccare sul bottone "+ Add" -> Global -> Secret text.
  Nel campo Secret incollare il token, nel campo ID inserire "jenkins-sonar" (o quello che si vuole). Cliccare su
  Create.
  Nel menù a tendina selezionare il token appena creato, cliccare su Apply e poi Save.
  ### SonarQube Webhook
  Per permettere a SonarQube di comunicare il risultato della sua analisi a Jenkins è necessario configurare un webhook.
  In SonarQube andare in Administration -> Configuration -> Webhook.
  Cliccare su "Create". Dopo aver inserito un nome (per esempio "Jenkins"), inserire come URL: http://<ip_jenkins>:8080/sonarqube-webhook
  Cliccare poi su "Create" in basso.
  Ora SonarQube dovrebbe essere configurato correttamente.

* Configurare Dependency-Check: cliccare su Impostazioni -> Tools.
  Cercare Dependency-Check, cliccare "Add Dependency-Check".
  Come nome inserire dependency-check, dopo cliccare su "Install automatically" -> "Add Installer" -> "Install from
  github.com".
  Infine cliccare su Apply e Save.

* Dalla home di Jenkins, cliccare su "Create a Job". Questo ci permetterà di configurare compilazioni automatiche.
  Mettere un nome a scelta per il job, selezionare "Pipeline". Infine premere bottone "Ok".
  Appare il pannello di configurazione del Job, spostarsi al form della Pipeline.
  All'interno di questo simil-editor, incollare la pipeline desiderata.
  Successivamente cliccare su "Apply" e "Save".

* Per avviare la build, cliccare su "Build Now", a sinistra.
  Si può vedere l'andamento della build in "Builds", sotto.
  Per visualizzare come sta procedendo una build, cliccare sulla build -> "Console Output".

## Dettagli Pipeline Jenkins

In modo da automatizzare i processi di build dobbiamo configurare una pipeline su Jenkins.
Questo strumento permette agli sviluppatori di assemblare, testare ed effettuare il deployment delle proprie
applicazioni in un contesto CI/CD.

Nella pratica consiste in una lista sequenziale di istruzioni, strutturata come codice in un cosiddetto Jenkinsfile.
Qui riportiamo la pipeline Jenkins utilizzata:

```groovy
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
			stage('Quality Gate') {
				steps {
                    waitForQualityGate abortPipeline: true
				}
			}
			stage('SpotBugs') {
				steps {
					sh 'spotbugs -html -output spotbugs-report.html -effort:max -xml:withMessages -sourcepath src/main/java target/'
				}
			}
			stage('OWASP Dependency-Check') {
				steps {
					dependencyCheck additionalArguments: '''
						-o './'
						-s './target'
						-f 'ALL'
						--prettyPrint''', odcInstallation: 'dependency-check'

						dependencyCheckPublisher pattern: 'dependency-check-report.xml'
				}
			}
			stage('Security Gate') {
				steps {
					script {
						def hasVulnerabilities = readFile('dependency-check-report.html').contains('High')
						if (hasVulnerabilities) {
							error "Vulnerabilità rilevate. Pipeline abort."
						}
					}
				}
			}
		}
	post {
		success {
			echo 'Build completata con successo'
			archiveArtifacts artifacts: 'target/*.war'
		}
		failure {
			echo 'Build fallita'
		}
		always {
			sh "echo -e 'Build Status: ${currentBuild.currentResult}\nProject: ${env.JOB_NAME}\nBuild Number: ${env.BUILD_NUMBER}\nBuild URL: ${env.BUILD_URL}' | mail -s 'Build Report - ${env.BUILD_NUMBER}' jenkins"
		}
	}
}
```
Specifichiamo di utilizzare qualsiasi nodo (Agent) disponibile per elaborare la pipeline.

Le sezioni Stage raggruppano fasi logiche della pipeline, in pratica si possono considerare una sequenza di task, dove
ogni task è dedicata a una precisa parte della build.
All'interno di ogni Stage possono esserci uno o più Step. Lo Step è un'ulteriore sezione contenente i comandi che
definiscono ciò che deve essere eseguito in ogni fase. Come si può vedere dalla build, possono essere comandi shell,
chiamate a script, ulteriormente invocazioni ad altri job.
La sezione Post contiene azioni da eseguire alla fine della pipeline, basate sui risultati degli Stage precedenti.
Ciò può essere utilizzata per inviare notifiche riguardo la build, archiviare artefatti etc.

* Nello stage "Checkout" andiamo a recuperare la codebase dalla sua repository GitHub. Specifichiamo il branch master.
* Nello stage "Build & Check" invochiamo Maven per scaricare le dipendenze e compilare i sorgenti. 
  Sempre nello stesso comando passiamo i file sorgente a SonarScanner, il quale effettua un'analisi.
* Sempre legato allo stage precedente, configuriamo un Quality Gate per accertarsi che le analisi di SonarQube siano
  andate a buon fine e abbiano dato risultati accettabili. 
* Nello stage "SpotBugs" richiamiamo SpotBugs per fare un'analisi dei bytecode (.class) precedentementi generati.
  I risultati di tali analisi verranno salvati in un report HTML (recuperabile dalla root del job).
* Nello stage "OWASP Dependency-Check" invochiamo Dependecy-Check per effettuare un'analisi delle librerie esterne
  utilizzate nel progetto.
  I risultati di tali analisi verranno salvati in un report HTML (recuperabile dalla root del job).
* Nello stage "Security-Gate" andiamo ad analizzare il report generato da Dependecy-Check, controllando se siano
  presenti vulnerabilità gravi. Se così fosse, la pipeline si arresta.
* Nella sezione Post specifichiamo come procedere a seconda dal risultato della pipeline.
  In caso di esito positivo gli artefatti vengono archiviati, nel nostro caso il file .WAR dell'applicazione web. 
  Indipendentemente da come si sia conclusa la pipeline, gli sviluppatori ricevono una mail che comunica il risultato
  della build.

## Report Vulnerabilità 

Sono state eseguite analisi di sicurezza utilizzando SonarScanner, SpotBugs e OWASP Depedency-Check.
In particolare, SonarScanner è stato utilizzato per analizzare la struttura dei file sorgenti .java, mentre SpotBugs per un'analisi
statica del bytecode generato durante la compilazione. 
Infine, OWASP Depedency-Check è servito per analizzare possibili vulnerabilità nelle librerie di terze parti utilizzate.
Questi tool sono stati integrati all'interno della pipeline di Jenkins, consentendo un'analisi dinamica per ogni
compilazione della codebase.

OWASP Dependency-Check riporta una serie di problemi di sicurezza riguardo le librerie utilizzate, specialmente per
quanto riguarda PostgreSQL (sistema RDSMS utilizzato), webapp-runner (tool che permette di eseguire applicativi web
Java servlet) e mysql-connector-java (driver che permette agli applicativi Java di connettersi e interagire con database
MySQL).

1. postgresql-42.3.7.jar:

   * OWASP Top 10: Injection;
   * Descrizione: Un attaccante potrebbe eseguire comandi SQL arbitrari a causa di una sanificazione inadeguata degli
     input.
   ---
   * OWASP Top 10: Broken Access Control;
   * Descrizione: Un attaccante potrebbe ottenere l'accesso inautorizzato alle risorse.

2. mysql-connector-java-8.0.28.jar:

   * OWASP Top 10: Broken Access Control;
   * Descrizione: Un attaccante potrebbe ottenere l'accesso inautorizzato ai dati, causa controlli di accesso
     inadeguati.

3. protobuf-java-3.11.4.jar:

   * OWASP Top 10: Insecure Direct Object References;
   * Descrizione: Gli input forniti non sono sufficientemente controllati per evitare usi impropri.

4. webapp-runner.jar:

   * OWASP Top 10: Insufficient Logging & Monitoring;
   * Descrizione: Un attaccante protrebbe sfruttare una configurazione di rete non sicura per provocare una negazione di
     servizio (DoS) attraverso richieste HTTP malformate.

L'analisi di SonarQube invece riporta relativamente pochi problemi di sicurezza relativi al codice, ma un discreto
numero di avvisi riguardo la sua mantenibilità e robustezza.

Un chiaro esempio di sicurezza riguarda le query al DB, come si può vedere da questo snippet:

```java
@Override

public List<Book> getBooksByCommaSeperatedBookIds(String commaSeperatedBookIds) throws StoreException {

	List<Book> books = new ArrayList<Book>();

	Connection con = DBUtil.getConnection();

	try {

		String getBooksByCommaSeperatedBookIdsQuery =

			"SELECT * FROM " + BooksDBConstants.TABLE_BOOK

			+ " WHERE " +

			BooksDBConstants.COLUMN_BARCODE + " IN ( " + commaSeperatedBookIds + " )";

		PreparedStatement ps = con.prepareStatement(getBooksByCommaSeperatedBookIdsQuery
				);
```
Questo, riferendosi agli OWASP Top 10, presenta rischio di Broken Access Control e Injection, poiché la mancanza di una corretta validazione 
dell'input potrebbe rendere l'applicazione vulnerabile a iniezioni SQL.
La validazione dell'input, dunque, potrebbe essere una soluzione.
