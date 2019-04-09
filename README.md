# Minimal CLJS VSCode extension using shadow-cljs.

This is a CLJS translation of the "Hello World Minimal Sample" for VSCode.
Original repo: https://github.com/Microsoft/vscode-extension-samples/tree/master/helloworld-minimal-sample

# Installation
Clone this repo.

Open the repo-folder in vscode (`File > Open`).

In a terminal (`Terminal > New Terminal`), do the following:

Install [shadow-cljs](https://shadow-cljs.github.io/docs/UsersGuide.html#_installation):\
`npm install -g shadow-cljs`

And it's dependencies:\
`npm install --save-dev shadow-cljs`

The `package.json` already contains the dependencies, but I wanted you to do this step, in case you later decide to create a new extension!

Compile:\
`shadow-cljs compile dev`

Start debugging (`Debug > Start Debugging`).

Run the command by opening the command palette (`View > Command Palette...`), then write "Hello World" and press Enter.

Voila!
