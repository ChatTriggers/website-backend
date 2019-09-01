# Password Resets
```sql
create table PasswordResets
(
	email varchar(191) not null,
	token varchar(36) not null,
	expiration datetime not null
);
```

# Releases
```sql
CREATE TABLE IF NOT EXISTS Releases 
(
    id BINARY(16) PRIMARY KEY,
    module_id INT UNSIGNED NOT NULL,
    release_version VARCHAR(20) NOT NULL,
    mod_version VARCHAR(20) NOT NULL,
    changelog TEXT NOT NULL,
    downloads INT DEFAULT 0 NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_Releases_module_id_id FOREIGN KEY (module_id) REFERENCES modules(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);
```

# Modules
```sql
alter table modules modify name varchar(64) not null;
alter table modules add tags varchar(2000) default '';
```