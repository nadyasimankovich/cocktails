create keyspace cocktails WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 3};

use cocktails;

CREATE TABLE catalog (name text PRIMARY KEY, ingredients text, recipe text, image blob, ts bigint);

CREATE TABLE ingredients (name text PRIMARY KEY, cocktails text);
