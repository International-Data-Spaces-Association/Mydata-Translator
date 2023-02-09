
![License](https://img.shields.io/badge/License-Apache_2.0-d22028.svg) ![Language](https://img.shields.io/badge/Language-Java-b74237.svg) ![build](https://img.shields.io/badge/Build-Maven-ff6805.svg)
# MY DATA Translator
The MYDATA Translator is a library to translate the pattern from the Policy Library Java model to [MY DATA Control Technologies](https://www.mydata-control.de/) Policies. The translated policies can be used with MY Data and can be enforced withing an MY DATA Solution.
## Maven
### Download
Download maven [here](https://maven.apache.org)
### Dependencies
#### Policy Library
To be able to run it, it is necessary to install  the maven project Policy Library first.
You can get it [here](https://gitlab.cc-asp.fraunhofer.de/iese-ids/policy-library).

    <dependency>  
         <groupId>de.fraunhofer.iese.ids.odrl</groupId>  
         <artifactId>policy-library</artifactId>  
         <version>${version}</version>  
    </dependency>

### Installation
To install the project to your local maven repository run:

    mvn clean install
To install skip the tests:

     mvn clean install -DskipTests
