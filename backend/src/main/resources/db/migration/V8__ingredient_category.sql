-- Phase 16: Supermarkt-Kategorie auf Zutaten fuer gruppierte Einkaufsliste.
-- Default SONSTIGES; Bestandsdaten werden per Namens-Heuristik grob einsortiert
-- (nur wo noch SONSTIGES steht, damit die Reihenfolge der UPDATEs egal bleibt).

ALTER TABLE ingredient
    ADD COLUMN category VARCHAR(30) NOT NULL DEFAULT 'SONSTIGES';

-- "tomate"/"tomato"/"paprika" wuerden als Substring auch verarbeitete Wuerz-
-- Produkte fangen: Tomatenmark/Tomato paste und Paprikapulver gehoeren aber zu
-- GEWUERZE_SAUCEN (spaeterer UPDATE). Hier explizit ausschliessen, damit die
-- Reihenfolge der UPDATEs egal bleibt (analog zur "ei"-Sonderbehandlung unten).
UPDATE ingredient SET category = 'OBST_GEMUESE'
WHERE category = 'SONSTIGES' AND (
    LOWER(name) SIMILAR TO '%(apfel|apple|banane|banana|tomate|tomato|zwiebel|onion|knoblauch|garlic|kartoffel|potato|salat|lettuce|paprika|karotte|carrot|zitrone|lemon|gurke|cucumber|spinat|spinach|pilz|mushroom|zucchini|brokkoli|broccoli|lauch|leek|ingwer|ginger|aubergine|eggplant)%'
) AND LOWER(name) NOT LIKE '%tomatenmark%'
  AND LOWER(name) NOT LIKE '%tomato paste%'
  AND LOWER(name) NOT LIKE '%paprikapulver%';

-- "ei" nur als exakter Name bzw. "eier"/"egg" — das blosse Substring "ei"
-- wuerde z. B. "Schweinefilet" oder "Eisbergsalat" falsch einsortieren.
UPDATE ingredient SET category = 'MILCHPRODUKTE'
WHERE category = 'SONSTIGES' AND (
    LOWER(name) SIMILAR TO '%(milch|milk|kaese|käse|cheese|butter|sahne|cream|joghurt|yogurt|yoghurt|quark|eier|egg|mozzarella|parmesan|feta|ricotta)%'
    OR LOWER(name) = 'ei'
);

UPDATE ingredient SET category = 'FLEISCH_FISCH'
WHERE category = 'SONSTIGES' AND (
    LOWER(name) SIMILAR TO '%(huhn|haehnchen|hähnchen|chicken|rind|beef|schwein|pork|fisch|fish|lachs|salmon|thunfisch|tuna|hack|mince|wurst|sausage|speck|bacon|garnele|shrimp|prawn)%'
);

UPDATE ingredient SET category = 'BACKWAREN'
WHERE category = 'SONSTIGES' AND (
    LOWER(name) SIMILAR TO '%(brot|bread|broetchen|brötchen|toast|baguette|tortilla|wrap)%'
);

UPDATE ingredient SET category = 'VORRAT'
WHERE category = 'SONSTIGES' AND (
    LOWER(name) SIMILAR TO '%(mehl|flour|zucker|sugar|reis|rice|nudel|pasta|spaghetti|penne|linse|lentil|bohne|bean|kichererbse|chickpea|haferflocken|oat|oel|öl|oil|essig|vinegar|honig|honey)%'
);

UPDATE ingredient SET category = 'GEWUERZE_SAUCEN'
WHERE category = 'SONSTIGES' AND (
    LOWER(name) SIMILAR TO '%(salz|salt|pfeffer|pepper|curry|paprikapulver|oregano|basilikum|basil|thymian|thyme|zimt|cinnamon|kreuzkuemmel|cumin|sojasauce|soy sauce|senf|mustard|ketchup|mayo|brühe|bruehe|stock|broth|tomatenmark|tomato paste)%'
);
