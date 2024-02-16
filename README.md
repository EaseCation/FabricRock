# Bedrock Loader

_**Game Version Notice:** Only for Minecraft Java Edition 1.18.2_

_**Note:** This project is currently in its early stages of development and is not yet ready for use._


Bedrock Loader is a pioneering mod for Minecraft Java Edition, designed to bridge the gap between Java and Bedrock editions by allowing the loading of Bedrock addons directly into the Java version. Developed as a Fabric mod, Bedrock Loader aims to enhance the Minecraft experience by integrating the diverse world of Bedrock addons, including custom blocks, models, and entities, into the Java edition's rich ecosystem.

## Installation and Setup

Before you begin, ensure you have Fabric Loader and Fabric API installed in your Minecraft Java Edition. Follow these steps to set up Bedrock Loader:

1. Download the latest version of Bedrock Loader from the Releases section.
2. Place the downloaded `.jar` file in your `mods` folder located in your Minecraft directory.
3. Launch Minecraft Java Edition using the Fabric profile.

## Using Bedrock Loader

To load Bedrock addons with Bedrock Loader, follow these steps:

1. Place your Bedrock edition addons (`.zip` or `.mcpack` files) into the `config/bedrock-loader` directory within your Minecraft game directory. If the `config/bedrock-loader` directory does not exist, create it manually.
2. Restart your Minecraft game. Bedrock Loader will automatically detect and load the addons into your Minecraft Java Edition.

## Running and Packaging from Source

To run and package Bedrock Loader for distribution, use the following Gradle tasks:

- Run in Development Environment:
    ```bash
    ./gradlew runClient
    ```

- Package for Release:
    ```bash
    ./gradlew build
    ```
The packaged mod will be in the build/libs directory, ready for distribution.

## Early Development Stage

Bedrock Loader is currently in its early stages of development.

A significant portion of features has yet to be implemented, with our current focus on architectural needs such as custom blocks and models, as well as entity models.

There are numerous bugs present, and the mod is nearly unusable in its current state.

We are actively working on addressing these issues and adding new features.

## TODO list

- [x] Basic mod structure
- [x] Addon loading & resource pack initialization
- [ ] Bedrock model baking
- [ ] Custom blocks
  - [x] Single cube texture
  - [ ] collision boxes
  - [ ] block states
  - [ ] block models
  - [ ] block entity
  - [ ] block loot table
  - [ ] block tags
  - [ ] block sounds
- [ ] Items
- [ ] Entities(Mobs)