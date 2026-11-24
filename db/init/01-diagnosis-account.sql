CREATE USER 'diagnosis'@'%' IDENTIFIED BY 'diagnosis';
GRANT SELECT ON performance_schema.* TO 'diagnosis'@'%';
GRANT SELECT ON shop.* TO 'diagnosis'@'%';
