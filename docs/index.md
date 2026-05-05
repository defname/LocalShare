# LocalShare - Documentation

## General Usage

To share files from your Android phone with any other device in your local
network you can select them inside the LocalShare app or share them directly
from other apps via the Android share-sheet.

Shared files will appear on the [Home Screen](#home-screen) and on the
[Shared Files Screen](#shared-files-screen).

Text snippets can also be shared. Use the Android share sheet from any other
app and select LocalShare as the target or paste text from the clipboard
to the [Shared Text Screen](#shared-text-screen).

When you click the "Start Sharing" button your shared files and texts will
be made available. Anyone who wants to access your shared content needs
to know the URL displayed on the [Home Screen](#home-screen) while sharing.

## Technical Details

LocalShare uses an integrated web server to make shared files available through
a browser-based user interface. This allows any device in the same network to
access the content without requiring additional software.

To ensure that newly shared files appear instantly in the web interface, the
application uses Server-Sent Events (SSE). This enables real-time updates
without requiring the user to refresh the page.

Since LocalShare is primarily designed for use within local networks, it relies
on plain HTTP instead of HTTPS. Using HTTPS in local environments is often
impractical, as valid signed certificates are typically not available for local
IP addresses.

The HTTP server can be bound to a specific IP address. This means that only
connections made to that particular address are accepted. In general, anyone
who can reach your device over the network can access the shared content,
provided they know both the correct IP address and the associated secret token.

## Screens

### Home Screen

#### "Hero Card"

At the top of the screen you see a section that indicates whether sharing is
active or not. You can access the URL or the QR code with which your files
are accessible.

#### Shared Files

Here you see a list of all shared files. You can add files by pressing the "Add
Files" button or remove them by long-pressing on a file icon and then on "Remove".
The small "X" on the right clears the file list completely.

#### Technical Details

You can set the security token manually (the default behavior is that the token
is generated randomly when the app starts, you can disable it in the
[Settings Screen](#settings-screen)). This might be useful when you want to
access your device using the same URL every time.

The other option in this section is to select an IP address (or a network interface)
to which the HTTP server should be bound. This might be useful if your phone is connected to multiple networks
at the same time, and you want to limit the access to a specific network. In this
case you can bind the server to an IP to only accept connections that try to connect
to this specific IP address.

#### Recent Activities

Here you see the latest log entries. See the [Logs Screen](#logs-screen) for
more details.

#### "Start/Stop Sharing"-Button

Press this button to start or stop sharing. While sharing is activated a notification is shown
in the Android notification menu.

### Shared Files Screen

On this screen you find a sortable list of all shared files. You can add files
by pressing the "+" button or remove them by long-pressing on a file and
then on "Remove".

### Shared Text Screen

On this screen you find a list of all text snippets that are shared. You can 
paste text from the clipboard by pressing on the paste button. Select snippets
by long-pressing to remove them.

### Logs Screen

Here you find a list of established connections to your device (or better to *Local Share*).
Last and still open connections are at the top. If you open the web interface on another device
to access your files, there will always be an active connection in the list (this is the SSE
connection to the web interface that is responsible for live updates).

The list has three columns to show the status of the connection, the path that is accessed and
the client who opened the connection.

The client's IP might be gray (in this case the connection waits for approval), green (the client
got whitelisted) or red (the client got blacklisted).

By long-pressing an entry you can add a client to the blacklist or remove it from the whitelist
or the blacklist. (You can not whitelist a client this way. A client that tries to connect will
trigger a notification to ask for approval. Approval leads to whitelisting).

Whitelist and blacklist are reset on app start.

#### Status Codes and Symbols

- **Spinning Wheel** The connection is still open.
- **Green 200** OK: The content was served successfully.
- **Green 204** No Content: It's used to prevent the browser to ask for a favicon for the web interface on the default location.
- **Green 206** Partial Content: The server is delivering only part of the resource due to a range header sent by the client. This is used for video seeking and resuming downloads.
- **Red 403** Forbidden: Access was denied (invalid token or path).
- **Red 404** Not Found: A file that should exist was not found.
- **Red 410** Gone: A file that was available earlier is not accessible anymore (this can happen if a file was shared via the Android share-sheet since the file URI might only be a temporary one and become invalid).
- **Red 500** Internal Server Error: Something went wrong on the server side (e.g. template rendering). This should not happen and I would appreciate if you could file a reproducible issue.

The following codes only appear to the SSE connection.
- **Stop Symbol (Green)** Sharing was stopped and so the connection broke.
- **Close Symbol (Green)** The client closed the connection (e.g. by closing the browser window with the web interface)
- **Shield Symbol (Red)** The client was banned.
- **Question Mark (Red)** Something unknown happened.

### Settings Screen

#### Server Configuration

- **Port**: The TCP port on which the HTTP server should listen. 
  This port will be part of the URL where the web interface is accessible.
  Default is `8080` (also common is `8000`) which will work in most
  local networks.
- **Idle Timeout**: The time in seconds after which sharing stops if there are
  no active connections. (As long as a client keeps the web interface open, there will be an active connection and the server will not shutdown automatically)

#### Security

- **Regenerate Token at App Start**: Generate a new security token every time the app starts.
- **Require manual approval**: If activated every new client that tries to connect will trigger a notification to ask for approval (highly recommended in all networks that are not 100% trustworthy)
- **Whitelist Entry TTL**: After how many seconds does a client needs to be approved again.

#### Webinterface

- **SSE Heartbeat Period**: To check if an SSE connection to a web interface is still active the server needs to send a "heartbeat" event. A longer period means it takes more time to detect closed connections (e.g. when a client closes the browser window with the web interface).

#### Misc

- **Clear File List on Share**: If activated the file list will be cleared every time new files are shared via the Android share-sheet.

### Info Screen

Here you find the version and most important the link to the issues page on GitHub.

## FAQ

### Why is there a spinning wheel in the top of the logs list?

There is an open connection. Most likely an SSE connection to a web interface.

### Can I stream videos directly to a media player?

Glad you asked - that’s actually the main reason this app exists. Just copy the link to the file from the web interface and open it in any video-player you like to enjoy full seeking support. (The video file format and the player still need to support streaming)


