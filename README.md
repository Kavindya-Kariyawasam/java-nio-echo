# Java NIO Echo Server & Client

This project demonstrates a **non-blocking Echo Server** and **Echo Client** built with Java NIO (New I/O) using `ServerSocketChannel`, `SocketChannel`, `Selector`, and `ByteBuffer`.

## Files
- `NonBlockingEchoServer.java` - Server that accepts multiple clients concurrently and echoes their messages.
- `NonBlockingEchoClient.java` - Client that connects to the server, sends user input, and prints echoed responses.
- `Instructions.txt` - Steps to compile, run, and explanation of how the server/client work.

## How to Run

### 1. Compile
```bash
javac NonBlockingEchoServer.java NonBlockingEchoClient.java
```

### 2. Start the server
```bash
java NonBlockingEchoServer
```

Output:
```
Server started on port 5000
```

### 3. Start the client
```bash
java NonBlockingEchoClient localhost 5000
```

Type any message in the client terminal. The server will echo it back.

Example:
```
Client: hello
Server: hello
```

To quit, type:
```
quit
```

## How it Works

### Server
* Opens a `ServerSocketChannel` on port `5000`.
* Registers with a `Selector` for two events:
  * **OP_ACCEPT**: Accept new clients.
  * **OP_READ**: Read incoming messages and echo them back.
* Uses `ByteBuffer` to temporarily store data.
* Handles multiple clients in a single thread efficiently.

### Client
* Opens a `SocketChannel` in non-blocking mode.
* Registers with `Selector` for:
  * **OP_CONNECT**: Complete the connection.
  * **OP_READ**: Read echoed messages from server.
  * **OP_WRITE**: Send messages typed by the user.
* Runs a background thread to read console input and send messages to the server.
* Terminates when the user types `"quit"`.

## Key Concepts

* **Channel** - abstraction of network connections (`ServerSocketChannel`, `SocketChannel`).
* **Selector** - allows handling multiple channels with one thread.
* **SelectionKey** - represents channel registration with selector (interestOps: `OP_ACCEPT`, `OP_READ`, etc.).
* **ByteBuffer** - stores data for read/write operations.
* **Non-blocking I/O** - enables concurrent client handling without multiple threads.

## Why NIO?

Using Java NIO provides scalability:
* A single thread handles many clients.
* Server does not block on slow clients.
* More efficient than traditional blocking I/O.

---

## Example Usage

Server terminal:
```
Server started on port 5000
Connected to client: /127.0.0.1:54321
Client disconnected: /127.0.0.1:54321
```

Client terminal:
```
Connected to server /127.0.0.1:5000
Type messages to send (type 'quit' to exit):
hello
Echo from server: hello
quit
Client exiting.
```

---

## License

This project is for educational use under the **MIT License**.