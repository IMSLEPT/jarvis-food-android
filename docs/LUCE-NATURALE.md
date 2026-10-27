# Stile chiaro e identità Jarvis Food

Home «Blu essenziale» per la zona approvata; robot chef di «Luce naturale»
per icona adattiva, navigazione e avatar del chatbot. Fondi pastello delle
schede sostituiti da superfici bianche; blu notte per i testi e colori nei
dettagli, nei grafici e negli indicatori di stato.

La home mantiene le destinazioni delle quattro azioni rapide. Macro e azioni
sono raggruppati in due superfici condivise, il nome è in evidenza e il
riepilogo mostra calorie residue a sinistra e indicatore a destra. Il diario
rimane espandibile. La navigazione usa una sottolineatura arancione.

Lo stesso trattamento delle superfici si estende a Piano, Cucina, categorie,
ricette, dispensa, spesa, Progressi, acqua, digiuno, foto AI, profilo, obiettivi,
questionario, smartwatch, micronutrienti e famiglia. Le selezioni mantengono
un indicatore visibile. Restano invariati i colori delle barre e i comandi.

Questa revisione conserva la base più recente del ramo delle fotografie,
comprese le immagini editoriali e le correzioni ai pulsanti inferiori.
Il manifest originale resta invariato; vengono autorizzati i 12 hash delle risorse grafiche approvate. Gli altri
file protetti restano invariati rispetto a questa base.

## Verifiche

Il precedente lavoro locale aveva compilato debug e APK dei test, con 57 test
JVM superati. La copia di lavoro precedente non era più disponibile: questa
revisione è stata ricostruita sul ramo aggiornato, quindi richiede un nuovo
controllo completo nella pipeline GitHub prima di considerarla validata.

La verifica grafica locale precedente non era passata: timeout del test
categorie e successivo blocco di System UI nell'emulatore senza KVM. Non sono
state accettate immagini native come prova di corretta resa.

Controlli prima della pubblicazione: manifest dei file protetti e
`git diff --check`. La pipeline già presente compila l'APK con la firma di
rilascio originale ed esegue i controlli Android previsti.

`tools/package_brand_assets.sh` rigenera le cinque densità dell'icona dal logo
trasparente; richiede ImageMagick. Nessun dato personale o motore AI è cambiato.
