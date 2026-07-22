# Secure Multi-Client Chat Application

A Java-based secure client-server chat application that enables real-time communication between multiple clients using TCP sockets. The project implements **SHA-256 hashing** to verify message integrity and includes an **automatic conversation backup system** for preserving chat history.

---

## 📖 Overview

This project demonstrates the fundamentals of secure network programming using Java. A central server manages multiple client connections simultaneously, allowing users to exchange messages in real time. To enhance security, each message is hashed using SHA-256 to verify its integrity, while all conversations are automatically backed up with timestamps.

---

## 🔐 Features

- Multi-client TCP chat server
- Real-time communication between connected clients
- SHA-256 hashing for message integrity verification
- Automatic conversation backup with timestamps
- Client-side and server-side logging
- File-based chat history storage
- Cross-platform compatibility (Windows, Linux, macOS)
- Simple command-line interface

---

## 🛠️ Technologies Used

- Java
- TCP Socket Programming
- Java Networking API
- Java Security (SHA-256)
- File I/O
- Multithreading
- NetBeans IDE

---

## 📂 Project Structure

```
Secure-Multi-Client-Chat/
│
├── Server/
│   ├── Server.java
│   └── ...
│
├── Client/
│   ├── Client.java
│   └── ...
│
├── backups/
│   └── Conversation log files
│
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- Java 8 or later
- NetBeans IDE (recommended) or any Java IDE

### Installation

Clone the repository:

```bash
git clone https://github.com/your-username/secure-multi-client-chat.git
```

Open the project in your preferred Java IDE.

---

## ▶️ Running the Application

### Start the Server

1. Open `Server.java`
2. Run the application.
3. The server starts listening on **port 5000**.

### Start the Client

1. Open `Client.java`
2. Run the application.
3. Connect to `localhost` on **port 5000**.

You can launch multiple client instances to simulate multiple users chatting simultaneously.

---

## 🔒 Security

This project uses the **SHA-256 hashing algorithm** to verify the integrity of transmitted messages. Although SHA-256 does not encrypt messages, it helps ensure that message contents have not been altered during transmission.

---

## 💾 Conversation Backup

Every conversation is automatically saved with timestamps, allowing chat history to be preserved for future reference.

---

## 🎯 Learning Outcomes

This project helped strengthen my understanding of:

- TCP client-server architecture
- Socket programming in Java
- Multi-client communication
- Java multithreading
- Secure hashing with SHA-256
- File handling and data persistence
- Network application development

---

## 🚀 Future Improvements

- End-to-end encryption (AES/RSA)
- User authentication and login system
- Graphical User Interface (JavaFX)
- Private messaging
- File sharing
- Online user status
- Database integration
- Secure SSL/TLS communication

---

## 👨‍💻 Author

Developed by **Enas Al-Jadaa**

Bachelor's Degree in Information and Network Security

---

## 📄 License

This project is intended for educational and learning purposes.
