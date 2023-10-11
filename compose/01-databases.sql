CREATE DATABASE IF NOT EXISTS `opennsa`;
CREATE USER 'opennsa'@'localhost' IDENTIFIED BY 'local';
GRANT ALL ON `opennsa` TO 'opennsa'@'%';

CREATE DATABASE IF NOT EXISTS `sense-rm`;
CREATE USER 'sense-rm'@'localhost' IDENTIFIED BY 'local';
GRANT ALL ON `sense-rm` TO 'sense-rm'@'%';

