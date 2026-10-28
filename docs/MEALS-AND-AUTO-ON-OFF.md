# Pasti e ON/OFF automatico — 27 ottobre 2026

Il diario mantiene per ora la grafica già installata: la nuova separazione è stata presentata come anteprima richiesta dall’utente.

## Pasti

Le sei categorie conservano gli ID storici. Preferenze della dieta permette di abilitarle separatamente e regolare le quote, riportate in proporzione al totale. Oggi e Piano seguono la stessa configurazione. Disabilitare una categoria non elimina le registrazioni esistenti: restano visibili nel relativo giorno. Gli indici del piano e i riferimenti delle registrazioni vengono rimappati per ID del pasto prima della rigenerazione.

## Separazione visiva nel Piano

Ogni momento ha un’intestazione grande in una fascia dedicata, alternata blu tenue/arancione tenue con indicatore navy/arancione. Fra le schede ci sono 26 dp. La ricetta conserva la foto, i macro e tutti i controlli esistenti; le calorie hanno un peso tipografico distinto e i comandi sono in un’area inferiore separata. Titoli e stato vanno a capo con caratteri ingranditi. Il test nativo verifica apertura ricetta, alternative, segna mangiato e annulla registrazione su entrambe le scale di caratteri.

## Calcolo automatico

Modalità opzionale e separata dai valori manuali. Basale dal modello già presente (Mifflin-St Jeor oppure Katch-McArdle quando disponibile massa grassa), moltiplicatore stimato da lavoro e passi. Quota sportiva netta: (MET − 1) × kg × minuti / 60, applicata esclusivamente ON. MET rappresentativi della sessione media per tipo e intensità, non misure individuali. La frequenza segue i giorni selezionati; la media settimanale determina l’aggiustamento di peso. Deficit limitato al 25% del mantenimento stimato e surplus al 15%. Soglia calorica minima mantenuta dal modello precedente. Proteine dalla regola g/kg già presente; grassi 27% o 0,8 g/kg, salvo preferenza low carb, con limite che lascia calorie disponibili per i carboidrati. Calorie arrotondate a 10, macro calcolati sull’esatto totale. Peso aggiornato e dati del profilo ricalcolano gli obiettivi. La schermata mostra ipotesi, formula e dati usati. Nessun doppio conteggio dell’attività registrata o dello smartwatch. Non interpreta foto o testo di un LLM per decidere i target.

Fonti verificate:
- https://pacompendium.com/conditioning-exercise/ (Adult Compendium 2024; pesi 3,5 / 5 / 6 MET; misto 3,5 / 5 / 7,5).
- https://pacompendium.com/running/ e https://pacompendium.com/sports/ (categorie rappresentative cardio e sport).
- https://link.springer.com/article/10.1186/s12970-017-0177-8 (ISSN 2017, proteine e recupero).
- https://link.springer.com/article/10.1186/s12970-017-0189-4 (ISSN 2017, distribuzione e frequenza dei pasti).

I coefficienti per lavoro/passi e il limite del surplus sono scelte esplicite del modello dell’app. La stima non è una misurazione metabolica. In condizioni speciali indicate nel profilo o per minori la modalità automatica non è applicabile.

## Sostituzione

Ricerca, barcode scanner Google e inserimento manuale del codice, foto AI online/offline. Nessun cambiamento del diario prima della conferma nel dettaglio della porzione. Un piatto fotografato con più ingredienti viene aggregato in una singola voce, sommando nutrienti e micronutrienti. L’ID, data, pasto e collegamento al piano della voce originale rimangono invariati. Annullare torna alla scelta senza sostituire. Se la voce viene rimossa nel frattempo, la sostituzione non la ricrea. Un barcode non presente nel catalogo può essere creato dai dati dell’etichetta.
