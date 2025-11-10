# Bedrock Loader

**English | [简体中文](README_zh.md)**

_**Game Version Notice:** Only for Minecraft Java Edition 1.20.6

_**Note:** This project is currently in its early stages of development and is not yet ready for use._


Bedrock Loader is a pioneering mod for Minecraft Java Edition, designed to bridge the gap between Java and Bedrock editions by allowing the loading of Bedrock addons directly into the Java version. Developed as a Fabric mod, Bedrock Loader aims to enhance the Minecraft experience by integrating the diverse world of Bedrock addons, including custom blocks, models, and entities, into the Java edition's rich ecosystem.

## Installation and Setup

Before you begin, ensure you have Fabric Loader and Fabric API installed in your Minecraft Java Edition. Follow these steps to set up Bedrock Loader:

1. Download the latest version of Bedrock Loader from the Releases section.
2. Place the downloaded `.jar` file in your `mods` folder located in your Minecraft directory.
3. Download and install dependence mods: [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api) and [Fabric Language Kotlin](https://www.curseforge.com/minecraft/mc-mods/fabric-language-kotlin).
4. Launch Minecraft Java Edition using the Fabric profile.

## Using Bedrock Loader

To load Bedrock addons with Bedrock Loader, follow these steps:

1. Place your Bedrock edition addons (`.zip` or `.mcpack` files) into the `config/bedrock-loader` directory within your Minecraft game directory. If the `config/bedrock-loader` directory does not exist, create it manually.
2. Restart your Minecraft game. Bedrock Loader will automatically detect and load the addons into your Minecraft Java Edition.

## Remote Pack Synchronization

Bedrock Loader supports automatic synchronization of resource packs from a remote server. This feature allows server administrators to centrally manage and distribute addons to clients.

### Directory Structure

The mod uses two separate directories to manage addons:

- **`config/bedrock-loader/`** - For manually placed addons (never modified by remote sync)
- **`config/bedrock-loader/remote/`** - For remotely downloaded addons (automatically managed)

This separation ensures your custom addons are never affected by remote synchronization.

### Client Configuration

Create a `client.yml` file in the `config/bedrock-loader/` directory with the following configuration:

```yaml
# Enable or disable remote synchronization
enabled: true

# Server URL (including protocol and port)
serverUrl: "http://localhost:8080"

# HTTP request timeout in seconds
timeoutSeconds: 10

# Show UI progress window during sync
showUI: true

# Auto-cancel sync on any error
autoCancelOnError: false

# Automatically remove packs that have been deleted from the server
# Only affects packs in the remote/ directory
autoCleanupRemovedPacks: true
```

### Configuration Options

- **`enabled`**: Enable/disable remote synchronization. Set to `false` to use only local packs.
- **`serverUrl`**: The HTTP server URL where resource packs are hosted.
- **`timeoutSeconds`**: Network request timeout duration.
- **`showUI`**: Display a graphical progress window during download (client-side only).
- **`autoCancelOnError`**: Whether to cancel synchronization if any error occurs.
- **`autoCleanupRemovedPacks`**: Automatically delete remotely downloaded packs that no longer exist on the server.

### How It Works

1. On game startup, the client connects to the configured server
2. The server provides a manifest of available resource packs with MD5 checksums
3. The client compares local packs (in `remote/`) with the server manifest
4. Missing or outdated packs are automatically downloaded to `remote/`
5. If enabled, packs deleted from the server are removed from `remote/`
6. All packs (both manual and remote) are loaded into the game

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
- [x] Bedrock model baking
- [ ] Custom blocks
  - [x] Single cube texture
  - [x] collision boxes
  - [ ] block states
  - [x] block models
  - [x] block entity
  - [ ] block loot table
  - [ ] block tags
  - [ ] block sounds
- [ ] Items
- [x] Entities