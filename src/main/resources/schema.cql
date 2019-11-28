create keyspace cocktails WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 3};

use cocktails;

CREATE TABLE catalog (name text PRIMARY KEY, recipe text, image blob, ts: timestamp);