# QuizCraft - Minecraft Quiz Competition Mod

<div align="center">
  <img alt="logo" src="https://raw.githubusercontent.com/SkyDreamLG/QuizCraft/refs/heads/master/logo.svg" width="120px">
  <br><br>
  <em>🎮 Quiz Competition System for Minecraft Servers</em>
</div>

<p align="center">
  <a href="https://github.com/SkyDreamLG/QuizCraft/releases"><img alt="release" src="https://img.shields.io/github/v/release/SkyDreamLG/QuizCraft?style=for-the-badge"></a>
  <a href="https://github.com/SkyDreamLG/QuizCraft/issues"><img alt="issues" src="https://img.shields.io/github/issues/SkyDreamLG/QuizCraft?style=for-the-badge"></a>
  <a href="./LICENSE"><img alt="license" src="https://img.shields.io/badge/license-LGPL--2.1-green?style=for-the-badge"></a>
</p>

---

English | [中文](./readme_cn.md)

## Features

- [x] **Auto Quiz System**
  - [x] Automatic question intervals with configurable timing
  - [x] Question timeout handling
  - [x] JSON-based question and reward configuration

- [x] **Smart Answer Detection**
  - [x] Contains-based answer matching (e.g., "龟派气功" matches "气功")
  - [x] Case-insensitive matching
  - [x] First-correct-player wins system

- [x] **Reward System**
  - [x] Random reward selection from configurable pool
  - [x] Variable reward amounts
  - [x] Item auto-completion with MC's item selector

- [x] **Scoreboard & Ranking**
  - [x] Real-time scoreboard display on client side
  - [x] Top 15 players ranking with colored positions
  - [x] Persistent score storage

- [x] **Comprehensive Commands**
  - [x] Player commands: `/qc question`
  - [x] Admin commands: Question/reward management, scoreboard control
  - [x] Permission-based command access

- [x] **Easy Configuration**
  - [x] JSON configuration files
  - [x] Hot-reload support
  - [x] Customizable messages and intervals

## Quick Start

### Installation
1. Download the latest `quizcraft.jar` from [Releases](https://github.com/skydream/quizcraft/releases)
2. Place it in your server's `mods` folder
3. Restart the server

### Basic Usage
**For Players:**
- Answer questions directly in chat when they appear
- Use `/qc question` to manually start a new question
- Check your ranking on the right-side scoreboard

**For Admins:**
```bash
# Reload configuration
/qc reload

# Add questions
/qc add question "What tool mines diamond?" "Iron Pickaxe"

# Add rewards with auto-completion
/qc add reward minecraft:diamond 5

# Manage rankings
/qc list ranking
/qc reset ranking
```

## Configuration

Configuration files are located in `config/quizcraft/`:

- `questions.json` - Question and answer pairs
- `rewards.json` - Reward items and maximum amounts
- `scoreboard.json` - Player scores and rankings

### Example Configuration

**questions.json:**
```json
[
  {
    "question": "What mob explodes in Minecraft?",
    "answer": "Creeper"
  }
]
```

**rewards.json:**
```json
[
  {
    "itemId": "minecraft:diamond",
    "maxAmount": 5
  }
]
```

## Commands Reference

### Player Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/qc question` | Start a new question | All players |

### Admin Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/qc reload` | Reload configuration | OP |
| `/qc add question <question> <answer>` | Add new question | OP |
| `/qc add reward <item> <maxAmount>` | Add reward item | OP |
| `/qc list question` | List all questions | OP |
| `/qc list reward` | List all rewards | OP |
| `/qc list ranking` | Show leaderboard | OP |
| `/qc reset ranking` | Reset scores | OP |
| `/qc remove question <question>` | Remove question | OP |
| `/qc remove reward <item>` | Remove reward | OP |

## Development

### Building from Source
```bash
git clone https://github.com/skydream/quizcraft.git
cd quizcraft
./gradlew build
```

### Requirements
- Minecraft 1.20.1+
- NeoForge
- Java 17+

## Support

- [GitHub Discussions](https://github.com/skydream/quizcraft/discussions) - For questions and support
- [Issue Tracker](https://github.com/skydream/quizcraft/issues) - For bug reports and feature requests

## License

This project is licensed under the GNU LGPL 2.1 License - see the [LICENSE](LICENSE) file for details.