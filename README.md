# 🛡️ LurkWap: Java-Based Telegram Steganography Bot

**LurkWap** is a specialized Telegram bot designed for covert communication. It allows users to "lurk" their private data within standard image files using digital steganography, ensuring that sensitive messages remain hidden in plain sight.

### 🚀 Key Features
* **Covert Text Encoding:** Seamlessly hide secret text strings inside image pixels using the LurkWap engine.
* **Secure Extraction:** A dedicated `/decode` mode to pull hidden ciphers back out of received images.
* **Stealth Transmission:** Optimized for **PNG** formatting to ensure no data is lost during Telegram's compression.
* **Interactive UI:** Fully integrated with the Telegram Bot API for a smooth, command-based experience.

### 🛠️ Technical Stack
* **Bot Name:** LurkWap / LurkWapBot
* **Language:** Java 21
* **Framework:** Spring Boot 3
* **Logic:** LSB (Least Significant Bit) Steganography

### 📖 How to Use
1. **Initiate:** Send `/start` to **LurkWapBot**.
2. **Encode:** Send your secret message, followed by the image you want to hide it in.
3. **Decode:** Use the `/decode` command and upload a "Lurked" PNG file as a **Document** to reveal the secret.

---
*LurkWap: Privacy through invisibility.* 
