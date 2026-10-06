<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.carlos</groupId>
        <artifactId>carlos-parent</artifactId>
        <version>3.0.0-SNAPSHOT</version>
    </parent>
    <packaging>pom</packaging>

    <version>${r'${revision}'}</version>
    <groupId>${project.groupId}</groupId>
    <artifactId>${project.artifactId}</artifactId>
    <name>${project.artifactId}</name>
    <description>${project.describe}</description>


    <modules>
        <module>${project.artifactId}-bus</module>
        <module>${project.artifactId}-api</module>
        <module>${project.artifactId}-cloud</module>
        <module>${project.artifactId}-boot</module>
    </modules>
    <modelVersion>4.0.0</modelVersion>

    <properties>
        <revision>${project.version}</revision>
    </properties>


    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>${project.groupId}</groupId>
                <artifactId>${project.artifactId}-bus</artifactId>
                <version>${r'${revision}'}</version>
            </dependency>
            <dependency>
                <groupId>${project.groupId}</groupId>
                <artifactId>${project.artifactId}-api</artifactId>
                <version>${r'${revision}'}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

</project>