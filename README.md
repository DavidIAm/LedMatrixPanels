# LED Matrix Panels Program

## Description
This is a Java-based application. It writes to the led matrix units from Framework on a linux box.

Included functionalities so far:
* DasBlinkenLights - creates blinkenlights on the panels
* WifiBattery - shows a battery meter and a wifi signal strength and noise meter

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
   ```bash
   SPRING_PROFILES_ACTIVE=dasblinkenlights java -jar build/libs/panels-0.0.1-SNAPSHOT.jar
   SPRING_PROFILES_ACTIVE=wifibattery java -jar build/libs/panels-0.0.1-SNAPSHOT.jar
   ```
   
if you use both profiles at once, I suspect that you'll get mostly blinkes with flashes of meters.


## File Overview
### Source Files
- **DasBlinkenLights.java**: Implements a blinking light simulation or uses randomness for specific actions.
- **WifiBattery.java**: Implements a meter for battery and wifi signal and noise.
- **CommunicationCreator.java**: A class for creating communication-related functionality.

### Build Files
- `build.gradle`: Build and dependency management.
- `gradlew` and `gradlew.bat`: Gradle wrapper scripts for Linux/macOS and Windows, respectively.

### Other
- `.gitignore`: Specifies files/folders to be ignored by Git.

## Project Structure
