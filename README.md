

## Test setup

### MySQL database

```bash
docker run -it --rm \
    --name mysql \
    -p 3306:3306 \ 
    -e MYSQL_ROOT_PASSWORD=debezium \
    -e MYSQL_USER=mysqluser \
    -e MYSQL_PASSWORD=mysqlpw \
    debezium/example-mysql:0.9
    
docker run -it --rm --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=debezium -e MYSQL_USER=mysqluser -e MYSQL_PASSWORD=mysqlpw debezium/example-mysql:0.9
	
```

Run test sql statements:

```bash
docker exec -it mysql bin/bash

# login as root user
mysql -u root -p 
SELECT host,user,authentication_string FROM mysql.user;
SHOW GRANTS FOR 'mysqluser';

# add require grants for application user
GRANT SUPER ON *.* TO 'mysqluser'@'%';
GRANT RELOAD ON *.* TO 'mysqluser'@'%';
GRANT REPLICATION SLAVE ON *.* TO 'mysqluser'@'%';

GRANT ALL ON *.* TO 'mysqluser'@'%';

# login as same user as java app
mysql -u mysqluser -p
show databases;
use inventory;

# Example statements
show tables;
SELECT * FROM customers;

INSERT INTO customers VALUES ( 1005, 'Samuel', 'Vandecasteele', 'samuel.vandecasteele@i8c.be');
INSERT INTO customers (first_name, last_name, email) VALUES ( 'Jozef', 'Verbeek', 'jverbeek@era.be');

UPDATE customers SET email = "vandecasteele.samuel@gmail.com" WHERE id = '1005';
```



### ActiveMQ Artemis

```bash
docker run -it --rm \
  -p 8161:8161 \
  -p 61616:61616 \
  vromero/activemq-artemis

```
Open browser on ``http://127.0.0.1:8161/`` 
* username: artemis
* password: simetraehcapa



Reference:
* https://github.com/vromero/activemq-artemis-docker