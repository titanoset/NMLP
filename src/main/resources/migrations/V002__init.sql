-- Remove "other" gender; pronouns follow male/female
UPDATE players SET gender_id = NULL, pronouns_id = NULL WHERE gender_id = 3;
DELETE FROM genders WHERE code = 'other' OR id = 3;
UPDATE players SET pronouns_id = 1 WHERE gender_id = 1;
UPDATE players SET pronouns_id = 2 WHERE gender_id = 2;
