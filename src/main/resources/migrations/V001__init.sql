-- NMLP initial schema (schema_migrations is created by MigrationRunner before applying files)

CREATE TABLE IF NOT EXISTS genders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS pronouns (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL UNIQUE
);

INSERT OR IGNORE INTO genders (id, code) VALUES (1, 'male'), (2, 'female');
INSERT OR IGNORE INTO pronouns (id, code) VALUES (1, 'he_him'), (2, 'she_her');

CREATE TABLE IF NOT EXISTS players (
    uuid TEXT PRIMARY KEY,
    username_last TEXT NOT NULL,
    gender_id INTEGER REFERENCES genders(id),
    pronouns_id INTEGER REFERENCES pronouns(id),
    setup_wizard_complete INTEGER NOT NULL DEFAULT 0,
    first_seen INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_players_username ON players(username_last);

CREATE TABLE IF NOT EXISTS relationships (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_low TEXT NOT NULL,
    player_high TEXT NOT NULL,
    status TEXT NOT NULL,
    started_at INTEGER NOT NULL,
    ended_at INTEGER,
    UNIQUE(player_low, player_high, ended_at)
);

-- Active rows use ended_at IS NULL — partial unique index below (SQLite)
CREATE UNIQUE INDEX IF NOT EXISTS idx_relationships_active_pair
    ON relationships(player_low, player_high)
    WHERE ended_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_relationships_player_low ON relationships(player_low);
CREATE INDEX IF NOT EXISTS idx_relationships_player_high ON relationships(player_high);

CREATE TABLE IF NOT EXISTS engagements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    relationship_id INTEGER NOT NULL REFERENCES relationships(id),
    proposed_at INTEGER NOT NULL,
    accepted_at INTEGER NOT NULL,
    ended_at INTEGER
);

CREATE TABLE IF NOT EXISTS marriages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    relationship_id INTEGER NOT NULL REFERENCES relationships(id),
    married_at INTEGER NOT NULL,
    divorced_at INTEGER
);

CREATE INDEX IF NOT EXISTS idx_engagements_rel ON engagements(relationship_id);
CREATE INDEX IF NOT EXISTS idx_marriages_rel ON marriages(relationship_id);

CREATE TABLE IF NOT EXISTS family_links (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    from_uuid TEXT NOT NULL,
    to_uuid TEXT NOT NULL,
    link_type TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    ended_at INTEGER
);

CREATE INDEX IF NOT EXISTS idx_family_from ON family_links(from_uuid);
CREATE INDEX IF NOT EXISTS idx_family_to ON family_links(to_uuid);

CREATE TABLE IF NOT EXISTS children (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    child_uuid TEXT NOT NULL,
    parent_uuid TEXT NOT NULL,
    biological INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    ended_at INTEGER,
    UNIQUE(child_uuid, parent_uuid, ended_at)
);

CREATE INDEX IF NOT EXISTS idx_children_child ON children(child_uuid);
CREATE INDEX IF NOT EXISTS idx_children_parent ON children(parent_uuid);

CREATE TABLE IF NOT EXISTS history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_type TEXT NOT NULL,
    actor_uuid TEXT,
    target_uuid TEXT,
    related_uuid TEXT,
    payload TEXT,
    created_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_history_actor ON history(actor_uuid);
CREATE INDEX IF NOT EXISTS idx_history_target ON history(target_uuid);
