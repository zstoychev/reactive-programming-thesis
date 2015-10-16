# Сървърна част

## Необходим софтуер:

- Java 8
- Scala Build Tool
- Apache Cassandra

## Стартиране

За да работи клъстера е необходимо да има стартирани сървъри поне на портове 2551 и 2552:

    sbt "project backend" "run 2551"
    sbt "project backend" "run 2552"
    sbt "project calculation" "run 9999"
    sbt "project calculation" "run"
    sbt "project frontend" "run"

При липса на порт се стартира на произволен. Frontend възлите винаги се стартират на произволен порт. 

Чрез `sbt "project проект" dist` се съдава готов пакет за внедряване.

Предврително трябва да бъде пусната и Apache Cassandra базата.

# Клиентска част

## Необходим софтуер:

- Node.js
- Node Package Manager (npm)
- grunt (`sudo npm install -g grunt-cli`)
- bower (`sudo npm install -g bower`)

## За да стартирате приложението:

1. Отидете в директория client;
2. Въведете `npm install`;
3. Въведете `bower install`;
4. Тестов сървър може да се пусне с `grunt serve`
5. Пакет за внедряване на уеб сървър се генеира чрез `grunt build`. След това в директорията `dist` са необходимите файлове.
