# LED Matrix Panels Program

## Description
This is a Java-based application. It writes to the led matrix units from Framework on a linux box.

Included functionalities so far:
* DasBlinkenLights - creates blinkenlights on the panels

The project is built on **Java** and uses **Gradle** as the build automation tool.

## Features
- **Communication Creation**: Handles or generates the communication components necessary for the project.
- **Randomized Processing**: Includes functionality for utilizing random operations (`DasBlinkenLights.java`).
- **Gradle Integration**: Simplifies project dependencies and building.

## Prerequisites
- **Java 17**: Ensure you have JDK 17 installed.
- **Gradle**: Use Gradle for managing dependencies and building the project.

## Setup and Usage
1. **Clone the Project**
   Clone this repository to your local machine:
   ```bash
   git clone <repository-url>
   ```

2. **Build the Project**
   Use the Gradle wrapper included in the project:
   ```bash
   ./gradlew bootJar
   ```

3. **Run the Application**
   Depending on the main class, you can execute the code. For example:
   ```bash
   java -jar build/libs/panels-0.0.1-SNAPSHOT.jar
   ```

## File Overview
### Source Files
- **DasBlinkenLights.java**: Likely implements a blinking light simulation or uses randomness for specific actions.
- **CommunicationCreator.java**: A class for creating communication-related functionality.

### Build Files
- `build.gradle`: Build and dependency management.
- `gradlew` and `gradlew.bat`: Gradle wrapper scripts for Linux/macOS and Windows, respectively.

### Other
- `.gitignore`: Specifies files/folders to be ignored by Git.

## Project Structure
