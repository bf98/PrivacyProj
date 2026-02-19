## Installazione e Configurazione dell'ambiente

* Creare immagine Jenkins custom (per avere Maven e SpotBugs al suo interno);
* Creare directory "jenkins_home", dentro la quale si potrà accedere al filesystem del container Jenkins.
  Impostare i permessi per tutti (chmod -R 777);
* Avviare compose, costituito da un container Jenkins e un container SonarQube;
* Aprire le loro interfacce web, raggiungibili su porta 8080 e 9000 (localhost:8080 e localhost:9000);
* Creare account per entrambi. La password default di Jenkins è recuperabile all'interno del container (vedi note.md).
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
  Ora SonarQube dovrebbe essere configurato correttamente.
* Configurare Dependency-Check: cliccare su Impostazioni -> Tools.
  Cercare Dependency-Check, cliccare "Add Dependency-Check".
  Come nome inserire dependency-check, dopo cliccare su "Install automatically" -> "Add Installer" -> "Install from
  github.com".
  Infine cliccare su Apply e Save.
* Dalla home di Jenkins, cliccare su "Create a Job". Questo ci permetterà di configurare compilazioni automatiche.
  Mettere un nome a scelta per il job, selezionare "Pipeline". Infine premere bottone "Ok".
  Appare il pannello di configurazione del Job, spostarsi al form della Pipeline.
  All'interno di questo simil-editor, incollare la pipeline più recente nel file note.md.
  Successivamente cliccare su "Apply" e "Save".
* Per avviare la build, cliccare su "Build Now", a sinistra.
  Si può vedere l'andamento della build in "Builds", sotto.
  Per visualizzare come sta procedendo una build, cliccare sulla build -> "Console Output".

//// CONTINUARE ////
## Report Vulnerabilità 

Sono state eseguite analisi di sicurezza utilizzando SonarScanner, SpotBugs e OWASP Depedency-Check.
In particolare, SonarScanner è stato utilizzato per analizzare la struttura dei file sorgenti .java, mentre SpotBugs per un'analisi
statica del bytecode generato durante la compilazione. 
Infine, OWASP Depedency-Check è servito per analizzare possibili vulnerabilità nelle librerie di terze parti utilizzate.
Questi tool sono stati integrati all'interno della pipeline di Jenkins, consentendo un'analisi dinamica per ogni
compilazione della codebase.

# Analisi OWASP Dependency-Check

OWASP Dependency-Check riporta una serie di problemi di sicurezza riguardo alle librerie utilizzate, specialmente per
quanto riguarda PostgreSQL (sistema RDSMS utilizzato), webapp-runner (tool che permette di eseguire applicativi web
Java servlet) e mysql-connector-java (driver che permette agli applicativi Java di connettersi e interagire con database
MySQL).

1. postgresql-42.3.7.jar:
   * OWASP Top 10: A1 - Injection;
   * Descrizione: Un attaccante potrebbe eseguire comandi SQL arbitrari a causa di una sanificazione inadeguata degli
     input.
   ---
   * OWASP Top 10: A5 - Broken Access Control;
   * Descrizione: Un attaccante potrebbe ottenere l'accesso inautorizzato alle risorse.

2. mysql-connector-java-8.0.28.jar:
   * OWASP Top 10: A5 - Broken Access Control;
   * Descrizione: Un attaccante potrebbe ottenere l'accesso inautorizzato ai dati, causa controlli di accesso
     inadeguati.

3. protobuf-java-3.11.4.jar:
   * OWASP Top 10: A4 - Insecure Direct Object References;
   * Descrizione: Gli input forniti non sono sufficientemente controllati per evitare usi impropri.

4. webapp-runner.jar:
   * OWASP Top 10: A10 - Insufficient Logging & Monitoring;
   * Descrizione: Un attaccante protrebbe sfruttare una configurazione di rete non sicura per provocare una negazione di
     servizio (DoS) attraverso richieste HTTP malformate.

# Analisi SonarScanner

# Analisi SpotBugs
