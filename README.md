![badge-labs](https://user-images.githubusercontent.com/327285/230928932-7c75f8ed-e57b-41db-9fb7-a292a13a1e58.svg)

# FDC3 Java API

Standardized Java API to enable integration of FDC3 for Java Desktop Applications.

## Installation

Prerequsite: Java 11

####  FDC3 Java API:
Build Project
```sh
mvn clean compile
```

#### FDC3 Container:

Install Dependencies
```sh
npm -i
```
Run Application
```sh
npm start
```
Runs on http:localhost:8080/

#### Client:

Build Project:
```sh
mvn clean compile package
```
Run the executable from Target directory
```sh
java -jar <Project.baseDirectory>\client\target\client-1.0.0-SNAPSHOT.jar
  ```

#### Stock Search React Receiver Application:
Install Dependencies
```sh
npm -i
```
Run Application
```sh
npm start
```
Runs on http:localhost:3000/

## Usage example

A few motivating and useful examples of how your project can be used. Spice this up with code blocks and potentially screenshots / videos ([LiceCap](https://www.cockos.com/licecap/) is great for this kind of thing).

_For more examples and usage, please refer to the [Wiki][wiki]._

## Development setup

Describe how to install all development dependencies and how to run an automated test-suite of some kind. Potentially do this for multiple platforms.

```sh
make install
npm test
```

## Roadmap

List the roadmap steps; alternatively link the Confluence Wiki page where the project roadmap is published.

1. Item 1
2. Item 2
3. ....

## Contributing

1. Fork it (<https://github.com/finos-labs/fdc3-java-api/fork>)
2. Create your feature branch (`git checkout -b feature/fooBar`)
3. Read our [contribution guidelines](.github/CONTRIBUTING.md) and [Community Code of Conduct](https://www.finos.org/code-of-conduct)
4. Commit your changes (`git commit -am 'Add some fooBar'`)
5. Push to the branch (`git push origin feature/fooBar`)
6. Create a new Pull Request

_NOTE:_ Commits and pull requests to FINOS repositories will only be accepted from those contributors with an active, executed Individual Contributor License Agreement (ICLA) with FINOS OR who are covered under an existing and active Corporate Contribution License Agreement (CCLA) executed with FINOS. Commits from individuals not covered under an ICLA or CCLA will be flagged and blocked by the FINOS Clabot tool. Please note that some CCLAs require individuals/employees to be explicitly named on the CCLA.

*Need an ICLA? Unsure if you are covered under an existing CCLA? Email [help@finos.org](mailto:help@finos.org)*


## License

Copyright 2023 {name of copyright owner}

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)
