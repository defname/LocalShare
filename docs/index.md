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
access your device with the same URL everytime.

The other option in this section is to select an IP address (or a network interface)
to bind the HTTP server to. This might be useful if your phone is connected to multiple networks
at the same time, and you want to limit the access to a specific network. In this
case you can bind the server to an IP to only accept connections that try to connect
to this specific IP address.

#### Recent Activities

Here you see the latest log entries. See the [Logs Screen](#logs-screen) for
more details.

#### "Start/Stop Sharing"-Button

Press this button to start or stop sharing. While sharing is activated a notification is shown
in Androids notification menu.

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
to access your files, there will be always an active connection in the list (this is the SSE
connection to the web interface that is responsible for live updates).

### Settings Screen

### Info Screen

## FAQ

Why is there a spinning wheel in the top of the logs list?
: 