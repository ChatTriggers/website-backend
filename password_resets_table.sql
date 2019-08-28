create table PasswordResets
(
	email varchar(191) not null,
	token varchar(36) not null,
	expiration datetime not null
);